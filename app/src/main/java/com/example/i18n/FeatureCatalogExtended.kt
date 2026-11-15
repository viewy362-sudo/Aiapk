package com.example.i18n

import com.example.security.ProtectionProfile

object FeatureCatalogExtended {

    object ResIds {
        const val SHRINK_RESOURCES = "res_shrink_all"
        const val SHRINK_ANIM = "res_shrink_anim"
        const val SHRINK_DRAWABLE = "res_shrink_drawable"
        const val SHRINK_COLOR = "res_shrink_color"
        const val SHRINK_ANIMATOR = "res_shrink_animator"
        const val SHRINK_LAYOUT = "res_shrink_layout"
        const val SHRINK_RAW = "res_shrink_raw"
        const val SHRINK_FONT = "res_shrink_font"
        const val SHRINK_MENU = "res_shrink_menu"
        const val SHRINK_MIPMAP = "res_shrink_mipmap"
        const val SHRINK_INTERPOLATOR = "res_shrink_interpolator"
        const val SHRINK_NAVIGATION = "res_shrink_navigation"
        const val SHRINK_TRANSITION = "res_shrink_transition"
        const val SHRINK_XML = "res_shrink_xml"
        const val SHRINK_XML_V1 = "res_shrink_xml_v1"
        const val SHRINK_XML_V2 = "res_shrink_xml_v2"
        const val STRIP_DEBUG = "res_strip_debug"
    }

    object CfgIds {
        const val CUSTOM_TOAST = "cfg_custom_toast"
        const val DETECT_HTTPCANARY = "cfg_httpcanary"
        const val DIALOG_CANCELABLE = "cfg_dialog_cancelable"
        const val CHECK_INTERNET = "cfg_check_net"
        const val REMOVE_DETECT_VPN = "cfg_remove_vpn"
        const val REMOVE_DETECT_CANARY = "cfg_remove_httpcanary"
        const val REMOVE_BUNDLING_SW = "cfg_remove_bundling"
        const val REMOVE_UPDATE_DIALOG = "cfg_remove_update_dialog"
        const val HIDE_ROOT = "cfg_hide_root"
        const val LOGIN_V1 = "adv_login_v1"
        const val LOGIN_V2 = "adv_login_v2"
    }

    private fun resSpec(
        id: String,
        title: String,
        folder: String,
        badge: String? = "VIP",
        isChecked: (ProtectionProfile) -> Boolean,
        applyToggle: (ProtectionProfile, Boolean) -> ProtectionProfile
    ): FeatureGuideSpec = FeatureGuideSpec(
        id = id,
        category = FeatureCategory.RESOURCES,
        defaultTitle = title,
        badge = badge,
        isChecked = isChecked,
        applyToggle = applyToggle,
        explain = { lang ->
            tr(
                lang,
                title = title,
                en = Triple(
                    "Applies strict shrinking & obfuscation rules for $folder entries without breaking compiled APK resource IDs.",
                    "Writes R8/AAPT2 resource shrinker keep/collapse directives into proguard-rules.pro and protection_policy.json while keeping R.id / resources.arsc pointers intact so Sketchware apps do not crash with Resources.NotFoundException.",
                    "Safe to enable on any APK. If you recompile in Android Studio/Sketchware Pro, copy proguard-rules.pro from the companion Kit."
                ),
                bn = Triple(
                    "কম্পাইল করা APK-র রিসোর্স আইডি না ভেঙে $folder ফোল্ডারের ফাইলগুলোর জন্য শ্রিঙ্কিং ও রিসোর্স কনফিউশন রুল যুক্ত করে।",
                    "সাথে থাকা ডেভেলপার কিটের proguard-rules.pro এবং protection_policy.json-এ $folder-এর জন্য রুলস সেট করে, কিন্তু Sketchware-এর resources.arsc আইডি পরিবর্তন করে না—তাই Resources.NotFoundException ক্র্যাশ হয় না।",
                    "যেকোনো Sketchware বা স্ট্যান্ডার্ড APK-তে নির্ভয়ে চালু করতে পারবেন; সোর্স প্রোজেক্টে রুলস ব্যবহার করতে কিটের ProGuard ফাইল যোগ করুন।"
                ),
                hi = Triple(
                    "बिना APK संसाधन ID तोड़े $folder फ़ाइलों के लिए श्रिंकिंग और कन्फ़्यूज़न नियम लागू करता है।",
                    "यह डेवलपर किट की proguard-rules.pro और protection_policy.json में नियम लिखता है लेकिन resources.arsc को सुरक्षित रखता है ताकि क्रैश न हो।",
                    "Sketchware और मानक APK दोनों पर सुरक्षित रूप से उपयोग किया जा सकता है।"
                )
            )
        }
    )

    val resourceFeatures: List<FeatureGuideSpec> = listOf(
        resSpec(ResIds.SHRINK_RESOURCES, "Shrink Resources", "all unused res/*", null, { it.shrinkResources }) { p, v -> p.copy(shrinkResources = v) },
        resSpec(ResIds.SHRINK_ANIM, "Shrink Anim", "res/anim/*", "VIP", { it.shrinkAnim }) { p, v -> p.copy(shrinkAnim = v) },
        resSpec(ResIds.SHRINK_DRAWABLE, "Shrink Drawable", "res/drawable/*", "VIP", { it.shrinkDrawablesAndAnim }) { p, v -> p.copy(shrinkDrawablesAndAnim = v) },
        resSpec(ResIds.SHRINK_COLOR, "Shrink Color", "res/color/*", "VIP", { it.shrinkColor }) { p, v -> p.copy(shrinkColor = v) },
        resSpec(ResIds.SHRINK_ANIMATOR, "Shrink Animator", "res/animator/*", "VIP", { it.shrinkAnimator }) { p, v -> p.copy(shrinkAnimator = v) },
        resSpec(ResIds.SHRINK_LAYOUT, "Shrink Layout", "res/layout/*", "VIP", { it.shrinkLayoutsAndXml }) { p, v -> p.copy(shrinkLayoutsAndXml = v) },
        resSpec(ResIds.SHRINK_RAW, "Shrink Raw", "res/raw/*", "VIP", { it.shrinkRaw }) { p, v -> p.copy(shrinkRaw = v) },
        resSpec(ResIds.SHRINK_FONT, "Shrink Font", "res/font/*", "VIP", { it.shrinkFont }) { p, v -> p.copy(shrinkFont = v) },
        resSpec(ResIds.SHRINK_MENU, "Shrink Menu", "res/menu/*", "VIP", { it.shrinkMenu }) { p, v -> p.copy(shrinkMenu = v) },
        resSpec(ResIds.SHRINK_MIPMAP, "Shrink Mipmap", "res/mipmap/*", "VIP", { it.shrinkMipmap }) { p, v -> p.copy(shrinkMipmap = v) },
        resSpec(ResIds.SHRINK_INTERPOLATOR, "Shrink Interpolator", "res/interpolator/*", "VIP", { it.shrinkInterpolator }) { p, v -> p.copy(shrinkInterpolator = v) },
        resSpec(ResIds.SHRINK_NAVIGATION, "Shrink Navigation", "res/navigation/*", "VIP", { it.shrinkNavigation }) { p, v -> p.copy(shrinkNavigation = v) },
        resSpec(ResIds.SHRINK_TRANSITION, "Shrink Transition", "res/transition/*", "VIP", { it.shrinkTransition }) { p, v -> p.copy(shrinkTransition = v) },
        resSpec(ResIds.SHRINK_XML, "Shrink XML", "res/xml/*", "VIP", { it.shrinkXml }) { p, v -> p.copy(shrinkXml = v) },
        resSpec(ResIds.SHRINK_XML_V1, "Shrink Resource XML V1", "flat AAPT2 XML Pass V1", "VIP", { it.shrinkResourceXmlV1 }) { p, v -> p.copy(shrinkResourceXmlV1 = v) },
        resSpec(ResIds.SHRINK_XML_V2, "Shrink Resource XML V2", "deep XML Attribute Pass V2", "VIP", { it.shrinkResourceXmlV2 }) { p, v -> p.copy(shrinkResourceXmlV2 = v) },
        FeatureGuideSpec(
            id = ResIds.STRIP_DEBUG,
            category = FeatureCategory.RESOURCES,
            defaultTitle = "Strip Build & Debug Metadata (APK)",
            badge = "SAFE",
            isChecked = { it.stripDebugMetadata },
            applyToggle = { p, v -> p.copy(stripDebugMetadata = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Strip Build & Debug Metadata (APK)",
                    en = Triple(
                        "Removes Kotlin coroutine debug probes, tooling JSON, and Gradle build metadata files from the protected APK.",
                        "Filters out DebugProbesKt.bin, kotlin-tooling-metadata.json, META-INF/*.version, and build metadata before re-signing, reducing APK size and fingerprinting leaks.",
                        "100% safe for all APKs (keeps app code and resources completely intact)."
                    ),
                    bn = Triple(
                        "APK-র ভেতর থেকে DebugProbesKt.bin, kotlin-tooling-metadata.json ও বিল্ড মেটাডেটা মুছে দেয়।",
                        "সাইন করার আগে অপ্রয়োজনীয় ডিবাগ ও ভার্সন মেটাডেটা বাদ দেয়—যাতে রিভার্স ইঞ্জিনিয়াররা আপনার বিল্ড টুল বা লাইব্রেরির তথ্য না পায় এবং APK সাইজও কিছুটা কমে।",
                        "সব ধরনের APK-র জন্য ১০০% সেফ।"
                    ),
                    hi = Triple(
                        "APK से DebugProbesKt.bin, kotlin-tooling-metadata.json और बिल्ड मेटाडेटा हटाता है।",
                        "री-साइन करने से पहले अनावश्यक डिबग मेटाडेटा फ़ाइलें हटाकर APK को साफ़ और छोटा बनाता है।",
                        "सभी APK के लिए 100% सुरक्षित है।"
                    )
                )
            }
        )
    )

    val configurationAndAdvancedFeatures: List<FeatureGuideSpec> = listOf(
        FeatureGuideSpec(
            id = CfgIds.CUSTOM_TOAST,
            category = FeatureCategory.CONFIGURATION,
            defaultTitle = "Add Custom Toast",
            badge = "FREE",
            isChecked = { it.customSecurityToast },
            applyToggle = { p, v -> p.copy(customSecurityToast = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Add Custom Toast",
                    en = Triple(
                        "Displays your own branded custom Toast message when the protected APK launches.",
                        "Reads customToastMessage from protection_policy.json inside BloodyStubApplication.onCreate() and posts a safe UI-thread Toast without blocking startup.",
                        "Turn ON whenever you want a 'Secured by Bloody Team' or custom creator credit banner on launch."
                    ),
                    bn = Triple(
                        "প্রোটেক্টেড অ্যাপ চালু হওয়ার সময় স্ক্রিনে আপনার কাস্টম Toast মেসেজ দেখায়।",
                        "BloodyStubApplication চালুর সময় protection_policy.json থেকে আপনার লেখা টেক্সট নিয়ে মেইন থ্রেডে Toast হিসেবে প্রদর্শন করে।",
                        "অ্যাপের শুরুতে নিজের টিম বা চ্যানেলের নাম দেখাতে এটি ON করে নিচের টেক্সট বক্সে বার্তা লিখুন।"
                    ),
                    hi = Triple(
                        "प्रोटेक्टेड ऐप खुलने पर आपका कस्टम Toast संदेश दिखाता है।",
                        "BloodyStubApplication शुरू होते समय protection_policy.json से आपका टेक्स्ट पढ़कर स्क्रीन पर दिखाता है।",
                        "लॉन्च पर अपना ब्रांड या नाम दिखाने के लिए इसे ON करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.DETECT_HTTPCANARY,
            category = FeatureCategory.CONFIGURATION,
            defaultTitle = "Detect HttpCanary",
            badge = "VIP",
            isChecked = { it.detectHttpCanary },
            applyToggle = { p, v -> p.copy(detectHttpCanary = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Detect HttpCanary & Packet Sniffers",
                    en = Triple(
                        "Detects installed packet capture & MITM apps such as HttpCanary, Reqable, SSL Capture, Charles, and Fiddler.",
                        "Queries installed sniffer packages cleanly inside the runtime shield and blocks launch with a security notice unless 'Remove Detect HttpCanary' override is active.",
                        "Enable for API/login apps to protect endpoints. Do not pair with 'Remove Detect HttpCanary'."
                    ),
                    bn = Triple(
                        "ফোনে HttpCanary, Reqable, SSL Capture বা Packet Sniffer অ্যাপ থাকলে ধরে ফেলে ও ট্রাফিক চুরি আটকায়।",
                        "রানটাইম স্টাব ইনস্টল করা স্নিফার প্যাকেজ ও প্রক্সি টুল চেক করে এবং শনাক্ত হলে প্রোটেকশন ডায়ালগ দেখিয়ে স্নিফিং বন্ধ করে।",
                        "লগইন বা API অ্যাপে চালু রাখুন। 'Remove Detect HttpCanary' অন থাকলে এটি ওভাররাইড হয়ে যাবে।"
                    ),
                    hi = Triple(
                        "फ़ोन में HttpCanary, Reqable या Packet Capture ऐप्स का पता लगाकर API चोरी रोकता है।",
                        "रनटाइम पर स्निफ़र पैकेज की जांच करता है और पाए जाने पर सुरक्षा चेतावनी दिखाता है।",
                        "API और लॉगिन ऐप्स के लिए ऑन करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.DIALOG_CANCELABLE,
            category = FeatureCategory.CONFIGURATION,
            defaultTitle = "Dialog Cancelable",
            badge = "VIP",
            isChecked = { it.dialogCancelable },
            applyToggle = { p, v -> p.copy(dialogCancelable = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Dialog Cancelable",
                    en = Triple(
                        "Controls whether runtime shield/internet dialogs can be dismissed by tapping outside or pressing Back.",
                        "Sets AlertDialog.setCancelable(true/false) on security, network check, and validation dialogs shown by the Stub.",
                        "Keep OFF for strict enforcement; turn ON if you prefer soft non-blocking warnings."
                    ),
                    bn = Triple(
                        "সিকিউরিটি বা ইন্টারনেট অ্যালার্ট ডায়ালগ ব্যাক বাটন বা বাইরে ট্যাপ করে বন্ধ করা যাবে কি না তা নিয়ন্ত্রণ করে।",
                        "স্টাবের AlertDialog-এ setCancelable(true/false) সেট করে।",
                        "কঠোর সিকিউরিটি চাইলে OFF রাখুন, আর সফট ওয়ার্নিং চাইলে ON করুন।"
                    ),
                    hi = Triple(
                        "तय करता है कि सुरक्षा या इंटरनेट अलर्ट डायलॉग को बैक बटन से बंद किया जा सकता है या नहीं।",
                        "Stub के AlertDialog पर setCancelable(true/false) लागू करता है।",
                        "कठोर सुरक्षा के लिए OFF रखें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.CHECK_INTERNET,
            category = FeatureCategory.CONFIGURATION,
            defaultTitle = "Check Internet",
            badge = "VIP",
            isChecked = { it.checkInternetRequired },
            applyToggle = { p, v -> p.copy(checkInternetRequired = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Check Internet Required",
                    en = Triple(
                        "Requires an active Wi-Fi or cellular data connection before the app opens.",
                        "Queries ConnectivityManager NetworkCapabilities for NET_CAPABILITY_INTERNET / VALIDATED on launch and prompts the user if offline.",
                        "Enable ONLY for online apps; keep OFF for offline Sketchware tools."
                    ),
                    bn = Triple(
                        "অ্যাপ খোলার আগে ফোনে চালু ইন্টারনেট কানেকশন (Wi-Fi / Mobile Data) আছে কি না তা যাচাই করে।",
                        "ConnectivityManager দিয়ে নেটওয়ার্ক যাচাই করে—ইন্টারনেট না থাকলে ওয়ার্নিং ডায়ালগ দেখায়।",
                        "শুধুমাত্র অনলাইন অ্যাপে ON করুন; অফলাইন অ্যাপের ক্ষেত্রে এটি OFF রাখুন।"
                    ),
                    hi = Triple(
                        "ऐप खुलने से पहले सक्रिय इंटरनेट कनेक्शन की जांच करता है।",
                        "ConnectivityManager से नेटवर्क जांचता है और ऑफ़लाइन होने पर अलर्ट दिखाता है।",
                        "केवल ऑनलाइन ऐप्स के लिए ON करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.REMOVE_DETECT_VPN,
            category = FeatureCategory.CONFIGURATION,
            defaultTitle = "Remove Detect Vpn Proxy",
            badge = "VIP",
            isChecked = { it.removeDetectVpnProxy },
            applyToggle = { p, v -> p.copy(removeDetectVpnProxy = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Remove Detect VPN Proxy (Override)",
                    en = Triple(
                        "Overrides VPN/Proxy blocking inside Bloody Shield so users behind VPNs are allowed cleanly.",
                        "Sets removeDetectVpnProxy=true in protection_policy.json, bypassing tun0/ppp0 & proxy termination checks.",
                        "Useful when building VPN apps or letting users on VPN use your app without errors."
                    ),
                    bn = Triple(
                        "রানিং অ্যাপে VPN বা Proxy ডিটেকশন চেক বাইপাস করে দেয় যাতে VPN চালু থাকলেও অ্যাপ স্বাভাবিকভাবে চলে।",
                        "protection_policy.json-এ removeDetectVpnProxy=true সেট করে প্রক্সি/VPN ব্লক নিষ্ক্রিয় করে।",
                        "VPN ইউজারদের অ্যাক্সেস দিতে চাইলে ব্যবহার করুন (Detect VPN Proxy এর সাথে একসাথে রাখবেন না)।"
                    ),
                    hi = Triple(
                        "VPN या Proxy ब्लॉक जांच को बायपास करता है ताकि VPN उपयोगकर्ता ऐप चला सकें।",
                        "रनटाइम पॉलिसी में VPN जांच ओवरराइड सेट करता है।",
                        "VPN समर्थित ऐप्स के लिए उपयोगी।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.REMOVE_DETECT_CANARY,
            category = FeatureCategory.CONFIGURATION,
            defaultTitle = "Remove Detect HttpCanary",
            badge = "VIP",
            isChecked = { it.removeDetectHttpCanary },
            applyToggle = { p, v -> p.copy(removeDetectHttpCanary = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Remove Detect HttpCanary (Override)",
                    en = Triple(
                        "Disables HttpCanary & packet sniffer termination checks inside the Stub runtime.",
                        "Sets removeDetectHttpCanary=true in policy so developer devices with traffic analyzers are not blocked.",
                        "Use only during debugging or if you don't want HttpCanary blocks enforced."
                    ),
                    bn = Triple(
                        "রানটাইমে HttpCanary ও ট্রাফিক স্নিফার ডিটেকশন চেক বাইপাস/বাতিল করে।",
                        "পলিসিতে removeDetectHttpCanary=true সেট করে দেয়।",
                        "শুধু নিজের টেস্টিং বা ডিবাগিং প্রয়োজনে ব্যবহার করুন।"
                    ),
                    hi = Triple(
                        "रनटाइम में HttpCanary जांच को निष्क्रिय करता है।",
                        "पॉलिसी ओवरराइड द्वारा स्निफ़र ब्लॉक को बंद करता है।",
                        "केवल डेवलपर टेस्टिंग के लिए उपयोग करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.REMOVE_BUNDLING_SW,
            category = FeatureCategory.CONFIGURATION,
            defaultTitle = "Remove Bundling Software",
            badge = "VIP",
            isChecked = { it.removeBundlingSoftware },
            applyToggle = { p, v -> p.copy(removeBundlingSoftware = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Remove Bundling Software (Anti-Virtual Clone)",
                    en = Triple(
                        "Blocks VirtualApp, Parallel Space, DualSpace, 2Accounts & multi-user clone wrappers.",
                        "Checks dataDir paths (/data/user/999, parallel/dual/virtual containers) inside the runtime Stub and prevents cloned execution.",
                        "100% crash-safe on normal Android & Sketchware phones; stops multi-account abuse."
                    ),
                    bn = Triple(
                        "Parallel Space, DualSpace, VirtualApp বা ক্লোনার অ্যাপের ভেতরে আপনার APK রান করা বন্ধ করে।",
                        "অ্যাপের dataDir পাথ ও মাল্টি-ইউজার কন্টেইনার প্যাটার্ন চেক করে ভার্চুয়াল ক্লোন শনাক্ত করলে ব্লক করে।",
                        "সাধারণ ফোনে ১০০% সেফ—শুধু ক্লোন অ্যাপে চলা আটকায়।"
                    ),
                    hi = Triple(
                        "Parallel Space, DualSpace और Virtual Clone ऐप्स में ऐप चलाने से रोकता है।",
                        "रनटाइम डेटा डायरेक्टरी और कंटेनर पाथ की जांच करके क्लोनिंग रोकता है।",
                        "सामान्य डिवाइस पर 100% सुरक्षित है।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.REMOVE_UPDATE_DIALOG,
            category = FeatureCategory.CONFIGURATION,
            defaultTitle = "Remove Update AlertDialog",
            badge = "VIP",
            isChecked = { it.removeUpdateAlertDialog },
            applyToggle = { p, v -> p.copy(removeUpdateAlertDialog = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Remove Update AlertDialog Guard",
                    en = Triple(
                        "Suppresses forced/injected update nag dialogs by marking window dialog guard flags in the runtime.",
                        "Applies dialog guard policy inside ActivityLifecycleCallbacks for cleaner uninterrupted launches.",
                        "Enable if an older APK triggers unwanted update popups."
                    ),
                    bn = Triple(
                        "পুরোনো অ্যাপে অযাচিত ফোর্স-আপডেট অ্যালার্ট ডায়ালগ পপ-আপ নিয়ন্ত্রণে উইন্ডো গার্ড পলিসি যুক্ত করে।",
                        "ActivityLifecycleCallbacks এর মাধ্যমে লঞ্চ পপ-আপ গার্ড করে।",
                        "অবাঞ্ছিত আপডেট পপ-আপ বন্ধ করতে চাইলে চালু করুন।"
                    ),
                    hi = Triple(
                        "लॉन्च के समय अवांछित फ़ोर्स-अपडेट पॉप-अप डायलॉग को नियंत्रित करता है।",
                        "ActivityLifecycleCallbacks के माध्यम से डायलॉग गार्ड लागू करता है।",
                        "पुराने ऐप्स में अपडेट पॉप-अप रोकने के लिए उपयोगी।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.HIDE_ROOT,
            category = FeatureCategory.CONFIGURATION,
            defaultTitle = "Hide Root",
            badge = "VIP",
            isChecked = { it.hideRoot },
            applyToggle = { p, v -> p.copy(hideRoot = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Hide Root (Allow Rooted Devices)",
                    en = Triple(
                        "Masks root detection inside Bloody Shield so rooted phones can run the app without warnings.",
                        "Overrides rootMagiskDetection checks via hideRoot=true inside protection_policy.json.",
                        "Keep OFF if you want strict anti-root protection; turn ON if your target users run rooted phones."
                    ),
                    bn = Triple(
                        "রুট চেকিং মাস্ক করে যাতে রুটেড ফোনেও আপনার অ্যাপ কোনো বাধা ছাড়া চলতে পারে।",
                        "protection_policy.json-এ hideRoot=true সেট করে রুট ওয়ার্নিং ওভাররাইড করে।",
                        "রুটেড ইউজারদের চালাতে দিতে চাইলে ON করুন; কিন্তু অ্যান্টি-রুট চাইলে OFF রাখুন।"
                    ),
                    hi = Triple(
                        "रूट जांच को मास्क करता है ताकि रूटेड डिवाइस पर भी ऐप बिना रोक के चले।",
                        "पॉलिसी में hideRoot=true सेट करके रूट चेतावनी हटाता है।",
                        "कठोर एंटी-रूट के लिए इसे OFF रखें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.LOGIN_V1,
            category = FeatureCategory.ADVANCED,
            defaultTitle = "Add Login Validate V1",
            badge = "VIP",
            isChecked = { it.loginValidateGate },
            applyToggle = { p, v -> p.copy(loginValidateGate = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Add Login Validate V1",
                    en = Triple(
                        "Shows an access-token verification prompt when the first Activity opens in the protected APK.",
                        "Stub registers an Activity lifecycle hook that presents a single-field Access Token / License Dialog before letting users interact.",
                        "Great for private VIP builds or beta testers."
                    ),
                    bn = Triple(
                        "অ্যাপ খোলার সাথে সাথে প্রথম স্ক্রিনে একটি Access Token / পাসকোড ভেরিফিকেশন ডায়ালগ দেখায়।",
                        "স্টাব প্রথম Activity রেডি হওয়ার সাথে সাথে লগইন গেট দেখায়—সঠিক টোকেন না দিলে ভেতরে ঢোকা যায় না।",
                        "প্রাইভেট VIP বা পেইড অ্যাপে অ্যাক্সেস লক লাগাতে এটি ব্যবহার করুন।"
                    ),
                    hi = Triple(
                        "ऐप खुलते ही पहली स्क्रीन पर Access Token सत्यापन प्रॉम्प्ट दिखाता है।",
                        "सही टोकन दर्ज करने के बाद ही यूज़र ऐप का उपयोग कर पाता है।",
                        "VIP या प्राइवेट बिल्ड के लिए बेहतरीन है।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = CfgIds.LOGIN_V2,
            category = FeatureCategory.ADVANCED,
            defaultTitle = "Add Login Validate V2",
            badge = "VIP",
            isChecked = { it.loginValidateV2 },
            applyToggle = { p, v -> p.copy(loginValidateV2 = v) },
            explain = { lang ->
                tr(
                    lang,
                    title = "Add Login Validate V2",
                    en = Triple(
                        "Shows a dual-field User ID + VIP License Key authentication gate on startup.",
                        "Presents a multi-field material validation dialog inside the launched Activity before unlocking the UI.",
                        "Use when you want User ID + License Key validation instead of single-field V1."
                    ),
                    bn = Triple(
                        "অ্যাপের শুরুতে User ID এবং VIP License Key—দুই ফিল্ডের ডাবল লগইন ভ্যালিডেশন গেট যুক্ত করে।",
                        "প্রথম Activity চালুর সাথেই User ID + লাইসেন্স কী যাচাই করে তবেই অ্যাপ আনলক করে।",
                        "VIP ইউজার ম্যানেজমেন্টের জন্য V1-এর চেয়ে শক্তিশালী ডুয়াল গেট।"
                    ),
                    hi = Triple(
                        "ऐप स्टार्टअप पर User ID और VIP License Key वाला दोहरा लॉगिन गेट दिखाता है।",
                        "दोनों फ़ील्ड सत्यापित होने के बाद ही ऐप अनलॉक होता है।",
                        "VIP यूज़र एक्सेस नियंत्रण के लिए उपयोग करें।"
                    )
                )
            }
        )
    )

    val studioToolFeatures: List<FeatureGuideSpec> = listOf(
        FeatureGuideSpec(
            id = FeatureIds.SMART_AUTO,
            category = FeatureCategory.STUDIO_TOOLS,
            defaultTitle = "Smart Auto Compatibility Analyzer",
            badge = "AUTO",
            explain = { lang ->
                tr(
                    lang,
                    title = "Smart Auto Compatibility Analyzer",
                    en = Triple(
                        "Automatically inspects your selected APK/ZIP and ticks the safest, crash-free protection options.",
                        "Scans DEX headers (DEX 035-040, 65K limits), AndroidManifest permissions, Sketchware/Flutter/Unity/VPN markers, and auto-configures a tailored profile plus live conflict warnings.",
                        "Triggered automatically when selecting an APK, or anytime by tapping 'Smart Auto'."
                    ),
                    bn = Triple(
                        "আপনি যে APK বা ZIP ফাইল সিলেক্ট করেন তা স্ক্যান করে কোন এনক্রিপশন দিলে ক্র্যাশ করবে না তা অটোমেটিক সিলেক্ট করে দেয়।",
                        "DEX হেডার, ম্যানিফেস্ট পারমিশন এবং Sketchware / Flutter / Unity / VPN অ্যাপের প্যাটার্ন দেখে নিরাপদ টিকমার্ক বসায় ও রিয়েল-টাইম ওয়ার্নিং দেখায়।",
                        "অ্যাপ ইনপুট দেওয়ার সাথে সাথেই চলে; আবার 'Smart Auto' চাপলেও রিসেট-সিলেক্ট হয়।"
                    ),
                    hi = Triple(
                        "आपके चुने हुए APK/ZIP को स्कैन करके सबसे सुरक्षित और क्रैश-फ़्री सुरक्षा विकल्प ऑटो-सिलेक्ट करता है।",
                        "DEX हेडर, मैनिफेस्ट अनुमतियाँ और Sketchware/Flutter/VPN मार्कर की जांच करके लाइव चेतावनी भी देता है।",
                        "APK चुनते ही या 'Smart Auto' बटन दबाने पर चलता है।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.PIPELINE_FORGE,
            category = FeatureCategory.STUDIO_TOOLS,
            defaultTitle = "Protect & Sign Output APK Pipeline",
            badge = "BUILD",
            explain = { lang ->
                tr(
                    lang,
                    title = "Protect & Sign Output APK Pipeline",
                    en = Triple(
                        "Builds the final protected & signed .apk alongside the companion developer security kit .zip.",
                        "Runs 8 streaming stages: Inspection -> DEX AES/XOR Packing -> 10-Layer Kit -> JNI/Stub Code -> StringVault -> ProGuard Rules -> Binary AXML + Stub DEX Injection -> ZipAlign + v1/v2 RSA Signing.",
                        "Tap after choosing your APK and checking the selected options."
                    ),
                    bn = Triple(
                        "আপনার বেছে নেওয়া সব সিকিউরিটি লেয়ার অ্যাপ্লাই করে ইনস্টলযোগ্য সাইন্ড .apk ও ডেভেলপার কিট .zip তৈরি করে।",
                        "৮টি স্ট্রিমিং ধাপে APK ইনস্পেক্ট, DEX এনক্রিপশন, বাইনারি AXML প্যাচ, Stub DEX ইনজেকশন, ZipAlign ও v1+v2 RSA সাইনিং সম্পন্ন করে।",
                        "অপশন ঠিক করার পর নিচের 'Protect & Sign Output APK' বাটনে ট্যাপ করুন।"
                    ),
                    hi = Triple(
                        "आपकी चुनी गई सभी सुरक्षा परतें जोड़कर इंस्टॉल योग्य साइन किया हुआ .apk और डेवलपर किट .zip तैयार करता है।",
                        "8 स्ट्रीमिंग चरणों में DEX पैकिंग, AXML पैचिंग, Stub इंजेक्शन, ZipAlign और v1+v2 साइनिंग करता है।",
                        "प्रोफ़ाइल चुनने के बाद मुख्य Protect बटन पर टैप करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.DEX_PACKER_LAB,
            category = FeatureCategory.STUDIO_TOOLS,
            defaultTitle = "DEX Packer & JNI Lab",
            badge = "LAB",
            explain = { lang ->
                tr(
                    lang,
                    title = "classes.dex Packer & JNI Lab",
                    en = Triple(
                        "Interactive testing lab that simulates Build-Time DEX encryption and Runtime RAM decryption, and exports C++ JNI + Java Stub source code.",
                        "Demonstrates live Shannon entropy, SHA-256 verification, and exact CMakeLists.txt, dex_shield_jni.cpp, and DexStubApplication.java templates.",
                        "Use the Packer tab to inspect the native C++ loader architecture or copy custom JNI code."
                    ),
                    bn = Triple(
                        "Build-Time DEX এনক্রিপশন এবং Runtime RAM ডিক্রিপশন লাইভ টেস্ট করার ল্যাব ও C++ JNI + Java Stub কোড এক্সপোর্টার।",
                        "লাইভ Shannon এনট্রপি, রাউন্ড-ট্রিপ ভেরিফিকেশন এবং আসল CMakeLists.txt ও JNI C++ কোড দেখায়।",
                        "নিচের 'Packer' ট্যাবে গিয়ে রান ও কপি করতে পারবেন।"
                    ),
                    hi = Triple(
                        "DEX एन्क्रिप्शन और RAM डिक्रिप्शन टेस्ट करने की लैब तथा C++ JNI / Java Stub कोड जनरेटर।",
                        "लाइव एन्ट्रॉपी और CMakeLists.txt / JNI C++ कोड दिखाता है।",
                        "नीचे 'Packer' टैब से खोलें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.ASSET_VAULT_TOOL,
            category = FeatureCategory.STUDIO_TOOLS,
            defaultTitle = "AES-256-GCM Asset Vault",
            badge = "TOOL",
            explain = { lang ->
                tr(
                    lang,
                    title = "AES-256-GCM Asset Vault",
                    en = Triple(
                        "Standalone encryption/decryption tool for JSON configs, text secrets, and binary files (.enc) using DXS1 authenticated AES-256-GCM.",
                        "Derives a 256-bit key from your passphrase via PBKDF2-HMAC-SHA256 (16,384 rounds) and protects ciphertext with a 128-bit GCM auth tag.",
                        "Use the Vault tab to encrypt or decrypt individual assets or verify .enc files."
                    ),
                    bn = Triple(
                        "আলাদাভাবে যেকোনো JSON, টেক্সট বা ফাইল (.enc) AES-256-GCM দিয়ে এনক্রিপ্ট ও ডিক্রিপ্ট করার টুল।",
                        "PBKDF2 ১৬,৩৮৪ রাউন্ড দিয়ে কী বানায় ও 128-বিট GCM ট্যাগ দিয়ে ফাইল সুরক্ষিত করে—ভুল পাসওয়ার্ড বা ট্যাম্পার হলেই ডিক্রিপশন আটকে দেয়।",
                        "নিচের 'Vault' ট্যাবে গিয়ে ফাইল বা টেক্সট এনক্রিপ্ট/ডিক্রিপ্ট করুন।"
                    ),
                    hi = Triple(
                        "किसी भी JSON, टेक्स्ट या बाइनरी फ़ाइल को AES-256-GCM से एन्क्रिप्ट/डिक्रिप्ट करने का टूल।",
                        "PBKDF2-HMAC-SHA256 और 128-बिट GCM टैग के साथ DXS1 फ़ॉर्मेट बनाता है।",
                        "नीचे 'Vault' टैब से उपयोग करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.LIVE_AUDIT,
            category = FeatureCategory.STUDIO_TOOLS,
            defaultTitle = "Live Integrity Audit & APK Inspector",
            badge = "AUDIT",
            explain = { lang ->
                tr(
                    lang,
                    title = "Live Integrity Audit & APK Inspector",
                    en = Triple(
                        "Audits the live device security environment (Root, Debugger, Frida/Xposed, VPN, Signing SHA-256) and inspects any APK's DEX/65K/native architecture.",
                        "Runs 10 real runtime environment checks and parses Dalvik DEX headers + AXML manifest metadata without loading untrusted code.",
                        "Open the Audit tab anytime to test device security or inspect an APK."
                    ),
                    bn = Triple(
                        "ডিভাইসের লাইভ সিকিউরিটি স্ট্যাটাস (Root, Debugger, Frida, VPN, Signing SHA-256) এবং যেকোনো APK-র ভেতরের DEX/65K/আর্কিটেকচার স্ক্যান করে।",
                        "রিয়েল-টাইমে ফোনের নিরাপত্তা স্কোর বের করে এবং APK না চালিয়েই তার হেডার ও অ্যাসেট বিশ্লেষণ করে।",
                        "নিচের 'Audit' ট্যাব থেকে যেকোনো সময় লাইভ স্ক্যান চালান।"
                    ),
                    hi = Triple(
                        "डिवाइस की लाइव सुरक्षा स्थिति और किसी भी APK के DEX/65K/मैनिफेस्ट विवरण को स्कैन करता है।",
                        "रियल-टाइम में 10 सुरक्षा जांच और APK हेडर विश्लेषण चलाता है।",
                        "नीचे 'Audit' टैब से स्कैन करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.SKETCHWARE_KIT,
            category = FeatureCategory.STUDIO_TOOLS,
            defaultTitle = "Sketchware Pro Java Kit & ProGuard",
            badge = "KIT",
            explain = { lang ->
                tr(
                    lang,
                    title = "Sketchware Pro Java Kit & ProGuard Exporter",
                    en = Triple(
                        "Generates copy-ready Java security classes (DexShieldGuard, AssetVaultHelper, SslPinningGuard) and R8/ProGuard rules tailored for Sketchware Pro.",
                        "Builds pure Java 8 / AndroidX-free drop-in classes that compile cleanly inside Sketchware Pro's local Java compiler.",
                        "Open the Kit tab to copy individual files or export the full combined bundle."
                    ),
                    bn = Triple(
                        "Sketchware Pro প্রোজেক্টের ভেতরে সরাসরি পেস্ট করার জন্য DexShieldGuard.java, AssetVaultHelper.java, SslPinningGuard.java ও ProGuard রুলস তৈরি করে দেয়।",
                        "অতিরিক্ত কোনো ভারী লাইব্রেরি ছাড়াই খাঁটি Java-তে কোড জেনারেট করে যাতে Sketchware-এ কম্পাইল এরর না আসে।",
                        "নিচের 'Kit' ট্যাব থেকে কোড কপি করে আপনার প্রোজেক্টে বসান।"
                    ),
                    hi = Triple(
                        "Sketchware Pro प्रोजेक्ट्स के लिए तैयार Java सुरक्षा क्लासेस और ProGuard नियम जनरेट करता है।",
                        "बिना किसी जटिल डिपेंडेंसी के शुद्ध Java कोड देता है।",
                        "नीचे 'Kit' टैब से कोड कॉपी करें।"
                    )
                )
            }
        ),
        FeatureGuideSpec(
            id = FeatureIds.STRING_CRASH_LAB,
            category = FeatureCategory.STUDIO_TOOLS,
            defaultTitle = "String Obfuscator & Zero-Crash Lab",
            badge = "LAB",
            explain = { lang ->
                tr(
                    lang,
                    title = "String Obfuscator & Zero-Crash Lab",
                    en = Triple(
                        "Generates Java/Kotlin XOR byte-rotated decoder snippets for secrets and lets you test the Zero-Crash handler live without crashing the app.",
                        "Encodes strings into unsigned byte arrays with zero plaintext literals and simulates a worker-thread exception absorbed by ZeroCrashGuard.",
                        "Located under the Kit tab -> 'String & Crash Lab'."
                    ),
                    bn = Triple(
                        "যেকোনো গোপন API কী-কে প্লেইনটেক্সট মুক্ত Java/Kotlin ডিকোডার কোডে রূপান্তর করে এবং অ্যাপ ক্র্যাশ না করিয়ে Zero-Crash গার্ড লাইভ টেস্ট করার সুবিধা দেয়।",
                        "স্ট্রিংকে XOR ও বিট-রোটেটেড বাইট অ্যারেতে বদলে দেয় এবং ব্যাকগ্রাউন্ড এরর নিরাপদে ধরে লগ করে।",
                        "Kit ট্যাবের ভেতর 'String & Crash Lab' সাব-ট্যাবে পাবেন।"
                    ),
                    hi = Triple(
                        "सीक्रेट कीज़ को XOR बाइट-रोटेटेड Java/Kotlin कोड में बदलता है और Zero-Crash गार्ड को लाइव टेस्ट करने देता है।",
                        "बिना प्लेनटेक्स्ट के बाइट डिकोडर बनाता है और बैकग्राउंड थ्रेड त्रुटियों को सुरक्षित रोकता है।",
                        "Kit टैब के अंदर 'String & Crash Lab' में उपलब्ध है।"
                    )
                )
            }
        )
    )
}
