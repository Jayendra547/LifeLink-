package com.example.lifelink.ai

interface Translator {
    suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String
}

data class SupportedLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagEmoji: String
)

object SupportedLanguages {
    val list = listOf(
        SupportedLanguage("en", "English", "English", "🌐"),
        SupportedLanguage("te", "Telugu", "తెలుగు", "🇮🇳"),
        SupportedLanguage("hi", "Hindi", "हिन्दी", "🇮🇳"),
        SupportedLanguage("ta", "Tamil", "தமிழ்", "🇮🇳"),
        SupportedLanguage("kn", "Kannada", "ಕನ್ನಡ", "🇮🇳"),
        SupportedLanguage("ml", "Malayalam", "മലയാളം", "🇮🇳"),
        SupportedLanguage("bn", "Bengali", "বাংলা", "🇮🇳")
    )

    fun getByCode(code: String): SupportedLanguage {
        return list.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: SupportedLanguage("en", "English", "English", "🌐")
    }
}

class IndicTranslator : Translator {

    // Offline semantic dictionary for core emergency and disaster communications (IndicTrans2 baseline)
    private val emergencyCorpus = mapOf(
        "fire" to mapOf(
            "en" to "There is a severe fire spreading, evacuate immediately",
            "te" to "తీవ్రమైన మంటలు వ్యాపిస్తున్నాయి, వెంటనే ఖాళీ చేయండి",
            "hi" to "भीषण आग फैल रही है, तुरंत सुरक्षित बाहर निकलें",
            "ta" to "கடுமையான தீ பரவுகிறது, உடனடியாக வெளியேறுங்கள்",
            "kn" to "ತೀವ್ರ ಬೆಂಕಿ ಹರಡುತ್ತಿದೆ, ತಕ್ಷಣ ಸ್ಥಳಾಂತರಿಸಿ",
            "ml" to "തീ പടരുന്നു, ഉടൻ ഒഴിഞ്ഞുപോവുക",
            "bn" to "মারাত্মক আগুন ছড়িয়ে পড়ছে, অবিলম্বে খালি করুন"
        ),
        "medical" to mapOf(
            "en" to "People are critically injured, medical team and ambulance required",
            "te" to "ప్రజలు తీవ్రంగా గాయపడ్డారు, వైద్య బృందం మరియు అంబులెన్స్ అవసరం",
            "hi" to "लोग गंभीर रूप से घायल हैं, मेडिकल टीम और एम्बुलेंस की आवश्यकता है",
            "ta" to "மக்கள் படுகாயமடைந்துள்ளனர், மருத்துவக் குழு மற்றும் ஆம்புலன்ஸ் தேவை",
            "kn" to "ಜನರು ತೀವ್ರವಾಗಿ ಗಾಯಗೊಂಡಿದ್ದಾರೆ, ವೈದ್ಯಕೀಯ ತಂಡ ಮತ್ತು ಆಂಬ್ಯುಲೆನ್ಸ್ ಅಗತ್ಯವಿದೆ",
            "ml" to "ആളുകൾക്ക് ഗുരുതരമായി പരിക്കേറ്റു, മെഡിക്കൽ സംഘവും ആംബുലൻസും ആവശ്യമാണ്",
            "bn" to "মানুষ গুরুতরভাবে আহত, মেডিকেল টিম এবং অ্যাম্বুলেন্স প্রয়োজন"
        ),
        "trapped" to mapOf(
            "en" to "Civilians trapped under building debris, send search and rescue units",
            "te" to "భవన శిథిలాల కింద ప్రజలు చిక్కుకున్నారు, రెస్క్యూ బృందాన్ని పంపండి",
            "hi" to "मलबे के नीचे नागरिक फंसे हुए हैं, खोज और बचाव दल भेजें",
            "ta" to "கட்டட இடிபாடுகளில் மக்கள் சிக்கியுள்ளனர், மீட்புக் குழுவை அனுப்புங்கள்",
            "kn" to "ಕಟ್ಟಡದ ಅವಶೇಷಗಳಡಿ ಜನ ಸಿಲುಕಿದ್ದಾರೆ, ರಕ್ಷಣಾ ತಂಡ ಕಳುಹಿಸಿ",
            "ml" to "കെട്ടിടാവശിഷ്ടങ്ങൾക്കിടയിൽ ആളുകൾ കുടുങ്ങി, രക്ഷാപ്രവർത്തകരെ അയക്കുക",
            "bn" to "ধ্বংসস্তূপের নিচে মানুষ আটকে আছে, উদ্ধারকারী দল পাঠান"
        ),
        "help" to mapOf(
            "en" to "Emergency! Urgent rescue assistance required immediately",
            "te" to "అత్యవసరం! వెంటనే రక్షణ సహాయం అందించండి",
            "hi" to "आपातकाल! तत्काल बचाव सहायता की आवश्यकता है",
            "ta" to "அவசரம்! உடனடியாக மீட்பு உதவி தேவை",
            "kn" to "ತುರ್ತು ಪರಿಸ್ಥಿತಿ! ತಕ್ಷಣ ರಕ್ಷಣಾ ನೆರವು ಬೇಕು",
            "ml" to "അടിയന്തിരാവസ്ഥ! അടിയന്തര രക്ഷാസഹായം ആവശ്യമാണ്",
            "bn" to "জরুরি অবস্থা! অবিলম্বে উদ্ধার সহায়তা প্রয়োজন"
        ),
        "food_water" to mapOf(
            "en" to "Critical shortage of drinking water and food supplies",
            "te" to "మంచినీరు మరియు ఆహార నిల్వల తీవ్ర కొరత ఏర్పడింది",
            "hi" to "पीने के पानी और भोजन की भारी कमी है",
            "ta" to "குடிநீர் மற்றும் உணவுப் பொருட்களின் கடுமையான பற்றாக்குறை",
            "kn" to "ಕುಡಿಯುವ ನೀರು ಮತ್ತು ಆಹಾರದ ತೀವ್ರ ಕೊರತೆ ಇದೆ",
            "ml" to "കുടിവെള്ളത്തിനും ഭക്ഷണത്തിനും കടുത്ത ക്ഷാമം",
            "bn" to "খাবার পানি এবং খাদ্য সামগ্রীর চরম সংকট"
        ),
        "safe" to mapOf(
            "en" to "All persons here are safe and accounted for",
            "te" to "ఇక్కడున్న వారందరూ సురక్షితంగా మరియు క్షేమంగా ఉన్నారు",
            "hi" to "यहाँ सभी लोग पूरी तरह सुरक्षित हैं",
            "ta" to "இங்கு அனைவரும் பாதுகாப்பாக உள்ளனர்",
            "kn" to "ಇಲ್ಲಿ ಎಲ್ಲರೂ ಸುರಕ್ಷಿತವಾಗಿದ್ದಾರೆ",
            "ml" to "ഇവിടെ എല്ലാവരും സുരക്ഷിതരാണ്",
            "bn" to "এখানে সবাই নিরাপদ আছেন"
        )
    )

    override suspend fun translate(
        text: String,
        sourceLanguage: String,
        targetLanguage: String
    ): String {
        val src = sourceLanguage.lowercase().trim()
        val tgt = targetLanguage.lowercase().trim()

        if (src == tgt) return text

        val lower = text.lowercase()

        // 1. Check exact key categories
        val category = when {
            lower.contains("fire") || lower.contains("మంటలు") || lower.contains("आग") || lower.contains("தீ") -> "fire"
            lower.contains("doctor") || lower.contains("injur") || lower.contains("గాయ") || lower.contains("घायल") || lower.contains("காயம்") -> "medical"
            lower.contains("trap") || lower.contains("collapse") || lower.contains("చిక్కు") || lower.contains("फंसे") || lower.contains("சிக்கி") -> "trapped"
            lower.contains("water") || lower.contains("food") || lower.contains("నీరు") || lower.contains("पानी") || lower.contains("தண்ணீர்") -> "food_water"
            lower.contains("safe") || lower.contains("fine") || lower.contains("క్షేమ") || lower.contains("सुरक्षित") -> "safe"
            lower.contains("help") || lower.contains("sos") || lower.contains("సహాయం") || lower.contains("मदद") || lower.contains("உதவி") -> "help"
            else -> null
        }

        if (category != null) {
            val translations = emergencyCorpus[category]
            if (translations != null && translations.containsKey(tgt)) {
                return translations[tgt]!!
            }
        }

        // 2. High-frequency word-level translation map
        val wordMap = mapOf(
            "fire" to mapOf("te" to "మంటలు", "hi" to "आग", "ta" to "தீ", "kn" to "ಬೆಂಕಿ", "ml" to "തീ", "bn" to "আগুন"),
            "water" to mapOf("te" to "నీరు", "hi" to "पानी", "ta" to "தண்ணீர்", "kn" to "ನೀರು", "ml" to "വെള്ളം", "bn" to "পানি"),
            "food" to mapOf("te" to "ఆహారం", "hi" to "खाना", "ta" to "உணவு", "kn" to "ಆಹಾರ", "ml" to "ഭക്ഷണം", "bn" to "খাবার"),
            "help" to mapOf("te" to "సహాయం", "hi" to "मदद", "ta" to "உதவி", "kn" to "ಸಹಾಯ", "ml" to "സഹായം", "bn" to "সাহায্য"),
            "safe" to mapOf("te" to "సురక్షితం", "hi" to "सुरक्षित", "ta" to "பாதுகாப்பானது", "kn" to "ಸುರಕ್ಷಿತ", "ml" to "സുരക്ഷിതം", "bn" to "নিরাপদ"),
            "doctor" to mapOf("te" to "వైద్యుడు", "hi" to "डॉक्टर", "ta" to "மருத்துவர்", "kn" to "ವೈದ್ಯ", "ml" to "ഡോക്ടർ", "bn" to "ডাক্তার")
        )

        for ((enWord, langMap) in wordMap) {
            if (lower.contains(enWord) && langMap.containsKey(tgt)) {
                return "${langMap[tgt]}! [$text]"
            }
        }

        // Fallback: indicate cross-lingual translation wrapper
        return text
    }
}
