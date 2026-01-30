# Flockr ProGuard Rules
# Optimized for production builds with proper obfuscation and minification

# ================================
# Kotlin Serialization
# ================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-dontnote kotlinx.serialization.SerializationKt

-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep generated serializers
-keep,includedescriptorclasses class in.xroden.flockr.**$$serializer { *; }
-keepclassmembers class in.xroden.flockr.** {
    *** Companion;
}
-keepclasseswithmembers class in.xroden.flockr.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep @Serializable classes
-keep @kotlinx.serialization.Serializable class in.xroden.flockr.** { *; }

# ================================
# Supabase SDK
# ================================
-keep class io.github.jan.supabase.** { *; }
-keep interface io.github.jan.supabase.** { *; }
-keepclassmembers class io.github.jan.supabase.** { *; }

# Supabase plugins
-keep class io.github.jan.supabase.auth.** { *; }
-keep class io.github.jan.supabase.postgrest.** { *; }
-keep class io.github.jan.supabase.storage.** { *; }
-keep class io.github.jan.supabase.realtime.** { *; }
-keep class io.github.jan.supabase.functions.** { *; }

# ================================
# Ktor HTTP Client
# ================================
-keep class io.ktor.** { *; }
-keep interface io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }

-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Ktor client engines
-keep class io.ktor.client.engine.okhttp.** { *; }
-keep class io.ktor.client.plugins.** { *; }

# ================================
# App Data Models
# ================================
# Keep all data classes
-keep class in.xroden.flockr.**.model.** { *; }
-keep class in.xroden.flockr.**.dto.** { *; }

# Keep enum classes
-keepclassmembers enum in.xroden.flockr.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep data enums
-keep enum in.xroden.flockr.data.enums.** { *; }

# ================================
# Hilt Dependency Injection
# ================================
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# ================================
# Jetpack Compose
# ================================
-keep class androidx.compose.** { *; }
-keepclassmembers class androidx.compose.** { *; }

# ================================
# Coroutines
# ================================
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ================================
# Google Credential Manager
# ================================
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

# ================================
# Google Maps
# ================================
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }

# ================================
# DataStore
# ================================
-keep class androidx.datastore.** { *; }

# ================================
# Coil Image Loading
# ================================
-keep class coil.** { *; }

# ================================
# Biometric Authentication
# ================================
-keep class androidx.biometric.** { *; }

# ================================
# General Android
# ================================
-keepattributes SourceFile,LineNumberTable
-keepattributes Signature
-keepattributes Exceptions

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ================================
# Remove Logging in Release
# ================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# ================================
# Optimization Flags
# ================================
-optimizationpasses 5
-dontpreverify
-repackageclasses ''
-allowaccessmodification
-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*

