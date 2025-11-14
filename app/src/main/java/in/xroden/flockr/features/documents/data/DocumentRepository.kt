package `in`.xroden.flockr.features.documents.data

import `in`.xroden.flockr.features.documents.model.Document
import `in`.xroden.flockr.data.model.CreateNotificationWithTypeParams
import `in`.xroden.flockr.utils.FlockrLogger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.Objects.isNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id
    
    companion object {
        private const val TAG = "DocumentRepository"
    }

    suspend fun getPersonalDocuments(): List<Document> {
        FlockrLogger.repoStart(TAG, "getPersonalDocuments", emptyMap())
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "getPersonalDocuments: No user logged in")
                return emptyList()
            }

            val documents = supabase.from("documents")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                        isNull("house_id")
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Document>()
            FlockrLogger.repoSuccess(TAG, "getPersonalDocuments", "Found ${documents.size} documents")
            documents
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getPersonalDocuments", e)
            emptyList()
        }
    }

    suspend fun getHouseDocuments(houseId: String): List<Document> {
        FlockrLogger.repoStart(TAG, "getHouseDocuments", mapOf("houseId" to houseId))
        return try {
            val documents = supabase.from("documents")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Document>()
            FlockrLogger.repoSuccess(TAG, "getHouseDocuments", "Found ${documents.size} documents")
            documents
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "getHouseDocuments", e)
            emptyList()
        }
    }

    suspend fun uploadDocument(
        houseId: String?,
        fileName: String,
        fileData: ByteArray,
        mimeType: String
    ): Result<Document> {
        FlockrLogger.repoStart(TAG, "uploadDocument", mapOf(
            "fileName" to fileName,
            "fileSize" to fileData.size,
            "houseId" to houseId,
            "mimeType" to mimeType
        ))
        return try {
            val currentUserId = userId ?: run {
                FlockrLogger.e(TAG, "uploadDocument: No user logged in")
                return Result.failure(Exception("No user logged in"))
            }

            // Upload to storage - use separate buckets for house and personal docs
            val bucket = if (houseId != null) "house-documents" else "personal-documents"
            val path = if (houseId != null) {
                "$houseId/$currentUserId/$fileName"
            } else {
                "$currentUserId/$fileName"
            }

            FlockrLogger.d(TAG, "uploadDocument: Uploading to bucket=$bucket, path=$path")
            supabase.storage.from(bucket).upload(path, fileData, upsert = false)
            FlockrLogger.d(TAG, "uploadDocument: Upload to storage successful")

            // Create document record
            val document = supabase.from("documents")
                .insert(
                    mapOf(
                        "house_id" to houseId,
                        "user_id" to currentUserId,
                        "storage_path" to path,
                        "file_name" to fileName,
                        "file_size" to fileData.size,
                        "mime_type" to mimeType
                    )
                ) {
                    select()
                }
                .decodeSingle<Document>()

            FlockrLogger.d(TAG, "uploadDocument: Document record created: ${document.id}")

            // Create notification if house document
            if (houseId != null) {
                val notificationParams = CreateNotificationWithTypeParams(
                    houseId = houseId,
                    title = "New Document Uploaded",
                    message = "Uploaded a new document: $fileName.",
                    type = "document",
                    data = """{"id":"${document.id}"}""",
                    excludeUserId = currentUserId
                )
                supabase.postgrest.rpc(
                    function = "create_notification_for_house",
                    parameters = notificationParams
                )
            }

            Result.success(document)
        } catch (e: Exception) {
            android.util.Log.e("DocumentRepository", "Error uploading document", e)
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(documentId: String, storagePath: String): Result<Unit> {
        return try {
            // Delete from storage - determine bucket from path
            val bucket = if (storagePath.contains("/") && !storagePath.startsWith("personal")) {
                "house-documents"
            } else {
                "personal-documents"
            }
            FlockrLogger.d(TAG, "deleteDocument: Deleting from bucket=$bucket, path=$storagePath")
            supabase.storage.from(bucket).delete(storagePath)

            // Delete record
            supabase.from("documents")
                .delete {
                    filter {
                        eq("id", documentId)
                    }
                }

            FlockrLogger.d(TAG, "deleteDocument: Successfully deleted document $documentId")
            Result.success(Unit)
        } catch (e: Exception) {
            FlockrLogger.repoError(TAG, "deleteDocument", e)
            Result.failure(e)
        }
    }
}

