package com.example.webrtc_client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.FrameLayout
import com.example.webrtc_client.utils.PermissionUtil
import com.example.webrtc_client.utils.Utils
import kotlinx.android.synthetic.main.activity_room.*
import org.webrtc.*

class ChatRoomActivity : Activity() {

    private var localVideoTrack: VideoTrack? = null
    lateinit var webRTCManager: WebRTCManager
    lateinit var rootEglBase: EglBase
    lateinit var utils: Utils

    var videoViews: HashMap<String, SurfaceViewRenderer> = HashMap<String, SurfaceViewRenderer>()
    var persons: ArrayList<String> = ArrayList<String>()

    companion object {
        fun openActivity(activity: Activity) {
            val intent = Intent(activity, ChatRoomActivity::class.java)
            activity.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)
        utils = Utils(this)
        initView()
    }

    private fun initView() {
        rootEglBase = EglBase.create()
        webRTCManager = WebRTCManager.instance
        if (!PermissionUtil.isNeedRequestPermission(this)) {
            webRTCManager.joinRoom(this, rootEglBase)
        }
    }

    fun onSetLocalStream(stream: MediaStream, userId: String) {
        //在子线程调用，不能直接刷新界面
        val videoTracks = stream.videoTracks
        if (videoTracks.size > 0) {
            localVideoTrack = videoTracks.get(0)
        }
        runOnUiThread {
            addView(userId, stream)
        }
    }

    //多次背调用，有几个人就调用几次
    private fun addView(userId: String, stream: MediaStream) {
        //界面显示
        val surfaceViewRenderer = SurfaceViewRenderer(this)
        surfaceViewRenderer.init(rootEglBase.eglBaseContext, null)
        //设置缩放模式
        surfaceViewRenderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        //翻转摄像头
        surfaceViewRenderer.setMirror(true)

        if (stream.videoTracks.size > 0) {
            stream.videoTracks.get(0).addSink(surfaceViewRenderer)
        }

        videoViews.put(userId, surfaceViewRenderer)
        persons.add(userId)

        //往布局里面添加view
        wr_video_view.addView(surfaceViewRenderer)

        val size = videoViews.size
        for (i in 0 until size) {
            val peerId = persons[i]
            val renderer = videoViews[peerId]
            val layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
            layoutParams.height = utils.getWidth(size)
            layoutParams.width = layoutParams.height
            layoutParams.leftMargin = utils.getX(size, i)
            layoutParams.topMargin = utils.getY(size, i)
            renderer?.layoutParams = layoutParams
        }
    }

    fun onAddRemoteStream(p0: MediaStream?, socketId: String) {
        runOnUiThread {
            if (p0 != null) {
                addView(socketId, p0)
            }
        }
    }
}