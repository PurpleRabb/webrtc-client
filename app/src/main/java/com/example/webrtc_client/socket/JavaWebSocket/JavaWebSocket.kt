package com.example.webrtc_client.socket.JavaWebSocket

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
        val uri = URI(wss)
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
            val sslContext: SSLContext = SSLContext.getInstance("TLS")
            sslContext.init(null, arrayOf<TrustManager>(TrustManagerTest()), SecureRandom())
            val factory = sslContext.socketFactory
            if (factory != null) {
                mWebSocketClient.socket = factory.createSocket()
                mWebSocketClient.connect()
            }
        }
    }

    private fun handleMessage(message: String?) {
        //将json转换成map
        val map = JSON.parseObject(message, Map::class.java)
        val eventName: String = map["eventName"] as String
        if (eventName == "_peers") {
            handleJoinRoom(map)
        }

        //对方响应
        if (eventName.equals("_ice_candidate")) {
            handleRemoteCandidate(map)
        }

        if (eventName == "_answer") {
            //获取对方的sdp
            handleAnswer(map)
        }
    }

    private fun handleAnswer(map: Map<*, *>?) {
        val data = map?.get("data") as Map<*, *>
        val sdpDic = data["sdp"] as Map<*,*>
        val socketId = data["socketId"] as String
        val sdp = sdpDic["sdp"] as String
        peerConnectionManager.onReceiverAnswer(socketId,sdp)
    }


    private fun handleRemoteCandidate(map: Map<*, *>?) {
        val data = map?.get("data") as Map<*, *>
        val socketId: String
        socketId = data["socketId"] as String
        val sdpMid = data["id"] as String
        val sdpMLineIndex = java.lang.String.valueOf(data["label"]).toDouble().toInt()
        val candidate = data["candidate"] as String
        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, candidate)
        peerConnectionManager.onRemoteIceCandidate(socketId, iceCandidate)
    }

    private fun handleJoinRoom(map: Map<*, *>?) {
        val data: Map<*, *> = map?.get("data") as Map<*, *>
        val arr: JSONArray = data["connections"] as JSONArray
        val js: String = JSONObject.toJSONString(arr, SerializerFeature.WriteClassName)
        val connections: ArrayList<String> = JSONObject.parseArray(js, String::class.java)
                as ArrayList<String> //房间里已经存在的链接数
        val myId: String = data["you"] as String
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
        val childMap1: HashMap<String, Any> = HashMap()
        childMap1.put("type", "offer")
        if (sdp != null) {
            childMap1["sdp"] = sdp.description
        }

        val childMap2: HashMap<String, Any> = HashMap()
        childMap2.put("socketId", socketId)
        childMap2["sdp"] = childMap1

        val map: HashMap<String, Any> = HashMap()
        map["eventName"] = "__offer"
        map["data"] = childMap2

        val job = JSONObject(map)
        val jsonString = job.toString()
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