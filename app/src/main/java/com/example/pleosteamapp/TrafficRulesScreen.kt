
package com.example.pleosteamapp

import android.content.Context
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

/*
 * K-DRIVE / Traffic Rules
 *
 * 7 situations.
 * Languages: KO / EN / FR / ES.
 * Uses Welcome language preference.
 * PC1 owns real TTS and CarUxRestrictions.
 */

private data class KdTranslatedRule(
    val id: String,
    val group: String,
    val title: String,
    val summary: String,
    val speech: String
)

private data class KdRulesWords(
    val title: String,
    val subtitle: String,
    val back: String,
    val groups: List<String>,
    val keyPoint: String,
    val voiceScript: String,
    val play: String,
    val stop: String,
    val replay: String,
    val restricted: String,
    val messages: List<String>
)

private val kdRuleGroupIds = listOf(
    "intersections",
    "safety",
    "essentials"
)

private fun kdRulesWords(
    lang: String
): KdRulesWords = when (lang) {

    "KO" -> KdRulesWords(
        title = "교통규칙",
        subtitle = "정차 중 학습 · 7개 상황",
        back = "← 뒤로",
        groups = listOf(
            "교차로",
            "도로 안전",
            "필수 정보"
        ),
        keyPoint = "핵심 내용",
        voiceScript = "음성 안내 문장",
        play = "재생",
        stop = "중지",
        replay = "다시 재생",
        restricted = "안전하게 정차한 후 안내를 확인하세요.",
        messages = listOf(
            "상황을 선택하세요.",
            "분류를 선택했습니다.",
            "상황을 선택했습니다.",
            "음성 재생을 요청했습니다.",
            "음성 중지를 요청했습니다.",
            "다시 재생을 요청했습니다."
        )
    )

    "FR" -> KdRulesWords(
        title = "Code de la route",
        subtitle = "À consulter à l'arrêt · 7 situations",
        back = "← Retour",
        groups = listOf(
            "Carrefours",
            "Sécurité routière",
            "Essentiel"
        ),
        keyPoint = "POINT CLÉ",
        voiceScript = "MESSAGE VOCAL",
        play = "Écouter",
        stop = "Arrêter",
        replay = "Réécouter",
        restricted =
            "Consultez ce guide lorsque le véhicule est garé.",
        messages = listOf(
            "Choisissez une situation.",
            "Catégorie sélectionnée.",
            "Situation sélectionnée.",
            "Lecture audio demandée.",
            "Arrêt audio demandé.",
            "Nouvelle lecture demandée."
        )
    )

    "ES" -> KdRulesWords(
        title = "Normas de tráfico",
        subtitle = "Consultar estacionado · 7 situaciones",
        back = "← Volver",
        groups = listOf(
            "Intersecciones",
            "Seguridad vial",
            "Información básica"
        ),
        keyPoint = "PUNTO CLAVE",
        voiceScript = "MENSAJE DE VOZ",
        play = "Reproducir",
        stop = "Detener",
        replay = "Repetir",
        restricted =
            "Consulte esta guía con el vehículo estacionado.",
        messages = listOf(
            "Seleccione una situación.",
            "Categoría seleccionada.",
            "Situación seleccionada.",
            "Reproducción solicitada.",
            "Detención solicitada.",
            "Repetición solicitada."
        )
    )

    else -> KdRulesWords(
        title = "Traffic Rules",
        subtitle = "Learn while parked · 7 situations",
        back = "← Back",
        groups = listOf(
            "Intersections",
            "Road Safety",
            "Essentials"
        ),
        keyPoint = "KEY POINT",
        voiceScript = "VOICE SCRIPT",
        play = "Play",
        stop = "Stop",
        replay = "Replay",
        restricted =
            "Open this guide while safely parked.",
        messages = listOf(
            "Choose a situation.",
            "Category selected.",
            "Situation selected.",
            "Voice requested.",
            "Stop requested.",
            "Replay requested."
        )
    )
}

// =====================================================
// Seven situations with translated voice scripts
// =====================================================

private fun kdTranslatedRules(
    lang: String
): List<KdTranslatedRule> = when (lang) {

    "KO" -> listOf(
        KdTranslatedRule(
            "roundabout",
            "intersections",
            "회전교차로",
            "회전교차로 안에서 주행 중인 차량에 양보하세요.",
            "회전교차로에 접근하면 속도를 줄이세요. " +
                    "이미 회전 중인 차량에 양보하고 안전할 때 진입하세요."
        ),
        KdTranslatedRule(
            "left_turn",
            "intersections",
            "비보호 좌회전",
            "신호를 확인하고 마주 오는 차량과 보행자에게 양보하세요.",
            "신호를 확인하세요. 마주 오는 차량과 보행자를 살피고, " +
                    "좌회전이 허용되고 안전할 때만 진행하세요."
        ),
        KdTranslatedRule(
            "right_turn",
            "intersections",
            "우회전",
            "신호와 횡단보도의 보행자를 확인하세요.",
            "우회전 신호와 보행자를 확인하세요. " +
                    "정지가 필요한 곳에서는 정지하고 안전할 때 우회전하세요."
        ),
        KdTranslatedRule(
            "crossing",
            "safety",
            "횡단보도 및 어린이보호구역",
            "보행자와 표시된 제한속도에 주의하세요.",
            "횡단보도의 보행자를 확인하고 필요한 경우 정지하세요. " +
                    "어린이보호구역에서는 제한속도를 지키고 어린이를 주의하세요."
        ),
        KdTranslatedRule(
            "signs",
            "safety",
            "한국 도로표지",
            "노면 표시와 보조표지를 함께 확인하세요.",
            "교통표지와 노면 표시를 확인하세요. " +
                    "표지의 뜻이 불분명하면 갑작스럽게 방향을 바꾸지 말고 안전하게 진행하세요."
        ),
        KdTranslatedRule(
            "toll",
            "essentials",
            "톨게이트 및 하이패스",
            "차량의 통행료 결제 방식에 맞는 차로를 이용하세요.",
            "차량에 맞는 통행료 결제 차로를 선택하세요. " +
                    "톨게이트에서 급정거하거나 후진하지 마세요."
        ),
        KdTranslatedRule(
            "bus",
            "essentials",
            "버스전용차로",
            "통행 제한과 운영시간을 확인하세요.",
            "버스전용차로 이용 제한은 구간과 시간에 따라 다를 수 있습니다. " +
                    "진입 전에 표지와 운영시간을 확인하세요."
        )
    )

    "FR" -> listOf(
        KdTranslatedRule(
            "roundabout",
            "intersections",
            "Rond-point",
            "Cédez le passage aux véhicules déjà engagés.",
            "Ralentissez à l'approche du rond-point. " +
                    "Cédez le passage aux véhicules déjà engagés " +
                    "et entrez uniquement lorsque cela est sûr."
        ),
        KdTranslatedRule(
            "left_turn",
            "intersections",
            "Tourner à gauche sans flèche dédiée",
            "Vérifiez les feux et laissez passer les véhicules venant en face.",
            "Vérifiez les feux de circulation. " +
                    "Cédez le passage aux véhicules venant en sens inverse et aux piétons. " +
                    "Tournez seulement lorsque cela est autorisé et sans danger."
        ),
        KdTranslatedRule(
            "right_turn",
            "intersections",
            "Tourner à droite",
            "Vérifiez les feux et les passages piétons.",
            "Vérifiez les signaux de virage à droite et la présence de piétons. " +
                    "Arrêtez-vous lorsque c'est obligatoire, puis tournez seulement si la voie est libre."
        ),
        KdTranslatedRule(
            "crossing",
            "safety",
            "Passages piétons et zones scolaires",
            "Surveillez les piétons et respectez les limitations affichées.",
            "Surveillez les piétons. Arrêtez-vous aux passages lorsque c'est nécessaire. " +
                    "Dans les zones scolaires, respectez la vitesse indiquée " +
                    "et faites attention aux enfants."
        ),
        KdTranslatedRule(
            "signs",
            "safety",
            "Panneaux routiers coréens",
            "Vérifiez le marquage au sol et les panneaux complémentaires.",
            "Observez les panneaux de signalisation et les marquages au sol. " +
                    "Si un panneau est difficile à comprendre, " +
                    "évitez les changements brusques de direction."
        ),
        KdTranslatedRule(
            "toll",
            "essentials",
            "Péages et Hi-Pass",
            "Choisissez une voie adaptée au mode de paiement du véhicule.",
            "Choisissez la voie de péage adaptée à votre véhicule. " +
                    "Ne vous arrêtez pas brusquement et ne faites jamais marche arrière au péage."
        ),
        KdTranslatedRule(
            "bus",
            "essentials",
            "Voies réservées aux bus",
            "Vérifiez les restrictions et les horaires d'utilisation.",
            "Les règles des voies réservées aux bus peuvent varier " +
                    "selon le lieu et les horaires. Vérifiez les panneaux avant d'y entrer."
        )
    )

    "ES" -> listOf(
        KdTranslatedRule(
            "roundabout",
            "intersections",
            "Glorieta",
            "Ceda el paso a los vehículos que ya circulan por ella.",
            "Reduzca la velocidad al acercarse a una glorieta. " +
                    "Ceda el paso a los vehículos que ya circulan dentro " +
                    "y entre solo cuando sea seguro."
        ),
        KdTranslatedRule(
            "left_turn",
            "intersections",
            "Giro a la izquierda sin flecha específica",
            "Compruebe los semáforos y ceda el paso al tráfico de frente.",
            "Compruebe el semáforo. Ceda el paso a los vehículos " +
                    "que vienen de frente y a los peatones. " +
                    "Gire solo cuando esté permitido y sea seguro."
        ),
        KdTranslatedRule(
            "right_turn",
            "intersections",
            "Giro a la derecha",
            "Compruebe los semáforos y los pasos de peatones.",
            "Compruebe las señales de giro a la derecha y la presencia de peatones. " +
                    "Deténgase cuando sea obligatorio y gire solo cuando sea seguro."
        ),
        KdTranslatedRule(
            "crossing",
            "safety",
            "Pasos de peatones y zonas escolares",
            "Preste atención a los peatones y a los límites de velocidad.",
            "Vigile los pasos de peatones y deténgase cuando corresponda. " +
                    "En zonas escolares, respete la velocidad indicada " +
                    "y tenga cuidado con los niños."
        ),
        KdTranslatedRule(
            "signs",
            "safety",
            "Señales de tráfico coreanas",
            "Revise las marcas viales y las señales complementarias.",
            "Observe las señales de tráfico y las marcas viales. " +
                    "Si una señal no está clara, evite los giros bruscos " +
                    "y continúe con seguridad."
        ),
        KdTranslatedRule(
            "toll",
            "essentials",
            "Peajes y Hi-Pass",
            "Elija el carril adecuado para el método de pago.",
            "Elija un carril de peaje apropiado para su vehículo. " +
                    "No frene bruscamente ni dé marcha atrás en un peaje."
        ),
        KdTranslatedRule(
            "bus",
            "essentials",
            "Carriles exclusivos para autobuses",
            "Compruebe las restricciones y sus horarios.",
            "Las restricciones de los carriles de autobús pueden variar " +
                    "según la zona y el horario. Revise las señales antes de entrar."
        )
    )

    else -> listOf(
        KdTranslatedRule(
            "roundabout",
            "intersections",
            "Roundabout",
            "Yield to circulating vehicles.",
            "Slow down. Yield to vehicles already in the roundabout. " +
                    "Enter only when it is safe."
        ),
        KdTranslatedRule(
            "left_turn",
            "intersections",
            "Unprotected Left Turn",
            "Follow signals and yield before turning.",
            "Check the traffic signal. Yield to oncoming traffic and pedestrians. " +
                    "Turn only when permitted and safe."
        ),
        KdTranslatedRule(
            "right_turn",
            "intersections",
            "Right Turn",
            "Check signals and pedestrian crossings.",
            "Check the right turn signals and pedestrians. " +
                    "Stop when required. Turn only when it is safe."
        ),
        KdTranslatedRule(
            "crossing",
            "safety",
            "Crosswalk & School Zone",
            "Watch for pedestrians and posted speed limits.",
            "Watch for pedestrians. Stop when required at crossings. " +
                    "In school zones, follow posted limits and watch for children."
        ),
        KdTranslatedRule(
            "signs",
            "safety",
            "Korean Road Signs",
            "Check road markings and supplementary signs.",
            "Check traffic signs and road markings. " +
                    "If a sign is unclear, avoid sudden turns and continue safely."
        ),
        KdTranslatedRule(
            "toll",
            "essentials",
            "Tollgate & Hi-Pass",
            "Check payment lanes before entering.",
            "Choose a payment lane that suits your vehicle. " +
                    "Do not stop suddenly or reverse at a tollgate."
        ),
        KdTranslatedRule(
            "bus",
            "essentials",
            "Bus-Only Lane",
            "Check lane restrictions and operating hours.",
            "Bus-only lane restrictions may vary. " +
                    "Check the signs and operating hours before entering."
        )
    )
}

// =====================================================
// Read language selected on Welcome
// =====================================================

@Composable
private fun kdCurrentRulesLanguage(): String {
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

// =====================================================
// Main screen
// =====================================================

@Composable
fun TrafficRulesScreen(
    onBack: () -> Unit,
    onSpeakRequest: (String) -> Unit,
    onStopRequest: () -> Unit,
    interactionRestricted: Boolean = true,
    uiLanguage: String? = null
) {
    val language = uiLanguage ?: kdCurrentRulesLanguage()
    val words = kdRulesWords(language)

    val allRules = remember(language) {
        kdTranslatedRules(language)
    }

    val colors = MaterialTheme.colorScheme

    if (interactionRestricted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                words.title,
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                words.restricted,
                style = MaterialTheme.typography.titleLarge
            )
        }
        return
    }

    var selectedGroup by rememberSaveable {
        mutableStateOf(kdRuleGroupIds.first())
    }

    var selectedRuleId by rememberSaveable {
        mutableStateOf("roundabout")
    }

    var lastSpeech by rememberSaveable {
        mutableStateOf("")
    }

    var statusIndex by rememberSaveable {
        mutableStateOf(0)
    }

    val visible = allRules.filter {
        it.group == selectedGroup
    }

    val selected = visible.firstOrNull {
        it.id == selectedRuleId
    } ?: visible.first()

    val latestStop by rememberUpdatedState(onStopRequest)

    DisposableEffect(Unit) {
        onDispose {
            latestStop()
        }
    }

    val selectGroup: (String) -> Unit = { group ->
        if (group != selectedGroup) {
            onStopRequest()

            selectedGroup = group

            selectedRuleId = allRules.first {
                it.group == group
            }.id

            lastSpeech = ""
            statusIndex = 1
        }
    }

    val selectRule: (KdTranslatedRule) -> Unit = { rule ->
        if (rule.id != selectedRuleId) {
            onStopRequest()
            selectedRuleId = rule.id
            lastSpeech = ""
            statusIndex = 2
        }
    }

    val play: () -> Unit = {
        onStopRequest()

        lastSpeech = selected.speech
        onSpeakRequest(selected.speech)

        // Request sent, not proof of actual audio.
        statusIndex = 3
    }

    val stop: () -> Unit = {
        onStopRequest()
        statusIndex = 4
    }

    val replay: () -> Unit = {
        if (lastSpeech.isNotBlank()) {
            onStopRequest()
            onSpeakRequest(lastSpeech)
            statusIndex = 5
        }
    }

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
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                KDriveActionButton(
                    label = words.back,
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
                        words.title,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        words.subtitle,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Group tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(12.dp)
            ) {
                kdRuleGroupIds.forEachIndexed {
                        index, group ->

                    KDriveActionButton(
                        label = words.groups[index],
                        onClick = {
                            selectGroup(group)
                        },
                        modifier = Modifier.weight(1f),
                        filled = selectedGroup == group
                    )
                }
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
                            .weight(0.95f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        visible.forEach { rule ->
                            KdRuleCard(
                                rule,
                                rule.id == selected.id
                            ) {
                                selectRule(rule)
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.05f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        KdRuleDetail(
                            rule = selected,
                            words = words,
                            status = words.messages[statusIndex],
                            canReplay = lastSpeech.isNotBlank(),
                            onPlay = play,
                            onStop = stop,
                            onReplay = replay
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement =
                        Arrangement.spacedBy(14.dp)
                ) {
                    visible.forEach { rule ->
                        KdRuleCard(
                            rule,
                            rule.id == selected.id
                        ) {
                            selectRule(rule)
                        }
                    }

                    KdRuleDetail(
                        rule = selected,
                        words = words,
                        status = words.messages[statusIndex],
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

@Composable
private fun KdRuleCard(
    rule: KdTranslatedRule,
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
            containerColor = if (selected) {
                colors.primaryContainer
            } else {
                colors.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                rule.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) {
                    colors.primary
                } else {
                    colors.onSurface
                },
                maxLines = 2
            )

            Text(
                rule.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun KdRuleDetail(
    rule: KdTranslatedRule,
    words: KdRulesWords,
    status: String,
    canReplay: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
    onReplay: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            rule.title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 2
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    words.keyPoint,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )

                Text(
                    rule.summary,
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    words.voiceScript,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )

                Text(
                    rule.speech,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(KDriveUiSize.actionGap)
        ) {
            KDriveActionButton(
                words.play,
                onPlay,
                modifier = Modifier.weight(1f)
            )

            KDriveActionButton(
                words.stop,
                onStop,
                modifier = Modifier.weight(1f),
                filled = false
            )
        }

        KDriveActionButton(
            words.replay,
            onReplay,
            modifier = Modifier.fillMaxWidth(),
            filled = false,
            enabled = canReplay
        )

        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2
        )
    }
}

@Preview(
    name = "Traffic - French",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdFrenchRulesPreview() {
    KDriveTheme(KDRIVE_THEME_PRESETS.first()) {
        TrafficRulesScreen(
            onBack = {},
            onSpeakRequest = {},
            onStopRequest = {},
            interactionRestricted = false,
            uiLanguage = "FR"
        )
    }
}
