
package com.example.pleosteamapp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class KdThemeWords(
    val back: String,
    val title: String,
    val restricted: String,
    val intro: String,
    val systemSelected: String,
    val followDevice: String,
    val deviceDark: String,
    val deviceLight: String,
    val systemExplanation: String,
    val customActive: String,
    val darkTitle: String,
    val lightTitle: String,
    val selected: String,
    val reset: String
)

private fun kdThemeWords(
    lang: String
): KdThemeWords = when (lang) {

    "KO" -> KdThemeWords(
        "← 뒤로",
        "테마 설정",
        "테마 변경은 안전하게 정차한 상태에서만 가능합니다.",
        "원하는 색상 조합 10개 중 하나를 선택하세요.",
        "시스템 설정 적용 중",
        "기기 화면 설정 따르기",
        "다크 모드",
        "라이트 모드",
        "현재 기기는 %s입니다. 시스템 모드는 기기의 화면 설정을 따릅니다.",
        "사용자 지정 테마가 적용되어 있습니다. 기본 테마를 선택하면 변경됩니다.",
        "다크 테마",
        "라이트 테마",
        "선택됨",
        "Midnight 테마로 초기화"
    )

    "FR" -> KdThemeWords(
        "← Retour",
        "Paramètres du thème",
        "Modifiez le thème uniquement lorsque le véhicule est garé.",
        "Choisissez l'une des 10 combinaisons de couleurs.",
        "Mode système sélectionné",
        "Suivre l'apparence de l'appareil",
        "mode sombre",
        "mode clair",
        "L'appareil utilise actuellement le %s. Le mode système suit les réglages de l'appareil.",
        "Un thème personnalisé est actif. Sélectionnez un préréglage pour le remplacer.",
        "THÈMES SOMBRES",
        "THÈMES CLAIRS",
        "Sélectionné",
        "Réinitialiser à Midnight"
    )

    "ES" -> KdThemeWords(
        "← Volver",
        "Configuración del tema",
        "Cambie el tema solo cuando el vehículo esté estacionado.",
        "Elija una de las 10 combinaciones de colores.",
        "Modo del sistema seleccionado",
        "Seguir el aspecto del dispositivo",
        "modo oscuro",
        "modo claro",
        "El dispositivo utiliza el %s. El modo del sistema sigue los ajustes del dispositivo.",
        "Hay un tema personalizado activo. Seleccione uno predefinido para sustituirlo.",
        "TEMAS OSCUROS",
        "TEMAS CLAROS",
        "Seleccionado",
        "Restablecer a Midnight"
    )

    else -> KdThemeWords(
        "← Back",
        "Theme Settings",
        "Theme editing is available while parked.",
        "Choose one of 10 ready-made color combinations while parked.",
        "System mode · selected",
        "Follow device appearance",
        "dark mode",
        "light mode",
        "The device currently uses %s. System mode follows the device appearance.",
        "A previously saved Custom theme is active. Select a preset to replace it.",
        "DARK THEMES",
        "LIGHT THEMES",
        "Selected",
        "Reset to Midnight"
    )
}

@Composable
private fun kdThemeLanguage(): String {
    val context = LocalContext.current
    val preview = LocalInspectionMode.current

    return remember(context, preview) {
        if (preview) {
            "EN"
        } else {
            context.applicationContext
                .getSharedPreferences(
                    "kdrive_ui_colors_v2",
                    Context.MODE_PRIVATE
                )
                .getString("startup_language", "EN")
                ?.uppercase(Locale.ROOT)
                ?.takeIf {
                    it in listOf("KO", "EN", "FR", "ES")
                } ?: "EN"
        }
    }
}

// ==================================================
// Theme settings
// ==================================================

@Suppress("UNUSED_PARAMETER")
@Composable
fun KDriveThemeSettingsScreen(
    selectedThemeId: String,
    customPalette: KDrivePalette,
    onThemeSelected: (String) -> Unit,
    onSaveCustom: (KDrivePalette) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    interactionRestricted: Boolean = true,
    backLabel: String = "← Back",
    uiLanguage: String? = null
) {
    val lang = uiLanguage ?: kdThemeLanguage()
    val words = kdThemeWords(lang)
    val colors = MaterialTheme.colorScheme

    if (interactionRestricted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                words.title,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground
            )

            Text(
                words.restricted,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )
        }
        return
    }

    val deviceDark = isSystemInDarkTheme()

    val darkThemes = KDRIVE_THEME_PRESETS.filter {
        it.dark
    }

    val lightThemes = KDRIVE_THEME_PRESETS.filter {
        !it.dark
    }

    val selected = if (
        selectedThemeId == "system" ||
        selectedThemeId == "custom" ||
        KDRIVE_THEME_PRESETS.any {
            it.id == selectedThemeId
        }
    ) {
        selectedThemeId
    } else {
        KDRIVE_DEFAULT_THEME
    }

    val translatedBack = if (
        backLabel == "← Back"
    ) {
        words.back
    } else {
        backLabel
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val columns = if (maxWidth >= 700.dp) {
            4
        } else {
            2
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(KDriveUiSize.padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                KDriveActionButton(
                    translatedBack,
                    onBack,
                    filled = false
                )

                Text(
                    words.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground
                )
            }

            Text(
                words.intro,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant
            )

            KDriveActionButton(
                label = if (selectedThemeId == "system") {
                    words.systemSelected
                } else {
                    words.followDevice
                },
                onClick = {
                    onThemeSelected("system")
                },
                modifier = Modifier.fillMaxWidth(),
                filled = selectedThemeId == "system"
            )

            val mode = if (deviceDark) {
                words.deviceDark
            } else {
                words.deviceLight
            }

            Text(
                words.systemExplanation.replace(
                    "%s",
                    mode
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackground
            )

            if (selectedThemeId == "custom") {
                Text(
                    words.customActive,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onBackground
                )
            }

            Text(
                "${words.darkTitle} · ${darkThemes.size}",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )

            KdLocalizedPresetGrid(
                palettes = darkThemes,
                selectedId = selected,
                columns = columns,
                selectedLabel = words.selected,
                onSelect = onThemeSelected
            )

            Text(
                "${words.lightTitle} · ${lightThemes.size}",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )

            KdLocalizedPresetGrid(
                palettes = lightThemes,
                selectedId = selected,
                columns = columns,
                selectedLabel = words.selected,
                onSelect = onThemeSelected
            )

            KDriveActionButton(
                words.reset,
                onReset,
                modifier = Modifier.fillMaxWidth(),
                filled = false
            )
        }
    }
}

// ==================================================
// Theme preset grid
// ==================================================

@Composable
private fun KdLocalizedPresetGrid(
    palettes: List<KDrivePalette>,
    selectedId: String,
    columns: Int,
    selectedLabel: String,
    onSelect: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        palettes.chunked(columns).forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                chunk.forEach { palette ->
                    KdLocalizedPresetCard(
                        palette = palette,
                        isSelected =
                            selectedId == palette.id,
                        selectedLabel = selectedLabel,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            onSelect(palette.id)
                        }
                    )
                }

                repeat(columns - chunk.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun KdLocalizedPresetCard(
    palette: KDrivePalette,
    isSelected: Boolean,
    selectedLabel: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme

    val bg =
        kdriveParseHex(palette.backgroundHex)
            ?: Color.Black

    val card =
        kdriveParseHex(palette.cardHex)
            ?: Color.DarkGray

    val accent =
        kdriveParseHex(palette.accentHex)
            ?: Color.White

    val borderColor = if (isSelected) {
        scheme.primary
    } else {
        scheme.outline.copy(alpha = 0.7f)
    }

    Column(
        modifier = modifier
            .heightIn(min = 116.dp)
            .border(
                if (isSelected) 3.dp else 1.dp,
                borderColor,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(
                    bg,
                    RoundedCornerShape(8.dp)
                )
                .padding(7.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(37.dp)
                    .background(
                        card,
                        RoundedCornerShape(6.dp)
                    )
                    .padding(7.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(12.dp)
                        .background(
                            accent,
                            RoundedCornerShape(4.dp)
                        )
                )
            }
        }

        Text(
            text = if (isSelected) {
                "${palette.name} · $selectedLabel"
            } else {
                palette.name
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) {
                scheme.primary
            } else {
                scheme.onBackground
            },
            maxLines = 2
        )
    }
}

@Preview(
    name = "Theme Settings - French",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdThemeFrenchPreview() {
    var chosen by remember {
        mutableStateOf("midnight")
    }

    val custom = KDRIVE_CUSTOM_DEFAULT

    KDriveTheme(
        kdriveResolvePalette(
            chosen,
            custom,
            isSystemInDarkTheme()
        )
    ) {
        KDriveThemeSettingsScreen(
            selectedThemeId = chosen,
            customPalette = custom,
            onThemeSelected = {
                chosen = it
            },
            onSaveCustom = {},
            onReset = {
                chosen = KDRIVE_DEFAULT_THEME
            },
            onBack = {},
            interactionRestricted = false,
            uiLanguage = "FR"
        )
    }
}
