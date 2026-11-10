package com.example.pleosteamapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import java.util.Locale

private data class Situation(
    val id: String,
    val title: String,
    val en: String,
    val ko: String,
    val es: String? = null,
    val fr: String? = null
) {
    fun message(locale: String): String? = when (locale) {
        "ko-KR" -> ko
        "es-US" -> es
        "fr-CA" -> fr
        else -> en
    }
}

private object SituationContent {
    val all = listOf(
        Situation(
            "roundabout", "회전교차로",
            "Roundabout ahead. Slow down and yield to vehicles already circulating.",
            "회전교차로에 접근 중입니다. 속도를 줄이고 회전 중인 차량에 양보하세요.",
            "Se acerca a una rotonda. Reduzca la velocidad y ceda el paso a los vehículos que ya circulan.",
            "Vous approchez d'un carrefour giratoire. Ralentissez et cédez le passage aux véhicules déjà engagés."
        ),
        Situation(
            "toll", "톨게이트",
            "Toll gate ahead. Check the lane signs and your payment method. Follow the posted speed limit.",
            "톨게이트가 있습니다. 차로 표지와 결제 방법을 확인하고 표시된 제한속도를 지키세요.",
            "Se acerca a un peaje. Compruebe los carriles y su método de pago. Respete el límite de velocidad.",
            "Vous approchez d'un poste de péage. Vérifiez les voies et votre mode de paiement. Respectez la limite de vitesse."
        ),
        Situation(
            "rest", "휴게소",
            "A highway rest area is ahead. Follow the signs if you plan to stop.",
            "앞에 고속도로 휴게소가 있습니다. 정차할 계획이라면 안내 표지를 따르세요.",
            "Hay un área de descanso más adelante. Siga las señales si desea detenerse.",
            "Une aire de repos se trouve plus loin. Suivez les panneaux si vous souhaitez vous arrêter."
        ),
        Situation(
            "left_turn", "비보호 좌회전",
            "At a marked unprotected left turn, follow the permitted signal and yield to oncoming traffic and pedestrians. Do not turn on red.",
            "비보호 좌회전 표지와 허용된 신호를 확인하고, 맞은편 차량과 보행자에게 양보하세요. 적색 신호에서는 좌회전하지 마세요."
        ),
        Situation(
            "right_turn", "우회전",
            "In Korea, stop first at a red light before turning right. Follow any right-turn signal and watch for pedestrians.",
            "전방 적색 신호에서는 먼저 일시정지하세요. 우회전 전용 신호와 보행자도 확인하세요."
        ),
        Situation(
            "hipass_center", "하이패스센터",
            "A Hi-Pass service center is on your route. Follow the signs if you plan to visit.",
            "경로에 하이패스센터가 있습니다. 방문하려면 현장 안내표지를 확인하세요.",
            "Hay un centro de servicio Hi-Pass en su ruta. Siga las señales si desea visitarlo.",
            "Un centre de services Hi-Pass se trouve sur votre itinéraire. Suivez les panneaux si vous souhaitez vous y arrêter."
        ),
        Situation(
            "hipass", "하이패스 이용",
            "Check your vehicle's Hi-Pass payment setup before using an electronic toll lane.",
            "하이패스 전용 차로를 사용하기 전에 차량의 단말기와 결제수단을 확인하세요."
        ),
        Situation(
            "wrong_toll", "하이패스 오진입",
            "If you enter a Hi-Pass lane by mistake, do not stop suddenly or reverse. Arrange payment safely afterward.",
            "하이패스 차로에 잘못 진입했다면 급정지하거나 후진하지 마세요. 이후 안전한 곳에서 미납 통행료 처리 방법을 확인하세요."
        ),
        Situation(
            "school", "어린이보호구역 횡단보도",
            "At a crosswalk without traffic signals in a school zone, stop even if no pedestrian is crossing.",
            "어린이보호구역 내 신호기가 없는 횡단보도 앞에서는 보행자가 없어도 일시정지하세요."
        ),
        Situation(
            "bus", "버스전용차로",
            "Before using a bus-only lane, check the posted operating hours and vehicle eligibility.",
            "버스전용차로 이용 전 운영 시간과 차량 이용 자격을 확인하세요."
        ),
        Situation(
            "fuel", "주유소 이용",
            "Turn off the engine and check your vehicle's fuel type. Hwi-bal-yu means gasoline, and gyeong-yu means diesel.",
            "주유 전 시동을 끄고 차량의 연료 종류를 확인하세요. 휘발유는 가솔린, 경유는 디젤입니다."
        ),
        Situation(
            "parking", "주차장 이용",
            "Before leaving a parking lot, check whether prepayment or parking discount registration is required.",
            "주차장을 나가기 전에 사전 정산이나 주차 할인 등록이 필요한지 확인하세요."
        ),
        Situation(
            "departure", "출발 전 안내",
            "Before driving in Korea, review local traffic signs, turning rules and toll payment options.",
            "한국에서 운전하기 전에 도로표지, 회전 규칙, 통행료 결제 방법을 확인하세요."
        )
    )

    fun find(id: String): Situation? = all.firstOrNull { it.id == id }

    fun tbtId(type: String): String? {
        val value = type.uppercase(Locale.US)
        return when {
            value.contains("ROUNDABOUT") || value.contains("ROTARY") -> "roundabout"
            value.contains("TURN_TOLL") -> "toll"
            value.contains("RESTAREA") || value.contains("SERVICEAREA") -> "rest"
            else -> null
        }
    }
}

private data class DetectedCue(
    val key: String,
    val situationId: String,
    val place: String,
    val rawDistance: Double?
)

private data class SituationCard(
    val situationId: String,
    val place: String,
    val source: String,
    val distanceMeters: Int?
)

class KDriveSituationActivity : ComponentActivity() {

    // KDRIVE_NAV_MUTE_FIX_V1
    // Only PC1 voice state is changed here.
    private var demoVoiceMuted by mutableStateOf(false)

    private fun stopSpeechByUser() {
        if (demoRunning || demoFinishPending) {
            demoVoiceMuted = true
            Log.d(TAG, "DEMO_MUTED_BY_USER")
        }

        voiceQueue.cancelAll("USER_STOP")
        ttsStatus = "음성 중지"

        // The user may stop speech while the final
        // demo point is waiting for TTS completion.
        if (demoFinishPending) {
            finishAfterDemoVoice("USER_STOP")
        }
    }

    private fun cancelQueueForNav() {
        Log.d(TAG, "NAV_AUDIO_QUEUE_CANCEL")
        voiceQueue.cancelAll("NAV_AUDIO")
    }


    // KDRIVE_VOICE_QUEUE_BRIDGE_V2
    // The navigation gate is unchanged to avoid circular inference.
    private val voiceQueue: KDriveVoiceQueue by lazy {
        KDriveVoiceQueue(
            playSdk = { item: KDriveQueueItem ->
                val engine = tts
                    ?: throw IllegalStateException("TTS is not ready")

                engine.setLocale(Locale.forLanguageTag(item.locale))
                engine.speak(item.text)
            },
            stopSdk = {
                tts?.stop()
            },
            canSpeak = {
                ttsReady && gate.maySpeak()
            },
            log = { message: String ->
                Log.d(TAG, message)
            }
        )
    }


    // KDRIVE_ROUTE_RECOVERY_V1
    // This fingerprint identifies a TBT candidate, not an active route.
    private var lastAcceptedTbtSignature: String? = null
    private var cancelledTbtSignature: String? = null
    private var pendingTbtSignature: String? = null
    private var pendingTbtCount = 0

    private fun tbtSignature(items: List<TBTInfo>): String {
        return items.joinToString("|") { item ->
            val type = item.type.toString()
                .trim().uppercase(Locale.US)
            val place = item.description?.toString()
                ?.trim()?.lowercase(Locale.US).orEmpty()

            "$type:$place"
        }
    }

    private fun rememberPreviousRoute() {
        cancelledTbtSignature = lastAcceptedTbtSignature
        pendingTbtSignature = null
        pendingTbtCount = 0
    }


    // KDRIVE_DEMO_VOICE_COMPLETE_V1
    private var demoFinishPending = false
    private var demoAudioInProgress = false

    private val demoFinishWatchdog = Runnable {
        if (!closed && demoFinishPending) {
            Log.w(TAG, "DEMO_TTS_CALLBACK_TIMEOUT")
            finishAfterDemoVoice("TIMEOUT")
        }
    }

    private fun finishAfterDemoVoice(reason: String) {
        if (!demoFinishPending) return

        demoFinishPending = false
        demoAudioInProgress = false
        handler.removeCallbacks(demoFinishWatchdog)

        demoRunning = false
        activeCard = null
        lastSpeechSource = null
        lastVoice = ""

        Log.d(TAG, "DEMO_FINISHED_AFTER_TTS reason=$reason")
        Log.d(TAG, "DEMO_FINISHED")
    }

    private fun finishDemoWhenVoiceCompletes() {
        if (demoFinishPending) return

        demoRunning = false
        handler.removeCallbacks(demoTask)
        demoFinishPending = true

        Log.d(
            TAG,
            "DEMO_DISTANCE_COMPLETE voiceActive=$demoAudioInProgress"
        )

        if (!demoAudioInProgress) {
            finishAfterDemoVoice("NO_ACTIVE_VOICE")
            return
        }

        Log.d(TAG, "DEMO_WAIT_FOR_TTS_DONE")

        // Do not stop the TTS on a timeout.
        // Clear the stale demo state if no callback arrives.
        handler.removeCallbacks(demoFinishWatchdog)
        handler.postDelayed(demoFinishWatchdog, 20000L)
    }


    // KDRIVE_STABILITY_V1
    // PC1: route changes, duplicate demo cues and stale TBT protection.
    private val spokenDemoCueKeys = mutableSetOf<String>()

    @Volatile
    private var routeAcceptingTbt = true

    private var lastSpeechSource: String? = null

    private fun resetSituationSession(reason: String) {
        stopDemo()

        try {
            voiceQueue.cancelAll("ROUTE_RESET")
        } catch (error: Exception) {
            Log.e(TAG, "SESSION_TTS_STOP_ERROR", error)
        }

        lastSpeechSource = null
        lastVoice = ""
        realCues = emptyList()
        activeCard = null
        demoQueue = emptyList()
        demoIndex = 0
        demoDistance = 1500
        demoAnnounced = false
        spokenDemoCueKeys.clear()
        ttsStatus = "이전 안내 초기화"

        Log.d(TAG, "SESSION_RESET reason=$reason")
    }


    // KDRIVE_PC2_UI_INTEGRATION_V1
    private var showPc2Ui by mutableStateOf(true)
    // KDRIVE_PERSONAL_GUIDE_V1
    private var showTripPrep by mutableStateOf(false)
    private var drivingCountry by mutableStateOf("OTHER")
    private var debugParkedPreview by mutableStateOf(false)
    private var lastVoiceLocale = "en-US"

    private fun isDebugBuild(): Boolean {
        return (
            applicationInfo.flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
        ) != 0
    }

    private fun speakUiText(
        message: String,
        localeTag: String = "en-US"
    ) {
        // Only the explicitly enabled parked Debug preview
        // can request manual learning speech here.
        if (!isDebugBuild() || !debugParkedPreview) return

        if (message.isBlank() || !ttsReady) {
            ttsStatus = "Speech unavailable"
            return
        }

        if (!gate.maySpeak()) {
            ttsStatus = "Navigation audio has priority"
            Log.w("KDRIVE_SITUATION", "UI_SPEECH_BLOCKED")
            return
        }

        try {
            // Locale is applied when playback begins.
            val accepted = voiceQueue.offer(
                key = "UI|${message.hashCode()}|$localeTag",
                text = message,
                locale = localeTag,
                source = "UI"
            )

            if (!accepted) {
                ttsStatus = "음성 중복 또는 요청 차단"
                return
            }

            lastVoice = message
            lastVoiceLocale = localeTag
            ttsStatus = "UI speech requested"

            Log.d("KDRIVE_SITUATION", "UI_SPEAK locale=$localeTag")
        } catch (error: Exception) {
            ttsStatus = "Speech error: ${error.message}"
            Log.e("KDRIVE_SITUATION", "UI_SPEAK_ERROR", error)
        }
    }


    companion object {
        private const val TAG = "KDRIVE_SITUATION"
    }

    private val handler = Handler(Looper.getMainLooper())
    private var closed = false

    private var navi: NaviHelper? = null
    private var naviListenerAdded = false
    private var tts: TextToSpeech? = null
    private var ttsListenerAdded = false

    private var naviStatus by mutableStateOf("내비 연결 대기")
    private var ttsStatus by mutableStateOf("TTS 초기화 대기")
    private var ttsReady by mutableStateOf(false)
    private var language by mutableStateOf("en-US")

    private var realCues by mutableStateOf<List<DetectedCue>>(emptyList())
    private var activeCard by mutableStateOf<SituationCard?>(null)
    private var lastVoice by mutableStateOf("아직 안내 없음")

    private var demoRunning by mutableStateOf(false)

    // KDRIVE_DEMO_TTS_SYNC_HANDLER_V1
    private var demoDistanceHeldForVoice = false
    private var demoIndex = 0
    private var demoDistance = 1500
    private var demoAnnounced = false
    private var demoQueue = emptyList<DetectedCue>()

    private val gate: KDriveNavPriorityGate by lazy {
        KDriveNavPriorityGate(
            applicationContext,
            log = { Log.d(TAG, "AUDIO_GATE $it") },
            stopOurTts = {
                try { cancelQueueForNav() } catch (e: Exception) {
                    Log.e(TAG, "STOP_ON_NAV_FAILED", e)
                }
            }
        )
    }

    private fun chooseLanguage(value: String) {
        language = value
        getSharedPreferences("kdrive_situation", MODE_PRIVATE)
            .edit().putString("locale", value).apply()
    }

    private fun playSituation(id: String, source: String) {

        if (source == "DEMO") {
            val cueKey =
                "$demoIndex|${activeCard?.place?.trim()?.lowercase(Locale.US).orEmpty()}|$id"

            if (!spokenDemoCueKeys.add(cueKey)) {
                Log.d(TAG, "DEMO_DUPLICATE_SKIPPED key=$cueKey")
                return
            }
        }

        val item = SituationContent.find(id) ?: return
        val message = KDriveExtraContent.forSituation(id, language, drivingCountry)
            ?: item.message(language)

        if (message.isNullOrBlank()) {
            ttsStatus = "이 언어의 문구가 아직 준비되지 않았습니다."
            Log.w(TAG, "VOICE_MISSING id=$id locale=$language")
            return
        }

        if (!ttsReady) {
            ttsStatus = "TTS 준비되지 않음"
            return
        }

        if (!gate.maySpeak()) {
            ttsStatus = "기본 내비 오디오 우선: 이번 안내 생략"
            Log.w(TAG, "VOICE_BLOCKED id=$id")
            return
        }

        try {
            // Locale is set when the queued request starts.
            val spoken = if (source == "DEMO") {
                if (language == "ko-KR") "시뮬레이션. $message"
                else if (language == "en-US") "Simulation. $message"
                else message
            } else message

            if (source == "DEMO") {
                demoAudioInProgress = true
            }
            val key = if (source == "DEMO") {
                "DEMO|$demoIndex|${activeCard?.place}|$id"
            } else {
                "$source|$id|$language|${System.nanoTime()}"
            }

            val accepted = voiceQueue.offer(
                key = key,
                text = spoken,
                locale = language,
                source = source
            )

            if (!accepted) {
                if (source == "DEMO") {
                    demoAudioInProgress =
                        voiceQueue.hasSource("DEMO")
                }
                ttsStatus = "음성 요청 차단"
                return
            }
            lastSpeechSource = source
            lastVoice = spoken
            lastVoiceLocale = language
            ttsStatus = "재생 요청: ${item.title}"
            Log.d(TAG, "SPEAK id=$id source=$source locale=$language")
        } catch (e: Exception) {
            ttsStatus = "재생 오류: ${e.message}"
            Log.e(TAG, "SPEAK_ERROR", e)
        }
    }

    private val speechListener = object : EventListener {
        override fun onReady() { Log.d(TAG, "TTS_READY") }
        override fun onStart() {
            runOnUiThread {
                if (!closed) ttsStatus = "재생 중"
            }
        }
        override fun onUpdatedRms(rms: Double) {}
        override fun onDone() {
            runOnUiThread {
                if (!closed) {
                    ttsStatus = "재생 완료"
                    voiceQueue.onFinished("DONE")
                    demoAudioInProgress =
                        voiceQueue.hasSource("DEMO")

                    if (demoFinishPending) {
                        finishAfterDemoVoice("PLAYBACK_DONE")
                    } else if (lastSpeechSource == "DEMO") {
                        lastSpeechSource = null
                    }
                }
            }
        }
        override fun onStop() {
            runOnUiThread {
                if (!closed) {
                    ttsStatus = "재생 중지"
                    voiceQueue.onFinished("STOP")
                    demoAudioInProgress =
                        voiceQueue.hasSource("DEMO")

                    if (demoFinishPending) {
                        finishAfterDemoVoice("PLAYBACK_STOP")
                    } else if (lastSpeechSource == "DEMO") {
                        lastSpeechSource = null
                    }
                }
            }
        }
        override fun onError(errorType: ErrorType) {
            runOnUiThread {
                if (!closed) {
                    ttsStatus = "TTS 오류: $errorType"
                    voiceQueue.onFinished("ERROR")
                    demoAudioInProgress =
                        voiceQueue.hasSource("DEMO")

                    if (demoFinishPending) {
                        finishAfterDemoVoice("TTS_ERROR")
                    }
                }
            }
        }
    }

    private val naviListener = object : NaviHelperEventListener {

        override fun onRouteStarted(
            routeStartInfo: ai.pleos.playground.navi.data.RouteStartInfo
        ) {
            runOnUiThread {
                if (closed) return@runOnUiThread

                rememberPreviousRoute()
                resetSituationSession("ROUTE_STARTED")
                routeAcceptingTbt = false

                naviStatus = "새 경로 탐색 중"
                Log.d(TAG, "ROUTE_SEARCH_STARTED")
            }
        }

        override fun onRoutingCompleted() {
            runOnUiThread {
                if (!closed) { resetSituationSession("ROUTING_COMPLETED"); routeAcceptingTbt = true; requestTbt() }
            }
        }

        override fun onDestinationChanged() {
            runOnUiThread {
                if (!closed) {
                    rememberPreviousRoute()
                    resetSituationSession("DESTINATION_CHANGED")
                    routeAcceptingTbt = false
                    Log.d(TAG, "DESTINATION_CHANGE_AWAITING_ROUTE")
                    requestTbt()
                }
            }
        }

        override fun onRouteCancelled() {
            runOnUiThread {
                if (!closed) {
                    rememberPreviousRoute()
                    resetSituationSession("ROUTE_CANCELLED")
                    routeAcceptingTbt = false
                    naviStatus = "경로 취소됨"
                    if (activeCard?.source == "REAL") activeCard = null
                }
            }
        }

        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {

            if (!routeAcceptingTbt) {
                val signature = tbtSignature(tbtInfoList)

                runOnUiThread {
                    if (closed || routeAcceptingTbt) return@runOnUiThread

                    val old = cancelledTbtSignature

                    if (
                        tbtInfoList.isEmpty() ||
                        old.isNullOrBlank() ||
                        signature == old
                    ) {
                        Log.d(
                            TAG,
                            "STALE_TBT_IGNORED_AFTER_ROUTE_CANCEL"
                        )
                        return@runOnUiThread
                    }

                    if (pendingTbtSignature != signature) {
                        pendingTbtSignature = signature
                        pendingTbtCount = 1

                        Log.d(TAG, "NEW_TBT_CANDIDATE_FIRST")

                        handler.postDelayed({
                            if (!closed && !routeAcceptingTbt) {
                                requestTbt()
                            }
                        }, 800L)
                    } else {
                        pendingTbtCount++
                    }

                    if (pendingTbtCount >= 2) {
                        resetSituationSession(
                            "DISTINCT_TBT_CANDIDATE_RECOVERY"
                        )

                        routeAcceptingTbt = true
                        pendingTbtSignature = null
                        pendingTbtCount = 0

                        naviStatus =
                            "새 TBT 후보 복구됨 · 실제 접근 미검증"

                        Log.d(
                            TAG,
                            "ROUTE_CANDIDATE_RECOVERED"
                        )

                        // Reprocess only as route candidates.
                        // Actual live automatic speech remains disabled.
                        onTBTInfo(tbtInfoList)
                    }
                }

                return
            }
            val cues = tbtInfoList.mapIndexedNotNull { index, item ->
                val type = item.type.toString()
                val id = SituationContent.tbtId(type)
                    ?: return@mapIndexedNotNull null

                val place = item.description?.toString()?.takeIf { it.isNotBlank() }
                    ?: type

                DetectedCue(
                    key = "${type.trim().uppercase(Locale.US)}|${place.trim().lowercase(Locale.US)}",
                    situationId = if (
                        id == "rest" &&
                        (
                            place.contains("하이패스", ignoreCase = true) ||
                            place.contains("Hi-Pass", ignoreCase = true) ||
                            place.contains("hipass", ignoreCase = true)
                        )
                    ) "hipass_center" else id,
                    place = place,
                    rawDistance = item.distance.toString().toDoubleOrNull()
                )
            }.distinctBy { it.key }

            runOnUiThread {
                if (closed) return@runOnUiThread

                if (!routeAcceptingTbt) {
                    Log.d(TAG, "TBT_UI_UPDATE_SUPPRESSED")
                    return@runOnUiThread
                }

                if (tbtInfoList.isNotEmpty()) {
                    lastAcceptedTbtSignature =
                        tbtSignature(tbtInfoList)
                }

                realCues = cues
                naviStatus = "TBT ${tbtInfoList.size}개 / 관련 지점 ${cues.size}개"

                Log.d(TAG, "REAL_TBT total=${tbtInfoList.size} matched=${cues.size}")

                // 경로에 지점이 있다는 사실만 표시합니다.
                // rawDistance의 실제 접근거리 의미가 검증되기 전에는
                // 이를 이용하여 자동 음성을 실행하지 않습니다.
                if (!demoRunning && cues.isNotEmpty()) {
                    val first = cues.first()
                    activeCard = SituationCard(
                        first.situationId,
                        first.place,
                        "REAL",
                        null
                    )
                }
            }
        }
    }

    private fun initializeNavi() {
        try {
            val helper = NaviHelper(applicationContext)
            navi = helper
            helper.initialize(
                onConnected = {
                    runOnUiThread {
                        if (closed) return@runOnUiThread
                        try {
                            helper.addListener(naviListener)
                            naviListenerAdded = true
                            naviStatus = "NaviHelper 연결됨"
                            requestTbt()
                        } catch (e: Exception) {
                            naviStatus = "리스너 오류: ${e.message}"
                        }
                    }
                },
                onFailed = { error ->
                    runOnUiThread {
                        if (!closed) naviStatus = "연결 오류: ${error.message}"
                    }
                }
            )
        } catch (e: Exception) {
            naviStatus = "NaviHelper 초기화 실패: ${e.message}"
        }
    }

    private fun requestTbt() {
        try {
            navi?.getTBTInfo()
            Log.d(TAG, "REQUEST_TBT")
        } catch (e: Exception) {
            naviStatus = "TBT 요청 실패: ${e.message}"
        }
    }

    private fun initializeTts() {
        try {
            val speech = TextToSpeech(applicationContext, Mode.ON_DEVICE)
            tts = speech

            speech.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    if (closed) return
                    try {
                        speech.addEventListener(speechListener)
                        ttsListenerAdded = true
                        speech.setLocale(Locale.forLanguageTag(language))

                        runOnUiThread {
                            if (!closed) {
                                ttsReady = true
                                ttsStatus = "TTS READY"
                            }
                        }
                    } catch (e: Exception) {
                        ttsStatus = "TTS 설정 실패: ${e.message}"
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    runOnUiThread {
                        if (!closed) ttsStatus = "초기화 실패: $errorType"
                    }
                }
            })
        } catch (e: Exception) {
            ttsStatus = "TTS 초기화 오류: ${e.message}"
        }
    }

    private val demoTask = object : Runnable {
        override fun run() {
            if (!demoRunning || closed) return

            // DEMO only: pause synthetic approach while speech is active.
            // Real navigation distance is not modified.
            val demoVoiceBusy =
                voiceQueue.hasSource("DEMO") ||
                voiceQueue.hasSource("UI")

            if (demoVoiceBusy) {
                if (!demoDistanceHeldForVoice) {
                    demoDistanceHeldForVoice = true
                    Log.d(
                        TAG,
                        "DEMO_DISTANCE_HOLD_TTS index=$demoIndex distance=$demoDistance"
                    )
                }

                handler.postDelayed(this, 150L)
                return
            }

            if (demoDistanceHeldForVoice) {
                demoDistanceHeldForVoice = false

                Log.d(
                    TAG,
                    "DEMO_DISTANCE_RESUME_TTS index=$demoIndex distance=$demoDistance"
                )

                // Brief gap after speech, then resume synthetic movement.
                handler.postDelayed(this, 600L)
                return
            }

            val cue = demoQueue.getOrNull(demoIndex)
            if (cue == null) {
                stopDemo()
                return
            }

            demoDistance = (demoDistance - 250).coerceAtLeast(0)
            activeCard = SituationCard(
                cue.situationId,
                cue.place,
                "DEMO",
                demoDistance
            )

            if (demoDistance <= 500 && !demoAnnounced) {
                demoAnnounced = true
                if (demoVoiceMuted) {
                    Log.d(
                        TAG,
                        "DEMO_VOICE_MUTED_SKIP id=${cue.situationId}"
                    )
                } else {
                    playSituation(cue.situationId, "DEMO")
                }
                Log.d(TAG, "DEMO_TRIGGER id=${cue.situationId} distance=$demoDistance")
            }

            if (demoDistance == 0) {
                demoIndex++

                if (demoIndex >= demoQueue.size) {
                    finishDemoWhenVoiceCompletes()
                    return
                }

                demoDistance = 1500
                demoAnnounced = false
                
                demoQueue.getOrNull(demoIndex)?.let { next ->
                    activeCard = SituationCard(
                        next.situationId,
                        next.place,
                        "DEMO",
                        demoDistance
                    )
                }
                handler.postDelayed(this, 6000L)
                return
            }

            handler.postDelayed(this, 750L)
        }
    }

    private fun startDemo() {
        stopDemo()
        demoVoiceMuted = false
        Log.d(TAG, "DEMO_VOICE_UNMUTED_ON_START")
        spokenDemoCueKeys.clear()

        demoQueue = if (realCues.isNotEmpty()) {
            realCues.take(3)
        } else {
            listOf(
                DetectedCue("demo-roundabout", "roundabout", "Sample Roundabout", null),
                DetectedCue("demo-toll", "toll", "Sample Toll Gate", null),
                DetectedCue("demo-rest", "rest", "Sample Rest Area", null)
            )
        }

        if (demoQueue.isEmpty()) return

        demoIndex = 0
        demoDistance = 1500
        demoAnnounced = false
        demoRunning = true

        val cue = demoQueue[0]
        activeCard = SituationCard(
            cue.situationId,
            cue.place,
            "DEMO",
            1500
        )

        Log.d(TAG, "DEMO_STARTED count=${demoQueue.size}")
        handler.postDelayed(demoTask, 700L)
    }

    private fun stopDemo() {
        demoDistanceHeldForVoice = false
        demoFinishPending = false
        demoAudioInProgress = false
        handler.removeCallbacks(demoFinishWatchdog)

        if (lastSpeechSource == "DEMO") {
            try {
                voiceQueue.cancelSource("DEMO", "DEMO_STOP")
            } catch (error: Exception) {
                Log.e(TAG, "DEMO_TTS_STOP_ERROR", error)
            }

            lastSpeechSource = null
            lastVoice = ""
            ttsStatus = "DEMO 음성 중지"
            Log.d(TAG, "DEMO_VOICE_STOP_REQUESTED")
        }

        if (activeCard?.source == "DEMO") {
            activeCard = null
        }

        demoRunning = false
        handler.removeCallbacks(demoTask)
        if (activeCard?.source == "DEMO") activeCard = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        language = getSharedPreferences("kdrive_situation", MODE_PRIVATE)
            .getString("locale", "en-US") ?: "en-US"
        drivingCountry = getSharedPreferences("kdrive_situation", MODE_PRIVATE)
            .getString("driving_country", "OTHER") ?: "OTHER"

        gate.start()
        initializeTts()
        initializeNavi()

        setContent {
            if (showTripPrep) {
                MaterialTheme {
                    KDrivePersonalGuideScreen(
                        selectedCountry = drivingCountry,
                        selectedLocale = language,
                        onCountryChange = { code ->
                            drivingCountry = code
                            getSharedPreferences("kdrive_situation", MODE_PRIVATE)
                                .edit().putString("driving_country", code).apply()
                        },
                        onLocaleChange = { code ->
                            chooseLanguage(code)
                        },
                        onPlay = { id ->
                            stopDemo()
                            val title = SituationContent.find(id)?.title ?: id
                            activeCard = SituationCard(id, title, "MANUAL", null)
                            playSituation(id, "MANUAL")
                        },
                        onStop = { stopSpeechByUser() },
                        onBack = { showTripPrep = false }
                    )
                }
            } else if (showPc2Ui) {
                // Actual approach is not yet verified.
                // Only DEMO cues are displayed as active guidance.
                val shown = activeCard?.takeIf {
                    it.source == "DEMO"
                }

                val item = shown?.let {
                    SituationContent.find(it.situationId)
                }

                val statusState = KDriveDrivingStatusUiState(
                    source = if (shown != null)
                        KDriveUiDataSource.DEMO
                    else
                        KDriveUiDataSource.NO_DATA,

                    navigationStatus = if (demoRunning)
                        "DEMO - Simulated approach"
                    else
                        "No verified live approach. TBT candidates: ${realCues.size}",

                    autoGuideEnabled = false,
                    demoRunning = demoRunning,
                    placeName = shown?.place,
                    distanceMeters = shown?.distanceMeters?.toDouble(),
                    guidanceTitle = item?.title,
                    guidanceText = item?.let {
                        KDriveExtraContent.forSituation(it.id, language, drivingCountry)
                            ?: it.message(language)
                    },
                    voiceStatus = ttsStatus
                )

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showPc2Ui = false
                                debugParkedPreview = false
                            }
                        ) {
                            Text("개발 화면")
                        }

                        Button(
                            enabled = isDebugBuild(),
                            onClick = {
                                debugParkedPreview = !debugParkedPreview
                            }
                        ) {
                            Text(
                                if (debugParkedPreview)
                                    "정차 미리보기 닫기"
                                else
                                    "정차 UI 미리보기"
                            )
                        }
                    }

                    Text(
                        "DEBUG ONLY · 정차 상태에서만 테스트하세요. " +
                        "실제 주행 규제 상태는 아직 연결하지 않았습니다."
                    )

                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        KDriveUiApp(
                            state = statusState,

                            onAutoGuideChange = { requested ->
                                Log.w(
                                    "KDRIVE_SITUATION",
                                    "LIVE_AUTO_NOT_ENABLED requested=$requested"
                                )
                            },

                            onDemoStartRequest = {
                                startDemo()
                            },

                            onDemoStopRequest = {
                                stopDemo()
                            },

                            onSpeakRequest = { message ->
                                // PC2 current rule content is English.
                                speakUiText(message, "en-US")
                            },

                            onStopVoiceRequest = {
                                try {
                                    stopSpeechByUser()
                                    ttsStatus = "Stopped"
                                } catch (error: Exception) {
                                    Log.e(
                                        "KDRIVE_SITUATION",
                                        "UI_STOP_ERROR",
                                        error
                                    )
                                }
                            },

                            onReplayRequest = {
                                if (lastVoice.isNotBlank()) {
                                    speakUiText(
                                        lastVoice,
                                        lastVoiceLocale
                                    )
                                }
                            },

                            // Unknown restrictions are fail-closed.
                            // Full UI is only a manually unlocked
                            // debug-only parked preview.
                            isDrivingRestricted = !(
                                isDebugBuild() &&
                                debugParkedPreview
                            )
                        )
                    }
                }
            } else MaterialTheme {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "K-DRIVE GUIDE · 상황별 안내",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        "실제 TBT 식별 + 접근 시뮬레이션",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "개발용 기능: 실제 주행 중 자동 음성은 아직 활성화하지 않았습니다.",
                        color = MaterialTheme.colorScheme.error
                    )

                    HorizontalDivider()

                    Text("안내 언어", style = MaterialTheme.typography.titleLarge)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { chooseLanguage("en-US") },
                            modifier = Modifier.weight(1f)
                        ) { Text("English") }
                        Button(
                            onClick = { chooseLanguage("ko-KR") },
                            modifier = Modifier.weight(1f)
                        ) { Text("한국어") }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { chooseLanguage("es-US") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Español") }
                        Button(
                            onClick = { chooseLanguage("fr-CA") },
                            modifier = Modifier.weight(1f)
                        ) { Text("Français") }
                    }

                    Text("선택: $language")
                    Text("TTS: $ttsStatus")
                    Text("NaviHelper: $naviStatus")

                    Button(
                        onClick = { requestTbt() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("현재 내비 경로 다시 조회") }

                    HorizontalDivider()

                    Text(
                        "현재 상황 안내",
                        style = MaterialTheme.typography.titleLarge
                    )

                    val card = activeCard

                    if (card == null) {
                        Text("아직 감지하거나 선택한 상황이 없습니다.")
                    } else {
                        val content = SituationContent.find(card.situationId)

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    when (card.source) {
                                        "DEMO" -> "DEMO · SIMULATION"
                                        "REAL" -> "REAL TBT · 접근 미검증"
                                        else -> "MANUAL · 정차 중 설명"
                                    },
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    content?.title ?: card.place,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Text("장소: ${card.place}")

                                if (card.distanceMeters != null) {
                                    Text(
                                        "가상 접근거리: ${card.distanceMeters}m",
                                        style = MaterialTheme.typography.titleLarge
                                    )
                                }

                                Text(
                                    content?.let {
                                        KDriveExtraContent.forSituation(it.id, language, drivingCountry)
                                            ?: it.message(language)
                                    }
                                        ?: "해당 언어의 안내 문구가 아직 준비되지 않았습니다.",
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                if (card.source == "REAL") {
                                    Text(
                                        "이 지점은 실제 경로에서 발견됐지만 현재 접근 여부는 검증되지 않았습니다."
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { startDemo() },
                            enabled = !demoRunning,
                            modifier = Modifier.weight(1f)
                        ) { Text("접근 시연 시작") }

                        Button(
                            onClick = { stopDemo() },
                            enabled = demoRunning,
                            modifier = Modifier.weight(1f)
                        ) { Text("시연 중지") }
                    }

                    Button(
                        onClick = { stopSpeechByUser() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("음성 중지") }

                    Text("마지막 음성: $lastVoice")

                    HorizontalDivider()

                    Text(
                        "실제 경로에서 찾은 안내 대상",
                        style = MaterialTheme.typography.titleLarge
                    )

                    if (realCues.isEmpty()) {
                        Text("현재 관련 TBT 없음. 경로를 설정한 뒤 다시 조회하세요.")
                    }

                    realCues.take(10).forEach { cue ->
                        Text(
                            "${SituationContent.find(cue.situationId)?.title} / " +
                                "${cue.place} / rawDistance=${cue.rawDistance ?: "미수신"}"
                        )
                    }

                    HorizontalDivider()

                    Text(
                        "정차 중 상황별 상세 안내",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "실제 주행 상황 감지가 어려운 항목은 여기서 직접 학습합니다."
                    )

                    SituationContent.all.forEach { situation ->
                        Button(
                            onClick = {
                                stopDemo()
                                activeCard = SituationCard(
                                    situation.id,
                                    situation.title,
                                    "MANUAL",
                                    null
                                )
                                playSituation(situation.id, "MANUAL")
                            },
                            enabled = ttsReady,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${situation.title} · 설명 듣기")
                        }
                    }

                    HorizontalDivider()

                    Button(
                        onClick = { finish() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("기존 K-DRIVE GUIDE로 돌아가기") }

                    Button(
                        onClick = { showTripPrep = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("국가별 · 주유 · 주차 가이드")
                    }

                    Text("Logcat: KDRIVE_SITUATION")

                    Button(
                        onClick = { showPc2Ui = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("PC2 통합 UI 열기")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        voiceQueue.close()
        closed = true
        stopDemo()

        try { gate.close() } catch (_: Exception) {}

        try {
            navi?.let {
                if (naviListenerAdded) it.removeListener(naviListener)
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "NAVI_RELEASE_ERROR", e)
        }

        try {
            tts?.let {
                it.stop()
                if (ttsListenerAdded) it.removeEventListener(speechListener)
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "TTS_RELEASE_ERROR", e)
        }

        super.onDestroy()
    }
}