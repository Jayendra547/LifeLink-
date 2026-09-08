package com.example.lifelink.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE priority >= :minPriority ORDER BY timestamp DESC")
    fun getHighPriorityMessages(minPriority: Int = 4): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE direction = :direction ORDER BY timestamp DESC")
    fun getMessagesByDirection(direction: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE direction = 'OUTGOING' AND deliveryStatus = 'OFFLINE_QUEUED' ORDER BY timestamp ASC")
    fun getPendingOutgoingQueue(): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE direction = 'OUTGOING' AND deliveryStatus = 'OFFLINE_QUEUED' ORDER BY timestamp ASC")
    suspend fun getPendingOutgoingList(): List<MessageEntity>

    @Query("SELECT COUNT(*) FROM messages")
    fun getTotalMessageCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE direction = 'OUTGOING' AND deliveryStatus = 'OFFLINE_QUEUED'")
    fun getPendingOutgoingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE direction = 'OUTGOING' AND deliveryStatus = 'DELIVERED_MESH'")
    fun getDeliveredOutgoingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE direction = 'INCOMING'")
    fun getIncomingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE id = :id")
    suspend fun updateDeliveryStatus(id: String, status: String)

    @Query("UPDATE messages SET retryCount = retryCount + 1 WHERE id = :id")
    suspend fun incrementRetryCount(id: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessageById(id: String)

    @Query("DELETE FROM messages")
    suspend fun clearAll()
}
