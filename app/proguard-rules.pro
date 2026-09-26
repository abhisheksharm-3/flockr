# Release shrinking. Every library Flockr uses ships its own consumer rules (kotlinx.serialization,
# Supabase, Ktor, Hilt, Compose, Coil, MapLibre, Firebase), so this file only adds what the app
# itself needs. A rule belongs here only with the crash or R8 error that proved it necessary.

# Crashlytics stack traces: keep line numbers, and hide the real file names behind one constant.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Optional classes Ktor and Supabase reference but Android never loads.
-dontwarn org.slf4j.**
-dontwarn java.lang.management.**
