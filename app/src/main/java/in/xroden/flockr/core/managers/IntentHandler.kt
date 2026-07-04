package `in`.xroden.flockr.core.managers

import android.content.Intent
import android.net.Uri
import `in`.xroden.flockr.core.validation.Validators

/**
 * Handles deep link and notification intent processing.
 * Extracts and validates invite codes from various intent sources.
 */
object IntentHandler {

    /**
     * Processes an intent to extract invite codes.
     *
     * @param intent The intent to process
     * @return The validated invite code if found, null otherwise
     */
    fun extractInviteCode(intent: Intent?): String? {
        intent ?: return null

        extractFromDeepLink(intent.data)?.let { return it }
        extractFromExtras(intent)?.let { return it }

        return null
    }

    private fun extractFromDeepLink(data: Uri?): String? {
        data ?: return null

        // Manifest-registered custom scheme: flockr://invite/<code> (host = "invite").
        if (data.scheme == "flockr" && data.host == "invite") {
            data.getQueryParameter("code")?.let { return validateInviteCode(it) }
            return data.pathSegments.firstOrNull()?.let { validateInviteCode(it) }
        }

        // Optional web link form: https://flockr.app/invite/<code>.
        if (data.host == "flockr.app") {
            val segments = data.pathSegments
            if (segments.size >= 2 && segments[0] == "invite") {
                return validateInviteCode(segments[1])
            }
        }

        return null
    }

    private fun extractFromExtras(intent: Intent): String? {
        val type = intent.getStringExtra("notification_type")
            ?: intent.getStringExtra("type")

        return when {
            type == "house_invitation" || type == "HOUSE_INVITE" -> {
                val code = intent.getStringExtra("invite_code")
                    ?: intent.getStringExtra("code")
                code?.let { validateInviteCode(it) }
            }
            type?.startsWith("house_invitation:") == true -> {
                val code = type.substringAfter("house_invitation:")
                if (code.isNotEmpty()) validateInviteCode(code) else null
            }
            else -> null
        }
    }

    private fun validateInviteCode(code: String): String? =
        Validators.validateInviteCode(code).getOrNull()
}
