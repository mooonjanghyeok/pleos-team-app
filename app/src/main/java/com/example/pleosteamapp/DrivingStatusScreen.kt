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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

enum class KDriveUiDataSource { NO_DATA, REAL, DEMO }

data class KDriveDrivingStatusUiState(
    val source: KDriveUiDataSource = KDriveUiDataSource.NO_DATA,
    val navigationStatus: String = "Waiting for navigation",
    val autoGuideEnabled: Boolean = false,
    val demoRunning: Boolean = false,
    val placeName: String? = null,
    val distanceMeters: Double? = null,
    val guidanceTitle: String? = null,
    val guidanceText: String? = null,
    val voiceStatus: String = "Idle"
)

// English on-screen text: at most 120 chars, 24 words, and 3 lines at render time.
private fun kdriveShortMessage(source: String?, maxChars: Int = 120): String {
    if (source.isNullOrBlank()) return "No current guidance"
    val words = source.trim().split(Regex("\\s+")).take(24).joinToString(" ")
    return if (words.length <= maxChars) words else words.take(maxChars - 1).trimEnd() + "…"
}

private fun kdriveDistanceText(meters: Double?): String =
    if (meters == null || !meters.isFinite() || meters < 0.0) "—" else
        String.format(Locale.US, "%,.0f m", meters)

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
    if (interactionRestricted) {
        Column(modifier = Modifier.fillMaxSize().background(colors.background).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text("K-DRIVE", style = MaterialTheme.typography.headlineLarge)
            Text("LIVE GUIDANCE", style = MaterialTheme.typography.titleLarge,
                color = colors.primary)
            if (state.source == KDriveUiDataSource.REAL) {
                Text(kdriveShortMessage(state.guidanceTitle),
                    style = MaterialTheme.typography.headlineMedium, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
                Text(kdriveShortMessage(state.guidanceText, 80),
                    style = MaterialTheme.typography.titleLarge, maxLines = 2,
                    overflow = TextOverflow.Ellipsis)
            } else {
                Text("Navigation guidance unavailable", style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            // No tutorial / demo / long text / theme controls in restricted mode.
        }
        return
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(colors.background)) {
        val wide = maxWidth >= 670.dp
        Column(modifier = Modifier.fillMaxSize().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KDriveActionButton("Back", onBack, filled = false)
                Text("Driving Status", style = MaterialTheme.typography.headlineMedium)
            }
            if (wide) {
                Row(modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        KDriveStatusOverview(state, onAutoGuideChange)
                    }
                    Column(modifier = Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
                        KDriveStatusControls(state, onDemoStartRequest, onDemoStopRequest,
                            onReplayRequest, onStopVoiceRequest)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    KDriveStatusOverview(state, onAutoGuideChange)
                    KDriveStatusControls(state, onDemoStartRequest, onDemoStopRequest,
                        onReplayRequest, onStopVoiceRequest)
                }
            }
        }
    }
}

@Composable
private fun KDriveStatusOverview(
    state: KDriveDrivingStatusUiState,
    onAutoGuideChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("DATA SOURCE", style = MaterialTheme.typography.labelLarge, color = colors.primary)
                Text(when (state.source) {
                    KDriveUiDataSource.NO_DATA -> "NO DATA"
                    KDriveUiDataSource.REAL -> "REAL NAVIGATION"
                    KDriveUiDataSource.DEMO -> "DEMO · SIMULATION"
                }, style = MaterialTheme.typography.titleLarge)
                Text(kdriveShortMessage(state.navigationStatus, 80),
                    style = MaterialTheme.typography.bodyLarge, maxLines = 2)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.surface),
            shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("NEXT GUIDANCE POINT", style = MaterialTheme.typography.labelLarge,
                    color = colors.primary)
                Text(kdriveShortMessage(state.placeName), style = MaterialTheme.typography.titleLarge,
                    maxLines = 2)
                Text(kdriveDistanceText(state.distanceMeters),
                    style = MaterialTheme.typography.headlineLarge)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = colors.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Automatic guide", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f))
                Switch(checked = state.autoGuideEnabled, onCheckedChange = onAutoGuideChange)
            }
        }
    }
}

@Composable
private fun KDriveStatusControls(
    state: KDriveDrivingStatusUiState,
    onDemoStartRequest: () -> Unit,
    onDemoStopRequest: () -> Unit,
    onReplayRequest: () -> Unit,
    onStopVoiceRequest: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("CURRENT GUIDANCE", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(kdriveShortMessage(state.guidanceTitle),
                    style = MaterialTheme.typography.titleLarge, maxLines = 2)
                Text(kdriveShortMessage(state.guidanceText),
                    style = MaterialTheme.typography.bodyLarge, maxLines = 3,
                    overflow = TextOverflow.Ellipsis)
                Text("Voice: ${kdriveShortMessage(state.voiceStatus, 60)}",
                    style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(KDriveUiSize.actionGap)) {
            KDriveActionButton("Replay", onReplayRequest,
                modifier = Modifier.weight(1f), enabled = !state.guidanceText.isNullOrBlank())
            KDriveActionButton("Stop voice", onStopVoiceRequest,
                modifier = Modifier.weight(1f), filled = false)
        }
        KDriveActionButton(
            label = if (state.demoRunning) "Stop demo" else "Start demo",
            onClick = if (state.demoRunning) onDemoStopRequest else onDemoStartRequest,
            modifier = Modifier.fillMaxWidth(), filled = false
        )
        Text("Demo is simulated. Not live driving information.",
            style = MaterialTheme.typography.bodyMedium, maxLines = 2)
    }
}

@Preview(name = "Status - Parked demo", widthDp = 1000, heightDp = 600, showBackground = true)
@Composable
private fun KDriveStatusParkedPreview() {
    KDriveTheme(KDRIVE_THEME_PRESETS[0]) {
        DrivingStatusScreen(
            state = KDriveDrivingStatusUiState(source = KDriveUiDataSource.DEMO,
                navigationStatus = "Simulation active", demoRunning = true,
                placeName = "Sample Tollgate", distanceMeters = 500.0,
                guidanceTitle = "Tollgate ahead", guidanceText = "Choose a safe payment lane."),
            onBack = {}, onAutoGuideChange = {}, onDemoStartRequest = {},
            onDemoStopRequest = {}, onReplayRequest = {}, onStopVoiceRequest = {},
            interactionRestricted = false
        )
    }
}

@Preview(name = "Status - Driving restricted", widthDp = 1000, heightDp = 600, showBackground = true)
@Composable
private fun KDriveStatusRestrictedPreview() {
    KDriveTheme(KDRIVE_THEME_PRESETS[0]) {
        DrivingStatusScreen(
            state = KDriveDrivingStatusUiState(source = KDriveUiDataSource.REAL,
                guidanceTitle = "Roundabout ahead", guidanceText = "Yield to circulating traffic."),
            onBack = {}, onAutoGuideChange = {}, onDemoStartRequest = {},
            onDemoStopRequest = {}, onReplayRequest = {}, onStopVoiceRequest = {},
            interactionRestricted = true
        )
    }
}
