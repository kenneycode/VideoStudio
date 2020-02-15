package io.github.kenneycode.videostudio.demo.samples

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.kenneycode.fusion.context.FusionEGL
import io.github.kenneycode.videostudio.GLUtil
import io.github.kenneycode.videostudio.encode.VideoEncoder
import io.github.kenneycode.videostudio.demo.R

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode/VideoStudio
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
            val egl = FusionEGL().apply {
                init(null)
                bind()
            }
            val bitmap = decodeBitmapFromAssets("test.png")
            Thread {
                val videoEncoder = VideoEncoder()
                videoEncoder.setOutputPath("/sdcard/test.mp4")
                videoEncoder.setEncodeSize(540, 540)
                videoEncoder.setShareContext(egl.eglContext)
                videoEncoder.init()
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