
package com.example.pleosteamapp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/*
 * K-DRIVE GUIDE - PC2
 *
 * Traffic Rules Screen
 *
 * - 11 driving situations
 * - 3 categories
 * - Play / Stop / Replay
 * - Landscape / Portrait Preview
 * - Driving restriction support
 *
 * PC1 owns NaviHelper, TTS and the guidance engine.
 */

private data class KDriveRule(
    val id: String,
    val category: String,
    val title: String,
    val korean: String,
    val summary: String,
    val speech: String
)

private val ruleGroups = listOf(
    "Intersections",
    "Road Safety",
    "Essentials"
)

// -------------------------------------------
// Traffic rule data - 11 situations
// -------------------------------------------

private val rules = listOf(

    KDriveRule(
        "roundabout",
        "Intersections",
        "Roundabout",
        "회전교차로",
        "Yield to circulating vehicles.",
        "Slow down. Yield to vehicles already " +
                "in the roundabout. Enter only when it is safe."
    ),

    KDriveRule(
        "left_turn",
        "Intersections",
        "Unprotected Left Turn",
        "비보호 좌회전",
        "Follow signals and yield before turning.",
        "Check the traffic signal. Yield to oncoming " +
                "traffic and pedestrians. Turn only when " +
                "permitted and safe."
    ),

    KDriveRule(
        "right_turn",
        "Intersections",
        "Right Turn",
        "우회전",
        "Check signals and pedestrian crossings.",
        "Check the right turn signals and pedestrians. " +
                "Stop when required. Turn only when it is safe."
    ),

    KDriveRule(
        "signals",
        "Intersections",
        "Flashing Lights",
        "신호등 및 점멸신호",
        "Flashing red means stop; yellow means caution.",
        "At flashing red, stop first. At flashing yellow, " +
                "proceed carefully. Always check for other traffic."
    ),

    KDriveRule(
        "yield",
        "Intersections",
        "Yield & Merge",
        "양보 및 합류",
        "Signal, check blind spots and yield.",
        "Use your turn signal. Check mirrors and blind spots. " +
                "Yield and merge only when there is enough space."
    ),

    KDriveRule(
        "crossing",
        "Road Safety",
        "Crosswalk & School Zone",
        "횡단보도 및 어린이보호구역",
        "Watch for pedestrians and posted speed limits.",
        "Watch for pedestrians. Stop when required at crossings. " +
                "In school zones, follow posted limits " +
                "and watch for children."
    ),

    KDriveRule(
        "distance",
        "Road Safety",
        "Following & Lane Changes",
        "안전거리 및 차로 변경",
        "Keep a safe distance and avoid sudden moves.",
        "Keep a safe distance. Signal before changing lanes, " +
                "check blind spots, and change lanes only when safe."
    ),

    KDriveRule(
        "signs",
        "Road Safety",
        "Korean Road Signs",
        "한국 도로표지 이해",
        "Check road markings and supplementary signs.",
        "Check traffic signs and road markings. " +
                "If a sign is unclear, avoid sudden turns " +
                "and continue safely."
    ),

    KDriveRule(
        "toll",
        "Essentials",
        "Tollgate & Hi-Pass",
        "톨게이트 및 하이패스",
        "Check payment lanes before entering.",
        "Choose a payment lane that suits your vehicle. " +
                "Do not stop suddenly or reverse at a tollgate."
    ),

    KDriveRule(
        "bus",
        "Essentials",
        "Bus-Only Lane",
        "버스전용차로",
        "Check lane restrictions and operating hours.",
        "Bus-only lane restrictions may vary. " +
                "Check the signs and operating hours before entering."
    ),

    KDriveRule(
        "parking",
        "Essentials",
        "Parking Rules",
        "주차 관련 규칙",
        "Use permitted parking spaces.",
        "Check parking signs. Use designated spaces " +
                "and keep intersections, crosswalks, " +
                "and vehicle entrances clear."
    )
)

// -------------------------------------------
// Main Screen
// -------------------------------------------

@Composable
fun TrafficRulesScreen(
    onBack: () -> Unit,
    onSpeakRequest: (String) -> Unit,
    onStopRequest: () -> Unit,
    interactionRestricted: Boolean = true
) {

    val colors = MaterialTheme.colorScheme

    // While driving, complex interaction is unavailable.
    if (interactionRestricted) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),

            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            Text(
                text = "Traffic Rules",
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = "Open this guide while safely parked.",
                style = MaterialTheme.typography.titleLarge
            )
        }

        return
    }

    var selectedGroup by rememberSaveable {
        mutableStateOf(ruleGroups.first())
    }

    var selectedRuleId by rememberSaveable {
        mutableStateOf("roundabout")
    }

    var lastSpeech by rememberSaveable {
        mutableStateOf("")
    }

    var uiMessage by rememberSaveable {
        mutableStateOf("Choose a situation.")
    }

    val visible = rules.filter {
        it.category == selectedGroup
    }

    val selected = visible.firstOrNull {
        it.id == selectedRuleId
    } ?: visible.first()

    // Category selection
    val selectGroup: (String) -> Unit = { group ->

        if (group != selectedGroup) {

            onStopRequest()

            selectedGroup = group

            selectedRuleId = rules.first {
                it.category == group
            }.id

            lastSpeech = ""

            uiMessage = "Category selected"
        }
    }

    // Situation selection
    val selectRule: (KDriveRule) -> Unit = { rule ->

        if (rule.id != selectedRuleId) {

            onStopRequest()

            selectedRuleId = rule.id
            lastSpeech = ""

            uiMessage = "Situation selected"
        }
    }

    // Play voice request
    val play: () -> Unit = {

        onStopRequest()

        lastSpeech = selected.speech

        onSpeakRequest(selected.speech)

        uiMessage = "Voice requested"
    }

    // Stop voice request
    val stop: () -> Unit = {

        onStopRequest()

        uiMessage = "Stop requested"
    }

    // Replay voice request
    val replay: () -> Unit = {

        if (lastSpeech.isNotBlank()) {

            onStopRequest()

            onSpeakRequest(lastSpeech)

            uiMessage = "Replay requested"
        }
    }

    // ---------------------------------------
    // Responsive Layout
    // ---------------------------------------

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {

        val wide = maxWidth >= 670.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(KDriveUiSize.padding),

            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Header
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                KDriveActionButton(
                    label = "Back",
                    onClick = {
                        onStopRequest()
                        onBack()
                    },
                    filled = false
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = "Traffic Rules",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "Learn while parked - 11 situations",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Category buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                ruleGroups.forEach { group ->

                    KDriveActionButton(
                        label = group,
                        onClick = {
                            selectGroup(group)
                        },
                        modifier = Modifier.weight(1f),
                        filled = selectedGroup == group
                    )
                }
            }

            if (wide) {

                // Landscape: list on left, detail on right
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    Column(
                        modifier = Modifier
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),

                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        visible.forEach { rule ->

                            KDriveRuleCard(
                                rule = rule,
                                selected = rule.id == selected.id,
                                onClick = {
                                    selectRule(rule)
                                }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {

                        KDriveRuleDetail(
                            rule = selected,
                            status = uiMessage,
                            canReplay = lastSpeech.isNotBlank(),
                            onPlay = play,
                            onStop = stop,
                            onReplay = replay
                        )
                    }
                }

            } else {

                // Portrait: vertical layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),

                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    visible.forEach { rule ->

                        KDriveRuleCard(
                            rule = rule,
                            selected = rule.id == selected.id,
                            onClick = {
                                selectRule(rule)
                            }
                        )
                    }

                    KDriveRuleDetail(
                        rule = selected,
                        status = uiMessage,
                        canReplay = lastSpeech.isNotBlank(),
                        onPlay = play,
                        onStop = stop,
                        onReplay = replay
                    )
                }
            }
        }
    }
}

// -------------------------------------------
// Rule Card
// -------------------------------------------

@Composable
private fun KDriveRuleCard(
    rule: KDriveRule,
    selected: Boolean,
    onClick: () -> Unit
) {

    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = KDriveUiSize.touchHeight)
            .clickable(onClick = onClick),

        shape = RoundedCornerShape(16.dp),

        colors = CardDefaults.cardColors(
            containerColor =
                if (selected) colors.primaryContainer
                else colors.surface
        )
    ) {

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),

            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {

            Text(
                text = rule.title,
                style = MaterialTheme.typography.titleMedium,
                color =
                    if (selected) colors.primary
                    else colors.onSurface,
                maxLines = 2
            )

            Text(
                text = rule.korean,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}

// -------------------------------------------
// Rule Details and Voice Controls
// -------------------------------------------

@Composable
private fun KDriveRuleDetail(
    rule: KDriveRule,
    status: String,
    canReplay: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = rule.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),

                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Text(
                    text = "KEY POINT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = rule.summary,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "VOICE SCRIPT",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = rule.speech,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(KDriveUiSize.actionGap)
        ) {

            KDriveActionButton(
                label = "Play",
                onClick = onPlay,
                modifier = Modifier.weight(1f)
            )

            KDriveActionButton(
                label = "Stop",
                onClick = onStop,
                modifier = Modifier.weight(1f),
                filled = false
            )
        }

        KDriveActionButton(
            label = "Replay",
            onClick = onReplay,
            modifier = Modifier.fillMaxWidth(),
            filled = false,
            enabled = canReplay
        )

        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2
        )
    }
}

// -------------------------------------------
// Landscape Preview
// -------------------------------------------

@Preview(
    name = "Traffic - Landscape parked",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KDriveRulesLandscapePreview() {

    KDriveTheme(KDRIVE_THEME_PRESETS[0]) {

        TrafficRulesScreen(
            onBack = {},
            onSpeakRequest = {},
            onStopRequest = {},
            interactionRestricted = false
        )
    }
}

// -------------------------------------------
// Portrait Preview
// -------------------------------------------

@Preview(
    name = "Traffic - Portrait parked",
    widthDp = 400,
    heightDp = 800,
    showBackground = true
)
@Composable
private fun KDriveRulesPortraitPreview() {

    KDriveTheme(KDRIVE_THEME_PRESETS[0]) {

        TrafficRulesScreen(
            onBack = {},
            onSpeakRequest = {},
            onStopRequest = {},
            interactionRestricted = false
        )
    }
}
