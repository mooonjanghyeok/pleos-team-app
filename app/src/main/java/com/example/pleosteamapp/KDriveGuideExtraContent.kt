package com.example.pleosteamapp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * K-DRIVE GUIDE:
 * Language-specific, pre-reviewed draft content.
 * No live traffic signal, pedestrian, lane, or fuel-type inference.
 */
object KDriveExtraContent {

    fun forSituation(
        id: String,
        locale: String,
        drivingCountry: String
    ): String? {
        val lang = locale.substringBefore("-")

        return when (id) {
            "departure" -> introduction(drivingCountry, lang)

            "hipass" -> when (lang) {
                "ko" -> "하이패스 차로를 사용하기 전에 차량의 단말기와 결제수단을 확인하세요. 모르면 렌터카 업체에 문의하세요."
                "en" -> "Before using a Hi-Pass lane, confirm that your vehicle has a working payment setup. Ask your rental company if unsure."
                "es" -> "Antes de usar un carril Hi-Pass, compruebe que el vehículo tenga un sistema de pago válido. Si tiene dudas, consulte a la empresa de alquiler."
                "fr" -> "Avant d'emprunter une voie Hi-Pass, vérifiez que le véhicule dispose d'un moyen de paiement valide. En cas de doute, consultez l'agence de location."
                else -> null
            }

            "fuel" -> when (lang) {
                "ko" -> "주유 전 시동을 끄고 차량에 필요한 연료를 확인하세요. 휘발유는 가솔린, 경유는 디젤입니다. 주유건 색상만으로 선택하지 마세요."
                "en" -> "Turn off the engine and check your vehicle's fuel type. Hwi-bal-yu means gasoline, and gyeong-yu means diesel. Do not rely on nozzle color alone."
                "es" -> "Apague el motor y compruebe el combustible de su vehículo. Hwi-bal-yu significa gasolina y gyeong-yu significa diésel. No se guíe solo por el color de la manguera."
                "fr" -> "Coupez le moteur et vérifiez le carburant de votre véhicule. Hwi-bal-yu signifie essence et gyeong-yu signifie diesel. Ne vous fiez pas uniquement à la couleur du pistolet."
                else -> null
            }

            "parking" -> when (lang) {
                "ko" -> "주차장을 나가기 전에 사전 정산이나 매장 주차 할인 등록이 필요한지 확인하세요. 차량번호 입력 방식은 주차장마다 다를 수 있습니다."
                "en" -> "Before leaving the parking lot, check whether advance payment or parking discount registration is needed. Payment procedures may vary."
                "es" -> "Antes de salir del estacionamiento, compruebe si necesita pagar por adelantado o registrar un descuento. El procedimiento puede variar."
                "fr" -> "Avant de quitter le stationnement, vérifiez si un prépaiement ou l'enregistrement d'un rabais est nécessaire. La procédure peut varier."
                else -> null
            }

            else -> null
        }
    }

    private fun introduction(country: String, lang: String): String? {
        val group = when (country) {
            "JP", "UK", "AU" -> "LEFT_TRAFFIC"
            "US_CA", "DE" -> "RIGHT_TRAFFIC"
            else -> "GENERAL"
        }

        return when (group) {
            "LEFT_TRAFFIC" -> when (lang) {
                "ko" -> "한국에서는 도로 오른쪽으로 주행하고 회전교차로는 반시계방향으로 통행합니다. 출발 전에 신호와 우회전 규칙을 확인하세요."
                "en" -> "In South Korea, drive on the right. Roundabouts circulate counterclockwise. Review traffic signals and right-turn rules before starting."
                "es" -> "En Corea del Sur se conduce por la derecha y las glorietas se recorren en sentido antihorario. Repase las señales y las normas de giro a la derecha."
                "fr" -> "En Corée du Sud, on roule à droite et les carrefours giratoires se prennent dans le sens antihoraire. Vérifiez les feux et les règles de virage à droite."
                else -> null
            }

            "RIGHT_TRAFFIC" -> when (lang) {
                "ko" -> "한국에서도 오른쪽으로 주행합니다. 비보호 좌회전 표지, 우회전 신호등, 하이패스 요금소 이용 규칙을 출발 전에 확인하세요."
                "en" -> "South Korea also drives on the right. Pay attention to unprotected left-turn signs, right-turn signals, and Hi-Pass toll lanes."
                "es" -> "En Corea del Sur también se conduce por la derecha. Preste atención a los giros a la izquierda sin protección, las señales de giro a la derecha y los carriles Hi-Pass."
                "fr" -> "En Corée du Sud aussi, on roule à droite. Portez attention aux virages à gauche non protégés, aux feux de virage à droite et aux voies Hi-Pass."
                else -> null
            }

            else -> when (lang) {
                "ko" -> "한국에서 운전하기 전에 도로표지, 좌우회전 규칙, 통행료 결제 방법을 확인하세요."
                "en" -> "Before driving in South Korea, review local road signs, turning rules, and toll payment options."
                "es" -> "Antes de conducir en Corea del Sur, repase las señales de tránsito, las normas de giro y las opciones de pago de peajes."
                "fr" -> "Avant de conduire en Corée du Sud, vérifiez les panneaux routiers, les règles de virage et les moyens de paiement aux péages."
                else -> null
            }
        }
    }
}

@Composable
fun KDrivePersonalGuideScreen(
    selectedCountry: String,
    selectedLocale: String,
    onCountryChange: (String) -> Unit,
    onLocaleChange: (String) -> Unit,
    onPlay: (String) -> Unit,
    onStop: () -> Unit,
    onBack: () -> Unit
) {
    val countries = listOf(
        "JP" to "Japan",
        "UK" to "UK",
        "AU" to "Australia",
        "US_CA" to "US (CA)",
        "DE" to "Germany",
        "OTHER" to "Other"
    )
    val languages = listOf(
        "en-US" to "English",
        "ko-KR" to "한국어",
        "es-US" to "Español",
        "fr-CA" to "Français"
    )
    val topics = listOf(
        "departure" to "Before driving / 출발 전",
        "hipass" to "Hi-Pass / 하이패스",
        "fuel" to "Fueling / 주유",
        "parking" to "Parking / 주차"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(onClick = onBack) {
            Text("Back / 돌아가기")
        }

        Text(
            "K-DRIVE · Local Driving Guide",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            "Select where you usually drive, not your nationality.",
            style = MaterialTheme.typography.bodyMedium
        )

        Text("Driving experience / 운전 경험 국가",
            style = MaterialTheme.typography.titleMedium)

        countries.chunked(3).forEach { group ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                group.forEach { (code, name) ->
                    OutlinedButton(
                        onClick = { onCountryChange(code) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            (if (selectedCountry == code) "✓ " else "") + name,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 2
                        )
                    }
                }
            }
        }

        Text("Guide language / 안내 언어",
            style = MaterialTheme.typography.titleMedium)

        languages.chunked(2).forEach { group ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                group.forEach { (code, name) ->
                    OutlinedButton(
                        onClick = { onLocaleChange(code) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            (if (selectedLocale == code) "✓ " else "") + name
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Text(
            "Local guidance / 상황별 도움",
            style = MaterialTheme.typography.titleLarge
        )

        topics.forEach { (id, title) ->
            val message = KDriveExtraContent.forSituation(
                id, selectedLocale, selectedCountry
            )

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        message ?: "Translation not available.",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Button(
                        onClick = { onPlay(id) },
                        enabled = !message.isNullOrBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Play guidance / 음성 듣기")
                    }
                }
            }
        }

        OutlinedButton(
            onClick = onStop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Stop voice / 음성 중지")
        }

        Text(
            "For learning while parked. This screen does not detect live signals, pedestrians, fuel type, or toll payment status.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}