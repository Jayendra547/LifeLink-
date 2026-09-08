package com.example.lifelink.network

interface Transport {
    fun start()
    fun stop()
    fun broadcast(data: ByteArray)
    fun onMessageReceived(callback: (ByteArray) -> Unit)
}
