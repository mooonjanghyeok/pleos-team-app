
package com.example.pleosteamapp

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.util.Locale

/*
 * K-DRIVE / Driving Status
 * Public state model and callback interface
 * are used by PC1.
 */

enum class KDriveUiDataSource {
    NO_DATA,
    REAL,
    DEMO
}

data class KDriveDrivingStatusUiState(
    val source: KDriveUiDataSource =
        KDriveUiDataSource.NO_DATA,
    val navigationStatus: String =
        "Waiting for navigation",
    val autoGuideEnabled: Boolean = false,
    val demoRunning: Boolean = false,
    val placeName: String? = null,
    val distanceMeters: Double? = null,
    val guidanceTitle: String? = null,
    val guidanceText: String? = null,
    val voiceStatus: String = "Idle"
)

private data class KdStatusWords(
    val back: String,
    val title: String,
    val live: String,
    val unavailable: String,
    val sourceLabel: String,
    val noData: String,
    val real: String,
    val demo: String,
    val nextPoint: String,
    val auto: String,
    val current: String,
    val noActive: String,
    val noGuidance: String,
    val guidancePlaceholder: String,
    val voice: String,
    val noVoice: String,
    val replay: String,
    val stopVoice: String,
    val startDemo: String,
    val stopDemo: String,
    val demoNote: String
)

private fun kdStatusWords(
    lang: String
): KdStatusWords = when (lang) {

    "KO" -> KdStatusWords(
        "← 뒤로",
        "주행 상태",
        "실시간 주행 안내",
        "길 안내를 사용할 수 없습니다.",
        "데이터 출처",
        "데이터 없음",
        "실제 내비게이션",
        "데모 · 시뮬레이션",
        "다음 안내 지점",
        "자동 안내",
        "현재 안내",
        "진행 중인 경로 없음",
        "현재 안내 없음",
        "경로 정보가 수신되면 이곳에 표시됩니다.",
        "음성 상태",
        "진행 중인 안내 없음",
        "다시 재생",
        "음성 중지",
        "데모 시작",
        "데모 중지",
        "데모는 가상 데이터이며 실제 주행 정보가 아닙니다."
    )

    "FR" -> KdStatusWords(
        "← Retour",
        "État de conduite",
        "GUIDAGE EN DIRECT",
        "Guidage indisponible.",
        "SOURCE DES DONNÉES",
        "AUCUNE DONNÉE",
        "NAVIGATION RÉELLE",
        "DÉMO · SIMULATION",
        "PROCHAINE INDICATION",
        "Guidage automatique",
        "GUIDAGE ACTUEL",
        "Aucun itinéraire actif",
        "Aucune instruction active",
        "Les indications apparaîtront ici lorsqu'elles seront disponibles.",
        "État vocal",
        "Aucun guidage actif",
        "Réécouter",
        "Arrêter la voix",
        "Lancer la démo",
        "Arrêter la démo",
        "Les données de démonstration sont simulées et ne reflètent pas la circulation réelle."
    )

    "ES" -> KdStatusWords(
        "← Volver",
        "Estado de conducción",
        "GUÍA EN DIRECTO",
        "Guía de navegación no disponible.",
        "ORIGEN DE DATOS",
        "SIN DATOS",
        "NAVEGACIÓN REAL",
        "DEMO · SIMULACIÓN",
        "SIGUIENTE INDICACIÓN",
        "Guía automática",
        "INDICACIONES ACTUALES",
        "Ninguna ruta activa",
        "Sin indicaciones activas",
        "Las indicaciones aparecerán aquí cuando estén disponibles.",
        "Estado de voz",
        "Sin guía activa",
        "Repetir",
        "Detener voz",
        "Iniciar demo",
        "Detener demo",
        "La demostración usa datos simulados, no información real de conducción."
    )

    else -> KdStatusWords(
        "← Back",
        "Driving Status",
        "LIVE GUIDANCE",
        "Navigation guidance unavailable.",
        "DATA SOURCE",
        "NO DATA",
        "REAL NAVIGATION",
        "DEMO · SIMULATION",
        "NEXT GUIDANCE POINT",
        "Automatic guide",
        "CURRENT GUIDANCE",
        "No active navigation",
        "No active guidance",
        "Navigation guidance will appear here when available.",
        "Voice",
        "No active navigation guidance",
        "Replay",
        "Stop voice",
        "Start demo",
        "Stop demo",
        "Demo is simulated. Not live driving information."
    )
}

@Composable
private fun kdStatusLanguage(): String {
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

private fun kdStatusShort(
    source: String?,
    fallback: String,
    maxChars: Int = 120
): String {
    if (source.isNullOrBlank()) return fallback

    val text = source.trim()
        .split(Regex("\\s+"))
        .take(24)
        .joinToString(" ")

    return if (text.length <= maxChars) {
        text
    } else {
        text.take(maxChars - 1).trimEnd() + "…"
    }
}

private fun kdStatusDistance(
    meters: Double?
): String {
    return if (
        meters == null ||
        !meters.isFinite() ||
        meters < 0.0
    ) {
        "—"
    } else {
        String.format(Locale.US, "%,.0f m", meters)
    }
}

// ==================================================
// Translate known demo/placeholder messages.
// Unknown real navigation data remains unchanged.
// ==================================================

private fun kdKnownStatusText(
    value: String?,
    lang: String
): String? {
    if (value.isNullOrBlank()) return value

    val translations =
        when (value.trim().lowercase(Locale.ROOT)) {
            "waiting for navigation" -> listOf(
                "길 안내 대기 중",
                "Waiting for navigation",
                "En attente de navigation",
                "Esperando navegación"
            )

            "navigation guidance unavailable" -> listOf(
                "길 안내를 사용할 수 없습니다",
                "Navigation guidance unavailable",
                "Guidage indisponible",
                "Guía de navegación no disponible"
            )

            "simulation active",
            "simulated route" -> listOf(
                "가상 주행 중",
                "Simulation active",
                "Simulation en cours",
                "Simulación activa"
            )

            "simulation stopped" -> listOf(
                "시뮬레이션 중지",
                "Simulation stopped",
                "Simulation arrêtée",
                "Simulación detenida"
            )

            "sample tollgate" -> listOf(
                "예시 톨게이트",
                "Sample Tollgate",
                "Péage de démonstration",
                "Peaje de ejemplo"
            )

            "tollgate ahead" -> listOf(
                "전방에 톨게이트가 있습니다",
                "Tollgate ahead",
                "Péage à proximité",
                "Peaje más adelante"
            )

            "choose a safe payment lane." -> listOf(
                "안전한 결제 차로를 선택하세요.",
                "Choose a safe payment lane.",
                "Choisissez une voie de paiement adaptée.",
                "Elija un carril de pago adecuado."
            )

            "check the payment lane signs." -> listOf(
                "결제 차로 안내 표지를 확인하세요.",
                "Check the payment lane signs.",
                "Vérifiez les panneaux des voies de péage.",
                "Compruebe las señales de los carriles de peaje."
            )

            "idle" -> listOf(
                "대기 중",
                "Idle",
                "En attente",
                "En espera"
            )

            "stop requested" -> listOf(
                "중지 요청됨",
                "Stop requested",
                "Arrêt demandé",
                "Detención solicitada"
            )

            "route guidance active" -> listOf(
                "경로 안내 중",
                "Route guidance active",
                "Guidage en cours",
                "Guía de ruta activa"
            )

            else -> return value
        }

    val index = when (lang) {
        "KO" -> 0
        "FR" -> 2
        "ES" -> 3
        else -> 1
    }

    return translations[index]
}

// ==================================================
// Main screen
// ==================================================

@Composable
fun DrivingStatusScreen(
    state: KDriveDrivingStatusUiState,
    onBack: () -> Unit,
    onAutoGuideChange: (Boolean) -> Unit,
    onDemoStartRequest: () -> Unit,
    onDemoStopRequest: () -> Unit,
    onReplayRequest: () -> Unit,
    onStopVoiceRequest: () -> Unit,
    interactionRestricted: Boolean = true,
    uiLanguage: String? = null
) {
    val lang = uiLanguage ?: kdStatusLanguage()
    val words = kdStatusWords(lang)
    val colors = MaterialTheme.colorScheme

    val hasNavigationData =
        state.source != KDriveUiDataSource.NO_DATA

    if (interactionRestricted) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Text(
                "K-DRIVE",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                words.live,
                style = MaterialTheme.typography.titleLarge,
                color = colors.primary
            )

            if (state.source == KDriveUiDataSource.REAL) {
                Text(
                    kdStatusShort(
                        kdKnownStatusText(state.guidanceTitle, lang),
                        words.noGuidance
                    ),
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    kdStatusShort(
                        kdKnownStatusText(state.guidanceText, lang),
                        words.guidancePlaceholder,
                        80
                    ),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Text(
                    words.unavailable,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
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
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KDriveActionButton(
                    words.back,
                    onBack,
                    filled = false
                )

                Text(
                    words.title,
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        KdStatusOverview(
                            state,
                            hasNavigationData,
                            words,
                            lang,
                            onAutoGuideChange
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                    ) {
                        KdStatusControls(
                            state,
                            hasNavigationData,
                            words,
                            lang,
                            onDemoStartRequest,
                            onDemoStopRequest,
                            onReplayRequest,
                            onStopVoiceRequest
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    KdStatusOverview(
                        state,
                        hasNavigationData,
                        words,
                        lang,
                        onAutoGuideChange
                    )

                    KdStatusControls(
                        state,
                        hasNavigationData,
                        words,
                        lang,
                        onDemoStartRequest,
                        onDemoStopRequest,
                        onReplayRequest,
                        onStopVoiceRequest
                    )
                }
            }
        }
    }
}

@Composable
private fun KdStatusOverview(
    state: KDriveDrivingStatusUiState,
    hasData: Boolean,
    words: KdStatusWords,
    lang: String,
    onAutoGuideChange: (Boolean) -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    words.sourceLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )

                Text(
                    when (state.source) {
                        KDriveUiDataSource.NO_DATA -> words.noData
                        KDriveUiDataSource.REAL -> words.real
                        KDriveUiDataSource.DEMO -> words.demo
                    },
                    style = MaterialTheme.typography.titleLarge
                )

                Text(
                    kdStatusShort(
                        kdKnownStatusText(state.navigationStatus, lang),
                        words.noActive,
                        80
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    words.nextPoint,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )

                Text(
                    if (hasData) {
                        kdStatusShort(
                            kdKnownStatusText(state.placeName, lang),
                            words.noActive
                        )
                    } else {
                        words.noActive
                    },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2
                )

                Text(
                    if (hasData) {
                        kdStatusDistance(state.distanceMeters)
                    } else {
                        "—"
                    },
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    words.auto,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = state.autoGuideEnabled,
                    onCheckedChange = onAutoGuideChange
                )
            }
        }
    }
}

@Composable
private fun KdStatusControls(
    state: KDriveDrivingStatusUiState,
    hasData: Boolean,
    words: KdStatusWords,
    lang: String,
    onDemoStartRequest: () -> Unit,
    onDemoStopRequest: () -> Unit,
    onReplayRequest: () -> Unit,
    onStopVoiceRequest: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Column(
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = colors.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    words.current,
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.primary
                )

                Text(
                    if (hasData) {
                        kdStatusShort(
                            kdKnownStatusText(state.guidanceTitle, lang),
                            words.noGuidance
                        )
                    } else {
                        words.noGuidance
                    },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2
                )

                Text(
                    if (hasData) {
                        kdStatusShort(
                            kdKnownStatusText(state.guidanceText, lang),
                            words.guidancePlaceholder
                        )
                    } else {
                        words.guidancePlaceholder
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    "${words.voice}: " + if (hasData) {
                        kdStatusShort(
                            kdKnownStatusText(state.voiceStatus, lang),
                            words.noVoice,
                            60
                        )
                    } else {
                        words.noVoice
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
            }
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(KDriveUiSize.actionGap)
        ) {
            KDriveActionButton(
                words.replay,
                onReplayRequest,
                modifier = Modifier.weight(1f),
                enabled =
                    hasData &&
                            !state.guidanceText.isNullOrBlank()
            )

            KDriveActionButton(
                words.stopVoice,
                onStopVoiceRequest,
                modifier = Modifier.weight(1f),
                filled = false
            )
        }

        KDriveActionButton(
            label = if (state.demoRunning) {
                words.stopDemo
            } else {
                words.startDemo
            },
            onClick = if (state.demoRunning) {
                onDemoStopRequest
            } else {
                onDemoStartRequest
            },
            modifier = Modifier.fillMaxWidth(),
            filled = false
        )

        Text(
            words.demoNote,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2
        )
    }
}

@Preview(
    name = "Driving Status - French",
    widthDp = 1000,
    heightDp = 600,
    showBackground = true
)
@Composable
private fun KdStatusFrenchPreview() {
    KDriveTheme(KDRIVE_THEME_PRESETS.first()) {
        DrivingStatusScreen(
            state = KDriveDrivingStatusUiState(
                source = KDriveUiDataSource.DEMO,
                navigationStatus = "Simulated route",
                demoRunning = true,
                placeName = "Sample Tollgate",
                guidanceTitle = "Tollgate ahead",
                guidanceText = "Check the payment lane signs."
            ),
            onBack = {},
            onAutoGuideChange = {},
            onDemoStartRequest = {},
            onDemoStopRequest = {},
            onReplayRequest = {},
            onStopVoiceRequest = {},
            interactionRestricted = false,
            uiLanguage = "FR"
        )
    }
}
