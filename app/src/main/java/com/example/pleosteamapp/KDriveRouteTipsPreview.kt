package com.example.pleosteamapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.Locale

// KDRIVE_PRETRIP_ROUTE_V1
// KDRIVE_COUNTRY_ROUTE_TIPS_V1
//
// Parked-only debug preview.
// TBT candidate does not establish real-time approach.
// Country and display language are independent selections.

private data class RouteTipCopy(
    val en: String,
    val ko: String,
    val fr: String,
    val es: String
) {
    fun at(locale: String): String {
        return when (
            locale.replace('_', '-')
                .substringBefore('-')
                .lowercase(Locale.ROOT)
        ) {
            "ko" -> ko
            "fr" -> fr
            "es" -> es
            else -> en
        }
    }
}

private fun tipCopy(
    en: String,
    ko: String,
    fr: String,
    es: String
) = RouteTipCopy(en, ko, fr, es)

internal fun supportedDisplayLocale(value: String): String = when (
    value.trim().replace('_', '-').substringBefore('-').lowercase(Locale.ROOT)
) {
    "ko" -> "ko-KR"
    "fr" -> "fr-CA"
    "es" -> "es-US"
    else -> "en-US"
}

internal fun normalizedRegion(value: String): String {
    return when (value.trim().uppercase(Locale.ROOT)) {
        "UK", "GB", "UNITED_KINGDOM" -> "UK"
        "US", "USA", "US_CA", "US-CA", "CALIFORNIA" -> "US"
        "FR", "FRANCE" -> "FR"
        "ES", "SPAIN" -> "ES"
        else -> "OTHER"
    }
}

private fun regionComparison(
    country: String,
    topic: String
): RouteTipCopy? {
    return when ("${normalizedRegion(country)}|$topic") {

        "UK|roundabout" -> tipCopy(
            "UK roundabouts normally run clockwise. " +
                "Korean roundabouts run counterclockwise, " +
                "and Korea drives on the right. Do not follow " +
                "your usual UK turning direction.",
            "영국 회전교차로는 보통 시계 방향이지만 " +
                "한국은 반시계 방향이며 우측통행입니다. " +
                "영국에서 익숙한 회전 방향을 그대로 적용하지 마세요.",
            "Les giratoires britanniques tournent normalement " +
                "dans le sens horaire. En Corée, ils tournent " +
                "dans le sens antihoraire et l'on roule à droite.",
            "En el Reino Unido, las glorietas suelen girar " +
                "en sentido horario. En Corea giran en sentido " +
                "antihorario y se conduce por la derecha."
        )

        "UK|toll" -> tipCopy(
            "UK toll payment arrangements vary by road. " +
                "Do not assume they work like Korean Hi-Pass. " +
                "Check your rental car's equipment and payment.",
            "영국의 통행료 결제 방식은 도로마다 다릅니다. " +
                "한국 하이패스와 같다고 가정하지 말고 " +
                "렌터카 장비와 결제 설정을 확인하세요.",
            "Au Royaume-Uni, le paiement du péage varie selon " +
                "la route. Ne supposez pas qu'il fonctionne " +
                "comme Hi-Pass en Corée. Vérifiez votre véhicule.",
            "En el Reino Unido, el pago del peaje varía según " +
                "la carretera. No suponga que funciona como " +
                "Hi-Pass. Compruebe el vehículo de alquiler."
        )

        "US|roundabout" -> tipCopy(
            "California and Korea both use counterclockwise " +
                "roundabouts. The basic direction is similar. " +
                "Follow Korean signs and lane markings.",
            "캘리포니아와 한국은 모두 회전교차로에서 " +
                "반시계 방향으로 통행합니다. 기본 방향은 " +
                "비슷하므로 한국의 표지와 차로 표시를 확인하세요.",
            "La Californie et la Corée utilisent toutes deux " +
                "le sens antihoraire dans les giratoires. " +
                "Respectez les panneaux et marquages coréens.",
            "California y Corea utilizan glorietas en sentido " +
                "antihorario. Respete las señales y marcas " +
                "de carril de Corea."
        )

        "US|toll" -> tipCopy(
            "Some California toll roads use FasTrak or " +
                "license-plate billing. These are not Korean " +
                "Hi-Pass. Confirm your Korean rental car's " +
                "device and payment arrangement.",
            "캘리포니아 일부 유료도로는 FasTrak이나 " +
                "번호판 기반 결제를 사용합니다. 한국 하이패스와 " +
                "다르므로 렌터카의 장비와 결제 설정을 확인하세요.",
            "Certains péages californiens utilisent FasTrak " +
                "ou la facturation par plaque. Ce n'est pas " +
                "Hi-Pass. Vérifiez l'équipement du véhicule.",
            "Algunos peajes de California usan FasTrak o " +
                "cobro por matrícula. No es Hi-Pass. " +
                "Compruebe el equipo y pago del vehículo."
        )

        "FR|roundabout" -> tipCopy(
            "As at a French carrefour à sens giratoire, yield to traffic already in the roundabout.",
            "프랑스의 carrefour à sens giratoire와 마찬가지로 이미 회전 중인 차량에 양보하세요.",
            "Comme dans un carrefour à sens giratoire en France, cédez le passage aux véhicules déjà engagés.",
            "Como en un carrefour à sens giratoire francés, ceda el paso a los vehículos que ya circulan."
        )

        "FR|toll" -> tipCopy(
            "France uses electronic toll-payment services " +
                "such as télépéage. Do not assume a French " +
                "toll badge works with Korean Hi-Pass. " +
                "Check your rental car.",
            "프랑스는 télépéage 같은 전자 통행료 결제를 " +
                "사용합니다. 프랑스 결제 단말기가 한국 " +
                "하이패스에서 작동한다고 가정하지 마세요.",
            "En France, le télépéage permet le paiement " +
                "électronique. Ne supposez pas qu'un badge " +
                "français fonctionne avec Hi-Pass en Corée.",
            "Francia utiliza servicios de telepeaje. " +
                "No suponga que un dispositivo francés " +
                "funciona con Hi-Pass en Corea."
        )

        "ES|roundabout" -> tipCopy(
            "As at a Spanish glorieta, follow the direction arrows and yield to traffic already circulating.",
            "스페인의 glorieta와 마찬가지로 진행 방향 화살표를 따르고 이미 회전 중인 차량에 양보하세요.",
            "Comme dans une glorieta espagnole, suivez les flèches et cédez le passage aux véhicules déjà engagés.",
            "Como en una glorieta española, siga las flechas y ceda el paso a los vehículos que ya circulan."
        )

        "ES|toll" -> tipCopy(
            "Spain uses VIA-T for electronic tolls. Confirm the Hi-Pass device and payment setup in your Korean rental car.",
            "스페인은 VIA-T 전자 통행료 시스템을 사용합니다. " +
                "한국 하이패스와 호환된다고 가정하지 말고 " +
                "렌터카 장비와 결제 설정을 확인하세요.",
            "L'Espagne utilise le télépéage VIA-T. Vérifiez le boîtier et le paiement Hi-Pass de votre voiture de location en Corée.",
            "España utiliza VIA-T para el telepeaje. Compruebe el dispositivo y el pago Hi-Pass de su coche de alquiler en Corea."
        )

        else -> null
    }
}

@Composable
private fun RouteTipAdviceCard(
    title: String,
    candidate: String,
    basicAdvice: String,
    difference: String?,
    candidateLabel: String,
    differenceLabel: String,
    playLabel: String,
    onSpeak: (String) -> Unit
) {
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
                "$candidateLabel$candidate",
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider()

            Text(basicAdvice)

            if (!difference.isNullOrBlank()) {
                Text(
                    differenceLabel,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(difference)
            }

            OutlinedButton(
                onClick = {
                    val speech = listOfNotNull(
                        basicAdvice,
                        difference?.takeIf { it.isNotBlank() }
                    ).joinToString(" ")
                    onSpeak(speech)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(playLabel)
            }
        }
    }
}

@Composable
fun KDriveRouteTipsPreview(
    tollCandidate: String?,
    roundaboutCandidate: String?,
    selectedLocale: String,
    selectedCountry: String,
    onCountryChange: (String) -> Unit,
    onLocaleChange: (String) -> Unit,
    onSpeak: (String) -> Unit,
    onStopSpeak: () -> Unit,
    onOpenLearning: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    fun tr(
        en: String,
        ko: String,
        fr: String,
        es: String
    ): String = tipCopy(en, ko, fr, es).at(selectedLocale)

    val country = normalizedRegion(selectedCountry)
    val hasToll = !tollCandidate.isNullOrBlank()
    val hasRoundabout = !roundaboutCandidate.isNullOrBlank()

    val tollBasic = tr(
        "Before departure, confirm that your rental car " +
            "has a working Hi-Pass device and payment setup. " +
            "Do not use dedicated lanes unless confirmed.",
        "출발 전 렌터카의 하이패스 단말기와 결제 설정을 " +
            "확인하세요. 확인 전에는 전용 차로를 이용하지 마세요.",
        "Avant de partir, vérifiez le dispositif Hi-Pass " +
            "et le paiement de votre voiture de location. " +
            "N'utilisez pas les voies réservées sans confirmation.",
        "Antes de salir, confirme el dispositivo Hi-Pass " +
            "y el pago del coche de alquiler. No use sus " +
            "carriles exclusivos sin confirmación."
    )

    val roundaboutBasic = tr(
        "Korean roundabouts circulate counterclockwise. " +
            "Slow down or stop before entering and yield to vehicles already circulating.",
        "한국 회전교차로는 반시계 방향입니다. " +
            "진입 전에 서행하거나 일시정지하고 회전 중인 차량에 양보하세요.",
        "En Corée, les giratoires tournent dans le sens " +
            "antihoraire. Ralentissez ou arrêtez-vous avant d'entrer et cédez le passage " +
            "aux véhicules déjà engagés.",
        "En Corea, las glorietas giran en sentido " +
            "antihorario. Reduzca la velocidad o deténgase antes de entrar y ceda " +
            "el paso a los vehículos que ya circulan."
    )

    val countries = listOf(
        "UK" to "United Kingdom",
        "US" to "USA (California)",
        "FR" to "France",
        "ES" to "Spain",
        "OTHER" to "Other"
    )
    val languages = listOf(
        "en-US" to "English",
        "ko-KR" to "한국어",
        "fr-CA" to "Français",
        "es-US" to "Español"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            tr(
                "Before You Drive",
                "출발 전 경로별 안내",
                "Avant de prendre la route",
                "Antes de conducir"
            ),
            style = MaterialTheme.typography.headlineSmall
        )

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            languages.forEach { (code, name) ->
                FilterChip(
                    selected = supportedDisplayLocale(selectedLocale) == code,
                    onClick = { onLocaleChange(code) },
                    label = { Text(name) }
                )
            }
        }

        Text(
            tr(
                "Choose the country where you are familiar " +
                    "with driving. This is separate from your " +
                    "display language.",
                "운전 경험이 익숙한 국가를 선택하세요. " +
                    "표시 언어와는 별도로 설정됩니다.",
                "Choisissez le pays où vous avez l'habitude " +
                    "de conduire. Ce choix est indépendant " +
                    "de la langue d'affichage.",
                "Elija el país donde está acostumbrado " +
                    "a conducir. Este ajuste es independiente " +
                    "del idioma."
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            countries.forEach { (code, name) ->
                if (country == code) {
                    Button(onClick = { onCountryChange(code) }) {
                        Text(name)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onCountryChange(code) }
                    ) {
                        Text(name)
                    }
                }
            }
        }

        Text(
            tr(
                "Route candidates received from Pleos navigation. " +
                    "The country comparison is educational.",
                "Pleos 내비에서 수신한 경로 후보입니다. " +
                    "국가별 비교는 출발 전 학습용입니다.",
                "Événements de route reçus de Pleos. " +
                    "La comparaison est pédagogique.",
                "Eventos de ruta recibidos de Pleos. " +
                    "La comparación es educativa."
            ),
            style = MaterialTheme.typography.bodySmall
        )

        if (!hasToll && !hasRoundabout) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "NO DATA",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        tr(
                            "No relevant route candidates. Start a route in the OEM navigation app.",
                            "관련 경로 후보가 없습니다. 기본 내비에서 " +
                                "경로 안내를 시작해 주세요.",
                            "Aucun événement de route pertinent. " +
                                "Démarrez un itinéraire dans la navigation.",
                            "No hay eventos de ruta pertinentes. " +
                                "Inicie una ruta en la navegación del vehículo."
                        )
                    )
                }
            }
        }

        val candidateLabel = tr(
            "Received candidate: ",
            "수신된 후보: ",
            "Événement reçu : ",
            "Evento recibido: "
        )

        val differenceLabel = tr(
            "Compared with your driving country",
            "익숙한 운전 국가와의 차이",
            "Comparaison avec votre pays",
            "Comparación con su país"
        )

        val playLabel = tr(
            "Play explanation",
            "설명 음성 재생",
            "Écouter l'explication",
            "Escuchar explicación"
        )

        if (hasToll) {
            RouteTipAdviceCard(
                title = tr(
                    "Tollgate & Hi-Pass",
                    "톨게이트·하이패스",
                    "Péage et Hi-Pass",
                    "Peaje e Hi-Pass"
                ),
                candidate = tollCandidate.orEmpty(),
                basicAdvice = tollBasic,
                difference = regionComparison(
                    country, "toll"
                )?.at(selectedLocale),
                candidateLabel = candidateLabel,
                differenceLabel = differenceLabel,
                playLabel = playLabel,
                onSpeak = onSpeak
            )
        }

        if (hasRoundabout) {
            RouteTipAdviceCard(
                title = tr(
                    "Roundabout",
                    "회전교차로",
                    "Carrefour giratoire",
                    "Glorieta"
                ),
                candidate = roundaboutCandidate.orEmpty(),
                basicAdvice = roundaboutBasic,
                difference = regionComparison(
                    country, "roundabout"
                )?.at(selectedLocale),
                candidateLabel = candidateLabel,
                differenceLabel = differenceLabel,
                playLabel = playLabel,
                onSpeak = onSpeak
            )
        }

        Text(
            tr(
                "These TBT candidates do not prove a live " +
                    "approach or that the route is current. " +
                    "Verify your destination. No live " +
                    "automatic guidance is enabled.",
                "이 TBT 후보만으로 실제 접근이나 경로 " +
                    "최신성을 확인할 수 없습니다. 목적지를 " +
                    "확인하세요. 실주행 자동 안내는 비활성화되어 있습니다.",
                "Ces événements ne prouvent ni l'approche " +
                    "réelle ni l'actualité de la route. " +
                    "Aucun guidage automatique réel n'est activé.",
                "Estos eventos no prueban la aproximación " +
                    "ni la vigencia de la ruta. No se ha " +
                    "activado la guía automática real."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = onStopSpeak,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                tr(
                    "Stop voice",
                    "음성 중지",
                    "Arrêter la voix",
                    "Detener voz"
                )
            )
        }

        Button(
            onClick = onOpenLearning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                tr(
                    "Open Korean Driving Rules",
                    "한국 운전 규칙 자세히 보기",
                    "Voir les règles de conduite coréennes",
                    "Ver las normas coreanas"
                )
            )
        }

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(tr("Back", "돌아가기", "Retour", "Volver"))
        }
    }
}
