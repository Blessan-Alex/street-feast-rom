package com.streatfeast.app.utils

import android.content.Context
import android.media.MediaPlayer
import com.streatfeast.app.R

object SoundManager {
    
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Play sound by type
     */
    fun playSound(context: Context, soundType: String) {
        try {
            // Release previous player if exists
            mediaPlayer?.release()
            
            // Get resource ID based on sound type
            val resourceId = when (soundType) {
                Constants.SOUND_PING -> R.raw.ping
                Constants.SOUND_CLICK -> R.raw.click
                Constants.SOUND_BUZZER -> R.raw.buzzer
                else -> return
            }
            
            // Create and play
            mediaPlayer = MediaPlayer.create(context, resourceId)
            mediaPlayer?.apply {
                setOnCompletionListener {
                    release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Release media player resources
     */
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
    }
}


