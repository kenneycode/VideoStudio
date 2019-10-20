package io.github.kenneycode.videostudio.demo.samples

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.kenneycode.glkit.EGL
import io.github.kenneycode.videostudio.GLUtil
import io.github.kenneycode.videostudio.VideoEncoder
import io.github.kenneycode.videostudio.demo.R

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode
 *
 *      MediaCodec视频编码demo
 *      video encode demo using MediaCodec
 *
 **/

class SampleVideoEncoder : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_common)
        Thread {
            val egl = EGL().apply {
                init()
                bind()
            }
            val bitmap = decodeBitmapFromAssets("test.png")
            Thread {
                val videoEncoder = VideoEncoder()
                videoEncoder.init("/sdcard/test.mp4", 540, 540, egl.eglContext)
                for (i in 0 until 100) {
                    val texture = GLUtil.bitmap2Texture(rotateBitmap(bitmap, i * 2f))
                    videoEncoder.encodeFrame(texture, 100 * i * 1000000L)
                    GLUtil.deleteTexture(texture)
                }
                videoEncoder.encodeFrame(0, 0)
                videoEncoder.release()
            }.start()
        }.start()
    }

    private fun decodeBitmapFromAssets(filename : String) : Bitmap {
        val options = BitmapFactory.Options()
        options.inSampleSize = 1
        return BitmapFactory.decodeStream(assets.open(filename))
    }

    private fun rotateBitmap(bitmap: Bitmap, rotate: Float): Bitmap {
        val matrix = Matrix()
        matrix.setRotate(rotate)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}