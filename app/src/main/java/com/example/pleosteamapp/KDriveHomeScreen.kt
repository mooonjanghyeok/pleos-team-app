
package com.example.pleosteamapp

import android.content.Context

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.util.Locale

/*
 * K-DRIVE GUIDE / PC2
 *
 * Home redesign
 *
 * Left:
 * - Welcome card
 * - Automatic voice guidance
 * - Accident & Emergency
 *
 * Right:
 * - Traffic Rules
 * - Driving Status
 * - Demo Mode
 *
 * Preserves:
 * - Existing PC1 callback interface
 * - 4 languages
 * - 10 theme presets
 * - Previous-page navigation
 * - Automatic guide control
 * - Emergency screen
 * - Emergency TTS callbacks
 * - Driving restriction mode
 *
 * No external image dependencies.
 */

// =====================================================
// Language preferences
// =====================================================

private const val KD_HOME_PREFS =
    "kdrive_ui_colors_v2"

private const val KD_HOME_LANG =
    "startup_language"

private fun kdHomeDefaultLanguage(): String =
    when (
        Locale.getDefault()
            .language
            .lowercase(Locale.ROOT)
    ) {
        "ko" -> "KO"
        "fr" -> "FR"
        "es" -> "ES"
        else -> "EN"
    }

private fun kdHomeLanguage(value: String?): String =
    when (value?.uppercase(Locale.ROOT)) {
        "KO" -> "KO"
        "EN" -> "EN"
        "FR" -> "FR"
        "ES" -> "ES"
        else -> "EN"
    }

// =====================================================
// 4-language UI text
// =====================================================

private data class KdHomeWords(
    val guidance: String,
    val currentStatus: String,
    val restrictedMessage: String,

    val welcomeLabel: String,
    val welcomeTitle: String,
    val welcomeDescription: String,

    val automaticTitle: String,
    val automaticDescription: String,
    val enabled: String,
    val disabled: String,

    val emergencyTitle: String,
    val emergencyDescription: String,

    val rulesTitle: String,
    val rulesDescription: String,

    val statusTitle: String,
    val statusDescription: String,

    val demoTitle: String,
    val demoDescription: String,
    val demoActive: String,

    val back: String
)

private fun kdHomeWords(language: String): KdHomeWords =
    when (language) {

        // -----------------------------------------
        // Korean
        // -----------------------------------------

        "KO" -> KdHomeWords(
            guidance = "한국 운전 가이드",

            currentStatus = "현재 상태",

            restrictedMessage =
                "주행 중에는 상세 메뉴를 사용할 수 없습니다.",

            welcomeLabel = "WELCOME TO KOREA",

            welcomeTitle =
                "한국에서의 운전,\nK-DRIVE가 함께합니다.",

            welcomeDescription =
                "낯선 교통규칙도 쉽게 알아보세요.",

            automaticTitle = "자동 음성 안내",

            automaticDescription =
                "주행 상황별 안내를 켜거나 끕니다.",

            enabled = "켜짐",
            disabled = "꺼짐",

            emergencyTitle = "사고 및 긴급상황",

            emergencyDescription =
                "112 · 119 · 한국어 도움 요청",

            rulesTitle = "교통규칙",

            rulesDescription =
                "한국의 교통법규 알아보기",

            statusTitle = "주행 상태",

            statusDescription =
                "현재 주행 안내 확인",

            demoTitle = "데모 모드",

            demoDescription =
                "가상 주행 상황 체험",

            demoActive = "데모 실행 중",

            back = "← 뒤로"
        )

        // -----------------------------------------
        // French
        // -----------------------------------------

        "FR" -> KdHomeWords(
            guidance = "Guide de conduite en Corée",

            currentStatus = "État actuel",

            restrictedMessage =
                "Les menus détaillés sont indisponibles pendant la conduite.",

            welcomeLabel = "BIENVENUE EN CORÉE",

            welcomeTitle =
                "Conduisez sereinement\nen Corée.",

            welcomeDescription =
                "Découvrez facilement les règles de conduite locales.",

            automaticTitle = "Guidage vocal automatique",

            automaticDescription =
                "Activez ou désactivez les annonces vocales.",

            enabled = "Activé",
            disabled = "Désactivé",

            emergencyTitle = "Accident et urgences",

            emergencyDescription =
                "112 · 119 · Aide en coréen",

            rulesTitle = "Code de la route",

            rulesDescription =
                "Découvrez les règles coréennes",

            statusTitle = "État de conduite",

            statusDescription =
                "Consultez le guidage actuel",

            demoTitle = "Mode démo",

            demoDescription =
                "Essayez une conduite simulée",

            demoActive = "Démo en cours",

            back = "← Retour"
        )

        // -----------------------------------------
        // Spanish
        // -----------------------------------------

        "ES" -> KdHomeWords(
            guidance = "Guía de conducción en Corea",

            currentStatus = "Estado actual",

            restrictedMessage =
                "Los menús detallados no están disponibles mientras conduce.",

            welcomeLabel = "BIENVENIDO A COREA",

            welcomeTitle =
                "Conduzca con confianza\nen Corea.",

            welcomeDescription =
                "Conozca fácilmente las normas de tráfico locales.",

            automaticTitle = "Guía de voz automática",

            automaticDescription =
                "Active o desactive los avisos de voz.",

            enabled = "Activada",
            disabled = "Desactivada",

            emergencyTitle = "Accidentes y emergencias",

            emergencyDescription =
                "112 · 119 · Ayuda en coreano",

            rulesTitle = "Normas de tráfico",

            rulesDescription =
                "Conozca las normas de Corea",

            statusTitle = "Estado de conducción",

            statusDescription =
                "Consulte la guía actual",

            demoTitle = "Modo demo",

            demoDescription =
                "Pruebe una conducción simulada",

            demoActive = "Demo activa",

            back = "← Volver"
        )

        // -----------------------------------------
        // English
        // -----------------------------------------

        else -> KdHomeWords(
            guidance = "Korean Driving Guide",

            currentStatus = "Current status",

            restrictedMessage =
                "Detailed menus are unavailable while driving.",

            welcomeLabel = "WELCOME TO KOREA",

            welcomeTitle =
                "Drive with confidence\nin Korea.",

            welcomeDescription =
                "Discover Korean driving rules with ease.",

            automaticTitle = "Automatic Voice Guide",

            automaticDescription =
                "Turn driving guidance audio on or off.",

            enabled = "ON",
            disabled = "OFF",

            emergencyTitle = "Accident & Emergency",

            emergencyDescription =
                "112 · 119 · Korean assistance",

            rulesTitle = "Traffic Rules",

            rulesDescription =
                "Learn Korean traffic regulations",

            statusTitle = "Driving Status",

            statusDescription =
                "Check your current guidance",

            demoTitle = "Demo Mode",

            demoDescription =
                "Experience a simulated drive",

            demoActive = "DEMO ACTIVE",

            back = "← Back"
        )
    }

// =====================================================
// Icon drawing
// Uses theme color, no extra icon dependency.
// =====================================================

private enum class KdHomeIconType {
    CAR,
    SPEAKER,
    SHIELD,
    BOOK,
    GAUGE,
    PLAY
}

@Composable
private fun KdHomeLineIcon(
    type: KdHomeIconType,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier) {
        val w = size.width
        val h = size.height

        val strokeWidth = 2.2.dp.toPx()

        when (type) {

            // ---------------------------------
            // Front-facing car
            // ---------------------------------

            KdHomeIconType.CAR -> {

                val roof = Path().apply {
                    moveTo(w * 0.20f, h * 0.45f)
                    lineTo(w * 0.30f, h * 0.18f)
                    lineTo(w * 0.70f, h * 0.18f)
                    lineTo(w * 0.80f, h * 0.45f)
                }

                drawPath(
                    roof,
                    color,
                    style = Stroke(strokeWidth)
                )

                drawRoundRect(
                    color = color,

                    topLeft = Offset(
                        w * 0.12f,
                        h * 0.43f
                    ),

                    size = Size(
                        w * 0.76f,
                        h * 0.34f
                    ),

                    cornerRadius =
                        CornerRadius(5.dp.toPx()),

                    style = Stroke(strokeWidth)
                )

                drawLine(
                    color,
                    Offset(w * 0.22f, h * 0.59f),
                    Offset(w * 0.33f, h * 0.59f),
                    strokeWidth
                )

                drawLine(
                    color,
                    Offset(w * 0.67f, h * 0.59f),
                    Offset(w * 0.78f, h * 0.59f),
                    strokeWidth
                )

                drawLine(
                    color,
                    Offset(w * 0.24f, h * 0.77f),
                    Offset(w * 0.24f, h * 0.88f),
                    strokeWidth
                )

                drawLine(
                    color,
                    Offset(w * 0.76f, h * 0.77f),
                    Offset(w * 0.76f, h * 0.88f),
                    strokeWidth
                )
            }

            // ---------------------------------
            // Speaker
            // ---------------------------------

            KdHomeIconType.SPEAKER -> {

                val speaker = Path().apply {
                    moveTo(w * 0.14f, h * 0.40f)
                    lineTo(w * 0.30f, h * 0.40f)
                    lineTo(w * 0.48f, h * 0.23f)
                    lineTo(w * 0.48f, h * 0.77f)
                    lineTo(w * 0.30f, h * 0.60f)
                    lineTo(w * 0.14f, h * 0.60f)
                    close()
                }

                drawPath(speaker, color)

                drawArc(
                    color = color,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,

                    topLeft = Offset(
                        w * 0.37f,
                        h * 0.28f
                    ),

                    size = Size(
                        w * 0.40f,
                        h * 0.44f
                    ),

                    style = Stroke(strokeWidth)
                )

                drawArc(
                    color = color,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,

                    topLeft = Offset(
                        w * 0.32f,
                        h * 0.14f
                    ),

                    size = Size(
                        w * 0.62f,
                        h * 0.72f
                    ),

                    style = Stroke(strokeWidth)
                )
            }

            // ---------------------------------
            // Emergency shield
            // ---------------------------------

            KdHomeIconType.SHIELD -> {

                val shield = Path().apply {
                    moveTo(w * 0.50f, h * 0.10f)
                    lineTo(w * 0.82f, h * 0.23f)
                    lineTo(w * 0.80f, h * 0.58f)

                    cubicTo(
                        w * 0.77f,
                        h * 0.76f,
                        w * 0.64f,
                        h * 0.88f,
                        w * 0.50f,
                        h * 0.94f
                    )

                    cubicTo(
                        w * 0.36f,
                        h * 0.88f,
                        w * 0.23f,
                        h * 0.76f,
                        w * 0.20f,
                        h * 0.58f
                    )

                    lineTo(w * 0.18f, h * 0.23f)
                    close()
                }

                drawPath(
                    shield,
                    color,
                    style = Stroke(strokeWidth)
                )

                drawLine(
                    color,
                    Offset(w * 0.50f, h * 0.36f),
                    Offset(w * 0.50f, h * 0.61f),
                    strokeWidth
                )

                drawCircle(
                    color,
                    radius = strokeWidth * 0.70f,
                    center = Offset(
                        w * 0.50f,
                        h * 0.73f
                    )
                )
            }

            // ---------------------------------
            // Open book
            // ---------------------------------

            KdHomeIconType.BOOK -> {

                val leftBook = Path().apply {
                    moveTo(w * 0.50f, h * 0.25f)
                    quadraticTo(
                        w * 0.32f,
                        h * 0.12f,
                        w * 0.12f,
                        h * 0.23f
                    )
                    lineTo(w * 0.12f, h * 0.77f)
                    quadraticTo(
                        w * 0.32f,
                        h * 0.68f,
                        w * 0.50f,
                        h * 0.81f
                    )
                }

                val rightBook = Path().apply {
                    moveTo(w * 0.50f, h * 0.25f)
                    quadraticTo(
                        w * 0.68f,
                        h * 0.12f,
                        w * 0.88f,
                        h * 0.23f
                    )
                    lineTo(w * 0.88f, h * 0.77f)
                    quadraticTo(
                        w * 0.68f,
                        h * 0.68f,
                        w * 0.50f,
                        h * 0.81f
                    )
                }

                drawPath(
                    leftBook,
                    color,
                    style = Stroke(strokeWidth)
                )

                drawPath(
                    rightBook,
                    color,
                    style = Stroke(strokeWidth)
                )

                drawLine(
                    color,
                    Offset(w * 0.50f, h * 0.25f),
                    Offset(w * 0.50f, h * 0.81f),
                    strokeWidth
                )
            }

            // ---------------------------------
            // Driving gauge
            // ---------------------------------

            KdHomeIconType.GAUGE -> {

                drawArc(
                    color = color,
                    startAngle = 145f,
                    sweepAngle = 250f,
                    useCenter = false,

                    topLeft = Offset(
                        w * 0.13f,
                        h * 0.13f
                    ),

                    size = Size(
                        w * 0.74f,
                        h * 0.74f
                    ),

                    style = Stroke(strokeWidth)
                )

                drawLine(
                    color,
                    Offset(w * 0.50f, h * 0.61f),
                    Offset(w * 0.72f, h * 0.36f),
                    strokeWidth
                )

                drawCircle(
                    color,
                    radius = strokeWidth * 1.4f,
                    center = Offset(
                        w * 0.50f,
                        h * 0.61f
                    )
                )
            }

            // ---------------------------------
            // Play
            // ---------------------------------

            KdHomeIconType.PLAY -> {

                drawCircle(
                    color,
                    radius = minOf(w, h) * 0.37f,
                    center = Offset(w * 0.50f, h * 0.50f),
                    style = Stroke(strokeWidth)
                )

                val play = Path().apply {
                    moveTo(w * 0.42f, h * 0.32f)
                    lineTo(w * 0.68f, h * 0.50f)
                    lineTo(w * 0.42f, h * 0.68f)
                    close()
                }

                drawPath(play, color)
            }
        }
    }
}

// =====================================================
// Shared icon tile
// =====================================================

@Composable
private fun KdHomeIconTile(
    type: KdHomeIconType,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .size(49.dp)
            .background(
                color = colors.primary.copy(
                    alpha = 0.11f
                ),
                shape = RoundedCornerShape(14.dp)
            ),

        contentAlignment = Alignment.Center
    ) {
        KdHomeLineIcon(
            type = type,
            modifier = Modifier.size(29.dp)
        )
    }
}

// =====================================================
// Main Home
// =====================================================

@Suppress("UNUSED_PARAMETER")
@Composable
fun KDriveHomeScreen(
    navigationStatus: String,
    autoGuideEnabled: Boolean,
    demoMode: Boolean,
    onAutoGuideChange: (Boolean) -> Unit,
    onTrafficRulesClick: () -> Unit,
    onDrivingStatusClick: () -> Unit,
    onDemoClick: () -> Unit,
    onThemeSettingsClick: () -> Unit = {},
    interactionRestricted: Boolean = true,
    onWelcomeClick: () -> Unit = {},
    onBackClick: () -> Unit = onWelcomeClick,
    showBackButton: Boolean = true,
    backLabel: String = "← Back",
    uiLanguage: String? = null,
    onEmergencySpeakRequest: (String) -> Unit = {},
    onEmergencyStopRequest: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current

    val savedLanguage = remember(
        context,
        inspection
    ) {
        if (inspection) {
            kdHomeDefaultLanguage()
        } else {
            context.applicationContext
                .getSharedPreferences(
                    KD_HOME_PREFS,
                    Context.MODE_PRIVATE
                )
                .getString(
                    KD_HOME_LANG,
                    kdHomeDefaultLanguage()
                ) ?: kdHomeDefaultLanguage()
        }
    }

    val language = kdHomeLanguage(
        uiLanguage ?: savedLanguage
    )

    val words = kdHomeWords(language)

    val actualBackLabel = if (
        backLabel == "← Back"
    ) {
        words.back
    } else {
        backLabel
    }

    var showEmergency by rememberSaveable {
        mutableStateOf(false)
    }

    // -----------------------------------------
    // Driving restrictions take priority
    // -----------------------------------------

    LaunchedEffect(interactionRestricted) {
        if (interactionRestricted) {
            showEmergency = false
        }
    }

    if (interactionRestricted) {
        KdHomeRestrictedScreen(
            words = words,
            navigationStatus = navigationStatus
        )
        return
    }

    // -----------------------------------------
    // Accident & Emergency
    // -----------------------------------------

    if (showEmergency) {
        KDriveAccidentEmergencyScreen(
            uiLanguage = language,

            onBack = {
                showEmergency = false
            },

            onSpeakRequest =
                onEmergencySpeakRequest,

            onStopVoiceRequest =
                onEmergencyStopRequest
        )
        return
    }

    // -----------------------------------------
    // Normal Home screen
    // -----------------------------------------

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Two columns for landscape.
        // Smaller layouts use a scrollable column.
        val landscape =
            maxWidth >= 700.dp &&
                    maxHeight >= 480.dp

        val wide = maxWidth >= 700.dp

        if (landscape) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                KdHomeHeader(
                    words = words,
                    backLabel = actualBackLabel,
                    showBackButton = showBackButton,
                    onBackClick = onBackClick,
                    wide = true
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    // =================================
                    // Left: Three feature cards
                    // =================================

                    Column(
                        modifier = Modifier
                            .weight(1.06f)
                            .fillMaxHeight(),

                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        KdHomeWelcomeCard(
                            words = words,
                            modifier = Modifier.weight(1.24f),
                            expanded = true
                        )

                        KdHomeAutomaticCard(
                            words = words,
                            autoGuideEnabled =
                                autoGuideEnabled,
                            onAutoGuideChange =
                                onAutoGuideChange,
                            modifier = Modifier.weight(0.96f),
                            expanded = true
                        )

                        KdHomeEmergencyCard(
                            words = words,
                            onClick = {
                                showEmergency = true
                            },
                            modifier = Modifier.weight(0.96f),
                            expanded = true
                        )
                    }

                    // =================================
                    // Right: Three menu cards
                    // =================================

                    Column(
                        modifier = Modifier
                            .weight(0.94f)
                            .fillMaxHeight(),

                        verticalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        KdHomeMenuCard(
                            number = "01",
                            title = words.rulesTitle,
                            description =
                                words.rulesDescription,
                            icon = KdHomeIconType.BOOK,
                            onClick = onTrafficRulesClick,
                            modifier = Modifier.weight(1f),
                            expanded = true
                        )

                        KdHomeMenuCard(
                            number = "02",
                            title = words.statusTitle,
                            description =
                                words.statusDescription,
                            icon = KdHomeIconType.GAUGE,
                            onClick = onDrivingStatusClick,
                            modifier = Modifier.weight(1f),
                            expanded = true
                        )

                        KdHomeMenuCard(
                            number = "03",
                            title = words.demoTitle,
                            description =
                                words.demoDescription,
                            icon = KdHomeIconType.PLAY,
                            onClick = onDemoClick,
                            modifier = Modifier.weight(1f),
                            expanded = true,
                            activeLabel = if (demoMode) {
                                words.demoActive
                            } else {
                                null
                            }
                        )
                    }
                }
            }

        } else {
            // =================================
            // Portrait / small window
            // =================================

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                KdHomeHeader(
                    words = words,
                    backLabel = actualBackLabel,
                    showBackButton = showBackButton,
                    onBackClick = onBackClick,
                    wide = wide
                )

                KdHomeWelcomeCard(
                    words = words,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 170.dp)
                )

                KdHomeAutomaticCard(
                    words = words,
                    autoGuideEnabled =
                        autoGuideEnabled,
                    onAutoGuideChange =
                        onAutoGuideChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                )

                KdHomeEmergencyCard(
                    words = words,
                    onClick = {
                        showEmergency = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp)
                )

                KdHomeMenuCard(
                    number = "01",
                    title = words.rulesTitle,
                    description = words.rulesDescription,
                    icon = KdHomeIconType.BOOK,
                    onClick = onTrafficRulesClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp)
                )

                KdHomeMenuCard(
                    number = "02",
                    title = words.statusTitle,
                    description = words.statusDescription,
                    icon = KdHomeIconType.GAUGE,
                    onClick = onDrivingStatusClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp)
                )

                KdHomeMenuCard(
                    number = "03",
                    title = words.demoTitle,
                    description = words.demoDescription,
                    icon = KdHomeIconType.PLAY,
                    onClick = onDemoClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 130.dp),
                    activeLabel = if (demoMode) {
                        words.demoActive
                    } else {
                        null
                    }
                )
            }
        }
    }
}

// =====================================================
// Home Header
// =====================================================

@Composable
private fun KdHomeHeader(
    words: KdHomeWords,
    backLabel: String,
    showBackButton: Boolean,
    onBackClick: () -> Unit,
    wide: Boolean
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement =
                Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "K-DRIVE",
                fontSize = if (wide) 32.sp else 28.sp,
                lineHeight = if (wide) 38.sp else 34.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )

            Text(
                text = words.guidance,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = colors.primary,
                fontWeight = FontWeight.Medium
            )
        }

        if (showBackButton) {
            KDriveActionButton(
                label = backLabel,
                onClick = onBackClick,
                filled = false
            )
        }
    }
}

// =====================================================
// LEFT 1: Welcome Card
// =====================================================

@Composable
private fun KdHomeWelcomeCard(
    words: KdHomeWords,
    modifier: Modifier = Modifier,
    expanded: Boolean = false
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Row(
            modifier = if (expanded) {
                Modifier
                    .fillMaxSize()
                    .padding(18.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            },

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(14.dp)
        ) {
            KdHomeIconTile(
                type = KdHomeIconType.CAR
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = words.welcomeLabel,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                Text(
                    text = words.welcomeTitle,
                    fontSize = 24.sp,
                    lineHeight = 31.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = words.welcomeDescription,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// =====================================================
// LEFT 2: Automatic Voice Guide
// =====================================================

@Composable
private fun KdHomeAutomaticCard(
    words: KdHomeWords,
    autoGuideEnabled: Boolean,
    onAutoGuideChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Row(
            modifier = if (expanded) {
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            },

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            KdHomeIconTile(
                type = KdHomeIconType.SPEAKER
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = words.automaticTitle,
                    fontSize = 20.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = words.automaticDescription,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = if (autoGuideEnabled) {
                        words.enabled
                    } else {
                        words.disabled
                    },

                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,

                    color = if (autoGuideEnabled) {
                        colors.primary
                    } else {
                        colors.onSurfaceVariant
                    }
                )
            }

            Switch(
                checked = autoGuideEnabled,
                onCheckedChange = onAutoGuideChange
            )
        }
    }
}

// =====================================================
// LEFT 3: Accident & Emergency
// =====================================================

@Composable
private fun KdHomeEmergencyCard(
    words: KdHomeWords,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.clickable(
            onClick = onClick
        ),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = colors.primary.copy(
                alpha = 0.11f
            )
        )
    ) {
        Row(
            modifier = if (expanded) {
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            },

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            KdHomeIconTile(
                type = KdHomeIconType.SHIELD
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = words.emergencyTitle,
                    fontSize = 21.sp,
                    lineHeight = 27.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = words.emergencyDescription,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = "›",
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primary
            )
        }
    }
}

// =====================================================
// RIGHT: Menu Cards
// =====================================================

@Composable
private fun KdHomeMenuCard(
    number: String,
    title: String,
    description: String,
    icon: KdHomeIconType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    activeLabel: String? = null
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.clickable(
            onClick = onClick
        ),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column(
            modifier = if (expanded) {
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 13.dp
                    )
            } else {
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 16.dp
                    )
            },

            verticalArrangement =
                Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = if (expanded) {
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                } else {
                    Modifier.fillMaxWidth()
                },

                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(14.dp)
            ) {
                KdHomeIconTile(
                    type = icon
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 23.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = description,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "›",
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    color = colors.primary
                )
            }

            // Small index at the bottom
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                Text(
                    text = "$number / 03",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.onSurfaceVariant
                )

                if (activeLabel != null) {
                    Text(
                        text = activeLabel,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                }
            }
        }
    }
}

// =====================================================
// Driving-restricted Home
//
// Preserve the simplified, non-interactive
// screen while driving.
// =====================================================

@Composable
private fun KdHomeRestrictedScreen(
    words: KdHomeWords,
    navigationStatus: String
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(24.dp),

        verticalArrangement =
            Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "K-DRIVE",
            style = MaterialTheme.typography.headlineLarge,
            color = colors.onBackground
        )

        Text(
            text = words.guidance,
            style = MaterialTheme.typography.titleLarge,
            color = colors.primary
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),

            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),

                verticalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = words.currentStatus,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface
                )

                Text(
                    text = kdriveSafeMessage(navigationStatus),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Text(
            text = words.restrictedMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onBackground,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// =====================================================
// Previews
// =====================================================

@Preview(
    name = "Home - Korean New Design",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdHomeNewKoreanPreview() {
    KDriveTheme(
        KDRIVE_THEME_PRESETS.first()
    ) {
        KDriveHomeScreen(
            navigationStatus = "Waiting for navigation",
            autoGuideEnabled = false,
            demoMode = false,
            onAutoGuideChange = {},
            onTrafficRulesClick = {},
            onDrivingStatusClick = {},
            onDemoClick = {},
            interactionRestricted = false,
            showBackButton = true,
            uiLanguage = "KO",
            onEmergencySpeakRequest = {},
            onEmergencyStopRequest = {}
        )
    }
}

@Preview(
    name = "Home - English New Design",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdHomeNewEnglishPreview() {
    KDriveTheme(
        KDRIVE_THEME_PRESETS.first()
    ) {
        KDriveHomeScreen(
            navigationStatus = "Waiting for navigation",
            autoGuideEnabled = false,
            demoMode = false,
            onAutoGuideChange = {},
            onTrafficRulesClick = {},
            onDrivingStatusClick = {},
            onDemoClick = {},
            interactionRestricted = false,
            showBackButton = true,
            uiLanguage = "EN"
        )
    }
}

@Preview(
    name = "Home - French New Design",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdHomeNewFrenchPreview() {
    KDriveTheme(
        KDRIVE_THEME_PRESETS.first()
    ) {
        KDriveHomeScreen(
            navigationStatus = "Waiting for navigation",
            autoGuideEnabled = false,
            demoMode = false,
            onAutoGuideChange = {},
            onTrafficRulesClick = {},
            onDrivingStatusClick = {},
            onDemoClick = {},
            interactionRestricted = false,
            showBackButton = true,
            uiLanguage = "FR"
        )
    }
}

@Preview(
    name = "Home - Spanish New Design",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdHomeNewSpanishPreview() {
    KDriveTheme(
        KDRIVE_THEME_PRESETS.first()
    ) {
        KDriveHomeScreen(
            navigationStatus = "Waiting for navigation",
            autoGuideEnabled = false,
            demoMode = false,
            onAutoGuideChange = {},
            onTrafficRulesClick = {},
            onDrivingStatusClick = {},
            onDemoClick = {},
            interactionRestricted = false,
            showBackButton = true,
            uiLanguage = "ES"
        )
    }
}

@Preview(
    name = "Home - Restricted",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdHomeNewRestrictedPreview() {
    KDriveTheme(
        KDRIVE_THEME_PRESETS.first()
    ) {
        KDriveHomeScreen(
            navigationStatus = "Route guidance active",
            autoGuideEnabled = true,
            demoMode = false,
            onAutoGuideChange = {},
            onTrafficRulesClick = {},
            onDrivingStatusClick = {},
            onDemoClick = {},
            interactionRestricted = true,
            showBackButton = false,
            uiLanguage = "KO"
        )
    }
}
