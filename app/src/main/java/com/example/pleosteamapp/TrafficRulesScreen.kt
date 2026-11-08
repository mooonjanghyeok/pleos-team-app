
package com.example.pleosteamapp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/*
 * K-DRIVE GUIDE - PC2
 *
 * Traffic Rules UI
 *
 * - 기존 6개 + 신규 5개 = 총 11개
 * - 3개 카테고리
 * - Landscape / Portrait 대응
 * - Play / Stop / Replay 콜백
 *
 * 실제 NaviHelper, GPS, TTS는 PC1에서 연결한다.
 * 안내 문구는 최종 법규 검증 전 초안이다.
 */

// -------------------------------------------------
// 1. 카테고리 정의
// -------------------------------------------------

private enum class TrafficGroup(
    val title: String,
    val koreanTitle: String
) {
    INTERSECTIONS(
        "Intersections & Signals",
        "교차로 및 신호"
    ),

    ROAD_SAFETY(
        "Road Safety",
        "도로 안전"
    ),

    HIGHWAY(
        "Highway & Essentials",
        "고속도로 및 기본규칙"
    )
}

// -------------------------------------------------
// 2. 교통규칙 데이터 구조
// -------------------------------------------------

private data class TrafficRuleItem(
    val id: String,
    val group: TrafficGroup,
    val title: String,
    val koreanTitle: String,
    val description: String,
    val speech: String
)

// -------------------------------------------------
// 3. 교통규칙 데이터 - 총 11개
// -------------------------------------------------

private val trafficRules = listOf(

    // ===== 교차로 및 신호 =====

    TrafficRuleItem(
        id = "roundabout",
        group = TrafficGroup.INTERSECTIONS,
        title = "Roundabout",
        koreanTitle = "회전교차로",
        description =
            "Vehicles already circulating have priority. " +
                    "Slow down and yield before entering.",
        speech =
            "You are approaching a roundabout. " +
                    "Slow down and follow the signs. " +
                    "Yield to vehicles already circulating. " +
                    "Enter only when it is safe."
    ),

    TrafficRuleItem(
        id = "left_turn",
        group = TrafficGroup.INTERSECTIONS,
        title = "Unprotected Left Turn",
        koreanTitle = "비보호 좌회전",
        description =
            "At a marked unprotected left turn, " +
                    "follow the signals and yield to " +
                    "oncoming traffic and pedestrians.",
        speech =
            "This is an unprotected left turn. " +
                    "Check the traffic signal and signs. " +
                    "Yield to oncoming vehicles and pedestrians. " +
                    "Turn only when permitted and safe."
    ),

    TrafficRuleItem(
        id = "right_turn",
        group = TrafficGroup.INTERSECTIONS,
        title = "Right Turn",
        koreanTitle = "우회전",
        description =
            "Check the traffic signal, dedicated " +
                    "right-turn lights, and pedestrians. " +
                    "Stop whenever required.",
        speech =
            "Before turning right, check the signals " +
                    "and pedestrian crossings. " +
                    "Stop when required. " +
                    "Follow dedicated right turn signals " +
                    "and proceed only when it is safe."
    ),

    TrafficRuleItem(
        id = "traffic_signals",
        group = TrafficGroup.INTERSECTIONS,
        title = "Traffic Signals & Flashing Lights",
        koreanTitle = "신호등 및 점멸신호",
        description =
            "Flashing red and flashing yellow " +
                    "signals have different meanings. " +
                    "Red requires a complete stop. " +
                    "Yellow requires extra caution.",
        speech =
            "Pay attention to flashing traffic lights. " +
                    "At a flashing red light, come to a " +
                    "complete stop before proceeding safely. " +
                    "At a flashing yellow light, proceed " +
                    "cautiously and follow road signs."
    ),

    TrafficRuleItem(
        id = "yield_merge",
        group = TrafficGroup.INTERSECTIONS,
        title = "Yield & Merge",
        koreanTitle = "양보 및 합류",
        description =
            "Check surrounding traffic and " +
                    "yield where required. " +
                    "Signal early and merge only when safe.",
        speech =
            "When merging into another lane, " +
                    "check your mirrors and blind spots. " +
                    "Use your turn signal. " +
                    "Yield to traffic with the right of way " +
                    "and merge only when there is enough space."
    ),

    // ===== 도로 안전 =====

    TrafficRuleItem(
        id = "crossing_school",
        group = TrafficGroup.ROAD_SAFETY,
        title = "Pedestrian Crossing & School Zone",
        koreanTitle = "횡단보도 및 어린이보호구역",
        description =
            "Watch for pedestrians and children. " +
                    "Observe posted school zone speed limits. " +
                    "Stop when required at crossings.",
        speech =
            "Watch carefully for pedestrians " +
                    "at crosswalks. " +
                    "Always stop when required. " +
                    "In school zones, slow down, " +
                    "follow posted speed limits " +
                    "and watch for children."
    ),

    TrafficRuleItem(
        id = "safe_distance",
        group = TrafficGroup.ROAD_SAFETY,
        title = "Safe Distance & Lane Changes",
        koreanTitle = "안전거리 및 차로 변경",
        description =
            "Keep enough distance from vehicles ahead. " +
                    "Signal and check blind spots " +
                    "before changing lanes.",
        speech =
            "Maintain a safe following distance. " +
                    "Avoid sudden lane changes. " +
                    "Before changing lanes, use your turn signal, " +
                    "check your mirrors and blind spots, " +
                    "and move only when it is safe."
    ),

    TrafficRuleItem(
        id = "road_signs",
        group = TrafficGroup.ROAD_SAFETY,
        title = "Korean Road Signs",
        koreanTitle = "한국 도로표지 이해",
        description =
            "Pay attention to Korean road signs, " +
                    "lane markings, and supplementary signs. " +
                    "Do not make sudden maneuvers " +
                    "when a sign is unclear.",
        speech =
            "Pay attention to Korean traffic signs " +
                    "and road markings. " +
                    "Some supplementary signs contain " +
                    "important Korean text. " +
                    "If you are unsure, remain calm " +
                    "and avoid sudden lane changes. " +
                    "Follow the signs and drive safely."
    ),

    // ===== 고속도로 및 기본규칙 =====

    TrafficRuleItem(
        id = "tollgate",
        group = TrafficGroup.HIGHWAY,
        title = "Tollgate & Hi-Pass",
        koreanTitle = "톨게이트 및 하이패스",
        description =
            "Check payment lane signs in advance. " +
                    "Use a lane that supports your " +
                    "vehicle's payment method.",
        speech =
            "A tollgate is ahead. " +
                    "Check the payment lane signs. " +
                    "Use a lane that supports your payment method. " +
                    "If you enter the wrong lane, " +
                    "do not stop suddenly or reverse. " +
                    "Continue safely and arrange payment later."
    ),

    TrafficRuleItem(
        id = "bus_lane",
        group = TrafficGroup.HIGHWAY,
        title = "Bus-Only Lane",
        koreanTitle = "버스전용차로",
        description =
            "Bus-only lane restrictions depend " +
                    "on the road and operating hours. " +
                    "Check the signs before entering.",
        speech =
            "Be aware of bus-only lanes in Korea. " +
                    "Some lanes are restricted to " +
                    "eligible vehicles during specified hours. " +
                    "Check the road signs before entering " +
                    "a bus-only lane."
    ),

    TrafficRuleItem(
        id = "parking",
        group = TrafficGroup.HIGHWAY,
        title = "Parking Rules",
        koreanTitle = "주차 관련 규칙",
        description =
            "Check parking restrictions, signs, " +
                    "road markings, and designated spaces.",
        speech =
            "Before parking your vehicle, " +
                    "check local parking signs and restrictions. " +
                    "Use a designated parking space. " +
                    "Do not block crosswalks, intersections " +
                    "or vehicle entrances."
    )
)

// -------------------------------------------------
// 4. 메인 UI
// -------------------------------------------------

@Composable
fun TrafficRulesScreen(
    onBack: () -> Unit,
    onSpeakRequest: (String) -> Unit,
    onStopRequest: () -> Unit
) {

    var selectedGroupName by rememberSaveable {
        mutableStateOf(TrafficGroup.INTERSECTIONS.name)
    }

    var selectedRuleId by rememberSaveable {
        mutableStateOf("roundabout")
    }

    var lastRequestedSpeech by rememberSaveable {
        mutableStateOf("")
    }

    var statusText by rememberSaveable {
        mutableStateOf("Select a situation to begin.")
    }

    val selectedGroup =
        TrafficGroup.values().firstOrNull {
            it.name == selectedGroupName
        } ?: TrafficGroup.INTERSECTIONS

    val visibleRules = trafficRules.filter {
        it.group == selectedGroup
    }

    val selectedRule =
        visibleRules.firstOrNull {
            it.id == selectedRuleId
        } ?: visibleRules.first()

    // 카테고리 변경
    val selectGroup: (TrafficGroup) -> Unit = { group ->

        if (group != selectedGroup) {

            onStopRequest()

            selectedGroupName = group.name

            selectedRuleId =
                trafficRules.first {
                    it.group == group
                }.id

            lastRequestedSpeech = ""

            statusText = "Category selected: ${group.title}"
        }
    }

    // 상황 선택
    val selectRule: (String) -> Unit = { id ->

        if (id != selectedRuleId) {

            onStopRequest()

            selectedRuleId = id
            lastRequestedSpeech = ""

            statusText = "Situation selected."
        }
    }

    // 선택한 영어 안내 재생 요청
    val playSelected: () -> Unit = {

        onStopRequest()

        lastRequestedSpeech = selectedRule.speech

        onSpeakRequest(selectedRule.speech)

        statusText = "Voice playback requested."
    }

    // 중지 요청
    val stopSelected: () -> Unit = {

        onStopRequest()

        statusText = "Stop requested."
    }

    // 마지막 문장 다시 요청
    val replaySelected: () -> Unit = {

        if (lastRequestedSpeech.isNotBlank()) {

            onStopRequest()

            onSpeakRequest(lastRequestedSpeech)

            statusText = "Replay requested."
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {

        val isLandscapeLayout = maxWidth >= 780.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),

            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // 상단 제목
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {

                TextButton(
                    onClick = {
                        onStopRequest()
                        onBack()
                    }
                ) {
                    Text("Back")
                }

                Column {

                    Text(
                        text = "Traffic Rules Guide",
                        style =
                            MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "K-DRIVE GUIDE | 11 SITUATIONS",
                        style =
                            MaterialTheme.typography.labelSmall
                    )
                }
            }

            HorizontalDivider()

            if (isLandscapeLayout) {

                // ===================================
                // Landscape: 3개 영역
                // ===================================

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),

                    horizontalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {

                    // 왼쪽: 카테고리
                    TrafficCategoryPanel(
                        selectedGroup = selectedGroup,
                        onSelectGroup = selectGroup,
                        modifier = Modifier
                            .width(185.dp)
                            .fillMaxHeight()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    )

                    // 중앙: 상황 목록
                    TrafficRuleList(
                        rules = visibleRules,
                        selectedRuleId = selectedRule.id,
                        onSelectRule = selectRule,
                        modifier = Modifier
                            .width(245.dp)
                            .fillMaxHeight()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    )

                    // 오른쪽: 상세 안내
                    TrafficRuleDetails(
                        rule = selectedRule,
                        statusText = statusText,
                        replayEnabled =
                            lastRequestedSpeech.isNotBlank(),
                        onPlay = playSelected,
                        onStop = stopSelected,
                        onReplay = replaySelected,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(
                                rememberScrollState()
                            )
                    )
                }

            } else {

                // ===================================
                // Portrait: 세로 배치
                // ===================================

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(
                            rememberScrollState()
                        ),

                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {

                    Text(
                        text = "Choose a Category",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    TrafficCategoryChips(
                        selectedGroup = selectedGroup,
                        onSelectGroup = selectGroup
                    )

                    HorizontalDivider()

                    TrafficRuleList(
                        rules = visibleRules,
                        selectedRuleId = selectedRule.id,
                        onSelectRule = selectRule
                    )

                    HorizontalDivider()

                    TrafficRuleDetails(
                        rule = selectedRule,
                        statusText = statusText,
                        replayEnabled =
                            lastRequestedSpeech.isNotBlank(),
                        onPlay = playSelected,
                        onStop = stopSelected,
                        onReplay = replaySelected
                    )
                }
            }
        }
    }
}

// -------------------------------------------------
// 5. Landscape 카테고리 패널
// -------------------------------------------------

@Composable
private fun TrafficCategoryPanel(
    selectedGroup: TrafficGroup,
    onSelectGroup: (TrafficGroup) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement =
            Arrangement.spacedBy(10.dp)
    ) {

        Text(
            text = "Categories",
            style = MaterialTheme.typography.titleMedium
        )

        TrafficGroup.values().forEach { group ->

            val selected = group == selectedGroup

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectGroup(group)
                    },

                colors = CardDefaults.cardColors(
                    containerColor =
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                )
            ) {

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(5.dp)
                ) {

                    Text(
                        text = group.title,
                        style =
                            MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = group.koreanTitle,
                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Text(
                        text = "${trafficRules.count {
                            it.group == group
                        }} situations",
                        style =
                            MaterialTheme.typography.labelSmall
                    )

                    if (selected) {

                        Text(
                            text = "SELECTED",
                            style =
                                MaterialTheme.typography.labelSmall,
                            color =
                                MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------
// 6. Portrait 카테고리 선택
// -------------------------------------------------

@Composable
private fun TrafficCategoryChips(
    selectedGroup: TrafficGroup,
    onSelectGroup: (TrafficGroup) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(
                rememberScrollState()
            ),

        horizontalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        TrafficGroup.values().forEach { group ->

            if (group == selectedGroup) {

                Button(
                    onClick = {
                        onSelectGroup(group)
                    }
                ) {
                    Text(group.title)
                }

            } else {

                OutlinedButton(
                    onClick = {
                        onSelectGroup(group)
                    }
                ) {
                    Text(group.title)
                }
            }
        }
    }
}

// -------------------------------------------------
// 7. 선택한 카테고리의 상황 목록
// -------------------------------------------------

@Composable
private fun TrafficRuleList(
    rules: List<TrafficRuleItem>,
    selectedRuleId: String,
    onSelectRule: (String) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement =
            Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = "Driving Situations",
            style = MaterialTheme.typography.titleMedium
        )

        rules.forEach { rule ->

            val selected = rule.id == selectedRuleId

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onSelectRule(rule.id)
                    },

                colors = CardDefaults.cardColors(
                    containerColor =
                        if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                )
            ) {

                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement =
                        Arrangement.spacedBy(4.dp)
                ) {

                    Text(
                        text = rule.title,
                        style =
                            MaterialTheme.typography.titleSmall
                    )

                    Text(
                        text = rule.koreanTitle,
                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    if (selected) {

                        Text(
                            text = "SELECTED",
                            color =
                                MaterialTheme.colorScheme.primary,
                            style =
                                MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------
// 8. 상세 설명 및 영어 음성 요청
// -------------------------------------------------

@Composable
private fun TrafficRuleDetails(
    rule: TrafficRuleItem,
    statusText: String,
    replayEnabled: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier,
        verticalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = rule.title,
            style = MaterialTheme.typography.headlineSmall
        )

        Text(
            text = rule.koreanTitle,
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = rule.description,
            style = MaterialTheme.typography.bodyLarge
        )

        HorizontalDivider()

        Text(
            text = "English Voice Guidance",
            style = MaterialTheme.typography.titleMedium
        )

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {

            Text(
                text = rule.speech,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Play / Stop
        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            Button(
                onClick = onPlay,
                modifier = Modifier.weight(1f)
            ) {
                Text("Play Voice")
            }

            OutlinedButton(
                onClick = onStop,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop")
            }
        }

        // Replay
        OutlinedButton(
            onClick = onReplay,
            enabled = replayEnabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Replay Last Message")
        }

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodySmall
        )

        HorizontalDivider()

        Text(
            text = "For learning before driving or " +
                    "while safely parked.",
            style = MaterialTheme.typography.labelMedium
        )

        Text(
            text = "Prototype guidance. Final traffic-rule " +
                    "verification is required before release.",
            style = MaterialTheme.typography.labelSmall,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// -------------------------------------------------
// 9. Landscape Preview
// -------------------------------------------------

@Preview(
    name = "K-DRIVE Traffic Rules Landscape",
    showBackground = true,
    widthDp = 1000,
    heightDp = 600
)
@Composable
private fun TrafficRulesLandscapePreview() {

    MaterialTheme {

        TrafficRulesScreen(
            onBack = {},
            onSpeakRequest = {},
            onStopRequest = {}
        )
    }
}

// -------------------------------------------------
// 10. Portrait Preview
// -------------------------------------------------

@Preview(
    name = "K-DRIVE Traffic Rules Portrait",
    showBackground = true,
    widthDp = 400,
    heightDp = 800
)
@Composable
private fun TrafficRulesPortraitPreview() {

    MaterialTheme {

        TrafficRulesScreen(
            onBack = {},
            onSpeakRequest = {},
            onStopRequest = {}
        )
    }
}
