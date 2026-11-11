package com.example.pleosteamapp

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

data class KDrivePalette(
    val id: String,
    val name: String,
    val backgroundHex: String,
    val cardHex: String,
    val accentHex: String,
    val textHex: String,
    val dark: Boolean
)

const val KDRIVE_DEFAULT_THEME = "midnight"

// Preset IDs remain stable; removed selections fall back to Midnight.
val KDRIVE_THEME_PRESETS = listOf(
    KDrivePalette("midnight", "Midnight", "#0B1726", "#1A3046", "#6EE7C4", "#FFFFFF", true),
    KDrivePalette("graphite", "Graphite", "#15191F", "#292F38", "#F5BD69", "#FFFFFF", true),
    KDrivePalette("snow", "Snow White", "#EFF4F9", "#FFFFFF", "#2369C9", "#152536", false),
    KDrivePalette("purple", "Royal Purple", "#191630", "#302A51", "#C3A4FF", "#FFFFFF", true),
    KDrivePalette("amber", "Amber", "#271D13", "#43301D", "#FFD173", "#FFFFFF", true),

    // Selected newer palettes retained for visual variety.
    KDrivePalette("sage", "Sage Studio", "#E8EFE6", "#FFFFFF", "#286B45", "#1D3327", false),
    KDrivePalette("lilac", "Lilac Paper", "#F3ECFF", "#FFFFFF", "#6740A6", "#282039", false),
    KDrivePalette("rose", "Rose Quartz", "#FFF0F4", "#FFFFFF", "#9F2851", "#382030", false),
    KDrivePalette("moss", "Moss Forest", "#101E18", "#283B2A", "#C9F27A", "#FFFFFF", true),
    KDrivePalette("plum", "Plum Wine", "#251524", "#452840", "#FFC0D7", "#FFFFFF", true)
)

// Retained for compatibility with existing KDriveUiApp.kt saved preferences.
val KDRIVE_CUSTOM_DEFAULT =
    KDRIVE_THEME_PRESETS.first().copy(id = "custom", name = "Custom")

fun kdriveNormalizeHex(raw: String): String =
    "#" + raw.trim().removePrefix("#").uppercase(Locale.US)

fun kdriveParseHex(raw: String): Color? {
    val hex = kdriveNormalizeHex(raw)
    if (!Regex("^#[0-9A-F]{6}$").matches(hex)) return null
    return try {
        Color(AndroidColor.parseColor(hex))
    } catch (_: Exception) {
        null
    }
}

fun kdriveContrastRatio(first: Color, second: Color): Double {
    val a = first.luminance().toDouble()
    val b = second.luminance().toDouble()
    return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
}

fun kdrivePaletteIsReadable(palette: KDrivePalette): Boolean {
    val bg = kdriveParseHex(palette.backgroundHex) ?: return false
    val card = kdriveParseHex(palette.cardHex) ?: return false
    val accent = kdriveParseHex(palette.accentHex) ?: return false
    val text = kdriveParseHex(palette.textHex) ?: return false
    return kdriveContrastRatio(text, bg) >= 4.5 &&
            kdriveContrastRatio(text, card) >= 4.5 &&
            kdriveContrastRatio(accent, bg) >= 4.5 &&
            kdriveContrastRatio(accent, card) >= 4.5
}

fun kdriveResolvePalette(
    id: String,
    custom: KDrivePalette,
    systemDark: Boolean
): KDrivePalette {
    val chosen = when (id) {
        "system" -> KDRIVE_THEME_PRESETS.first {
            it.id == if (systemDark) "midnight" else "snow"
        }
        // Backward compatibility for older saved custom themes.
        "custom" -> custom
        else -> KDRIVE_THEME_PRESETS.firstOrNull { it.id == id }
            ?: KDRIVE_THEME_PRESETS.first()
    }
    return if (kdrivePaletteIsReadable(chosen)) chosen else KDRIVE_THEME_PRESETS.first()
}

fun kdriveSafeMessage(value: String?, maxChars: Int = 120): String {
    if (value.isNullOrBlank()) return "No current guidance"
    val limited = value.trim().split(Regex("\\s+")).take(24).joinToString(" ")
    return if (limited.length <= maxChars) limited
    else limited.take(maxChars - 1).trimEnd() + "…"
}

// Project UI choice, not a claimed official Pleos dp requirement.
object KDriveUiSize {
    val touchHeight = 72.dp
    val actionGap = 12.dp
    val padding = 18.dp
}

@Composable
fun KDriveActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true
) {
    val scheme = MaterialTheme.colorScheme
    val size = modifier.heightIn(min = KDriveUiSize.touchHeight)
    val content: @Composable () -> Unit = {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (filled) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = size,
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary,
                disabledContainerColor = scheme.surfaceVariant,
                disabledContentColor = scheme.onSurfaceVariant
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) { content() }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = enabled,
            modifier = size,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = scheme.primary,
                disabledContentColor = scheme.onSurfaceVariant
            ),
            border = BorderStroke(
                1.dp,
                if (enabled) scheme.outline else scheme.outline.copy(alpha = 0.65f)
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
        ) { content() }
    }
}

private val KDriveTypography = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 32.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 21.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 20.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun KDriveTheme(palette: KDrivePalette, content: @Composable () -> Unit) {
    val safe = if (kdrivePaletteIsReadable(palette)) palette else KDRIVE_THEME_PRESETS.first()
    val bg = kdriveParseHex(safe.backgroundHex)!!
    val card = kdriveParseHex(safe.cardHex)!!
    val accent = kdriveParseHex(safe.accentHex)!!
    val text = kdriveParseHex(safe.textHex)!!

    val muted = lerp(text, card, 0.13f)
    val secondaryText = if (kdriveContrastRatio(muted, card) >= 4.5 &&
        kdriveContrastRatio(muted, bg) >= 4.5) muted else text
    val onAccent = if (kdriveContrastRatio(accent, Color.Black) >= 4.5)
        Color.Black else Color.White

    val scheme = if (safe.dark) darkColorScheme(
        primary = accent, onPrimary = onAccent,
        primaryContainer = card, onPrimaryContainer = text,
        background = bg, onBackground = text,
        surface = card, onSurface = text,
        surfaceVariant = card, onSurfaceVariant = secondaryText,
        secondary = accent, onSecondary = onAccent,
        outline = accent
    ) else lightColorScheme(
        primary = accent, onPrimary = onAccent,
        primaryContainer = card, onPrimaryContainer = text,
        background = bg, onBackground = text,
        surface = card, onSurface = text,
        surfaceVariant = card, onSurfaceVariant = secondaryText,
        secondary = accent, onSecondary = onAccent,
        outline = accent
    )

    MaterialTheme(colorScheme = scheme, typography = KDriveTypography) {
        // Ensures plain Text outside cards inherits the correct theme text color.
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
            content()
        }
    }
}
