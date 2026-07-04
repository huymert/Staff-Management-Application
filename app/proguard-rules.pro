# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===== Firebase =====
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# ===== Firestore model classes =====
-keep class com.example.staff_management.models.** { *; }

# ===== Glide =====
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule { <init>(...); }
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# ===== OkHttp =====
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ===== Gson =====
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ===== MPAndroidChart =====
-keep class com.github.mikephil.charting.** { *; }

# ===== Google Generative AI =====
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**

# ===== AndroidX =====
-keep class androidx.** { *; }
-dontwarn androidx.**

# ===== Guava =====
-dontwarn com.google.common.**
-dontwarn com.google.errorprone.**
-dontwarn javax.annotation.**
