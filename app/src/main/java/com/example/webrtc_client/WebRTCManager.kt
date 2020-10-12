package com.example.webrtc_client

import com.example.webrtc_client.connection.PeerConnectionManager.PeerConnectionManager
import com.example.webrtc_client.socket.JavaWebSocket.JavaWebSocket

class WebRTCManager {
    private lateinit var webSocket: JavaWebSocket
    private lateinit var peerConnectionManager: PeerConnectionManager

    companion object {
        val instance: WebRTCManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            WebRTCManager()
        }
    }

    fun connect(activity: MainActivity, roomId: String) {
        webSocket = JavaWebSocket(activity)
        peerConnectionManager = PeerConnectionManager()
        webSocket.connect("wss://ipaddress/wss")
    }
}