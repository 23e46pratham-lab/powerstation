package com.example.utils

import android.media.AudioManager
import android.media.ToneGenerator

object SoundManager {
    private var toneGenerator: ToneGenerator? = null

    fun init() {
        if (toneGenerator == null) {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 100)
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    fun playConnectSound() {
        Thread {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 100)
            Thread.sleep(150)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
        }.start()
    }

    fun playDisconnectSound() {
        Thread {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 100)
            Thread.sleep(150)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_NACK, 200)
        }.start()
    }
    
    fun playChargingStartedSound() {
        Thread {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 100)
            Thread.sleep(150)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_KEYPAD_VOLUME_KEY_LITE, 150)
        }.start()
    }
    
    fun playDischargingStartedSound() {
        Thread {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
            Thread.sleep(150)
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 150)
        }.start()
    }

    fun playLowBatterySound() {
        Thread {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_NETWORK_BUSY, 200)
            Thread.sleep(250)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_NETWORK_BUSY, 200)
        }.start()
    }
    
    fun playCutoffSound() {
        Thread {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 300)
            Thread.sleep(350)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 300)
            Thread.sleep(350)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ABBR_INTERCEPT, 500)
        }.start()
    }
    
    fun playClickSound() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 50)
    }
}
