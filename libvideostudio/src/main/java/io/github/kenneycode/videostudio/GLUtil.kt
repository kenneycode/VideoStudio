package io.github.kenneycode.videostudio

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.GLES30
import android.opengl.GLUtils

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode/VideoStudio
 *
 *      GL util 类
 *      GL util
 *
 **/

class GLUtil {

    companion object {

        var isDebug = false

        fun createTexture(): Int {
            val textures = IntArray(1)
            GLES30.glGenTextures(textures.size, textures, 0)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textures[0])
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0)
            return textures[0]
        }

        fun deleteTexture(texture: Int) {
            val textures = intArrayOf(texture)
            GLES30.glDeleteTextures(1, textures, 0)
        }

        fun bitmap2Texture(bitmap: Bitmap, flipY: Boolean = false): Int {
            val texture = createTexture()
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
            GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, if (flipY) { flipBitmapY(bitmap) } else { bitmap }, 0)
            return texture
        }

        fun flipBitmapY(bitmap: Bitmap): Bitmap {
            val matrix = Matrix()
            matrix.setScale(1f, -1f)
            val ret = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            bitmap.recycle()
            return ret
        }

        fun checkGLError(msg: String = "") {
            if (isDebug) {
                val error = GLES30.glGetError()
                if (error != GLES30.GL_NO_ERROR) {
                    val hexErrorCode = Integer.toHexString(error)
                    LogUtil.loge("GLUtil", "$msg glError: $hexErrorCode")
                    val ste = Thread.getAllStackTraces()
                    for (s in ste) {
                        LogUtil.loge("GLUtil", s.toString())
                    }
                    throw RuntimeException("GLError")
                }
            }
        }
    }

}