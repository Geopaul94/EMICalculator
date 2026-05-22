# ── Kotlin ────────────────────────────────────────────────────────────────────
-dontwarn kotlin.**
-keepattributes *Annotation*, InnerClasses, Signature, SourceFile, LineNumberTable

# ── Jetpack Compose ───────────────────────────────────────────────────────────
# The Compose compiler plugin handles most rules automatically.
# We only protect the runtime internals that R8 can incorrectly strip.
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ── ViewModel ─────────────────────────────────────────────────────────────────
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# ── Our model (StateFlow holds data classes — field names must survive R8) ────
-keep class com.loansolver.app.model.** { *; }

# ── Coroutines ────────────────────────────────────────────────────────────────
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# ── FileProvider (used for PDF sharing) ──────────────────────────────────────
-keep class androidx.core.content.FileProvider { *; }

# ── Splash screen ─────────────────────────────────────────────────────────────
-keep class androidx.core.splashscreen.** { *; }

# ── Remove all debug/verbose logging in release ───────────────────────────────
# This shrinks the binary and prevents internal info from leaking.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
