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

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KDriveRealAudioProbeActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KDRIVE_REAL_AUDIO"
    }

    private lateinit var controller: KDriveSpeechController

    private val preferences by lazy {
        getSharedPreferences("kdrive_real_audio_probe", MODE_PRIVATE)
    }

    private var history by mutableStateOf("")
    private var navUsage by mutableIntStateOf(0)
    private var summary by mutableStateOf("아직 관측 없음")
    private var count by mutableIntStateOf(0)

    private fun record(message: String) {
        val time = SimpleDateFormat(
            "HH:mm:ss.SSS",
            Locale.US
        ).format(Date())

        val line = "$time $message"

        Log.d(TAG, line)

        val updated = (
            history.lines().filter { it.isNotBlank() } + line
        ).takeLast(100).joinToString("\n")

        history = updated

        preferences.edit()
            .putString("history", updated)
            .apply()
    }

    private fun updateSnapshot() {
        navUsage = controller.navigationUsageCount
        summary = controller.currentSummary
        count = controller.observationCount
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        history = preferences.getString("history", "") ?: ""

        controller = KDriveSpeechController(this) { event ->
            runOnUiThread {
                record(event)
                updateSnapshot()
            }
        }

        controller.startObserving()
        updateSnapshot()

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
                        "K-DRIVE REAL AUDIO MONITOR",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text("기본 내비 음성 관찰 실험")

                    HorizontalDivider()

                    Text("NAVIGATION usage 관측 수: $navUsage")
                    Text("오디오 관측 횟수: $count")
                    Text("현재 상태: $summary")

                    Button(
                        onClick = {
                            controller.refresh()
                            updateSnapshot()
                            record("USER_REFRESH")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("현재 오디오 상태 조회")
                    }

                    Button(
                        onClick = {
                            history = ""
                            preferences.edit()
                                .remove("history")
                                .apply()

                            record("LOG_CLEARED_MONITOR_RUNNING")
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("기록 초기화")
                    }

                    HorizontalDivider()

                    Text(
                        "실행 방법",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("1. 관찰 화면을 연 상태에서 하단 내비 아이콘으로 이동")
                    Text("2. 기본 내비에서 목적지를 설정하고 안내 시작")
                    Text("3. 음성이 나오면 버튼을 누르지 않아도 됨")
                    Text("4. 이 화면으로 돌아와 관찰 기록 확인")

                    HorizontalDivider()

                    Text(
                        "최근 기록",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        history.ifBlank { "아직 기록이 없습니다." },
                        style = MaterialTheme.typography.bodySmall
                    )

                    HorizontalDivider()

                    Text(
                        "주의: NAVIGATION usage는 오디오 유형이며 " +
                            "기본 내비 앱의 고유 신원이 아닙니다."
                    )

                    Text(
                        "앱 프로세스가 종료되면 관찰도 중단됩니다."
                    )

                    Text("Logcat: KDRIVE_REAL_AUDIO")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (::controller.isInitialized) {
            controller.refresh()
            updateSnapshot()
        }
    }

    override fun onDestroy() {
        if (::controller.isInitialized) {
            controller.close()
        }

        super.onDestroy()
    }
}