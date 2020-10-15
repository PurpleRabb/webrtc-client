package com.example.webrtc_client

import com.example.webrtc_client.connection.PeerConnectionManager.PeerConnectionManager
import com.example.webrtc_client.socket.JavaWebSocket.JavaWebSocket

class WebRTCManager {
    private lateinit var webSocket: JavaWebSocket
    private lateinit var peerConnectionManager: PeerConnectionManager
    private lateinit var roomId : String

    companion object {
        val instance: WebRTCManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            WebRTCManager()
        }
    }

    fun connect(activity: MainActivity, roomId: String) {
        webSocket = JavaWebSocket(activity)
        this.roomId = roomId
        peerConnectionManager = PeerConnectionManager()
        webSocket.connect("wss://8.210.69.115/wss")
    }

    fun joinRoom(chatRoomActivity: ChatRoomActivity) {
        webSocket.joinRoom(roomId)
    }
}