package `in`.xroden.flockr.domain.usecase.house

import java.security.SecureRandom
import javax.inject.Inject

/**
 * Use case to generate secure invite codes for houses
 */
class GenerateInviteCodeUseCase @Inject constructor() {

    private val random = SecureRandom()
    private val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // Removed ambiguous chars (0, O, 1, I)

    /**
     * Generate a secure random invite code
     * 
     * @param length Length of the code (default 8)
     * @return Generated invite code
     */
    operator fun invoke(length: Int = 8): String {
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Validate invite code format
     */
    fun isValidFormat(code: String): Boolean {
        return code.length >= 6 && 
               code.length <= 12 && 
               code.all { it in chars }
    }
}


