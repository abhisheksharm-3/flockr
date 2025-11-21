package `in`.xroden.flockr.features.auth.data

import `in`.xroden.flockr.data.dto.ProfileUpdate
import `in`.xroden.flockr.features.auth.model.Profile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.SessionStatus
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val supabase: SupabaseClient
) {
    val sessionFlow: Flow<SessionStatus> = supabase.auth.sessionStatus

    val currentUser: UserInfo?
        get() = supabase.auth.currentUserOrNull()

    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> {
        return try {
            supabase.auth.signUpWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                this.email = email
                this.password = password
                data = kotlinx.serialization.json.buildJsonObject {
                    put("full_name", kotlinx.serialization.json.JsonPrimitive(fullName))
                }
            }

            // Verify profile creation
            val newUserId = supabase.auth.currentUserOrNull()?.id
            if (newUserId != null) {
                try {
                    supabase.from("profiles")
                        .select(Columns.ALL) {
                            filter { eq("id", newUserId) }
                        }
                        .decodeSingle<Profile>()
                } catch (dbEx: Exception) {
                    return Result.failure(dbEx)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
                this.email = email
                this.password = password
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProfile(): Result<Profile?> {
        return try {
            val userId = currentUser?.id ?: return Result.success(null)

            val profile = supabase.from("profiles")
                .select(Columns.ALL) {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<Profile>()

            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProfile(fullName: String?, hasCompletedOnboarding: Boolean?): Result<Unit> {
        return try {
            val userId = currentUser?.id ?: return Result.failure(Exception("No user logged in"))

            if (fullName == null && hasCompletedOnboarding == null) {
                return Result.success(Unit)
            }

            supabase.from("profiles")
                .update(
                    ProfileUpdate(
                        fullName = fullName,
                        hasCompletedOnboarding = hasCompletedOnboarding
                    )
                ) {
                    filter { eq("id", userId) }
                }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
