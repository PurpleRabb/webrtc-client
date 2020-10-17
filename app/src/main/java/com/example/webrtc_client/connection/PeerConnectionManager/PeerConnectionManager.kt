package com.example.webrtc_client.connection.PeerConnectionManager

import android.content.Context
import com.example.webrtc_client.socket.JavaWebSocket.JavaWebSocket
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class PeerConnectionManager private constructor() {
    var videoEnable = true
    var executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private lateinit var peerConnections: List<PeerConnection>
    private lateinit var context: Context
    private lateinit var eglBase: EglBase

    companion object {
        val instance: PeerConnectionManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            PeerConnectionManager()
        }
    }

    fun initContext(context: Context, eglBase: EglBase) {
        this.context = context
        this.eglBase = eglBase
    }

    fun joinToRoom(
        javaWebSocket: JavaWebSocket,
        connections: ArrayList<String>,
        myId: String,
        isVideoEnabled: Boolean
    ) {
        this.videoEnable = isVideoEnabled
        //这里初始化PeerConnection
        executor.execute {
            if (this.peerConnectionFactory == null) {
                this.peerConnectionFactory = createConnectionFactory()
            }
        }
    }

    fun createConnectionFactory(): PeerConnectionFactory {
        val videoEncoderFactory: VideoEncoderFactory =
            DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)
        val videoDecoderFactory: VideoDecoderFactory =
            DefaultVideoDecoderFactory(eglBase.eglBaseContext)
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .createInitializationOptions()
        )
        val options = PeerConnectionFactory.Options()
        return PeerConnectionFactory.builder().apply {
            setOptions(options)
            setAudioDeviceModule(JavaAudioDeviceModule.builder(context).createAudioDeviceModule())
            setVideoEncoderFactory(videoEncoderFactory)
            setVideoDecoderFactory(videoDecoderFactory)
            createPeerConnectionFactory()
        }.createPeerConnectionFactory()
//        return PeerConnectionFactory.builder().setOptions(options).setAudioDeviceModule(
//            JavaAudioDeviceModule.builder(context).createAudioDeviceModule()
//        ).setVideoEncoderFactory(videoEncoderFactory)
//            .setVideoDecoderFactory(videoDecoderFactory)
//            .createPeerConnectionFactory()
    }

}