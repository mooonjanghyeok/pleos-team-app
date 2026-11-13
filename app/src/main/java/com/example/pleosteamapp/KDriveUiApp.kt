package com.example.pleosteamapp
import androidx.compose.material3.OutlinedButton

import android.content.Context
import android.content.SharedPreferences

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import java.util.Locale

/*
 * K-DRIVE GUIDE / PC2
 *
 * Pages:
 * 1. Welcome
 * 2. Korean Driving Culture
 * 3. Home
 * 4. Traffic Rules
 * 5. Driving Status
 * 6. Theme Settings
 *
 * The Emergency screen is opened from Home.
 *
 * PC1 provides:
 * - Real navigation status
 * - Automatic guide state
 * - Demo state
 * - TTS speak/stop callbacks
 * - Driving restriction state
 *
 * NEW:
 * PC1 TTS callbacks are forwarded to the Home
 * and then to Accident & Emergency.
 */

private const val KD_UI_PREFS = "kdrive_ui_colors_v2"
private const val KD_UI_LANG = "startup_language"

// ==================================================
// Language
// ==================================================

private fun kdUiDefaultLanguage(): String =
    when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
        "ko" -> "KO"
        "fr" -> "FR"
        "es" -> "ES"
        else -> "EN"
    }

private fun kdUiLanguage(value: String?): String =
    when (value) {
        "KO", "EN", "FR", "ES" -> value
        else -> kdUiDefaultLanguage()
    }

private fun kdUiBack(language: String): String =
    when (language) {
        "KO" -> "← 뒤로"
        "FR" -> "← Retour"
        "ES" -> "← Volver"
        else -> "← Back"
    }

private fun kdUiThemeLabel(language: String): String =
    when (language) {
        "KO" -> "테마"
        "FR" -> "Thème"
        "ES" -> "Tema"
        else -> "Theme"
    }

// ==================================================
// Theme settings
// ==================================================

private fun kdUiSavedPalette(
    prefs: SharedPreferences?
): KDrivePalette {
    val fallback = KDRIVE_CUSTOM_DEFAULT

    val bg = prefs?.getString(
        "bg",
        fallback.backgroundHex
    ) ?: fallback.backgroundHex

    val card = prefs?.getString(
        "card",
        fallback.cardHex
    ) ?: fallback.cardHex

    val accent = prefs?.getString(
        "accent",
        fallback.accentHex
    ) ?: fallback.accentHex

    val text = prefs?.getString(
        "text",
        fallback.textHex
    ) ?: fallback.textHex

    val candidate = KDrivePalette(
        "custom",
        "Custom",
        bg,
        card,
        accent,
        text,
        (kdriveParseHex(bg)?.luminance() ?: 0f) < 0.5f
    )

    return if (kdrivePaletteIsReadable(candidate)) {
        candidate
    } else {
        fallback
    }
}

// ==================================================
// Welcome translations
// ==================================================

private data class KdUiWelcomeWords(
    val title: String,
    val description: String,
    val wish: String,
    val start: String,
    val tagline: String
)

private fun kdUiWelcomeWords(language: String): KdUiWelcomeWords =
    when (language) {
        "KO" -> KdUiWelcomeWords(
            title = "한국에서의 여행을 환영해요!",
            description =
                "새로운 풍경과 설레는 순간들이 여러분을 기다리고 있어요.\n" +
                        "낯선 길에서도 K-DRIVE가 함께할게요.",
            wish =
                "안전하고 즐거운 여행 되세요!\n" +
                        "한국에서 좋은 추억 많이 만드세요.",
            start = "여행 시작하기",
            tagline = "여행의 시작"
        )

        "FR" -> KdUiWelcomeWords(
            title = "Bienvenue en Corée !",
            description =
                "De nouveaux paysages et de belles découvertes vous attendent.\n" +
                        "K-DRIVE vous accompagne sur la route.",
            wish =
                "Bon voyage en toute sécurité !\n" +
                        "Créez de beaux souvenirs en Corée.",
            start = "Commencer le voyage",
            tagline = "VOTRE VOYAGE COMMENCE ICI"
        )

        "ES" -> KdUiWelcomeWords(
            title = "¡Le damos la bienvenida a Corea!",
            description =
                "Nuevos paisajes y momentos inolvidables le esperan.\n" +
                        "K-DRIVE le acompaña en su viaje.",
            wish =
                "¡Le deseamos un viaje seguro y maravilloso!\n" +
                        "¡Disfrute de Corea!",
            start = "Comenzar el viaje",
            tagline = "SU VIAJE COMIENZA AQUÍ"
        )

        else -> KdUiWelcomeWords(
            title = "Welcome to Korea!",
            description =
                "New sights and memorable moments await you.\n" +
                        "K-DRIVE is here to accompany you.",
            wish =
                "Wishing you a safe and wonderful trip!\n" +
                        "Make beautiful memories in Korea.",
            start = "Start your journey",
            tagline = "YOUR JOURNEY STARTS HERE"
        )
    }

// ==================================================
// Culture translations
// ==================================================

private data class KdUiCultureWords(
    val title: String,
    val paragraphs: List<String>,
    val closing: String,
    val start: String
)

private fun kdUiCultureWords(language: String): KdUiCultureWords =
    when (language) {
        "KO" -> KdUiCultureWords(
            title = "한국에서 운전하기 전, 잠깐!",
            paragraphs = listOf(
                "한국에서는 다른 차가 양보해 주면 비상등을 짧게 켜 감사의 마음을 " +
                        "전하기도 해요. 이는 법적 의무가 아닌 운전 관행이에요.",
                "하지만 고속도로에서 앞차들이 비상등을 켠다면 정체나 위험을 " +
                        "알리는 신호일 수 있으니 속도를 줄여주세요.",
                "경적은 꼭 필요할 때만 사용하고, 교차로가 막혀 있다면 " +
                        "초록불이어도 기다려 주세요. 안전거리를 지키고 " +
                        "무리한 끼어들기를 피하는 작은 배려가 더 즐거운 여행을 만듭니다."
            ),
            closing =
                "서로를 배려하는 마음으로,\n" +
                        "안전하고 행복한 한국 여행을 시작해 보세요!",
            start = "확인하고 시작하기  →"
        )

        "FR" -> KdUiCultureWords(
            title = "Avant de conduire en Corée…",
            paragraphs = listOf(
                "En Corée, certains conducteurs allument brièvement leurs " +
                        "feux de détresse pour remercier un conducteur qui les a laissés passer. " +
                        "Il s’agit d’un usage, pas d’une obligation.",
                "Sur autoroute, les feux de détresse des véhicules devant " +
                        "vous peuvent signaler un embouteillage ou un danger. Ralentissez.",
                "Utilisez le klaxon seulement si nécessaire. Même au vert, " +
                        "attendez si le carrefour est bloqué. Gardez vos distances " +
                        "et évitez les insertions brusques."
            ),
            closing =
                "Avec prudence et respect,\n" +
                        "profitez pleinement de votre voyage en Corée !",
            start = "Compris, commencer  →"
        )

        "ES" -> KdUiCultureWords(
            title = "Antes de conducir en Corea…",
            paragraphs = listOf(
                "En Corea, algunos conductores encienden brevemente las " +
                        "luces de emergencia para agradecer que les cedan el paso. " +
                        "Es una costumbre, no una obligación.",
                "En autopista, las luces de emergencia de los vehículos de " +
                        "delante pueden advertir de retenciones o peligro. " +
                        "Reduzca la velocidad.",
                "Use el claxon solo cuando sea necesario. Aunque el semáforo " +
                        "esté en verde, espere si el cruce está bloqueado. " +
                        "Mantenga la distancia y evite incorporaciones bruscas."
            ),
            closing =
                "Con prudencia y respeto,\n" +
                        "¡disfrute de un viaje maravilloso por Corea!",
            start = "Entendido, comenzar  →"
        )

        else -> KdUiCultureWords(
            title = "Before you drive in Korea…",
            paragraphs = listOf(
                "In Korea, some drivers briefly flash their hazard lights " +
                        "to say thanks when another driver yields. " +
                        "It's a custom, not a legal requirement.",
                "On highways, hazard lights ahead may warn of congestion " +
                        "or danger. Slow down safely.",
                "Use the horn only when necessary. Even on green, wait if " +
                        "the intersection is blocked. Keep a safe distance " +
                        "and avoid sudden merging."
            ),
            closing =
                "Drive with care and respect,\n" +
                        "and enjoy a safe journey in Korea!",
            start = "Got it, let's start  →"
        )
    }

// ==================================================
// Progress bar
// ==================================================

@Composable
private fun KdUiProgress(
    current: Int,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(3) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(5.dp)
                    .background(
                        if (index < current) {
                            colors.primary
                        } else {
                            colors.onBackground.copy(alpha = 0.22f)
                        },
                        RoundedCornerShape(4.dp)
                    )
            )
        }
    }
}

// ==================================================
// Welcome: front-facing car illustration
// ==================================================

private fun DrawScope.kdUiDrawFrontCar(
    centerX: Float,
    bottom: Float,
    width: Float,
    accent: Color,
    window: Color,
    tire: Color
) {
    val w = width
    val h = w * 0.64f
    val x = centerX - w / 2f
    val y = bottom - h

    drawOval(
        color = tire.copy(alpha = 0.12f),
        topLeft = Offset(
            x - w * 0.06f,
            bottom - h * 0.12f
        ),
        size = Size(
            w * 1.12f,
            h * 0.18f
        )
    )

    listOf(0.11f, 0.74f).forEach { part ->
        drawRoundRect(
            color = tire,
            topLeft = Offset(
                x + w * part,
                y + h * 0.64f
            ),
            size = Size(
                w * 0.15f,
                h * 0.32f
            ),
            cornerRadius = CornerRadius(w * 0.045f)
        )
    }

    val roof = Path().apply {
        moveTo(x + w * 0.17f, y + h * 0.45f)
        lineTo(x + w * 0.27f, y + h * 0.10f)
        quadraticTo(
            x + w * 0.30f,
            y + h * 0.03f,
            x + w * 0.37f,
            y + h * 0.03f
        )
        lineTo(x + w * 0.63f, y + h * 0.03f)
        quadraticTo(
            x + w * 0.70f,
            y + h * 0.03f,
            x + w * 0.73f,
            y + h * 0.10f
        )
        lineTo(x + w * 0.83f, y + h * 0.45f)
        close()
    }

    drawPath(roof, accent)

    val glass = Path().apply {
        moveTo(x + w * 0.31f, y + h * 0.13f)
        lineTo(x + w * 0.69f, y + h * 0.13f)
        lineTo(x + w * 0.76f, y + h * 0.38f)
        lineTo(x + w * 0.24f, y + h * 0.38f)
        close()
    }

    drawPath(glass, window)

    listOf(0.01f, 0.88f).forEach { part ->
        drawRoundRect(
            color = accent,
            topLeft = Offset(
                x + w * part,
                y + h * 0.37f
            ),
            size = Size(
                w * 0.11f,
                h * 0.13f
            ),
            cornerRadius = CornerRadius(w * 0.035f)
        )
    }

    drawRoundRect(
        color = accent,
        topLeft = Offset(
            x + w * 0.06f,
            y + h * 0.39f
        ),
        size = Size(
            w * 0.88f,
            h * 0.44f
        ),
        cornerRadius = CornerRadius(w * 0.105f)
    )

    listOf(0.15f, 0.68f).forEach { part ->
        drawRoundRect(
            color = window,
            topLeft = Offset(
                x + w * part,
                y + h * 0.51f
            ),
            size = Size(
                w * 0.17f,
                h * 0.12f
            ),
            cornerRadius = CornerRadius(w * 0.03f)
        )
    }

    drawRoundRect(
        color = window,
        topLeft = Offset(
            x + w * 0.38f,
            y + h * 0.68f
        ),
        size = Size(
            w * 0.24f,
            h * 0.07f
        ),
        cornerRadius = CornerRadius(w * 0.02f)
    )
}

@Composable
private fun KdUiGlobeCar(
    compact: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val progress = remember { Animatable(0f) }

    // Plays once when Welcome is composed.
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 4200,
                easing = FastOutSlowInEasing
            )
        )
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 132.dp else 164.dp)
            .clipToBounds()
            .clearAndSetSemantics { }
    ) {
        val w = size.width
        val h = size.height

        if (w <= 0f || h <= 0f) return@Canvas

        val p = progress.value.coerceIn(0f, 1f)
        val centerX = w / 2f

        val radius = minOf(
            h * 0.235f,
            w * 0.16f
        )

        val globe = Offset(
            centerX,
            h * 0.04f + radius
        )

        // KDRIVE_WELCOME_GLOBE_REMOVED_V1
        // Globe drawing removed. Original road/car retained.
        val horizon = globe.y + radius * 0.76f
        val roadBottom = h * 0.95f
        val nearHalf = minOf(w * 0.30f, h * 0.72f)
        val farHalf = radius * 0.18f

        val road = Path().apply {
            moveTo(centerX - farHalf, horizon)
            lineTo(centerX + farHalf, horizon)
            lineTo(centerX + nearHalf, roadBottom)
            lineTo(centerX - nearHalf, roadBottom)
            close()
        }

        drawPath(
            road,
            colors.primary.copy(alpha = 0.07f)
        )

        listOf(-1f, 1f).forEach { direction ->
            drawLine(
                color = colors.primary.copy(alpha = 0.32f),
                start = Offset(
                    centerX + direction * farHalf,
                    horizon
                ),
                end = Offset(
                    centerX + direction * nearHalf,
                    roadBottom
                ),
                strokeWidth = 1.6.dp.toPx()
            )
        }

        repeat(4) { i ->
            val depth = (i + 1) / 5f
            val y = horizon + (roadBottom - horizon) * depth
            val dashLength = h * (
                    0.025f + depth * 0.035f
                    )

            drawLine(
                color = colors.primary.copy(alpha = 0.32f),
                start = Offset(centerX, y),
                end = Offset(
                    centerX,
                    minOf(y + dashLength, roadBottom)
                ),
                strokeWidth =
                    1.5.dp.toPx() + depth * 1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        val maxCarWidth = minOf(
            120.dp.toPx(),
            h * 0.62f,
            w * 0.30f
        )

        kdUiDrawFrontCar(
            centerX = centerX,
            bottom = horizon + (roadBottom - horizon) * p,
            width = maxCarWidth * (0.22f + 0.78f * p),
            accent = colors.primary,
            window = colors.onPrimary,
            tire = colors.onSurface
        )
    }
}

// ==================================================
// Culture icons
// ==================================================

@Composable
private fun KdUiCultureIcon(
    type: String,
    modifier: Modifier = Modifier
) {
    val color = MaterialTheme.colorScheme.primary

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val line = 2.7.dp.toPx()

        when (type) {
            "car" -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * 0.14f, h * 0.40f),
                    size = Size(w * 0.72f, h * 0.38f),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(line)
                )

                val roof = Path().apply {
                    moveTo(w * 0.26f, h * 0.40f)
                    lineTo(w * 0.37f, h * 0.24f)
                    lineTo(w * 0.63f, h * 0.24f)
                    lineTo(w * 0.74f, h * 0.40f)
                }

                drawPath(
                    roof,
                    color,
                    style = Stroke(line, cap = StrokeCap.Round)
                )

                drawCircle(
                    color,
                    w * 0.065f,
                    Offset(w * 0.28f, h * 0.78f)
                )

                drawCircle(
                    color,
                    w * 0.065f,
                    Offset(w * 0.72f, h * 0.78f)
                )
            }

            "warning" -> {
                val triangle = Path().apply {
                    moveTo(w * 0.50f, h * 0.16f)
                    lineTo(w * 0.88f, h * 0.78f)
                    quadraticTo(
                        w * 0.90f,
                        h * 0.86f,
                        w * 0.80f,
                        h * 0.86f
                    )
                    lineTo(w * 0.20f, h * 0.86f)
                    quadraticTo(
                        w * 0.10f,
                        h * 0.86f,
                        w * 0.12f,
                        h * 0.78f
                    )
                    close()
                }

                drawPath(
                    triangle,
                    color,
                    style = Stroke(
                        line,
                        cap = StrokeCap.Round
                    )
                )

                drawLine(
                    color,
                    Offset(w * 0.50f, h * 0.39f),
                    Offset(w * 0.50f, h * 0.62f),
                    line
                )

                drawCircle(
                    color,
                    line * 0.60f,
                    Offset(w * 0.50f, h * 0.73f)
                )
            }

            "shield" -> {
                val shield = Path().apply {
                    moveTo(w * 0.50f, h * 0.12f)
                    lineTo(w * 0.82f, h * 0.25f)
                    lineTo(w * 0.80f, h * 0.57f)
                    cubicTo(
                        w * 0.78f,
                        h * 0.75f,
                        w * 0.64f,
                        h * 0.87f,
                        w * 0.50f,
                        h * 0.94f
                    )
                    cubicTo(
                        w * 0.36f,
                        h * 0.87f,
                        w * 0.22f,
                        h * 0.75f,
                        w * 0.20f,
                        h * 0.57f
                    )
                    lineTo(w * 0.18f, h * 0.25f)
                    close()
                }

                drawPath(
                    shield,
                    color,
                    style = Stroke(line, cap = StrokeCap.Round)
                )

                val check = Path().apply {
                    moveTo(w * 0.33f, h * 0.53f)
                    lineTo(w * 0.46f, h * 0.66f)
                    lineTo(w * 0.70f, h * 0.39f)
                }

                drawPath(
                    check,
                    color,
                    style = Stroke(
                        line * 1.15f,
                        cap = StrokeCap.Round
                    )
                )
            }
        }
    }
}

// ==================================================
// Culture road animation
// ==================================================

@Composable
private fun KdUiCultureRoad(
    compact: Boolean
) {
    val colors = MaterialTheme.colorScheme

    val transition = rememberInfiniteTransition(
        label = "CultureRoad"
    )

    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 6000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "CultureCar"
    )

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 72.dp else 92.dp)
            .clipToBounds()
            .clearAndSetSemantics { }
    ) {
        val w = size.width
        val h = size.height

        val left = 8.dp.toPx()
        val right = w - left

        if (right <= left) return@Canvas

        val roadTop = h * 0.25f
        val roadBottom = h * 0.91f
        val roadHeight = roadBottom - roadTop

        drawRoundRect(
            color = colors.primary.copy(alpha = 0.09f),
            topLeft = Offset(left, roadTop),
            size = Size(right - left, roadHeight),
            cornerRadius = CornerRadius(14.dp.toPx())
        )

        listOf(roadTop, roadBottom).forEach { y ->
            drawLine(
                color = colors.onSurface.copy(alpha = 0.26f),
                start = Offset(left, y),
                end = Offset(right, y),
                strokeWidth = 1.4.dp.toPx()
            )
        }

        drawLine(
            color = colors.primary.copy(alpha = 0.55f),
            start = Offset(
                left + 12.dp.toPx(),
                roadTop + roadHeight * 0.72f
            ),
            end = Offset(
                right - 12.dp.toPx(),
                roadTop + roadHeight * 0.72f
            ),
            strokeWidth = 2.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(
                    24.dp.toPx(),
                    18.dp.toPx()
                )
            )
        )

        val carWidth = if (compact) {
            50.dp.toPx()
        } else {
            67.dp.toPx()
        }

        val carHeight = carWidth * 0.48f

        val x = -carWidth +
                (w + carWidth * 2f) * progress.value

        val y = roadTop + roadHeight * 0.31f

        repeat(3) { i ->
            val lineY = y + carHeight * (
                    0.28f + i * 0.22f
                    )

            drawLine(
                color = colors.primary.copy(alpha = 0.43f),
                start = Offset(
                    x - carWidth * 0.34f,
                    lineY
                ),
                end = Offset(
                    x - carWidth * 0.10f,
                    lineY
                ),
                strokeWidth = 1.7.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        drawRoundRect(
            color = colors.primary,
            topLeft = Offset(
                x,
                y + carHeight * 0.35f
            ),
            size = Size(
                carWidth,
                carHeight * 0.49f
            ),
            cornerRadius = CornerRadius(7.dp.toPx())
        )

        val roof = Path().apply {
            moveTo(
                x + carWidth * 0.20f,
                y + carHeight * 0.35f
            )
            lineTo(
                x + carWidth * 0.34f,
                y + carHeight * 0.05f
            )
            lineTo(
                x + carWidth * 0.69f,
                y + carHeight * 0.05f
            )
            lineTo(
                x + carWidth * 0.83f,
                y + carHeight * 0.35f
            )
            close()
        }

        drawPath(roof, colors.primary)

        val window = Path().apply {
            moveTo(
                x + carWidth * 0.37f,
                y + carHeight * 0.13f
            )
            lineTo(
                x + carWidth * 0.50f,
                y + carHeight * 0.13f
            )
            lineTo(
                x + carWidth * 0.50f,
                y + carHeight * 0.31f
            )
            lineTo(
                x + carWidth * 0.27f,
                y + carHeight * 0.31f
            )
            close()
        }

        drawPath(window, colors.surface)

        drawRoundRect(
            color = colors.surface,
            topLeft = Offset(
                x + carWidth * 0.54f,
                y + carHeight * 0.13f
            ),
            size = Size(
                carWidth * 0.16f,
                carHeight * 0.18f
            ),
            cornerRadius = CornerRadius(2.dp.toPx())
        )

        listOf(0.24f, 0.76f).forEach { fraction ->
            val wheel = Offset(
                x + carWidth * fraction,
                y + carHeight * 0.84f
            )

            drawCircle(
                color = colors.onSurface,
                radius = carWidth * 0.09f,
                center = wheel
            )

            drawCircle(
                color = colors.surface,
                radius = carWidth * 0.038f,
                center = wheel
            )
        }

        drawRoundRect(
            color = colors.onPrimary,
            topLeft = Offset(
                x + carWidth * 0.93f,
                y + carHeight * 0.50f
            ),
            size = Size(
                carWidth * 0.06f,
                carHeight * 0.12f
            ),
            cornerRadius = CornerRadius(2.dp.toPx())
        )
    }
}

// ==================================================
// Page 1: Welcome
// ==================================================

@Composable
private fun KdUiWelcome(
    language: String,
    onLanguageChange: (String) -> Unit,
    onTheme: () -> Unit,
    onContinue: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val words = kdUiWelcomeWords(language)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val narrow = maxWidth < 700.dp
        val shallow = maxHeight < 680.dp
        val columns = if (narrow) 2 else 4

        val titleSize = when {
            narrow -> 31.sp
            shallow -> 38.sp
            else -> 40.sp
        }

        val bodySize = when {
            narrow -> 22.sp
            shallow -> 25.sp
            else -> 27.sp
        }

        val wishSize = when {
            narrow -> 24.sp
            shallow -> 27.sp
            else -> 29.sp
        }

        val languages = listOf(
            "KO" to "한국어",
            "EN" to "English",
            "FR" to "Français",
            "ES" to "Español"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (shallow) 12.dp else 20.dp,
                    vertical = if (shallow) 10.dp else 16.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            if (narrow) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "K-DRIVE GUIDE",
                        modifier = Modifier.weight(1f),
                        color = colors.primary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    KDriveActionButton(
                        label = kdUiThemeLabel(language),
                        onClick = onTheme,
                        filled = false
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "K-DRIVE GUIDE",
                        color = colors.primary,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier.align(
                            Alignment.CenterEnd
                        )
                    ) {
                        KDriveActionButton(
                            label = kdUiThemeLabel(language),
                            onClick = onTheme,
                            filled = false
                        )
                    }
                }
            }

            Text(
                text = "SELECT LANGUAGE / 언어 선택",
                color = colors.onBackground,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center
            )

            // Four languages
            languages.chunked(columns).forEach { chunk ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    chunk.forEach { (id, name) ->
                        val selected = language == id

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(
                                    min = KDriveUiSize.touchHeight
                                )
                                .clickable {
                                    onLanguageChange(id)
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (selected) {
                                    colors.primary
                                } else {
                                    colors.surface
                                }
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(
                                        min = KDriveUiSize.touchHeight
                                    )
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    color = if (selected) {
                                        colors.onPrimary
                                    } else {
                                        colors.onSurface
                                    }
                                )
                            }
                        }
                    }

                    repeat(columns - chunk.size) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KdUiProgress(
                    current = 1,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "01 / 03",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onBackground.copy(alpha = 0.72f)
                )
            }

            // Welcome card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                )
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val cardHeight = maxHeight

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = cardHeight)
                                .padding(
                                    horizontal = if (narrow) {
                                        16.dp
                                    } else {
                                        28.dp
                                    },
                                    vertical = if (shallow) {
                                        12.dp
                                    } else {
                                        24.dp
                                    }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Text(
                                text = words.tagline,
                                color = colors.primary,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center
                            )

                            KdUiGlobeCar(
                                compact = narrow || shallow
                            )

                            Text(
                                text = words.title,
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.primary,
                                fontSize = titleSize,
                                lineHeight = when {
                                    narrow -> 40.sp
                                    shallow -> 47.sp
                                    else -> 50.sp
                                },
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = words.description,
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.onSurface,
                                fontSize = bodySize,
                                lineHeight = when {
                                    narrow -> 31.sp
                                    shallow -> 35.sp
                                    else -> 39.sp
                                },
                                textAlign = TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .width(110.dp)
                                    .height(2.dp)
                                    .background(
                                        colors.primary.copy(alpha = 0.65f)
                                    )
                            )

                            Text(
                                text = words.wish,
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.onSurface,
                                fontSize = wishSize,
                                lineHeight = when {
                                    narrow -> 33.sp
                                    shallow -> 37.sp
                                    else -> 41.sp
                                },
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            KDriveActionButton(
                label = words.start,
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==================================================
// Page 2: Culture
// ==================================================

@Composable
private fun KdUiCulture(
    language: String,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val words = kdUiCultureWords(language)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val compact = maxHeight < 680.dp || maxWidth < 600.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = if (compact) 12.dp else 20.dp,
                    vertical = if (compact) 10.dp else 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(
                if (compact) 9.dp else 14.dp
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                KdUiCultureIcon(
                    "car",
                    Modifier.size(28.dp)
                )

                Text(
                    text = "K-DRIVE GUIDE",
                    modifier = Modifier.weight(1f),
                    color = colors.primary,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "02 / 03",
                    color = colors.onBackground.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium
                )

                KDriveActionButton(
                    label = kdUiBack(language),
                    onClick = onBack,
                    filled = false
                )
            }

            KdUiProgress(
                current = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                )
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val cardHeight = maxHeight

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = cardHeight)
                                .padding(
                                    horizontal = if (compact) {
                                        14.dp
                                    } else {
                                        26.dp
                                    },
                                    vertical = if (compact) {
                                        12.dp
                                    } else {
                                        22.dp
                                    }
                                ),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(
                                    if (compact) 28.dp else 44.dp
                                ),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val iconSize = if (compact) {
                                    56.dp
                                } else {
                                    68.dp
                                }

                                KdUiCultureIcon(
                                    "car",
                                    Modifier.size(iconSize)
                                )

                                KdUiCultureIcon(
                                    "warning",
                                    Modifier.size(iconSize)
                                )

                                KdUiCultureIcon(
                                    "shield",
                                    Modifier.size(iconSize)
                                )
                            }

                            Spacer(Modifier.height(5.dp))

                            Text(
                                text = words.title,
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.primary,
                                fontSize = if (compact) {
                                    27.sp
                                } else {
                                    34.sp
                                },
                                lineHeight = if (compact) {
                                    34.sp
                                } else {
                                    42.sp
                                },
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            words.paragraphs.forEach { paragraph ->
                                Text(
                                    text = paragraph,
                                    modifier = Modifier.fillMaxWidth(),
                                    color = colors.onSurface,
                                    fontSize = if (compact) {
                                        19.sp
                                    } else {
                                        23.sp
                                    },
                                    lineHeight = if (compact) {
                                        28.sp
                                    } else {
                                        34.sp
                                    },
                                    textAlign = TextAlign.Center
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(
                                        if (compact) 150.dp else 220.dp
                                    )
                                    .height(2.dp)
                                    .background(
                                        colors.onSurface.copy(
                                            alpha = 0.24f
                                        )
                                    )
                            )

                            Text(
                                text = words.closing,
                                modifier = Modifier.fillMaxWidth(),
                                color = colors.primary,
                                fontSize = if (compact) {
                                    23.sp
                                } else {
                                    28.sp
                                },
                                lineHeight = if (compact) {
                                    32.sp
                                } else {
                                    39.sp
                                },
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )

                            KdUiCultureRoad(
                                compact = compact
                            )
                        }
                    }
                }
            }

            KDriveActionButton(
                label = words.start,
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==================================================
// Router / PC1 bridge
// ==================================================

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
    val inspection = LocalInspectionMode.current
    val systemDark = isSystemInDarkTheme()

    val prefs = remember(context, inspection) {
        if (inspection) {
            null
        } else {
            context.applicationContext
                .getSharedPreferences(
                    KD_UI_PREFS,
                    Context.MODE_PRIVATE
                )
        }
    }

    var themeId by remember(prefs) {
        mutableStateOf(
            prefs?.getString(
                "theme",
                KDRIVE_DEFAULT_THEME
            ) ?: KDRIVE_DEFAULT_THEME
        )
    }

    var custom by remember(prefs) {
        mutableStateOf(kdUiSavedPalette(prefs))
    }

    var language by remember(prefs) {
        mutableStateOf(
            kdUiLanguage(
                prefs?.getString(
                    KD_UI_LANG,
                    kdUiDefaultLanguage()
                )
            )
        )
    }

    var page by rememberSaveable {
        mutableStateOf("WELCOME")
    }

    var backStack by rememberSaveable {
        mutableStateOf(listOf<String>())
    }

    val navigateTo: (String) -> Unit = { destination ->
        if (!isDrivingRestricted && destination != page) {
            backStack = backStack + page
            page = destination
        }
    }

    val navigateBack: () -> Unit = {
        if (!isDrivingRestricted && backStack.isNotEmpty()) {
            if (page == "RULES") {
                onStopVoiceRequest()
            }

            page = backStack.last()
            backStack = backStack.dropLast(1)
        }
    }

    LaunchedEffect(isDrivingRestricted) {
        if (isDrivingRestricted) {
            if (page == "RULES") {
                onStopVoiceRequest()
            }

            backStack = emptyList()
            page = "HOME"
        }
    }

    BackHandler(
        enabled = !isDrivingRestricted &&
                backStack.isNotEmpty()
    ) {
        navigateBack()
    }

    KDriveTheme(
        kdriveResolvePalette(
            themeId,
            custom,
            systemDark
        )
    ) {
        when {
            // Driving-restricted Home
            isDrivingRestricted -> KDriveHomeScreen(
                navigationStatus =
                    if (state.source == KDriveUiDataSource.REAL) {
                        state.guidanceTitle
                            ?: state.navigationStatus
                    } else {
                        "Navigation guidance unavailable"
                    },
                autoGuideEnabled = state.autoGuideEnabled,
                demoMode = false,
                onAutoGuideChange = onAutoGuideChange,
                onTrafficRulesClick = {},
                onDrivingStatusClick = {},
                onDemoClick = {},
                interactionRestricted = true,
                showBackButton = false,
                uiLanguage = language
            )

            // Welcome
            page == "WELCOME" -> KdUiWelcome(
                language = language,
                onLanguageChange = { next ->
                    language = kdUiLanguage(next)

                    prefs?.edit()
                        ?.putString(KD_UI_LANG, language)
                        ?.apply()
                },
                onTheme = {
                    navigateTo("THEME")
                },
                onContinue = {
                    navigateTo("CULTURE")
                }
            )

            // Korean driving culture
            page == "CULTURE" -> KdUiCulture(
                language = language,
                onBack = navigateBack,
                onContinue = {
                    navigateTo("HOME")
                }
            )

            // Existing Traffic Rules
            page == "RULES" -> TrafficRulesScreen(
                onBack = navigateBack,
                onSpeakRequest = onSpeakRequest,
                onStopRequest = onStopVoiceRequest,
                interactionRestricted = false
            )

            // Existing Driving Status
            page == "STATUS" -> DrivingStatusScreen(
                state = state,
                onBack = navigateBack,
                onAutoGuideChange = onAutoGuideChange,
                onDemoStartRequest = onDemoStartRequest,
                onDemoStopRequest = onDemoStopRequest,
                onReplayRequest = onReplayRequest,
                onStopVoiceRequest = onStopVoiceRequest,
                interactionRestricted = false
            )

            // Existing Theme Settings
            page == "THEME" -> KDriveThemeSettingsScreen(
                selectedThemeId = themeId,
                customPalette = custom,
                onThemeSelected = { chosen ->
                    themeId = chosen

                    prefs?.edit()
                        ?.putString("theme", chosen)
                        ?.apply()
                },
                onSaveCustom = { chosen ->
                    if (kdrivePaletteIsReadable(chosen)) {
                        custom = chosen
                        themeId = "custom"

                        prefs?.edit()
                            ?.putString("theme", "custom")
                            ?.putString(
                                "bg",
                                chosen.backgroundHex
                            )
                            ?.putString(
                                "card",
                                chosen.cardHex
                            )
                            ?.putString(
                                "accent",
                                chosen.accentHex
                            )
                            ?.putString(
                                "text",
                                chosen.textHex
                            )
                            ?.apply()
                    }
                },
                onReset = {
                    themeId = KDRIVE_DEFAULT_THEME
                    custom = KDRIVE_CUSTOM_DEFAULT

                    prefs?.edit()
                        ?.putString(
                            "theme",
                            KDRIVE_DEFAULT_THEME
                        )
                        ?.remove("bg")
                        ?.remove("card")
                        ?.remove("accent")
                        ?.remove("text")
                        ?.apply()
                },
                onBack = navigateBack,
                interactionRestricted = false,
                backLabel = kdUiBack(language)
            )

            // Normal Home
            else -> Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // KDRIVE_WELCOME_THEME_NAV_V1
                // Parked-only navigation; restricted branch remains first.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            backStack = emptyList()
                            page = "WELCOME"
                        }
                    ) {
                        Text(
                            when (language) {
                                "KO" -> "언어 / 시작 화면"
                                "FR" -> "Langue / Accueil"
                                "ES" -> "Idioma / Inicio"
                                else -> "Language / Start screen"
                            }
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    OutlinedButton(
                        onClick = {
                            backStack = listOf("HOME")
                            page = "THEME"
                        }
                    ) {
                        Text(kdUiThemeLabel(language))
                    }
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
KDriveHomeScreen(
                navigationStatus = state.navigationStatus,
                autoGuideEnabled = state.autoGuideEnabled,
                demoMode =
                    state.source == KDriveUiDataSource.DEMO,
                onAutoGuideChange = onAutoGuideChange,

                onTrafficRulesClick = {
                    navigateTo("RULES")
                },
                onDrivingStatusClick = {
                    navigateTo("STATUS")
                },
                onDemoClick = {
                    onDemoStartRequest()
                    navigateTo("STATUS")
                },

                onThemeSettingsClick = { page = "THEME" },

                interactionRestricted = false,
                onBackClick = navigateBack,
                showBackButton = backStack.isNotEmpty(),
                backLabel = kdUiBack(language),
                uiLanguage = language,

                // =====================================
                // NEW: ACTUAL TTS CALLBACK CONNECTION
                // =====================================

                onEmergencySpeakRequest = onSpeakRequest,
                onEmergencyStopRequest = onStopVoiceRequest
            )
                }
            }
        }
    }
}

// ==================================================
// Interactive preview harness
// ==================================================

@Composable
private fun KdUiPreviewHarness(restricted: Boolean) {
    var state by remember {
        mutableStateOf(
            KDriveDrivingStatusUiState()
        )
    }

    var lastSpeech by remember {
        mutableStateOf("")
    }

    KDriveUiApp(
        state = state,
        onAutoGuideChange = {
            state = state.copy(autoGuideEnabled = it)
        },
        onDemoStartRequest = {
            state = state.copy(
                source = KDriveUiDataSource.DEMO,
                navigationStatus = "Simulated route",
                demoRunning = true,
                placeName = "Sample Tollgate",
                distanceMeters = 500.0,
                guidanceTitle = "Tollgate ahead",
                guidanceText = "Check the payment lane signs.",
                voiceStatus = "Preview only"
            )
        },
        onDemoStopRequest = {
            state = state.copy(
                source = KDriveUiDataSource.NO_DATA,
                navigationStatus = "Simulation stopped",
                demoRunning = false,
                placeName = null,
                distanceMeters = null,
                guidanceTitle = null,
                guidanceText = null
            )
        },
        onSpeakRequest = { speech ->
            lastSpeech = speech

            // Preview records the request.
            // It does NOT synthesize audio.
            state = state.copy(
                guidanceTitle = "Manual speech",
                guidanceText = speech,
                voiceStatus = "TTS requested (preview)"
            )
        },
        onStopVoiceRequest = {
            state = state.copy(
                voiceStatus = "Stop requested (preview)"
            )
        },
        onReplayRequest = {
            state = state.copy(
                voiceStatus =
                    if (lastSpeech.isNotBlank()) {
                        "Replay requested (preview)"
                    } else {
                        "No saved message"
                    }
            )
        },
        isDrivingRestricted = restricted
    )
}

@Preview(
    name = "Welcome - Globe Car",
    widthDp = 1000,
    heightDp = 800,
    showBackground = true
)
@Composable
private fun KdUiWelcomePreview() {
    KDriveTheme(KDRIVE_THEME_PRESETS.first()) {
        KdUiWelcome(
            language = "KO",
            onLanguageChange = {},
            onTheme = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "Culture - Moving Road Car",
    widthDp = 1000,
    heightDp = 800,
    showBackground = true
)
@Composable
private fun KdUiCulturePreview() {
    KDriveTheme(KDRIVE_THEME_PRESETS.first()) {
        KdUiCulture(
            language = "KO",
            onBack = {},
            onContinue = {}
        )
    }
}

@Preview(
    name = "K-DRIVE Parked Landscape",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdUiParkedPreview() {
    KdUiPreviewHarness(false)
}

@Preview(
    name = "K-DRIVE Parked Portrait",
    widthDp = 400,
    heightDp = 800,
    showBackground = true
)
@Composable
private fun KdUiPortraitPreview() {
    KdUiPreviewHarness(false)
}

@Preview(
    name = "K-DRIVE Driving Restricted",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdUiRestrictedPreview() {
    KdUiPreviewHarness(true)
}