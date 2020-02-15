package io.github.kenneycode.videostudio.encode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.opengl.*
import android.view.Surface
import io.github.kenneycode.videostudio.GLUtil
import io.github.kenneycode.videostudio.LogUtil
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode/VideoStudio
 *
 *      视频编码器
 *      video encoder
 *
 **/

class VideoEncoder {

    companion object {
        private val TAG = VideoEncoder::class.java.simpleName
    }

    private val bufferInfo = MediaCodec.BufferInfo()
    private lateinit var mediaCodec: MediaCodec
    private lateinit var egl: EncodeEGL
    private lateinit var mediaMuxer: MediaMuxer
    private var encodeWidth = 0
    private var encodeHeight = 0
    private var trackIndex = 0
    private var muxerStarted = false
    private lateinit var encodeRenderer: EncoderRenderer
    private lateinit var outputPath: String
    private lateinit var shareContext: EGLContext

    fun init(encoderConfig: EncoderConfig = EncoderConfig()) {
        val format = MediaFormat.createVideoFormat("video/avc", encodeWidth, encodeHeight).apply {
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, encoderConfig.bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, encoderConfig.frameRate)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, encoderConfig.iframeInterval)
        }
        mediaCodec = MediaCodec.createEncoderByType(encoderConfig.mimeType)
        mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

        egl = EncodeEGL(shareContext, mediaCodec.createInputSurface()).apply {
            init()
            makeCurrent()
        }

        encodeRenderer = EncoderRenderer().apply {
            this.width = encodeWidth
            this.height = encodeHeight
            init()
        }

        mediaCodec.start()

        val file = File(outputPath)
        if (!file.exists()) {
            file.createNewFile()
        }

        mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        trackIndex = -1
        muxerStarted = false

    }

    fun setOutputPath(outputPath: String) {
        this.outputPath = outputPath
    }

    fun setShareContext(shareContext: EGLContext) {
        this.shareContext = shareContext
    }

    fun setEncodeSize(encodeWidth: Int, encodeHeight: Int) {
        this.encodeWidth = encodeWidth
        this.encodeHeight = encodeHeight
    }

    fun encodeFrame(texture: Int, timestamp: Long) {
        if (texture != 0) {
            encodeRenderer.drawFrame(texture)
            egl.setTimestamp(timestamp)
            egl.swapBuffers()
            GLUtil.checkGLError()
            drainEncoder(false)
        } else {
            drainEncoder(true)
        }
    }

    private fun drainEncoder(endOfStream: Boolean) {
        try {
            if (endOfStream) {
                mediaCodec.signalEndOfInputStream()
            }
            var encoderOutputBuffers = mediaCodec.outputBuffers
            while (true) {
                val bufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
                if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    if (!endOfStream) {
                        break
                    }
                } else if (bufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = mediaCodec.outputBuffers
                } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    if (muxerStarted) {
                        mediaCodec.releaseOutputBuffer(bufferIndex, false)
                        continue
                    }
                    trackIndex = mediaMuxer.addTrack(mediaCodec.outputFormat)
                    mediaMuxer.start()
                    muxerStarted = true
                } else {
                    val encodedData = encoderOutputBuffers[bufferIndex]
                    if (encodedData == null) {
                        mediaCodec.releaseOutputBuffer(bufferIndex, false)
                        continue
                    } else {
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                            bufferInfo.size = 0
                        }
                        if (bufferInfo.size != 0) {
                            if (!muxerStarted) {
                                mediaCodec.releaseOutputBuffer(bufferIndex, false)
                                continue

                            }
                            encodedData.position(bufferInfo.offset)
                            encodedData.limit(bufferInfo.offset + bufferInfo.size)
                            mediaMuxer.writeSampleData(trackIndex, encodedData, bufferInfo)
                        }
                        mediaCodec.releaseOutputBuffer(bufferIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            break
                        }
                    }
                }
            }
        } catch (e: RuntimeException) {
            LogUtil.loge(TAG, e.toString())
        }

    }

    fun release() {
        mediaCodec.stop()
        mediaCodec.release()
        egl.release()
        mediaMuxer.stop()
        mediaMuxer.release()
        encodeRenderer.release()
    }

    private class EncodeEGL(private var shareContext: EGLContext, private var surface: Surface) {

        private var eglDisplay = EGL14.EGL_NO_DISPLAY
        private var eglContext = EGL14.EGL_NO_CONTEXT
        private var eglSurface = EGL14.EGL_NO_SURFACE

        fun init() {
            eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay === EGL14.EGL_NO_DISPLAY) {
                throw RuntimeException("unable to get EGL14 display")
            }
            val version = IntArray(2)
            if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                throw RuntimeException("eglInitialize fail")
            }
            val attribList = intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, 0x3142, 1, EGL14.EGL_NONE, 0,
                EGL14.EGL_NONE
            )
            val eglConfig = arrayOfNulls<android.opengl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!EGL14.eglChooseConfig(
                    eglDisplay, attribList, 0, eglConfig, 0, eglConfig.size,
                    numConfigs, 0
                )
            ) {
                throw RuntimeException("eglChooseConfig fail")
            }
            eglContext = EGL14.eglCreateContext(
                eglDisplay, eglConfig[0], shareContext,
                intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE), 0
            )
            val values = IntArray(1)
            EGL14.eglQueryContext(
                eglDisplay, eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
                values, 0
            )
            LogUtil.logd("debug", "EGLContext created, client version " + values[0])
            val surfaceAttribs = intArrayOf(
                EGL14.EGL_NONE
            )
            eglSurface = EGL14.eglCreateWindowSurface(
                eglDisplay, eglConfig[0], surface,
                surfaceAttribs, 0
            )
        }

        fun release() {
            if (eglDisplay !== EGL14.EGL_NO_DISPLAY) {
                EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                EGL14.eglDestroySurface(eglDisplay, eglSurface)
                EGL14.eglDestroyContext(eglDisplay, eglContext)
                EGL14.eglReleaseThread()
                EGL14.eglTerminate(eglDisplay)
            }
            surface.release()
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE

        }

        fun makeCurrent() {
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        }

        fun swapBuffers(): Boolean {
            return EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }

        fun setTimestamp(timestamp: Long) {
            LogUtil.loge(TAG, "setTimestamp = $timestamp")
            LogUtil.loge(TAG, "eglPresentationTimeANDROID start")
            EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, timestamp)
            LogUtil.loge(TAG, "eglPresentationTimeANDROID end")
        }

    }

    private class EncoderRenderer {

        private val vertexShaderCode =
                    "precision mediump float;\n" +
                    "attribute vec4 a_position;\n" +
                    "attribute vec2 a_textureCoordinate;\n" +
                    "varying vec2 v_textureCoordinate;\n" +
                    "void main() {\n" +
                    "    v_textureCoordinate = a_textureCoordinate;\n" +
                    "    gl_Position = a_position;\n" +
                    "}"
        private val fragmentShaderCode =
                    "precision mediump float;\n" +
                    "varying vec2 v_textureCoordinate;\n" +
                    "uniform sampler2D s_texture;\n" +
                    "void main() {\n" +
                    "    gl_FragColor = texture2D(s_texture, v_textureCoordinate);" +
                    "}"

        private val vertexData = floatArrayOf(-1f, -1f, -1f, 1f, 1f, 1f, -1f, -1f, 1f, 1f, 1f, -1f)
        private lateinit var vertexDataBuffer: FloatBuffer
        private val textureCoordinateData = floatArrayOf(0f, 0f, 0f, 1f, 1f, 1f, 0f, 0f, 1f, 1f, 1f, 0f)
        private lateinit var textureCoordinateBuffer: FloatBuffer
        private val VERTEX_COMPONENT_COUNT = 2
        private val TEXTURE_COORDINATE_COMPONENT_COUNT = 2

        private var positionLocation = 0
        private var textureCoordinateLocation = 0
        private var textureLocation = 0

        var width = 0
        var height = 0

        private var programId = 0

        fun drawFrame(texture: Int) {

            GLES30.glUseProgram(programId)

            GLES30.glEnableVertexAttribArray(positionLocation)
            GLES30.glVertexAttribPointer(
                positionLocation,
                VERTEX_COMPONENT_COUNT,
                GLES30.GL_FLOAT,
                false,
                0,
                vertexDataBuffer
            )

            GLES30.glEnableVertexAttribArray(textureCoordinateLocation)
            GLES30.glVertexAttribPointer(
                textureCoordinateLocation,
                TEXTURE_COORDINATE_COMPONENT_COUNT,
                GLES30.GL_FLOAT,
                false,
                0,
                textureCoordinateBuffer
            )

            GLES30.glUniform1i(textureLocation, 0)
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0)
            GLES30.glClearColor(0.0f, 0.9f, 0.0f, 1f)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
            GLES30.glViewport(0, 0, width, height)
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, vertexData.size / VERTEX_COMPONENT_COUNT)
        }

        fun init() {
            programId = GLES30.glCreateProgram()

            val vertexShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER)
            val fragmentShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER)
            GLES30.glShaderSource(vertexShader, vertexShaderCode)
            GLES30.glShaderSource(fragmentShader, fragmentShaderCode)
            GLES30.glCompileShader(vertexShader)
            GLES30.glCompileShader(fragmentShader)

            GLES30.glAttachShader(programId, vertexShader)
            GLES30.glAttachShader(programId, fragmentShader)

            GLES30.glLinkProgram(programId)

            GLES30.glDeleteShader(vertexShader)
            GLES30.glDeleteShader(fragmentShader)

            GLES30.glUseProgram(programId)

            vertexDataBuffer = ByteBuffer.allocateDirect(vertexData.size * java.lang.Float.SIZE / 8)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            vertexDataBuffer.put(vertexData)
            vertexDataBuffer.position(0)
            positionLocation = GLES30.glGetAttribLocation(programId, "a_position")

            textureCoordinateBuffer =
                ByteBuffer.allocateDirect(textureCoordinateData.size * java.lang.Float.SIZE / 8)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer()
            textureCoordinateBuffer.put(textureCoordinateData)
            textureCoordinateBuffer.position(0)
            textureCoordinateLocation = GLES30.glGetAttribLocation(programId, "a_textureCoordinate")

            textureLocation = GLES30.glGetUniformLocation(programId, "s_texture")
            GLES30.glUniform1i(textureLocation, 0)
        }

        fun release() {
            GLES30.glDeleteProgram(programId)
        }

    }
}