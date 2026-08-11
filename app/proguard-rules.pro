# ProGuard / R8 rules tuned for Jetpack Compose and AndroidX Room.
# Adjust / shrink further only after verifying release build & testing.

########################################
# Retain Kotlin metadata & annotations
########################################
-keepattributes Signature,InnerClasses,*Annotation*
-keepclassmembers class kotlin.Metadata { *; }
-keep class kotlin.Metadata { *; }

########################################
# Jetpack Compose - keep runtime & UI entry points
# (Keep broadly to avoid accidental removal of compiler-injected classes)
########################################
-keep class androidx.compose.** { *; }
-keep class com.google.android.material.compose.** { *; }
-dontwarn androidx.compose.**
-dontwarn com.google.android.material.compose.**

# Keep Compose UI Tooling classes (optional for non-release debug tooling)
-keep class androidx.compose.ui.tooling.** { *; }

# Keep Compose compiler-injected lambda/functional types used by UI
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

########################################
# AndroidX Room - keep annotated entities/DAOs and generated classes
########################################
-keep class androidx.room.RoomDatabase { *; }
-keep class androidx.room.migration.** { *; }
-keep class androidx.room.util.** { *; }

# Keep classes annotated with Room annotations (Entity, Dao, Database)
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# Keep Room-generated implementation classes and their members (use wildcard)
-keep class *$$Room* { *; }
-keepclassmembers class * {
    @androidx.room.* <fields>;
    @androidx.room.* <methods>;
}
-dontwarn androidx.room.**

########################################
# Kotlin / reflection / synthetic access
########################################
# Keep data class metadata (used by reflection and some libs)
-keepclassmembers class * {
    @kotlin.Metadata *;
}

########################################
# Keep Parcelize (kotlinx.android.parcel) annotated classes
########################################
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

########################################
# Logging & debug helpers: keep if used by reflection (reduce risk)
-dontwarn org.jetbrains.**
-dontwarn kotlin.**
-dontwarn kotlinx.coroutines.**

########################################
# Keep generated binding classes (if using ViewBinding / DataBinding)
-keep class **.databinding.* { *; }
-keep class **.databinding.*Impl { *; }
-keep class **.binding.* { *; }

########################################
# Optional: Glide/Picasso hooks (uncomment if used)
#-keep public class * implements com.bumptech.glide.module.GlideModule
#-keep public class * implements com.bumptech.glide.module.AppGlideModule
