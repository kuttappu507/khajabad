plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Events feed (the Notte function powering the website).
// Lookup order: env var > local.properties > notte.properties > fallback.
fun localOrEnv(key: String, fallback: String = ""): String {
    // Reading plain key=value files with the Kotlin stdlib keeps this script free
    // of java.* references (the Kotlin DSL already binds `java` to an extension).
    System.getenv(key)?.takeIf { it.isNotBlank() }?.let { return it }
    listOf("local.properties", "notte.properties").forEach { name ->
        val file = rootProject.file(name)
        if (file.exists()) {
            val value = file.readLines()
                .map { it.trim() }
                .firstOrNull { it.startsWith("$key=") || it.startsWith("$key =") }
                ?.substringAfter('=')
                ?.trim()
            if (!value.isNullOrBlank()) return value
        }
    }
    return fallback
}

android {
    namespace = "com.khajabad.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.khajabad.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "1.1.0"
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "NOTTE_API_KEY", "\"" + localOrEnv("NOTTE_API_KEY") + "\"")
        buildConfigField("String", "NOTTE_FUNCTION_ID", "\"" + localOrEnv("NOTTE_FUNCTION_ID", "635cfdc6-667e-453c-bc57-bce54f35724b") + "\"")
        buildConfigField("String", "NOTTE_BASE_URL", "\"" + localOrEnv("NOTTE_BASE_URL", "https://us-prod.notte.cc") + "\"")
    }

    signingConfigs {
        create("release") {
            // Stable, committed signing key so every CI run installs over the previous one.
            storeFile = file("keystore.p12")
            storeType = "PKCS12"
            storePassword = "khajabad"
            keyAlias = "khajabad"
            keyPassword = "khajabad"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Keep debug installs compatible with the same package name.
            applicationIdSuffix = ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        noCompress += listOf("pdf")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
