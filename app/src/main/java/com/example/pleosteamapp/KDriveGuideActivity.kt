
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

    companion object {
        private const val TAG = "KDRIVE_UNIFIED"
        private const val GPS_TEST_LAT = 37.399000
        private const val GPS_TEST_LON = 127.120000
        private const val GPS_ARM_M = 1000f
        private const val GPS_TRIGGER_M = 500f
        private const val DEMO_TICK_MS = 650L
    }

    private enum class PlaceKind { TOLL, REST }

    private data class GuidePlace(
        val order: Int,
        val name: String,
        val kind: PlaceKind
    )

    private val mainHandler = Handler(Looper.getMainLooper())
    private var destroyed = false

    private var navi: NaviHelper? = null
    private var naviListenerAdded = false
    private var naviConnected by mutableStateOf(false)
    private var naviStatus by mutableStateOf("연결 중")
    private var destinationText by mutableStateOf("아직 조회하지 않음")
    private var routePlaces by mutableStateOf<List<GuidePlace>>(emptyList())
    private var routeStatus by mutableStateOf("경로 정보를 기다리는 중")

    private var engine: TextToSpeech? = null
    private var ttsListenerAdded = false
    private var ttsReady by mutableStateOf(false)
    private var ttsStatus by mutableStateOf("초기화 중")
    private var lastMessage by mutableStateOf("아직 안내 없음")

    private var mode by mutableStateOf("DEMO")
    private var demoStops = emptyList<GuidePlace>()
    private var demoRunning by mutableStateOf(false)
    private var demoIndex by mutableIntStateOf(0)
    private var demoMeters by mutableIntStateOf(1500)
    private var demoAnnounced = false
    private var demoStatus by mutableStateOf("시연 시작 전")

    private var locationManager: LocationManager? = null
    private var gpsRegistered = false
    private var gpsStatus by mutableStateOf("GPS 대기")
    private var gpsLat by mutableStateOf<Double?>(null)
    private var gpsLon by mutableStateOf<Double?>(null)
    private var gpsDistance by mutableStateOf<Float?>(null)
    private var gpsTestArmed by mutableStateOf(false)
    private var gpsTestAnnounced by mutableStateOf(false)
    private var gpsTestStatus by mutableStateOf("GPS 시험 모드를 선택해 주세요")

    private val locationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startGps()
        else gpsStatus = "정밀 위치 권한이 없습니다"
    }

    private val naviListener = object : NaviHelperEventListener {

        override fun onDestinationChanged() {
            runOnUiThread {
                if (!destroyed && naviConnected) {
                    destinationText = "목적지 변경 감지. 다시 조회합니다."
                    requestDestination()
                }
            }
        }

        override fun onDestinationInfo(destinationInfo: DestinationInfo) {
            runOnUiThread {
                if (!destroyed) {
                    destinationText = destinationInfo.toString()
                    requestTbt()
                }
            }
        }

        override fun onRoutingCompleted() {
            runOnUiThread {
                if (!destroyed && naviConnected) requestTbt()
            }
        }

        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
            runOnUiThread {
                if (destroyed) return@runOnUiThread

                val found = tbtInfoList.mapIndexedNotNull { index, item ->
                    val kind = when (item.type.toString()) {
                        "TURN_TOLL" -> PlaceKind.TOLL
                        "TURN_RESTAREA" -> PlaceKind.REST
                        else -> null
                    }

                    if (kind == null) null
                    else GuidePlace(
                        index,
                        item.description.toString(),
                        kind
                    )
                }.distinctBy { it.kind to it.name }

                routePlaces = found

                routeStatus =
                    "TBT ${tbtInfoList.size}개 수신 / " +
                            "톨게이트 ${found.count { it.kind == PlaceKind.TOLL }}개 / " +
                            "휴게소 ${found.count { it.kind == PlaceKind.REST }}개"

                Log.d(TAG, "TBT_RECEIVED total=${tbtInfoList.size} places=${found.size}")
            }
        }

        override fun onError(errorCode: NaviErrorCode) {
            runOnUiThread {
                if (!destroyed) naviStatus = "내비게이션 오류: $errorCode"
            }
            Log.e(TAG, "NAVI_ERROR $errorCode")
        }
    }

    private val speechListener = object : EventListener {
        override fun onReady() = updateTts("READY")
        override fun onStart() = updateTts("PLAYBACK START")
        override fun onUpdatedRms(rms: Double) {}
        override fun onDone() = updateTts("PLAYBACK DONE")
        override fun onStop() = updateTts("PLAYBACK STOP")
        override fun onError(errorType: ErrorType) = updateTts("ERROR: $errorType")
    }

    private fun updateTts(message: String) {
        Log.d(TAG, "TTS $message")
        runOnUiThread {
            if (!destroyed) ttsStatus = message
        }
    }

    private val gpsListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            if (destroyed) return

            gpsLat = location.latitude
            gpsLon = location.longitude

            val distances = FloatArray(1)

            Location.distanceBetween(
                location.latitude,
                location.longitude,
                GPS_TEST_LAT,
                GPS_TEST_LON,
                distances
            )

            gpsDistance = distances[0]
            gpsStatus = "Android GPS 위치 수신 중"

            if (mode == "GPS_TEST") {
                if (!gpsTestArmed && !gpsTestAnnounced &&
                    distances[0] >= GPS_ARM_M
                ) {
                    gpsTestArmed = true
                    gpsTestStatus = "1km 밖에서 대기 확인. 이제 시험 좌표 B로 이동하세요."
                    Log.d(TAG, "GPS_TEST_ARMED distance=${distances[0]}")
                }
                maybeAnnounceGps()
            }
        }
    }

    private val demoTask = object : Runnable {
        override fun run() {
            if (destroyed || !demoRunning || mode != "DEMO") return

            if (demoIndex >= demoStops.size) {
                finishDemo()
                return
            }

            demoMeters = (demoMeters - 100).coerceAtLeast(0)
            val place = demoStops[demoIndex]

            demoStatus = "${place.name} 가상 접근: ${demoMeters}m"

            if (!demoAnnounced && demoMeters <= 500) {
                demoAnnounced = true
                Log.d(TAG, "DEMO_TRIGGER ${place.name} at $demoMeters")
                speakEnglish(demoSentence(place))
            }

            if (demoMeters == 0) {
                demoIndex++

                if (demoIndex >= demoStops.size) {
                    finishDemo()
                    return
                }

                demoMeters = 1500
                demoAnnounced = false
            }

            mainHandler.postDelayed(this, DEMO_TICK_MS)
        }
    }

    private fun initializeNavi() {
        try {
            val helper = NaviHelper(applicationContext)
            navi = helper

            helper.initialize(
                onConnected = {
                    runOnUiThread {
                        if (!destroyed) {
                            try {
                                helper.addListener(naviListener)
                                naviListenerAdded = true
                                naviConnected = true
                                naviStatus = "연결 완료"
                                requestDestination()
                                requestTbt()
                            } catch (error: Exception) {
                                naviStatus = "리스너 연결 실패: ${error.message}"
                                Log.e(TAG, "NAVI_LISTENER_FAILED", error)
                            }
                        }
                    }
                },
                onFailed = { error ->
                    runOnUiThread {
                        if (!destroyed) {
                            naviStatus = "SDK 연결 실패: ${error.message}"
                        }
                    }
                }
            )
        } catch (error: Exception) {
            naviStatus = "SDK 초기화 실패: ${error.message}"
            Log.e(TAG, "NAVI_INIT_FAILED", error)
        }
    }

    private fun requestDestination() {
        if (!naviConnected || destroyed) return

        try {
            navi?.getDestinationInfo()
        } catch (error: Exception) {
            destinationText = "목적지 조회 실패: ${error.message}"
        }
    }

    private fun requestTbt() {
        if (!naviConnected || destroyed) return

        try {
            navi?.getTBTInfo()
            routeStatus = "TBT 응답 대기 중"
        } catch (error: Exception) {
            routeStatus = "TBT 조회 실패: ${error.message}"
            Log.e(TAG, "TBT_FAILED", error)
        }
    }

    private fun initializeTts() {
        try {
            val speech = TextToSpeech(applicationContext, Mode.ON_DEVICE)
            engine = speech

            speech.initialize(object : OnInitializeListener {

                override fun onSuccess() {
                    if (destroyed) return

                    try {
                        speech.addEventListener(speechListener)
                        ttsListenerAdded = true
                        speech.setLocale(Locale.US)

                        runOnUiThread {
                            if (!destroyed) {
                                ttsReady = true
                                ttsStatus = "영어 TTS 준비 완료"
                                maybeAnnounceGps()
                            }
                        }
                    } catch (error: Exception) {
                        updateTts("설정 실패: ${error.message}")
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    updateTts("초기화 실패: $errorType")
                }
            })
        } catch (error: Exception) {
            updateTts("초기화 예외: ${error.message}")
        }
    }

    private fun speakEnglish(message: String): Boolean {
        if (!ttsReady || destroyed) return false

        return try {
            engine?.speak(message) ?: return false
            lastMessage = message
            Log.d(TAG, "SPEAK_REQUEST $message")
            true
        } catch (error: Exception) {
            Log.e(TAG, "SPEAK_FAILED", error)
            false
        }
    }

    private fun englishTollName(korean: String): String = when {
        korean.contains("서울") -> "Seoul Toll Gate"
        korean.contains("청주") -> "Cheongju Toll Gate"
        else -> "the next toll gate"
    }

    private fun demoSentence(place: GuidePlace): String =
        when (place.kind) {
            PlaceKind.TOLL ->
                "Simulation. You are approaching ${englishTollName(place.name)}. " +
                        "Check the toll payment lane signs and drive safely."

            PlaceKind.REST ->
                "Simulation. A highway rest area is coming up. " +
                        "You may take a break if needed. Follow the road signs."
        }

    private fun beginDemo() {
        if (!ttsReady || routePlaces.isEmpty() || demoRunning) return

        demoStops = (
                routePlaces.filter { it.kind == PlaceKind.TOLL }.take(2) +
                        routePlaces.filter { it.kind == PlaceKind.REST }.take(1)
                ).sortedBy { it.order }

        if (demoStops.isEmpty()) return

        demoIndex = 0
        demoMeters = 1500
        demoAnnounced = false
        demoRunning = true
        demoStatus = "가상 주행 시작"

        mainHandler.removeCallbacks(demoTask)
        mainHandler.post(demoTask)
    }

    private fun finishDemo() {
        demoRunning = false
        mainHandler.removeCallbacks(demoTask)
        demoStatus = "가상 주행 완료: ${demoStops.size}개 안내 지점"
        Log.d(TAG, "DEMO_FINISHED")
    }

    private fun stopDemo() {
        demoRunning = false
        mainHandler.removeCallbacks(demoTask)
        demoStatus = "가상 주행 중지"
    }

    private fun switchMode(next: String) {
        stopDemo()
        mode = next
        if (next == "GPS_TEST") resetGpsTest()
    }

    private fun resetGpsTest() {
        gpsTestArmed = false
        gpsTestAnnounced = false
        gpsTestStatus = "GPS 시험: 먼저 좌표 A를 입력하세요"
        Log.d(TAG, "GPS_TEST_RESET")
    }

    private fun maybeAnnounceGps() {
        if (mode != "GPS_TEST" || !gpsTestArmed ||
            gpsTestAnnounced || !ttsReady
        ) return

        val distance = gpsDistance ?: return
        if (distance > GPS_TRIGGER_M) return

        val message =
            "GPS test. You have reached simulated checkpoint B. " +
                    "This is not a real toll gate alert."

        if (speakEnglish(message)) {
            gpsTestAnnounced = true
            gpsTestStatus = "GPS 거리 조건 자동 안내 성공 (중복 재생 차단)"
            Log.d(TAG, "GPS_PROXIMITY_TRIGGER distance=$distance")
        }
    }

    private fun ensureGps() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startGps()
        } else {
            locationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startGps() {
        if (destroyed || gpsRegistered) return

        try {
            val manager =
                getSystemService(Context.LOCATION_SERVICE) as LocationManager

            locationManager = manager

            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsStatus = "GPS 제공자가 꺼져 있음"
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
            gpsStatus = "GPS 변경 대기 중"
        } catch (error: Exception) {
            gpsStatus = "GPS 등록 실패: ${error.message}"
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "K-DRIVE GUIDE",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        "외국인 운전자를 위한 영어 주행 안내",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("현재 버전: 경로 정보 수신 + 접근 상황 시연 + GPS 시험")

                    HorizontalDivider()

                    Text(
                        "① 경로 정보",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text("NaviHelper: $naviStatus")
                    Text(routeStatus)

                    Button(
                        onClick = {
                            requestDestination()
                            requestTbt()
                        },
                        enabled = naviConnected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("경로 / 목적지 새로 조회")
                    }

                    Text(
                        "목적지 원본: $destinationText",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text("감지한 장소 (총 ${routePlaces.size}개)")

                    if (routePlaces.isEmpty()) {
                        Text("기본 내비게이션에서 경로 안내를 시작한 뒤 다시 조회하세요.")
                    } else {
                        routePlaces.take(12).forEach { place ->
                            Text(
                                "• ${if (place.kind == PlaceKind.TOLL) "톨게이트" else "휴게소"} / ${place.name}"
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "② 안내 방식 선택",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { switchMode("DEMO") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (mode == "DEMO") "시연 모드 ✓"
                                else "시연 모드"
                            )
                        }

                        Button(
                            onClick = { switchMode("GPS_TEST") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                if (mode == "GPS_TEST") "GPS 시험 ✓"
                                else "GPS 시험"
                            )
                        }
                    }

                    Text("TTS: $ttsStatus")

                    if (mode == "DEMO") {
                        Text(
                            "경로에 존재하는 톨게이트 최대 2개, 휴게소 최대 1개를 " +
                                    "가상 거리 500m에서 안내합니다. 실제 주행 감지가 아닙니다."
                        )

                        Button(
                            onClick = { beginDemo() },
                            enabled = !demoRunning &&
                                    ttsReady &&
                                    routePlaces.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("가상 주행 자동 안내 시작")
                        }

                        Button(
                            onClick = { stopDemo() },
                            enabled = demoRunning,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("가상 주행 중지")
                        }

                        Text("시연 상태: $demoStatus")

                        if (demoRunning && demoIndex < demoStops.size) {
                            Text("접근 대상: ${demoStops[demoIndex].name}")
                            Text(
                                "가상 잔여거리: ${demoMeters}m",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    } else {
                        Text(
                            "시험 기준점 B: 37.399000, 127.120000 " +
                                    "(실제 톨게이트가 아님)"
                        )

                        Text("Android GPS: $gpsStatus")
                        Text("현재 좌표: ${gpsLat ?: "미수신"}, ${gpsLon ?: "미수신"}")
                        Text("B까지 직선거리: ${gpsDistance?.toInt() ?: "미수신"}m")
                        Text("1km 밖 확인: $gpsTestArmed")
                        Text("1회 안내 완료: $gpsTestAnnounced")
                        Text(gpsTestStatus)

                        Button(
                            onClick = { resetGpsTest() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("GPS 시험 초기화")
                        }

                        Text(
                            "ADB로 좌표 A → B를 주입하면 500m 안에서 " +
                                    "자동 음성 안내합니다."
                        )
                    }

                    HorizontalDivider()

                    Text(
                        "③ 마지막 영어 안내",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(lastMessage)

                    Button(
                        onClick = { engine?.stop() },
                        enabled = ttsReady,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("음성 중지")
                    }

                    Text(
                        "실제 톨게이트 접근 감지와 내비게이션 " +
                                "백그라운드 동작은 아직 미검증입니다.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    Text(
                        "Logcat: $TAG",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        initializeTts()
        initializeNavi()
        ensureGps()
    }

    override fun onStart() {
        super.onStart()

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            startGps()
        }
    }

    override fun onStop() {
        stopGps()
        super.onStop()
    }

    override fun onDestroy() {
        destroyed = true
        stopDemo()
        stopGps()
        mainHandler.removeCallbacksAndMessages(null)

        try {
            navi?.let { helper ->
                if (naviListenerAdded) {
                    helper.removeListener(naviListener)
                }
                helper.release()
            }
        } catch (error: Exception) {
            Log.w(TAG, "NAVI_RELEASE_FAILED", error)
        }

        try {
            engine?.let { speech ->
                if (ttsListenerAdded) {
                    speech.removeEventListener(speechListener)
                }
                speech.release()
            }
        } catch (error: Exception) {
            Log.w(TAG, "TTS_RELEASE_FAILED", error)
        }

        super.onDestroy()
    }
}
