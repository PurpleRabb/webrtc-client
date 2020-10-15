package com.example.webrtc_client.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


object PermissionUtil {
    // 檢查是否有權限
    fun isNeedRequestPermission(activity: Activity): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            false
        } else isNeedRequestPermission(
            activity, Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    private fun isNeedRequestPermission(activity: Activity, vararg permissions: String): Boolean {
        val mPermissionListDenied: ArrayList<String> = ArrayList()
        for (permission in permissions) {
            val result = checkPermission(activity, permission)
            if (result != PackageManager.PERMISSION_GRANTED) {
                mPermissionListDenied.add(permission)
            }
        }
        return if (mPermissionListDenied.size > 0) {
            var pears: Array<String?>? = arrayOfNulls(mPermissionListDenied.size)
            pears = mPermissionListDenied.toArray<String>(pears)
            ActivityCompat.requestPermissions(activity, pears!!, 0)
            true
        } else {
            false
        }
    }

    private fun checkPermission(activity: Activity, permission: String): Int {
        return ContextCompat.checkSelfPermission(activity, permission)
    }
}