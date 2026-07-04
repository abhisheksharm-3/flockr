package `in`.xroden.flockr.core.security

/** Input sanitization utilities for preventing injection attacks. */
object InputSanitizer {

    /**
     * Normalizes free-text input for storage. This is a native app persisting to
     * PostgREST (parameterized) and rendering with Compose Text (no HTML sink), so
     * HTML-escaping would only corrupt data (e.g. "Mom's" -> "Mom&#39;s"). We only
     * trim and strip control characters here; injection safety is the SDK's job.
     */
    fun sanitizeText(input: String): String = input
        .filter { it == '\n' || it == '\t' || it.code >= 32 }
        .trim()

    /** Sanitizes JSON strings by escaping control characters. */
    fun sanitizeJsonValue(input: String): String = input
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .filter { it.code >= 32 || it == '\n' || it == '\r' || it == '\t' }
        .trim()

    /** Sanitizes SQL-like input for query parameters. */
    fun sanitizeQueryParam(input: String): String = input
        .replace(";", "")
        .replace("--", "")
        .replace("/*", "")
        .replace("*/", "")
        .replace("'", "''")
        .trim()

    /** Sanitizes file names to prevent path traversal. */
    fun sanitizeFileName(input: String): String = input
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
        .take(255)

    /** Validates and sanitizes URL input. Returns null if invalid or dangerous. */
    fun sanitizeUrl(input: String): String? {
        val trimmed = input.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)) {
            return null
        }
        if (trimmed.contains("javascript:", ignoreCase = true) ||
            trimmed.contains("data:", ignoreCase = true) ||
            trimmed.contains("vbscript:", ignoreCase = true)) {
            return null
        }
        return trimmed
    }

    /** Sanitizes user display names while allowing unicode. */
    fun sanitizeDisplayName(input: String): String = input
        .replace("<", "")
        .replace(">", "")
        .replace("&", "")
        .trim()
        .take(100)

    /** Sanitizes markdown input, preventing script injection. */
    fun sanitizeMarkdown(input: String): String = input
        .replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("<iframe[^>]*>.*?</iframe>", RegexOption.IGNORE_CASE), "")
        .replace(Regex("javascript:", RegexOption.IGNORE_CASE), "")
        .replace(Regex("on\\w+\\s*=", RegexOption.IGNORE_CASE), "")
        .trim()
}

fun String.sanitizeForDisplay(): String = InputSanitizer.sanitizeText(this)
fun String.sanitizeForJson(): String = InputSanitizer.sanitizeJsonValue(this)
fun String.sanitizeForQuery(): String = InputSanitizer.sanitizeQueryParam(this)
fun String.sanitizeAsFileName(): String = InputSanitizer.sanitizeFileName(this)
fun String.sanitizeAsUrl(): String? = InputSanitizer.sanitizeUrl(this)
fun String.sanitizeAsDisplayName(): String = InputSanitizer.sanitizeDisplayName(this)
