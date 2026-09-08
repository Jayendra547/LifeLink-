package com.example.lifelink

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.lifelink.data.ConnectivityZone
import com.example.lifelink.data.DeliveryStatus
import com.example.lifelink.data.EmergencyIntent
import com.example.lifelink.data.LifeLinkMessage
import com.example.lifelink.data.MessageDirection
import com.example.lifelink.data.local.LifeLinkDatabase
import com.example.lifelink.data.local.RoomDatabaseManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomDatabaseManagerTest {

    private lateinit var db: LifeLinkDatabase
    private lateinit var manager: RoomDatabaseManager

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LifeLinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = RoomDatabaseManager(db.messageDao())
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun testStoreOutgoingMessageInOfflineZone() = runBlocking {
        val outgoingMsg = LifeLinkMessage(
            id = "out-101",
            senderId = "Phone A",
            text = "Severe fire on floor 2, need immediate help",
            language = "en",
            intent = EmergencyIntent.FIRE,
            priority = 5,
            timestamp = System.currentTimeMillis()
        )

        val saved = manager.storeOutgoingMessage(outgoingMsg, ConnectivityZone.OFFLINE_ZERO_BARS)

        assertEquals(MessageDirection.OUTGOING, saved.direction)
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, saved.deliveryStatus)
        assertEquals(ConnectivityZone.OFFLINE_ZERO_BARS, saved.connectivityZone)

        val allMessages = manager.allMessages.first()
        assertEquals(1, allMessages.size)
        assertEquals("out-101", allMessages[0].id)
        assertEquals(DeliveryStatus.OFFLINE_QUEUED, allMessages[0].deliveryStatus)

        val pending = manager.pendingOutgoingQueue.first()
        assertEquals(1, pending.size)
        assertEquals("out-101", pending[0].id)
    }

    @Test
    fun testStoreIncomingMessageWhileOffline() = runBlocking {
        val incomingMsg = LifeLinkMessage(
            id = "inc-202",
            senderId = "Phone C",
            text = "Rescue team dispatched to sector 4",
            language = "en",
            intent = EmergencyIntent.HELP,
            priority = 4,
            timestamp = System.currentTimeMillis()
        )

        val saved = manager.storeIncomingMessage(incomingMsg, ConnectivityZone.LOW_CONNECTIVITY_EDGE)

        assertEquals(MessageDirection.INCOMING, saved.direction)
        assertEquals(DeliveryStatus.RECEIVED_OFFLINE, saved.deliveryStatus)

        val all = manager.allMessages.first()
        assertEquals(1, all.size)
        assertEquals(MessageDirection.INCOMING, all[0].direction)
        assertEquals(DeliveryStatus.RECEIVED_OFFLINE, all[0].deliveryStatus)
    }

    @Test
    fun testSyncPendingOutgoingQueue() = runBlocking {
        val msg1 = LifeLinkMessage(
            id = "queue-1",
            senderId = "Phone A",
            text = "Medical emergency",
            language = "en",
            intent = EmergencyIntent.MEDICAL,
            priority = 5,
            timestamp = 1000L
        )
        val msg2 = LifeLinkMessage(
            id = "queue-2",
            senderId = "Phone A",
            text = "Need clean water",
            language = "en",
            intent = EmergencyIntent.FOOD_WATER,
            priority = 3,
            timestamp = 2000L
        )

        manager.storeOutgoingMessage(msg1, ConnectivityZone.OFFLINE_ZERO_BARS)
        manager.storeOutgoingMessage(msg2, ConnectivityZone.OFFLINE_ZERO_BARS)

        assertEquals(2, manager.pendingOutgoingQueue.first().size)

        val transmittedIds = mutableListOf<String>()
        val syncedCount = manager.syncPendingOutgoingQueue { packet ->
            transmittedIds.add(packet.id)
            true
        }

        assertEquals(2, syncedCount)
        assertEquals(listOf("queue-1", "queue-2"), transmittedIds)
        assertTrue(manager.pendingOutgoingQueue.first().isEmpty())

        val all = manager.allMessages.first()
        assertTrue(all.all { it.deliveryStatus == DeliveryStatus.DELIVERED_MESH })
    }
}
