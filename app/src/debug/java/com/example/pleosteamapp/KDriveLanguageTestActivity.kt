package com.example.pleosteamapp

import android.os.Bundle
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

private data class KDriveLanguageCase(
    val label: String,
    val localeTag: String,
    val message: String
)

class KDriveLanguageTestActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KDRIVE_LANG_TEST"
    }

    private val cases = listOf(
        KDriveLanguageCase(
            "English",
            "en-US",
            "You are approaching a roundabout. Slow down and yield to vehicles already circulating."
        ),
        KDriveLanguageCase(
            "한국어",
            "ko-KR",
            "회전교차로에 접근하고 있습니다. 속도를 줄이고 이미 진입한 차량에 양보하세요."
        ),
        KDriveLanguageCase(
            "Español - US",
            "es-US",
            "Se acerca a una rotonda. Reduzca la velocidad y ceda el paso a los vehículos que ya circulan."
        ),
        KDriveLanguageCase(
            "Español - MX",
            "es-MX",
            "Está por llegar a una glorieta. Reduzca la velocidad y ceda el paso a los vehículos que ya están dentro."
        ),
        KDriveLanguageCase(
            "Français - CA",
            "fr-CA",
            "Vous approchez d'un carrefour giratoire. Ralentissez et cédez le passage aux véhicules déjà engagés."
        )
    )

    private var tts: TextToSpeech? = null
    private var listenerAdded = false
    private var closed = false

    private var ready by mutableStateOf(false)
    private var playing by mutableStateOf(false)
    private var status by mutableStateOf("SDK 초기화 중")
    private var currentLocale by mutableStateOf("-")
    private var currentMessage by mutableStateOf("-")

    private val listener = object : EventListener {
        override fun onReady() {
            report("TTS READY")
        }

        override fun onStart() {
            report("PLAYBACK START", true)
        }

        override fun onUpdatedRms(rms: Double) {}

        override fun onDone() {
            report("PLAYBACK DONE", false)
        }

        override fun onStop() {
            report("PLAYBACK STOP", false)
        }

        override fun onError(errorType: ErrorType) {
            report("ERROR: $errorType", false)
        }
    }

    private fun report(event: String, isPlaying: Boolean? = null) {
        Log.d(TAG, "locale=$currentLocale event=$event")

        runOnUiThread {
            if (closed) return@runOnUiThread

            status = event

            if (isPlaying != null) {
                playing = isPlaying
            }
        }
    }

    private fun initializeTts() {
        try {
            val speech = TextToSpeech(
                applicationContext,
                Mode.ON_DEVICE
            )

            tts = speech

            speech.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    if (closed) return

                    try {
                        speech.addEventListener(listener)
                        listenerAdded = true

                        runOnUiThread {
                            if (!closed) {
                                ready = true
                                status = "SDK READY"
                                Log.d(TAG, "SDK_INIT_SUCCESS")
                            }
                        }
                    } catch (e: Exception) {
                        report("LISTENER ERROR: ${e.message}", false)
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    report("SDK INIT ERROR: $errorType", false)
                }
            })
        } catch (e: Exception) {
            report("SDK INIT EXCEPTION: ${e.message}", false)
        }
    }

    private fun playLanguage(test: KDriveLanguageCase) {
        if (!ready || playing || closed) return

        val speech = tts ?: return

        try {
            val locale = Locale.forLanguageTag(test.localeTag)

            currentLocale = test.localeTag
            currentMessage = test.message
            status = "REQUEST SENT: ${test.localeTag}"
            playing = true

            speech.setLocale(locale)

            Log.d(
                TAG,
                "LANG_REQUEST locale=${test.localeTag} text=${test.message}"
            )

            speech.speak(test.message)
        } catch (e: Exception) {
            playing = false
            status = "SPEAK ERROR: ${e.message}"
            Log.e(TAG, "SPEAK_FAILED", e)
        }
    }

    private fun stopSpeech() {
        try {
            tts?.stop()
            Log.d(TAG, "STOP_REQUEST locale=$currentLocale")
            playing = false
            status = "STOP REQUESTED"
        } catch (e: Exception) {
            report("STOP ERROR: ${e.message}", false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        "K-DRIVE GUIDE",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        "Pleos 다국어 TTS 테스트",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("Mode: ON_DEVICE")
                    Text("SDK 준비: $ready")
                    Text("현재 언어: $currentLocale")
                    Text("상태: $status")

                    HorizontalDivider()

                    Text(
                        "언어별 음성 재생",
                        style = MaterialTheme.typography.titleLarge
                    )

                    cases.forEach { test ->
                        Button(
                            onClick = { playLanguage(test) },
                            enabled = ready && !playing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${test.label} (${test.localeTag})")
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "마지막 재생 문장",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(currentMessage)

                    Button(
                        onClick = { stopSpeech() },
                        enabled = ready,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("음성 중지")
                    }

                    Text("Logcat TAG: KDRIVE_LANG_TEST")
                    Text("SDK 완료 로그와 실제 발음을 모두 확인하세요.")
                }
            }
        }

        initializeTts()
    }

    override fun onDestroy() {
        closed = true

        try {
            tts?.let { speech ->
                if (listenerAdded) {
                    speech.removeEventListener(listener)
                }
                speech.stop()
                speech.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "RELEASE_FAILED", e)
        }

        super.onDestroy()
    }
}