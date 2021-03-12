package io.github.kenneycode.videostudio.Util

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer
import android.media.MediaCodec
import android.util.Log

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode/VideoStudio
 *
 *      音频/视频裁剪器
 *      audio/video clipper
 *
 **/

class MediaClipper {

    companion object {

        enum class MEDIA_TYPE {
            AUDIO,
            VIDEO
        }

        /**
         *
         * 音频/视频时长裁剪
         *
         * @param inputPath     源音频/视频路径
         * @param outputPath    裁剪后的音频/视频路径
         * @param startTime     裁剪开始时间，单位是秒
         * @param endTime       裁剪结束时间，单位是秒
         * @param mediaType     媒体类型，默认是视频
         *  @see MEDIA_TYPE
         *
         */
        fun clip(inputPath: String, outputPath: String, startTime: Float, endTime: Float, mediaType:MEDIA_TYPE = MEDIA_TYPE.VIDEO) {
            val startTimeUs = (startTime * 1000000).toLong()
            val endTimeUs = (endTime * 1000000).toLong()
            MediaExtractor().apply {
                setDataSource(inputPath)
                val extractorTrackIndex = getTrackIndex(this, mediaType)
                val extractorAudioTrackIndex = getTrackIndex(this, MEDIA_TYPE.AUDIO)
                selectTrack(extractorTrackIndex)
                seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                val videoFormat = getTrackFormat(extractorTrackIndex)
                val audioFormat = getTrackFormat(extractorAudioTrackIndex)
                videoFormat.setInteger(MediaFormat.KEY_DURATION, (endTimeUs - startTimeUs).toInt())
                val mediaMuxer = MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
                val rotation = videoFormat.getInteger(MediaFormat.KEY_ROTATION)
                mediaMuxer.setOrientationHint(rotation)
                val muxerTrackIndex = mediaMuxer.addTrack(videoFormat)
                val muxerAudioTrackIndex = mediaMuxer.addTrack(audioFormat)
                mediaMuxer.start()
                val byteBuffer = ByteBuffer.allocate(videoFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE))
                while (true) {
                    val sampleSize = readSampleData(byteBuffer, 0)
                    if (sampleSize < 0 || sampleTime > endTimeUs) {
                        unselectTrack(extractorTrackIndex)
                        break
                    }
                    val bufferInfo = MediaCodec.BufferInfo().apply {
                        offset = 0
                        size = sampleSize
                        flags = sampleFlags
                        presentationTimeUs = sampleTime
                    }
                    mediaMuxer.writeSampleData(muxerTrackIndex, byteBuffer, bufferInfo)
                    advance()
                }
                //音频
                selectTrack(extractorAudioTrackIndex)
                seekTo(startTimeUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                while (true) {
                    val sampleSize = readSampleData(byteBuffer, 0)
                    if (sampleSize < 0 || sampleTime > endTimeUs) {
                        unselectTrack(extractorAudioTrackIndex)
                        break
                    }

                    val bufferInfo = MediaCodec.BufferInfo().apply {
                        offset = 0
                        size = sampleSize
                        flags = sampleFlags
                        presentationTimeUs = sampleTime
                    }

                    mediaMuxer.writeSampleData(muxerAudioTrackIndex, byteBuffer, bufferInfo)
                    advance()
                }

                release()
                mediaMuxer.stop()
                mediaMuxer.release()
            }
        }

        /**
         *
         * 获取音频/视频轨道index
         *
         * @param mediaExtractor MediaExtractor
         * @param mediaType     媒体类型
         *  @see MEDIA_TYPE
         *
         * @return 轨道index
         *
         */
        private fun getTrackIndex(mediaExtractor: MediaExtractor, mediaType: MEDIA_TYPE): Int {
            for (trackIndex in 0 until mediaExtractor.trackCount) {
                mediaExtractor.getTrackFormat(trackIndex).let { trackFormat ->
                    val mineType = if (mediaType == MEDIA_TYPE.VIDEO) { "video" } else { "audio" }
                    if (trackFormat.getString(MediaFormat.KEY_MIME).contains(mineType)) {
                        return trackIndex
                    }
                }
            }
            return -1
        }

    }

}
