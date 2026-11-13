package com.example.pleosteamapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener

import java.util.Locale

class KDriveAudioGateLabActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KDRIVE_AUDIO_GATE"
    }

    private lateinit var audioManager: AudioManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speech: TextToSpeech? = null
    private var listenerAdded = false
    private var closed = false

    private var ttsReady by mutableStateOf(false)
    private var speechRequested by mutableStateOf(false)
    private var navActive by mutableStateOf(false)
    private var navObserved by mutableStateOf(false)
    private var navCount by mutableIntStateOf(0)
    private var playbackTypes by mutableStateOf("No data")
    private var status by mutableStateOf("초기화 중")
    private var lastDecision by mutableStateOf("아직 요청 없음")

    private val playbackCallback =
        object : AudioManager.AudioPlaybackCallback() {
            override fun onPlaybackConfigChanged(
                configs: MutableList<AudioPlaybackConfiguration>?
            ) {
                updateAudioState(
                    configs ?: emptyList(),
                    "CALLBACK"
                )
            }
        }

    private fun updateAudioState(
        configs: List<AudioPlaybackConfiguration>,
        reason: String
    ) {
        if (closed) return

        val types = configs.groupingBy {
            it.audioAttributes.usage
        }.eachCount()

        navCount = configs.count {
            it.audioAttributes.usage ==
                AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
        }

        navActive = navCount > 0

        if (navActive) {
            navObserved = true
        }

        playbackTypes = if (types.isEmpty()) {
            "No active playback reported"
        } else {
            types.entries.sortedBy { it.key }.joinToString {
                "usage=${it.key}, count=${it.value}"
            }
        }

        Log.d(
            TAG,
            "AUDIO_STATE reason=$reason " +
                "navCount=$navCount total=${configs.size} " +
                "types=[$playbackTypes]"
        )

        if (navActive && speechRequested) {
            speechRequested = false
            lastDecision = "NAVIGATION 유형 감지: 우리 TTS 중지"

            Log.w(
                TAG,
                "TTS_STOP_NAV_AUDIO_DETECTED " +
                    "(source identity not verified)"
            )

            try {
                speech?.stop()
            } catch (e: Exception) {
                Log.e(TAG, "TTS_STOP_FAILED", e)
            }
        }
    }

    private fun refreshAudio() {
        try {
            updateAudioState(
                audioManager.activePlaybackConfigurations,
                "MANUAL_REFRESH"
            )
        } catch (e: Exception) {
            status = "오디오 조회 실패: ${e.message}"
            Log.e(TAG, "AUDIO_QUERY_FAILED", e)
        }
    }

    private val ttsListener = object : EventListener {
        override fun onReady() {
            reportTts("READY")
        }

        override fun onStart() {
            reportTts("PLAYBACK START")
        }

        override fun onUpdatedRms(rms: Double) {}

        override fun onDone() {
            reportTts("PLAYBACK DONE", true)
        }

        override fun onStop() {
            reportTts("PLAYBACK STOP", true)
        }

        override fun onError(errorType: ErrorType) {
            reportTts("ERROR: $errorType", true)
        }
    }

    private fun reportTts(
        message: String,
        finished: Boolean = false
    ) {
        Log.d(TAG, "TTS_EVENT $message")

        runOnUiThread {
            if (closed) return@runOnUiThread

            status = message

            if (finished) {
                speechRequested = false
            }
        }
    }

    private fun initializeTts() {
        try {
            val instance = TextToSpeech(
                applicationContext,
                Mode.ON_DEVICE
            )

            speech = instance

            instance.initialize(
                object : OnInitializeListener {
                    override fun onSuccess() {
                        if (closed) return

                        try {
                            instance.addEventListener(ttsListener)
                            listenerAdded = true
                            instance.setLocale(Locale.US)

                            runOnUiThread {
                                if (!closed) {
                                    ttsReady = true
                                    status = "TTS READY"
                                    Log.d(TAG, "SDK_INIT_SUCCESS")
                                }
                            }
                        } catch (e: Exception) {
                            reportTts(
                                "SDK SETUP ERROR: ${e.message}"
                            )
                        }
                    }

                    override fun onFailure(errorType: ErrorType) {
                        reportTts(
                            "SDK INIT ERROR: $errorType"
                        )
                    }
                }
            )
        } catch (e: Exception) {
            reportTts("SDK EXCEPTION: ${e.message}")
        }
    }

    private fun requestSpeech() {
        if (!ttsReady || closed || speechRequested) return

        // 말하기 직전 활성 오디오를 재확인한다.
        refreshAudio()

        if (navActive) {
            lastDecision = "내비 음성 유형 재생 중: 요청 차단"

            Log.w(
                TAG,
                "TTS_BLOCKED_NAV_ACTIVE navCount=$navCount"
            )
            return
        }

        val message =
            "Audio conflict test. " +
            "You are approaching a roundabout. " +
            "Slow down and yield to vehicles already circulating. " +
            "This is a test announcement from K Drive Guide."

        try {
            speechRequested = true
            lastDecision = "재생 요청 (내비 감지 없음)"

            Log.d(TAG, "TTS_REQUEST")

            val instance = speech
                ?: throw IllegalStateException("TTS not initialized")

            instance.speak(message)
        } catch (e: Exception) {
            speechRequested = false
            lastDecision = "음성 요청 실패"
            Log.e(TAG, "TTS_REQUEST_FAILED", e)
        }
    }

    private fun stopSpeech() {
        try {
            speechRequested = false
            speech?.stop()
            lastDecision = "사용자가 재생 중지"
            Log.d(TAG, "TTS_MANUAL_STOP")
        } catch (e: Exception) {
            Log.e(TAG, "TTS_MANUAL_STOP_FAILED", e)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.registerAudioPlaybackCallback(
            playbackCallback,
            mainHandler
        )

        refreshAudio()
        initializeTts()

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "K-DRIVE AUDIO TEST",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text("내비게이션 음성 감지 실험")
                    HorizontalDivider()

                    Text(
                        "NAVIGATION 유형 재생 중: " +
                            if (navActive) "YES" else "NO"
                    )

                    Text("감지된 NAVIGATION 재생 수: $navCount")
                    Text("NAVIGATION 유형 관측 이력: $navObserved")
                    Text("활성 오디오: $playbackTypes")

                    Button(
                        onClick = { refreshAudio() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("오디오 상태 새로 확인")
                    }

                    HorizontalDivider()

                    Text("Pleos TTS: $status")
                    Text("우리 음성 요청 활성: $speechRequested")
                    Text("마지막 판단: $lastDecision")

                    Button(
                        onClick = { requestSpeech() },
                        enabled = ttsReady && !speechRequested,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("영어 음성 테스트 (감지 시 차단)")
                    }

                    Button(
                        onClick = { stopSpeech() },
                        enabled = ttsReady,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("음성 중지")
                    }

                    HorizontalDivider()

                    Text(
                        "이 화면은 Debug 전용 감지 실험입니다."
                    )

                    Text(
                        "NAVIGATION 유형은 오디오의 용도이며, " +
                            "기본 내비 앱의 고유 신원은 아닙니다."
                    )

                    Text(
                        "실제 내비 음성 재생 여부와 " +
                            "관측값을 반드시 비교하세요."
                    )

                    Text("Logcat: KDRIVE_AUDIO_GATE")
                }
            }
        }
    }

    override fun onDestroy() {
        closed = true

        try {
            audioManager.unregisterAudioPlaybackCallback(
                playbackCallback
            )
        } catch (e: Exception) {
            Log.e(TAG, "AUDIO_UNREGISTER_FAILED", e)
        }

        try {
            speech?.let {
                it.stop()
                if (listenerAdded) {
                    it.removeEventListener(ttsListener)
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS_RELEASE_FAILED", e)
        }

        super.onDestroy()
    }
}