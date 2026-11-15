package com.example.i18n

import com.example.security.ProtectionProfile
import com.example.security.WallProtectionTier

enum class FeatureCategory(
    val en: String,
    val bn: String,
    val hi: String,
    val ar: String,
    val id: String,
    val es: String
) {
    CORE_SHIELD("Core Shields", "কোর শিল্ড", "कोर शील्ड", "الدروع الأساسية", "Perisai Inti", "Escudos Core"),
    WALL_TIER("Wall Tiers", "ওয়াল টিয়ার", "वॉल टियर", "مستويات الجدار", "Level Wall", "Niveles Wall"),
    RESOURCES("Resources", "রিসোর্স", "रिसोर्स", "الموارد", "Sumber Daya", "Recursos"),
    CONFIGURATION("Configuration", "কনফিগারেশন", "कॉन्फ़िगरेशन", "الإعدادات", "Konfigurasi", "Configuración"),
    ADVANCED("Advanced", "অ্যাডভান্সড", "एडवांस्ड", "متقدم", "Lanjutan", "Avanzado"),
    STUDIO_TOOLS("Lab & Tools", "টুলস ও ল্যাব", "टूल्स और लैब", "الأدوات", "Alat & Lab", "Herramientas");

    fun label(lang: AppLanguage): String = when (lang) {
        AppLanguage.ENGLISH -> en
        AppLanguage.BENGALI -> bn
        AppLanguage.HINDI -> hi
        AppLanguage.ARABIC -> ar
        AppLanguage.INDONESIAN -> id
        AppLanguage.SPANISH -> es
    }
}

data class FeatureExplanation(
    val title: String,
    val whatItDoes: String,
    val howItWorks: String,
    val whenToUse: String
)

data class FeatureGuideSpec(
    val id: String,
    val category: FeatureCategory,
    val defaultTitle: String,
    val badge: String? = null,
    val isChecked: ((ProtectionProfile) -> Boolean)? = null,
    val applyToggle: ((ProtectionProfile, Boolean) -> ProtectionProfile)? = null,
    val explain: (AppLanguage) -> FeatureExplanation
)

internal fun tr(
    lang: AppLanguage,
    title: String,
    en: Triple<String, String, String>,
    bn: Triple<String, String, String>,
    hi: Triple<String, String, String>,
    ar: Triple<String, String, String>? = null,
    id: Triple<String, String, String>? = null,
    es: Triple<String, String, String>? = null
): FeatureExplanation {
    val t = when (lang) {
        AppLanguage.ENGLISH -> en
        AppLanguage.BENGALI -> bn
        AppLanguage.HINDI -> hi
        AppLanguage.ARABIC -> ar ?: Triple(
            "${en.first}",
            "${en.second}",
            "${en.third}"
        )
        AppLanguage.INDONESIAN -> id ?: en
        AppLanguage.SPANISH -> es ?: en
    }
    return FeatureExplanation(
        title = title,
        whatItDoes = t.first,
        howItWorks = t.second,
        whenToUse = t.third
    )
}

object FeatureIds {
    const val DEX_PACKING = "dex_packing"
    const val NATIVE_JNI_STUB = "native_jni_stub"
    const val ASSET_AES = "asset_aes"
    const val ANTI_TAMPER = "anti_tamper"
    const val ANTI_DEBUG_FRIDA = "anti_debug_frida"
    const val STRING_OBFUSCATE = "string_obfuscate"
    const val ZERO_CRASH = "zero_crash"
    const val ROOT_SHIELD = "root_shield"
    const val EMULATOR_SHIELD = "emulator_shield"
    const val SSL_VPN_BLOCK = "ssl_vpn_block"
    const val SCREEN_TAPJACKING = "screen_tapjacking"
    const val SIGN_OUTPUT_APK = "sign_output_apk"
    const val DISGUISE_FORMAT = "disguise_format"
    const val SMART_AUTO = "smart_auto"
    const val PIPELINE_FORGE = "pipeline_forge"
    const val DEX_PACKER_LAB = "dex_packer_lab"
    const val LIVE_AUDIT = "live_audit"
    const val ASSET_VAULT_TOOL = "asset_vault_tool"
    const val SKETCHWARE_KIT = "sketchware_kit"
    const val STRING_CRASH_LAB = "string_crash_lab"
}

object FeatureGuideCatalog {

    val allFeatures: List<FeatureGuideSpec> by lazy {
        coreAndQuickFeatures + wallTierFeatures + FeatureCatalogExtended.resourceFeatures +
            FeatureCatalogExtended.configurationAndAdvancedFeatures + FeatureCatalogExtended.studioToolFeatures
    }

    fun findById(id: String): FeatureGuideSpec? = allFeatures.firstOrNull { it.id == id }

    private val coreAndQuickFeatures: List<FeatureGuideSpec> = listOf(
        FeatureGuideSpec(
            id = FeatureIds.DEX_PACKING,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "1. DEX Packing & 65K Method Guard",
            badge = "CORE",
            isChecked = { it.dexBytecodeGuard },
            applyToggle = { p, v -> p.copy(dexBytecodeGuard = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "1. DEX Packing & 65K Method Guard",
                    en = Triple(
                        "Hides and encrypts all classes*.dex Dalvik bytecode files so tools like JADX, Apktool, or MT Manager cannot read your Java/Kotlin source code.",
                        "At build time, every classes*.dex is encrypted with PBKDF2-AES-256 + XOR masking and disguised as a .png/.dat/.bin asset inside assets/bloody_wall/. At runtime, the compiled DEX 035 Stub decrypts them straight into RAM using InMemoryDexClassLoader.",
                        "Ideal for Sketchware Pro & standard Java/Kotlin APKs. Keep OFF for Flutter/Unity/already-packed APKs (Smart Auto disables this automatically to prevent crashes)."
                    ),
                    bn = Triple(
                        "অ্যাপের সব classes*.dex ফাইল এনক্রিপ্ট করে লুকিয়ে রাখে যাতে JADX, Apktool বা MT Manager দিয়ে কেউ আপনার Java/Kotlin কোড চুরি বা ডিকম্পাইল করতে না পারে।",
                        "বিল্ড করার সময় প্রতিটি classes*.dex কে AES-256 ও XOR দিয়ে এনক্রিপ্ট করে .png/.dat/.bin নামে assets/bloody_wall/ ফোল্ডারে লুকানো হয়। অ্যাপ খোলার সাথে সাথে Stub সরাসরি RAM-এ ডিক্রিপ্ট করে InMemoryDexClassLoader দিয়ে অ্যাপ চালায় (ফোনের স্টোরেজে কখনোই আনলক ফাইল সেভ হয় না)।",
                        "Sketchware Pro এবং সাধারণ Android অ্যাপের জন্য সেরা। কিন্তু Flutter, Unity বা আগে থেকেই প্যাক করা APK হলে এটি বন্ধ রাখুন (স্মার্ট অটো নিজে থেকেই ক্র্যাশ ঠেকাতে এটি অফ করে দেয়)।"
                    ),
                    hi = Triple(
                        "ऐप की सभी classes*.dex फ़ाइलों को एन्क्रिप्ट करके छिपाता है ताकि JADX, Apktool या MT Manager से कोई आपका कोड न पढ़ सके।",
                        "बिल्ड के समय हर classes*.dex को AES-256 और XOR से एन्क्रिप्ट करके .png/.dat/.bin रूप में assets/bloody_wall/ में रखा जाता है। रनटाइम पर Stub सीधे RAM में डिक्रिप्ट करके InMemoryDexClassLoader से ऐप चलाता है।",
                        "Sketchware Pro और सामान्य Java/Kotlin APK के लिए बेहतरीन है। Flutter/Unity या पहले से पैक APK के लिए इसे बंद रखें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.NATIVE_JNI_STUB,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "Native C++ JNI + InMemoryDexClassLoader Stub",
            badge = "VIP",
            isChecked = { it.nativeJniStubPacking },
            applyToggle = { p, v -> p.copy(nativeJniStubPacking = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Native C++ JNI + Stub Loader",
                    en = Triple(
                        "Replaces the launcher entry with BloodyStubApplication in AndroidManifest.xml and exports a custom NDK C++ JNI bridge + CMake script in the Developer Kit.",
                        "Patches binary AXML AndroidManifest.xml cleanly (preserving package & original Application class), injects the compiled DEX 035 loader, and hands execution back to your original Application after RAM loading.",
                        "Enable together with DEX Packing for full DEX replacement. When OFF, Bloody Team uses a safe Wrapper Stub (classesN.dex) so runtime shields still work without replacing classes.dex."
                    ),
                    bn = Triple(
                        "AndroidManifest.xml-এ লঞ্চার অ্যাপ্লিকেশন ক্লাস হিসেবে BloodyStubApplication সেট করে এবং NDK C++ JNI ডিক্রিপ্টর + CMake কোড জেনারেট করে।",
                        "বাইনারি AXML ম্যানিফেস্টে আসল Application ক্লাসের নাম সুরক্ষিত রেখে স্টাব ক্লাস বসায়, RAM-এ DEX লোড হওয়ার পর মূল অ্যাপের হাতে নিয়ন্ত্রণ ফিরিয়ে দেয় ও PathClassLoader ঠিক রাখে যাতে .so লাইব্রেরি ক্র্যাশ না করে।",
                        "ফুল DEX প্যাকিংয়ের জন্য DEX Packing-এর সাথে চালু করুন। এটি বন্ধ রাখলেও ক্র্যাশ-সেফ Wrapper Stub (classesN.dex) হিসেবে বাকি সিকিউরিটি গার্ড কাজ করতে পারে।"
                    ),
                    hi = Triple(
                        "AndroidManifest.xml में BloodyStubApplication लोडर जोड़ता है और डेवलपर किट में NDK C++ JNI डिक्रिप्टर कोड बनाता है।",
                        "यह बाइनरी AXML मैनिफेस्ट को सुरक्षित पैच करके असली Application क्लास का नाम बचाए रखता है और RAM में DEX लोड होने के बाद मूल ऐप को नियंत्रण सौंप देता है।",
                        "पूर्ण DEX रिप्लेसमेंट के लिए DEX Packing के साथ ऑन करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.ASSET_AES,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "2. Asset & Resource AES-256-GCM",
            badge = "SAFE",
            isChecked = { it.assetAes256Encryption },
            applyToggle = { p, v -> p.copy(assetAes256Encryption = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "2. Asset & Data File AES-256-GCM Encryption",
                    en = Triple(
                        "Encrypts sensitive files inside assets/ (JSON, config, custom data) with authenticated AES-256-GCM (DXS1 format) and creates AssetVaultHelper.java.",
                        "Uses PBKDF2-HMAC-SHA256 (16,384 rounds) + 12-byte random IV + 128-bit GCM auth tag. Any modified byte causes decryption rejection. Original APK assets stay untouched inside signed APK unless decrypted via AssetVaultHelper.",
                        "Turn ON when your app has JSON configs, custom scripts, or game data files. Turn OFF if your APK has no sensitive assets or loads raw WebView/media files directly without AssetVaultHelper."
                    ),
                    bn = Triple(
                        "অ্যাপের assets/ ফোল্ডারের JSON, কনফিগ ও ডেটা ফাইলগুলোকে AES-256-GCM দিয়ে এনক্রিপ্ট করে এবং ডিক্রিপ্ট করার জন্য AssetVaultHelper.java কোড দেয়।",
                        "১৬,৩৮৪ রাউন্ড PBKDF2-HMAC-SHA256 কী, র‍্যান্ডম IV ও 128-বিট GCM ট্যাগ ব্যবহার করে ডেটা প্রটেক্ট করে। ফলে কিটের protected_assets/-এ এনক্রিপ্টেড ফাইল তৈরি হয় এবং APK ক্র্যাশ না করিয়েই ডেটা সুরক্ষিত থাকে।",
                        "অ্যাপে JSON বা সংবেদনশীল ডেটা ফাইল থাকলে ON করুন। কোনো অ্যাসেট না থাকলে বা অ্যাসেট সরাসরি WebView/Media-তে চললে ইউজার চাইলে এটি OFF রাখতে পারেন।"
                    ),
                    hi = Triple(
                        "ऐप के assets/ फ़ोल्डर की JSON, कॉन्फ़िग और डेटा फ़ाइलों को AES-256-GCM से एन्क्रिप्ट करता है और AssetVaultHelper.java बनाता है।",
                        "यह 16,384 राउंड PBKDF2-HMAC-SHA256 और 128-बिट GCM ऑथ टैग का उपयोग करता है ताकि डेटा से छेड़छाड़ न हो सके।",
                        "संवेदनशील डेटा फ़ाइलें होने पर ON करें; यदि ऐप की फ़ाइलें सीधे WebView/मीडिया से लोड होती हैं तो OFF रख सकते हैं।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.ANTI_TAMPER,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "3. Anti-Tamper & Signature Lock",
            badge = "SAFE",
            isChecked = { it.antiTamperSignature },
            applyToggle = { p, v -> p.copy(antiTamperSignature = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "3. Anti-Tamper & Signature Lock",
                    en = Triple(
                        "Verifies the app's signing certificate SHA-256 fingerprint at startup to block modded, cloned, or re-signed APKs.",
                        "Compares PackageManager GET_SIGNING_CERTIFICATES (and legacy GET_SIGNATURES fallback) against the expected SHA-256 pinned in protection_policy.json. If mismatched, blocks execution with a clean Shield dialog.",
                        "Safe for almost every APK (including Sketchware). Auto-pinned to the output signing key during APK Forge."
                    ),
                    bn = Triple(
                        "অ্যাপ চালুর সময় সিগনেচার সার্টিফিকেট SHA-256 যাচাই করে যাতে কেউ আপনার APK এডিট/মড করে রিসাইন করলে অ্যাপ চলতে না পারে।",
                        "Android PackageManager থেকে আসল সার্টিফিকেট SHA-256 নিয়ে protection_policy.json-এ থাকা পিন করা SHA-256 এর সাথে মেলায়। মিল না থাকলে মড করা APK ব্লক করে দেয়।",
                        "Sketchware সহ প্রায় সব অ্যাপেই ১০০% সেফ ও রিকমেন্ডেড।"
                    ),
                    hi = Triple(
                        "ऐप खुलते समय साइन सर्टिफिकेट SHA-256 की जांच करता है ताकि कोई आपका APK मॉड या री-साइन न कर सके।",
                        "यह PackageManager से सर्टिफिकेट फिंगरप्रिंट निकालकर policy से मिलाता है और बदलाव मिलने पर ऐप को ब्लॉक कर देता है।",
                        "लगभग सभी Sketchware और Android APK के लिए सुरक्षित है।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.ANTI_DEBUG_FRIDA,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "4. Anti-Debug & Frida / Xposed Shield",
            badge = "VIP",
            isChecked = { it.antiDebugFridaShield },
            applyToggle = { p, v -> p.copy(antiDebugFridaShield = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "4. Anti-Debug & Frida / Xposed Shield",
                    en = Triple(
                        "Detects attached Java/native debuggers, Frida instrumentation servers, Xposed, and LSPosed hooking frameworks.",
                        "Checks Debug.isDebuggerConnected(), /proc/self/status TracerPid, /proc/self/maps for frida/xposed/lsposed modules, and local Frida ports (27042).",
                        "Use on production release APKs. Keep OFF while testing with Android Studio Debugger or Xposed-enabled test devices."
                    ),
                    bn = Triple(
                        "অ্যাপে ডিবাগার লাগানো আছে কি না এবং Frida, Xposed বা LSPosed দিয়ে কেউ হুক করছে কি না তা ধরে ফেলে।",
                        "/proc/self/status-এর TracerPid, /proc/self/maps মেমোরি ম্যাপ ও লোকাল ফ্রিডা পোর্ট স্ক্যান করে হুকিং টুল পেলে সাথে সাথে প্রোটেকশন ট্রিগার করে।",
                        "রিলিজ APK-তে চালু রাখুন। তবে নিজে ডিবাগার বা Xposed দিয়ে টেস্ট করার সময় বন্ধ রাখুন।"
                    ),
                    hi = Triple(
                        "डिबगर, Frida, Xposed या LSPosed जैसे हुकिंग फ्रेमवर्क का पता लगाकर ऐप की सुरक्षा करता है।",
                        "यह TracerPid, /proc/self/maps और Frida पोर्ट स्कैन करके हुकिंग का पता लगाता है।",
                        "पब्लिक रिलीज़ APK के लिए ऑन करें; टेस्टिंग डिबगर के समय बंद रखें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.STRING_OBFUSCATE,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "5. String & API Key Obfuscation",
            badge = "SAFE",
            isChecked = { it.stringMetadataObfuscation },
            applyToggle = { p, v -> p.copy(stringMetadataObfuscation = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "5. String & API Key Obfuscation",
                    en = Triple(
                        "Eliminates plaintext API keys, URLs, and secrets from compiled code by generating an encrypted byte-rotated StringVault.",
                        "Applies XOR + nibble-rotation + random salt across your secret string and exports StringVault.java (plus encrypted vault_config.enc in APK) so 'strings' or JADX cannot see plaintext.",
                        "100% safe for all apps including Sketchware Pro. Zero crash risk."
                    ),
                    bn = Triple(
                        "আপনার API Key, গোপন URL বা টোকেন প্লেইনটেক্সট হিসেবে কোডে না রেখে বাইট-রোটেটেড StringVault-এ লুকিয়ে ফেলে।",
                        "র‍্যান্ডম সল্ট, XOR ও বিট রোটেশন ব্যবহার করে StringVault.java এবং APK-র ভেতরে vault_config.enc তৈরি করে, ফলে JADX বা strings টুল দিয়ে সার্চ করেও সিক্রেট পাওয়া যায় না।",
                        "Sketchware ও সব অ্যাপের জন্য ১০০% সেফ—কোনো ক্র্যাশ ঝুঁকি নেই।"
                    ),
                    hi = Triple(
                        "आपकी API Key और सीक्रेट URL को सादे टेक्स्ट से हटाकर एन्क्रिप्टेड StringVault में छिपाता है।",
                        "यह रैंडम सॉल्ट और XOR बिट रोटेशन से StringVault.java और vault_config.enc बनाता है।",
                        "सभी ऐप के लिए 100% सुरक्षित है।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.ZERO_CRASH,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "6. Zero-Crash & ANR Exception Guard",
            badge = "SAFE",
            isChecked = { it.zeroCrashExceptionGuard },
            applyToggle = { p, v -> p.copy(zeroCrashExceptionGuard = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "6. Zero-Crash & ANR Exception Guard",
                    en = Triple(
                        "Prevents sudden uncaught background thread crashes from killing the app immediately and logs recovery faults.",
                        "Installs a global Thread.UncaughtExceptionHandler inside the Stub/Kit before handing over to the app, intercepting background worker exceptions safely.",
                        "Recommended ON for almost every APK (especially Sketchware Pro apps that run async network/file code)."
                    ),
                    bn = Triple(
                        "ব্যাকগ্রাউন্ড থ্রেডে হঠাৎ আনকট এক্সেপশন হলে অ্যাপ সাথে সাথে ক্র্যাশ হওয়া থেকে বাঁচায় এবং নিরাপদে রিকভারি করে।",
                        "অ্যাপ চালু হওয়ার আগেই গ্লোবাল UncaughtExceptionHandler বসিয়ে ব্যাকগ্রাউন্ড ক্র্যাশ আটকে দেয় (যদি মূল অ্যাপে নিজস্ব হ্যান্ডলার থাকে তবে সেটিকেও চেইন করে)।",
                        "Sketchware Pro সহ সব অ্যাপের জন্য এটি চালু রাখা নিরাপদ ও অত্যন্ত উপকারী।"
                    ),
                    hi = Triple(
                        "बैकग्राउंड थ्रेड में अचानक आने वाले क्रैश को रोककर ऐप को बंद होने से बचाता है।",
                        "ऐप शुरू होते ही ग्लोबल UncaughtExceptionHandler सेट करता है जो बैकग्राउंड त्रुटियों को सुरक्षित हैंडल करता है।",
                        "Sketchware Pro और सभी Android ऐप्स के लिए अनुशंसित।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.ROOT_SHIELD,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "7. Root & Magisk Environment Shield (Check Root)",
            badge = "VIP",
            isChecked = { it.rootMagiskDetection },
            applyToggle = { p, v -> p.copy(rootMagiskDetection = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "7. Check Root (Root & Magisk Shield)",
                    en = Triple(
                        "Detects rooted devices, Magisk, KernelSU, su binaries, and test-keys builds so crackers cannot dump memory or modify runtime state.",
                        "Scans standard su paths (/system/xbin/su, /sbin/su, /vendor/bin/su), Magisk traces, and Build.TAGS test-keys. Unless 'Hide Root' is enabled, warns/blocks rooted execution.",
                        "Enable when protecting paid injectors/banking/license apps. Do NOT combine with 'Hide Root' unless you intentionally want rooted devices allowed."
                    ),
                    bn = Triple(
                        "ফোন রুট করা বা Magisk / KernelSU ইনস্টল থাকলে তা শনাক্ত করে যাতে রুটেড ডিভাইস থেকে অ্যাপের মেমোরি ডাম্প বা ক্র্যাক না করা যায়।",
                        "su বাইনারি পাথ, Magisk ডিরেক্টরি ও Build.TAGS test-keys স্ক্যান করে। রুট পাওয়া গেলে শিল্ড অ্যালার্ট দেখিয়ে আনঅথরাইজড অ্যাক্সেস আটকায়।",
                        "পেইড বা লাইসেন্সড অ্যাপের জন্য অন করুন। রুটেড ইউজারদের অ্যালাউ করতে চাইলে 'Hide Root' অপশনটি ব্যবহার করুন (দুটি একসাথে অন করলে কনফ্লিক্ট হবে)।"
                    ),
                    hi = Triple(
                        "रूटेड फोन, Magisk या su बाइनरी का पता लगाकर मेमोरी डंप और क्रैकिंग को रोकता है।",
                        "सिस्टम पाथ में su बाइनरी, Magisk और Build.TAGS की जांच करता है।",
                        "पेड या लाइसेंस ऐप्स के लिए ऑन करें; रूटेड डिवाइस को अनुमति देने के लिए Hide Root का उपयोग करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.EMULATOR_SHIELD,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "8. Emulator & Virtual-Clone Blocker (Check Emulator)",
            badge = "VIP",
            isChecked = { it.emulatorCloneShield },
            applyToggle = { p, v -> p.copy(emulatorCloneShield = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "8. Detect / Check Emulator & Clone Blocker",
                    en = Triple(
                        "Detects PC Android emulators (QEMU, BlueStacks, Nox, LDPlayer, Goldfish) and warns against automated reverse-engineering sandboxes.",
                        "Inspects Build.FINGERPRINT, Build.MODEL, Build.HARDWARE, Build.MANUFACTURER, and QEMU/genymotion device properties at runtime.",
                        "Keep OFF if your users intentionally test or run the app inside PC emulators or cloud streaming emulators!"
                    ),
                    bn = Triple(
                        "BlueStacks, LDPlayer, Nox বা QEMU-এর মতো এমুলেটর পরিবেশ শনাক্ত করে অটোমেটেড রিভার্স-ইঞ্জিনিয়ারিং স্যান্ডবক্স আটকায়।",
                        "ডিভাইসের Build.FINGERPRINT, MODEL, HARDWARE এবং goldfish/ranchu/qemu প্রোপার্টি চেক করে এমুলেটর ধরে ফেলে।",
                        "আপনার ইউজাররা যদি PC এমুলেটর বা ব্রাউজার এমুলেটরে অ্যাপ চালাতে চায় তবে এটি OFF রাখুন, নাহলে এমুলেটরে ঢুকতে বাধা দেবে।"
                    ),
                    hi = Triple(
                        "BlueStacks, LDPlayer, Nox या QEMU जैसे एम्युलेटर वातावरण का पता लगाकर रिवर्स-इंजीनियरिंग रोकता है।",
                        "Build.FINGERPRINT, MODEL और HARDWARE प्रॉपर्टी की जांच करता है।",
                        "यदि यूज़र्स PC एम्युलेटर पर ऐप चलाते हैं तो इसे OFF रखें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.SSL_VPN_BLOCK,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "9. SSL Pinning & Check VPN Proxy",
            badge = "VIP",
            isChecked = { it.sslPinningProxyBlock },
            applyToggle = { p, v -> p.copy(sslPinningProxyBlock = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "9. SSL Pinning & Check VPN / Proxy",
                    en = Triple(
                        "Detects active VPN tunnels (tun0, ppp0) and HTTP packet proxies, plus generates SslPinningGuard.java with SHA-256 certificate pinning.",
                        "Enumerates NetworkInterface names and ConnectivityManager VPN transport flags, as well as http.proxyHost/Port properties, blocking MITM traffic interception.",
                        "NEVER enable on VPN/Proxy/Injector apps themselves! Smart Auto turns this OFF automatically if BIND_VPN_SERVICE is detected."
                    ),
                    bn = Triple(
                        "অ্যাক্টিভ VPN (tun0/ppp0) ও HTTP প্রক্সি ব্লক করে এবং সার্ভার API-র জন্য SSL সার্টিফিকেট পিনিং কোড দেয় যাতে কেউ ট্রাফিক স্নিফ করতে না পারে।",
                        "NetworkInterface ও ConnectivityManager দিয়ে VPN এবং সিস্টেম প্রক্সি চেক করে। ধরা পড়লে নেটওয়ার্ক ম্যানিপুলেশন আটকে দেয়।",
                        "সতর্কতা: আপনার অ্যাপ নিজেই যদি VPN বা Proxy অ্যাপ হয় তবে এটি অবশ্যই OFF রাখবেন (স্মার্ট অটো এটি নিজেই শনাক্ত করে)।"
                    ),
                    hi = Triple(
                        "सक्रिय VPN (tun0/ppp0) और HTTP प्रॉक्सी को ब्लॉक करता है तथा SSL पिनिंग सुरक्षा देता है।",
                        "NetworkInterface और सिस्टम प्रॉक्सी सेटिंग्स की जांच करके MITM ट्रैफ़िक रोकता है।",
                        "यदि आपका ऐप खुद VPN ऐप है तो इसे कभी ON न करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.SCREEN_TAPJACKING,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "10. Anti-Screenshot & Tapjacking Shield",
            badge = "VIP",
            isChecked = { it.screenTapjackingShield },
            applyToggle = { p, v -> p.copy(screenTapjackingShield = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "10. Anti-Screenshots & Tapjacking",
                    en = Triple(
                        "Blocks screenshots, screen recording, recent-apps thumbnail leaks, and overlay tapjacking on every Activity.",
                        "Registers ActivityLifecycleCallbacks inside BloodyStubApplication (and DexShieldGuard) to apply WindowManager.LayoutParams.FLAG_SECURE and filter obscured touches automatically.",
                        "100% crash-safe for Sketchware & all apps. Only turn OFF if you want users to be able to take screenshots."
                    ),
                    bn = Triple(
                        "অ্যাপের প্রতিটি স্ক্রিনে স্ক্রিনশট, স্ক্রিন রেকর্ডিং এবং ওভারলে ট্যাপজ্যাকিং আক্রমণ সম্পূর্ণ বন্ধ করে।",
                        "ActivityLifecycleCallbacks ব্যবহার করে প্রতিবার কোনো Activity খোলার সাথে সাথে FLAG_SECURE বসিয়ে দেয় এবং Recent Apps থাম্বনেইল প্রিভিউও লুকিয়ে রাখে।",
                        "Sketchware সহ সব অ্যাপের জন্য ১০০% সেফ। শুধু ইউজারদের স্ক্রিনশট নেওয়ার অনুমতি দিতে চাইলে বন্ধ রাখুন।"
                    ),
                    hi = Triple(
                        "ऐप के हर पेज पर स्क्रीनशॉट, स्क्रीन रिकॉर्डिंग और ओवरले टैपजैकिंग को ब्लॉक करता है।",
                        "ActivityLifecycleCallbacks से हर Activity की विंडो पर FLAG_SECURE सेट कर देता है।",
                        "Sketchware सहित सभी ऐप्स के लिए 100% सुरक्षित है।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.SIGN_OUTPUT_APK,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "Auto-Sign Output APK (v1 JAR + v2 Block)",
            badge = "CORE",
            isChecked = { it.signOutputApk },
            applyToggle = { p, v -> p.copy(signOutputApk = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Auto-Sign Output APK (ZipAlign + v1 + v2)",
                    en = Triple(
                        "Builds a ready-to-install, ZipAligned APK signed with both JAR v1 and APK Signature Scheme v2 using a 2048-bit RSA certificate.",
                        "Streams all entries with 4-byte/16KB alignment, computes SHA-256 digests for MANIFEST.MF, CERT.SF, and CERT.RSA, and injects the Merkle-hashed 'APK Sig Block 42' verified by Android 7–16+.",
                        "Keep ON whenever protecting an .apk so Android allows immediate installation without needing external signers."
                    ),
                    bn = Triple(
                        "সরাসরি ইনস্টল করার উপযোগী ZipAligned APK তৈরি করে যা 2048-বিট RSA সার্টিফিকেট দিয়ে JAR v1 এবং APK Signature Scheme v2 উভয়ভাবেই সাইন করা থাকে।",
                        "স্ট্রিমিং পদ্ধতিতে সব ফাইল সাজিয়ে MANIFEST.MF, CERT.SF, CERT.RSA এবং Android 7–16+ এর জন্য Merkle হ্যাশড APK Sig Block 42 যুক্ত করে।",
                        ".apk ইনপুট দিলে এটি অবশ্যই ON রাখুন যাতে আউটপুট APK সরাসরি ফোনে ইনস্টল করা যায়।"
                    ),
                    hi = Triple(
                        "2048-बिट RSA कुंजी से JAR v1 और APK Signature Scheme v2 के साथ इंस्टॉल करने योग्य ZipAligned APK बनाता है।",
                        "स्ट्रीमिंग द्वारा सभी फाइलें Align करके MANIFEST.MF, CERT.SF, CERT.RSA और APK Sig Block 42 जोड़ता है।",
                        ".apk प्रोटेक्ट करते समय इसे ON रखें ताकि APK सीधे इंस्टॉल हो सके।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.DISGUISE_FORMAT,
            category = FeatureCategory.CORE_SHIELD,
            defaultTitle = "Disguise Vault Format (.png / .dat / .bin)",
            badge = "SAFE",
            explain = { lang ->
                tr(
                    lang,
                    title = "Disguise Vault Extension (.png / .dat / .bin)",
                    en = Triple(
                        "Camouflages encrypted DEX payloads inside assets/bloody_wall/ as harmless image/font/cache files.",
                        "Changes the packed filenames to tex_atlas_N.png, font_metrics_N.dat, or res_cache_N.bin while keeping full authenticated headers for the runtime Stub.",
                        "All 3 formats work identically at runtime—pick whichever blends best with your APK's asset structure."
                    ),
                    bn = Triple(
                        "এনক্রিপ্টেড DEX ফাইলগুলোকে সাধারণ ছবি (.png), ফন্ট ডেটা (.dat) বা ক্যাশ ফাইল (.bin) ছদ্মবেশে assets/bloody_wall/-এ লুকিয়ে রাখে।",
                        "রিভার্স ইঞ্জিনিয়াররা ফাইল দেখে সাধারণ অ্যাসেট মনে করবে, কিন্তু রানটাইম স্টাব সঠিক ফরম্যাট পড়েই RAM-এ ডিক্রিপ্ট করবে।",
                        "তিনটি ফরম্যাটই ১০০% কাজ করে—পছন্দমতো যেকোনো একটি বেছে নিন।"
                    ),
                    hi = Triple(
                        "एन्क्रिप्टेड DEX फ़ाइलों को सामान्य .png, .dat या .bin फ़ाइलों के रूप में assets/bloody_wall/ में छिपाता है।",
                        "रनटाइम पर Stub इन्हें पहचानकर सीधे RAM में डिक्रिप्ट करता है।",
                        "तीनों फ़ॉर्मेट समान रूप से सुरक्षित और कार्यशील हैं।"
                    )
                )
            }
        )
    )

    private val wallTierFeatures: List<FeatureGuideSpec> = WallProtectionTier.entries.map { tier ->
        FeatureGuideSpec(
            id = "wall_${tier.name.lowercase()}",
            category = FeatureCategory.WALL_TIER,
            defaultTitle = tier.title,
            badge = tier.badge,
            isChecked = { it.resolvedWallTier == tier },
            applyToggle = { p, enable ->
                if (!enable) p.copy(wallProtectionTier = "")
                else p.applyWallPreset(tier)
            },
            explain = { lang -> wallTierExplanation(lang, tier) }
        )
    }

    private fun wallTierExplanation(lang: AppLanguage, tier: WallProtectionTier): FeatureExplanation {
        val detailsEn = when (tier) {
            WallProtectionTier.WALL_V1_STANDARD -> Triple(
                "Balanced entry preset enabling Anti-Tamper, Zero-Crash Guard, DEX Guard, and v1/v2 APK Signing.",
                "Applies lightweight DEX bytecode protection without replacing resources or enabling strict root/emulator blocks.",
                "Best for basic Sketchware or utility apps where maximum device compatibility is needed."
            )
            WallProtectionTier.WALL_V2_VAULT -> Triple(
                "Vault-focused preset adding Asset AES-256 Encryption, String Obfuscation, HttpCanary Block & VPN Proxy Check.",
                "Secures stored configs/assets and locks down HTTP traffic capture without injecting aggressive anti-clone checks.",
                "Great for API-driven apps; turn OFF Check VPN manually if targeting VPN apps."
            )
            WallProtectionTier.WALL_V3_JNI_STUB -> Triple(
                "Hardened JNI + InMemoryDexClassLoader Stub preset tuned for Sketchware Pro & Java/Kotlin APKs.",
                "Combines DEX Packing, Native JNI Stub, Anti-Tamper, Anti-Debug, Root Shield, StringVault, Asset AES, and Clone Blocker while keeping resource IDs safe.",
                "Recommended default for Sketchware Pro APKs — strong protection with zero resource ID crashes."
            )
            WallProtectionTier.WALL_V4_FORTRESS -> Triple(
                "Maximum Fortress preset activating all 10 security layers, Native Stub, Resource Confusing rules, Metadata Strip & APK Signing.",
                "Enforces full layered defense from bytecode to window FLAG_SECURE, network checks, and R8/AAPT2 Kit rules.",
                "Use for high-value release apps after checking compatibility warnings for your target APK."
            )
            WallProtectionTier.WALL_360_JIAGU -> Triple(
                "360加固-style multi-layer DEX shell with Anti-Debug, Anti-Tamper, Asset Vault, Clone Shield & Metadata Strip.",
                "Wraps original DEX in a protected shell while stripping Kotlin/build debug metadata from the archive.",
                "Strong commercial-grade preset for standard & Sketchware APKs."
            )
            WallProtectionTier.WALL_KIWI_SEC -> Triple(
                "几维安全-style bytecode guard focusing on DEX Packing, StringVault obfuscation, Anti-Debug & safe XML/Resource rules.",
                "Protects hardcoded strings and Dalvik code alongside ProGuard/R8 resource shrinking rules.",
                "Best when hiding keys/endpoints and hardening Java/Kotlin classes."
            )
            WallProtectionTier.WALL_IJIAMI -> Triple(
                "爱加密-style runtime hook defense prioritizing Frida/Xposed blocking, Root Shield, HttpCanary block & Anti-Screenshot.",
                "Guards active runtime memory, hooks, and window frames against dynamic analysis tools.",
                "Use when protecting apps against Frida scripts, LSPosed modules, or screen capture."
            )
            WallProtectionTier.WALL_TENCENT_LEGU -> Triple(
                "腾讯加固-style proxy stub combining DEX Vault, Anti-Debug, SSL Pinning/Proxy block & debug metadata stripping.",
                "Focuses on network MITM defense, certificate pinning, and bytecode hiding.",
                "Ideal for client-server login/API apps (avoid on VPN tools)."
            )
            WallProtectionTier.WALL_BANGCLE -> Triple(
                "梆梆加固-style RAM-only DEX vault with StringVault, Anti-Clone, Tapjacking shield & signature verification.",
                "Decrypts bytecode only in RAM while enforcing window protection and virtual-clone rejection.",
                "Strong all-round shield for financial, tool, or commercial APKs."
            )
        }
        val bnTitle = "${tier.title} (${tier.badge})"
        val detailsBn = when (tier) {
            WallProtectionTier.WALL_V1_STANDARD -> Triple(
                "হালকা ও ১০০% স্ট্যাবল প্রোটেকশন প্রিসেট — Anti-Tamper, Zero-Crash, DEX Guard এবং APK Signer চালু করে।",
                "অতিরিক্ত রুট বা এমুলেটর ব্লক ছাড়াই নিরাপদে DEX কোড প্রটেক্ট করে এবং v1+v2 সিগনেচার লাগায়।",
                "যেকোনো সাধারণ বা Sketchware অ্যাপের প্রথমিক সুরক্ষার জন্য একদম নির্ভুল।"
            )
            WallProtectionTier.WALL_V2_VAULT -> Triple(
                "Asset AES-256 এনক্রিপশন, StringVault, HttpCanary এবং VPN/Proxy ডিটেকশনসহ নেটওয়ার্ক-ফোকাসড প্রিসেট।",
                "অ্যাপের অ্যাসেট ও API ডেটা সুরক্ষিত করে এবং ট্রাফিক ক্যাপচার টুল ব্লক করে।",
                "API-ভিত্তিক অ্যাপের জন্য দারুণ; তবে VPN অ্যাপ হলে VPN চেক বন্ধ করে নিন।"
            )
            WallProtectionTier.WALL_V3_JNI_STUB -> Triple(
                "Sketchware Pro এবং Java/Kotlin APK-র জন্য বিশেষভাবে টিউন করা JNI + InMemoryDexClassLoader স্টাব প্রিসেট।",
                "DEX প্যাকিং, নেটিভ স্টাব, অ্যান্টি-ট্যাম্পার, অ্যান্টি-ডিবাগ, রুট ও ক্লোন শিল্ড চালু করে কিন্তু Sketchware-এর resources.arsc কখনো ভাঙে না।",
                "Sketchware প্রোজেক্টের জন্য সবচেয়ে নিরাপদ ও সেরা রিকমেন্ডেড প্রিসেট।"
            )
            WallProtectionTier.WALL_V4_FORTRESS -> Triple(
                "১০টি কোর লেয়ার, Native Stub, রিসোর্স কনফিউজিং ও মেটাডেটা স্ট্রিপিংসহ সর্বোচ্চ শক্তিশালী প্রিসেট।",
                "বাইটকোড থেকে শুরু করে স্ক্রিনশট ব্লক, নেটওয়ার্ক পিনিং এবং ক্লোন-ডিটেকশন একসাথে চালু করে।",
                "হাই-সিকিউরিটি রিলিজ বিল্ডের জন্য উপযুক্ত (চালানোর আগে ওয়ার্নিং কার্ড দেখে নিন)।"
            )
            else -> Triple(
                "${tier.title} স্টাইলের মাল্টি-লেয়ার শিল্ড: ${tier.summary}।",
                "এক ক্লিকে DEX প্যাকিং, স্টাব হ্যান্ডওভার, অ্যান্টি-ট্যাম্পার ও প্রাসঙ্গিক রানটাইম শিল্ডগুলো অটো-কনফিগার করে।",
                "টার্গেট অ্যাপের প্রয়োজন অনুযায়ী এই প্রিসেট বেছে নিয়ে পরে ইচ্ছেমতো অপশন কাস্টমাইজ করতে পারবেন।"
            )
        }
        val detailsHi = Triple(
            "${tier.title} (${tier.badge}) प्रीसेट: ${tier.summary}",
            "एक क्लिक में DEX पैकिंग, एंटी-टैम्पर, रनटाइम गार्ड और APK साइनिंग प्रोफ़ाइल ऑटो-सेट करता है।",
            "अपने ऐप के अनुसार चुनें; Sketchware ऐप्स के लिए Protection V3 सबसे सुरक्षित है।"
        )
        return tr(lang, bnTitle, detailsEn, detailsBn, detailsHi)
    }
}

fun ProtectionProfile.applyWallPreset(tier: WallProtectionTier): ProtectionProfile = when (tier) {
    WallProtectionTier.WALL_V1_STANDARD -> copy(
        wallProtectionTier = tier.name,
        antiTamperSignature = true,
        zeroCrashExceptionGuard = true,
        dexBytecodeGuard = true,
        assetAes256Encryption = false,
        nativeJniStubPacking = false,
        signOutputApk = true
    )
    WallProtectionTier.WALL_V2_VAULT -> copy(
        wallProtectionTier = tier.name,
        antiTamperSignature = true,
        zeroCrashExceptionGuard = true,
        dexBytecodeGuard = true,
        assetAes256Encryption = true,
        stringMetadataObfuscation = true,
        detectHttpCanary = true,
        sslPinningProxyBlock = true,
        nativeJniStubPacking = false,
        signOutputApk = true
    )
    WallProtectionTier.WALL_V3_JNI_STUB -> copy(
        wallProtectionTier = tier.name,
        antiTamperSignature = true,
        zeroCrashExceptionGuard = true,
        dexBytecodeGuard = true,
        assetAes256Encryption = true,
        antiDebugFridaShield = true,
        stringMetadataObfuscation = true,
        rootMagiskDetection = true,
        removeBundlingSoftware = true,
        nativeJniStubPacking = true,
        signOutputApk = true
    )
    WallProtectionTier.WALL_V4_FORTRESS -> copy(
        wallProtectionTier = tier.name,
        dexBytecodeGuard = true,
        assetAes256Encryption = true,
        antiTamperSignature = true,
        antiDebugFridaShield = true,
        stringMetadataObfuscation = true,
        zeroCrashExceptionGuard = true,
        rootMagiskDetection = true,
        emulatorCloneShield = true,
        sslPinningProxyBlock = true,
        screenTapjackingShield = true,
        nativeJniStubPacking = true,
        shrinkResources = true,
        shrinkAnim = true,
        shrinkDrawablesAndAnim = true,
        shrinkLayoutsAndXml = true,
        stripDebugMetadata = true,
        detectHttpCanary = true,
        removeBundlingSoftware = true,
        signOutputApk = true
    )
    WallProtectionTier.WALL_360_JIAGU -> copy(
        wallProtectionTier = tier.name,
        dexBytecodeGuard = true,
        nativeJniStubPacking = true,
        assetAes256Encryption = true,
        antiTamperSignature = true,
        antiDebugFridaShield = true,
        zeroCrashExceptionGuard = true,
        removeBundlingSoftware = true,
        stripDebugMetadata = true,
        signOutputApk = true
    )
    WallProtectionTier.WALL_KIWI_SEC -> copy(
        wallProtectionTier = tier.name,
        dexBytecodeGuard = true,
        nativeJniStubPacking = true,
        stringMetadataObfuscation = true,
        antiTamperSignature = true,
        antiDebugFridaShield = true,
        zeroCrashExceptionGuard = true,
        shrinkResources = true,
        shrinkXml = true,
        signOutputApk = true
    )
    WallProtectionTier.WALL_IJIAMI -> copy(
        wallProtectionTier = tier.name,
        dexBytecodeGuard = true,
        nativeJniStubPacking = true,
        antiDebugFridaShield = true,
        antiTamperSignature = true,
        rootMagiskDetection = true,
        screenTapjackingShield = true,
        zeroCrashExceptionGuard = true,
        detectHttpCanary = true,
        signOutputApk = true
    )
    WallProtectionTier.WALL_TENCENT_LEGU -> copy(
        wallProtectionTier = tier.name,
        dexBytecodeGuard = true,
        nativeJniStubPacking = true,
        assetAes256Encryption = true,
        antiTamperSignature = true,
        antiDebugFridaShield = true,
        zeroCrashExceptionGuard = true,
        sslPinningProxyBlock = true,
        stripDebugMetadata = true,
        signOutputApk = true
    )
    WallProtectionTier.WALL_BANGCLE -> copy(
        wallProtectionTier = tier.name,
        dexBytecodeGuard = true,
        nativeJniStubPacking = true,
        assetAes256Encryption = true,
        antiTamperSignature = true,
        stringMetadataObfuscation = true,
        zeroCrashExceptionGuard = true,
        removeBundlingSoftware = true,
        screenTapjackingShield = true,
        signOutputApk = true
    )
}
