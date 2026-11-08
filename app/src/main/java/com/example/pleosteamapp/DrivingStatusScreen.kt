
package com.example.pleosteamapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

/**
 * K-DRIVE GUIDE - PC2
 *
 * 주행 안내 상태 화면
 *
 * 실제 NaviHelper, GPS, TTS 및 안내 엔진을
 * 직접 생성하거나 호출하지 않는다.
 *
 * 모든 데이터는 state로 전달받는다.
 * 모든 버튼 요청은 callback으로 전달한다.
 */

// 데이터 출처
enum class KDriveUiDataSource {
    NO_DATA,
    REAL,
    DEMO
}

// 화면 표시용 데이터
data class KDriveDrivingStatusUiState(
    val source: KDriveUiDataSource =
        KDriveUiDataSource.NO_DATA,

    val navigationStatus: String =
        "Waiting for navigation",

    val autoGuideEnabled: Boolean = false,

    val demoRunning: Boolean = false,

    val placeName: String? = null,

    val distanceMeters: Double? = null,

    val guidanceTitle: String? = null,

    val guidanceText: String? = null,

    val voiceStatus: String = "Idle"
)

/**
 * PC1 연결용 콜백
 *
 * onAutoGuideChange : 자동 안내 설정 요청
 * onDemoStartRequest : Demo 시작 요청
 * onDemoStopRequest : Demo 중지 요청
 * onReplayRequest : 마지막 음성 재생 요청
 * onStopVoiceRequest : 음성 중지 요청
 */
@Composable
fun DrivingStatusScreen(
    state: KDriveDrivingStatusUiState,
    onBack: () -> Unit,
    onAutoGuideChange: (Boolean) -> Unit,
    onDemoStartRequest: () -> Unit,
    onDemoStopRequest: () -> Unit,
    onReplayRequest: () -> Unit,
    onStopVoiceRequest: () -> Unit
) {

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {

        val wideScreen = maxWidth >= 700.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // 화면 제목
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                TextButton(
                    onClick = onBack
                ) {
                    Text("Back")
                }

                Column {
                    Text(
                        text = "Driving Status",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "K-DRIVE GUIDE",
                        style =
                            MaterialTheme.typography.labelMedium
                    )
                }
            }

            HorizontalDivider()

            if (wideScreen) {

                // 가로 화면
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    horizontalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {

                    KDriveStatusOverview(
                        state = state,
                        onAutoGuideChange =
                            onAutoGuideChange,

                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    )

                    KDriveStatusGuidance(
                        state = state,
                        onDemoStartRequest =
                            onDemoStartRequest,
                        onDemoStopRequest =
                            onDemoStopRequest,
                        onReplayRequest =
                            onReplayRequest,
                        onStopVoiceRequest =
                            onStopVoiceRequest,

                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    )
                }

            } else {

                // 세로 화면
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(16.dp)
                ) {

                    KDriveStatusOverview(
                        state = state,
                        onAutoGuideChange =
                            onAutoGuideChange
                    )

                    KDriveStatusGuidance(
                        state = state,
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
            }
        }
    }
}

// -------------------------------------------
// 왼쪽: 현재 주행 상태
// -------------------------------------------

@Composable
private fun KDriveStatusOverview(
    state: KDriveDrivingStatusUiState,
    onAutoGuideChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Navigation Information",
            style = MaterialTheme.typography.titleLarge
        )

        // 데이터 출처
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer
            )
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "DATA SOURCE",
                    style =
                        MaterialTheme.typography.labelMedium
                )

                val sourceLabel = when (state.source) {
                    KDriveUiDataSource.REAL ->
                        "REAL - Navigation Data"

                    KDriveUiDataSource.DEMO ->
                        "DEMO - Simulated Data"

                    KDriveUiDataSource.NO_DATA ->
                        "NO DATA - Waiting"
                }

                Text(
                    text = sourceLabel,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = state.navigationStatus,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // 거리 정보
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "NEXT GUIDANCE POINT",
                    style =
                        MaterialTheme.typography.labelMedium
                )

                Text(
                    text = state.placeName
                        ?.takeIf { it.isNotBlank() }
                        ?: "Not available",

                    style =
                        MaterialTheme.typography.titleMedium
                )

                val distance = state.distanceMeters

                val distanceText =
                    if (
                        distance != null &&
                        !distance.isNaN() &&
                        !distance.isInfinite() &&
                        distance >= 0.0
                    ) {
                        String.format(
                            Locale.US,
                            "%,.0f m",
                            distance
                        )
                    } else {
                        "—"
                    }

                Text(
                    text = distanceText,
                    style =
                        MaterialTheme.typography.headlineMedium
                )

                Text(
                    text = if (
                        state.source ==
                        KDriveUiDataSource.DEMO
                    ) {
                        "Simulated distance"
                    } else {
                        "Reported navigation distance"
                    },

                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // 자동 안내 ON/OFF
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = "Automatic Voice Guide",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text =
                            if (state.autoGuideEnabled) {
                                "ON"
                            } else {
                                "OFF"
                            },

                        style =
                            MaterialTheme.typography.bodySmall
                    )
                }

                Switch(
                    checked = state.autoGuideEnabled,
                    onCheckedChange = onAutoGuideChange
                )
            }
        }

        Text(
            text = "Automatic guidance requires " +
                    "a valid navigation connection " +
                    "and integration with the core engine.",
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------
// 오른쪽: 안내 내용 및 제어 버튼
// -------------------------------------------

@Composable
private fun KDriveStatusGuidance(
    state: KDriveDrivingStatusUiState,
    onDemoStartRequest: () -> Unit,
    onDemoStopRequest: () -> Unit,
    onReplayRequest: () -> Unit,
    onStopVoiceRequest: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Current Guidance",
            style = MaterialTheme.typography.titleLarge
        )

        // 안내 문구
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = state.guidanceTitle
                        ?.takeIf { it.isNotBlank() }
                        ?: "No active guidance",

                    style =
                        MaterialTheme.typography.titleLarge
                )

                HorizontalDivider()

                Text(
                    text = state.guidanceText
                        ?.takeIf { it.isNotBlank() }
                        ?: "Guidance will appear here " +
                        "when data is available.",

                    style =
                        MaterialTheme.typography.bodyLarge
                )
            }
        }

        // 음성 재생 상태
        OutlinedCard(
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = "VOICE STATUS",
                    style =
                        MaterialTheme.typography.labelMedium
                )

                Text(
                    text = state.voiceStatus,
                    style =
                        MaterialTheme.typography.titleMedium
                )
            }
        }

        // 음성 Replay 및 Stop
        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = onReplayRequest,
                enabled =
                    !state.guidanceText.isNullOrBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Replay Voice")
            }

            OutlinedButton(
                onClick = onStopVoiceRequest,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop Voice")
            }
        }

        HorizontalDivider()

        Text(
            text = "Demonstration",
            style = MaterialTheme.typography.titleMedium
        )

        Text(
            text =
                if (state.demoRunning) {
                    "Demo is running."
                } else {
                    "Demo is not running."
                },

            style = MaterialTheme.typography.bodyMedium
        )

        // Demo 시작 또는 중지 요청
        if (state.demoRunning) {

            OutlinedButton(
                onClick = onDemoStopRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop Demo")
            }

        } else {

            Button(
                onClick = onDemoStartRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Demo")
            }
        }

        Text(
            text = "Demo mode uses simulated data. " +
                    "It must not be mistaken for " +
                    "real driving guidance.",
            style = MaterialTheme.typography.bodySmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------
// Preview 공통 테스트
// -------------------------------------------

@Composable
private fun KDriveStatusPreviewContent(
    startWithDemo: Boolean
) {

    var previewState by remember {
        mutableStateOf(
            if (startWithDemo) {
                KDriveDrivingStatusUiState(
                    source = KDriveUiDataSource.DEMO,
                    navigationStatus =
                        "Simulation active",
                    autoGuideEnabled = false,
                    demoRunning = true,
                    placeName = "Sample Tollgate",
                    distanceMeters = 500.0,
                    guidanceTitle = "Tollgate Ahead",
                    guidanceText =
                        "A tollgate is ahead. " +
                                "Check the payment lane signs " +
                                "and choose the correct lane.",
                    voiceStatus = "Ready (Preview)"
                )
            } else {
                KDriveDrivingStatusUiState()
            }
        )
    }

    MaterialTheme {

        DrivingStatusScreen(
            state = previewState,

            onBack = {},

            onAutoGuideChange = { enabled ->
                previewState = previewState.copy(
                    autoGuideEnabled = enabled
                )
            },

            onDemoStartRequest = {
                previewState = previewState.copy(
                    source = KDriveUiDataSource.DEMO,
                    navigationStatus = "Simulation active",
                    demoRunning = true,
                    placeName = "Sample Tollgate",
                    distanceMeters = 500.0,
                    guidanceTitle = "Tollgate Ahead",
                    guidanceText =
                        "A tollgate is ahead. " +
                                "Check the payment lane signs " +
                                "and choose the correct lane.",
                    voiceStatus = "Ready (Preview)"
                )
            },

            onDemoStopRequest = {
                previewState = previewState.copy(
                    demoRunning = false,
                    navigationStatus = "Simulation stopped",
                    voiceStatus = "Stopped (Preview)"
                )
            },

            onReplayRequest = {
                previewState = previewState.copy(
                    voiceStatus =
                        "Replay requested (Preview only)"
                )
            },

            onStopVoiceRequest = {
                previewState = previewState.copy(
                    voiceStatus =
                        "Stop requested (Preview only)"
                )
            }
        )
    }
}

// -------------------------------------------
// Landscape Preview
// -------------------------------------------

@Preview(
    name = "K-DRIVE Driving Status Landscape",
    showBackground = true,
    widthDp = 1000,
    heightDp = 600
)
@Composable
private fun DrivingStatusLandscapePreview() {

    KDriveStatusPreviewContent(
        startWithDemo = true
    )
}

// -------------------------------------------
// Portrait Preview
// -------------------------------------------

@Preview(
    name = "K-DRIVE Driving Status Portrait",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun DrivingStatusPortraitPreview() {

    KDriveStatusPreviewContent(
        startWithDemo = false
    )
}
