package com.example.pleosteamapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/*
 * K-DRIVE GUIDE 전체를 하나로 묶은 화면
 * MainActivity의 setContent 안에서 KDriveGuideApp() 한 줄만 호출하면 됩니다.
 *
 * 지금 단계: LogSpeaker(소리 대신 Logcat) + Demo 버튼 확인용
 * 다음 단계: speaker를 PleosSpeaker로 바꾸고, AUTO 스위치에 NaviHelper TBT 연결
 */
@Composable
fun KDriveGuideApp(
    speaker: Speaker = remember { LogSpeaker() }
) {
    var uiState by remember { mutableStateOf(GuideUiState()) }

    val engine = remember(speaker) {
        GuideEngine(speaker, object : GuideEngine.Listener {
            override fun onStatus(signal: GuideSignal?) {
                uiState = uiState.copy(status = signal)
            }

            override fun onAnnouncement(announcement: Announcement) {
                uiState = uiState.copy(current = announcement)
            }
        })
    }

    val demo = remember(engine) { DemoFeed(engine) }

    DisposableEffect(Unit) {
        onDispose {
            demo.stop()
            engine.stop()
        }
    }

    KDriveGuideScreen(
        state = uiState,
        onAutoModeChange = { on ->
            uiState = uiState.copy(autoMode = on)
            // TODO 다음 단계: on이면 NaviHelper TBT 감시 시작, off면 중지
        },
        onDemoToll = {
            uiState = uiState.copy(autoMode = false)
            demo.start()
        },
        onReplay = { engine.replay() },
        onStop = {
            engine.stop()
            demo.stop()
        }
    )
}
