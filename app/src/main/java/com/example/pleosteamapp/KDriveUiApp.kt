
package com.example.pleosteamapp

import androidx.activity.compose.BackHandler
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview

/**
 * K-DRIVE GUIDE
 *
 * PC2 UI 통합 담당
 *
 * 연결 화면:
 * 1. KDriveHomeScreen
 * 2. TrafficRulesScreen
 * 3. DrivingStatusScreen
 *
 * PC1의 NaviHelper, GPS, TTS, 안내 엔진은
 * 이 파일에서 직접 생성하지 않는다.
 *
 * 실제 데이터와 동작은 외부 콜백으로 연결한다.
 */

private enum class KDrivePage {
    HOME,
    TRAFFIC_RULES,
    DRIVING_STATUS
}

/**
 * PC1이 실제 데이터 및 이벤트를 연결할 수 있는
 * K-DRIVE UI 진입점.
 */
@Composable
fun KDriveUiApp(
    state: KDriveDrivingStatusUiState,
    onAutoGuideChange: (Boolean) -> Unit,
    onDemoStartRequest: () -> Unit,
    onDemoStopRequest: () -> Unit,
    onSpeakRequest: (String) -> Unit,
    onStopVoiceRequest: () -> Unit,
    onReplayRequest: () -> Unit
) {

    // 현재 화면
    var currentPage by rememberSaveable {
        mutableStateOf(KDrivePage.HOME.name)
    }

    // Android 뒤로 가기
    BackHandler(
        enabled = currentPage != KDrivePage.HOME.name
    ) {
        if (currentPage == KDrivePage.TRAFFIC_RULES.name) {
            onStopVoiceRequest()
        }

        currentPage = KDrivePage.HOME.name
    }

    when (currentPage) {

        // ----------------------------------
        // 1. 교통규칙 안내 화면
        // ----------------------------------
        KDrivePage.TRAFFIC_RULES.name -> {

            TrafficRulesScreen(
                onBack = {
                    currentPage = KDrivePage.HOME.name
                },

                onSpeakRequest = onSpeakRequest,

                onStopRequest = onStopVoiceRequest
            )
        }

        // ----------------------------------
        // 2. 주행 상태 및 Demo 화면
        // ----------------------------------
        KDrivePage.DRIVING_STATUS.name -> {

            DrivingStatusScreen(
                state = state,

                onBack = {
                    currentPage = KDrivePage.HOME.name
                },

                onAutoGuideChange =
                    onAutoGuideChange,

                onDemoStartRequest =
                    onDemoStartRequest,

                onDemoStopRequest =
                    onDemoStopRequest,

                onReplayRequest =
                    onReplayRequest,

                onStopVoiceRequest =
                    onStopVoiceRequest
            )
        }

        // ----------------------------------
        // 3. 기본 홈 화면
        // ----------------------------------
        else -> {

            KDriveHomeScreen(
                navigationStatus =
                    state.navigationStatus,

                autoGuideEnabled =
                    state.autoGuideEnabled,

                demoMode =
                    state.source == KDriveUiDataSource.DEMO,

                onAutoGuideChange =
                    onAutoGuideChange,

                onTrafficRulesClick = {
                    currentPage =
                        KDrivePage.TRAFFIC_RULES.name
                },

                onDrivingStatusClick = {
                    currentPage =
                        KDrivePage.DRIVING_STATUS.name
                },

                onDemoClick = {
                    // Demo 시작은 PC1 엔진에 요청
                    onDemoStartRequest()

                    // 주행 상태 화면으로 이동
                    currentPage =
                        KDrivePage.DRIVING_STATUS.name
                }
            )
        }
    }
}

// -------------------------------------------
// 4. Preview 테스트용 가상 데이터
// -------------------------------------------

private const val PREVIEW_TOLL_MESSAGE =
    "A tollgate is ahead. " +
            "Check the payment lane signs " +
            "and choose the correct lane."

/**
 * 실제 앱 기능이 아니라
 * PC2의 UI를 독립적으로 시험하는 용도.
 *
 * 버튼 클릭 시 테스트 상태만 변경한다.
 * 실제 영어 음성을 재생하지 않는다.
 */
@Composable
private fun KDriveUiTestContent() {

    var uiState by remember {
        mutableStateOf(
            KDriveDrivingStatusUiState(
                navigationStatus =
                    "Waiting for navigation"
            )
        )
    }

    var lastSpeech by remember {
        mutableStateOf("")
    }

    MaterialTheme {

        KDriveUiApp(
            state = uiState,

            // 자동 안내 ON/OFF UI 테스트
            onAutoGuideChange = { enabled ->

                uiState = uiState.copy(
                    autoGuideEnabled = enabled
                )
            },

            // 가상 Demo 시작
            onDemoStartRequest = {

                lastSpeech = PREVIEW_TOLL_MESSAGE

                uiState = uiState.copy(
                    source =
                        KDriveUiDataSource.DEMO,

                    navigationStatus =
                        "Simulation active",

                    demoRunning = true,

                    placeName = "Sample Tollgate",

                    distanceMeters = 500.0,

                    guidanceTitle =
                        "Tollgate Ahead",

                    guidanceText =
                        PREVIEW_TOLL_MESSAGE,

                    voiceStatus =
                        "Ready - Preview only"
                )
            },

            // 가상 Demo 중지
            onDemoStopRequest = {

                uiState = KDriveDrivingStatusUiState(
                    navigationStatus =
                        "Simulation stopped",

                    autoGuideEnabled =
                        uiState.autoGuideEnabled,

                    voiceStatus =
                        "Stopped - Preview only"
                )
            },

            // 수동 영어 안내 요청 테스트
            onSpeakRequest = { englishText ->

                lastSpeech = englishText

                uiState = uiState.copy(
                    guidanceTitle =
                        "Manual Traffic Rule",

                    guidanceText = englishText,

                    voiceStatus =
                        "Voice requested - Preview only"
                )
            },

            // 음성 중지 요청 테스트
            onStopVoiceRequest = {

                uiState = uiState.copy(
                    voiceStatus =
                        "Stop requested - Preview only"
                )
            },

            // 마지막 안내 Replay 요청 테스트
            onReplayRequest = {

                uiState = uiState.copy(
                    voiceStatus =
                        if (lastSpeech.isNotBlank()) {
                            "Replay requested - Preview only"
                        } else {
                            "No previous message - Preview"
                        }
                )
            }
        )
    }
}

// -------------------------------------------
// 5. 차량 가로 화면 통합 Preview
// -------------------------------------------

@Preview(
    name = "K-DRIVE Full UI Landscape",
    showBackground = true,
    widthDp = 1000,
    heightDp = 600
)
@Composable
private fun KDriveFullLandscapePreview() {

    KDriveUiTestContent()
}

// -------------------------------------------
// 6. 세로 화면 통합 Preview
// -------------------------------------------

@Preview(
    name = "K-DRIVE Full UI Portrait",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun KDriveFullPortraitPreview() {

    KDriveUiTestContent()
}
