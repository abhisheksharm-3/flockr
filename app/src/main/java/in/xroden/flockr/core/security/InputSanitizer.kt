package `in`.xroden.flockr.core.security

/**
 * Input sanitization utilities for preventing injection attacks.
 * Sanitizes user input before storage or display.
 */
object InputSanitizer {

    /**
     * Sanitizes text input to prevent XSS attacks.
     * Escapes HTML special characters.
     */
    fun sanitizeText(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")
            .trim()
    }

    /**
     * Sanitizes JSON strings to prevent injection.
     * Removes control characters and escapes quotes.
     */
    fun sanitizeJsonValue(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .filter { it.code >= 32 || it == '\n' || it == '\r' || it == '\t' }
            .trim()
    }

    /**
     * Sanitizes SQL-like input (for query parameters).
     * Removes potentially dangerous characters.
     */
    fun sanitizeQueryParam(input: String): String {
        return input
            .replace(";", "")
            .replace("--", "")
            .replace("/*", "")
            .replace("*/", "")
            .replace("'", "''")
            .trim()
    }

    /**
     * Sanitizes file names to prevent path traversal.
     * Removes directory separators and special characters.
     */
    fun sanitizeFileName(input: String): String {
        return input
            .replace("/", "_")
            .replace("\\", "_")
            .replace("..", "_")
            .replace(":", "_")
            .replace("*", "_")
            .replace("?", "_")
            .replace("\"", "_")
            .replace("<", "_")
            .replace(">", "_")
            .replace("|", "_")
            .trim()
            .take(255) // Limit filename length
    }

    /**
     * Validates and sanitizes URL input.
     * Returns null if URL is invalid or dangerous.
     */
    fun sanitizeUrl(input: String): String? {
        val trimmed = input.trim()

        // Only allow http and https protocols
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)) {
            return null
        }

        // Check for common injection patterns
        if (trimmed.contains("javascript:", ignoreCase = true) ||
            trimmed.contains("data:", ignoreCase = true) ||
            trimmed.contains("vbscript:", ignoreCase = true)) {
            return null
        }

        return trimmed
    }

    /**
     * Sanitizes user display names.
     * Removes dangerous characters while allowing unicode.
     */
    fun sanitizeDisplayName(input: String): String {
        return input
            .replace("<", "")
            .replace(">", "")
            .replace("&", "")
            .trim()
            .take(100) // Limit display name length
    }

    /**
     * Sanitizes markdown/rich text input.
     * Allows basic markdown but prevents script injection.
     */
    fun sanitizeMarkdown(input: String): String {
        return input
            .replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("<iframe[^>]*>.*?</iframe>", RegexOption.IGNORE_CASE), "")
            .replace(Regex("javascript:", RegexOption.IGNORE_CASE), "")
            .replace(Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}

/**
 * Extension functions for easy sanitization.
 */
fun String.sanitizeForDisplay(): String = InputSanitizer.sanitizeText(this)
fun String.sanitizeForJson(): String = InputSanitizer.sanitizeJsonValue(this)
fun String.sanitizeForQuery(): String = InputSanitizer.sanitizeQueryParam(this)
fun String.sanitizeAsFileName(): String = InputSanitizer.sanitizeFileName(this)
fun String.sanitizeAsUrl(): String? = InputSanitizer.sanitizeUrl(this)
fun String.sanitizeAsDisplayName(): String = InputSanitizer.sanitizeDisplayName(this)
