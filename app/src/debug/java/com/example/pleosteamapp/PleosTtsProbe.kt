
package com.example.pleosteamapp

import android.content.Context
import android.util.Log
import java.util.Locale

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener

class PleosTtsProbe(
    context: Context,
    private val onStatusChanged: (String) -> Unit
) {

    private val tag = "PLEOS_TTS_PC1"

    // 1. 차량 내부 엔진을 사용하는 TTS 객체
    private val tts = TextToSpeech(
        context.applicationContext,
        Mode.ON_DEVICE
    )

    private var initialized = false
    private var released = false
    private var listenerRegistered = false

    // 2. 음성 출력 이벤트 확인
    private val eventListener = object : EventListener {

        override fun onReady() {
            report("TTS READY")
        }

        override fun onStart() {
            report("PLAYBACK START")
        }

        override fun onUpdatedRms(rms: Double) {
            // 음성 출력 레벨 업데이트
        }

        override fun onDone() {
            report("PLAYBACK DONE")
        }

        override fun onStop() {
            report("PLAYBACK STOP")
        }

        override fun onError(errorType: ErrorType) {
            reportError("TTS ERROR = $errorType")
        }
    }

    // 3. TTS SDK 초기화
    fun start() {

        if (initialized || released) return

        report("INITIALIZING TTS")

        try {
            tts.initialize(object : OnInitializeListener {

                override fun onSuccess() {

                    if (released) return

                    try {
                        tts.addEventListener(eventListener)
                        listenerRegistered = true

                        // 영어 설정
                        tts.setLocale(Locale.US)

                        initialized = true

                        report("INITIALIZE SUCCESS - en-US")

                    } catch (error: Exception) {
                        reportError("TTS SETUP FAILED", error)
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    reportError(
                        "INITIALIZE FAILED = $errorType"
                    )
                }
            })

        } catch (error: Exception) {
            reportError("INITIALIZE EXCEPTION", error)
        }
    }

    // 4. 영어 음성 출력
    fun speakEnglish() {

        if (!initialized || released) {
            report("TTS NOT READY")
            return
        }

        try {
            report("SPEAK REQUEST")

            tts.speak(
                "Welcome to Korea. " +
                        "This is your driving assistant. " +
                        "Your route may include toll gates " +
                        "and unfamiliar road signs. " +
                        "Please follow the traffic rules " +
                        "and drive safely."
            )

        } catch (error: Exception) {
            reportError("SPEAK FAILED", error)
        }
    }

    // 5. 음성 출력 중지
    fun stopSpeech() {

        if (!initialized || released) {
            report("TTS NOT READY")
            return
        }

        try {
            report("STOP REQUEST")
            tts.stop()

        } catch (error: Exception) {
            reportError("STOP FAILED", error)
        }
    }

    // 6. SDK 종료
    fun release() {

        if (released) return

        released = true
        initialized = false

        try {
            if (listenerRegistered) {
                tts.removeEventListener(eventListener)
                listenerRegistered = false
            }
        } catch (error: Exception) {
            Log.e(tag, "LISTENER RELEASE FAILED", error)
        } finally {
            try {
                tts.release()
                Log.d(tag, "TTS RELEASED")
            } catch (error: Exception) {
                Log.e(tag, "TTS RELEASE FAILED", error)
            }
        }
    }

    private fun report(message: String) {
        Log.d(tag, message)
        onStatusChanged(message)
    }

    private fun reportError(
        message: String,
        error: Exception? = null
    ) {
        Log.e(tag, message, error)
        onStatusChanged(message)
    }
}
