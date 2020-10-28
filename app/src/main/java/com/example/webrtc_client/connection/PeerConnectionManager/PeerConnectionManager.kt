package com.example.webrtc_client.connection.PeerConnectionManager

import android.util.Log
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
    private lateinit var myId: String
    private lateinit var iceServers: ArrayList<PeerConnection.IceServer>
    private lateinit var connectionIds: ArrayList<String> //保存所有用户的ID
    private lateinit var connectionPeerDic: HashMap<String, Peer> //会议室外部用户与本地用户的peer链接
    private lateinit var webSocket: JavaWebSocket

    enum class Role {
        Caller, //邀请
        Receiver //被邀请
    }

    companion object {
        val instance: PeerConnectionManager by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
            PeerConnectionManager()
        }
    }

    fun initContext(context: ChatRoomActivity, eglBase: EglBase) {
        this.context = context
        this.eglBase = eglBase
        this.iceServers = ArrayList()
        val iceServer = PeerConnection.IceServer.builder("stun:8.210.69.115:3478?transport=udp")
            .setUsername("").setPassword("").createIceServer()

        val iceServer1 = PeerConnection.IceServer.builder("turn:8.210.69.115:3478?transport=udp")
            .setUsername("").setPassword("").createIceServer() //TODO
        iceServers.add(iceServer)
        iceServers.add(iceServer1)

        connectionIds = ArrayList()
        connectionPeerDic = HashMap()
    }

    fun joinToRoom(
        javaWebSocket: JavaWebSocket,
        connections: ArrayList<String>,
        myId: String,
        isVideoEnabled: Boolean,
    ) {
        this.videoEnable = isVideoEnabled
        this.myId = myId
        this.webSocket = javaWebSocket
        //这里初始化PeerConnection
        executor.execute {
            if (this.peerConnectionFactory == null) {
                this.peerConnectionFactory = createConnectionFactory()
            }

            if (localStream == null) {
                createLocalStream()
            }
            connectionIds.addAll(connections)
            createPeerConnections()
            //把本地数据流推向会议室的每一个人
            addStreams()
            //为每一个人发送邀请
            createOffers()
        }
    }

    private lateinit var role: Role
    private fun createOffers() {
        for (p in connectionPeerDic) {
            role = Role.Caller
            val mPeer = p.value
            mPeer.peerConnection.createOffer(mPeer, offerOrAnswerConstrant())
        }
    }

    private fun offerOrAnswerConstrant(): MediaConstraints {
        val mediaConstraints = MediaConstraints()
        val keyValuePairs: ArrayList<MediaConstraints.KeyValuePair> = ArrayList()
        keyValuePairs.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        keyValuePairs.add(
            MediaConstraints.KeyValuePair(
                "OfferToReceiveVideo",
                videoEnable.toString()
            )
        )
        mediaConstraints.mandatory.addAll(keyValuePairs)
        return mediaConstraints
    }

    //为所有用户推流
    private fun addStreams() {
        for (p in connectionPeerDic) {
            if (localStream == null) {
                createLocalStream()
            }
            p.value.peerConnection.addStream(localStream)
        }
    }

    private fun createPeerConnections() {
        //对每一个用户建立p2p链接
        for (id in connectionIds) {
            val peer = Peer(id)
            connectionPeerDic[id] = peer
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
            videoSource = peerConnectionFactory?.createVideoSource(videoCapturer!!.isScreencast)
            surfaceTextureHelper =
                SurfaceTextureHelper.create("CaptureThread", eglBase.eglBaseContext)
            videoCapturer?.initialize(surfaceTextureHelper, context, videoSource?.capturerObserver)
            videoCapturer?.startCapture(320, 230, 10) //宽高，帧率
            localVideoTrack = peerConnectionFactory?.createVideoTrack("ARDAMSv0", videoSource)
            localStream?.addTrack(localVideoTrack)

            context.onSetLocalStream(localStream!!, myId)
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

    fun onRemoteIceCandidate(socketId: String, iceCandidate: IceCandidate) {
        var peer = connectionPeerDic[socketId]
        if (peer != null) {
            peer.peerConnection.addIceCandidate(iceCandidate)
        }
    }

    inner class Peer(id: String) : PeerConnection.Observer, SdpObserver {
        var peerConnection: PeerConnection //myid与远端用户之间的链接
        private var socketId: String //其它用户的id

        init {
            val rtcConfiguration = PeerConnection.RTCConfiguration(iceServers)
            peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfiguration, this)!!
            this.socketId = id
        }

        //调用分两种情况：第一类连接到ICE服务器的时候，调用次数是网络中有多少个路由节点
        //第二类 有人进入这个房间，对方到ICE的路由节点,调用次数是对方到ICE有多少个节点
        override fun onIceCandidate(p0: IceCandidate?) {
            //通过socket传递到服务器
            if (p0 != null) {
                webSocket.sendIceCandidate(socketId, p0)
            }
        }

        override fun onDataChannel(p0: DataChannel?) {

        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {

        }

        override fun onIceConnectionChange(p0: PeerConnection.IceConnectionState?) {

        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState?) {

        }

        override fun onAddStream(p0: MediaStream?) {
            //建立成功后设置多媒体流
            context.onAddRemoteStream(p0, socketId)
        }

        override fun onSignalingChange(p0: PeerConnection.SignalingState?) {

        }

        override fun onIceCandidatesRemoved(p0: Array<out IceCandidate>?) {

        }

        override fun onRemoveStream(p0: MediaStream?) {

        }

        override fun onRenegotiationNeeded() {

        }

        override fun onAddTrack(p0: RtpReceiver?, p1: Array<out MediaStream>?) {

        }

        //---->SdpObserver
        override fun onSetFailure(p0: String?) {

        }

        override fun onSetSuccess() {
            //交换彼此的sdp

        }

        override fun onCreateSuccess(p0: SessionDescription?) {
            Log.i("PeerConnectionManager", "onCreateSuccess")
            //设置本地的SDP，如果设置成功则回调onSetSuccess
            peerConnection.setLocalDescription(this, p0)
            if (peerConnection.signalingState() == PeerConnection.SignalingState.HAVE_LOCAL_OFFER) {
                //本地sdp
                webSocket.sendOffer(socketId, peerConnection.localDescription)
            }
        }

        override fun onCreateFailure(p0: String?) {

        }

    }

}