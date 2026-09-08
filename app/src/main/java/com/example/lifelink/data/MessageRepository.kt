package com.example.lifelink.data

import com.example.lifelink.data.local.MessageDao
import com.example.lifelink.data.local.OfflineStorageStats
import com.example.lifelink.data.local.RoomDatabaseManager
import kotlinx.coroutines.flow.Flow

class MessageRepository(private val dao: MessageDao) {
    val dbManager = RoomDatabaseManager(dao)

    val allMessages: Flow<List<LifeLinkMessage>> = dbManager.allMessages
    val pendingQueue: Flow<List<LifeLinkMessage>> = dbManager.pendingOutgoingQueue
    val storageStats: Flow<OfflineStorageStats> = dbManager.storageStats

    suspend fun storeOutgoingMessage(message: LifeLinkMessage, zone: ConnectivityZone): LifeLinkMessage {
        return dbManager.storeOutgoingMessage(message, zone)
    }

    suspend fun storeIncomingMessage(message: LifeLinkMessage, zone: ConnectivityZone): LifeLinkMessage {
        return dbManager.storeIncomingMessage(message, zone)
    }

    suspend fun syncPendingOutgoingQueue(transmitAction: suspend (LifeLinkMessage) -> Boolean): Int {
        return dbManager.syncPendingOutgoingQueue(transmitAction)
    }

    suspend fun saveMessage(message: LifeLinkMessage) {
        dbManager.storeOutgoingMessage(message, ConnectivityZone.MESH_HOP_CONNECTED)
    }

    suspend fun deleteMessage(id: String) {
        dbManager.deleteMessage(id)
    }

    suspend fun clearHistory() {
        dbManager.clearAll()
    }
}
