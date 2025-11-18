package `in`.xroden.flockr.features.documents.data

import `in`.xroden.flockr.data.dto.DocumentInsert
import `in`.xroden.flockr.features.documents.model.Document
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Objects.isNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    private val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    suspend fun getPersonalDocuments(): Result<List<Document>> {
        return try {
            val currentUserId = userId ?: return Result.success(emptyList())

            val documents = supabase.from("documents")
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", currentUserId)
                        isNull("house_id")
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Document>()

            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getHouseDocuments(houseId: String): Result<List<Document>> {
        return try {
            val documents = supabase.from("documents")
                .select(Columns.ALL) {
                    filter {
                        eq("house_id", houseId)
                    }
                    order("created_at", io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }
                .decodeList<Document>()

            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
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

            val bucket = if (houseId != null) "house-documents" else "personal-documents"
            val path = if (houseId != null) {
                "$houseId/$currentUserId/$fileName"
            } else {
                "$currentUserId/$fileName"
            }

            supabase.storage.from(bucket).upload(path, fileData, upsert = false)

            val document = supabase.from("documents")
                .insert(
                    DocumentInsert(
                        houseId = houseId,
                        userId = currentUserId,
                        storagePath = path,
                        fileName = fileName,
                        fileSize = fileData.size.toLong(),
                        mimeType = mimeType
                    )
                ) {
                    select()
                }
                .decodeSingle<Document>()

            if (houseId != null) {
                try {
                    @Serializable
                    data class HouseNotificationParams(
                        @SerialName("p_house_id")
                        val houseId: String,
                        @SerialName("p_title")
                        val title: String,
                        @SerialName("p_message")
                        val message: String,
                        @SerialName("p_type")
                        val type: String,
                        @SerialName("p_data")
                        val data: String,
                        @SerialName("p_exclude_user_id")
                        val excludeUserId: String?
                    )

                    supabase.postgrest.rpc(
                        function = "create_notification_for_house",
                        parameters = HouseNotificationParams(
                            houseId = houseId,
                            title = "New Document Uploaded",
                            message = "Uploaded a new document: $fileName.",
                            type = "document",
                            data = """{"id":"${document.id}"}""",
                            excludeUserId = currentUserId
                        )
                    )
                } catch (e: Exception) {
                    // Ignore notification errors
                }
            }

            Result.success(document)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteDocument(documentId: String, storagePath: String): Result<Unit> {
        return try {
            val bucket = if (storagePath.contains("/") && !storagePath.startsWith("personal")) {
                "house-documents"
            } else {
                "personal-documents"
            }

            try {
                supabase.storage.from(bucket).delete(storagePath)
            } catch (e: Exception) {
                // Continue even if storage deletion fails
            }

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

    suspend fun getDocumentUrl(storagePath: String, houseId: String?): Result<String> {
        return try {
            val bucket = if (houseId != null) "house-documents" else "personal-documents"
            val url = supabase.storage.from(bucket).createSignedUrl(storagePath, kotlin.time.Duration.parse("PT1H"))

            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadDocument(storagePath: String): Result<ByteArray> {
        return try {
            // Extract bucket from storagePath (format: bucket/path/to/file)
            val pathParts = storagePath.split("/", limit = 2)
            if (pathParts.size < 2) {
                return Result.failure(Exception("Invalid storage path format"))
            }
            val bucket = pathParts[0]
            val filePath = pathParts[1]
            
            val data = supabase.storage.from(bucket).downloadAuthenticated(filePath)
            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
