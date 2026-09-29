plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // IMPORTANT: keep YOUR existing package name here (must match your MainActivity's package)
    namespace = "com.example.minimallauncher"
    compileSdk = 34

    defaultConfig {
        // IMPORTANT: keep YOUR existing applicationId
        applicationId = "com.example.minimallauncher"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // ---- THE FIX: Java and Kotlin must both target 17 ----
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
