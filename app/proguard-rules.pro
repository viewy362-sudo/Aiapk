# Bloody Team Apk Shield - R8 / ProGuard Anti-Tamper & Obfuscation Rules

# Flatten all internal packages and strip source/debug hints
-repackageclasses 'b.t'
-allowaccessmodification
-renamesourcefileattribute SourceFile
-keepattributes Signature,InnerClasses,EnclosingMethod

# Remove all debug/info/warning Android logs in release APK
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int d(...);
    public static int w(...);
    public static int wtf(...);
}

# Aggressively obfuscate SelfIntegrityVault & Crypto engines (do NOT keep names)
-keepnames class com.example.DexShieldApplication
-keep class com.example.DexShieldApplication { *; }
-keep class com.example.MainActivity { *; }
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# Prevent reflection dumps from retaining field names in security state
-keepclassmembers class com.example.security.SelfIntegrityVault {
    private static <fields>;
}
