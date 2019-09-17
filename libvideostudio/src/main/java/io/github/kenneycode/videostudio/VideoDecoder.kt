package io.github.kenneycode.videostudio

import android.graphics.SurfaceTexture
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.view.Surface
import java.nio.ByteBuffer

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode
 *
 **/

class VideoDecoder {

    companion object {
        private val TAG = VideoDecoder.javaClass.simpleName
    }

    private var videoTrackIndex = 0
    private var maxInputSize = 0
    private var frameRate = 0
    private val bufferInfo = MediaCodec.BufferInfo()

    private lateinit var mediaExtractor: MediaExtractor
    private lateinit var mediaCodec : MediaCodec
    private lateinit var byteBuffer : ByteBuffer
    private lateinit var surface: Surface
    private var timestamp = 0L
    private var width = 0
    private var height = 0
    private var duration = 0L
    private var filePath = ""
    private var readDataEOS = false

    fun init(filePath : String, surfaceTexture: SurfaceTexture) {
        try {
            initParameters(filePath)
            initCodec(surfaceTexture)
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    private fun initParameters(filePath : String) {
        try {
            this.filePath = filePath
            val mr = MediaMetadataRetriever()
            mr.setDataSource(filePath)
            width = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            height = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            duration = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    private fun initCodec(surfaceTexture: SurfaceTexture) {
        surface = Surface(surfaceTexture)
        mediaExtractor = MediaExtractor()
        mediaExtractor.setDataSource(filePath)
        val trackCount = mediaExtractor.getTrackCount()
        for (i in 0 until trackCount) {
            val trackFormat = mediaExtractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME)
            if (mime.contains("video")) {
                videoTrackIndex = i
                break
            }
        }
        if (videoTrackIndex == -1) {
            mediaExtractor.release()
            return
        }
        val videoFormat = mediaExtractor.getTrackFormat(videoTrackIndex)
        val videoMime = videoFormat.getString(MediaFormat.KEY_MIME)
        frameRate = videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)//24
        maxInputSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)//45591
        byteBuffer = ByteBuffer.allocate(maxInputSize)
        mediaCodec = MediaCodec.createDecoderByType(videoMime)
        mediaCodec.configure(videoFormat, surface, null, 0)
        mediaCodec.start()
        mediaExtractor.selectTrack(videoTrackIndex)
    }

    fun decode() : Boolean {
        LogUtil.loge(TAG, "decode start")
        while (!Thread.interrupted()) {
            LogUtil.loge(TAG,  "decode loop")
            if (!readDataEOS) {
                val inputBufferIndex = mediaCodec.dequeueInputBuffer(10000)
                if (inputBufferIndex >= 0) {
                    val buffer = mediaCodec.getInputBuffers()[inputBufferIndex]
                    val sampleSize = mediaExtractor.readSampleData(buffer, 0)
                    if (sampleSize < 0) {
                        LogUtil.loge(TAG, "decode BUFFER_FLAG_END_OF_STREAM");
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        readDataEOS = true
                    } else {
                        LogUtil.loge(TAG, "decode mediaCodec.queueInputBuffer")
                        mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, mediaExtractor.sampleTime, 0)
                        mediaExtractor.advance()
                    }
                }
            }
            val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000)
            LogUtil.loge(TAG, "mediaCodec.dequeueOutputBuffer, outputBufferIndex = $outputBufferIndex")
            when (outputBufferIndex) {
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED, MediaCodec.INFO_OUTPUT_FORMAT_CHANGED, MediaCodec.INFO_TRY_AGAIN_LATER -> {
                }
                else -> {
                    timestamp = bufferInfo.presentationTimeUs;
                    LogUtil.loge(TAG, "mediaCodec.releaseOutputBuffer, outputBufferIndex = $outputBufferIndex, timestamp = $timestamp")
                    val isEOSBuffer = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    if (isEOSBuffer) {
                        LogUtil.loge(TAG, "decode complete")
                    }
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, !isEOSBuffer)
                    return !isEOSBuffer
                }
            }
        }
        return false
    }

    fun getVideoWidth() : Int {
        return width
    }

    fun getVideoHeight() : Int {
        return  height
    }

    fun getVideoDuration() : Long {
        return duration
    }

    fun getTimestamp(): Long {
        return timestamp
    }

    fun seekTo(timestamp : Long) {
        mediaExtractor.seekTo(timestamp, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
    }

    fun release() {
        try {
            mediaExtractor.unselectTrack(videoTrackIndex)
            mediaExtractor.release()
            mediaCodec.stop()
            mediaCodec.release()
            surface.release()
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }
}
