import java.util.Properties

// Signing credentials live in local.properties (gitignored — never committed).
// ⚠️  IMPORTANT: Back up loansolver-release.jks + these passwords safely.
//     Losing them means you can NEVER update the app on the Play Store.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }
        ?.inputStream()?.use { load(it) }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace  = "com.loansolver.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.loansolver.app"
        minSdk        = 26
        targetSdk     = 35
        versionCode   = 1
        versionName   = "1.0.0"
    }

    // ── Signing ───────────────────────────────────────────────────────────────
    signingConfigs {
        create("release") {
            storeFile     = file(localProps.getProperty("KEYSTORE_PATH", ""))
            storePassword = localProps.getProperty("KEYSTORE_PASSWORD", "")
            keyAlias      = localProps.getProperty("KEY_ALIAS", "")
            keyPassword   = localProps.getProperty("KEY_PASSWORD", "")
        }
    }

    // ── Build types ───────────────────────────────────────────────────────────
    buildTypes {
        debug {
            isDebuggable        = true
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }

        release {
            // R8 shrinks, obfuscates, and optimises code → much smaller APK
            isMinifyEnabled   = true
            isShrinkResources = true
            signingConfig     = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // ── AAB splits ────────────────────────────────────────────────────────────
    // Play Store delivers only the slices the user's device actually needs.
    // A user on a 2x-screen phone with English + ARM64 gets a much smaller
    // download than the full AAB — typically 40-60 % smaller.
    bundle {
        language { enableSplit = true }   // only user's language(s)
        density  { enableSplit = true }   // only matching screen density
        abi      { enableSplit = true }   // only matching CPU architecture
    }

    // ── Compiler options ──────────────────────────────────────────────────────
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all",           // optimise interface default methods
            "-opt-in=kotlin.RequiresOptIn"
        )
    }

    buildFeatures {
        compose      = true
        buildConfig  = false   // removes the unused BuildConfig class
        resValues    = false   // we use string resources, not generated res values
    }

    // ── Strip unnecessary packaging metadata ─────────────────────────────────
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/*.kotlin_module",
                "kotlin-tooling-metadata.json",
                "DebugProbesKt.bin"
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}
