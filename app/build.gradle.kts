// ─────────────────────────────────────────────────────────────────────────────
//  app/build.gradle.kts  —  Module-level build configuration
//
//  This file tells Gradle:
//    • What Android features to enable (Compose, etc.)
//    • What the app's identity is (applicationId, version, SDK targets)
//    • What libraries the app depends on
// ─────────────────────────────────────────────────────────────────────────────

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace   = "com.example.emicalculator"   // Must match your package name
    compileSdk  = 35                             // The SDK version used to compile

    defaultConfig {
        applicationId  = "com.example.emicalculator"  // MUST be unique on Play Store
        minSdk         = 26                            // Lowest Android version supported (~95% of devices)
        targetSdk      = 35                            // Android version you designed for
        versionCode    = 1                             // Integer: increment by 1 for every Play Store upload
        versionName    = "1.0.0"                       // Human-readable version shown on Play Store
    }

    // ── Signing ───────────────────────────────────────────────────────────────
    // Before uploading to Play Store you need a keystore.
    // Steps:
    //   1. In Android Studio → Build → Generate Signed Bundle/APK
    //   2. Create a new keystore (keep the file + passwords safe!)
    //   3. Uncomment and fill in the block below for automated release builds.
    //
    // signingConfigs {
    //     create("release") {
    //         storeFile     = file("../keystore/my-release-key.jks")
    //         storePassword = System.getenv("KEYSTORE_PASSWORD")   // use env vars, not hardcoded!
    //         keyAlias      = System.getenv("KEY_ALIAS")
    //         keyPassword   = System.getenv("KEY_PASSWORD")
    //     }
    // }

    buildTypes {
        // Debug build — for development. NOT for Play Store.
        debug {
            isDebuggable        = true
            applicationIdSuffix = ".debug"        // Lets you install debug + release side by side
            versionNameSuffix   = "-debug"
        }

        // Release build — this is what you upload to the Play Store.
        release {
            isMinifyEnabled    = true             // Shrinks & obfuscates code (smaller APK)
            isShrinkResources  = true             // Removes unused images/strings
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("release")  // Uncomment after keystore setup
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
        compose = true   // Enable Jetpack Compose
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))   // BOM manages all Compose versions for you
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    // ViewModel — lets our EMIViewModel survive screen rotations
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
