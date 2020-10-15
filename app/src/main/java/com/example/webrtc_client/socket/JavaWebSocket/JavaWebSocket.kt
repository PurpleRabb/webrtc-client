package com.example.webrtc_client.socket.JavaWebSocket

import android.util.Log
import com.alibaba.fastjson.JSONObject
import com.example.webrtc_client.ChatRoomActivity
import com.example.webrtc_client.MainActivity
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
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

    fun connect(wss: String) {
        var uri: URI = URI(wss)
        mWebSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.i(TAG, "onOpen")
                ChatRoomActivity.openActivity(mAcitivity)
            }

            override fun onMessage(message: String?) {
                Log.i(TAG, "onMessage:$message")
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