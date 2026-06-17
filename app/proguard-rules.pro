# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.example.storagecleanup.**$$serializer { *; }
-keepclassmembers class com.example.storagecleanup.** { *** Companion; }
-keepclasseswithmembers class com.example.storagecleanup.** { kotlinx.serialization.KSerializer serializer(...); }
