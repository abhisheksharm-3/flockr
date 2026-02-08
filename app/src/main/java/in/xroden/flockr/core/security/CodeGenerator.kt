package `in`.xroden.flockr.core.security

import java.security.SecureRandom

/** Utility for generating secure random codes. */
object CodeGenerator {
    private const val ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val secureRandom = SecureRandom()

    /** Generates a cryptographically secure invite code. */
    fun generateInviteCode(length: Int = 6): String {
        return (1..length)
            .map { ALPHANUMERIC_CHARS[secureRandom.nextInt(ALPHANUMERIC_CHARS.length)] }
            .joinToString("")
    }
}
