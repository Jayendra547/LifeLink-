package com.example.lifelink.network

import com.example.lifelink.data.LifeLinkMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MeshNode(
    val id: String,
    val name: String,
    val role: String,
    val signalDbm: Int,
    val isOnline: Boolean = true
)

data class PacketHopEvent(
    val packetId: String,
    val fromNode: String,
    val toNode: String,
    val hopNumber: Int,
    val latencyMs: Long,
    val intentName: String,
    val priority: Int
)

class MeshNetworkSimulator(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : Transport {

    private var messageListener: ((ByteArray) -> Unit)? = null
    private val router = MessageRouter()

    private val _activeNodes = MutableStateFlow(
        listOf(
            MeshNode("Phone_A", "Phone A", "Ground Zero / Victim", -48),
            MeshNode("Phone_B", "Phone B", "Relay Bridge / Drone", -62),
            MeshNode("Phone_C", "Phone C", "Incident Command / Rescue", -75)
        )
    )
    val activeNodes: StateFlow<List<MeshNode>> = _activeNodes.asStateFlow()

    private val _currentHopEvent = MutableStateFlow<PacketHopEvent?>(null)
    val currentHopEvent: StateFlow<PacketHopEvent?> = _currentHopEvent.asStateFlow()

    private val _isSimulatingTransit = MutableStateFlow(false)
    val isSimulatingTransit: StateFlow<Boolean> = _isSimulatingTransit.asStateFlow()

    private var isRunning = false

    override fun start() {
        isRunning = true
    }

    override fun stop() {
        isRunning = false
    }

    override fun broadcast(data: ByteArray) {
        if (!isRunning) return

        scope.launch {
            try {
                val originalMessage = MessageCodec.decode(data)
                simulateMultiHopTransmission(originalMessage)
            } catch (_: Exception) {
                // Raw payload fallback
                messageListener?.invoke(data)
            }
        }
    }

    override fun onMessageReceived(callback: (ByteArray) -> Unit) {
        this.messageListener = callback
    }

    /**
     * Simulates the multi-hop transmission:
     * Phone A (Origin) -> Phone B (Relay 1) -> Phone C (Destination Receiver)
     */
    private suspend fun simulateMultiHopTransmission(message: LifeLinkMessage) {
        _isSimulatingTransit.value = true

        // Hop 1: Phone A -> Phone B
        _currentHopEvent.value = PacketHopEvent(
            packetId = message.id,
            fromNode = message.senderId.ifBlank { "Phone A" },
            toNode = "Phone B",
            hopNumber = 1,
            latencyMs = 64,
            intentName = message.intent.name,
            priority = message.priority
        )
        delay(700)

        // Relay node prepares packet
        val relayPacket = router.prepareRelay(message, "Phone B")

        // Hop 2: Phone B -> Phone C
        _currentHopEvent.value = PacketHopEvent(
            packetId = message.id,
            fromNode = "Phone B",
            toNode = "Phone C",
            hopNumber = 2,
            latencyMs = 82,
            intentName = message.intent.name,
            priority = message.priority
        )
        delay(700)

        // Arrived at Phone C
        val finalPacket = router.prepareRelay(relayPacket, "Phone C")
        val finalEncoded = MessageCodec.encode(finalPacket)

        _isSimulatingTransit.value = false
        messageListener?.invoke(finalEncoded)
    }

    fun injectTestMessage(message: LifeLinkMessage) {
        scope.launch {
            simulateMultiHopTransmission(message)
        }
    }
}
