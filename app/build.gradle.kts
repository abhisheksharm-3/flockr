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

fun readVersionProps(): Pair<String, Int> {
    if (versionFile.exists()) {
        val props = versionFile.readText()
            .lines()
            .filter { it.isNotBlank() && !it.trim().startsWith("#") && it.contains("=") }
            .associate {
                val (key, value) = it.split("=", limit = 2)
                key.trim() to value.trim()
            }
        return Pair(
            props["VERSION_NAME"] ?: "1.0.0",
            props["VERSION_CODE"]?.toIntOrNull() ?: 1
        )
    }
    return Pair("1.0.0", 1)
}

val (appVersionName, appVersionCode) = readVersionProps()

android {
    namespace = "in.xroden.flockr"
    compileSdk = 36

    defaultConfig {
        applicationId = "in.xroden.flockr"
        minSdk = 29
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        // Make version available to BuildConfig
        buildConfigField("String", "VERSION_NAME", "\"$appVersionName\"")
        buildConfigField("int", "VERSION_CODE", "$appVersionCode")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            shrinkResources = true
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
    kotlinOptions {
        jvmTarget = "11"
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
    implementation("androidx.compose.material:material-icons-extended:1.7.5")

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.foundation)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Supabase
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.gotrue)
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


    // DataStore
    implementation(libs.datastore.preferences)

    // Coil
    implementation(libs.coil.compose)

    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)

    // Accompanist
    implementation(libs.accompanist.systemuicontroller)

    // Google Fonts
    implementation("androidx.compose.ui:ui-text-google-fonts:1.6.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

// Task to print version name for CI/CD
tasks.register("printVersionName") {
    doLast {
        println(android.defaultConfig.versionName)
    }
}
