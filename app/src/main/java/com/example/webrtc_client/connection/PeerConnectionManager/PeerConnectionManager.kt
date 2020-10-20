package com.example.webrtc_client.connection.PeerConnectionManager

import android.content.Context
import com.example.webrtc_client.ChatRoomActivity
import com.example.webrtc_client.socket.JavaWebSocket.JavaWebSocket
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class PeerConnectionManager private constructor() {

    private var videoCapturer: VideoCapturer? = null
    private var videoSource: VideoSource? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var audioSource: AudioSource? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null
    var videoEnable = true
    var executor: ExecutorService = Executors.newSingleThreadExecutor()
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private lateinit var peerConnections: List<PeerConnection>
    private lateinit var context: ChatRoomActivity
    private lateinit var eglBase: EglBase
    private var localStream: MediaStream? = null
    private lateinit var myId : String

    companion object {
        val instance: PeerConnectionManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            PeerConnectionManager()
        }
    }

    fun initContext(context: ChatRoomActivity, eglBase: EglBase) {
        this.context = context
        this.eglBase = eglBase
    }

    fun joinToRoom(
        javaWebSocket: JavaWebSocket,
        connections: ArrayList<String>,
        myId: String,
        isVideoEnabled: Boolean,
    ) {
        this.videoEnable = isVideoEnabled
        this.myId = myId
        //这里初始化PeerConnection
        executor.execute {
            if (this.peerConnectionFactory == null) {
                this.peerConnectionFactory = createConnectionFactory()
            }

            if (localStream == null) {
                createLocalStream()
            }
        }
    }

    private fun createLocalStream() {
        //子线程执行
        localStream = peerConnectionFactory?.createLocalMediaStream("ARDAMS")
        //音频设置
        audioSource = peerConnectionFactory?.createAudioSource(createMediaConstraints())
        //采集音频
        localAudioTrack = peerConnectionFactory?.createAudioTrack("ARDAMSa0", audioSource)
        localStream?.addTrack(localAudioTrack)

        if (videoEnable) {
            videoCapturer = createVideoCapture()
            videoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast())
            surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
            videoCapturer?.startCapture(320, 230, 10) //宽高，帧率
            localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
            localStream?.addTrack(localVideoTrack)

            if (context != null) {
                context.onSetLocalStream(localStream!!,myId)
            }
        }
    }

    private fun createVideoCapture(): VideoCapturer? {
        return if (Camera2Enumerator.isSupported(context)) {
            createCameraCapture(Camera2Enumerator(context))
        } else {
            createCameraCapture(Camera1Enumerator())
        }
    }

    private fun createCameraCapture(cameraEnumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = cameraEnumerator.deviceNames
        var videoCapturer: VideoCapturer? = null
        for (name in deviceNames) {
            if (cameraEnumerator.isFrontFacing(name)) {
                videoCapturer = cameraEnumerator.createCapturer(name, null)
            }
            if (videoCapturer != null) {
                return videoCapturer
            }
        }

        for (name in deviceNames) {
            if (!cameraEnumerator.isFrontFacing(name)) {
                videoCapturer = cameraEnumerator.createCapturer(name, null)
            }
            if (videoCapturer != null) {
                return videoCapturer
            }
        }

        return null
    }

    private fun createMediaConstraints(): MediaConstraints {
        val audioConstraints = MediaConstraints()
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googEchoCancellation",
                "true"
            )
        )
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googNoiseSuppression",
                "true"
            )
        )
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googAutoGainControl",
                "false"
            )
        )
        audioConstraints.mandatory.add(
            MediaConstraints.KeyValuePair(
                "googHighpassFilter",
                "true"
            )
        )
        return audioConstraints
    }

    private fun createConnectionFactory(): PeerConnectionFactory {
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