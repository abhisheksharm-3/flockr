package `in`.xroden.flockr.data.repository

import `in`.xroden.flockr.data.model.Document
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

    suspend fun getPersonalDocuments(): List<Document> {
        return try {
            val currentUserId = userId ?: return emptyList()

            supabase.from("documents")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                        isNull("house_id")
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Document>()
        } catch (e: Exception) {
            android.util.Log.e("DocumentRepository", "Error getting personal documents", e)
            emptyList()
        }
    }

    suspend fun getHouseDocuments(houseId: String): List<Document> {
        return try {
            supabase.from("documents")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Document>()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun uploadDocument(
        houseId: String?,
        fileName: String,
        fileData: ByteArray,
        mimeType: String
    ): Result<Document> {
        return try {
            val currentUserId = userId ?: return Result.failure(Exception("No user logged in"))

            android.util.Log.d("DocumentRepository", "Starting upload: fileName=$fileName, size=${fileData.size}, houseId=$houseId")

            // Upload to storage - use documents bucket for both
            val bucket = "documents"
            val path = if (houseId != null) {
                "house/$houseId/$currentUserId/$fileName"
            } else {
                "personal/$currentUserId/$fileName"
            }

            android.util.Log.d("DocumentRepository", "Uploading to bucket=$bucket, path=$path")
            supabase.storage.from(bucket).upload(path, fileData, upsert = false)
            android.util.Log.d("DocumentRepository", "Upload successful")

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

            android.util.Log.d("DocumentRepository", "Document record created: ${document.id}")

            // Create notification if house document
            if (houseId != null) {
                supabase.postgrest.rpc(
                    "create_notification_for_house",
                    mapOf(
                        "p_house_id" to houseId,
                        "p_title" to "New Document Uploaded",
                        "p_message" to "Uploaded a new document: $fileName.",
                        "p_type" to "document",
                        "p_data" to mapOf("type" to "document", "id" to document.id),
                        "p_exclude_user_id" to currentUserId
                    )
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
            // Delete from storage
            val bucket = if (storagePath.contains("/")) "house_documents" else "personal_documents"
            supabase.storage.from(bucket).delete(storagePath)

            // Delete record
            supabase.from("documents")
                .delete {
                    filter {
                        eq("id", documentId)
                    }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

