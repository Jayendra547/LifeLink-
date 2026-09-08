package com.example.lifelink.data

import com.example.lifelink.data.local.MessageDao
import com.example.lifelink.data.local.MessageEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MessageRepository(private val dao: MessageDao) {
    val allMessages: Flow<List<LifeLinkMessage>> = dao.getAllMessages().map { list ->
        list.map { it.toDomain() }
    }

    val highPriorityMessages: Flow<List<LifeLinkMessage>> = dao.getHighPriorityMessages().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun saveMessage(message: LifeLinkMessage) {
        dao.insertMessage(MessageEntity.fromDomain(message))
    }

    suspend fun deleteMessage(id: String) {
        dao.deleteMessageById(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }
}
