package io.github.kenneycode.videostudio.demo.samples

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.kenneycode.videostudio.Util.MediaClipper
import io.github.kenneycode.videostudio.demo.R

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode/VideoStudio
 *
 *      音频/视频时长裁剪
 *      audio/video clip
 *
 **/

class SampleMediaClip : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_common)
        MediaClipper.clip("/sdcard/0.mp4", "/sdcard/1.mp4", 2f, 5f)
    }

}
