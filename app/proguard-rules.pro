# CubeLens ProGuard rules

# Keep line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlin
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings { <fields>; }

# Coroutines
-dontwarn kotlinx.coroutines.**

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Compose — keep runtime stability
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# Kociemba solver & pruning tables (reflection-safe)
-keep class com.cubelens.solver.** { *; }

# DataStore / preferences
-keep class com.cubelens.data.** { *; }

# CameraX
-dontwarn androidx.camera.**

# Keep BuildConfig
-keep class com.cubelens.BuildConfig { *; }
