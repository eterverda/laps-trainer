package ru.fpvladder.laps.trainer.audio

import android.content.Context
import android.media.SoundPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.fpvladder.laps.trainer.R

object SoundManager {
    private var soundPool: SoundPool? = null
    private var stageId: Int = 0
    private var buzzerId: Int = 0
    private var gateId: Int = 0
    private var vehicleLostId: Int = 0
    private var isLoaded = false

    fun init(context: Context) {
        if (soundPool != null) return
        val pool = SoundPool.Builder().setMaxStreams(3).build()
        pool.setOnLoadCompleteListener { _, _, _ ->
            isLoaded = true
        }
        stageId = pool.load(context.applicationContext, R.raw.stage, 1)
        buzzerId = pool.load(context.applicationContext, R.raw.buzzer, 1)
        gateId = pool.load(context.applicationContext, R.raw.gate, 1)
        vehicleLostId = pool.load(context.applicationContext, R.raw.vehicle_lost, 1)
        soundPool = pool
    }

    fun playStage(volume: Float = 1f) {
        soundPool?.let { if (isLoaded) it.play(stageId, volume, volume, 0, 0, 1f) }
    }

    fun playBuzzer(volume: Float = 1f) {
        soundPool?.let { if (isLoaded) it.play(buzzerId, volume, volume, 0, 0, 1f) }
    }

    fun playGate(volume: Float = 1f) {
        soundPool?.let { if (isLoaded) it.play(gateId, volume, volume, 0, 0, 1f) }
    }

    fun playVehicleLost(volume: Float = 1f) {
        soundPool?.let { if (isLoaded) it.play(vehicleLostId, volume, volume, 0, 0, 1f) }
    }

    fun playStageSequence(scope: CoroutineScope, volume: Float = 1f): Job {
        return scope.launch {
            repeat(3) { index ->
                playStage(volume)
                if (index < 2) {
                    delay(STAGE_DURATION_MS + STAGE_DELAY_MS)
                }
            }
        }
    }

    fun stop() {
        soundPool?.autoPause()
    }

    fun release() {
        soundPool?.release()
        soundPool = null
    }
}
