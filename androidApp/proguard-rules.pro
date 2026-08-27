# MyT release shrinker rules (M14)

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod

# Kotlin serialization
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.myt.**$$serializer { *; }
-keepclassmembers class com.myt.** {
    *** Companion;
}
-keepclasseswithmembers class com.myt.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-dontwarn io.ktor.**
-keep class io.ktor.** { *; }

# SQLDelight
-keep class com.myt.data.local.** { *; }

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Compose / AndroidX
-dontwarn androidx.compose.**
-keep class androidx.lifecycle.** { *; }
