package com.example.lifelink.data

enum class EmergencyIntent {
    FIRE,
    MEDICAL,
    HELP,
    TRAPPED,
    FOOD_WATER,
    SAFE,
    GENERAL;

    fun getDisplayName(): String = when (this) {
        FIRE -> "Fire / Smoke Hazard"
        MEDICAL -> "Medical / Critical Injury"
        HELP -> "Emergency Assistance"
        TRAPPED -> "Trapped / Structural Collapse"
        FOOD_WATER -> "Rations / Clean Water"
        SAFE -> "Status: Safe / Evacuated"
        GENERAL -> "General Emergency Broadcast"
    }

    fun getIconEmoji(): String = when (this) {
        FIRE -> "🔥"
        MEDICAL -> "🚑"
        HELP -> "🆘"
        TRAPPED -> "🏚️"
        FOOD_WATER -> "💧"
        SAFE -> "✅"
        GENERAL -> "📢"
    }
}

data class LifeLinkMessage(
    val id: String,
    val senderId: String,
    val text: String,
    val language: String,
    val intent: EmergencyIntent,
    val priority: Int,
    val timestamp: Long,
    val hopCount: Int = 0,
    val ttl: Int = 5,
    val path: List<String> = emptyList(),
    val translatedText: String? = null,
    val targetLanguage: String? = null
)
