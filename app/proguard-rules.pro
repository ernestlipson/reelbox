# Keep TMDB response models intact for kotlinx.serialization.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**

-keepclassmembers class dev.reelbox.data.** {
    *** Companion;
}
-keepclasseswithmembers class dev.reelbox.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class dev.reelbox.data.**$$serializer { *; }
-keepclassmembers class dev.reelbox.data.** {
    <fields>;
}

# Retrofit / OkHttp
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.**
-dontwarn okio.**

# Coil
-dontwarn coil.**
