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

class KDriveAudioAutoLabActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KDRIVE_AUDIO_AUTO"
    }

    private val handler = Handler(Looper.getMainLooper())

    private lateinit var audioManager: AudioManager
    private var speech: TextToSpeech? = null
    private var listenerAdded = false
    private var closed = false
    private var session = 0

    private var ready by mutableStateOf(false)
    private var running by mutableStateOf(false)
    private var simulatedNav by mutableStateOf(false)
    private var speechRequested by mutableStateOf(false)

    private var phase by mutableStateOf("IDLE")
    private var status by mutableStateOf("SDK 초기화 중")
    private var history by mutableStateOf<List<String>>(emptyList())
    private var observedAudio by mutableStateOf("미관측")

    private var passBlock by mutableStateOf(false)
    private var passAfterNav by mutableStateOf(false)
    private var passInterrupt by mutableStateOf(false)

    private var interruptScheduled = false
    private var lastObservedNavCount = -1

    private fun record(message: String) {
        Log.d(TAG, message)
        history = (history + message).takeLast(45)
    }

    // 실제 Android 오디오 유형 관찰용.
    // 관찰 결과는 모의 시험의 합격 판정에 사용하지 않는다.
    private val playbackCallback =
        object : AudioManager.AudioPlaybackCallback() {

            override fun onPlaybackConfigChanged(
                configs: MutableList<AudioPlaybackConfiguration>?
            ) {
                val active = configs.orEmpty()

                val navCount = active.count {
                    it.audioAttributes.usage ==
                        AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
                }

                val summary = if (active.isEmpty()) {
                    "활성 오디오 없음"
                } else {
                    active.groupingBy {
                        it.audioAttributes.usage
                    }.eachCount().entries.joinToString {
                        "usage=${it.key}: ${it.value}"
                    }
                }

                runOnUiThread {
                    if (closed) return@runOnUiThread

                    observedAudio = summary

                    if (navCount != lastObservedNavCount) {
                        lastObservedNavCount = navCount
                        record("OBSERVED_NAV_USAGE count=$navCount")
                    }
                }
            }
        }

    private fun later(delayMs: Long, action: () -> Unit) {
        val expectedSession = session

        handler.postDelayed({
            if (!closed && running && expectedSession == session) {
                action()
            }
        }, delayMs)
    }

    private val ttsListener = object : EventListener {

        override fun onReady() {
            Log.d(TAG, "TTS_READY_EVENT")
        }

        override fun onStart() {
            runOnUiThread {
                if (closed) return@runOnUiThread

                status = "TTS 재생 중"
                record("TTS_PLAYBACK_START phase=$phase")

                if (running &&
                    phase == "B_TTS_FIRST" &&
                    !interruptScheduled
                ) {
                    interruptScheduled = true

                    later(900) {
                        simulatedNav = true
                        phase = "B_WAIT_STOP_CALLBACK"

                        record("TEST_B_NAV_EVENT_START")

                        if (speechRequested) {
                            record("TEST_B_STOP_REQUESTED")

                            try {
                                speech?.stop()
                            } catch (e: Exception) {
                                record("TEST_B_STOP_ERROR ${e.message}")
                            }

                            speechRequested = false
                        } else {
                            record("TEST_B_TTS_ALREADY_INACTIVE")
                        }

                        later(1700) {
                            simulatedNav = false
                            record("TEST_B_NAV_EVENT_END")
                            finishTest()
                        }
                    }
                }
            }
        }

        override fun onUpdatedRms(rms: Double) {}

        override fun onDone() {
            runOnUiThread {
                if (closed) return@runOnUiThread

                speechRequested = false
                status = "TTS 재생 완료"
                record("TTS_PLAYBACK_DONE phase=$phase")

                if (running && phase == "A_TTS_ALLOWED") {
                    passAfterNav = true

                    later(700) {
                        startTestB()
                    }
                } else if (running && phase == "B_TTS_FIRST") {
                    record("TEST_B_ENDED_BEFORE_INTERRUPT")
                    finishTest()
                }
            }
        }

        override fun onStop() {
            runOnUiThread {
                if (closed) return@runOnUiThread

                speechRequested = false
                status = "TTS 재생 중단"

                record("TTS_PLAYBACK_STOP phase=$phase")

                if (running && phase == "B_WAIT_STOP_CALLBACK") {
                    passInterrupt = true
                    record("TEST_B_INTERRUPT_CONFIRMED")
                }
            }
        }

        override fun onError(errorType: ErrorType) {
            runOnUiThread {
                if (closed) return@runOnUiThread

                speechRequested = false
                status = "TTS ERROR: $errorType"
                record("TTS_ERROR $errorType")

                if (running) {
                    finishTest()
                }
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

            instance.initialize(object : OnInitializeListener {

                override fun onSuccess() {
                    if (closed) return

                    try {
                        instance.addEventListener(ttsListener)
                        listenerAdded = true
                        instance.setLocale(Locale.US)

                        runOnUiThread {
                            if (!closed) {
                                ready = true
                                status = "SDK READY"
                                record("SDK_INIT_SUCCESS")
                            }
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            status = "SDK 설정 실패"
                            record("SDK_SETUP_ERROR ${e.message}")
                        }
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    runOnUiThread {
                        status = "SDK 초기화 실패: $errorType"
                        record("SDK_INIT_FAILED $errorType")
                    }
                }
            })
        } catch (e: Exception) {
            status = "SDK 초기화 예외"
            record("SDK_EXCEPTION ${e.message}")
        }
    }

    private fun requestGuide(
        message: String,
        label: String
    ): Boolean {

        if (!ready || closed) {
            record("${label}_NOT_READY")
            return false
        }

        if (simulatedNav) {
            record("${label}_BLOCKED_NAV_ACTIVE")
            return false
        }

        if (speechRequested) {
            record("${label}_BLOCKED_TTS_BUSY")
            return false
        }

        return try {
            val tts = speech ?: return false

            speechRequested = true
            record("${label}_ACCEPTED")
            tts.speak(message)
            true
        } catch (e: Exception) {
            speechRequested = false
            record("${label}_ERROR ${e.message}")
            false
        }
    }

    private fun startTestB() {
        if (!running || closed) return

        phase = "B_TTS_FIRST"
        interruptScheduled = false

        record("TEST_B_START")

        val longMessage = (
            "This is a test announcement from K Drive Guide. " +
            "We are verifying that the announcement stops " +
            "when navigation guidance becomes active. "
        ).repeat(6)

        if (!requestGuide(longMessage, "TEST_B")) {
            finishTest()
            return
        }

        later(10000) {
            if (phase == "B_TTS_FIRST") {
                record("TEST_B_TIMEOUT")
                finishTest()
            }
        }
    }

    private fun startAutomaticTest() {
        if (!ready || running || closed) return

        session++
        handler.removeCallbacksAndMessages(null)

        history = emptyList()
        running = true
        simulatedNav = true
        speechRequested = false

        passBlock = false
        passAfterNav = false
        passInterrupt = false
        interruptScheduled = false

        phase = "A_NAV_ACTIVE"
        status = "모의 이벤트 테스트 진행 중"

        record("TEST_START")
        record("TEST_A_NAV_EVENT_START")

        // TEST A: 모의 내비 진행 중 TTS 차단
        later(600) {
            val accepted = requestGuide(
                "This sentence should not be spoken.",
                "TEST_A"
            )

            passBlock = !accepted
            record("TEST_A_BLOCK_PASS=$passBlock")
        }

        // TEST A: 모의 내비 종료 후 실제 TTS 재생
        later(2000) {
            simulatedNav = false
            record("TEST_A_NAV_EVENT_END")

            phase = "A_TTS_ALLOWED"

            val accepted = requestGuide(
                "The navigation event has ended. " +
                "K Drive Guide can now provide its announcement.",
                "TEST_A_AFTER_NAV"
            )

            if (!accepted) {
                finishTest()
            }
        }

        later(15000) {
            if (phase == "A_TTS_ALLOWED") {
                record("TEST_A_TIMEOUT")
                finishTest()
            }
        }
    }

    private fun finishTest() {
        if (!running) return

        running = false
        simulatedNav = false
        phase = "FINISHED"

        record(
            "TEST_RESULT A_BLOCK=$passBlock " +
            "A_PLAYBACK=$passAfterNav " +
            "B_INTERRUPT=$passInterrupt"
        )

        status = if (
            passBlock && passAfterNav && passInterrupt
        ) {
            "모의 제어 시험 통과"
        } else {
            "모의 제어 시험 미통과"
        }

        record("TEST_FINISHED")
    }

    private fun stopTest() {
        session++
        handler.removeCallbacksAndMessages(null)

        running = false
        simulatedNav = false
        speechRequested = false
        phase = "STOPPED"

        try {
            speech?.stop()
        } catch (e: Exception) {
            record("STOP_ERROR ${e.message}")
        }

        record("TEST_STOPPED_BY_USER")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        audioManager =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager

        audioManager.registerAudioPlaybackCallback(
            playbackCallback,
            handler
        )

        initializeTts()

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        "K-DRIVE AUDIO AUTO TEST",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text("모의 내비 이벤트 기반 음성 제어 시험")
                    Text("SDK: $status")
                    Text("진행 단계: $phase")
                    Text("모의 내비 이벤트: $simulatedNav")
                    Text("TTS 요청 중: $speechRequested")

                    Button(
                        onClick = { startAutomaticTest() },
                        enabled = ready && !running,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("자동 음성 제어 테스트 시작")
                    }

                    Button(
                        onClick = { stopTest() },
                        enabled = running,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("테스트 중지")
                    }

                    HorizontalDivider()

                    Text(
                        "시험 결과",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("A. 내비 이벤트 중 차단: $passBlock")
                    Text("B. 내비 이벤트 종료 후 재생: $passAfterNav")
                    Text("C. 새 내비 이벤트에 TTS 중단: $passInterrupt")

                    HorizontalDivider()

                    Text(
                        "실제 Android 오디오 관측",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(observedAudio)

                    HorizontalDivider()

                    Text(
                        "실행 기록",
                        style = MaterialTheme.typography.titleMedium
                    )

                    history.forEach { item ->
                        Text(
                            item,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    HorizontalDivider()

                    Text(
                        "모의 내비 이벤트는 소리를 발생시키지 않습니다."
                    )
                    Text(
                        "이 테스트는 내부 TTS 제어 규칙만 검증합니다."
                    )
                    Text(
                        "실제 기본 내비 음성 감지는 별도 검증 대상입니다."
                    )

                    Text("Logcat: KDRIVE_AUDIO_AUTO")
                }
            }
        }
    }

    override fun onDestroy() {
        closed = true
        session++
        handler.removeCallbacksAndMessages(null)

        try {
            audioManager.unregisterAudioPlaybackCallback(
                playbackCallback
            )
        } catch (_: Exception) {}

        try {
            speech?.let {
                it.stop()

                if (listenerAdded) {
                    it.removeEventListener(ttsListener)
                }

                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "RELEASE_ERROR", e)
        }

        super.onDestroy()
    }
}