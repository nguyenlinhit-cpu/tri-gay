package local.vn.suppermario

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

class SoundManager(context: Context) {
    private val soundPool: SoundPool
    private val soundJump: Int
    private val soundCoin: Int
    private val soundStomp: Int
    private val soundHurt: Int

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        soundJump = soundPool.load(context, R.raw.sfx_jump, 1)
        soundCoin = soundPool.load(context, R.raw.sfx_coin, 1)
        soundStomp = soundPool.load(context, R.raw.sfx_stomp, 1)
        soundHurt = soundPool.load(context, R.raw.sfx_hurt, 1)
    }

    fun playJump() { soundPool.play(soundJump, 1f, 1f, 0, 0, 1f) }
    fun playCoin() { soundPool.play(soundCoin, 1f, 1f, 0, 0, 1f) }
    fun playStomp() { soundPool.play(soundStomp, 1f, 1f, 0, 0, 1f) }
    fun playHurt() { soundPool.play(soundHurt, 1f, 1f, 0, 0, 1f) }

    fun release() {
        soundPool.release()
    }
}
