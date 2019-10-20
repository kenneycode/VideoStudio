package io.github.kenneycode.videostudio.demo

import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import io.github.kenneycode.funrenderer.common.Keys
import io.github.kenneycode.funrenderer.common.RenderChain
import io.github.kenneycode.funrenderer.io.Input
import io.github.kenneycode.funrenderer.io.Texture
import io.github.kenneycode.funrenderer.renderer.OES2RGBARenderer
import io.github.kenneycode.funrenderer.renderer.ScreenRenderer
import io.github.kenneycode.videostudio.VideoDecoder
import kotlinx.android.synthetic.main.activity_sample_common.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode
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
            private lateinit var input: Input
            private var oesTexture = 0
            private val videoDecoder = VideoDecoder()
            private var hasNewFrame = false


            override fun onDrawFrame(gl: GL10?) {
                Log.e("debug", "onDrawFrame")
                if (hasNewFrame) {
                    hasNewFrame = false
                    surfaceTexture.updateTexImage()
                    val stMatrix = FloatArray(16)
                    surfaceTexture.getTransformMatrix(stMatrix)
                    val data = mutableMapOf<String, Any>()
                    data[Keys.ST_MATRIX] = stMatrix
                    renderChain.render(input, data)
                    Thread.sleep(40)
                    videoDecoder.decode()
                }
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                input = Texture(oesTexture, width, height, false)
            }

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {

                renderChain = RenderChain.create()
                        .addRenderer(OES2RGBARenderer())
                        .addRenderer(ScreenRenderer())
                renderChain.init()

                oesTexture = createOESTexture()
                surfaceTexture = SurfaceTexture(oesTexture)
                surfaceTexture.setOnFrameAvailableListener {
                    hasNewFrame = true
                    glSurfaceView.requestRender()
                }
                videoDecoder.init("/sdcard/2ae0840bf9e995adc1a382f78458cafb.mp4", surfaceTexture)
                videoDecoder.decode()
            }

        })
        glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
    }

    fun createOESTexture() : Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(textures.size, textures, 0)
        GLES30.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
        return return textures[0]
    }

}
