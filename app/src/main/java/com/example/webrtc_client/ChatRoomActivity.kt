package com.example.webrtc_client

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import com.example.webrtc_client.utils.PermissionUtil
import org.webrtc.EglBase

class ChatRoomActivity : Activity() {

    lateinit var webRTCManager: WebRTCManager
    lateinit var rootEglBase: EglBase

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
        initView()
    }

    private fun initView() {
        rootEglBase = EglBase.create()
        webRTCManager = WebRTCManager.instance
        if (!PermissionUtil.isNeedRequestPermission(this)) {
            webRTCManager.joinRoom(this, rootEglBase)
        }
    }
}