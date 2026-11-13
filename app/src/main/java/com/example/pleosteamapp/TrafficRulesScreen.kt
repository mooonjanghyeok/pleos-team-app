package com.example.pleosteamapp

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * K-DRIVE GUIDE | DESIGN STEP 4/6
 * TRAFFIC RULES CLEAN V3
 *
 * - Right detail panel vertical alignment adjusted
 * - "상황 선택" header moved above both columns
 * - Replay removed, Listen replays from start
 * - Existing TTS/back/restriction callbacks preserved
 */

private data class KdrL(
    val en: String,
    val ko: String,
    val fr: String,
    val es: String
) {
    fun get(lang: String): String = when (lang) {
        "KO" -> ko
        "FR" -> fr
        "ES" -> es
        else -> en
    }
}

private fun kdr(
    en: String,
    ko: String,
    fr: String,
    es: String
) = KdrL(en, ko, fr, es)

private data class KdrGroup(
    val id: String,
    val title: KdrL
)

private val kdrGroups = listOf(
    KdrGroup(
        "junction",
        kdr(
            "Intersections",
            "교차로",
            "Intersections",
            "Intersecciones"
        )
    ),
    KdrGroup(
        "safety",
        kdr(
            "Road Safety",
            "안전운전",
            "Sécurité routière",
            "Seguridad vial"
        )
    ),
    KdrGroup(
        "basics",
        kdr(
            "Essentials",
            "운전 기본",
            "L'essentiel",
            "Conceptos básicos"
        )
    )
)

private data class KdrRule(
    val id: String,
    val group: String,
    val title: KdrL,
    val keyPoint: KdrL,
    val guidance: KdrL
)

private val kdrRules = listOf(
    KdrRule(
        "roundabout",
        "junction",
        kdr("Roundabout", "회전교차로", "Rond-point", "Rotonda"),
        kdr(
            "Yield to circulating traffic.",
            "이미 회전 중인 차량이 우선입니다.",
            "Priorité aux véhicules déjà engagés.",
            "Tienen prioridad los vehículos que circulan."
        ),
        kdr(
            "Slow down before entering. Yield to vehicles already in the roundabout and check for pedestrians. Enter only when safe.",
            "진입 전 속도를 줄이세요. 이미 회전 중인 차량에 양보하고 보행자를 확인한 뒤 안전할 때 진입하세요.",
            "Ralentissez avant d'entrer. Cédez le passage aux véhicules déjà engagés et surveillez les piétons.",
            "Reduzca la velocidad antes de entrar. Ceda el paso a los vehículos que circulan y compruebe si hay peatones."
        )
    ),
    KdrRule(
        "left_turn",
        "junction",
        kdr(
            "Unprotected Left Turn",
            "비보호 좌회전",
            "Virage à gauche sans priorité (« 비보호 »)",
            "Giro a la izquierda no protegido"
        ),
        kdr(
            "Check the signal and yield.",
            "신호를 확인하고 대향 차량에 양보하세요.",
            "Vérifiez les feux et cédez le passage.",
            "Compruebe el semáforo y ceda el paso."
        ),
        kdr(
            "In Korea, a “비보호” sign permits an unprotected left turn on a circular green light. Yield to oncoming traffic and pedestrians, and turn only when safe. Do not turn left on red.",
            "한국에서 ‘비보호’ 좌회전 표지가 있으면 원형 녹색신호에서 마주 오는 차량과 보행자에게 양보한 뒤 안전할 때 좌회전하세요. 적색신호에서는 좌회전하지 마세요.",
            "En Corée, le panneau « 비보호 » autorise le virage à gauche au feu vert circulaire, sans priorité. Cédez le passage aux véhicules venant en sens inverse et aux piétons. Ne tournez pas au rouge.",
            "En Corea, la señal «비보호» permite girar a la izquierda con luz verde circular, sin prioridad. Ceda el paso al tráfico que viene de frente y a los peatones. No gire a la izquierda con el semáforo en rojo."
        )
    ),
    KdrRule(
        "right_turn",
        "junction",
        kdr("Right Turn", "우회전", "Tourner à droite", "Giro a la derecha"),
        kdr(
            "Check pedestrians and signals.",
            "보행자와 신호를 먼저 확인하세요.",
            "Surveillez les piétons et les feux.",
            "Compruebe si hay peatones y respete los semáforos."
        ),
        kdr(
            "In Korea, first stop completely at a red vehicle signal: before the stop line, or before the crosswalk if there is no line, or just before the intersection if neither is present. If there is a dedicated right-turn signal, turn only on its green arrow. Otherwise, turn slowly only when you will not obstruct pedestrians or other traffic proceeding on its signal.",
            "한국에서 전방 차량 신호가 적색이면 먼저 정지선 앞에서 완전히 정지하세요. 정지선이 없으면 횡단보도 앞, 둘 다 없으면 교차로 직전에 정지하세요. 우회전 전용 신호등이 있으면 녹색 화살표에만 진행하세요. 전용 신호등이 없으면 보행자와 신호에 따라 진행하는 다른 교통을 방해하지 않을 때만 서행하여 우회전하세요.",
            "En Corée, au feu rouge, marquez un arrêt complet avant la ligne, à défaut avant le passage piéton, ou juste avant le carrefour si aucun des deux n’est présent. Avec un feu dédié au virage à droite, attendez sa flèche verte. Sans ce feu, tournez lentement uniquement sans gêner les piétons ni les véhicules autorisés à passer.",
            "En Corea, con el semáforo en rojo, deténgase por completo antes de la línea de detención; si no la hay, antes del paso de peatones, o justo antes del cruce si no hay ninguno. Si existe un semáforo específico de giro a la derecha, espere su flecha verde. En caso contrario, gire despacio solo sin obstaculizar a los peatones ni al tráfico que tiene permitido pasar."
        )
    ),
    KdrRule(
        "signals",
        "junction",
        kdr("Flashing Traffic Lights", "점멸 신호등", "Feux clignotants", "Semáforos intermitentes"),
        kdr(
            "Red: stop. Yellow: caution.",
            "적색 점멸은 일시정지, 황색 점멸은 주의.",
            "Rouge : arrêt. Jaune : prudence.",
            "Rojo: pare. Amarillo: precaución."
        ),
        kdr(
            "At a flashing red light, stop and check for traffic. At flashing yellow, slow down and proceed carefully.",
            "적색 점멸 신호에서는 일시정지하고 안전을 확인하세요. 황색 점멸 신호에서는 서행하며 주변 차량을 주의하세요.",
            "Au rouge clignotant, arrêtez-vous et vérifiez la circulation. Au jaune clignotant, ralentissez et avancez prudemment.",
            "Con luz roja intermitente, deténgase y compruebe el tráfico. Con amarilla, reduzca la velocidad y avance con precaución."
        )
    ),
    KdrRule(
        "yield",
        "junction",
        kdr("Yield & Merge", "양보 및 합류", "Céder le passage et s'insérer", "Ceder el paso e incorporarse"),
        kdr(
            "Signal and check blind spots.",
            "방향지시등과 사각지대를 확인하세요.",
            "Clignotant et angles morts.",
            "Intermitente y ángulos muertos."
        ),
        kdr(
            "Use your turn signal. Check mirrors and blind spots, yield as needed and merge only when there is enough space.",
            "방향지시등을 켜고 거울과 사각지대를 확인하세요. 필요한 경우 양보하고 충분한 공간이 있을 때 합류하세요.",
            "Mettez le clignotant, vérifiez les rétroviseurs et les angles morts, cédez le passage si nécessaire, puis insérez-vous lorsqu’il y a suffisamment d’espace.",
            "Use el intermitente, revise los espejos y los ángulos muertos. Ceda el paso e incorpórese cuando sea seguro."
        )
    ),
    KdrRule(
        "crossing",
        "safety",
        kdr(
            "Crosswalk & School Zone",
            "횡단보도·어린이보호구역",
            "Passages piétons et zones scolaires",
            "Pasos de peatones y zonas escolares"
        ),
        kdr(
            "Watch for pedestrians and children.",
            "보행자와 어린이를 우선 보호하세요.",
            "Attention aux piétons et aux enfants.",
            "Atención a peatones y niños."
        ),
        kdr(
            "Stop before a crosswalk for pedestrians who are crossing or about to cross. At a crosswalk without traffic signals inside a Korean child-protection zone, stop even when no pedestrian is present. Obey the posted speed limit.",
            "횡단보도를 건너거나 건너려는 보행자가 있으면 횡단보도 앞에서 일시정지하세요. 한국의 어린이보호구역 안에서 신호기가 없는 횡단보도는 보행자가 없어도 일시정지해야 합니다. 현장의 제한속도도 지키세요.",
            "Arrêtez-vous avant le passage piéton si une personne traverse ou s’apprête à traverser. Dans une zone coréenne de protection des enfants, arrêtez-vous à tout passage piéton sans feux, même sans piéton. Respectez la limitation de vitesse affichée.",
            "Deténgase antes del paso de peatones si alguien cruza o va a cruzar. En una zona coreana de protección infantil, deténgase ante los pasos sin semáforo aunque no haya peatones. Respete el límite de velocidad indicado."
        )
    ),
    KdrRule(
        "distance",
        "safety",
        kdr("Following & Lane Changes", "안전거리·차로 변경", "Distance et changement de voie", "Distancia y cambio de carril"),
        kdr(
            "Keep your distance and signal early.",
            "안전거리를 유지하고 미리 신호하세요.",
            "Gardez vos distances et signalez tôt.",
            "Mantenga la distancia y señalice antes."
        ),
        kdr(
            "Keep a safe following distance. Before changing lanes, signal, check mirrors and blind spots, and avoid sudden moves.",
            "앞차와 안전거리를 유지하세요. 차로를 바꾸기 전 방향지시등을 켜고 거울과 사각지대를 확인하며 급격한 조작을 피하세요.",
            "Gardez une distance de sécurité. Avant de changer de voie, signalez, vérifiez les rétroviseurs et les angles morts.",
            "Mantenga una distancia segura. Antes de cambiar de carril, señalice y revise espejos y ángulos muertos."
        )
    ),
    KdrRule(
        "signs",
        "safety",
        kdr("Road Signs & Markings", "도로표지·노면 표시", "Panneaux et marquages", "Señales y marcas viales"),
        kdr(
            "Follow current signs and road markings.",
            "현장 표지와 노면 표시를 따르세요.",
            "Respectez panneaux et marquages.",
            "Respete señales y marcas viales."
        ),
        kdr(
            "Follow road signs, markings and supplementary conditions. If a sign is unclear, avoid abrupt manoeuvres while still obeying any stop or entry restriction. Check its meaning after parking legally and safely.",
            "도로표지와 노면 표시, 보조표지의 조건을 따르세요. 뜻이 불분명하면 급격한 조작을 피하면서 현재의 정지·통행금지 지시를 준수하세요. 합법적이고 안전하게 주차한 뒤 의미를 확인하세요.",
            "Respectez les panneaux, les marquages et les panonceaux. En cas de doute, évitez les manœuvres brusques tout en respectant les obligations d’arrêt et les interdictions de passage. Vérifiez le sens du panneau une fois garé légalement et en sécurité.",
            "Respete las señales, las marcas viales y los paneles complementarios. Si tiene dudas, evite maniobras bruscas sin ignorar las obligaciones de detenerse ni las prohibiciones de paso. Compruebe el significado después de aparcar de forma legal y segura."
        )
    ),
    KdrRule(
        "toll",
        "basics",
        kdr("Tollgate & Hi-Pass", "톨게이트·하이패스", "Péages et Hi-Pass", "Peajes y Hi-Pass"),
        kdr(
            "Choose a compatible payment lane.",
            "결제 방식에 맞는 차로를 선택하세요.",
            "Choisissez la voie de paiement adaptée.",
            "Elija el carril de pago adecuado."
        ),
        kdr(
            "Check your Hi-Pass device and payment method before entering a tollgate. Never stop suddenly or reverse to change lanes.",
            "톨게이트 진입 전 하이패스 단말기와 결제 수단을 확인하세요. 차로를 바꾸려고 급정지하거나 후진하지 마세요.",
            "Vérifiez votre dispositif Hi-Pass et votre moyen de paiement avant le péage. Ne freinez pas brusquement et ne reculez pas.",
            "Compruebe el dispositivo Hi-Pass y el pago antes del peaje. No frene de golpe ni dé marcha atrás."
        )
    ),
    KdrRule(
        "bus",
        "basics",
        kdr("Bus-Only Lanes", "버스전용차로", "Voies réservées aux bus", "Carriles exclusivos para autobuses"),
        kdr(
            "Restrictions depend on signs and hours.",
            "운영 시간과 이용 조건을 확인하세요.",
            "Vérifiez horaires et conditions.",
            "Compruebe horarios y restricciones."
        ),
        kdr(
            "Bus-only lane rules may depend on location, time and vehicle type. Check posted signs before entering.",
            "버스전용차로의 적용 시간과 구간, 차종 조건은 다를 수 있습니다. 진입 전 현장 표지를 확인하세요.",
            "Les restrictions varient selon le lieu, l'heure et le véhicule. Vérifiez les panneaux avant d'entrer.",
            "Las restricciones pueden variar según el lugar, horario y vehículo. Revise las señales antes de entrar."
        )
    ),
    KdrRule(
        "parking",
        "basics",
        kdr("Parking Rules", "주차 관련 규칙", "Règles de stationnement", "Normas de aparcamiento"),
        kdr(
            "Park only where permitted.",
            "허용된 장소에 주차하세요.",
            "Stationnez uniquement où c'est autorisé.",
            "Aparque solo donde esté permitido."
        ),
        kdr(
            "Check parking signs and use designated spaces. Do not block crosswalks, intersections or vehicle entrances.",
            "주차 가능 표지를 확인하고 지정된 구역을 이용하세요. 횡단보도, 교차로 및 차량 출입구를 막지 마세요.",
            "Vérifiez les panneaux et utilisez les places autorisées. Ne bloquez ni passages piétons, ni carrefours, ni accès.",
            "Compruebe las señales y use plazas autorizadas. No bloquee pasos de peatones, cruces ni accesos."
        )
    )
)

private data class KdrUi(
    val title: String,
    val subtitle: String,
    val back: String,
    val choose: String,
    val point: String,
    val explanation: String,
    val listen: String,
    val stop: String,
    val requestSent: String,
    val stopSent: String,
    val parked: String,
    val restricted: String
)

private fun kdrUi(lang: String): KdrUi = when (lang) {
    "KO" -> KdrUi(
        "교통규칙",
        "정차 중 학습 · 11개 상황",
        "← 뒤로",
        "상황 선택",
        "핵심 규칙",
        "상황 안내",
        "음성 듣기",
        "중지",
        "음성 재생을 요청했습니다.",
        "중지를 요청했습니다.",
        "정차 중에 확인하세요. 현장 신호와 표지가 우선합니다.",
        "주행 중에는 교통규칙 학습 화면을 이용할 수 없습니다."
    )
    "FR" -> KdrUi(
        "Code de la route",
        "À consulter à l'arrêt · 11 situations",
        "← Retour",
        "Choisir une situation",
        "Règle essentielle",
        "Conseils",
        "Écouter",
        "Arrêter",
        "Lecture audio demandée.",
        "Arrêt demandé.",
        "À consulter à l'arrêt. Respectez les feux et panneaux sur place.",
        "Ce guide est indisponible pendant la conduite."
    )
    "ES" -> KdrUi(
        "Normas de tráfico",
        "Solo estacionado · 11 situaciones",
        "← Volver",
        "Elegir una situación",
        "Regla clave",
        "Orientación",
        "Escuchar",
        "Detener",
        "Reproducción solicitada.",
        "Se ha solicitado detener el audio.",
        "Consúltelo estacionado. Respete las señales y semáforos locales.",
        "La guía no está disponible mientras conduce."
    )
    else -> KdrUi(
        "Traffic Rules",
        "Parked learning · 11 situations",
        "← Back",
        "Choose a situation",
        "Key point",
        "What to do",
        "Listen",
        "Stop",
        "Voice playback requested.",
        "Stop requested.",
        "Review while parked. Follow current signs and signals.",
        "Traffic rule lessons are unavailable while driving."
    )
}

@Composable
private fun kdrLanguage(): String {
    val context = LocalContext.current

    val saved = remember(context) {
        context.applicationContext
            .getSharedPreferences(
                "kdrive_ui_colors_v2",
                Context.MODE_PRIVATE
            )
            .getString("startup_language", null)
    }

    val raw = saved ?: Locale.getDefault().language

    return when (raw.uppercase(Locale.ROOT)) {
        "KO" -> "KO"
        "FR" -> "FR"
        "ES" -> "ES"
        else -> "EN"
    }
}

@Composable
private fun KdrCategoryTab(
    title: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Button(
        onClick = onClick,
        modifier = Modifier
            .heightIn(min = KDriveScreenStyle.touchHeight)
            .widthIn(min = 170.dp),
        shape = RoundedCornerShape(KDriveScreenStyle.buttonRadius),
        contentPadding = PaddingValues(
            horizontal = 18.dp,
            vertical = 12.dp
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) colors.primaryContainer else colors.surface,
            contentColor = if (active) colors.onPrimaryContainer else colors.onSurface
        )
    ) {
        Text(
            title,
            fontSize = 19.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2
        )
    }
}

@Composable
private fun KdrRuleItem(
    rule: KdrRule,
    lang: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .heightIn(min = 88.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(KDriveScreenStyle.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) colors.primaryContainer else colors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    rule.title.get(lang),
                    fontSize = 20.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    rule.keyPoint.get(lang),
                    fontSize = 15.sp,
                    lineHeight = 20.sp,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                "›",
                fontSize = 30.sp,
                color = colors.primary
            )
        }
    }
}

@Composable
private fun KdrRuleDetail(
    rule: KdrRule,
    lang: String,
    labels: KdrUi
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KDriveScreenStyle.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(KDriveScreenStyle.cardPadding),
            verticalArrangement = Arrangement.spacedBy(17.dp)
        ) {
            Text(
                rule.title.get(lang),
                fontSize = 29.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            if (lang != "KO") {
                Text(
                    rule.title.ko,
                    fontSize = 17.sp,
                    color = colors.onSurfaceVariant
                )
            }

            HorizontalDivider(
                color = colors.outlineVariant
            )

            Text(
                labels.point,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )

            Text(
                rule.keyPoint.get(lang),
                fontSize = 23.sp,
                lineHeight = 31.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        colors.primary.copy(alpha = 0.07f),
                        RoundedCornerShape(15.dp)
                    )
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    labels.explanation,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary
                )

                Text(
                    rule.guidance.get(lang),
                    fontSize = 19.sp,
                    lineHeight = 29.sp,
                    color = colors.onSurface
                )
            }

            Text(
                labels.parked,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = colors.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun KdrVoiceActions(
    labels: KdrUi,
    note: String,
    onListen: () -> Unit,
    onStop: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KDrivePrimaryAction(
            label = "▶  ${labels.listen}",
            onClick = onListen,
            modifier = Modifier.weight(1f)
        )

        KDriveSecondaryAction(
            label = "■  ${labels.stop}",
            onClick = onStop,
            modifier = Modifier.weight(1f)
        )
    }

    if (note.isNotBlank()) {
        Text(
            text = note,
            modifier = Modifier.padding(top = 8.dp),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
fun TrafficRulesScreen(
    onBack: () -> Unit,
    onSpeakRequest: (String) -> Unit,
    onStopRequest: () -> Unit,
    interactionRestricted: Boolean = true
) {
    val colors = MaterialTheme.colorScheme
    val lang = kdrLanguage()
    val labels = kdrUi(lang)

    if (interactionRestricted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                labels.title,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )

            Text(
                labels.restricted,
                fontSize = 21.sp,
                lineHeight = 30.sp,
                color = colors.onBackground
            )
        }
        return
    }

    DisposableEffect(Unit) {
        onDispose {
            onStopRequest()
        }
    }

    var selectedGroup by rememberSaveable {
        mutableStateOf("junction")
    }

    var selectedRuleId by rememberSaveable {
        mutableStateOf("roundabout")
    }

    var voiceNote by remember {
        mutableStateOf("")
    }

    val visibleRules = remember(selectedGroup) {
        kdrRules.filter { it.group == selectedGroup }
    }

    val selectedRule =
        visibleRules.firstOrNull { it.id == selectedRuleId } ?: visibleRules.first()

    val chooseGroup: (String) -> Unit = { next ->
        if (selectedGroup != next) {
            onStopRequest()
            selectedGroup = next
            selectedRuleId = kdrRules.first { it.group == next }.id
            voiceNote = ""
        }
    }

    val chooseRule: (String) -> Unit = { next ->
        if (selectedRuleId != next) {
            onStopRequest()
            selectedRuleId = next
            voiceNote = ""
        }
    }

    val listen: () -> Unit = {
        onStopRequest()
        onSpeakRequest(selectedRule.guidance.get(lang))
        voiceNote = labels.requestSent
    }

    val stop: () -> Unit = {
        onStopRequest()
        voiceNote = labels.stopSent
    }

    val leave: () -> Unit = {
        onStopRequest()
        onBack()
    }

    BackHandler {
        leave()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val wide = maxWidth >= 760.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = KDriveScreenStyle.pageHorizontal,
                    vertical = KDriveScreenStyle.pageVertical
                ),
            verticalArrangement = Arrangement.spacedBy(
                KDriveScreenStyle.sectionGap
            )
        ) {
            KDriveScreenHeader(
                title = labels.title,
                subtitle = labels.subtitle,
                backLabel = labels.back,
                onBack = leave
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                kdrGroups.forEach { group ->
                    KdrCategoryTab(
                        title = group.title.get(lang),
                        active = group.id == selectedGroup,
                        onClick = { chooseGroup(group.id) }
                    )
                }
            }

            if (wide) {
                Text(
                    labels.choose,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onBackground
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        visibleRules.forEach { rule ->
                            KdrRuleItem(
                                rule = rule,
                                lang = lang,
                                selected = rule.id == selectedRule.id,
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { chooseRule(rule.id) }
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                        ) {
                            KdrRuleDetail(
                                rule = selectedRule,
                                lang = lang,
                                labels = labels
                            )
                        }

                        KdrVoiceActions(
                            labels = labels,
                            note = voiceNote,
                            onListen = listen,
                            onStop = stop
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        labels.choose,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onBackground
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            visibleRules,
                            key = { it.id }
                        ) { rule ->
                            KdrRuleItem(
                                rule = rule,
                                lang = lang,
                                selected = rule.id == selectedRule.id,
                                modifier = Modifier.width(260.dp),
                                onClick = { chooseRule(rule.id) }
                            )
                        }
                    }

                    KdrRuleDetail(
                        rule = selectedRule,
                        lang = lang,
                        labels = labels
                    )

                    KdrVoiceActions(
                        labels = labels,
                        note = voiceNote,
                        onListen = listen,
                        onStop = stop
                    )
                }
            }
        }
    }
}