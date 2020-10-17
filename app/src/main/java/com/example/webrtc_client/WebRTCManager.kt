package com.example.webrtc_client

import com.example.webrtc_client.connection.PeerConnectionManager.PeerConnectionManager
import com.example.webrtc_client.socket.JavaWebSocket.JavaWebSocket
import org.webrtc.EglBase

class WebRTCManager private constructor() {
    private lateinit var webSocket: JavaWebSocket
    private lateinit var roomId: String
    private lateinit var peerConnectionManager: PeerConnectionManager

    companion object {
        val instance: WebRTCManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            WebRTCManager()
        }
    }

    fun connect(activity: MainActivity, roomId: String) {
        peerConnectionManager = PeerConnectionManager.instance
        webSocket = JavaWebSocket(activity)
        this.roomId = roomId
        webSocket.connect("wss://8.210.69.115/wss")
    }

    fun joinRoom(chatRoomActivity: ChatRoomActivity, rootEglBase: EglBase) {
        peerConnectionManager.initContext(chatRoomActivity.baseContext, rootEglBase)
        webSocket.joinRoom(roomId)
    }
}