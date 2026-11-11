
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

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/*
 * K-DRIVE GUIDE
 * PC2 - Country & Language Guide
 *
 * 4 countries:
 * US California / UK / France / Spain
 *
 * 4 display languages:
 * English / Korean / French / Spanish
 *
 * 8 comparison topics
 * Accordion UI: only one expanded at a time
 *
 * Parked-only learning interface.
 * PC1 owns real TTS and CarUxRestrictions.
 */

// -----------------------------------------
// Multilingual Text
// -----------------------------------------

private data class GuideText(
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

private fun gt(
    en: String,
    ko: String,
    fr: String,
    es: String
) = GuideText(en, ko, fr, es)

// -----------------------------------------
// Countries
// -----------------------------------------

private data class GuideCountry(
    val id: String,
    val name: GuideText
)

private val guideCountries = listOf(
    GuideCountry(
        "US",
        gt(
            "USA (California)",
            "미국 (캘리포니아)",
            "États-Unis (Californie)",
            "EE. UU. (California)"
        )
    ),
    GuideCountry(
        "UK",
        gt(
            "United Kingdom",
            "영국",
            "Royaume-Uni",
            "Reino Unido"
        )
    ),
    GuideCountry(
        "FR",
        gt(
            "France",
            "프랑스",
            "France",
            "Francia"
        )
    ),
    GuideCountry(
        "ES",
        gt(
            "Spain",
            "스페인",
            "Espagne",
            "España"
        )
    )
)

// -----------------------------------------
// Comparison Topics
// -----------------------------------------

private data class GuideTopic(
    val id: String,
    val title: GuideText,
    val korea: GuideText,
    val takeaway: GuideText
)

private val guideTopics = listOf(

    GuideTopic(
        "side",
        gt(
            "Driving Side and Speed Units",
            "통행 방향·속도 단위",
            "Sens de circulation et vitesse",
            "Sentido de circulación y velocidad"
        ),
        gt(
            "In Korea, vehicles drive on the right. " +
                    "Speed limits are displayed in km/h.",
            "한국에서는 도로 오른쪽으로 통행합니다. " +
                    "제한속도는 km/h 단위로 표시됩니다.",
            "En Corée, on roule à droite. " +
                    "Les vitesses sont indiquées en km/h.",
            "En Corea se conduce por la derecha. " +
                    "Las velocidades se indican en km/h."
        ),
        gt(
            "Check the driving side and speed units " +
                    "before setting off.",
            "출발 전에 통행 방향과 속도 단위를 확인하세요.",
            "Vérifiez le sens de circulation " +
                    "et les unités avant de partir.",
            "Compruebe el sentido de circulación " +
                    "y las unidades antes de salir."
        )
    ),

    GuideTopic(
        "roundabout",
        gt(
            "Roundabouts",
            "회전교차로",
            "Carrefours giratoires",
            "Glorietas"
        ),
        gt(
            "Korean roundabouts operate counterclockwise. " +
                    "Slow down and yield to vehicles already " +
                    "circulating. Follow local signs and signals.",
            "한국 회전교차로는 반시계 방향으로 통행합니다. " +
                    "진입 전 서행하고 이미 회전 중인 차량에 " +
                    "양보해야 합니다. 현장 표지와 신호도 확인하세요.",
            "En Corée, les giratoires fonctionnent " +
                    "dans le sens antihoraire. Cédez le passage " +
                    "aux véhicules déjà engagés.",
            "En Corea las glorietas funcionan " +
                    "en sentido antihorario. Ceda el paso " +
                    "a los vehículos que ya circulan."
        ),
        gt(
            "Do not assume that the circulation direction " +
                    "is the same as in your home country.",
            "자국에서 익숙했던 회전 방향을 " +
                    "그대로 적용하지 마세요.",
            "Ne supposez pas que le sens de circulation " +
                    "est identique à celui de votre pays.",
            "No suponga que el sentido de circulación " +
                    "es el mismo que en su país."
        )
    ),

    GuideTopic(
        "red",
        gt(
            "Right Turn at a Red Light",
            "적색신호에서 우회전",
            "Tourner à droite au rouge",
            "Giro a la derecha con rojo"
        ),
        gt(
            "At a red traffic signal in Korea, stop first. " +
                    "Proceed only if the applicable signals " +
                    "and pedestrian conditions permit. " +
                    "Follow a dedicated right-turn signal " +
                    "where one is installed.",
            "한국에서 전방 차량신호가 적색이면 먼저 " +
                    "일시정지해야 합니다. 보행자와 신호 조건을 " +
                    "확인하고 허용되는 경우에만 진행하세요. " +
                    "우회전 전용 신호등이 있으면 해당 신호를 따르세요.",
            "Au feu rouge en Corée, marquez d'abord l'arrêt. " +
                    "Ne tournez que si les signaux et la situation " +
                    "des piétons le permettent.",
            "Ante un semáforo rojo en Corea, deténgase primero. " +
                    "Gire solo si las señales y la situación " +
                    "de los peatones lo permiten."
        ),
        gt(
            "A navigation instruction to turn right does not " +
                    "mean turning is legally permitted right now.",
            "내비게이션이 우회전을 안내한다고 해서 " +
                    "현재 우회전이 허용되는 것은 아닙니다.",
            "Une instruction du GPS ne garantit pas " +
                    "que le virage est autorisé.",
            "Una indicación del navegador no garantiza " +
                    "que el giro esté permitido."
        )
    ),

    GuideTopic(
        "left",
        gt(
            "Unprotected Left Turns",
            "비보호 좌회전",
            "Virages à gauche non protégés",
            "Giros a la izquierda sin protección"
        ),
        gt(
            "The Korean sign '비보호' indicates an " +
                    "unprotected left turn. Where permitted, " +
                    "turn on the applicable green signal " +
                    "after yielding to oncoming vehicles " +
                    "and pedestrians.",
            "한국의 '비보호' 표지는 비보호 좌회전이 " +
                    "허용되는 곳을 나타냅니다. 해당 장소에서 " +
                    "허용된 녹색신호에 대향차와 보행자를 " +
                    "확인하고 양보한 뒤 좌회전해야 합니다.",
            "Le panneau coréen « 비보호 » indique " +
                    "un virage à gauche non protégé. " +
                    "Au signal vert applicable, cédez le passage " +
                    "aux véhicules venant en face et aux piétons.",
            "La señal coreana «비보호» indica " +
                    "un giro a la izquierda no protegido. " +
                    "Con la señal verde correspondiente, " +
                    "ceda el paso al tráfico contrario y a los peatones."
        ),
        gt(
            "Learn the sign '비보호'. A left-turn direction " +
                    "from navigation does not explain the traffic rule.",
            "'비보호' 표지를 익히세요. 단순 좌회전 길 안내만으로 " +
                    "해당 교차로의 통행 조건을 알 수는 없습니다.",
            "Apprenez « 비보호 ». Une instruction du GPS " +
                    "ne remplace pas les règles de priorité.",
            "Aprenda «비보호». Una indicación del navegador " +
                    "no sustituye las normas de prioridad."
        )
    ),

    GuideTopic(
        "crossing",
        gt(
            "Crosswalks and School Zones",
            "횡단보도·어린이보호구역",
            "Passages piétons et zones scolaires",
            "Pasos de peatones y zonas escolares"
        ),
        gt(
            "Protect pedestrians at crossings. " +
                    "At an unsignalized crosswalk inside " +
                    "a Korean child-protection zone, " +
                    "drivers must stop even if no pedestrian " +
                    "is present.",
            "한국에서는 횡단보도 보행자를 보호해야 합니다. " +
                    "특히 어린이보호구역 안에 있는 신호등 없는 " +
                    "횡단보도에서는 보행자가 없어도 " +
                    "일시정지해야 합니다.",
            "Protégez les piétons. Dans une zone de protection " +
                    "des enfants en Corée, l'arrêt est obligatoire " +
                    "à un passage sans feux, même sans piéton.",
            "Proteja a los peatones. En una zona coreana " +
                    "de protección infantil, hay que detenerse " +
                    "ante un paso sin semáforo aunque no haya peatones."
        ),
        gt(
            "Pay special attention to this Korean " +
                    "school-zone stopping requirement.",
            "한국 어린이보호구역의 특별한 " +
                    "일시정지 의무를 기억하세요.",
            "Retenez cette obligation d'arrêt " +
                    "dans les zones scolaires coréennes.",
            "Recuerde esta obligación de parada " +
                    "en las zonas escolares coreanas."
        )
    ),

    GuideTopic(
        "signs",
        gt(
            "Korean Road Signs and Signals",
            "한국 도로표지·신호",
            "Panneaux et feux coréens",
            "Señales y semáforos coreanos"
        ),
        gt(
            "Korean supplementary signs may show " +
                    "weekdays, holidays, hours or vehicle restrictions " +
                    "in Hangul. Flashing red means stop first. " +
                    "Flashing yellow means proceed with caution.",
            "한국의 보조표지에는 평일·공휴일·시간·차종 제한이 " +
                    "한글로 적힐 수 있습니다. 적색 점멸신호에서는 " +
                    "일시정지하고, 황색 점멸신호에서는 " +
                    "주의하며 진행해야 합니다.",
            "Les panneaux supplémentaires coréens peuvent " +
                    "indiquer les jours, horaires et véhicules en hangeul. " +
                    "Rouge clignotant : arrêt ; jaune : prudence.",
            "Los paneles coreanos pueden indicar días, horarios " +
                    "y restricciones en hangul. Rojo intermitente: " +
                    "detenerse; amarillo: precaución."
        ),
        gt(
            "Learn '비보호', '어린이보호구역', " +
                    "'버스전용' and operating-time signs.",
            "'비보호', '어린이보호구역', '버스전용'과 " +
                    "운영시간 보조표지를 우선 익히세요.",
            "Apprenez « 비보호 », « 어린이보호구역 », " +
                    "« 버스전용 » et les horaires.",
            "Aprenda «비보호», «어린이보호구역», " +
                    "«버스전용» y los horarios."
        )
    ),

    GuideTopic(
        "toll",
        gt(
            "Tollgates and Hi-Pass",
            "톨게이트·하이패스",
            "Péages et Hi-Pass",
            "Peajes y Hi-Pass"
        ),
        gt(
            "Korea uses Hi-Pass electronic toll lanes. " +
                    "Before departure, check your rental car's " +
                    "equipment and payment arrangement.",
            "한국에는 하이패스 전자 통행료 결제 차로가 있습니다. " +
                    "출발 전에 렌터카의 하이패스 단말기와 " +
                    "결제수단 준비 여부를 확인하세요.",
            "La Corée utilise des voies électroniques Hi-Pass. " +
                    "Avant de partir, vérifiez le dispositif " +
                    "et le paiement du véhicule loué.",
            "Corea dispone de carriles electrónicos Hi-Pass. " +
                    "Antes de salir, compruebe el dispositivo " +
                    "y el medio de pago del coche de alquiler."
        ),
        gt(
            "Do not stop suddenly or reverse at a tollgate. " +
                    "Ask the operator or rental company about payment issues.",
            "톨게이트에서 급정지하거나 후진하지 마세요. " +
                    "결제 문제가 있으면 운영사나 렌터카 업체에 문의하세요.",
            "Ne vous arrêtez pas brusquement et ne reculez pas. " +
                    "Contactez l'exploitant ou le loueur si nécessaire.",
            "No se detenga bruscamente ni retroceda. " +
                    "Consulte al operador o a la empresa de alquiler."
        )
    ),

    GuideTopic(
        "bus",
        gt(
            "Bus-Only Lanes",
            "버스전용차로",
            "Voies réservées aux bus",
            "Carriles reservados a autobuses"
        ),
        gt(
            "Bus-only lanes in Korea have specific operating " +
                    "hours and vehicle restrictions. On some expressways, " +
                    "eligibility depends on registered seating capacity " +
                    "and actual occupancy.",
            "한국 버스전용차로는 도로별 운영시간과 " +
                    "이용 가능한 차량이 다릅니다. 일부 고속도로에서는 " +
                    "차량 등록 승차정원과 실제 탑승 인원이 " +
                    "이용 조건에 포함됩니다.",
            "En Corée, les voies de bus ont des horaires " +
                    "et restrictions spécifiques. Certaines autoroutes " +
                    "tiennent compte des places et des occupants.",
            "En Corea, los carriles bus tienen horarios " +
                    "y restricciones específicas. Algunas autopistas " +
                    "consideran las plazas y los ocupantes."
        ),
        gt(
            "Permission to use a foreign bus or carpool lane " +
                    "does not automatically apply in Korea.",
            "해외에서 버스전용차로나 다인승차로를 이용할 수 " +
                    "있었다고 한국에서도 가능한 것은 아닙니다.",
            "L'accès à une voie réservée à l'étranger " +
                    "ne s'applique pas automatiquement en Corée.",
            "El acceso a un carril especial en otro país " +
                    "no implica que esté permitido en Corea."
        )
    )
)

// -----------------------------------------
// Foreign Country Differences
// -----------------------------------------

private fun foreignGuide(
    country: String
): Map<String, GuideText> {

    return when (country) {

        "UK" -> mapOf(
            "side" to gt(
                "The UK drives on the left and uses mph.",
                "영국은 좌측통행이며 속도 표시는 일반적으로 mph입니다.",
                "Le Royaume-Uni roule à gauche et utilise le mph.",
                "El Reino Unido circula por la izquierda y usa mph."
            ),
            "roundabout" to gt(
                "UK roundabouts generally circulate clockwise. " +
                        "Entering vehicles usually yield to traffic " +
                        "from the right unless signs say otherwise.",
                "영국 회전교차로는 일반적으로 시계 방향입니다. " +
                        "진입 차량은 오른쪽에서 오는 차량에 양보합니다. " +
                        "현장 표지의 예외를 확인하세요.",
                "Les giratoires britanniques tournent " +
                        "généralement dans le sens horaire.",
                "Las glorietas británicas circulan " +
                        "normalmente en sentido horario."
            ),
            "red" to gt(
                "A red signal means stop. Turning through red " +
                        "is generally prohibited unless a separate " +
                        "permitted signal controls the turn.",
                "영국은 적색신호에서 원칙적으로 회전할 수 없습니다. " +
                        "별도 허용 신호가 있는 경우에는 그 신호를 따릅니다.",
                "Au rouge, l'arrêt est obligatoire. " +
                        "Un signal distinct peut autoriser le virage.",
                "Con rojo hay que detenerse. " +
                        "Una señal distinta puede autorizar el giro."
            ),
            "left" to gt(
                "Because the UK drives on the left, " +
                        "the turn crossing opposing traffic " +
                        "is generally a right turn.",
                "영국에서는 좌측통행이므로 대향차 흐름을 " +
                        "가로지르는 회전이 일반적으로 우회전입니다.",
                "Au Royaume-Uni, le virage traversant " +
                        "la circulation opposée est plutôt à droite.",
                "En el Reino Unido, el giro que cruza " +
                        "el tráfico contrario suele ser a la derecha."
            ),
            "crossing" to gt(
                "The UK has zebra-crossing and school-crossing " +
                        "rules, but they are not identical to Korea's " +
                        "specific school-zone stop requirement.",
                "영국에도 횡단보도와 학교 주변 통행규칙이 있지만, " +
                        "한국의 특정 어린이보호구역 정지 의무와는 다릅니다.",
                "Le Royaume-Uni réglemente aussi les passages " +
                        "piétons et les zones scolaires.",
                "El Reino Unido también regula los pasos " +
                        "peatonales y las zonas escolares."
            ),
            "signs" to gt(
                "UK signs use terms such as GIVE WAY. " +
                        "Bus-lane time restrictions may be shown " +
                        "in English.",
                "영국에는 GIVE WAY 등 영어 도로표지가 있습니다. " +
                        "버스전용차로 운영시간도 영어로 표시될 수 있습니다.",
                "Les panneaux britanniques utilisent " +
                        "des termes comme GIVE WAY.",
                "Las señales británicas usan " +
                        "expresiones como GIVE WAY."
            ),
            "toll" to gt(
                "UK toll payment methods vary by road " +
                        "and crossing. They are not identical " +
                        "to Korea's Hi-Pass system.",
                "영국은 유료도로와 구간별로 결제 방식이 다릅니다. " +
                        "한국 하이패스와 동일하지 않습니다.",
                "Les péages britanniques varient selon la route.",
                "Los peajes británicos varían según la vía."
            ),
            "bus" to gt(
                "Some UK bus lanes allow taxis or bicycles " +
                        "at specified times. Korean eligibility " +
                        "rules differ.",
                "영국 일부 버스전용차로는 시간과 표지에 따라 " +
                        "택시나 자전거를 허용합니다. " +
                        "한국의 이용 조건과는 다릅니다.",
                "Certaines voies de bus britanniques " +
                        "admettent les taxis ou vélos.",
                "Algunos carriles bus británicos " +
                        "admiten taxis o bicicletas."
            )
        )

        "FR" -> mapOf(
            "side" to gt(
                "France also drives on the right " +
                        "and uses km/h.",
                "프랑스도 한국처럼 우측통행이며 km/h를 사용합니다.",
                "La France roule également à droite " +
                        "et utilise le km/h.",
                "Francia también circula por la derecha " +
                        "y usa km/h."
            ),
            "roundabout" to gt(
                "Typical French giratoires circulate " +
                        "counterclockwise. Yield to traffic " +
                        "already inside when entry signs require it.",
                "프랑스의 일반적인 회전교차로도 반시계 방향입니다. " +
                        "진입부 양보 표지가 있으면 회전 중인 차량이 우선입니다.",
                "Les giratoires français sont généralement " +
                        "antihoraires. Cédez le passage aux véhicules " +
                        "déjà engagés lorsque la signalisation l'impose.",
                "Las glorietas francesas suelen circular " +
                        "en sentido antihorario."
            ),
            "red" to gt(
                "Red generally means stop. A special " +
                        "flashing amber arrow may permit a turn " +
                        "while yielding to other traffic.",
                "프랑스에서는 적색신호에서 원칙적으로 정지합니다. " +
                        "별도의 황색 점멸 화살표가 있으면 " +
                        "양보를 전제로 회전할 수 있는 경우가 있습니다.",
                "Le rouge impose normalement l'arrêt. " +
                        "Une flèche orange clignotante peut " +
                        "autoriser un virage sous conditions.",
                "El rojo obliga normalmente a parar. " +
                        "Una flecha ámbar intermitente " +
                        "puede permitir girar con condiciones."
            ),
            "left" to gt(
                "France permits some left turns across " +
                        "opposing traffic subject to local signals " +
                        "and yield rules. Korea uses the Hangul " +
                        "sign '비보호'.",
                "프랑스에서도 대향차를 가로지르는 좌회전이 있지만, " +
                        "한국의 한글 '비보호' 표지는 별도로 익혀야 합니다.",
                "En France, les virages à gauche dépendent " +
                        "des feux et priorités. La Corée utilise " +
                        "le panneau « 비보호 ».",
                "En Francia, los giros a la izquierda " +
                        "dependen de señales y prioridades."
            ),
            "crossing" to gt(
                "France protects pedestrians at crossings " +
                        "and uses school-area or Zone 30 signs.",
                "프랑스도 보행자 보호와 Zone 30 등의 " +
                        "학교 주변·저속 구역 표지를 운영합니다.",
                "La France protège les piétons et utilise " +
                        "notamment les zones 30.",
                "Francia protege a los peatones " +
                        "y utiliza zonas 30."
            ),
            "signs" to gt(
                "French signs may say Cédez le passage. " +
                        "In Korea, restrictions may be written " +
                        "in Hangul.",
                "프랑스의 양보 표지에는 Cédez le passage가 " +
                        "사용될 수 있습니다. 한국의 제한 조건은 " +
                        "한글로 적힐 수 있습니다.",
                "Les panneaux français indiquent notamment " +
                        "« Cédez le passage ». En Corée, " +
                        "certaines restrictions sont en hangeul.",
                "En Francia aparece «Cédez le passage». " +
                        "En Corea hay restricciones en hangul."
            ),
            "toll" to gt(
                "France uses péage and télépéage systems. " +
                        "A French electronic toll badge should not " +
                        "be assumed compatible with Korea's Hi-Pass.",
                "프랑스에는 péage와 télépéage가 있습니다. " +
                        "프랑스 단말기가 한국 하이패스와 " +
                        "호환된다고 가정하면 안 됩니다.",
                "La France utilise le péage et le télépéage. " +
                        "Ne supposez pas qu'un badge français " +
                        "fonctionne avec Hi-Pass.",
                "Francia utiliza peajes y telepeaje. " +
                        "No suponga compatibilidad con Hi-Pass."
            ),
            "bus" to gt(
                "France has dedicated bus lanes, " +
                        "but Korea's expressway rules use " +
                        "different vehicle and occupancy criteria.",
                "프랑스에도 버스전용차로가 있지만 " +
                        "한국 고속도로의 차종·승차 인원 기준은 다릅니다.",
                "La France possède des voies réservées. " +
                        "Les règles coréennes de capacité " +
                        "et d'occupation sont différentes.",
                "Francia tiene carriles reservados. " +
                        "Los requisitos coreanos son distintos."
            )
        )

        "ES" -> mapOf(
            "side" to gt(
                "Spain also drives on the right " +
                        "and uses km/h.",
                "스페인도 한국처럼 우측통행이며 km/h를 사용합니다.",
                "L'Espagne roule à droite et utilise le km/h.",
                "España también circula por la derecha " +
                        "y usa km/h."
            ),
            "roundabout" to gt(
                "Spanish glorietas normally circulate " +
                        "counterclockwise. Vehicles already inside " +
                        "generally have priority.",
                "스페인 회전교차로도 일반적으로 반시계 방향이며 " +
                        "회전 중인 차량이 우선입니다.",
                "Les giratoires espagnols sont " +
                        "généralement antihoraires.",
                "Las glorietas españolas circulan " +
                        "en sentido antihorario. " +
                        "Tienen prioridad los vehículos " +
                        "que ya están dentro."
            ),
            "red" to gt(
                "A red light normally prohibits turning. " +
                        "A separate auxiliary arrow may permit " +
                        "a turn under specified conditions.",
                "스페인은 적색신호에서 원칙적으로 회전이 금지됩니다. " +
                        "별도의 보조 화살표 신호가 허용하는 경우는 예외입니다.",
                "En Espagne, le rouge interdit normalement " +
                        "de tourner, sauf signal auxiliaire autorisé.",
                "En España, el rojo normalmente prohíbe girar. " +
                        "Una flecha auxiliar puede permitir " +
                        "el giro bajo ciertas condiciones."
            ),
            "left" to gt(
                "Spain has left turns subject to signals " +
                        "and yielding. Korea's '비보호' sign " +
                        "is a different sign convention.",
                "스페인에도 신호와 양보 조건에 따른 좌회전이 있지만 " +
                        "한국의 '비보호' 표지를 별도로 익혀야 합니다.",
                "L'Espagne applique les feux et règles " +
                        "de priorité aux virages à gauche.",
                "En España, el giro a la izquierda " +
                        "está sujeto a semáforos y prioridades."
            ),
            "crossing" to gt(
                "Spain protects pedestrians at crossings " +
                        "and requires care near schools.",
                "스페인에서도 횡단보도 보행자를 보호하고 " +
                        "학교 주변에서 주의해야 합니다.",
                "L'Espagne protège les piétons " +
                        "aux passages et près des écoles.",
                "España protege a los peatones " +
                        "en los pasos y cerca de colegios."
            ),
            "signs" to gt(
                "Spanish signs may say Ceda el paso " +
                        "or Excepto. Korean supplementary signs " +
                        "may instead show Hangul restrictions.",
                "스페인 표지에는 Ceda el paso, Excepto 등이 " +
                        "쓰입니다. 한국에서는 한글 보조표지를 읽어야 합니다.",
                "En Espagne, les panneaux peuvent indiquer " +
                        "« Ceda el paso » ou « Excepto ».",
                "En España aparecen «Ceda el paso» y «Excepto». " +
                        "En Corea hay restricciones en hangul."
            ),
            "toll" to gt(
                "Spain uses the VIA-T electronic toll system " +
                        "on some toll roads. This is different " +
                        "from Korean Hi-Pass.",
                "스페인은 일부 유료도로에서 VIA-T를 사용합니다. " +
                        "한국 하이패스와는 별개의 시스템입니다.",
                "L'Espagne utilise parfois VIA-T. " +
                        "Il est distinct de Hi-Pass.",
                "España utiliza VIA-T en algunas autopistas. " +
                        "Es distinto de Hi-Pass."
            ),
            "bus" to gt(
                "Spain has bus lanes and some BUS-VAO " +
                        "high-occupancy lanes. Their conditions " +
                        "differ from Korea's bus-only lanes.",
                "스페인에는 버스전용차로와 BUS-VAO " +
                        "다인승차로가 있습니다. 한국과 이용 조건이 다릅니다.",
                "L'Espagne utilise des voies bus et BUS-VAO. " +
                        "Les critères diffèrent de ceux de Corée.",
                "España tiene carriles bus y BUS-VAO. " +
                        "Sus condiciones no son las mismas que en Corea."
            )
        )

        else -> mapOf(
            "side" to gt(
                "California drives on the right but uses mph " +
                        "on speed signs, unlike Korea's km/h.",
                "캘리포니아도 우측통행이지만 속도 표시는 " +
                        "한국의 km/h와 달리 mph입니다.",
                "La Californie roule à droite, " +
                        "mais utilise le mph.",
                "California conduce por la derecha, " +
                        "pero usa mph."
            ),
            "roundabout" to gt(
                "California roundabouts also run " +
                        "counterclockwise and yield to " +
                        "circulating traffic.",
                "캘리포니아도 반시계 방향으로 회전하고 " +
                        "회전 중인 차량에 양보합니다.",
                "Les giratoires californiens fonctionnent " +
                        "aussi dans le sens antihoraire.",
                "Las glorietas de California también " +
                        "circulan en sentido antihorario."
            ),
            "red" to gt(
                "California generally permits a right turn " +
                        "on a steady red after a complete stop, " +
                        "unless signs prohibit it. A red arrow " +
                        "prohibits the turn.",
                "캘리포니아는 일반 적색신호에서 완전 정지 후 " +
                        "금지표지가 없으면 조건부 우회전이 가능합니다. " +
                        "적색 화살표에서는 회전할 수 없습니다.",
                "En Californie, tourner à droite au rouge " +
                        "fixe peut être permis après arrêt, " +
                        "sauf interdiction.",
                "En California, girar a la derecha " +
                        "con rojo fijo puede estar permitido " +
                        "tras detenerse, salvo prohibición."
            ),
            "left" to gt(
                "California also allows certain unprotected " +
                        "left turns on green. Some intersections " +
                        "use a flashing yellow arrow.",
                "캘리포니아에도 비보호 좌회전이 있고 " +
                        "일부 교차로에서는 황색 점멸 화살표를 사용합니다.",
                "La Californie permet certains virages " +
                        "à gauche non protégés au vert.",
                "California permite ciertos giros a la " +
                        "izquierda sin protección con verde."
            ),
            "crossing" to gt(
                "California has pedestrian and school-zone " +
                        "laws, but their details are not identical " +
                        "to Korea's special stop rule.",
                "캘리포니아에도 보행자·학교구역 법규가 있지만 " +
                        "한국의 특별한 일시정지 의무와 동일하지는 않습니다.",
                "La Californie possède ses propres règles " +
                        "pour piétons et zones scolaires.",
                "California tiene sus propias normas " +
                        "para peatones y zonas escolares."
            ),
            "signs" to gt(
                "California uses signs such as STOP, YIELD " +
                        "and NO TURN ON RED. Korea uses Hangul " +
                        "for some restrictions.",
                "캘리포니아에서는 STOP, YIELD, " +
                        "NO TURN ON RED 등을 사용하지만 " +
                        "한국에서는 일부 제한 조건을 한글로 표시합니다.",
                "La Californie utilise STOP, YIELD " +
                        "et NO TURN ON RED.",
                "California usa señales como STOP, YIELD " +
                        "y NO TURN ON RED."
            ),
            "toll" to gt(
                "Some California toll facilities use FasTrak " +
                        "or license-plate billing. These are not " +
                        "interchangeable with Korean Hi-Pass.",
                "캘리포니아 일부 유료도로는 FasTrak 또는 " +
                        "번호판 기반 결제를 사용합니다. " +
                        "한국 하이패스와 호환되지 않습니다.",
                "Certains péages californiens utilisent " +
                        "FasTrak ou la facturation par plaque.",
                "Algunos peajes de California utilizan " +
                        "FasTrak o cobro por matrícula."
            ),
            "bus" to gt(
                "California distinguishes bus-only lanes " +
                        "from carpool or HOV lanes. Eligibility " +
                        "does not transfer to Korea.",
                "캘리포니아에서는 버스전용차로와 HOV " +
                        "다인승차로를 구분합니다. " +
                        "한국에서는 별도의 조건을 따라야 합니다.",
                "La Californie distingue les voies bus " +
                        "et les voies HOV.",
                "California distingue carriles bus " +
                        "y carriles HOV."
            )
        )
    }
}

// -----------------------------------------
// Main Screen
// -----------------------------------------

@Composable
fun KDriveCountryLanguageGuideScreen(
    onBack: () -> Unit,
    interactionRestricted: Boolean = true
) {
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
                "Country Driving Guide",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground
            )
            Text(
                "Open this guide while safely parked.",
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )
        }
        return
    }

    var selectedCountry by rememberSaveable {
        mutableStateOf("US")
    }

    var selectedLanguage by rememberSaveable {
        mutableStateOf("EN")
    }

    // Empty means every topic is collapsed.
    var expandedTopicId by rememberSaveable {
        mutableStateOf("")
    }

    val language = selectedLanguage

    val country = guideCountries.firstOrNull {
        it.id == selectedCountry
    } ?: guideCountries.first()

    val foreign = foreignGuide(country.id)

    fun label(
        en: String,
        ko: String,
        fr: String,
        es: String
    ): String = gt(en, ko, fr, es).get(language)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val columns = when {
            maxWidth >= 920.dp -> 4
            maxWidth >= 520.dp -> 2
            else -> 1
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(KDriveUiSize.padding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KDriveActionButton(
                    label = label(
                        "Back", "뒤로", "Retour", "Volver"
                    ),
                    onClick = onBack,
                    filled = false
                )

                Text(
                    text = label(
                        "Country Driving Guide",
                        "국가별 운전 가이드",
                        "Guide de conduite par pays",
                        "Guía de conducción por países"
                    ),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground
                )
            }

            Text(
                text = label(
                    "Compare driving rules before driving in Korea.",
                    "한국에서 운전하기 전 익숙한 국가와 규칙을 비교하세요.",
                    "Comparez les règles avant de conduire en Corée.",
                    "Compare las normas antes de conducir en Corea."
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground
            )

            Text(
                text = label(
                    "SELECT YOUR FAMILIAR COUNTRY",
                    "익숙한 운전 국가 선택",
                    "PAYS DE RÉFÉRENCE",
                    "PAÍS DE REFERENCIA"
                ),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )

            GuideOptionGrid(
                options = guideCountries.map {
                    it.id to it.name.get(language)
                },
                selected = selectedCountry,
                columns = columns,
                onSelect = { next ->
                    if (next != selectedCountry) {
                        selectedCountry = next
                        expandedTopicId = ""
                    }
                }
            )

            Text(
                text = label(
                    "DISPLAY LANGUAGE",
                    "표시 언어",
                    "LANGUE D'AFFICHAGE",
                    "IDIOMA DE PANTALLA"
                ),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )

            GuideOptionGrid(
                options = listOf(
                    "EN" to "English",
                    "KO" to "한국어",
                    "FR" to "Français",
                    "ES" to "Español"
                ),
                selected = selectedLanguage,
                columns = columns,
                onSelect = { selectedLanguage = it }
            )

            Text(
                text = label(
                    "Korea vs ",
                    "한국 vs ",
                    "Corée vs ",
                    "Corea vs "
                ) + country.name.get(language),
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground
            )

            Text(
                text = label(
                    "8 topics. Tap one to read details.",
                    "8개 항목 중 하나를 눌러 자세히 읽으세요.",
                    "8 sujets. Touchez un titre pour lire.",
                    "8 temas. Toque un título para leer."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackground
            )

            // ---------------------------------
            // Accordion list
            // ---------------------------------

            guideTopics.forEach { topic ->

                val isExpanded =
                    expandedTopicId == topic.id

                GuideAccordionCard(
                    title = topic.title.get(language),
                    isExpanded = isExpanded,

                    onToggle = {
                        expandedTopicId =
                            if (isExpanded) "" else topic.id
                    },

                    hint = if (isExpanded) {
                        label(
                            "Tap to close",
                            "눌러서 닫기",
                            "Touchez pour fermer",
                            "Toque para cerrar"
                        )
                    } else {
                        label(
                            "Tap to read",
                            "눌러서 자세히 보기",
                            "Touchez pour lire",
                            "Toque para leer"
                        )
                    },

                    koreaLabel = label(
                        "KOREA", "한국", "CORÉE", "COREA"
                    ),
                    foreignLabel = country.name.get(language),
                    takeawayLabel = label(
                        "REMEMBER IN KOREA",
                        "한국에서 기억할 점",
                        "À RETENIR EN CORÉE",
                        "RECUERDE EN COREA"
                    ),
                    koreaText = topic.korea.get(language),
                    foreignText = foreign.getValue(topic.id)
                        .get(language),
                    takeawayText = topic.takeaway.get(language)
                )
            }

            Text(
                text = label(
                    "For parked learning only. Follow current " +
                            "road signs, signals and applicable laws.",
                    "정차 중 학습용 정보입니다. 실제 운전 시에는 " +
                            "현장 표지와 신호 및 관련 법규를 따르세요.",
                    "À consulter à l'arrêt. Respectez " +
                            "toujours la signalisation et la loi.",
                    "Solo para consultar con el vehículo " +
                            "estacionado. Respete siempre las señales y la ley."
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onBackground
            )
        }
    }
}

// -----------------------------------------
// Country / Language Selector
// -----------------------------------------

@Composable
private fun GuideOptionGrid(
    options: List<Pair<String, String>>,
    selected: String,
    columns: Int,
    onSelect: (String) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        options.chunked(columns).forEach { row ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (id, title) ->

                    val active = selected == id

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = KDriveUiSize.touchHeight)
                            .clickable { onSelect(id) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (active) {
                                colors.primary
                            } else {
                                colors.surface
                            }
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (active) {
                                    colors.onPrimary
                                } else {
                                    colors.onSurface
                                }
                            )
                        }
                    }
                }

                repeat(columns - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

// -----------------------------------------
// Accordion Comparison Card
// -----------------------------------------

@Composable
private fun GuideAccordionCard(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    hint: String,
    koreaLabel: String,
    foreignLabel: String,
    takeawayLabel: String,
    koreaText: String,
    foreignText: String,
    takeawayText: String
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {

            // Only the title row is clickable.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = KDriveUiSize.touchHeight)
                    .clickable(onClick = onToggle)
                    .padding(
                        horizontal = 18.dp,
                        vertical = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onSurface
                    )

                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant
                    )
                }

                Text(
                    text = if (isExpanded) "−" else "+",
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.primary
                )
            }

            // Detailed description
            if (isExpanded) {

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            colors.outline.copy(alpha = 0.35f)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    Text(
                        text = koreaLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary
                    )

                    Text(
                        text = koreaText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = foreignLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary
                    )

                    Text(
                        text = foreignText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = takeawayLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = colors.primary
                    )

                    Text(
                        text = takeawayText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = colors.onSurface
                    )
                }
            }
        }
    }
}

// -----------------------------------------
// Landscape Preview
// -----------------------------------------

@Preview(
    name = "Country Guide - Accordion Landscape",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun CountryAccordionLandscapePreview() {
    KDriveTheme(KDRIVE_THEME_PRESETS.first()) {
        KDriveCountryLanguageGuideScreen(
            onBack = {},
            interactionRestricted = false
        )
    }
}

// -----------------------------------------
// Portrait Preview
// -----------------------------------------

@Preview(
    name = "Country Guide - Accordion Portrait",
    widthDp = 400,
    heightDp = 800,
    showBackground = true
)
@Composable
private fun CountryAccordionPortraitPreview() {
    KDriveTheme(KDRIVE_THEME_PRESETS.first()) {
        KDriveCountryLanguageGuideScreen(
            onBack = {},
            interactionRestricted = false
        )
    }
}
