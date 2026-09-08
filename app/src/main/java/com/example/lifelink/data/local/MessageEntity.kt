package com.example.lifelink.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.lifelink.data.ConnectivityZone
import com.example.lifelink.data.DeliveryStatus
import com.example.lifelink.data.EmergencyIntent
import com.example.lifelink.data.LifeLinkMessage
import com.example.lifelink.data.MessageDirection

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val senderId: String,
    val text: String,
    val language: String,
    val intent: String,
    val priority: Int,
    val timestamp: Long,
    val hopCount: Int,
    val ttl: Int,
    val pathString: String,
    val translatedText: String?,
    val targetLanguage: String?,
    val direction: String = MessageDirection.OUTGOING.name,
    val deliveryStatus: String = DeliveryStatus.OFFLINE_QUEUED.name,
    val connectivityZone: String = ConnectivityZone.OFFLINE_ZERO_BARS.name,
    val retryCount: Int = 0
) {
    fun toDomain(): LifeLinkMessage {
        val parsedIntent = try {
            EmergencyIntent.valueOf(intent)
        } catch (_: Exception) {
            EmergencyIntent.GENERAL
        }
        val parsedDirection = try {
            MessageDirection.valueOf(direction)
        } catch (_: Exception) {
            MessageDirection.OUTGOING
        }
        val parsedDeliveryStatus = try {
            DeliveryStatus.valueOf(deliveryStatus)
        } catch (_: Exception) {
            DeliveryStatus.OFFLINE_QUEUED
        }
        val parsedZone = try {
            ConnectivityZone.valueOf(connectivityZone)
        } catch (_: Exception) {
            ConnectivityZone.OFFLINE_ZERO_BARS
        }
        val pathList = if (pathString.isBlank()) emptyList() else pathString.split(" -> ")

        return LifeLinkMessage(
            id = id,
            senderId = senderId,
            text = text,
            language = language,
            intent = parsedIntent,
            priority = priority,
            timestamp = timestamp,
            hopCount = hopCount,
            ttl = ttl,
            path = pathList,
            translatedText = translatedText,
            targetLanguage = targetLanguage,
            direction = parsedDirection,
            deliveryStatus = parsedDeliveryStatus,
            connectivityZone = parsedZone,
            retryCount = retryCount
        )
    }

    companion object {
        fun fromDomain(msg: LifeLinkMessage): MessageEntity {
            return MessageEntity(
                id = msg.id,
                senderId = msg.senderId,
                text = msg.text,
                language = msg.language,
                intent = msg.intent.name,
                priority = msg.priority,
                timestamp = msg.timestamp,
                hopCount = msg.hopCount,
                ttl = msg.ttl,
                pathString = msg.path.joinToString(" -> "),
                translatedText = msg.translatedText,
                targetLanguage = msg.targetLanguage,
                direction = msg.direction.name,
                deliveryStatus = msg.deliveryStatus.name,
                connectivityZone = msg.connectivityZone.name,
                retryCount = msg.retryCount
            )
        }
    }
}
