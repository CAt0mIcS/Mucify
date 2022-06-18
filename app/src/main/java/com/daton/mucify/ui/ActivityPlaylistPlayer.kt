package com.daton.mucify.ui

import android.os.Bundle
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.daton.media.MediaController
import com.daton.media.data.MediaId
import com.daton.media.ext.toMediaId
import com.daton.mucify.databinding.ActivityPlaylistPlayerBinding

class ActivityPlaylistPlayer : AppCompatActivity() {

    private lateinit var binding: ActivityPlaylistPlayerBinding
    private val controller = MediaController()

    private var playbackStrings = mutableListOf<MediaId>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaylistPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        controller.create(this)

        binding.rvPlaylistItems.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            playbackStrings
        )

        binding.rvPlaylistItems.setOnItemClickListener { adapterView, _, i, _ ->
            controller.mediaId = adapterView.getItemAtPosition(i) as MediaId
            controller.play()
        }


        controller.onMediaIdChanged = {
            binding.txtPlaylistTitle.text = controller.playlistName
            binding.txtTitle.text = controller.title
        }

        controller.onConnected = {
            // Getting media id requires connection to MediaService
            controller.subscribe(controller.mediaId.baseMediaId.serialize()) { playlistItems ->
                playbackStrings.addAll(playlistItems.map { it.mediaId!!.toMediaId() })
                (binding.rvPlaylistItems.adapter as ArrayAdapter<*>).notifyDataSetChanged()
            }

            controller.onMediaIdChanged!!()
        }
    }

    override fun onStart() {
        super.onStart()
        controller.connect(this)
    }

    override fun onStop() {
        super.onStop()
        controller.disconnect()
    }

}