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

enum class MessageDirection {
    OUTGOING,
    INCOMING
}

enum class DeliveryStatus {
    OFFLINE_QUEUED,
    TRANSMITTING_MESH,
    DELIVERED_MESH,
    RECEIVED_OFFLINE
}

enum class ConnectivityZone {
    OFFLINE_ZERO_BARS,
    LOW_CONNECTIVITY_EDGE,
    MESH_HOP_CONNECTED;

    fun getDisplayName(): String = when (this) {
        OFFLINE_ZERO_BARS -> "Zero Connectivity (Offline Zone)"
        LOW_CONNECTIVITY_EDGE -> "Low-Connectivity (Intermittent Edge)"
        MESH_HOP_CONNECTED -> "Mesh Relay Connected"
    }

    fun getShortLabel(): String = when (this) {
        OFFLINE_ZERO_BARS -> "Offline (0-Bars)"
        LOW_CONNECTIVITY_EDGE -> "Low Signal (Edge)"
        MESH_HOP_CONNECTED -> "Mesh Connected"
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
    val targetLanguage: String? = null,
    val direction: MessageDirection = MessageDirection.OUTGOING,
    val deliveryStatus: DeliveryStatus = DeliveryStatus.OFFLINE_QUEUED,
    val connectivityZone: ConnectivityZone = ConnectivityZone.OFFLINE_ZERO_BARS,
    val retryCount: Int = 0
)
