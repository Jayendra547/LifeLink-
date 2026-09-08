package com.example.lifelink.data.local

import com.example.lifelink.data.ConnectivityZone
import com.example.lifelink.data.DeliveryStatus
import com.example.lifelink.data.LifeLinkMessage
import com.example.lifelink.data.MessageDirection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class OfflineStorageStats(
    val totalStoredCount: Int = 0,
    val outgoingQueuedCount: Int = 0,
    val incomingOfflineCount: Int = 0,
    val deliveredCount: Int = 0
)

class RoomDatabaseManager(
    private val dao: MessageDao
) {
    val allMessages: Flow<List<LifeLinkMessage>> = dao.getAllMessages().map { list ->
        list.map { it.toDomain() }
    }

    val pendingOutgoingQueue: Flow<List<LifeLinkMessage>> = dao.getPendingOutgoingQueue().map { list ->
        list.map { it.toDomain() }
    }

    val storageStats: Flow<OfflineStorageStats> = combine(
        dao.getTotalMessageCount(),
        dao.getPendingOutgoingCount(),
        dao.getIncomingCount(),
        dao.getDeliveredOutgoingCount()
    ) { total, pending, incoming, delivered ->
        OfflineStorageStats(
            totalStoredCount = total,
            outgoingQueuedCount = pending,
            incomingOfflineCount = incoming,
            deliveredCount = delivered
        )
    }

    suspend fun storeOutgoingMessage(
        message: LifeLinkMessage,
        zone: ConnectivityZone
    ): LifeLinkMessage = withContext(Dispatchers.IO) {
        val initialStatus = if (zone == ConnectivityZone.MESH_HOP_CONNECTED) {
            DeliveryStatus.DELIVERED_MESH
        } else {
            DeliveryStatus.OFFLINE_QUEUED
        }

        val enriched = message.copy(
            direction = MessageDirection.OUTGOING,
            deliveryStatus = initialStatus,
            connectivityZone = zone
        )
        dao.insertMessage(MessageEntity.fromDomain(enriched))
        enriched
    }

    suspend fun storeIncomingMessage(
        message: LifeLinkMessage,
        zone: ConnectivityZone
    ): LifeLinkMessage = withContext(Dispatchers.IO) {
        val enriched = message.copy(
            direction = MessageDirection.INCOMING,
            deliveryStatus = DeliveryStatus.RECEIVED_OFFLINE,
            connectivityZone = zone
        )
        dao.insertMessage(MessageEntity.fromDomain(enriched))
        enriched
    }

    suspend fun syncPendingOutgoingQueue(
        transmitAction: suspend (LifeLinkMessage) -> Boolean
    ): Int = withContext(Dispatchers.IO) {
        val pending = dao.getPendingOutgoingList()
        var syncedSuccessCount = 0

        for (item in pending) {
            dao.updateDeliveryStatus(item.id, DeliveryStatus.TRANSMITTING_MESH.name)
            val domainMsg = item.toDomain().copy(deliveryStatus = DeliveryStatus.TRANSMITTING_MESH)
            val success = try {
                transmitAction(domainMsg)
            } catch (_: Exception) {
                false
            }

            if (success) {
                dao.updateDeliveryStatus(item.id, DeliveryStatus.DELIVERED_MESH.name)
                syncedSuccessCount++
            } else {
                dao.incrementRetryCount(item.id)
                dao.updateDeliveryStatus(item.id, DeliveryStatus.OFFLINE_QUEUED.name)
            }
        }
        syncedSuccessCount
    }

    suspend fun markDelivered(messageId: String) = withContext(Dispatchers.IO) {
        dao.updateDeliveryStatus(messageId, DeliveryStatus.DELIVERED_MESH.name)
    }

    suspend fun deleteMessage(id: String) = withContext(Dispatchers.IO) {
        dao.deleteMessageById(id)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }
}
