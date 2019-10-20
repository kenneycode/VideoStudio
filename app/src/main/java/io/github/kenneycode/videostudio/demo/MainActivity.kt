package io.github.kenneycode.videostudio.demo

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import io.github.kenneycode.videostudio.demo.samples.SampleVideoEncoder
import kotlinx.android.synthetic.main.activity_main.*

/**
 *
 *      Coded by kenney
 *
 *      http://www.github.com/kenneycode
 *
 **/

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val sampleItems = arrayOf(
                SampleItem(getString(R.string.sample_video_decoder), SampleVideoDecoder::class.java),
                SampleItem(getString(R.string.sample_video_encoder), SampleVideoEncoder::class.java)
        )

        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        samples.layoutManager = layoutManager
        samples.adapter = SampleAdapter(sampleItems)
    }

    inner class SampleItem(val sampleSame: String, val sampleActivity: Class<*>)

    inner class SampleAdapter(private val sampleItems: Array<SampleItem>) : RecyclerView.Adapter<VH>() {

        override fun onCreateViewHolder(p0: ViewGroup, p1: Int): VH {
            return VH(LayoutInflater.from(p0.context).inflate(R.layout.item_sample, null, false))
        }

        override fun getItemCount(): Int {
            return sampleItems.size
        }

        override fun onBindViewHolder(vh: VH, position: Int) {
            vh.button.text = sampleItems[position].sampleSame
            vh.button.setOnClickListener {
                val intent = Intent(this@MainActivity, sampleItems[position].sampleActivity)
                intent.putExtra(Constants.KEY_SAMPLE_NAME, sampleItems[position].sampleSame)
                this@MainActivity.startActivity(intent)
            }
        }

    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button = itemView.findViewById<Button>(R.id.button)
    }

}
