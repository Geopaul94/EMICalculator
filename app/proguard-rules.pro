# ─────────────────────────────────────────────────────────────────────────────
#  ProGuard Rules
#
#  ProGuard runs during a release build. It does three things:
#    1. Minify  — renames classes/methods to short names (a, b, c …) → smaller APK
#    2. Shrink  — removes code that is never called
#    3. Obfuscate — makes it harder for others to reverse-engineer your app
#
#  Sometimes ProGuard removes things your app actually needs at runtime.
#  You add "-keep" rules here to protect those classes.
# ─────────────────────────────────────────────────────────────────────────────

# Keep ViewModel classes (they are referenced by name internally by Compose)
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep data classes used in StateFlow (Kotlin reflection reads field names)
-keep class com.example.emicalculator.model.** { *; }

# Kotlin serialization / coroutines internals
-keepattributes *Annotation*
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }
