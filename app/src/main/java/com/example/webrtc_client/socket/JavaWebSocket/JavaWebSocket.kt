package com.example.webrtc_client.socket.JavaWebSocket

import android.R.attr
import android.util.Log
import com.alibaba.fastjson.JSON
import com.alibaba.fastjson.JSONArray
import com.alibaba.fastjson.JSONObject
import com.alibaba.fastjson.serializer.SerializerFeature
import com.example.webrtc_client.ChatRoomActivity
import com.example.webrtc_client.MainActivity
import com.example.webrtc_client.connection.PeerConnectionManager.PeerConnectionManager
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class JavaWebSocket(activity: MainActivity) {
    private lateinit var mWebSocketClient: WebSocketClient
    private var mAcitivity: MainActivity = activity //小心内存泄漏
    private val TAG = "JavaWebSocket"
    private lateinit var peerConnectionManager: PeerConnectionManager

    fun connect(wss: String) {
        peerConnectionManager = PeerConnectionManager.instance
        var uri: URI = URI(wss)
        mWebSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.i(TAG, "onOpen")
                ChatRoomActivity.openActivity(mAcitivity)
            }

            override fun onMessage(message: String?) {
                Log.i(TAG, "onMessage:$message")
                handleMessage(message)
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.i(TAG, "onClose")
            }

            override fun onError(ex: Exception?) {
                Log.i(TAG, "onError")
            }

        }

        if (wss.startsWith("wss")) {
            var sslContext: SSLContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(TrustManagerTest()), SecureRandom())
            var factory = sslContext?.socketFactory
            if (factory != null) {
                mWebSocketClient.socket = factory.createSocket()
                mWebSocketClient.connect()
            }
        }
    }

    private fun handleMessage(message: String?) {
        //将json转换成map
        var map = JSON.parseObject(message, Map::class.java)
        var eventName: String = map["eventName"] as String
        if (eventName.equals("_peers")) {
            handleJoinRoom(map)
        }

        //对方响应
        if (eventName.equals("_ice_candidate")) {
            handleRemoteCandidate(map)
        }
    }

    private fun handleRemoteCandidate(map: Map<*, *>?) {
        var data = map?.get("data") as Map<*, *>
        var socketId: String
        if (data != null) {
            socketId = data.get("socketId") as String
            var sdpMid = data.get("id") as String
            if (sdpMid == null) {
                sdpMid = "video"
            }
            val sdpMLineIndex = java.lang.String.valueOf(data["label"]).toDouble() as Int
            val candidate = data["candidate"] as String
            val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
            peerConnectionManager.onRemoteIceCandidate(socketId, iceCandidate)
        }
    }

    private fun handleJoinRoom(map: Map<*, *>?) {
        var data: Map<*, *> = map?.get("data") as Map<*, *>
        var arr: JSONArray = data?.get("connections") as JSONArray
        var js: String = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName)
        var connections: ArrayList<String> = JSONObject.parseArray(js, String::class.java)
                as ArrayList<String> //房间里已经存在的链接数
        var myId: String = data["you"] as String
        peerConnectionManager.joinToRoom(this, connections, myId, true)
    }


    fun joinRoom(roomId: String) {
        //给服务器发送json
        val map: MutableMap<String, Any> = HashMap()
        map["eventName"] = "__join"
        val childMap: MutableMap<String, String> = HashMap()
        childMap["room"] = roomId
        map["data"] = childMap
        val jsonObject = JSONObject(map)
        val jsonString: String = jsonObject.toString()
        Log.d(TAG, "send-->$jsonString")
        mWebSocketClient.send(jsonString)
    }

    fun sendOffer(socketId: String, sdp: SessionDescription?) {
        var childMap1: HashMap<String, Any> = HashMap()
        childMap1.put("type", "offer")
        if (sdp != null) {
            childMap1.put("sdp", sdp)
        }

        var childMap2: HashMap<String, Any> = HashMap()
        childMap2.put("socketId", socketId)
        childMap2.put("sdp", childMap1)

        var map: HashMap<String, Any> = HashMap()
        map.put("eventName", "__offer")
        map.put("data", childMap2)

        var job = JSONObject(map)
        var jsonString = job.toString()
        mWebSocketClient.send(jsonString)

    }

    fun sendIceCandidate(socketId: String, iceCandidate: IceCandidate) {
        val childMap = HashMap<String, Any>()
        childMap["id"] = iceCandidate.sdpMid
        childMap["label"] = iceCandidate.sdpMLineIndex
        childMap["candidate"] = iceCandidate.sdp
        childMap["socketId"] = socketId
        val map = HashMap<String, Any>()
        map["eventName"] = "__ice_candidate"
        map["data"] = childMap
        val obj = JSONObject(map)
        val jsonString = obj.toString()
        mWebSocketClient.send(jsonString)
    }

    class TrustManagerTest : X509TrustManager {
        //忽略证书
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {

        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {

        }

        override fun getAcceptedIssuers(): Array<X509Certificate?>? {
            //忽略证书
            return arrayOfNulls(0)
        }

    }

}