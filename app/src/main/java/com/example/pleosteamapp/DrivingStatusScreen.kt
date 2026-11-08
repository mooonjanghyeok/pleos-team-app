
package com.example.pleosteamapp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import java.util.Locale

/**
 * K-DRIVE GUIDE - PC2
 * Driving Status UI
 *
 * NO_DATA 상태에서는 이전 수동 안내를
 * 현재 내비게이션 안내로 표시하지 않는다.
 *
 * PC1 SDK 및 TTS는 수정하지 않는다.
 */

enum class KDriveUiDataSource {
    NO_DATA,
    REAL,
    DEMO
}

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

private fun kdriveShortMessage(
    source: String?,
    maxChars: Int = 120
): String {

    if (source.isNullOrBlank()) {
        return "No current guidance"
    }

    val words = source.trim()
        .split(Regex("\\s+"))
        .take(24)
        .joinToString(" ")

    return if (words.length <= maxChars) {
        words
    } else {
        words.take(maxChars - 1).trimEnd() + "…"
    }
}

private fun kdriveDistanceText(
    meters: Double?
): String {

    if (
        meters == null ||
        !meters.isFinite() ||
        meters < 0.0
    ) {
        return "—"
    }

    return String.format(
        Locale.US,
        "%,.0f m",
        meters
    )
}

// -------------------------------------------
// Main Screen
// -------------------------------------------

@Composable
fun DrivingStatusScreen(
    state: KDriveDrivingStatusUiState,
    onBack: () -> Unit,
    onAutoGuideChange: (Boolean) -> Unit,
    onDemoStartRequest: () -> Unit,
    onDemoStopRequest: () -> Unit,
    onReplayRequest: () -> Unit,
    onStopVoiceRequest: () -> Unit,
    interactionRestricted: Boolean = true
) {

    val colors = MaterialTheme.colorScheme

    val hasNavigationData =
        state.source != KDriveUiDataSource.NO_DATA

    // 주행 제한 상태
    if (interactionRestricted) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),

            verticalArrangement =
                Arrangement.spacedBy(18.dp)
        ) {

            Text(
                text = "K-DRIVE",
                style =
                    MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "LIVE GUIDANCE",
                style =
                    MaterialTheme.typography.titleLarge,
                color = colors.primary
            )

            if (state.source == KDriveUiDataSource.REAL) {

                Text(
                    text = kdriveShortMessage(
                        state.guidanceTitle
                    ),
                    style =
                        MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = kdriveShortMessage(
                        state.guidanceText,
                        80
                    ),
                    style =
                        MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

            } else {

                Text(
                    text = "Navigation guidance unavailable",
                    style =
                        MaterialTheme.typography.headlineMedium,
                    maxLines = 2
                )
            }
        }

        return
    }

    // 정차 모드
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {

        val wide = maxWidth >= 670.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),

            verticalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                KDriveActionButton(
                    label = "Back",
                    onClick = onBack,
                    filled = false
                )

                Text(
                    text = "Driving Status",
                    style =
                        MaterialTheme.typography.headlineMedium
                )
            }

            if (wide) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    horizontalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {

                        KDriveStatusOverview(
                            state = state,
                            hasNavigationData =
                                hasNavigationData,
                            onAutoGuideChange =
                                onAutoGuideChange
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    ) {

                        KDriveStatusControls(
                            state = state,
                            hasNavigationData =
                                hasNavigationData,
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

            } else {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    KDriveStatusOverview(
                        state = state,
                        hasNavigationData =
                            hasNavigationData,
                        onAutoGuideChange =
                            onAutoGuideChange
                    )

                    KDriveStatusControls(
                        state = state,
                        hasNavigationData =
                            hasNavigationData,
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
// Navigation Overview
// -------------------------------------------

@Composable
private fun KDriveStatusOverview(
    state: KDriveDrivingStatusUiState,
    hasNavigationData: Boolean,
    onAutoGuideChange: (Boolean) -> Unit
) {

    val colors = MaterialTheme.colorScheme

    Column(
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Card(
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "DATA SOURCE",
                    style =
                        MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )

                Text(
                    text = when (state.source) {

                        KDriveUiDataSource.NO_DATA ->
                            "NO DATA"

                        KDriveUiDataSource.REAL ->
                            "REAL NAVIGATION"

                        KDriveUiDataSource.DEMO ->
                            "DEMO · SIMULATION"
                    },
                    style =
                        MaterialTheme.typography.titleLarge
                )

                Text(
                    text = kdriveShortMessage(
                        state.navigationStatus,
                        80
                    ),
                    style =
                        MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "NEXT GUIDANCE POINT",
                    style =
                        MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )

                Text(
                    text = if (hasNavigationData) {
                        kdriveShortMessage(state.placeName)
                    } else {
                        "No active navigation"
                    },
                    style =
                        MaterialTheme.typography.titleLarge,
                    maxLines = 2
                )

                Text(
                    text = if (hasNavigationData) {
                        kdriveDistanceText(
                            state.distanceMeters
                        )
                    } else {
                        "—"
                    },
                    style =
                        MaterialTheme.typography.headlineLarge
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    text = "Automatic guide",
                    style =
                        MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = state.autoGuideEnabled,
                    onCheckedChange = onAutoGuideChange
                )
            }
        }
    }
}

// -------------------------------------------
// Guidance and Voice Controls
// -------------------------------------------

@Composable
private fun KDriveStatusControls(
    state: KDriveDrivingStatusUiState,
    hasNavigationData: Boolean,
    onDemoStartRequest: () -> Unit,
    onDemoStopRequest: () -> Unit,
    onReplayRequest: () -> Unit,
    onStopVoiceRequest: () -> Unit
) {

    val colors = MaterialTheme.colorScheme

    val hasReplay =
        hasNavigationData &&
                !state.guidanceText.isNullOrBlank()

    Column(
        verticalArrangement =
            Arrangement.spacedBy(14.dp)
    ) {

        Card(
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                Text(
                    text = "CURRENT GUIDANCE",
                    style =
                        MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )

                Text(
                    text = if (hasNavigationData) {
                        kdriveShortMessage(
                            state.guidanceTitle
                        )
                    } else {
                        "No active guidance"
                    },
                    style =
                        MaterialTheme.typography.titleLarge,
                    maxLines = 2
                )

                Text(
                    text = if (hasNavigationData) {
                        kdriveShortMessage(
                            state.guidanceText
                        )
                    } else {
                        "Navigation guidance will appear " +
                                "here when available."
                    },
                    style =
                        MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (hasNavigationData) {
                        "Voice: " +
                                kdriveShortMessage(
                                    state.voiceStatus,
                                    60
                                )
                    } else {
                        "Voice: no active navigation guidance"
                    },
                    style =
                        MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(
                    KDriveUiSize.actionGap
                )
        ) {

            KDriveActionButton(
                label = "Replay",
                onClick = onReplayRequest,
                modifier = Modifier.weight(1f),
                enabled = hasReplay
            )

            KDriveActionButton(
                label = "Stop voice",
                onClick = onStopVoiceRequest,
                modifier = Modifier.weight(1f),
                filled = false
            )
        }

        KDriveActionButton(
            label = if (state.demoRunning) {
                "Stop demo"
            } else {
                "Start demo"
            },
            onClick = if (state.demoRunning) {
                onDemoStopRequest
            } else {
                onDemoStartRequest
            },
            modifier = Modifier.fillMaxWidth(),
            filled = false
        )

        Text(
            text = "Demo is simulated. " +
                    "Not live driving information.",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2
        )
    }
}

// -------------------------------------------
// Preview: Demo
// -------------------------------------------

@Preview(
    name = "Status - Parked demo",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KDriveStatusParkedPreview() {

    KDriveTheme(KDRIVE_THEME_PRESETS[0]) {

        DrivingStatusScreen(
            state = KDriveDrivingStatusUiState(
                source = KDriveUiDataSource.DEMO,
                navigationStatus = "Simulation active",
                demoRunning = true,
                placeName = "Sample Tollgate",
                distanceMeters = 500.0,
                guidanceTitle = "Tollgate ahead",
                guidanceText =
                    "Choose a safe payment lane."
            ),
            onBack = {},
            onAutoGuideChange = {},
            onDemoStartRequest = {},
            onDemoStopRequest = {},
            onReplayRequest = {},
            onStopVoiceRequest = {},
            interactionRestricted = false
        )
    }
}

// -------------------------------------------
// Preview: NO DATA + Previous Manual Rule
// -------------------------------------------

@Preview(
    name = "Status - No Data with old manual text",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KDriveStatusNoDataPreview() {

    KDriveTheme(KDRIVE_THEME_PRESETS[0]) {

        DrivingStatusScreen(
            state = KDriveDrivingStatusUiState(
                source = KDriveUiDataSource.NO_DATA,
                guidanceTitle = "Manual rule",
                guidanceText =
                    "Yield to traffic when merging.",
                voiceStatus = "Stop requested"
            ),
            onBack = {},
            onAutoGuideChange = {},
            onDemoStartRequest = {},
            onDemoStopRequest = {},
            onReplayRequest = {},
            onStopVoiceRequest = {},
            interactionRestricted = false
        )
    }
}

// -------------------------------------------
// Preview: Driving Restricted
// -------------------------------------------

@Preview(
    name = "Status - Driving restricted",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KDriveStatusRestrictedPreview() {

    KDriveTheme(KDRIVE_THEME_PRESETS[0]) {

        DrivingStatusScreen(
            state = KDriveDrivingStatusUiState(
                source = KDriveUiDataSource.REAL,
                guidanceTitle = "Roundabout ahead",
                guidanceText =
                    "Yield to circulating traffic."
            ),
            onBack = {},
            onAutoGuideChange = {},
            onDemoStartRequest = {},
            onDemoStopRequest = {},
            onReplayRequest = {},
            onStopVoiceRequest = {},
            interactionRestricted = true
        )
    }
}
