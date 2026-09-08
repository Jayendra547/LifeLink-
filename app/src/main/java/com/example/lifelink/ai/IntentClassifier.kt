package com.example.lifelink.ai

import com.example.lifelink.data.EmergencyIntent

class IntentClassifier {

    fun classify(text: String): EmergencyIntent {
        val input = text.lowercase()

        return when {
            input.containsAny(
                "fire", "flames", "burning", "smoke", "blaze", "explosion",
                "మంటలు", "నిప్పు", "పొగ", // Telugu
                "आग", "धुआं", "जल रहा", // Hindi
                "தீ", "புகை", "எரிகிறது", // Tamil
                "ಬೆಂಕಿ", "ಹೊಗೆ" // Kannada
            ) -> EmergencyIntent.FIRE

            input.containsAny(
                "doctor", "injured", "injury", "bleeding", "medical", "ambulance", "heart attack", "unconscious", "fracture",
                "గాయం", "రక్తం", "వైద్యుడు", "ఆసుపత్రి", // Telugu
                "चोट", "खून", "डॉक्टर", "अस्पताल", "एम्बुलेंस", // Hindi
                "காயம்", "மருத்துவர்", "ரத்தம்", // Tamil
                "ಗಾಯ", "ವೈದ್ಯ", "ರಕ್ತ" // Kannada
            ) -> EmergencyIntent.MEDICAL

            input.containsAny(
                "trapped", "stuck", "cannot get out", "collapse", "debris", "rubble", "under building",
                "చిక్కుకున్నాము", "బయటకు రాలేకపోతున్నాము", // Telugu
                "फंसे हुए", "दबे हुए", "मलबे", "बाहर नहीं निकल सकते", // Hindi
                "சிக்கியுள்ளோம்", "வெளியே வர முடியவில்லை", // Tamil
                "ಸಿಲುಕಿಕೊಂಡಿದ್ದೇವೆ" // Kannada
            ) -> EmergencyIntent.TRAPPED

            input.containsAny(
                "help", "save me", "save us", "emergency", "danger", "urgent", "sos",
                "సహాయం", "కాపాడండి", "రక్షించండి", // Telugu
                "मदद", "बचाओ", "आपातकाल", // Hindi
                "உதவி", "காப்பாற்றுங்கள்", // Tamil
                "ಸಹಾಯ", "ಕಾಪಾಡಿ" // Kannada
            ) -> EmergencyIntent.HELP

            input.containsAny(
                "food", "water", "thirsty", "hungry", "rations", "starving", "drinking water",
                "ఆహారం", "మంచినీరు", "దాహం", "ఆకలి", // Telugu
                "खाना", "पानी", "प्यास", "भूख", "राशन", // Hindi
                "உணவு", "தண்ணீர்", "தாகம்", // Tamil
                "ಆಹಾರ", "ನೀರು" // Kannada
            ) -> EmergencyIntent.FOOD_WATER

            input.containsAny(
                "safe", "i am safe", "we are safe", "unhurt", "ok", "fine", "evacuated",
                "సురక్షితంగా ఉన్నాము", "క్షేమంగా ఉన్నాము", // Telugu
                "सुरक्षित", "ठीक हैं", "बच गए", // Hindi
                "பாதுகாப்பாக", "நலமாக", // Tamil
                "ಸುರಕ್ಷಿತ" // Kannada
            ) -> EmergencyIntent.SAFE

            else -> EmergencyIntent.GENERAL
        }
    }

    private fun String.containsAny(vararg words: String): Boolean {
        return words.any { this.contains(it, ignoreCase = true) }
    }

    fun priority(intent: EmergencyIntent): Int {
        return when (intent) {
            EmergencyIntent.FIRE -> 5
            EmergencyIntent.MEDICAL -> 5
            EmergencyIntent.TRAPPED -> 5
            EmergencyIntent.HELP -> 4
            EmergencyIntent.FOOD_WATER -> 2
            EmergencyIntent.SAFE -> 1
            EmergencyIntent.GENERAL -> 1
        }
    }
}
