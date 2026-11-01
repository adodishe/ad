package com.auratune.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

class StreamProviders(private val context: Context) {
    fun getPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context).build()
    }
}
