package com.caca.fantasma.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.caca.fantasma.R

class SoundManager(context: Context) {

    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = intArrayOf(
        pool.load(context, R.raw.snd_click, 1),
        pool.load(context, R.raw.snd_clue, 1),
        pool.load(context, R.raw.snd_coin, 1),
        pool.load(context, R.raw.snd_unlock, 1),
        pool.load(context, R.raw.snd_victory, 1),
        pool.load(context, R.raw.snd_defeat, 1),
        pool.load(context, R.raw.snd_hurt, 1)
    )

    fun click() = play(0)
    fun clue() = play(1)
    fun coin() = play(2)
    fun unlock() = play(3)
    fun victory() = play(4)
    fun defeat() = play(5)
    fun hurt() = play(6)

    private fun play(i: Int) {
        try {
            pool.play(ids[i], 0.8f, 0.8f, 1, 0, 1f)
        } catch (_: Exception) {
        }
    }

    fun release() = try {
        pool.release()
    } catch (_: Exception) {
    }
}