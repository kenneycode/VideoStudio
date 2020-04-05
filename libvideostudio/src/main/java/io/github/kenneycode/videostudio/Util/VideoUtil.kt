package io.github.kenneycode.videostudio.util

import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import io.github.kenneycode.videostudio.LogUtil
import io.github.kenneycode.videostudio.bean.Size
import java.io.IOException

/**
 *
 * Coded by kenney
 *
 * 视频相关util类
 *
 */

class VideoUtil {

    companion object {

        fun getVideoVido(videoPath: String, considerRotation: Boolean = true): Size {
            val mr = MediaMetadataRetriever()
            mr.setDataSource(videoPath)
            val rotation = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION).toInt()
            var width = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH).toInt()
            var height = mr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT).toInt()
            if (considerRotation && (rotation == 90 || rotation == 270)) {
                val temp = width
                width = height
                height = temp
            }
            mr.release()
            return Size(width, height)
        }

        fun getVideoFrameRate(videoPath: String): Int {
            val extractor = MediaExtractor()
            var frameRate = -1
            try {
                extractor.setDataSource(videoPath)
                val tracks = extractor.trackCount
                for (i in 0 until tracks) {
                    val format = extractor.getTrackFormat(i)
                    if (format.getString(MediaFormat.KEY_MIME).startsWith("video/")) {
                        if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE)
                        }
                    }
                }
            } catch (e: IOException) {
                LogUtil.loge(e)
            } finally {
                extractor.release()
            }
            return frameRate
        }

    }

}
