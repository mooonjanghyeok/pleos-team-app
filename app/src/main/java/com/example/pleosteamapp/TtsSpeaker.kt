
package com.example.pleosteamapp

import android.util.Log

/**
 * GuideEngine과 Pleos TTS를 연결하는 클래스.
 *
 * 새로운 TTS 객체를 생성하지 않고
 * MainActivity의 기존 PleosTtsProbe를 재사용한다.
 */
class TtsSpeaker(
    private val ttsProbe: PleosTtsProbe
) : Speaker {

    private val tag = "KDRIVE_TTS"

    override fun speak(text: String) {
        if (text.isBlank()) return

        Log.d(tag, "GUIDE SPEAK: $text")
        ttsProbe.speakText(text)
    }

    override fun stop() {
        Log.d(tag, "GUIDE STOP")
        ttsProbe.stopSpeech()
    }
}
