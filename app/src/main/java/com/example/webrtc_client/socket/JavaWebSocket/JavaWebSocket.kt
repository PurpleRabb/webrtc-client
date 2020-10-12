package com.example.webrtc_client.socket.JavaWebSocket

import android.util.Log
import com.example.webrtc_client.MainActivity
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
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
            }

            override fun onMessage(message: String?) {
                Log.i(TAG, "onMessage")
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

    class TrustManagerTest : X509TrustManager {
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