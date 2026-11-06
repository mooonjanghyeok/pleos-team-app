
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

    // 1. TTS 객체 생성
    private val tts = TextToSpeech(
        context.applicationContext,
        Mode.ON_DEVICE
    )

    // 2. TTS 상태 저장
    private var initialized = false
    private var listenerRegistered = false
    private var released = false

    // 3. TTS 이벤트 리스너
    private val eventListener = object : EventListener {

        override fun onReady() {
            Log.d(tag, "TTS READY")
        }

        override fun onStart() {
            Log.d(tag, "PLAYBACK START")
        }

        override fun onUpdatedRms(rms: Double) {
            // 음성 출력 레벨
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

    // 4. TTS 초기화
    fun start() {

        if (released) return

        Log.d(tag, "INITIALIZING TTS")

        tts.initialize(object : OnInitializeListener {

            override fun onSuccess() {

                if (released) return

                initialized = true

                Log.d(tag, "INITIALIZE SUCCESS")

                // 이벤트 리스너 등록
                tts.addEventListener(eventListener)
                listenerRegistered = true

                // 영어 설정
                tts.setLocale(Locale.US)

                Log.d(tag, "ENGLISH SET")
            }

            override fun onFailure(errorType: ErrorType) {
                Log.e(
                    tag,
                    "INITIALIZE FAILED: $errorType"
                )
            }
        })
    }

    // 5. 영어 음성 재생
    fun speakEnglish() {

        if (!initialized || released) {
            Log.w(tag, "TTS NOT INITIALIZED")
            return
        }

        Log.d(tag, "SPEAK REQUEST")

        tts.speak(
            "Welcome to Korea. " +
                    "This is your driving assistant. " +
                    "You are approaching a roundabout. " +
                    "In Korea, vehicles already circulating " +
                    "have priority. " +
                    "Please follow road signs " +
                    "and drive safely."
        )
    }

    // 6. 영어 음성 중지
    fun stopSpeech() {

        if (!initialized || released) {
            Log.w(tag, "TTS NOT INITIALIZED")
            return
        }

        Log.d(tag, "STOP REQUEST")

        tts.stop()
    }

    // 7. SDK 종료 및 자원 정리
    fun release() {

        if (released) return

        released = true
        initialized = false

        try {
            if (listenerRegistered) {
                tts.removeEventListener(eventListener)
                listenerRegistered = false
            }
        } finally {
            tts.release()
        }

        Log.d(tag, "TTS RELEASED")
    }
}
