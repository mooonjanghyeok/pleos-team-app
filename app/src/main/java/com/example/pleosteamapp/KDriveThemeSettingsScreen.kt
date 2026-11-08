package com.example.pleosteamapp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Theme selection and HEX editing are for parked use only. */
@Composable
fun KDriveThemeSettingsScreen(
    selectedThemeId: String,
    customPalette: KDrivePalette,
    onThemeSelected: (String) -> Unit,
    onSaveCustom: (KDrivePalette) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
    interactionRestricted: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    if (interactionRestricted) {
        Column(modifier = Modifier.fillMaxSize().background(colors.background).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Theme Settings", style = MaterialTheme.typography.headlineMedium)
            Text("Theme editing is available while parked.", style = MaterialTheme.typography.titleLarge)
        }
        return
    }

    var bg by rememberSaveable(customPalette.backgroundHex) { mutableStateOf(customPalette.backgroundHex) }
    var card by rememberSaveable(customPalette.cardHex) { mutableStateOf(customPalette.cardHex) }
    var accent by rememberSaveable(customPalette.accentHex) { mutableStateOf(customPalette.accentHex) }
    var text by rememberSaveable(customPalette.textHex) { mutableStateOf(customPalette.textHex) }
    val bgColor = kdriveParseHex(bg)
    val draft = KDrivePalette(
        "custom", "Custom", kdriveNormalizeHex(bg), kdriveNormalizeHex(card),
        kdriveNormalizeHex(accent), kdriveNormalizeHex(text),
        (bgColor?.luminance() ?: 0.0f) < 0.5f
    )
    val valid = kdrivePaletteIsReadable(draft)

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(colors.background)) {
        val columns = if (maxWidth >= 700.dp) 4 else 2
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                KDriveActionButton("Back", onBack, filled = false)
                Text("Theme Settings", style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f))
            }
            Text("Change colors while safely parked.", style = MaterialTheme.typography.bodyLarge)
            KDriveActionButton(
                label = if (selectedThemeId == "system") "System mode · selected" else "Follow system day / night mode",
                onClick = { onThemeSelected("system") },
                modifier = Modifier.fillMaxWidth(), filled = selectedThemeId == "system"
            )
            Text("12 PRESET THEMES", style = MaterialTheme.typography.titleLarge)
            KDRIVE_THEME_PRESETS.chunked(columns).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { palette ->
                        KDrivePaletteCard(palette, selectedThemeId == palette.id,
                            { onThemeSelected(palette.id) }, Modifier.weight(1f))
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Text("CUSTOM COLORS", style = MaterialTheme.typography.titleLarge)
            Text("HEX input is available while parked.", style = MaterialTheme.typography.bodyMedium)
            KDriveHexField("Background", bg) { bg = it }
            KDriveHexField("Card", card) { card = it }
            KDriveHexField("Accent", accent) { accent = it }
            KDriveHexField("Text", text) { text = it }
            if (valid) {
                val previewBg = kdriveParseHex(draft.backgroundHex)!!
                val previewCard = kdriveParseHex(draft.cardHex)!!
                val previewAccent = kdriveParseHex(draft.accentHex)!!
                val previewText = kdriveParseHex(draft.textHex)!!
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(previewBg, RoundedCornerShape(18.dp)).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("CUSTOM PREVIEW", style = MaterialTheme.typography.titleLarge,
                        color = previewText)
                    Card(colors = CardDefaults.cardColors(containerColor = previewCard)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("GUIDANCE", style = MaterialTheme.typography.labelLarge,
                                color = previewAccent)
                            Text("Waiting for navigation", style = MaterialTheme.typography.bodyLarge,
                                color = previewText)
                        }
                    }
                }
            }
            Text(if (valid) "Contrast checks passed (4.5:1)." else
                "Use valid HEX colors with at least 4.5:1 contrast.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (valid) colors.primary else colors.error)
            KDriveActionButton("Save custom theme", { onSaveCustom(draft) },
                modifier = Modifier.fillMaxWidth(), enabled = valid)
            KDriveActionButton("Reset to Midnight", onReset,
                modifier = Modifier.fillMaxWidth(), filled = false)
        }
    }
}

@Composable
private fun KDrivePaletteCard(
    palette: KDrivePalette, selected: Boolean, onClick: () -> Unit, modifier: Modifier
) {
    val bg = kdriveParseHex(palette.backgroundHex) ?: Color.Black
    val accent = kdriveParseHex(palette.accentHex) ?: Color.White
    val card = kdriveParseHex(palette.cardHex) ?: Color.DarkGray
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.heightIn(min = 112.dp)
        .border(if (selected) 3.dp else 1.dp, colors.primary, RoundedCornerShape(16.dp))
        .clickable(onClick = onClick).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(50.dp)
            .background(bg, RoundedCornerShape(8.dp)).padding(8.dp)) {
            Box(Modifier.fillMaxWidth().height(30.dp)
                .background(card, RoundedCornerShape(5.dp)).padding(7.dp)) {
                Box(Modifier.fillMaxWidth(0.65f).height(12.dp)
                    .background(accent, RoundedCornerShape(4.dp)))
            }
        }
        Text(palette.name, style = MaterialTheme.typography.bodyMedium,
            maxLines = 2, color = colors.onBackground)
    }
}

@Composable
private fun KDriveHexField(label: String, value: String, onChange: (String) -> Unit) {
    val parsed = kdriveParseHex(value)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = value, onValueChange = { if (it.length <= 7) onChange(it) },
            label = { Text(label, style = MaterialTheme.typography.labelLarge) },
            textStyle = MaterialTheme.typography.bodyLarge, singleLine = true,
            isError = parsed == null, modifier = Modifier.weight(1f)
                .heightIn(min = KDriveUiSize.touchHeight)
        )
        Box(modifier = Modifier.size(KDriveUiSize.touchHeight)
            .background(parsed ?: Color.Transparent, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)))
    }
}

@Preview(name = "Theme - Parked", widthDp = 1000, heightDp = 600, showBackground = true)
@Composable
private fun KDriveThemeScreenPreview() {
    var chosen by remember { mutableStateOf("midnight") }
    var custom by remember { mutableStateOf(KDRIVE_CUSTOM_DEFAULT) }
    KDriveTheme(kdriveResolvePalette(chosen, custom, true)) {
        KDriveThemeSettingsScreen(chosen, custom, { chosen = it },
            { custom = it; chosen = "custom" }, { chosen = "midnight" }, {}, false)
    }
}
