
package com.example.pleosteamapp

import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class TtsTestActivity : ComponentActivity() {

    private var ttsProbe: PleosTtsProbe? = null

    private var statusText by mutableStateOf(
        "TTS 초기화 준비 중"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 테스트 화면 구성
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(16.dp)
                    ) {

                        Text(
                            text = "Pleos 영어 TTS 테스트",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Text(
                            text = "현재 상태: $statusText"
                        )

                        Button(
                            onClick = {
                                ttsProbe?.speakEnglish()
                            }
                        ) {
                            Text("영어 음성 재생")
                        }

                        Button(
                            onClick = {
                                ttsProbe?.stopSpeech()
                            }
                        ) {
                            Text("음성 중지")
                        }

                        Text(
                            text = "Logcat: PLEOS_TTS_PC1"
                        )
                    }
                }
            }
        }

        // TTS SDK 초기화
        try {
            ttsProbe = PleosTtsProbe(
                applicationContext
            ) { message ->

                runOnUiThread {
                    statusText = message
                }
            }

            ttsProbe?.start()

        } catch (error: Exception) {
            Log.e(
                "PLEOS_TTS_PC1",
                "TTS 시작 실패",
                error
            )
            statusText = "TTS 초기화 실패"
        }
    }

    override fun onDestroy() {
        try {
            ttsProbe?.release()
        } catch (error: Exception) {
            Log.e(
                "PLEOS_TTS_PC1",
                "TTS 종료 실패",
                error
            )
        } finally {
            ttsProbe = null
            super.onDestroy()
        }
    }
}
