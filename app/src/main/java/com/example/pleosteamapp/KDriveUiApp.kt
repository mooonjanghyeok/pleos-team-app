package com.example.pleosteamapp

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview

/*
 * PC2 UI-only entrypoint. PC1 supplies restriction state derived from
 * CarUxRestrictions; no real vehicle SDK is called here.
 * Fail-closed: when restriction state is not supplied, it defaults to true.
 */
private const val UI_PREFS = "kdrive_ui_colors_v2"

private fun loadSavedCustom(prefs: SharedPreferences?): KDrivePalette {
    val fallback = KDRIVE_CUSTOM_DEFAULT
    val bg = prefs?.getString("bg", fallback.backgroundHex) ?: fallback.backgroundHex
    val card = prefs?.getString("card", fallback.cardHex) ?: fallback.cardHex
    val accent = prefs?.getString("accent", fallback.accentHex) ?: fallback.accentHex
    val text = prefs?.getString("text", fallback.textHex) ?: fallback.textHex
    val draft = KDrivePalette("custom", "Custom", bg, card, accent, text,
        (kdriveParseHex(bg)?.luminance() ?: 0.0f) < 0.5f)
    return if (kdrivePaletteIsReadable(draft)) draft else fallback
}

@Composable
fun KDriveUiApp(
    state: KDriveDrivingStatusUiState,
    onAutoGuideChange: (Boolean) -> Unit,
    onDemoStartRequest: () -> Unit,
    onDemoStopRequest: () -> Unit,
    onSpeakRequest: (String) -> Unit,
    onStopVoiceRequest: () -> Unit,
    onReplayRequest: () -> Unit,
    isDrivingRestricted: Boolean = true
) {
    val context = LocalContext.current
    val inspectionMode = LocalInspectionMode.current
    val systemDark = isSystemInDarkTheme()
    val prefs = remember(context, inspectionMode) {
        if (inspectionMode) null else
            context.applicationContext.getSharedPreferences(UI_PREFS, Context.MODE_PRIVATE)
    }
    var themeId by remember(prefs) {
        mutableStateOf(prefs?.getString("theme", KDRIVE_DEFAULT_THEME) ?: KDRIVE_DEFAULT_THEME)
    }
    var custom by remember(prefs) { mutableStateOf(loadSavedCustom(prefs)) }
    var page by rememberSaveable { mutableStateOf("HOME") }

    LaunchedEffect(isDrivingRestricted) {
        if (isDrivingRestricted) {
            if (page == "RULES") onStopVoiceRequest()
            page = "HOME"
        }
    }
    BackHandler(enabled = page != "HOME" && !isDrivingRestricted) {
        if (page == "RULES") onStopVoiceRequest()
        page = "HOME"
    }

    KDriveTheme(kdriveResolvePalette(themeId, custom, systemDark)) {
        when {
            isDrivingRestricted -> KDriveHomeScreen(
                navigationStatus = if (state.source == KDriveUiDataSource.REAL)
                    state.guidanceTitle ?: state.navigationStatus else "Navigation guidance unavailable",
                autoGuideEnabled = state.autoGuideEnabled,
                demoMode = false,
                onAutoGuideChange = onAutoGuideChange,
                onTrafficRulesClick = {}, onDrivingStatusClick = {}, onDemoClick = {},
                interactionRestricted = true
            )
            page == "RULES" -> TrafficRulesScreen(
                onBack = { page = "HOME" }, onSpeakRequest = onSpeakRequest,
                onStopRequest = onStopVoiceRequest, interactionRestricted = false
            )
            page == "STATUS" -> DrivingStatusScreen(
                state = state, onBack = { page = "HOME" },
                onAutoGuideChange = onAutoGuideChange,
                onDemoStartRequest = onDemoStartRequest,
                onDemoStopRequest = onDemoStopRequest,
                onReplayRequest = onReplayRequest,
                onStopVoiceRequest = onStopVoiceRequest,
                interactionRestricted = false
            )
            page == "THEME" -> KDriveThemeSettingsScreen(
                selectedThemeId = themeId,
                customPalette = custom,
                onThemeSelected = { chosen ->
                    themeId = chosen
                    prefs?.edit()?.putString("theme", chosen)?.apply()
                },
                onSaveCustom = { chosen ->
                    if (kdrivePaletteIsReadable(chosen)) {
                        custom = chosen
                        themeId = "custom"
                        prefs?.edit()
                            ?.putString("theme", "custom")
                            ?.putString("bg", chosen.backgroundHex)
                            ?.putString("card", chosen.cardHex)
                            ?.putString("accent", chosen.accentHex)
                            ?.putString("text", chosen.textHex)
                            ?.apply()
                    }
                },
                onReset = {
                    themeId = KDRIVE_DEFAULT_THEME
                    custom = KDRIVE_CUSTOM_DEFAULT
                    prefs?.edit()?.clear()?.apply()
                },
                onBack = { page = "HOME" },
                interactionRestricted = false
            )
            else -> KDriveHomeScreen(
                navigationStatus = state.navigationStatus,
                autoGuideEnabled = state.autoGuideEnabled,
                demoMode = state.source == KDriveUiDataSource.DEMO,
                onAutoGuideChange = onAutoGuideChange,
                onTrafficRulesClick = { page = "RULES" },
                onDrivingStatusClick = { page = "STATUS" },
                onDemoClick = { onDemoStartRequest(); page = "STATUS" },
                onThemeSettingsClick = { page = "THEME" },
                interactionRestricted = false
            )
        }
    }
}

// Independent UI demonstration: does NOT use real TTS or navigation.
@Composable
private fun KDrivePreviewHarness(restricted: Boolean) {
    var state by remember { mutableStateOf(KDriveDrivingStatusUiState()) }
    var lastSpeech by remember { mutableStateOf("") }
    KDriveUiApp(
        state = state,
        onAutoGuideChange = { state = state.copy(autoGuideEnabled = it) },
        onDemoStartRequest = {
            state = state.copy(
                source = KDriveUiDataSource.DEMO, navigationStatus = "Simulated route",
                demoRunning = true, placeName = "Sample Tollgate", distanceMeters = 500.0,
                guidanceTitle = "Tollgate ahead", guidanceText = "Check the payment lane signs.",
                voiceStatus = "Preview only"
            )
        },
        onDemoStopRequest = {
            state = state.copy(source = KDriveUiDataSource.NO_DATA,
                navigationStatus = "Simulation stopped", demoRunning = false,
                placeName = null, distanceMeters = null, guidanceText = null, guidanceTitle = null)
        },
        onSpeakRequest = { speech ->
            lastSpeech = speech
            state = state.copy(guidanceTitle = "Manual rule", guidanceText = speech,
                voiceStatus = "Voice requested (preview)")
        },
        onStopVoiceRequest = { state = state.copy(voiceStatus = "Stop requested (preview)") },
        onReplayRequest = { state = state.copy(voiceStatus = if (lastSpeech.isNotBlank())
            "Replay requested (preview)" else "No saved message") },
        isDrivingRestricted = restricted
    )
}

@Preview(name = "K-DRIVE Parked Landscape", widthDp = 1000, heightDp = 600, showBackground = true)
@Composable
private fun KDriveParkedLandscapePreview() { KDrivePreviewHarness(false) }

@Preview(name = "K-DRIVE Parked Portrait", widthDp = 400, heightDp = 800, showBackground = true)
@Composable
private fun KDriveParkedPortraitPreview() { KDrivePreviewHarness(false) }

@Preview(name = "K-DRIVE Driving Restricted", widthDp = 1000, heightDp = 600, showBackground = true)
@Composable
private fun KDriveRestrictedPreview() { KDrivePreviewHarness(true) }
