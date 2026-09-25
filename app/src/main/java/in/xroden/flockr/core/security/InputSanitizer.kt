package `in`.xroden.flockr.core.security

/** Cleans free text and file names before they are stored. */
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
}
