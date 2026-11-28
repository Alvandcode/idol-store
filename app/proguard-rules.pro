# Idol Store — قوانین R8 برای ریلیز فشرده

-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations

# ---------- OkHttp / Okio ----------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.*

# ---------- kotlinx.serialization ----------
# سریالایزرهای تولیدشده باید حفظ شوند
-keep,includedescriptorclasses class com.idol.store.**$$serializer { *; }
-keepclassmembers class com.idol.store.** {
    *** Companion;
}
-keepclasseswithmembers class com.idol.store.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ---------- Coil ----------
-dontwarn coil.**

# ---------- security-crypto / WorkManager ----------
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.**

# ---------- Tink (security-crypto) ----------
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.crypto.tink.**
