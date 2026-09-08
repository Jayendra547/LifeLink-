package com.example.lifelink

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.lifelink.data.ConnectivityZone
import com.example.lifelink.data.DeliveryStatus
import com.example.lifelink.data.EmergencyIntent
import com.example.lifelink.data.LifeLinkMessage
import com.example.lifelink.data.MessageDirection
import com.example.lifelink.data.local.LifeLinkDatabase
import com.example.lifelink.data.local.RoomDatabaseManager
import com.example.lifelink.ui.RoomStoredMessagesList
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RoomStoredMessagesListTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: LifeLinkDatabase
    private lateinit var manager: RoomDatabaseManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, LifeLinkDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = RoomDatabaseManager(db.messageDao())
    }

    @After
    fun tearDown() {
        // In-memory database will be garbage collected automatically; avoiding closing connection pool
        // while active Compose observation flows are terminating.
    }

    @Test
    fun testObservesEmptyRoomDatabaseFlow() {
        composeTestRule.setContent {
            RoomStoredMessagesList(
                messagesFlow = manager.allMessages,
                isScrollable = false
            )
        }

        composeTestRule.onNodeWithTag("room_stored_messages_list").assertExists()
        composeTestRule.onNodeWithTag("room_empty_state_card").assertExists()
        composeTestRule.onNodeWithText("No Messages in Local Room DB").assertExists()
    }

    @Test
    fun testObservesRealtimeRoomDatabaseEmissions() {
        runBlocking {
            // Initially set content observing manager.allMessages
            composeTestRule.setContent {
                RoomStoredMessagesList(
                    messagesFlow = manager.allMessages,
                    isScrollable = false
                )
            }

            // Verify initial empty state
            composeTestRule.onNodeWithTag("room_empty_state_card").assertExists()

            // Insert message into Room database
            val msg = LifeLinkMessage(
                id = "msg-flow-test-1",
                senderId = "Phone A",
                text = "Emergency fire outbreak in hospital block B",
                translatedText = "अस्पताल ब्लॉक बी में आग लग गई है",
                language = "en",
                targetLanguage = "hi",
                intent = EmergencyIntent.FIRE,
                priority = 5,
                direction = MessageDirection.OUTGOING,
                deliveryStatus = DeliveryStatus.OFFLINE_QUEUED,
                connectivityZone = ConnectivityZone.OFFLINE_ZERO_BARS,
                timestamp = System.currentTimeMillis()
            )
            manager.storeOutgoingMessage(msg, ConnectivityZone.OFFLINE_ZERO_BARS)

            // Wait for Compose to process the Room database Flow emission
            composeTestRule.waitUntil(timeoutMillis = 5000) {
                composeTestRule.onAllNodesWithTag("room_message_card_msg-flow-test-1")
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Card should now be rendered in the Composable list UI in real-time
            composeTestRule.onNodeWithTag("room_message_card_msg-flow-test-1").assertExists()
            composeTestRule.onNodeWithText("Emergency fire outbreak in hospital block B").assertExists()
            composeTestRule.onNodeWithText("ROOM QUEUED").assertExists()
            composeTestRule.onNodeWithText("P5").assertExists()
            composeTestRule.onNodeWithText("FIRE").assertExists()
        }
    }

    @Test
    fun testRoomListActionsTriggerCallbacks() {
        runBlocking {
            var playedMessageId = ""
            var inspectedMessageId = ""
            var deletedMessageId = ""

            val testMsg = LifeLinkMessage(
                id = "action-msg-123",
                senderId = "Phone B",
                text = "Rescue boat approaching north gate",
                language = "en",
                intent = EmergencyIntent.HELP,
                priority = 4,
                direction = MessageDirection.INCOMING,
                deliveryStatus = DeliveryStatus.RECEIVED_OFFLINE,
                connectivityZone = ConnectivityZone.LOW_CONNECTIVITY_EDGE,
                timestamp = System.currentTimeMillis()
            )
            manager.storeIncomingMessage(testMsg, ConnectivityZone.LOW_CONNECTIVITY_EDGE)

            composeTestRule.setContent {
                RoomStoredMessagesList(
                    messagesFlow = manager.allMessages,
                    isScrollable = false,
                    onPlayTts = { playedMessageId = it.id },
                    onInspect = { inspectedMessageId = it.id },
                    onDelete = { deletedMessageId = it }
                )
            }

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("room_message_card_action-msg-123").assertExists()
            composeTestRule.onNodeWithText("ROOM SAVED").assertExists()
            composeTestRule.onNodeWithText("Rescue boat approaching north gate").assertExists()
        }
    }

    @Test
    fun testVisualStatusIndicatorsForPendingSentReceived() {
        runBlocking {
            // 1. Pending message
            val pendingMsg = LifeLinkMessage(
                id = "msg-pending-1",
                senderId = "User A",
                text = "Pending message waiting for mesh connection",
                language = "en",
                intent = EmergencyIntent.HELP,
                priority = 4,
                direction = MessageDirection.OUTGOING,
                deliveryStatus = DeliveryStatus.OFFLINE_QUEUED,
                connectivityZone = ConnectivityZone.OFFLINE_ZERO_BARS,
                timestamp = 1000L
            )
            // 2. Sent message
            val sentMsg = LifeLinkMessage(
                id = "msg-sent-2",
                senderId = "User A",
                text = "Sent message transmitted via mesh network",
                language = "en",
                intent = EmergencyIntent.SAFE,
                priority = 2,
                direction = MessageDirection.OUTGOING,
                deliveryStatus = DeliveryStatus.DELIVERED_MESH,
                connectivityZone = ConnectivityZone.MESH_HOP_CONNECTED,
                timestamp = 2000L
            )
            // 3. Received message
            val receivedMsg = LifeLinkMessage(
                id = "msg-received-3",
                senderId = "User B",
                text = "Received message incoming from peer relay",
                language = "en",
                intent = EmergencyIntent.FOOD_WATER,
                priority = 3,
                direction = MessageDirection.INCOMING,
                deliveryStatus = DeliveryStatus.RECEIVED_OFFLINE,
                connectivityZone = ConnectivityZone.LOW_CONNECTIVITY_EDGE,
                timestamp = 3000L
            )

            manager.storeOutgoingMessage(pendingMsg, ConnectivityZone.OFFLINE_ZERO_BARS)
            manager.storeOutgoingMessage(sentMsg, ConnectivityZone.MESH_HOP_CONNECTED)
            manager.storeIncomingMessage(receivedMsg, ConnectivityZone.LOW_CONNECTIVITY_EDGE)

            composeTestRule.setContent {
                RoomStoredMessagesList(
                    messagesFlow = manager.allMessages,
                    isScrollable = false
                )
            }

            composeTestRule.waitForIdle()

            // Verify status summary pills
            composeTestRule.onNodeWithTag("status_summary_pending").assertIsDisplayed()
            composeTestRule.onNodeWithTag("status_summary_sent").assertIsDisplayed()
            composeTestRule.onNodeWithTag("status_summary_received").assertIsDisplayed()

            // Verify visual badges and indicators exist on each card
            composeTestRule.onNodeWithTag("status_indicator_pending_msg-pending-1").assertExists()
            composeTestRule.onNodeWithTag("status_indicator_sent_msg-sent-2").assertExists()
            composeTestRule.onNodeWithTag("status_indicator_received_msg-received-3").assertExists()

            // Verify trailing status icons on each card
            composeTestRule.onNodeWithTag("timestamp_status_icon_pending_msg-pending-1").assertExists()
            composeTestRule.onNodeWithTag("timestamp_status_icon_sent_msg-sent-2").assertExists()
            composeTestRule.onNodeWithTag("timestamp_status_icon_received_msg-received-3").assertExists()

            // Test filtering by Pending
            composeTestRule.onNodeWithTag("room_filter_pending").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("room_message_card_msg-pending-1").assertExists()
            composeTestRule.onNodeWithTag("room_message_card_msg-sent-2").assertDoesNotExist()
            composeTestRule.onNodeWithTag("room_message_card_msg-received-3").assertDoesNotExist()

            // Test filtering by Sent
            composeTestRule.onNodeWithTag("room_filter_sent").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("room_message_card_msg-sent-2").assertExists()
            composeTestRule.onNodeWithTag("room_message_card_msg-pending-1").assertDoesNotExist()
            composeTestRule.onNodeWithTag("room_message_card_msg-received-3").assertDoesNotExist()

            // Test filtering by Received
            composeTestRule.onNodeWithTag("room_filter_received").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("room_message_card_msg-received-3").assertExists()
            composeTestRule.onNodeWithTag("room_message_card_msg-pending-1").assertDoesNotExist()
            composeTestRule.onNodeWithTag("room_message_card_msg-sent-2").assertDoesNotExist()

            // Reset back to All
            composeTestRule.onNodeWithTag("room_filter_all").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("room_message_card_msg-pending-1").assertExists()
            composeTestRule.onNodeWithTag("room_message_card_msg-sent-2").assertExists()
            composeTestRule.onNodeWithTag("room_message_card_msg-received-3").assertExists()
        }
    }
}
