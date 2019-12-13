package io.github.kenneycode.videostudio.demo.samples

import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.github.kenneycode.fusion.common.DataKeys
import io.github.kenneycode.fusion.framebuffer.FrameBuffer
import io.github.kenneycode.fusion.framebuffer.FrameBufferCache
import io.github.kenneycode.fusion.process.RenderChain
import io.github.kenneycode.fusion.renderer.DisplayRenderer
import io.github.kenneycode.fusion.renderer.OES2RGBARenderer
import io.github.kenneycode.fusion.util.GLUtil
import io.github.kenneycode.videostudio.decode.VideoDecoder
import io.github.kenneycode.videostudio.demo.R
import kotlinx.android.synthetic.main.activity_sample_common.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode/VideoStudio
 *
 *      MediaCodec视频解码demo
 *      video decode demo using MediaCodec
 *
 **/

class SampleVideoDecoder : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_common)
        glSurfaceView.setEGLContextClientVersion(3)
        glSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {

            private lateinit var surfaceTexture: SurfaceTexture
            private lateinit var renderChain: RenderChain
            private lateinit var input: FrameBuffer
            private var oesTexture = 0
            private var surfaceWidth = 0
            private var surfaceHeight = 0
            private val videoDecoder = VideoDecoder()
            private var hasNewFrame = false


            override fun onDrawFrame(gl: GL10?) {
                Log.e("debug", "onDrawFrame")
                if (hasNewFrame) {
                    hasNewFrame = false
                    surfaceTexture.updateTexImage()
                    val stMatrix = FloatArray(16).apply {
                        surfaceTexture.getTransformMatrix(this)
                    }
                    val data = mutableMapOf<String, Any>().apply {
                        put(DataKeys.ST_MATRIX, stMatrix)
                        put(DataKeys.KEY_DISPLAY_WIDTH, surfaceWidth)
                        put(DataKeys.KEY_DISPLAY_HEIGHT, surfaceHeight)
                    }
                    renderChain.setInput(input)
                    renderChain.update(data)
                    renderChain.render()
                    Thread.sleep(40)
                    videoDecoder.decode()
                }
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                surfaceWidth = width
                surfaceHeight = height
                input = FrameBufferCache.obtainFrameBuffer().apply {
                    this.texture = oesTexture
                    this.width = videoDecoder.getVideoWidth()
                    this.height = videoDecoder.getVideoHeight()
                    this.hasExternalTexture = true
                    this.retain = true
                }
            }

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                renderChain = RenderChain(OES2RGBARenderer()).apply {
                    addNextRenderer(DisplayRenderer())
                    init()
                }
                oesTexture = GLUtil.createOESTexture()
                surfaceTexture = SurfaceTexture(oesTexture).apply {
                    setOnFrameAvailableListener {
                        hasNewFrame = true
                        glSurfaceView.requestRender()
                    }
                }
                videoDecoder.init("/sdcard/v1.mp4", surfaceTexture)
                videoDecoder.decode()
            }

        })
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

}
