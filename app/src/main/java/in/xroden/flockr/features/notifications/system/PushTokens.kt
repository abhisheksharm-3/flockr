/** Links this phone's push token to the signed-in member, so the database's notifications reach it. */
package `in`.xroden.flockr.features.notifications.system

import com.google.firebase.messaging.FirebaseMessaging
import `in`.xroden.flockr.core.logging.Logger
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A failure here only delays pushes, and the next sign-in or token refresh retries, so it is logged, not shown. */
@Singleton
class PushTokens @Inject constructor(private val supabase: SupabaseClient) {

    /** Registers [token], or this phone's current one when null, for the signed-in member. */
    suspend fun register(token: String? = null) {
        if (supabase.auth.currentUserOrNull() == null) return
        runCatching {
            val current = token ?: FirebaseMessaging.getInstance().token.await()
            supabase.postgrest.rpc("register_device_token", buildJsonObject {
                put("p_token", current)
                put("p_platform", "android")
            })
        }.onFailure { Logger.w("PushTokens", "Could not register the push token", it) }
    }

    /** Stops pushes for the member who is signing out; must run while their session is still valid. */
    suspend fun unregister() {
        runCatching {
            val current = FirebaseMessaging.getInstance().token.await()
            supabase.postgrest.rpc("unregister_device_token", buildJsonObject { put("p_token", current) })
        }.onFailure { Logger.w("PushTokens", "Could not unregister the push token", it) }
    }
}
