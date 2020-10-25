package com.example.webrtc_client.utils

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

class Utils(context: Context) {
    var mScreenWidth = 0
    var mScreenHeight = 0
    var context: Context = context

    init {
        if (mScreenWidth == 0) {
            var manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            var metrix = DisplayMetrics()
            manager.defaultDisplay.getMetrics(metrix)
            mScreenWidth = metrix.widthPixels
        }
    }

    //获取每一个显示框的宽度
    fun getWidth(size: Int): Int {
        if (size <= 4) {
            return mScreenWidth / 2
        } else {
            return mScreenWidth / 3
        }
    }

    //获取显示框的X坐标
    fun getX(size: Int, index: Int): Int {
        if (size <= 4) {
            return if (size == 3 && index == 2) {
                mScreenWidth / 4
            } else index % 2 * mScreenWidth / 2
        } else if (size <= 9) {
            if (size == 5) {
                if (index == 3) {
                    return mScreenWidth / 6
                }
                if (index == 4) {
                    return mScreenWidth / 2
                }
            }
            if (size == 7 && index == 6) {
                return mScreenWidth / 3
            }
            if (size == 8) {
                if (index == 6) {
                    return mScreenWidth / 6
                }
                if (index == 7) {
                    return mScreenWidth / 2
                }
            }
            return index % 3 * mScreenWidth / 3
        }
        return 0
    }

    fun getY(size: Int, index: Int): Int {
        if (size < 3) {
            return mScreenWidth / 4
        } else if (size < 5) {
            return if (index < 2) {
                0
            } else {
                mScreenWidth / 2
            }
        } else if (size < 7) {
            return if (index < 3) {
                mScreenWidth / 2 - mScreenWidth / 3
            } else {
                mScreenWidth / 2
            }
        } else if (size <= 9) {
            return if (index < 3) {
                0
            } else if (index < 6) {
                mScreenWidth / 3
            } else {
                mScreenWidth / 3 * 2
            }
        }
        return 0
    }


}