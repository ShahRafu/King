# ProGuard rules placeholder for King Assistant
# Keep Kotlin metadata and common Compose classes to avoid R8 issues.
-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
