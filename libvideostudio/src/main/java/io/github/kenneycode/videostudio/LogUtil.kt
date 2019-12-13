package io.github.kenneycode.videostudio

import android.util.Log

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode/VideoStudio
 *
 **/

class LogUtil {

    companion object {

        private val enableLog = true

        fun logd(tag : String, msg : String) {
            if (enableLog) {
                Log.d(tag, msg)
            }
        }

        fun loge(tag : String, msg : String) {
            if (enableLog) {
                Log.e(tag, msg)
            }
        }

        fun loge(e: Exception) {
            if (enableLog) {
                e.printStackTrace()
            }
        }

        fun loge(e : Throwable) {
            if (enableLog) {
                e.printStackTrace()
            }
        }

    }

}