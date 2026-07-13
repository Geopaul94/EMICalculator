# ── Readable crash reports ────────────────────────────────────────────────────
# Keep file/line info so Play Console stack traces map back to source,
# while hiding the original file path.
-keepattributes SourceFile, LineNumberTable
-renamesourcefileattribute SourceFile

# ── No manual -keep rules on purpose ─────────────────────────────────────────
# AndroidX libraries (Compose, Lifecycle/ViewModel, FileProvider, SplashScreen)
# ship their own consumer R8 rules inside their AARs. Broad keeps like
# `-keep class androidx.compose.runtime.** { *; }` disable shrinking and
# obfuscation for whole libraries and inflate the APK — never add them back
# unless a specific release crash proves a rule is missing.
# Our own code uses no reflection or serialization, so nothing to keep there.

# ── Remove all debug/verbose logging in release ───────────────────────────────
# Shrinks the binary and prevents internal info from leaking.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
