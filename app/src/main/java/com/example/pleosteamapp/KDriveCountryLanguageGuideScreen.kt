package com.example.pleosteamapp

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/*
 * BEFORE YOU DRIVE — COUNTRY BRIEFING V1
 *
 * One dataset for the three-item briefing and all eight topics.
 * The order is editorial, NOT an accident-risk prediction.
 *
 * Country and guide language are saved independently
 * of PC1 TTS settings.
 *
 * No real-time permission decisions or automatic speech.
 */

private data class BpText(
    val ko: String,
    val en: String,
    val fr: String,
    val es: String
) {
    fun at(lang: String): String = when (lang) {
        "KO" -> ko
        "FR" -> fr
        "ES" -> es
        else -> en
    }
}

private fun bp(
    ko: String,
    en: String,
    fr: String,
    es: String
) = BpText(ko, en, fr, es)

private fun bpLanguage(raw: String?): String = when (
    raw.orEmpty()
        .replace('_', '-')
        .substringBefore('-')
        .uppercase(Locale.ROOT)
) {
    "KO" -> "KO"
    "FR" -> "FR"
    "ES" -> "ES"
    else -> "EN"
}

private fun bpCountry(raw: String?): String = when (raw) {
    "UK", "US", "FR", "ES", "JP", "CN", "OTHER" -> raw
    null, "" -> ""
    else -> "OTHER"
}

private val bpCountries = listOf(
    "UK" to bp(
        "영국",
        "United Kingdom",
        "Royaume-Uni",
        "Reino Unido"
    ),
    "US" to bp(
        "미국 · 캘리포니아",
        "USA · California",
        "États-Unis · Californie",
        "EE. UU. · California"
    ),
    "FR" to bp(
        "프랑스",
        "France",
        "France",
        "Francia"
    ),
    "ES" to bp(
        "스페인",
        "Spain",
        "Espagne",
        "España"
    ),
    "JP" to bp(
        "일본",
        "Japan",
        "Japon",
        "Japón"
    ),
    "CN" to bp(
        "중국 본토",
        "Mainland China",
        "Chine continentale",
        "China continental"
    ),
    "OTHER" to bp(
        "기타 / 여러 국가",
        "Other / several countries",
        "Autre / plusieurs pays",
        "Otro / varios países"
    )
)

private data class BpTopic(
    val id: String,
    val title: BpText,
    val takeaway: BpText,
    val korea: BpText,
    val source: String
)

private val bpTopics = listOf(
    BpTopic(
        id = "side",
        title = bp(
            "통행 방향·속도 단위",
            "Driving side & speed units",
            "Sens de circulation et unités",
            "Sentido y unidades de velocidad"
        ),
        takeaway = bp(
            "한국은 우측통행, 속도 표시는 km/h입니다.",
            "Korea drives on the right and uses km/h.",
            "En Corée, on roule à droite. Les vitesses sont indiquées en km/h.",
            "En Corea: circulación por la derecha y velocidades en km/h."
        ),
        korea = bp(
            "교차로를 돌아 나온 뒤에도 도로 오른쪽으로 주행하세요. 계기판과 제한속도 표지의 단위를 확인하세요.",
            "Keep to the right after turning at an intersection. Check the units on the speedometer and speed-limit signs.",
            "Après avoir tourné à un carrefour, circulez du côté droit de la chaussée. Vérifiez les unités du compteur de vitesse et des panneaux de limitation.",
            "Manténgase a la derecha después de girar. Compruebe las unidades del velocímetro y de los límites."
        ),
        source =
            "KR: 도로교통법 제13조 · UK: Highway Code 160 / Speed limits · US: California DMV"
    ),

    BpTopic(
        id = "roundabout",
        title = bp(
            "회전교차로",
            "Roundabouts",
            "Carrefours giratoires",
            "Glorietas"
        ),
        takeaway = bp(
            "반시계 방향으로 돌고, 이미 회전 중인 차량에 양보하세요.",
            "Travel counterclockwise; yield to traffic already circulating.",
            "Circulez dans le sens antihoraire et cédez le passage aux véhicules déjà engagés.",
            "Circule en sentido antihorario y ceda al tráfico que ya circula."
        ),
        korea = bp(
            "진입할 때 서행하거나 일시정지하고 회전 중인 차량에 양보하세요. 현장의 차선·표지·신호도 확인하세요.",
            "Slow down or stop on entry and yield to circulating vehicles. Check local lane markings, signs and signals.",
            "Ralentissez ou arrêtez-vous à l'entrée et cédez le passage. Vérifiez les voies, panneaux et feux.",
            "Reduzca la velocidad o deténgase al entrar y ceda el paso. Compruebe carriles, señales y semáforos."
        ),
        source =
            "KR: 도로교통법 제25조의2 · UK: Highway Code 184–190 · US: California DMV §7"
    ),

    BpTopic(
        id = "red",
        title = bp(
            "적색신호에서 우회전",
            "Right turns at red lights",
            "Tourner à droite au rouge",
            "Girar a la derecha con rojo"
        ),
        takeaway = bp(
            "먼저 정지하세요. 우회전 전용 신호등이 있으면 그 신호를 따르세요.",
            "Stop first. Obey a dedicated right-turn signal when present.",
            "Arrêtez-vous d'abord. Respectez le feu dédié au virage s'il existe.",
            "Pare primero. Obedezca el semáforo específico de giro si existe."
        ),
        korea = bp(
            "전방 차량 신호가 적색이면 정지선 앞에서 일시정지하세요. 우회전 전용 신호등이 있으면 녹색 화살표에만 진행하세요. 전용 신호등이 없어도 보행자와 다른 교통을 방해하지 않는 등 허용 조건을 충족해야 합니다.",
            "At a red vehicle signal, stop before the stop line. With a dedicated right-turn signal, wait for its green arrow. Without one, turn only when permitted and without interfering with pedestrians or other traffic.",
            "Au rouge, arrêtez-vous avant la ligne. Avec un feu dédié, attendez sa flèche verte. Sans ce feu, ne tournez que si cela est permis, sans gêner piétons ni autres véhicules.",
            "Con rojo, pare antes de la línea. Si hay semáforo de giro, espere su flecha verde. Sin él, gire solo cuando esté permitido y sin interferir con peatones ni otros vehículos."
        ),
        source =
            "KR: 경찰청 우회전 안내 · UK: Highway Code 176 · US: DMV §7 · FR: signalisation art. 7 · ES: DGT Flechas en señales"
    ),

    BpTopic(
        id = "left",
        title = bp(
            "‘비보호’ 좌회전",
            "The ‘비보호’ left-turn sign",
            "Le panneau « 비보호 »",
            "La señal «비보호»"
        ),
        takeaway = bp(
            "‘비보호’는 좌회전 우선권이 있다는 뜻이 아닙니다.",
            "‘비보호’ does not give you priority to turn left.",
            "« 비보호 » ne vous donne pas la priorité pour tourner à gauche.",
            "«비보호» no le da prioridad para girar a la izquierda."
        ),
        korea = bp(
            "비보호 좌회전이 허용된 곳에서는 해당 녹색신호에서 대향 차량과 보행자에게 양보하고 안전할 때 좌회전하세요. 적색신호에서 좌회전해도 된다는 표지가 아닙니다.",
            "Where an unprotected left turn is permitted, turn on the applicable green signal only after yielding to oncoming traffic and pedestrians. This sign does not permit a left turn on red.",
            "En Corée, le panneau « 비보호 » autorise le virage à gauche au feu vert circulaire, sans priorité. Cédez le passage aux véhicules venant en sens inverse et aux piétons. Ne tournez pas au rouge.",
            "En Corea, la señal «비보호» permite girar a la izquierda con luz verde circular, sin prioridad. Ceda el paso al tráfico que viene de frente y a los peatones. No gire a la izquierda con el semáforo en rojo."
        ),
        source =
            "KR: 경찰청 비보호 좌회전 안내 · US: California DMV §7 · UK: Highway Code 179–181"
    ),

    BpTopic(
        id = "crossing",
        title = bp(
            "어린이보호구역 횡단보도",
            "School-zone crosswalks",
            "Passages en zone scolaire",
            "Pasos en zona escolar"
        ),
        takeaway = bp(
            "어린이보호구역의 신호 없는 횡단보도에서는 사람이 없어도 일시정지하세요.",
            "Stop at an unsignalized school-zone crosswalk even with no pedestrians.",
            "En zone scolaire, arrêtez-vous au passage sans feux, même sans piéton.",
            "En zona escolar, pare ante un paso sin semáforo aunque no haya peatones."
        ),
        korea = bp(
            "이 의무는 ‘어린이보호구역 안의 신호기가 없는 횡단보도’에 해당합니다. 일반 횡단보도에서도 건너거나 건너려는 보행자가 있으면 일시정지해야 합니다.",
            "This rule applies to crosswalks without signals inside a Korean child-protection zone. At other crosswalks, stop for pedestrians crossing or about to cross.",
            "Cette règle vise les passages sans feux dans une zone coréenne de protection des enfants. Ailleurs aussi, arrêtez-vous pour les piétons qui traversent ou s'apprêtent à traverser.",
            "Esta norma se aplica a pasos sin semáforo dentro de una zona coreana de protección infantil. En otros pasos, pare ante peatones que cruzan o van a cruzar."
        ),
        source = "KR: 도로교통법 제27조 제1항·제7항"
    ),

    BpTopic(
        id = "signs",
        title = bp(
            "한국어 보조표지",
            "Korean supplementary signs",
            "Panonceaux en coréen",
            "Paneles complementarios en coreano"
        ),
        takeaway = bp(
            "‘비보호’·‘버스전용’과 시간·차종 조건을 함께 확인하세요.",
            "Check ‘비보호’, ‘버스전용’, operating times and vehicle conditions.",
            "Vérifiez « 비보호 », « 버스전용 », les horaires et les véhicules autorisés.",
            "Compruebe «비보호», «버스전용», los horarios y los vehículos permitidos."
        ),
        korea = bp(
            "‘비보호’는 비보호 좌회전, ‘버스전용’은 버스전용차로를 가리킵니다. 그림만 보지 말고 보조표지의 시간·대상 조건까지 확인하세요. 내비게이션의 길 안내는 통행 허가가 아닙니다.",
            "‘비보호’ identifies an unprotected left turn; ‘버스전용’ identifies a bus-only lane. Read supplementary conditions, not just the symbol. A navigation instruction is not permission to proceed.",
            "« 비보호 » indique un virage à gauche non protégé ; « 버스전용 », une voie de bus. Lisez aussi les conditions. Une instruction GPS n'est pas une autorisation de passer.",
            "«비보호» indica giro a la izquierda no protegido; «버스전용», carril bus. Lea también las condiciones. Una indicación del navegador no autoriza a pasar."
        ),
        source =
            "KR: 도로교통법 시행규칙 안전표지 · 경찰청 비보호 좌회전 안내"
    ),

    BpTopic(
        id = "toll",
        title = bp(
            "하이패스·통행료",
            "Hi-Pass & tolls",
            "Hi-Pass et péages",
            "Hi-Pass y peajes"
        ),
        takeaway = bp(
            "렌터카 업체에 하이패스 이용·결제 방법을 확인하세요.",
            "Ask the rental company how Hi-Pass and toll payment work for your car.",
            "Demandez au loueur comment utiliser Hi-Pass et régler les péages.",
            "Pregunte a la empresa de alquiler cómo usar Hi-Pass y pagar los peajes."
        ),
        korea = bp(
            "차량의 단말기와 결제 설정을 확인한 뒤 맞는 차로를 선택하세요. 하이패스 차로에서 결제 문제가 생겨도 급정지·후진하지 마세요. 안전하게 통과한 뒤 운영사나 렌터카 업체에 확인하세요.",
            "Check the car's device and payment setup before choosing a lane. Do not brake suddenly or reverse because of a Hi-Pass payment problem. Pass safely, then contact the operator or rental company.",
            "Vérifiez le dispositif et le paiement avant de choisir la voie. En cas de problème Hi-Pass, ne freinez pas brusquement et ne reculez pas. Passez en sécurité, puis contactez l'exploitant ou le loueur.",
            "Compruebe el dispositivo y el pago antes de elegir carril. Ante un problema Hi-Pass, no frene de golpe ni retroceda. Pase con seguridad y consulte al operador o al arrendador."
        ),
        source =
            "KR: 대한민국 정책브리핑 ‘고속도로 통행료, 안 내고 통과했다면?’ · 서울경찰청 하이패스 안전안내"
    ),

    BpTopic(
        id = "bus",
        title = bp(
            "버스전용차로",
            "Bus-only lanes",
            "Voies de bus",
            "Carriles bus"
        ),
        takeaway = bp(
            "해외의 다인승차로 이용 조건을 한국에 그대로 적용하지 마세요.",
            "Do not apply foreign carpool-lane eligibility to Korean bus lanes.",
            "N'appliquez pas les règles étrangères de covoiturage aux voies de bus coréennes.",
            "No aplique las normas extranjeras de los carriles para vehículos de alta ocupación (VAO) a los carriles bus de Corea."
        ),
        korea = bp(
            "이용 조건은 도로·운영시간·차종에 따라 다릅니다. 고속도로 일부 조건은 등록 승차정원과 실제 탑승 인원도 따집니다. 일반 승용차가 여러 명을 태웠다는 이유만으로 진입하지 마세요.",
            "Eligibility varies by road, hours and vehicle type. Some expressway conditions also depend on registered seating capacity and occupants. Several passengers in an ordinary car do not automatically qualify it.",
            "L'accès dépend de la route, des horaires et du véhicule. Sur autoroute, places homologuées et occupants peuvent aussi compter. Plusieurs passagers ne suffisent pas à autoriser une voiture ordinaire.",
            "El acceso depende de la vía, horario y vehículo. En autopista pueden importar las plazas homologadas y los ocupantes. Llevar varios pasajeros no autoriza automáticamente a un turismo."
        ),
        source =
            "KR: 도로교통법 시행령 별표1 · US: California DMV §6 (HOV)"
    )
)

/*
 * A null comparison means:
 * show a Korea-only reminder, not an invented country difference.
 */
// KDRIVE_ASIA_COUNTRIES_V1

private fun bpAsiaComparison(
    country: String,
    id: String
): BpText? = when (country to id) {

    "JP" to "side" -> bp(
        "일본은 좌측통행, 한국은 우측통행입니다. 양국 모두 km/h를 사용합니다.",
        "Japan drives on the left; Korea drives on the right. Both use km/h.",
        "Au Japon, on roule à gauche ; en Corée, à droite. Les deux utilisent le km/h.",
        "En Japón se circula por la izquierda; en Corea, por la derecha. Ambos usan km/h."
    )

    "JP" to "roundabout" -> bp(
        "일본의 표지로 지정된 환상교차로는 시계 방향, 한국 회전교차로는 반시계 방향입니다. 진입 전 회전 중인 차량을 확인하세요.",
        "Signed Japanese roundabouts circulate clockwise; Korean roundabouts circulate counterclockwise. Check circulating traffic before entering.",
        "Les giratoires signalés du Japon tournent dans le sens horaire ; ceux de Corée, dans le sens antihoraire. Vérifiez le trafic engagé.",
        "Las glorietas señalizadas de Japón giran en sentido horario; las de Corea, en sentido antihorario. Compruebe el tráfico interior."
    )

    "JP" to "red" -> bp(
        "일본에서는 원칙적으로 적색신호에 회전할 수 없습니다. 허용된 화살표 신호가 있으면 이를 따르세요. 한국에서는 적색신호에 먼저 정지한 뒤 허용 조건을 확인해야 합니다.",
        "Japan generally prohibits turning on red unless an authorized signal permits it. In Korea, stop first at red and check the conditions before turning.",
        "Au Japon, tourner au rouge est généralement interdit sauf signal autorisé. En Corée, arrêtez-vous d'abord et vérifiez les conditions.",
        "En Japón normalmente no se gira con rojo salvo señal autorizada. En Corea, deténgase primero y compruebe las condiciones."
    )

    "JP" to "left" -> bp(
        "좌측통행인 일본에서는 대향차 흐름을 가로지르는 회전이 보통 우회전입니다. 한국에서는 좌회전 시 대향차와 비보호 표지의 조건을 확인하세요.",
        "In Japan, crossing oncoming traffic usually involves a right turn. In Korea, check oncoming traffic and unprotected-left-turn signs.",
        "Au Japon, on traverse généralement le trafic opposé en tournant à droite. En Corée, vérifiez les conditions du virage à gauche.",
        "En Japón, cruzar el tráfico contrario suele implicar girar a la derecha. En Corea, compruebe las condiciones para girar a la izquierda."
    )

    "JP" to "signs" -> bp(
        "한국에서는 ‘비보호’, ‘버스전용’ 등 한글 표지와 시간제한 조건을 확인해야 합니다.",
        "In Korea, learn Hangul signs such as '비보호' and '버스전용', including posted time restrictions.",
        "En Corée, apprenez les panneaux en hangeul comme « 비보호 » et « 버스전용 » et vérifiez les horaires.",
        "En Corea, aprenda las señales en hangul como «비보호» y «버스전용» y compruebe los horarios."
    )

    "JP" to "toll" -> bp(
        "일본 ETC와 한국 하이패스는 별개입니다. 렌터카의 하이패스 단말기 및 결제수단을 확인하세요.",
        "Japan's ETC and Korea's Hi-Pass are separate systems. Check the rental car's device and payment method.",
        "L'ETC japonais et le Hi-Pass coréen sont distincts. Vérifiez le dispositif et le paiement du véhicule loué.",
        "El ETC japonés y el Hi-Pass coreano son sistemas diferentes. Compruebe el dispositivo y el pago del coche alquilado."
    )

    "CN" to "side" -> bp(
        "중국 본토와 한국은 모두 우측통행이며 제한속도 단위도 km/h입니다.",
        "Mainland China and Korea both drive on the right and use km/h.",
        "La Chine continentale et la Corée roulent à droite et utilisent le km/h.",
        "China continental y Corea circulan por la derecha y usan km/h."
    )

    "CN" to "red" -> bp(
        "중국 본토의 일반 규정에서는 다른 교통을 방해하지 않는 조건으로 적색신호 우회전이 허용될 수 있습니다. 한국에서는 먼저 일시정지하고 우회전 전용 신호등이 있으면 그 신호를 따라야 합니다.",
        "Mainland China can permit right turns on red if other traffic is not impeded. In Korea, stop first and obey a dedicated right-turn signal when present.",
        "En Chine continentale, tourner à droite au rouge peut être permis sans gêner le trafic. En Corée, marquez l'arrêt et respectez tout feu dédié.",
        "En China continental se puede permitir girar a la derecha con rojo sin obstaculizar el tráfico. En Corea, deténgase primero y respete los semáforos específicos."
    )

    "CN" to "crossing" -> bp(
        "중국 본토에도 횡단보도 보행자 보호 규정이 있습니다. 한국의 어린이보호구역 내 신호 없는 횡단보도에서는 보행자가 없어도 일시정지해야 합니다.",
        "Mainland China also protects pedestrians. In Korea, stop at unsignalized crosswalks inside child-protection zones even when no pedestrian is present.",
        "La Chine continentale protège aussi les piétons. En Corée, l'arrêt est obligatoire aux passages sans feux dans les zones de protection des enfants, même sans piéton.",
        "China continental también protege a los peatones. En Corea, debe detenerse ante pasos sin semáforo en zonas de protección infantil incluso sin peatones."
    )

    "CN" to "signs" -> bp(
        "한국에서는 ‘비보호’, ‘버스전용’ 등 한글 표지와 요일·시간·차종별 제한사항을 확인하세요.",
        "In Korea, check Hangul signs such as '비보호' and '버스전용', plus restrictions by day, time or vehicle.",
        "En Corée, vérifiez les panneaux en hangeul comme « 비보호 » et « 버스전용 » ainsi que les restrictions.",
        "En Corea, compruebe las señales en hangul como «비보호» y «버스전용» y sus restricciones."
    )

    "CN" to "toll" -> bp(
        "중국 본토의 ETC와 한국의 하이패스는 별개입니다. 한국 차량의 단말기와 결제 설정을 확인하세요.",
        "Mainland China's ETC and Korea's Hi-Pass are separate systems. Check the vehicle's device and payment setup.",
        "L'ETC chinois et le Hi-Pass coréen sont distincts. Vérifiez le dispositif et le paiement.",
        "El ETC de China continental y el Hi-Pass de Corea son diferentes. Compruebe el dispositivo y el pago."
    )

    "CN" to "bus" -> bp(
        "중국 본토에도 버스전용차로가 있지만, 한국의 운영시간·차종·승차인원 조건과 같다고 가정하면 안 됩니다.",
        "Mainland China also has bus-only lanes, but do not assume the rules on hours, vehicles or occupancy match Korea.",
        "La Chine continentale possède aussi des voies de bus, mais les horaires et conditions peuvent différer de la Corée.",
        "China continental también tiene carriles bus, pero los horarios y requisitos pueden diferir de los de Corea."
    )

    else -> null
}
private fun bpComparison(
    id: String,
    country: String
): BpText? = when {
    country == "JP" || country == "CN" -> bpAsiaComparison(country, id)
    id == "side" && country == "UK" -> bp(
        "영국: 좌측통행·mph → 한국: 우측통행·km/h.",
        "UK: left-hand traffic and mph → Korea: right-hand traffic and km/h.",
        "Royaume-Uni : gauche et mph → Corée : droite et km/h.",
        "Reino Unido: izquierda y mph → Corea: derecha y km/h."
    )

    id == "side" && country == "US" -> bp(
        "캘리포니아도 우측통행이지만 속도 표시는 mph입니다. 한국에서는 km/h를 읽으세요.",
        "California also drives on the right, but uses mph. Read Korean speeds in km/h.",
        "La Californie roule aussi à droite, mais utilise les mph. En Corée, lisez les km/h.",
        "California también circula por la derecha, pero usa mph. En Corea lea km/h."
    )

    id == "side" && country in listOf("FR", "ES") -> bp(
        "통행 방향과 km/h 단위는 한국과 같습니다. 이 항목은 차이점이 아니라 확인사항입니다.",
        "The driving side and km/h unit are the same as Korea. This is a reminder, not a difference.",
        "Le côté de circulation et le km/h sont identiques. C'est un rappel, pas une différence.",
        "El sentido de circulación y km/h coinciden. Es un recordatorio, no una diferencia."
    )

    id == "roundabout" && country == "UK" -> bp(
        "영국의 일반적인 회전교차로는 시계 방향, 한국은 반시계 방향입니다.",
        "Typical UK roundabouts run clockwise; Korean roundabouts run counterclockwise.",
        "Les giratoires britanniques tournent généralement dans le sens horaire ; en Corée, antihoraire.",
        "Las glorietas británicas suelen girar en sentido horario; las coreanas, antihorario."
    )

    id == "roundabout" && country == "US" -> bp(
        "캘리포니아도 반시계 방향이며 회전 중인 차량에 양보합니다. 기본 원칙은 비슷합니다.",
        "California also uses counterclockwise circulation and yields to circulating traffic. The basic principle is similar.",
        "La Californie suit aussi le sens antihoraire et cède aux véhicules engagés. Le principe est similaire.",
        "California también gira en sentido antihorario y cede al tráfico interior. El principio es similar."
    )

    id == "red" && country == "UK" -> bp(
        "영국은 적색신호에서 정지하며 별도 허용 신호를 따라야 합니다. 한국의 조건부 우회전을 같은 방식으로 가정하지 마세요.",
        "In the UK, stop at red and follow any separate permitted signal. Learn Korea's conditional right-turn rules separately.",
        "Au Royaume-Uni, arrêtez-vous au rouge et respectez le signal distinct éventuel. Apprenez séparément les conditions coréennes.",
        "En el Reino Unido pare con rojo y siga cualquier señal específica que permita avanzar. Aprenda aparte las condiciones coreanas."
    )

    id == "red" && country == "US" -> bp(
        "캘리포니아도 완전 정지 후 조건부 우회전이 가능하지만 적색 화살표에서는 불가합니다. 한국의 전용 신호와 보행자 조건을 확인하세요.",
        "California also permits some right turns after stopping at red, but not at a red arrow. Check Korea's dedicated signals and pedestrian conditions.",
        "La Californie autorise aussi certains virages après arrêt au rouge, mais pas à la flèche rouge. Vérifiez les feux dédiés et piétons en Corée.",
        "California permite algunos giros tras parar con rojo, pero no con flecha roja. Revise los semáforos de giro y peatones en Corea."
    )

    id == "red" && country == "FR" -> bp(
        "프랑스는 적색에서 원칙적으로 정지합니다. 별도 황색 점멸 화살표가 허용하는 경우에는 양보 조건이 적용됩니다. 한국 신호 체계를 따로 확인하세요.",
        "France normally requires stopping at red. A separate flashing amber arrow can permit a movement subject to yielding. Check the Korean signal arrangement separately.",
        "En France, arrêt au rouge en principe. Une flèche jaune clignotante distincte peut autoriser un mouvement en cédant le passage. Vérifiez le système coréen.",
        "En Francia se para con rojo en general. Una flecha ámbar intermitente específica puede permitir avanzar cediendo el paso. Revise el sistema coreano."
    )

    id == "red" && country == "ES" -> bp(
        "스페인도 보조 화살표 신호가 회전 허용 여부에 영향을 줍니다. 한국의 ‘적색에서 정지 후 조건부 우회전’을 별도로 익히세요.",
        "In Spain, auxiliary arrow signals can control permission to turn. Learn Korea's stop-first, conditional right-turn rule separately.",
        "En Espagne, les flèches auxiliaires peuvent autoriser un virage. Apprenez la règle coréenne d'arrêt préalable et de virage conditionnel.",
        "En España, las flechas auxiliares pueden autorizar un giro. Aprenda la norma coreana de parar primero y girar solo bajo sus condiciones."
    )

    id == "left" && country == "US" -> bp(
        "캘리포니아에도 양보 후 녹색신호에서 하는 비보호 좌회전이 있습니다. 한국의 ‘비보호’ 표지를 읽는 것이 중요합니다.",
        "California also has unprotected left turns on green after yielding. Recognize Korea's ‘비보호’ sign.",
        "La Californie connaît aussi le virage non protégé au vert après avoir cédé. Reconnaissez le panneau coréen « 비보호 ».",
        "California también tiene giros no protegidos con verde tras ceder. Reconozca la señal coreana «비보호»."
    )

    id == "left" && country == "UK" -> bp(
        "좌측통행인 영국에서는 대향차 흐름을 가로지르는 회전이 보통 우회전입니다. 한국에서는 좌회전 때 대향 차량을 특히 확인하세요.",
        "With UK left-hand traffic, the turn across opposing traffic is usually a right turn. In Korea, pay attention to opposing traffic when turning left.",
        "Avec la conduite à gauche britannique, on traverse généralement le flux opposé en tournant à droite. En Corée, surveillez-le en tournant à gauche.",
        "En el Reino Unido suele cruzarse el tráfico contrario al girar a la derecha. En Corea compruébelo al girar a la izquierda."
    )

    id == "bus" && country == "US" -> bp(
        "캘리포니아의 HOV 다인승차로와 한국 버스전용차로는 이용 조건이 다릅니다. HOV 이용 자격을 한국에 그대로 적용하지 마세요.",
        "California HOV lanes and Korean bus lanes have different eligibility rules. HOV eligibility does not transfer to Korea.",
        "Les voies HOV californiennes et les voies de bus coréennes ont des critères différents. L'autorisation HOV ne se transfère pas.",
        "Los carriles HOV de California y los carriles bus coreanos tienen requisitos distintos. La autorización HOV no se traslada a Corea."
    )

    else -> null
}

private fun bpPriority(country: String): List<String> = when (country) {
    "UK" -> listOf("side", "roundabout", "red")
    "US" -> listOf("side", "crossing", "bus")
    "FR", "ES" -> listOf("red", "left", "crossing")
    "JP" -> listOf("side", "roundabout", "red")
    "CN" -> listOf("red", "crossing", "signs")
    else -> listOf("red", "crossing", "toll")
}

private fun bpBadge(
    id: String,
    country: String
): BpText = when {
    bpComparison(id, country) == null -> bp(
        "한국 현지 확인",
        "Korea reminder",
        "À vérifier en Corée",
        "Revisar en Corea"
    )

    (country == "CN" && id in listOf("side", "crossing", "bus")) ||
    (id == "side" && country in listOf("FR", "ES")) ||
            (country == "US" &&
                    id in listOf("roundabout", "red", "left")) -> bp(
        "비슷하지만 조건 확인",
        "Similar; check conditions",
        "Similaire, conditions à vérifier",
        "Similar; revise condiciones"
    )

    else -> bp(
        "차이가 있어요",
        "Different",
        "Différent",
        "Diferente"
    )
}

@Composable
private fun BpChoiceDialog(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    closeLabel: String,
    onChoose: (String) -> Unit,
    onClose: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                title,
                fontSize = 24.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                options.forEach { (id, label) ->
                    KDriveSecondaryAction(
                        label = if (id == selected) "✓  $label" else label,
                        onClick = { onChoose(id) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            KDriveSecondaryAction(
                label = closeLabel,
                onClick = onClose
            )
        }
    )
}

@Composable
private fun BpTopicCard(
    topic: BpTopic,
    index: Int,
    country: String,
    countryName: String,
    lang: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val comparison = bpComparison(topic.id, country)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KDriveScreenStyle.cardRadius),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .clickable(
                        role = Role.Button,
                        onClick = onToggle
                    )
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "0${index + 1}",
                    color = colors.primary,
                    fontSize = 23.sp,
                    fontWeight = FontWeight.Bold
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        bpBadge(topic.id, country).at(lang),
                        color = colors.primary,
                        fontSize = 16.sp
                    )

                    Text(
                        topic.title.at(lang),
                        color = colors.onSurface,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        topic.takeaway.at(lang),
                        color = colors.onSurfaceVariant,
                        fontSize = 18.sp,
                        lineHeight = 26.sp
                    )
                }

                Text(
                    if (expanded) "−" else "+",
                    color = colors.primary,
                    fontSize = 30.sp
                )
            }

            if (expanded) {
                HorizontalDivider(
                    color = colors.outlineVariant
                )

                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (comparison != null) {
                        Text(
                            countryName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary
                        )

                        Text(
                            comparison.at(lang),
                            fontSize = 20.sp,
                            lineHeight = 29.sp,
                            color = colors.onSurface
                        )
                    }

                    Text(
                        bp(
                            "한국에서는",
                            "In Korea",
                            "En Corée",
                            "En Corea"
                        ).at(lang),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )

                    Text(
                        topic.korea.at(lang),
                        fontSize = 20.sp,
                        lineHeight = 29.sp,
                        color = colors.onSurface
                    )

                    Text(
                        topic.source,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun KDriveCountryLanguageGuideScreen(
    onBack: () -> Unit,
    interactionRestricted: Boolean = true,
    uiLanguage: String? = null,
    onTrafficRulesClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    val prefs = remember(context) {
        context.applicationContext.getSharedPreferences(
            "kdrive_before_drive_v1",
            Context.MODE_PRIVATE
        )
    }

    val initialLanguage = remember(context, uiLanguage) {
        bpLanguage(
            uiLanguage ?: context.applicationContext
                .getSharedPreferences(
                    "kdrive_ui_colors_v2",
                    Context.MODE_PRIVATE
                )
                .getString(
                    "startup_language",
                    Locale.getDefault().language
                )
        )
    }

    var country by rememberSaveable {
        mutableStateOf(
            bpCountry(
                prefs.getString("country", "")
            )
        )
    }

    var lang by rememberSaveable {
        mutableStateOf(
            bpLanguage(
                prefs.getString(
                    "language",
                    initialLanguage
                )
            )
        )
    }

    var allTopics by rememberSaveable {
        mutableStateOf(false)
    }

    var expandedId by rememberSaveable {
        mutableStateOf("")
    }

    var dialog by remember {
        mutableStateOf("")
    }

    val scroll = rememberScrollState()

    fun s(
        ko: String,
        en: String,
        fr: String,
        es: String
    ): String = bp(ko, en, fr, es).at(lang)

    LaunchedEffect(interactionRestricted) {
        if (interactionRestricted) {
            dialog = ""
            expandedId = ""
            allTopics = false
        }
    }

    LaunchedEffect(country, allTopics, lang) {
        scroll.scrollTo(0)
    }

    if (interactionRestricted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                "Before You Drive",
                fontSize = 30.sp,
                color = colors.onBackground
            )

            Text(
                s(
                    "안전하게 주차한 상태에서 이용하세요.",
                    "Open this guide while safely parked.",
                    "Comparez les règles lorsque le véhicule est garé en lieu sûr.",
                    "Consulte esta guía con el vehículo estacionado."
                ),
                fontSize = 22.sp,
                color = colors.onBackground
            )
        }
        return
    }

    val leave: () -> Unit = {
        if (allTopics) {
            allTopics = false
            expandedId = ""
        } else {
            onBack()
        }
    }

    BackHandler {
        leave()
    }

    val countryName = bpCountries
        .firstOrNull { it.first == country }
        ?.second
        ?.at(lang)
        ?: s(
            "국가 선택",
            "Choose country",
            "Choisir un pays",
            "Elegir país"
        )

    val topicIds = if (allTopics) {
        bpTopics.map { it.id }
    } else {
        bpPriority(country)
    }

    val topics = topicIds.map { id ->
        bpTopics.first { it.id == id }
    }

    if (dialog.isNotEmpty()) {
        BpChoiceDialog(
            title = if (dialog == "country") {
                s(
                    "평소 운전하던 국가",
                    "Where do you usually drive?",
                    "Où conduisez-vous habituellement ?",
                    "¿Dónde suele conducir?"
                )
            } else {
                s(
                    "이 가이드의 표시 언어",
                    "Guide language",
                    "Langue du guide",
                    "Idioma de la guía"
                )
            },
            options = if (dialog == "country") {
                bpCountries.map {
                    it.first to it.second.at(lang)
                }
            } else {
                listOf(
                    "KO" to "한국어",
                    "EN" to "English",
                    "FR" to "Français",
                    "ES" to "Español"
                )
            },
            selected = if (dialog == "country") {
                country
            } else {
                lang
            },
            closeLabel = s(
                "닫기",
                "Close",
                "Fermer",
                "Cerrar"
            ),
            onChoose = { value ->
                if (dialog == "country") {
                    country = value
                    prefs.edit()
                        .putString("country", value)
                        .apply()
                } else {
                    lang = value
                    prefs.edit()
                        .putString("language", value)
                        .apply()
                }

                expandedId = ""
                dialog = ""
            },
            onClose = {
                dialog = ""
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(scroll)
            .padding(
                horizontal = KDriveScreenStyle.pageHorizontal,
                vertical = KDriveScreenStyle.pageVertical
            ),
        verticalArrangement = Arrangement.spacedBy(
            KDriveScreenStyle.sectionGap
        )
    ) {
        KDriveScreenHeader(
            title = if (allTopics) {
                s(
                    "8개 주제 비교·안내",
                    "Eight topics: compare & learn",
                    "Huit sujets à comparer",
                    "Ocho temas para comparar"
                )
            } else {
                "Before You Drive"
            },
            subtitle = s(
                "익숙한 운전 습관과 한국의 규칙을 확인하세요.",
                "Check familiar habits against Korean road rules.",
                "Comparez vos habitudes aux règles coréennes.",
                "Compare sus hábitos con las normas coreanas."
            ),
            backLabel = s(
                "← 뒤로",
                "← Back",
                "← Retour",
                "← Volver"
            ),
            onBack = leave
        )
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth()
        ) {
            val countryButton:
                    @Composable (Modifier) -> Unit = { modifier ->

                OutlinedButton(
                    onClick = {
                        dialog = "country"
                    },
                    modifier = modifier.heightIn(min = KDriveScreenStyle.touchHeight),
                    shape = RoundedCornerShape(KDriveScreenStyle.buttonRadius)
                ) {
                    Column {
                        Text(
                            s(
                                "평소 운전하던 국가",
                                "Country where you usually drive",
                                "Pays de conduite habituel",
                                "País habitual de conducción"
                            ),
                            fontSize = 16.sp
                        )

                        Text(
                            "$countryName  ▾",
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val languageButton:
                    @Composable (Modifier) -> Unit = { modifier ->

                OutlinedButton(
                    onClick = {
                        dialog = "language"
                    },
                    modifier = modifier.heightIn(min = KDriveScreenStyle.touchHeight),
                    shape = RoundedCornerShape(KDriveScreenStyle.buttonRadius)
                ) {
                    Text(
                        s(
                            "언어",
                            "Language",
                            "Langue",
                            "Idioma"
                        ) + " · $lang  ▾",
                        fontSize = 19.sp
                    )
                }
            }

            if (maxWidth >= 650.dp) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    countryButton(
                        Modifier.weight(1.5f)
                    )
                    languageButton(
                        Modifier.weight(1f)
                    )
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    countryButton(
                        Modifier.fillMaxWidth()
                    )
                    languageButton(
                        Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (country.isEmpty() || country == "OTHER") {
            Text(
                s(
                    "국가별 차이를 추정하지 않고 한국의 핵심 주의사항을 표시합니다.",
                    "Showing Korean essentials without assuming another country's rules.",
                    "Rappels coréens, sans supposer les règles d'un autre pays.",
                    "Puntos esenciales de Corea sin suponer normas de otro país."
                ),
                fontSize = 17.sp,
                lineHeight = 24.sp,
                color = colors.onSurfaceVariant
            )
        }

        Text(
            if (allTopics) {
                s(
                    "전체 8개 주제",
                    "All 8 topics",
                    "Les 8 sujets",
                    "Los 8 temas"
                )
            } else {
                s(
                    "먼저 확인할 3가지",
                    "Three things to check first",
                    "Trois points à vérifier d'abord",
                    "Tres puntos que revisar primero"
                )
            },
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )

        topics.forEachIndexed { index, topic ->
            BpTopicCard(
                topic = topic,
                index = index,
                country = country,
                countryName = countryName,
                lang = lang,
                expanded = expandedId == topic.id,
                onToggle = {
                    expandedId = if (expandedId == topic.id) {
                        ""
                    } else {
                        topic.id
                    }
                }
            )
        }

        KDriveSecondaryAction(
            label = if (allTopics) {
                s(
                    "핵심 3가지로 돌아가기",
                    "Back to the three essentials",
                    "Revenir aux trois points",
                    "Volver a los tres puntos"
                )
            } else {
                s(
                    "8개 주제 모두 보기",
                    "View all 8 topics",
                    "Voir les 8 sujets",
                    "Ver los 8 temas"
                )
            },
            onClick = {
                allTopics = !allTopics
                expandedId = ""
            },
            modifier = Modifier.fillMaxWidth()
        )

        onTrafficRulesClick?.let { openRules ->
            KDrivePrimaryAction(
                label = s(
                    "교통규칙 목록 열기",
                    "Open traffic-rule lessons",
                    "Ouvrir les règles de circulation",
                    "Abrir las normas de tráfico"
                ),
                onClick = openRules,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Text(
            s(
                "정차 중 학습용입니다. 실시간 통행 허가가 아니며 현장 신호·표지·법규를 따르세요. ‘한국 현지 확인’은 다른 국가와 다르다고 단정하지 않는 항목입니다.",
                "For parked learning, not live permission to proceed. Follow signs, signals and law. ‘Korea reminder’ does not claim a difference from another country.",
                "À consulter à l'arrêt, sans autorisation de circuler en temps réel. Respectez la signalisation et la loi. Un rappel coréen n'affirme pas une différence entre pays.",
                "Solo para consultar estacionado, no autoriza a circular en tiempo real. Respete señales y leyes. Un recordatorio coreano no afirma diferencias entre países."
            ),
            fontSize = 16.sp,
            lineHeight = 23.sp,
            color = colors.onSurfaceVariant
        )
    }
}