package com.example.pleosteamapp

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// KDRIVE_PRETRIP_ROUTE_V1
// Debug parked preview only. TBT candidate != verified live approach.

private fun routeTipsTranslate(
    locale: String,
    en: String,
    ko: String,
    fr: String,
    es: String
): String {
    return when (
        locale.replace('_', '-').substringBefore('-').lowercase()
    ) {
        "ko" -> ko
        "fr" -> fr
        "es" -> es
        else -> en
    }
}

@Composable
fun KDriveRouteTipsPreview(
    tollCandidate: String?,
    roundaboutCandidate: String?,
    selectedLocale: String,
    onOpenLearning: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    fun tr(
        en: String,
        ko: String,
        fr: String,
        es: String
    ): String = routeTipsTranslate(
        selectedLocale, en, ko, fr, es
    )

    val hasToll = !tollCandidate.isNullOrBlank()
    val hasRoundabout = !roundaboutCandidate.isNullOrBlank()

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

        Text(
            tr(
                "Received route candidates from Pleos navigation.",
                "Pleos 내비에서 수신한 경로 후보입니다.",
                "Événements d'itinéraire reçus de Pleos.",
                "Eventos de ruta recibidos de Pleos."
            )
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
                            "No relevant route candidates. Start route guidance in the OEM navigation app, then reopen.",
                            "해당 경로 후보가 없습니다. 기본 내비에서 경로 안내를 시작한 뒤 다시 열어 주세요.",
                            "Aucun événement pertinent. Démarrez la navigation, puis rouvrez cet écran.",
                            "No hay eventos pertinentes. Inicie la navegación y vuelva a abrir esta pantalla."
                        )
                    )
                }
            }
        }

        if (hasToll) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tr(
                            "Tollgate & Hi-Pass",
                            "톨게이트·하이패스",
                            "Péage et Hi-Pass",
                            "Peaje e Hi-Pass"
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        tr(
                            "Received candidate: ",
                            "수신된 후보: ",
                            "Événement reçu : ",
                            "Evento recibido: "
                        ) + tollCandidate.orEmpty()
                    )

                    Text(
                        tr(
                            "Before departure, confirm the rental car's Hi-Pass device and payment setup. Do not use dedicated lanes unless confirmed.",
                            "출발 전 렌터카의 하이패스 단말기와 결제 설정을 확인하세요. 확인 전에는 전용 차로를 이용하지 마세요.",
                            "Avant de partir, vérifiez le boîtier et le paiement Hi-Pass du véhicule. Sans confirmation, évitez les voies réservées.",
                            "Antes de salir, compruebe el dispositivo y el pago Hi-Pass. Sin confirmación, evite los carriles exclusivos."
                        )
                    )
                }
            }
        }

        if (hasRoundabout) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        tr(
                            "Roundabout",
                            "회전교차로",
                            "Carrefour giratoire",
                            "Glorieta"
                        ),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        tr(
                            "Received candidate: ",
                            "수신된 후보: ",
                            "Événement reçu : ",
                            "Evento recibido: "
                        ) + roundaboutCandidate.orEmpty()
                    )

                    Text(
                        tr(
                            "Korean roundabouts circulate counterclockwise. Slow down and yield to vehicles already circulating.",
                            "한국 회전교차로는 반시계 방향입니다. 서행하고 회전 중인 차량에 양보하세요.",
                            "En Corée, les giratoires tournent dans le sens antihoraire. Ralentissez et cédez le passage aux véhicules déjà engagés.",
                            "En Corea, las glorietas giran en sentido antihorario. Reduzca la velocidad y ceda el paso a los vehículos que ya circulan."
                        )
                    )
                }
            }
        }

        Text(
            tr(
                "Route candidates do not confirm live approach or route freshness. Verify the destination. This is not live driving guidance.",
                "후보 정보만으로 실제 접근이나 경로 최신성을 확인할 수 없습니다. 목적지를 확인하세요. 실주행 안내가 아닙니다.",
                "Ces événements ne confirment ni l'approche réelle ni la mise à jour de l'itinéraire.",
                "Estos eventos no confirman la aproximación real ni la vigencia de la ruta."
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onOpenLearning,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                tr(
                    "Open Korean Driving Rules",
                    "한국 운전 규칙 자세히 보기",
                    "Voir les règles de conduite coréennes",
                    "Ver las normas de conducción coreanas"
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