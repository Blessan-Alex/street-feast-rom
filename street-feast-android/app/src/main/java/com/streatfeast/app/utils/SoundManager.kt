package com.streatfeast.app.utils

import android.content.Context
import android.media.MediaPlayer
import com.streatfeast.app.R

object SoundManager {
    
    private var mediaPlayer: MediaPlayer? = null
    private var stopRunnable: Runnable? = null
    
    fun playSoundWithFallback(context: Context, soundType: String, loopMillis: Long = 0) {
        try {
            stopRunnable = null
            mediaPlayer?.release()
            mediaPlayer = null

            val resourceId = resolveSoundId(context, soundType) ?: return
            val player = MediaPlayer.create(context, resourceId) ?: return
            mediaPlayer = player

            if (loopMillis > 0) {
                player.isLooping = true
                player.start()
                // stop after duration
                val handler = android.os.Handler(context.mainLooper)
                stopRunnable = Runnable {
                    player.stop()
                    player.release()
                    mediaPlayer = null
                }
                handler.postDelayed(stopRunnable!!, loopMillis)
            } else {
                player.setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                player.start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    fun release() {
        mediaPlayer?.release()
        mediaPlayer = null
        stopRunnable = null
    }

    private fun resolveSoundId(context: Context, soundType: String): Int? {
        return when (soundType) {
            Constants.SOUND_PING -> {
                val longId = context.resources.getIdentifier("ping_long", "raw", context.packageName)
                if (longId != 0) longId else R.raw.ping
            }
            Constants.SOUND_CLICK -> R.raw.click
            Constants.SOUND_BUZZER -> R.raw.buzzer
            else -> null
        }
    }
}


