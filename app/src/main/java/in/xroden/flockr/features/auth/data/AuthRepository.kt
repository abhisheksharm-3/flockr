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

    suspend fun signUp(email: String, password: String, fullName: String): Result<Unit> = runCatching {
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
            supabase.from("profiles")
                .select(Columns.ALL) {
                    filter { eq("id", newUserId) }
                }
                .decodeSingle<Profile>()
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> = runCatching {
        supabase.auth.signInWith(io.github.jan.supabase.gotrue.providers.builtin.Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut(): Result<Unit> = runCatching {
        supabase.auth.signOut()
    }

    suspend fun getProfile(): Result<Profile?> = runCatching {
        val userId = currentUser?.id ?: return@runCatching null

        supabase.from("profiles")
            .select(Columns.ALL) {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<Profile>()
    }

    suspend fun updateProfile(fullName: String?, hasCompletedOnboarding: Boolean?): Result<Unit> = runCatching {
        val userId = currentUser?.id ?: throw IllegalStateException("No user logged in")

        if (fullName == null && hasCompletedOnboarding == null) {
            return@runCatching
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
    }
}
