package io.github.kenneycode.videostudio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.util.Log
import java.nio.ByteBuffer

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
    private var startTime = 0L
    private var endTime = 0L
    private var timestamp = 0L
    private var width = 0
    private var height = 0
    private var duration = 0L
    private var filePath = ""

    fun init(filePath : String, startTime : Long, endTime : Long) {
        try {
            initParameters(filePath, startTime, endTime)
            initCodec()
        } catch (e : Exception) {
        }
    }

    private fun initParameters(filePath : String, startTime : Long, endTime : Long) {
        try {
            this.filePath = filePath
            this.startTime = startTime
            this.endTime = endTime
            val mr = MediaMetadataRetriever()
            mr.setDataSource(filePath)
            width = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            height = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            duration = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toLong()
        } catch (e : Exception) {
        }
    }

    private fun initCodec() {
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
        Log.e(TAG, "视频编码器 run: " + videoFormat.toString())
        val videoMime = videoFormat.getString(MediaFormat.KEY_MIME)
        frameRate = videoFormat.getInteger(MediaFormat.KEY_FRAME_RATE)//24
        maxInputSize = videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)//45591
        byteBuffer = ByteBuffer.allocate(maxInputSize)
        mediaCodec = MediaCodec.createDecoderByType(videoMime)
        mediaCodec.configure(videoFormat, videoDecodeOutputSurface, null, 0)
        mediaCodec.start()
        mediaExtractor.selectTrack(videoTrackIndex)
        mediaExtractor.seekTo(startTime, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
    }

    fun decodeToSurface() : Boolean {
        while (!Thread.interrupted()) {
            Log.e("debug", "decodeToSurface, filePath = $filePath")
            val sampleSize = mediaExtractor.readSampleData(byteBuffer, 0)
            //填充要解码的数据
            if (sampleSize != -1) {
                if (sampleSize >= 0) {
                    val sampleTime = mediaExtractor.sampleTime
                    if (sampleTime >= 0) {
                        val inputBufferIndex = mediaCodec.dequeueInputBuffer(-1)
                        if (inputBufferIndex >= 0) {
                            val inputBuffer = mediaCodec.inputBuffers[inputBufferIndex]
                            if (inputBuffer != null) {
                                inputBuffer.clear()
                                inputBuffer.put(byteBuffer)
                                mediaCodec.queueInputBuffer(inputBufferIndex, 0, sampleSize, sampleTime, 0)
                                mediaExtractor.advance()
                            }
                        }
                    }
                }
            }
            //解码已填充的数据
            val outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0)
            when (outputBufferIndex) {
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                }
                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                }
                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                }
                else -> {
                    timestamp = bufferInfo.presentationTimeUs
                    mediaCodec.releaseOutputBuffer(outputBufferIndex, timestamp < endTime)
                    Log.e("debug", "releaseOutputBuffer, filePath = $filePath, timestamp < endTime = ${timestamp < endTime}")
                    return timestamp < endTime
                }
            }
            if (sampleSize == -1) {
                Log.e("debug", "decodeToSurface return false 0")
                return false
            }
        }
        Log.e("debug", "decodeToSurface return false 1")
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

    fun getTimestamps(): List<Long> {
        val timestamps = mutableListOf<Long>()
        try {
            var eos = false
            val extractor = MediaExtractor()
            extractor.setDataSource(filePath)
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime.startsWith("video/")) {
                    extractor.selectTrack(i)
                    break
                }
            }
            val buffer = ByteBuffer.allocate(getVideoWidth() * getVideoHeight())
            while (!eos) {
                val sampleSize = extractor.readSampleData(buffer, 0)
                if (sampleSize < 0) {
                    eos = true
                } else {
                    val t = extractor.sampleTime
                    timestamps.add(t)
                    extractor.advance()
                }
            }
        } catch (e: Exception) {
        }
        return timestamps
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
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }
}
