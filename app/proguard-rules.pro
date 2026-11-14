# DexShield R8 / ProGuard Hardening Configuration
# Compatible with Dalvik (API 24+), ART, and Sketchware Pro / AGP toolchains

# Aggressive package flattening & metadata stripping
-repackageclasses 'o'
-allowaccessmodification
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod

# Strip debug/verbose logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
    public static int w(...);
}

# Preserve Compose & Android framework entry points
-keep class com.example.DexShieldApplication { *; }
-keep class com.example.MainActivity { *; }
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
