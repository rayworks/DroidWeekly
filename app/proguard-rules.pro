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
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-repackageclasses
-dontskipnonpubliclibraryclassmembers

-dontwarn android.support.**
-dontwarn javax.lang.**
-dontwarn com.google.android.gms.**


# Keep Options:
-keepattributes *Annotation*,EnclosingMethod
-keepattributes JavascriptInterface
-keepattributes Exceptions
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# okhttp
-dontwarn com.squareup.okhttp.**
-dontwarn okio.**
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }

# Dagger
-dontoptimize
-dontpreverify
-dontwarn dagger.internal.codegen.**
-keepclassmembers,allowobfuscation class * {
    @javax.inject.* *;
    @dagger.* *;
    <init>();
}
-keep class dagger.** { *; }
-keep class javax.inject.* { *; }

-keep public class * extends android.app.Activity
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.app.Service
-keep public class * extends android.view.View
-keep public class * extends android.content.ContentProvider
-keep class androidx.compose.**{ *;}

-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.*
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE

