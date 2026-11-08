
package com.example.pleosteamapp

import android.content.Context
import android.util.Log
import java.util.Locale

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener

class PleosTtsProbe(context: Context) {

    private val tag = "PLEOS_TTS_TEST"

    private val tts = TextToSpeech(
        context.applicationContext,
        Mode.ON_DEVICE
    )

    @Volatile
    private var initialized = false

    @Volatile
    private var released = false

    private var startRequested = false
    private var listenerRegistered = false

    @Volatile
    private var pendingText: String? = null

    private val eventListener = object : EventListener {

        override fun onReady() {
            Log.d(tag, "TTS READY")
        }

        override fun onStart() {
            Log.d(tag, "PLAYBACK START")
        }

        override fun onUpdatedRms(rms: Double) {
            // 음성 출력 상태
        }

        override fun onDone() {
            Log.d(tag, "PLAYBACK DONE")
        }

        override fun onStop() {
            Log.d(tag, "PLAYBACK STOP")
        }

        override fun onError(errorType: ErrorType) {
            Log.e(tag, "TTS ERROR: $errorType")
        }
    }

    // TTS 초기화
    fun start() {

        if (released || initialized || startRequested) return

        startRequested = true
        Log.d(tag, "INITIALIZING TTS")

        try {
            tts.initialize(object : OnInitializeListener {

                override fun onSuccess() {

                    if (released) return

                    try {
                        if (!listenerRegistered) {
                            tts.addEventListener(eventListener)
                            listenerRegistered = true
                        }

                        tts.setLocale(Locale.US)
                        initialized = true

                        Log.d(tag, "INITIALIZE SUCCESS")
                        Log.d(tag, "ENGLISH SET")

                        val waitingText = pendingText
                        pendingText = null

                        if (!waitingText.isNullOrBlank()) {
                            speakText(waitingText)
                        }

                    } catch (error: Exception) {
                        Log.e(tag, "TTS SETUP FAILED", error)
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    startRequested = false
                    Log.e(
                        tag,
                        "INITIALIZE FAILED: $errorType"
                    )
                }
            })

        } catch (error: Exception) {
            startRequested = false
            Log.e(tag, "TTS INITIALIZATION ERROR", error)
        }
    }

    // 원하는 영어 문장 재생
    fun speakText(text: String) {

        if (released) {
            Log.w(tag, "TTS ALREADY RELEASED")
            return
        }

        val message = text.trim()

        if (message.isEmpty()) {
            Log.w(tag, "EMPTY SPEECH TEXT")
            return
        }

        if (!initialized) {
            pendingText = message
            Log.w(tag, "TTS NOT READY - MESSAGE SAVED")
            return
        }

        try {
            Log.d(tag, "SPEAK TEXT: $message")
            tts.speak(message)

        } catch (error: Exception) {
            Log.e(tag, "SPEAK FAILED", error)
        }
    }

    // 기존 테스트 버튼 호환
    fun speakEnglish() {

        speakText(
            "Welcome to Korea. " +
                    "This is your driving assistant. " +
                    "You are approaching a roundabout. " +
                    "In Korea, vehicles already circulating " +
                    "have priority. " +
                    "Please follow road signs " +
                    "and drive safely."
        )
    }

    // 음성 중지
    fun stopSpeech() {

        pendingText = null

        if (released || !initialized) return

        try {
            Log.d(tag, "STOP REQUEST")
            tts.stop()

        } catch (error: Exception) {
            Log.e(tag, "STOP FAILED", error)
        }
    }

    // SDK 리소스 정리
    fun release() {

        if (released) return

        released = true
        initialized = false
        pendingText = null

        try {
            if (listenerRegistered) {
                tts.removeEventListener(eventListener)
                listenerRegistered = false
            }
        } catch (error: Exception) {
            Log.w(tag, "LISTENER REMOVE FAILED", error)
        }

        try {
            tts.release()
            Log.d(tag, "TTS RELEASED")

        } catch (error: Exception) {
            Log.e(tag, "TTS RELEASE FAILED", error)
        }
    }
}
