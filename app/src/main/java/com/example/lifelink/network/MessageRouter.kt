package com.example.lifelink.network

import com.example.lifelink.data.LifeLinkMessage

class MessageRouter {

    private val seenMessages = mutableSetOf<String>()

    fun shouldForward(message: LifeLinkMessage): Boolean {
        if (message.id in seenMessages) {
            return false
        }

        if (message.ttl <= 0) {
            return false
        }

        seenMessages.add(message.id)
        return true
    }

    fun prepareRelay(message: LifeLinkMessage, relayNodeId: String = "RelayNode"): LifeLinkMessage {
        val updatedPath = message.path.toMutableList()
        if (!updatedPath.contains(relayNodeId)) {
            updatedPath.add(relayNodeId)
        }
        return message.copy(
            hopCount = message.hopCount + 1,
            ttl = message.ttl - 1,
            path = updatedPath
        )
    }

    fun markSeen(messageId: String) {
        seenMessages.add(messageId)
    }

    fun clearSeen() {
        seenMessages.clear()
    }
}
