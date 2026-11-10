
package com.example.pleosteamapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.DestinationInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.constants.NaviErrorCode

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener

import java.util.Locale

class KDriveGuideActivity : ComponentActivity() {

    private val kdriveNavPriorityGate by lazy {
        KDriveNavPriorityGate(
            applicationContext,
            log = { message ->
                android.util.Log.d("KDRIVE_AUDIO_GATE", message)
            },
            stopOurTts = {
                try {
                    tts?.stop()
                } catch (e: Exception) {
                    android.util.Log.e(
                        "KDRIVE_AUDIO_GATE",
                        "TTS_STOP_FAILED",
                        e
                    )
                }
            }
        )
    }


    companion object {
        private const val TAG = "KDRIVE_UNIFIED"

        // 실제 톨게이트가 아닌 GPS 시험 좌표
        private const val TEST_LAT = 37.399000
        private const val TEST_LON = 127.120000

        private const val ARM_METERS = 1000f
        private const val TRIGGER_METERS = 500f

        private const val DEMO_START_METERS = 1500
        private const val DEMO_TICK_MS = 650L
    }

    private enum class GuideMode {
        DEMO,
        GPS_TEST
    }

    private val guideEngine = KDriveGuideEngine()
    private val speechGate = KDriveSpeechGate()

    private val handler = Handler(Looper.getMainLooper())
    private var destroyed = false

    // ========================================
    // NaviHelper 상태
    // ========================================

    private var naviHelper: NaviHelper? = null
    private var naviListenerRegistered = false

    private var naviConnected by mutableStateOf(false)
    private var naviStatus by mutableStateOf("연결 대기")

    private var destinationText by mutableStateOf(
        "목적지 정보 대기"
    )

    private var routeStatus by mutableStateOf(
        "경로 정보를 기다리는 중"
    )

    private var routeCues by mutableStateOf(
        emptyList<KDriveGuideCue>()
    )

    // ========================================
    // TTS 상태
    // ========================================

    private var tts: TextToSpeech? = null
    private var ttsListenerRegistered = false

    private var ttsReady by mutableStateOf(false)
    private var ttsStatus by mutableStateOf("초기화 중")

    private var lastEnglish by mutableStateOf(
        "아직 안내 없음"
    )

    // ========================================
    // 가상 주행 상태
    // ========================================

    private var mode by mutableStateOf(GuideMode.DEMO)
    private var demoQueue = emptyList<KDriveGuideCue>()

    private var demoRunning by mutableStateOf(false)
    private var demoIndex by mutableIntStateOf(0)
    private var demoMeters by mutableIntStateOf(
        DEMO_START_METERS
    )

    private var demoStatus by mutableStateOf(
        "시연 시작 전"
    )

    // ========================================
    // GPS 시험 상태
    // ========================================

    private var locationManager: LocationManager? = null
    private var gpsRegistered = false

    private var gpsStatus by mutableStateOf("GPS 대기")
    private var gpsLat by mutableStateOf<Double?>(null)
    private var gpsLon by mutableStateOf<Double?>(null)
    private var gpsDistance by mutableStateOf<Float?>(null)

    private var gpsArmed by mutableStateOf(false)
    private var gpsAnnounced by mutableStateOf(false)

    private var gpsTestStatus by mutableStateOf(
        "GPS 시험 모드를 선택하세요"
    )

    // ========================================
    // NaviHelper 이벤트
    // ========================================

    private val naviListener =
        object : NaviHelperEventListener {

            override fun onDestinationChanged() {
                runOnUiThread {
                    if (destroyed) return@runOnUiThread

                    stopDemo()
                    routeCues = emptyList()
                    speechGate.resetForNewRoute()

                    destinationText =
                        "목적지 변경 감지. 정보 갱신 중"

                    refreshRoute()
                }
            }

            override fun onDestinationInfo(
                destinationInfo: DestinationInfo
            ) {
                runOnUiThread {
                    if (destroyed) return@runOnUiThread

                    destinationText =
                        destinationInfo.toString()

                    requestTbt()
                }
            }

            override fun onRoutingCompleted() {
                runOnUiThread {
                    if (!destroyed) {
                        requestTbt()
                    }
                }
            }

            override fun onTBTInfo(
                tbtInfoList: List<TBTInfo>
            ) {
                runOnUiThread {
                    if (destroyed) return@runOnUiThread

                    val classified =
                        guideEngine.classifyTbt(
                            tbtInfoList
                        )

                    routeCues = classified

                    val tolls = classified.count {
                        it.situation == KDriveSituation.TOLL
                    }

                    val serviceAreas = classified.count {
                        it.situation ==
                                KDriveSituation.SERVICE_AREA
                    }

                    val drowsyRest = classified.count {
                        it.situation ==
                                KDriveSituation.DROWSY_REST
                    }

                    val roundabouts = classified.count {
                        it.situation ==
                                KDriveSituation.ROUNDABOUT_ENTER
                    }

                    val highwayEntries = classified.count {
                        it.situation ==
                                KDriveSituation.HIGHWAY_ENTER
                    }

                    val highwayExits = classified.count {
                        it.situation ==
                                KDriveSituation.HIGHWAY_EXIT
                    }

                    routeStatus =
                        "TBT ${tbtInfoList.size}개 수신\n" +
                                "안내 상황 ${classified.size}개 분류\n" +
                                "톨게이트 $tolls / " +
                                "휴게소 $serviceAreas / " +
                                "졸음쉼터 $drowsyRest\n" +
                                "회전교차로 진입 $roundabouts / " +
                                "고속도로 진입 $highwayEntries / " +
                                "고속도로 진출 $highwayExits"

                    Log.d(
                        TAG,
                        "ENGINE_CLASSIFIED " +
                                "total=${tbtInfoList.size} " +
                                "cues=${classified.size} " +
                                "roundabout=$roundabouts"
                    )

                    classified.forEach { cue ->
                        Log.d(
                            TAG,
                            "CUE " +
                                    "type=${cue.tbtType} " +
                                    "situation=${cue.situation.name} " +
                                    "location=${cue.locationLabel} " +
                                    "source=${cue.source} " +
                                    "rawDistance=${cue.rawDistance}"
                        )
                    }
                }
            }

            override fun onError(errorCode: NaviErrorCode) {
                Log.e(TAG, "NAVI_ERROR: $errorCode")

                runOnUiThread {
                    if (!destroyed) {
                        naviStatus =
                            "내비게이션 오류: $errorCode"
                    }
                }
            }
        }

    // ========================================
    // NaviHelper 연결
    // ========================================

    private fun initializeNavi() {
        try {
            val helper = NaviHelper(applicationContext)
            naviHelper = helper

            helper.initialize(
                onConnected = {
                    runOnUiThread {
                        if (destroyed) return@runOnUiThread

                        try {
                            helper.addListener(naviListener)

                            naviListenerRegistered = true
                            naviConnected = true
                            naviStatus = "연결 완료"

                            refreshRoute()

                        } catch (error: Exception) {
                            naviStatus =
                                "리스너 등록 실패: ${error.message}"

                            Log.e(
                                TAG,
                                "NAVI_LISTENER_FAILED",
                                error
                            )
                        }
                    }
                },

                onFailed = { error ->
                    runOnUiThread {
                        if (!destroyed) {
                            naviConnected = false

                            naviStatus =
                                "SDK 연결 실패: ${error.message}"
                        }
                    }
                }
            )
        } catch (error: Exception) {
            naviStatus =
                "SDK 초기화 실패: ${error.message}"

            Log.e(TAG, "NAVI_INIT_FAILED", error)
        }
    }

    private fun refreshRoute() {
        requestDestination()
        requestTbt()
    }

    private fun requestDestination() {
        if (!naviConnected || destroyed) return

        try {
            naviHelper?.getDestinationInfo()
        } catch (error: Exception) {
            destinationText =
                "목적지 조회 실패: ${error.message}"
        }
    }

    private fun requestTbt() {
        if (!naviConnected || destroyed) return

        try {
            routeStatus = "TBT 조회 중"
            naviHelper?.getTBTInfo()

        } catch (error: Exception) {
            routeStatus =
                "TBT 조회 실패: ${error.message}"

            Log.e(TAG, "TBT_REQUEST_FAILED", error)
        }
    }

    // ========================================
    // TTS
    // ========================================

    private val ttsListener = object : EventListener {

        override fun onReady() {
            updateTtsStatus("READY")
        }

        override fun onStart() {
            updateTtsStatus("PLAYBACK START")
        }

        override fun onUpdatedRms(rms: Double) {
        }

        override fun onDone() {
            updateTtsStatus("PLAYBACK DONE")
        }

        override fun onStop() {
            updateTtsStatus("PLAYBACK STOP")
        }

        override fun onError(errorType: ErrorType) {
            updateTtsStatus("ERROR: $errorType")
        }
    }

    private fun updateTtsStatus(message: String) {
        Log.d(TAG, "TTS $message")

        runOnUiThread {
            if (!destroyed) {
                ttsStatus = message
            }
        }
    }

    private fun initializeTts() {
        try {
            val speech = TextToSpeech(
                applicationContext,
                Mode.ON_DEVICE
            )

            tts = speech

            speech.initialize(
                object : OnInitializeListener {

                    override fun onSuccess() {
                        if (destroyed) return

                        try {
                            speech.addEventListener(ttsListener)
                            ttsListenerRegistered = true

                            speech.setLocale(Locale.US)

                            runOnUiThread {
                                if (!destroyed) {
                                    ttsReady = true

                                    ttsStatus =
                                        "영어 TTS 준비 완료"

                                    maybeAnnounceGps()
                                }
                            }

                        } catch (error: Exception) {
                            Log.e(
                                TAG,
                                "TTS_SETUP_FAILED",
                                error
                            )

                            updateTtsStatus(
                                "TTS 설정 실패: ${error.message}"
                            )
                        }
                    }

                    override fun onFailure(
                        errorType: ErrorType
                    ) {
                        updateTtsStatus(
                            "TTS 초기화 실패: $errorType"
                        )
                    }
                }
            )
        } catch (error: Exception) {
            Log.e(TAG, "TTS_INIT_FAILED", error)

            updateTtsStatus(
                "TTS 초기화 예외: ${error.message}"
            )
        }
    }

    private fun speakEnglish(message: String): Boolean {

        if (!kdriveNavPriorityGate.maySpeak()) {
            android.util.Log.w(
                "KDRIVE_AUDIO_GATE",
                "GUIDANCE_BLOCKED_BY_NAV_AUDIO"
            )
            return false
        }

        if (!ttsReady || destroyed) return false

        val speech = tts ?: return false

        return try {
            speech.speak(message)

            lastEnglish = message

            Log.d(TAG, "SPEAK_REQUEST $message")
            true

        } catch (error: Exception) {
            Log.e(TAG, "SPEAK_FAILED", error)
            false
        }
    }

    // ========================================
    // 실제 경로 우선 시연 목록 생성
    // ========================================

    private fun createDemoQueue(): List<KDriveGuideCue> {

        // 각 상황을 전체 routeCues에서 직접 검색한다.
        // 따라서 뒤쪽에 있는 회전교차로도 누락되지 않는다.
        val wantedSituations = listOf(
            KDriveSituation.TOLL,
            KDriveSituation.SERVICE_AREA,
            KDriveSituation.DROWSY_REST,
            KDriveSituation.ROUNDABOUT_ENTER
        )

        val selected = wantedSituations.mapNotNull { situation ->
            routeCues.firstOrNull {
                it.situation == situation
            }
        }.toMutableList()

        val hasRealRoundabout = routeCues.any {
            it.situation ==
                    KDriveSituation.ROUNDABOUT_ENTER
        }

        // 실제 회전교차로 TBT가 전혀 없을 때만
        // 별도의 시뮬레이션 데이터를 사용한다.
        if (!hasRealRoundabout) {
            guideEngine.makeSimulationCue(
                KDriveSituation.ROUNDABOUT_ENTER
            )?.let { simulatedCue ->
                selected.add(simulatedCue)
            }
        }

        // 실제 경로 안내는 TBT 등장 순서로 정렬.
        // 가상 항목은 맨 마지막으로 이동한다.
        return selected.sortedBy { cue ->
            val position = routeCues.indexOfFirst {
                it.id == cue.id
            }

            if (position >= 0) {
                position
            } else {
                Int.MAX_VALUE
            }
        }
    }

    private val demoTask = object : Runnable {

        override fun run() {
            if (
                destroyed ||
                !demoRunning ||
                mode != GuideMode.DEMO
            ) return

            if (demoIndex >= demoQueue.size) {
                finishDemo()
                return
            }

            val cue = demoQueue[demoIndex]

            demoMeters =
                (demoMeters - 100).coerceAtLeast(0)

            demoStatus =
                "${cue.locationLabel} 가상 접근: ${demoMeters}m"

            if (demoMeters <= 500) {
                if (
                    speechGate.canAnnounce(
                        cue,
                        approachConfirmed = true
                    )
                ) {
                    val prefix =
                        if (
                            cue.source ==
                            KDriveGuideSource.SIMULATION
                        ) {
                            "Simulation. "
                        } else {
                            "Route based simulation. "
                        }

                    val message =
                        prefix + cue.approachEnglish

                    if (speakEnglish(message)) {
                        speechGate.markAnnounced(cue)

                        Log.d(
                            TAG,
                            "DEMO_TRIGGER " +
                                    "type=${cue.situation.name} " +
                                    "source=${cue.source} " +
                                    "name=${cue.locationLabel} " +
                                    "distance=$demoMeters"
                        )
                    }
                }
            }

            if (demoMeters == 0) {
                demoIndex++

                if (demoIndex >= demoQueue.size) {
                    finishDemo()
                    return
                }

                demoMeters = DEMO_START_METERS
            }

            handler.postDelayed(
                this,
                DEMO_TICK_MS
            )
        }
    }

    private fun beginDemo() {
        if (destroyed || demoRunning || !ttsReady) {
            return
        }

        if (routeCues.isEmpty()) {
            demoStatus = "먼저 TBT 경로를 조회해 주세요."
            return
        }

        val selected = createDemoQueue()

        if (selected.isEmpty()) {
            demoStatus = "시연 가능한 안내가 없습니다."
            return
        }

        speechGate.resetForNewRoute()

        demoQueue = selected
        demoIndex = 0
        demoMeters = DEMO_START_METERS
        demoRunning = true

        demoStatus =
            "가상 주행 시작: ${selected.size}개 장소"

        handler.removeCallbacks(demoTask)

        // 어떤 데이터를 사용할지 먼저 로그로 남긴다.
        selected.forEachIndexed { index, cue ->
            Log.d(
                TAG,
                "DEMO_QUEUE " +
                        "index=$index " +
                        "type=${cue.situation.name} " +
                        "source=${cue.source} " +
                        "name=${cue.locationLabel}"
            )
        }

        Log.d(TAG, "DEMO_STARTED cues=${selected.size}")

        handler.post(demoTask)
    }

    private fun stopDemo() {
        demoRunning = false
        handler.removeCallbacks(demoTask)
        demoStatus = "시연 중지"
    }

    private fun finishDemo() {
        demoRunning = false
        handler.removeCallbacks(demoTask)

        demoStatus =
            "가상 주행 완료: ${demoQueue.size}개 안내 지점"

        Log.d(TAG, "DEMO_FINISHED")
    }

    // ========================================
    // Android GPS 시험
    // ========================================

    private val gpsListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {
            if (destroyed) return

            gpsLat = location.latitude
            gpsLon = location.longitude

            val results = FloatArray(1)

            Location.distanceBetween(
                location.latitude,
                location.longitude,
                TEST_LAT,
                TEST_LON,
                results
            )

            gpsDistance = results[0]
            gpsStatus = "Android GPS 위치 수신 중"

            if (mode == GuideMode.GPS_TEST) {
                if (
                    !gpsArmed &&
                    !gpsAnnounced &&
                    results[0] >= ARM_METERS
                ) {
                    gpsArmed = true

                    gpsTestStatus =
                        "1km 밖 확인. 시험 기준점 B로 이동하세요."

                    Log.d(
                        TAG,
                        "GPS_TEST_ARMED distance=${results[0]}"
                    )
                }

                maybeAnnounceGps()
            }
        }
    }

    private fun maybeAnnounceGps() {
        if (
            destroyed ||
            mode != GuideMode.GPS_TEST ||
            !gpsArmed ||
            gpsAnnounced ||
            !ttsReady
        ) return

        val distance = gpsDistance ?: return

        if (distance > TRIGGER_METERS) return

        val message =
            "GPS test. You have reached " +
                    "simulated checkpoint B. " +
                    "This is not a real toll gate alert."

        if (speakEnglish(message)) {
            gpsAnnounced = true
            gpsTestStatus = "GPS 거리 조건 자동 안내 성공"

            Log.d(
                TAG,
                "GPS_PROXIMITY_TRIGGER distance=$distance"
            )
        }
    }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                startGps()
            } else {
                gpsStatus = "정밀 위치 권한 없음"
            }
        }

    private fun ensureGpsPermission() {
        val granted = checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            startGps()
        } else {
            permissionLauncher.launch(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun startGps() {
        if (destroyed || gpsRegistered) return

        try {
            val manager = getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

            locationManager = manager

            if (
                !manager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            ) {
                gpsStatus = "GPS 제공자 비활성화"
                return
            }

            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                gpsListener,
                Looper.getMainLooper()
            )

            gpsRegistered = true
            gpsStatus = "GPS 수신 대기 중"

        } catch (error: Exception) {
            gpsStatus = "GPS 시작 실패: ${error.message}"
            Log.e(TAG, "GPS_START_FAILED", error)
        }
    }

    private fun stopGps() {
        if (!gpsRegistered) return

        try {
            locationManager?.removeUpdates(gpsListener)

        } catch (error: Exception) {
            Log.w(TAG, "GPS_STOP_FAILED", error)
        }

        gpsRegistered = false
    }

    private fun resetGpsTest() {
        gpsArmed = false
        gpsAnnounced = false

        gpsTestStatus =
            "GPS 시험 초기화. 먼저 위치 A를 입력하세요."
    }

    private fun switchMode(nextMode: GuideMode) {
        stopDemo()
        mode = nextMode

        if (nextMode == GuideMode.GPS_TEST) {
            resetGpsTest()
        }
    }

    // ========================================
    // 화면
    // ========================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        kdriveNavPriorityGate.start()

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),

                    verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                ) {
                    // KDRIVE_SITUATION_ENTRY_V1
                    Button(onClick = {
                        startActivity(
                            android.content.Intent(
                                this@KDriveGuideActivity,
                                KDriveSituationActivity::class.java
                            )
                        )
                    }) {
                        Text("상황별 안내 화면 열기")
                    }
                    Text(
                        "K-DRIVE GUIDE",
                        style =
                            MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        "외국인 운전자를 위한 한국 도로 안내",
                        style =
                            MaterialTheme.typography.titleMedium
                    )

                    HorizontalDivider()

                    Text(
                        "① NaviHelper 경로 분석",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Text("SDK 연결: $naviStatus")
                    Text(routeStatus)

                    Button(
                        onClick = { refreshRoute() },
                        enabled = naviConnected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("경로 정보 새로 조회")
                    }

                    Text(
                        "분류된 안내 목록: ${routeCues.size}개"
                    )

                    if (routeCues.isEmpty()) {
                        Text(
                            "경로에 안내 대상이 없거나 " +
                                    "아직 TBT를 받지 않았습니다."
                        )
                    } else {
                        routeCues.take(20).forEach { cue ->
                            Text(
                                "${cue.situation.koreanTitle}\n" +
                                        "장소: ${cue.locationLabel}\n" +
                                        "유형: ${cue.tbtType}"
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "② 안내 방식 선택",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Row(
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                switchMode(GuideMode.DEMO)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (mode == GuideMode.DEMO) {
                                    "시연 모드 ✓"
                                } else {
                                    "시연 모드"
                                }
                            )
                        }

                        Button(
                            onClick = {
                                switchMode(GuideMode.GPS_TEST)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (mode == GuideMode.GPS_TEST) {
                                    "GPS 시험 ✓"
                                } else {
                                    "GPS 시험"
                                }
                            )
                        }
                    }

                    Text("TTS 상태: $ttsStatus")

                    if (mode == GuideMode.DEMO) {
                        Text(
                            "실제 경로에서 안내 장소를 선택하고 " +
                                    "접근 거리만 가상으로 재현합니다."
                        )

                        Text(
                            "실제 회전교차로 TBT가 있으면 " +
                                    "해당 장소를 우선 사용합니다."
                        )

                        Button(
                            onClick = { beginDemo() },
                            enabled = ttsReady && !demoRunning,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("가상 주행 안내 시작")
                        }

                        Button(
                            onClick = { stopDemo() },
                            enabled = demoRunning,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("가상 주행 중지")
                        }

                        Text("시연 상태: $demoStatus")

                        if (
                            demoRunning &&
                            demoIndex < demoQueue.size
                        ) {
                            val cue = demoQueue[demoIndex]

                            Text(
                                "접근 대상: ${cue.locationLabel}"
                            )

                            Text(
                                "안내 유형: " +
                                        cue.situation.koreanTitle
                            )

                            Text(
                                "데이터 출처: " +
                                        if (
                                            cue.source ==
                                            KDriveGuideSource.SIMULATION
                                        ) {
                                            "시뮬레이션"
                                        } else {
                                            "실제 경로 기반 가상 주행"
                                        }
                            )

                            Text(
                                "가상 거리: ${demoMeters}m",
                                style =
                                    MaterialTheme.typography.headlineSmall
                            )
                        }

                    } else {
                        Text(
                            "GPS 시험 기준점 B:\n" +
                                    "$TEST_LAT, $TEST_LON"
                        )

                        Text(
                            "실제 톨게이트가 아닌 실험용 좌표입니다."
                        )

                        Text("GPS: $gpsStatus")

                        Text(
                            "현재 위치: " +
                                    "${gpsLat ?: "미수신"}, " +
                                    "${gpsLon ?: "미수신"}"
                        )

                        Text(
                            "기준점까지 직선거리: " +
                                    "${gpsDistance?.toInt() ?: "미수신"}m"
                        )

                        Text("1km 밖 확인: $gpsArmed")
                        Text("자동 안내 완료: $gpsAnnounced")
                        Text(gpsTestStatus)

                        Button(
                            onClick = { resetGpsTest() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("GPS 시험 초기화")
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "③ 마지막 영어 안내",
                        style =
                            MaterialTheme.typography.titleLarge
                    )

                    Text(lastEnglish)

                    Button(
                        onClick = {
                            try {
                                tts?.stop()
                            } catch (error: Exception) {
                                Log.e(
                                    TAG,
                                    "TTS_STOP_FAILED",
                                    error
                                )
                            }
                        },
                        enabled = ttsReady,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("음성 중지")
                    }

                    Text(
                        "실제 차량의 접근거리 연동과 " +
                                "백그라운드 자동 안내는 아직 미검증입니다.",
                        style =
                            MaterialTheme.typography.bodySmall
                    )

                    Text("Logcat: $TAG")
                }
            }
        }

        initializeTts()
        initializeNavi()
        ensureGpsPermission()
    }

    override fun onStart() {
        super.onStart()

        if (
            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startGps()
        }
    }

    override fun onStop() {
        stopGps()
        super.onStop()
    }

    override fun onDestroy() {

        kdriveNavPriorityGate.close()
        destroyed = true

        stopDemo()
        stopGps()
        handler.removeCallbacksAndMessages(null)

        try {
            naviHelper?.let { helper ->
                if (naviListenerRegistered) {
                    helper.removeListener(naviListener)
                }
                helper.release()
            }
        } catch (error: Exception) {
            Log.w(TAG, "NAVI_RELEASE_FAILED", error)
        }

        try {
            tts?.let { speech ->
                if (ttsListenerRegistered) {
                    speech.removeEventListener(ttsListener)
                }
                speech.release()
            }
        } catch (error: Exception) {
            Log.w(TAG, "TTS_RELEASE_FAILED", error)
        }

        super.onDestroy()
    }
}
