package com.example.pleosteamapp

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/*
 * HOME BRIEFING V5
 *
 * Before You Drive opens the country briefing directly.
 * Existing PC1 callback signature is preserved.
 * Startup screen is unchanged.
 */

private fun hbLanguage(raw: String?): String = when (
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

private data class HbWords(
    val guide: String,
    val welcome: String,
    val welcomeSub: String,
    val before: String,
    val beforeSub: String,
    val beforeFacts: List<String>,
    val beforeOpen: String,
    val rules: String,
    val rulesSub: String,
    val ruleFacts: List<String>,
    val rulesOpen: String,
    val emergency: String,
    val emergencySub: String,
    val numbers: String,
    val back: String,
    val status: String,
    val restricted: String,
    val debug: String,
    val debugHint: String,
    val driving: String,
    val demo: String,
    val restart: String,
    val close: String
)

private fun hbWords(lang: String): HbWords = when (lang) {
    "KO" -> HbWords(
        guide = "한국 운전 가이드",
        welcome = "한국에서의 운전, K-DRIVE가 함께합니다.",
        welcomeSub = "출발 전 꼭 필요한 정보를 확인해 보세요.",
        before = "Before You Drive",
        beforeSub = "익숙한 운전 습관과 한국의 차이",
        beforeFacts = listOf(
            "평소 운전하던 국가 선택",
            "먼저 확인할 핵심 3가지",
            "8개 주제 비교·현지 안내"
        ),
        beforeOpen = "맞춤 안내 열기",
        rules = "교통규칙",
        rulesSub = "상황별 행동 요령과 음성 학습",
        ruleFacts = listOf(
            "교차로·회전교차로",
            "횡단보도·안전운전",
            "통행료·버스전용차로"
        ),
        rulesOpen = "교통규칙 살펴보기",
        emergency = "사고 및 긴급상황",
        emergencySub = "대응 방법과 긴급 연락처",
        numbers = "경찰 112  ·  소방·구급 119",
        back = "← 뒤로",
        status = "현재 상태",
        restricted = "주행 중에는 상세 학습 메뉴를 이용할 수 없습니다.",
        debug = "시연 및 진단",
        debugHint = "DEBUG 전용입니다. 안전하게 주차한 상태에서만 사용하세요.",
        driving = "주행 상태 확인",
        demo = "데모 모드 시작",
        restart = "데모 다시 시작",
        close = "닫기"
    )

    "FR" -> HbWords(
        guide = "Guide de conduite en Corée",
        welcome = "Conduisez avec confiance en Corée.",
        welcomeSub = "Vérifiez l'essentiel avant de partir.",
        before = "Avant de conduire",
        beforeSub = "Vos habitudes et les règles coréennes",
        beforeFacts = listOf(
            "Votre pays de conduite habituel",
            "Trois points à vérifier d'abord",
            "Huit sujets à comparer"
        ),
        beforeOpen = "Ouvrir le briefing",
        rules = "Code de la route",
        rulesSub = "Conseils par situation et écoute audio",
        ruleFacts = listOf(
            "Intersections et giratoires",
            "Piétons et sécurité",
            "Péages et voies de bus"
        ),
        rulesOpen = "Voir les règles",
        emergency = "Accident et urgences",
        emergencySub = "Conseils et contacts utiles",
        numbers = "Police 112  ·  Secours 119",
        back = "← Retour",
        status = "État actuel",
        restricted = "Les menus détaillés ne sont pas disponibles pendant la conduite.",
        debug = "Démonstration et diagnostic",
        debugHint = "Mode DEBUG uniquement. À utiliser lorsque le véhicule est garé en lieu sûr.",
        driving = "État de conduite",
        demo = "Démarrer la démo",
        restart = "Redémarrer la démo",
        close = "Fermer"
    )

    "ES" -> HbWords(
        guide = "Guía de conducción en Corea",
        welcome = "Conduzca con confianza en Corea.",
        welcomeSub = "Revise lo esencial antes de salir.",
        before = "Antes de conducir",
        beforeSub = "Sus hábitos y las normas coreanas",
        beforeFacts = listOf(
            "País donde suele conducir",
            "Tres puntos que revisar primero",
            "Ocho temas para comparar"
        ),
        beforeOpen = "Abrir guía personalizada",
        rules = "Normas de tráfico",
        rulesSub = "Consejos por situación y audio",
        ruleFacts = listOf(
            "Cruces y glorietas",
            "Peatones y seguridad",
            "Peajes y carriles bus"
        ),
        rulesOpen = "Ver normas de tráfico",
        emergency = "Accidentes y emergencias",
        emergencySub = "Ayuda y contactos de emergencia",
        numbers = "Policía 112  ·  Emergencias 119",
        back = "← Volver",
        status = "Estado actual",
        restricted = "Los menús detallados no están disponibles al conducir.",
        debug = "Demostración y diagnóstico",
        debugHint = "Solo DEBUG. Úselo con el vehículo estacionado.",
        driving = "Estado de conducción",
        demo = "Iniciar demostración",
        restart = "Reiniciar demostración",
        close = "Cerrar"
    )

    else -> HbWords(
        guide = "Korean Driving Guide",
        welcome = "Drive with confidence in Korea.",
        welcomeSub = "Check the essentials before your journey.",
        before = "Before You Drive",
        beforeSub = "Your driving habits and Korean roads",
        beforeFacts = listOf(
            "Choose the country where you usually drive",
            "Three things to check first",
            "Eight topics on driving in Korea"
        ),
        beforeOpen = "Open your briefing",
        rules = "Traffic Rules",
        rulesSub = "Situation-based guidance and audio lessons",
        ruleFacts = listOf(
            "Intersections and roundabouts",
            "Crosswalks and road safety",
            "Tolls and bus-only lanes"
        ),
        rulesOpen = "Explore traffic rules",
        emergency = "Accident & Emergency",
        emergencySub = "Response guidance and important numbers",
        numbers = "Police 112  ·  Fire / EMS 119",
        back = "← Back",
        status = "Current status",
        restricted = "Detailed learning menus are unavailable while driving.",
        debug = "Demo & Diagnostics",
        debugHint = "DEBUG only. Use while safely parked.",
        driving = "Driving status",
        demo = "Start demo mode",
        restart = "Restart demo mode",
        close = "Close"
    )
}

@Composable
private fun HbIcon(type: String) {
    val tint = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(60.dp)
            .background(
                tint.copy(alpha = .10f),
                RoundedCornerShape(14.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.size(29.dp)
        ) {
            val w = size.width
            val h = size.height
            val sw = 2.4.dp.toPx()

            when (type) {
                "car" -> {
                    drawRoundRect(
                        tint,
                        Offset(w * .1f, h * .4f),
                        Size(w * .8f, h * .36f),
                        CornerRadius(5.dp.toPx()),
                        style = Stroke(sw)
                    )

                    drawLine(
                        tint,
                        Offset(w * .23f, h * .4f),
                        Offset(w * .35f, h * .18f),
                        sw
                    )

                    drawLine(
                        tint,
                        Offset(w * .35f, h * .18f),
                        Offset(w * .65f, h * .18f),
                        sw
                    )

                    drawLine(
                        tint,
                        Offset(w * .65f, h * .18f),
                        Offset(w * .77f, h * .4f),
                        sw
                    )

                    drawCircle(
                        tint,
                        2.dp.toPx(),
                        Offset(w * .27f, h * .79f)
                    )

                    drawCircle(
                        tint,
                        2.dp.toPx(),
                        Offset(w * .73f, h * .79f)
                    )
                }

                "book" -> {
                    drawRoundRect(
                        tint,
                        Offset(w * .08f, h * .18f),
                        Size(w * .4f, h * .62f),
                        CornerRadius(3.dp.toPx()),
                        style = Stroke(sw)
                    )

                    drawRoundRect(
                        tint,
                        Offset(w * .52f, h * .18f),
                        Size(w * .4f, h * .62f),
                        CornerRadius(3.dp.toPx()),
                        style = Stroke(sw)
                    )

                    drawLine(
                        tint,
                        Offset(w * .5f, h * .18f),
                        Offset(w * .5f, h * .82f),
                        sw
                    )
                }

                "shield" -> {
                    val path = Path().apply {
                        moveTo(w * .5f, h * .08f)
                        lineTo(w * .85f, h * .24f)
                        lineTo(w * .78f, h * .64f)

                        quadraticBezierTo(
                            w * .7f,
                            h * .84f,
                            w * .5f,
                            h * .94f
                        )

                        quadraticBezierTo(
                            w * .3f,
                            h * .84f,
                            w * .22f,
                            h * .64f
                        )

                        lineTo(w * .15f, h * .24f)
                        close()
                    }

                    drawPath(
                        path,
                        tint,
                        style = Stroke(sw)
                    )

                    drawLine(
                        tint,
                        Offset(w * .5f, h * .31f),
                        Offset(w * .5f, h * .6f),
                        sw
                    )

                    drawCircle(
                        tint,
                        1.6.dp.toPx(),
                        Offset(w * .5f, h * .73f)
                    )
                }

                else -> {
                    drawCircle(
                        tint,
                        w * .36f,
                        Offset(w * .5f, h * .5f),
                        style = Stroke(sw)
                    )

                    drawLine(
                        tint,
                        Offset(w * .29f, h * .5f),
                        Offset(w * .44f, h * .65f),
                        sw
                    )

                    drawLine(
                        tint,
                        Offset(w * .44f, h * .65f),
                        Offset(w * .73f, h * .32f),
                        sw
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HbWelcome(
    words: HbWords,
    debug: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    val touch = if (debug) {
        Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.clickable(
            onClick = onClick
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(touch),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            HbIcon("car")

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    words.welcome,
                    fontSize = 23.sp,
                    lineHeight = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Text(
                    words.welcomeSub,
                    fontSize = 17.sp,
                    lineHeight = 23.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun HbMainCard(
    title: String,
    subtitle: String,
    icon: String,
    facts: List<String>,
    action: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = modifier.clickable(
            role = Role.Button,
            onClick = onClick
        ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                HbIcon(icon)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        title,
                        fontSize = 25.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface
                    )

                    Text(
                        subtitle,
                        fontSize = 17.sp,
                        lineHeight = 24.sp,
                        color = colors.onSurfaceVariant
                    )
                }

                Text(
                    "›",
                    fontSize = 30.sp,
                    color = colors.primary
                )
            }

            facts.forEach { fact ->
                Text(
                    text = "•  $fact",
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            colors.primary.copy(alpha = .07f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(
                            horizontal = 15.dp,
                            vertical = 12.dp
                        ),
                    fontSize = 19.sp,
                    lineHeight = 26.sp,
                    color = colors.onSurface
                )
            }

            Text(
                text = "$action  ›",
                modifier = Modifier.align(Alignment.End),
                fontSize = 18.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
        }
    }
}

@Composable
private fun HbEmergency(
    words: HbWords,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.primary.copy(alpha = .10f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            HbIcon("shield")

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    words.emergency,
                    fontSize = 22.sp,
                    lineHeight = 29.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                )

                Text(
                    words.emergencySub + "  ·  " + words.numbers,
                    fontSize = 18.sp,
                    lineHeight = 26.sp,
                    color = colors.onSurfaceVariant
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

@Suppress("UNUSED_PARAMETER")
@Composable
fun KDriveHomeScreen(
    navigationStatus: String,
    autoGuideEnabled: Boolean,
    demoMode: Boolean,
    onAutoGuideChange: (Boolean) -> Unit,
    onTrafficRulesClick: () -> Unit,
    onDrivingStatusClick: () -> Unit,
    onDemoClick: () -> Unit,
    onThemeSettingsClick: () -> Unit = {},
    interactionRestricted: Boolean = true,
    onWelcomeClick: () -> Unit = {},
    onBackClick: () -> Unit = onWelcomeClick,
    showBackButton: Boolean = true,
    backLabel: String = "← Back",
    uiLanguage: String? = null,
    onEmergencySpeakRequest: (String) -> Unit = {},
    onEmergencyStopRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val colors = MaterialTheme.colorScheme

    val savedLanguage = remember(context, inspection) {
        if (inspection) {
            Locale.getDefault().language
        } else {
            context.applicationContext
                .getSharedPreferences(
                    "kdrive_ui_colors_v2",
                    Context.MODE_PRIVATE
                )
                .getString(
                    "startup_language",
                    Locale.getDefault().language
                )
        }
    }

    val lang = hbLanguage(uiLanguage ?: savedLanguage)
    val words = hbWords(lang)

    val debug = inspection ||
            (context.applicationInfo.flags and
                    ApplicationInfo.FLAG_DEBUGGABLE) != 0

    var screen by rememberSaveable {
        mutableStateOf("HOME")
    }

    var diagnostics by rememberSaveable {
        mutableStateOf(false)
    }

    val stopEmergency by rememberUpdatedState(
        onEmergencyStopRequest
    )

    LaunchedEffect(interactionRestricted) {
        if (interactionRestricted) {
            screen = "HOME"
            diagnostics = false
        }
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
                "K-DRIVE",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onBackground
            )

            Text(
                words.guide,
                fontSize = 21.sp,
                color = colors.primary
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.surface
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        words.status,
                        fontSize = 19.sp,
                        color = colors.onSurface
                    )

                    Text(
                        kdriveSafeMessage(navigationStatus),
                        fontSize = 25.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = colors.onSurface
                    )
                }
            }

            Text(
                words.restricted,
                fontSize = 20.sp,
                color = colors.onBackground
            )
        }
        return
    }

    BackHandler(
        enabled = screen != "HOME"
    ) {
        screen = "HOME"
    }

    when (screen) {
        "BEFORE", "COUNTRY" -> {
            KDriveCountryLanguageGuideScreen(
                onBack = {
                    screen = "HOME"
                },
                interactionRestricted = interactionRestricted,
                uiLanguage = lang,
                onTrafficRulesClick = {
                    screen = "HOME"
                    onTrafficRulesClick()
                }
            )
            return
        }

        "EMERGENCY" -> {
            DisposableEffect(Unit) {
                onDispose {
                    stopEmergency()
                }
            }

            KDriveAccidentEmergencyScreen(
                uiLanguage = lang,
                onBack = {
                    screen = "HOME"
                },
                onSpeakRequest = onEmergencySpeakRequest,
                onStopVoiceRequest = onEmergencyStopRequest
            )
            return
        }
    }

    if (debug && diagnostics) {
        AlertDialog(
            onDismissRequest = {
                diagnostics = false
            },
            title = {
                Text(words.debug)
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        words.debugHint,
                        fontSize = 17.sp
                    )

                    OutlinedButton(
                        onClick = {
                            diagnostics = false
                            onDrivingStatusClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 76.dp)
                    ) {
                        Text(
                            words.driving,
                            fontSize = 19.sp
                        )
                    }

                    Button(
                        onClick = {
                            diagnostics = false
                            onDemoClick()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 76.dp)
                    ) {
                        Text(
                            if (demoMode) {
                                words.restart
                            } else {
                                words.demo
                            },
                            fontSize = 19.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        diagnostics = false
                    },
                    modifier = Modifier.heightIn(min = 76.dp)
                ) {
                    Text(
                        words.close,
                        fontSize = 18.sp
                    )
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        val wide = maxWidth >= 780.dp

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(
                    horizontal = 22.dp,
                    vertical = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "K-DRIVE",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )

                    Text(
                        words.guide,
                        fontSize = 17.sp,
                        color = colors.primary
                    )
                }

                if (showBackButton) {
                    OutlinedButton(
                        onClick = onBackClick,
                        modifier = Modifier.heightIn(min = 76.dp)
                    ) {
                        Text(
                            if (backLabel == "← Back") {
                                words.back
                            } else {
                                backLabel
                            },
                            fontSize = 18.sp
                        )
                    }
                }
            }

            HbWelcome(
                words = words,
                debug = debug,
                onClick = onWelcomeClick,
                onLongClick = {
                    diagnostics = true
                }
            )

            val beforeCard:
                    @Composable (Modifier) -> Unit = { modifier ->

                HbMainCard(
                    title = words.before,
                    subtitle = words.beforeSub,
                    icon = "check",
                    facts = words.beforeFacts,
                    action = words.beforeOpen,
                    modifier = modifier,
                    onClick = {
                        screen = "BEFORE"
                    }
                )
            }

            val rulesCard:
                    @Composable (Modifier) -> Unit = { modifier ->

                HbMainCard(
                    title = words.rules,
                    subtitle = words.rulesSub,
                    icon = "book",
                    facts = words.ruleFacts,
                    action = words.rulesOpen,
                    modifier = modifier,
                    onClick = onTrafficRulesClick
                )
            }

            if (wide) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    beforeCard(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    rulesCard(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            } else {
                beforeCard(
                    Modifier.fillMaxWidth()
                )

                rulesCard(
                    Modifier.fillMaxWidth()
                )
            }

            HbEmergency(
                words = words,
                onClick = {
                    screen = "EMERGENCY"
                }
            )
        }
    }
}