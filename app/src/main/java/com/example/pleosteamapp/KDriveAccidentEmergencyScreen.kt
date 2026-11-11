package com.example.pleosteamapp

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/*
 * Accident & Emergency
 *
 * TTS uses callbacks received from PC1.
 *
 * No Clipboard feature.
 * No rental-phone input.
 * No direct emergency call (ACTION_DIAL only).
 */

private val KD_EMERGENCY_PHRASES = listOf(
    "교통사고가 났습니다. 도와주세요.",
    "다친 사람이 있습니다. 119에 신고해 주세요.",
    "통역사를 연결해 주세요."
)

private data class KdEmergencyStrings(
    val back: String,
    val title: String,
    val intro: String,
    val contacts: String,
    val police: String,
    val rescue: String,
    val dialHint: String,
    val interpreterHint: String,
    val stepsTitle: String,
    val steps: List<Pair<String, String>>,
    val legalNote: String,
    val helpTitle: String,
    val helpDescription: String,
    val helpQuestion: String,
    val situations: List<String>,
    val meanings: List<String>,
    val showHint: String,
    val play: String,
    val close: String,
    val dialUnavailable: String,
    val source: String
)

private fun kdEmergencyStrings(
    language: String
): KdEmergencyStrings = when (language) {

    "KO" -> KdEmergencyStrings(
        back = "← 뒤로",
        title = "사고 및 긴급상황 안내",
        intro = "한국에서 교통사고가 발생했을 때 필요한 조치를 확인하세요.",
        contacts = "긴급 연락처",
        police = "경찰",
        rescue = "구급·소방",
        dialHint = "번호를 누르면 전화 앱이 열립니다.",
        interpreterHint =
            "112 또는 119 신고 시 통역을 요청할 수 있습니다. 현재 위치도 함께 알려주세요.",
        stepsTitle = "사고 후 확인할 5단계",
        steps = listOf(
            "안전하게 정차" to
                    "즉시 정차하고 안전한 경우 비상등을 켜세요. 추가 사고 위험을 피하세요.",
            "부상자 확인" to
                    "부상자를 확인하고 필요한 구호조치를 하세요. 구급차가 필요하면 119, 경찰 도움이 필요하면 112로 연락하세요.",
            "렌터카 업체 연락" to
                    "렌터카를 이용 중이라면 렌터카 업체에 연락하고 보험사 안내를 확인하세요.",
            "사고 현장 기록" to
                    "안전이 확보된 경우 현장과 차량 손상을 기록하세요. 피해자에게 필요한 인적사항을 제공하세요.",
            "공식 안내 따르기" to
                    "경찰·구급대의 지시를 따르고 추가 조치가 필요한지 확인하세요."
        ),
        legalNote =
            "경찰 신고 의무는 사고 상황에 따라 다릅니다. 모든 경미한 차량 접촉사고에 경찰 신고가 무조건 필요한 것은 아닙니다.",
        helpTitle = "한국어로 도움 요청",
        helpDescription =
            "상황에 맞는 한국어 문장을 크게 보여주거나 음성으로 재생하세요.",
        helpQuestion = "어떤 도움이 필요하신가요?",
        situations = listOf("사고 발생", "부상자 발생", "통역 요청"),
        meanings = listOf(
            "교통사고가 발생하여 도움이 필요합니다.",
            "부상자가 있어 구급차가 필요합니다.",
            "한국어 통역이 필요합니다."
        ),
        showHint = "이 화면을 주변 한국인에게 보여줄 수도 있습니다.",
        play = "한국어 음성 재생",
        close = "닫기",
        dialUnavailable = "이 기기에서 전화 앱을 열 수 없습니다.",
        source = "안내 기준: 한국관광공사 / 도로교통법 제54조"
    )

    "FR" -> KdEmergencyStrings(
        back = "← Retour",
        title = "Accident et urgences",
        intro =
            "Consultez les étapes à suivre en cas d'accident de la route en Corée du Sud.",
        contacts = "Numéros d'urgence",
        police = "Police",
        rescue = "Ambulance / Pompiers",
        dialHint = "Touchez un numéro pour ouvrir le composeur.",
        interpreterHint =
            "Vous pouvez demander un interprète en appelant le 112 ou le 119. Indiquez aussi votre position.",
        stepsTitle = "5 étapes après un accident",
        steps = listOf(
            "Arrêtez-vous en sécurité" to
                    "Arrêtez-vous immédiatement. Allumez les feux de détresse si cela est approprié et évitez tout danger supplémentaire.",
            "Vérifiez les blessures" to
                    "Portez assistance si nécessaire. Appelez le 119 pour une ambulance et le 112 si l'aide de la police est nécessaire.",
            "Contactez votre agence de location" to
                    "Si le véhicule est loué, prévenez l'agence et suivez les instructions de votre assurance.",
            "Documentez l'accident" to
                    "Si vous êtes en sécurité, prenez des photos et communiquez les informations personnelles nécessaires.",
            "Suivez les instructions officielles" to
                    "Respectez les instructions de la police et des secours."
        ),
        legalNote =
            "La déclaration à la police dépend des circonstances. Tous les petits accrochages ne nécessitent pas systématiquement un signalement.",
        helpTitle = "Demander de l'aide en coréen",
        helpDescription =
            "Affichez une phrase en coréen ou faites-la lire à voix haute.",
        helpQuestion = "De quelle aide avez-vous besoin ?",
        situations = listOf("Accident", "Personne blessée", "Interprète"),
        meanings = listOf(
            "J'ai eu un accident de voiture. Aidez-moi, s'il vous plaît.",
            "Une personne est blessée. Appelez le 119.",
            "Veuillez me mettre en relation avec un interprète."
        ),
        showHint =
            "Vous pouvez aussi montrer ce message à une personne coréenne.",
        play = "Écouter en coréen",
        close = "Fermer",
        dialUnavailable = "Le composeur téléphonique est indisponible.",
        source =
            "Sources : VisitKorea / Loi coréenne sur la circulation routière, art. 54"
    )

    "ES" -> KdEmergencyStrings(
        back = "← Volver",
        title = "Accidentes y emergencias",
        intro =
            "Consulte los pasos a seguir si tiene un accidente de tráfico en Corea del Sur.",
        contacts = "Contactos de emergencia",
        police = "Policía",
        rescue = "Ambulancia / Bomberos",
        dialHint = "Toque un número para abrir el marcador.",
        interpreterHint =
            "Puede solicitar un intérprete al llamar al 112 o al 119. Indique también su ubicación.",
        stepsTitle = "5 pasos después de un accidente",
        steps = listOf(
            "Deténgase con seguridad" to
                    "Detenga el vehículo inmediatamente. Active las luces de emergencia cuando corresponda y evite nuevos peligros.",
            "Compruebe si hay heridos" to
                    "Preste la ayuda necesaria. Llame al 119 si necesita una ambulancia o al 112 si necesita asistencia policial.",
            "Contacte con la empresa de alquiler" to
                    "Si conduce un coche de alquiler, informe a la empresa y siga las instrucciones del seguro.",
            "Documente el accidente" to
                    "Si es seguro, fotografíe el lugar y los daños. Facilite los datos personales necesarios.",
            "Siga las instrucciones oficiales" to
                    "Siga las indicaciones de la policía y de los servicios de emergencia."
        ),
        legalNote =
            "La obligación de informar a la policía depende de las circunstancias. No todos los choques leves requieren una denuncia obligatoria.",
        helpTitle = "Pedir ayuda en coreano",
        helpDescription =
            "Muestre una frase en coreano o reprodúzcala en voz alta.",
        helpQuestion = "¿Qué ayuda necesita?",
        situations = listOf("Accidente", "Persona herida", "Intérprete"),
        meanings = listOf(
            "He tenido un accidente. Por favor, ayúdeme.",
            "Hay una persona herida. Llame al 119.",
            "Por favor, póngame en contacto con un intérprete."
        ),
        showHint =
            "También puede mostrar este mensaje a una persona coreana.",
        play = "Reproducir en coreano",
        close = "Cerrar",
        dialUnavailable = "No se puede abrir el marcador telefónico.",
        source =
            "Fuentes: VisitKorea / Ley de tráfico de Corea, art. 54"
    )

    else -> KdEmergencyStrings(
        back = "← Back",
        title = "Accident & Emergency",
        intro =
            "Essential steps if you are involved in a traffic accident in South Korea.",
        contacts = "Emergency contacts",
        police = "Police",
        rescue = "Ambulance / Fire",
        dialHint = "Tap a number to open the phone dialer.",
        interpreterHint =
            "You can request an interpreter when calling 112 or 119. Be ready to share your location.",
        stepsTitle = "5 steps after an accident",
        steps = listOf(
            "Stop safely" to
                    "Stop immediately. Use hazard lights when appropriate and avoid further danger.",
            "Check for injuries" to
                    "Provide necessary assistance. Call 119 for an ambulance or 112 if police assistance is needed.",
            "Contact your rental company" to
                    "Notify your rental company and follow your insurance provider's instructions.",
            "Document the accident" to
                    "If safe, take photos of the scene and vehicle damage. Provide required personal details to those affected.",
            "Follow official instructions" to
                    "Follow instructions from the police and emergency responders."
        ),
        legalNote =
            "Police reporting requirements depend on the circumstances. Not every minor vehicle-only collision requires a police report.",
        helpTitle = "Ask for Help in Korean",
        helpDescription = "Show a large Korean message or play it aloud.",
        helpQuestion = "What kind of help do you need?",
        situations = listOf(
            "Traffic accident",
            "Someone is injured",
            "Need an interpreter"
        ),
        meanings = listOf(
            "I've been in a car accident. Please help me.",
            "Someone is injured. Please call 119.",
            "Please connect me to an interpreter."
        ),
        showHint =
            "You can also show this message to someone who speaks Korean.",
        play = "Play Korean audio",
        close = "Close",
        dialUnavailable = "The phone dialer is unavailable on this device.",
        source =
            "Sources: VisitKorea / Korean Road Traffic Act, Article 54"
    )
}

private fun kdEmergencyDial(
    context: Context,
    number: String
): Boolean {
    return try {
        val intent = Intent(
            Intent.ACTION_DIAL,
            Uri.parse("tel:$number")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

// ==================================================
// Main Screen
// ==================================================

@Composable
fun KDriveAccidentEmergencyScreen(
    uiLanguage: String,
    onBack: () -> Unit,
    onSpeakRequest: (String) -> Unit = {},
    onStopVoiceRequest: () -> Unit = {}
) {
    val language = when (uiLanguage) {
        "KO", "EN", "FR", "ES" -> uiLanguage
        else -> "EN"
    }

    val words = kdEmergencyStrings(language)
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val preview = LocalInspectionMode.current

    var showKoreanHelp by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedPhrase by rememberSaveable {
        mutableStateOf(0)
    }

    var dialNotice by remember {
        mutableStateOf("")
    }

    // Use the newest callback if the host recomposes.
    val stopVoice by rememberUpdatedState(onStopVoiceRequest)

    // Stop any emergency speech when this screen disappears.
    DisposableEffect(Unit) {
        onDispose {
            stopVoice()
        }
    }

    BackHandler(enabled = !showKoreanHelp) {
        onBack()
    }

    val dial: (String) -> Unit = { number ->
        dialNotice = if (preview) {
            words.dialHint
        } else if (kdEmergencyDial(context, number)) {
            ""
        } else {
            words.dialUnavailable
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val wide = maxWidth >= 750.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                KDriveActionButton(
                    label = words.back,
                    onClick = onBack,
                    filled = false
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = words.title,
                        fontSize = if (wide) 27.sp else 22.sp,
                        lineHeight = if (wide) 34.sp else 29.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )

                    Text(
                        text = "K-DRIVE GUIDE",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
            }

            Text(
                text = words.intro,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onBackground
            )

            if (dialNotice.isNotBlank()) {
                Text(
                    text = dialNotice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary
                )
            }

            if (wide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        KdEmergencyContacts(words, dial)
                        KdEmergencySteps(words)
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        KdEmergencyHelpEntry(words) {
                            selectedPhrase = 0
                            showKoreanHelp = true
                        }
                    }
                }
            } else {
                KdEmergencyContacts(words, dial)
                KdEmergencySteps(words)

                KdEmergencyHelpEntry(words) {
                    selectedPhrase = 0
                    showKoreanHelp = true
                }
            }

            Text(
                text = words.source,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
        }
    }

    if (showKoreanHelp) {
        KdEmergencyHelpDialog(
            words = words,
            selectedIndex = selectedPhrase,
            onSelect = { index ->
                if (index != selectedPhrase) {
                    onStopVoiceRequest()
                }
                selectedPhrase = index
            },
            onSpeak = { phrase ->
                onSpeakRequest(phrase)
            },
            onDismiss = {
                onStopVoiceRequest()
                showKoreanHelp = false
            }
        )
    }
}

// ==================================================
// 112 / 119
// ==================================================

@Composable
private fun KdEmergencyContacts(
    words: KdEmergencyStrings,
    onDial: (String) -> Unit
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = words.contacts,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KdEmergencyNumber(
                    number = "112",
                    title = words.police,
                    modifier = Modifier.weight(1f),
                    onClick = { onDial("112") }
                )

                KdEmergencyNumber(
                    number = "119",
                    title = words.rescue,
                    modifier = Modifier.weight(1f),
                    onClick = { onDial("119") }
                )
            }

            Text(
                text = words.dialHint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )

            Text(
                text = words.interpreterHint,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onSurface
            )
        }
    }
}

@Composable
private fun KdEmergencyNumber(
    number: String,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier
            .heightIn(min = 114.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.primary.copy(alpha = 0.12f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = number,
                fontSize = 31.sp,
                lineHeight = 37.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary
            )

            Text(
                text = title,
                fontSize = 17.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurface
            )
        }
    }
}

// ==================================================
// Five steps
// ==================================================

@Composable
private fun KdEmergencySteps(
    words: KdEmergencyStrings
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(
                text = words.stepsTitle,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            words.steps.forEachIndexed { index, step ->
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${step.first}",
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Text(
                        text = step.second,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }

            Text(
                text = words.legalNote,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
}

// ==================================================
// Korean help entry
// ==================================================

@Composable
private fun KdEmergencyHelpEntry(
    words: KdEmergencyStrings,
    onOpen: () -> Unit
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Text(
                text = words.helpTitle,
                fontSize = 23.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            Text(
                text = words.helpDescription,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                color = colors.onSurfaceVariant
            )

            KDriveActionButton(
                label = words.helpTitle,
                onClick = onOpen,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==================================================
// Korean help dialog
// ==================================================

@Composable
private fun KdEmergencyHelpDialog(
    words: KdEmergencyStrings,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onSpeak: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val index = selectedIndex.coerceIn(
        0,
        KD_EMERGENCY_PHRASES.lastIndex
    )

    val koreanPhrase = KD_EMERGENCY_PHRASES[index]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
        ) {
            val wide = maxWidth >= 650.dp

            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = words.helpTitle,
                            modifier = Modifier.weight(1f),
                            fontSize = if (wide) 26.sp else 21.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )

                        KDriveActionButton(
                            label = words.close,
                            onClick = onDismiss,
                            filled = false
                        )
                    }

                    Text(
                        text = words.helpQuestion,
                        fontSize = 19.sp,
                        lineHeight = 26.sp,
                        color = colors.onSurface
                    )

                    if (wide) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            words.situations.forEachIndexed { i, name ->
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    KDriveActionButton(
                                        label = name,
                                        onClick = { onSelect(i) },
                                        modifier = Modifier.fillMaxWidth(),
                                        filled = i == index
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            words.situations.forEachIndexed { i, name ->
                                KDriveActionButton(
                                    label = name,
                                    onClick = { onSelect(i) },
                                    modifier = Modifier.fillMaxWidth(),
                                    filled = i == index
                                )
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                colors.primary.copy(alpha = 0.10f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 20.dp,
                                    vertical = 26.dp
                                ),
                            horizontalAlignment =
                                Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            Text(
                                text = koreanPhrase,
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = if (wide) 37.sp else 29.sp,
                                lineHeight = if (wide) 51.sp else 41.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = words.meanings[index],
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = if (wide) 18.sp else 16.sp,
                                lineHeight = 26.sp,
                                color = colors.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Text(
                        text = words.showHint,
                        modifier = Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = colors.onSurfaceVariant
                    )

                    // This sends the Korean string to PC1 TTS.
                    KDriveActionButton(
                        label = "🔊 ${words.play}",
                        onClick = {
                            onSpeak(koreanPhrase)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Accident - TTS Callback",
    widthDp = 1000,
    heightDp = 650,
    showBackground = true
)
@Composable
private fun KdAccidentPreview() {
    KDriveTheme(KDRIVE_THEME_PRESETS.first()) {
        KDriveAccidentEmergencyScreen(
            uiLanguage = "KO",
            onBack = {},
            onSpeakRequest = {},
            onStopVoiceRequest = {}
        )
    }
}