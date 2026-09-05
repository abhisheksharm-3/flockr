import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

// Read version from properties file
val versionFile = file("$rootDir/version.properties")

fun readVersionName(): String {
    if (versionFile.exists()) {
        val props = versionFile.readText()
            .lines()
            .filter { it.isNotBlank() && !it.trim().startsWith("#") && it.contains("=") }
            .associate {
                val (key, value) = it.split("=", limit = 2)
                // Strip any trailing inline comment (e.g. the release-please annotation).
                key.trim() to value.substringBefore("#").trim()
            }
        return props["VERSION_NAME"] ?: "1.0.0"
    }
    return "1.0.0"
}

// versionCode is derived from the semantic version so it always increases monotonically
// and VERSION_NAME stays the single source of truth (managed by release-please).
fun versionNameToCode(name: String): Int {
    val parts = name.split(".").map { it.toIntOrNull() ?: 0 }
    return parts.getOrElse(0) { 0 } * 10000 + parts.getOrElse(1) { 0 } * 100 + parts.getOrElse(2) { 0 }
}

val appVersionName = readVersionName()
val appVersionCode = versionNameToCode(appVersionName)

android {
    namespace = "in.xroden.flockr"
    compileSdk = 36

    // Read Google Client ID from local.properties
    val localProperties = Properties()
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { localProperties.load(it) }
    }
    val googleClientId = localProperties.getProperty("GOOGLE_CLIENT_ID", "")

    defaultConfig {
        applicationId = "in.xroden.flockr"
        minSdk = 34
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        // Make version available to BuildConfig
        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")
        buildConfigField("int", "VERSION_CODE", "$appVersionCode")
        
        // Google Sign-In Client ID from local.properties
        buildConfigField("String", "GOOGLE_CLIENT_ID", "\"$googleClientId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.time.ExperimentalTime"
            )
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.graphics)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.lifecycle.viewmodel.compose)

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.realtime)
    implementation(libs.supabase.functions)

    // Ktor
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.utils)

    // Navigation
    implementation(libs.navigation.compose)

    // Lifecycle & ViewModel
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    // Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)


    // DataStore
    implementation(libs.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.androidx.ui.text.google.fonts)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.biometric)
}

// Task to print version name for CI/CD
tasks.register("printVersionName") {
    doLast {
        println(android.defaultConfig.versionName)
    }
}
