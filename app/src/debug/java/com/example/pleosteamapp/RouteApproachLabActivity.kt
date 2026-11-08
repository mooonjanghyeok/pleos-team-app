
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
import ai.pleos.playground.navi.constants.NaviErrorCode

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener

import java.util.Locale

class RouteApproachLabActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KDRIVE_APPROACH"
        private const val POLL_INTERVAL_MS = 5000L
        private const val DEMO_TICK_MS = 600L
        private const val DEMO_START_DISTANCE = 1500
        private const val DEMO_TRIGGER_DISTANCE = 500
    }

    private data class TollPoint(
        val koreanName: String,
        val englishName: String,
        val rawDistance: String
    )

    private val handler = Handler(Looper.getMainLooper())

    private var naviHelper: NaviHelper? = null
    private var tts: TextToSpeech? = null

    private var destroyed = false
    private var naviListenerRegistered = false
    private var ttsListenerRegistered = false

    // SDK 연결 상태
    private var naviConnected by mutableStateOf(false)
    private var naviStatus by mutableStateOf("NaviHelper 초기화 중")
    private var ttsReady by mutableStateOf(false)
    private var ttsStatus by mutableStateOf("TTS 초기화 중")

    // 실시간 정보 변화 감시
    private var monitorEnabled by mutableStateOf(false)
    private var tbtCount by mutableIntStateOf(0)
    private var tbtChangeCount by mutableIntStateOf(0)
    private var tbtStatus by mutableStateOf("아직 TBT를 조회하지 않음")
    private var tbtPreview by mutableStateOf("경로 정보 없음")
    private var lastFingerprint: String? = null

    // 경로에서 발견한 톨게이트
    private var tollPoints by mutableStateOf<List<TollPoint>>(emptyList())

    // 가상 주행 시뮬레이션
    private var demoRunning by mutableStateOf(false)
    private var demoIndex by mutableIntStateOf(0)
    private var demoDistance by mutableIntStateOf(DEMO_START_DISTANCE)
    private var demoAnnounced = false
    private var demoStatus by mutableStateOf("시연 시작 전")
    private var lastSpokenText by mutableStateOf("아직 안내하지 않음")

    // -------------------------------------------------
    // NaviHelper 이벤트
    // -------------------------------------------------

    private val naviListener = object : NaviHelperEventListener {

        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {

            runOnUiThread {
                if (destroyed) return@runOnUiThread

                tbtCount = tbtInfoList.size

                // 원본 데이터 전체를 비교해 변화 여부 확인
                val fingerprint = tbtInfoList.joinToString("|") {
                    "${it.type}:${it.description}:${it.distance}"
                }

                val previous = lastFingerprint

                if (previous != null && previous != fingerprint) {
                    tbtChangeCount++

                    Log.d(
                        TAG,
                        "REAL_TBT_CHANGED count=$tbtChangeCount"
                    )
                }

                lastFingerprint = fingerprint

                tbtStatus =
                    "수신 성공: ${tbtInfoList.size}개, " +
                            "원본 변화 ${tbtChangeCount}회"

                tbtPreview = tbtInfoList.take(5).joinToString("\n") {
                    "${it.type} / ${it.description} / 원본 거리값 ${it.distance}"
                }

                // 톨게이트만 추출
                val detectedTolls = tbtInfoList
                    .filter {
                        it.type.toString() == "TURN_TOLL"
                    }
                    .map { item ->

                        val korean = item.description.toString()

                        TollPoint(
                            koreanName = korean,
                            englishName = englishTollName(korean),
                            rawDistance = item.distance.toString()
                        )
                    }

                // 시연 중에는 대상 목록이 변경되지 않게 유지
                if (!demoRunning) {
                    tollPoints = detectedTolls
                }

                Log.d(
                    TAG,
                    "TBT_RECEIVED total=$tbtCount " +
                            "tolls=${detectedTolls.size} " +
                            "changes=$tbtChangeCount"
                )
            }
        }

        override fun onRoutingCompleted() {
            Log.d(TAG, "ROUTING_COMPLETED")

            runOnUiThread {
                if (!destroyed && naviConnected) {
                    requestTbt()
                }
            }
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.e(TAG, "NAVI_ERROR=$errorCode")

            runOnUiThread {
                if (!destroyed) {
                    naviStatus = "내비게이션 오류: $errorCode"
                }
            }
        }
    }

    // -------------------------------------------------
    // TTS 이벤트
    // -------------------------------------------------

    private val ttsListener = object : EventListener {

        override fun onReady() {
            updateTtsStatus("TTS READY")
        }

        override fun onStart() {
            updateTtsStatus("PLAYBACK START")
        }

        override fun onUpdatedRms(rms: Double) {
            // 출력 레벨 정보는 사용하지 않음
        }

        override fun onDone() {
            updateTtsStatus("PLAYBACK DONE")
        }

        override fun onStop() {
            updateTtsStatus("PLAYBACK STOP")
        }

        override fun onError(errorType: ErrorType) {
            updateTtsStatus("TTS ERROR: $errorType")
        }
    }

    private fun updateTtsStatus(message: String) {
        Log.d(TAG, message)

        runOnUiThread {
            if (!destroyed) {
                ttsStatus = message
            }
        }
    }

    // -------------------------------------------------
    // TBT 주기적 조회
    // -------------------------------------------------

    private val monitorTask = object : Runnable {
        override fun run() {

            if (destroyed || !monitorEnabled) return

            requestTbt()

            handler.postDelayed(
                this,
                POLL_INTERVAL_MS
            )
        }
    }

    private fun startMonitor() {
        if (!naviConnected || monitorEnabled) return

        monitorEnabled = true

        handler.removeCallbacks(monitorTask)
        handler.post(monitorTask)

        Log.d(TAG, "MONITOR STARTED")
    }

    private fun stopMonitor() {
        monitorEnabled = false
        handler.removeCallbacks(monitorTask)

        Log.d(TAG, "MONITOR STOPPED")
    }

    private fun requestTbt() {
        if (!naviConnected || destroyed) return

        try {
            naviHelper?.getTBTInfo()

            Log.d(TAG, "TBT REQUEST")
        } catch (error: Exception) {
            Log.e(TAG, "TBT REQUEST FAILED", error)
            tbtStatus = "조회 실패: ${error.message}"
        }
    }

    // -------------------------------------------------
    // 가상 주행 시뮬레이션
    // -------------------------------------------------

    private val demoTask = object : Runnable {
        override fun run() {

            if (destroyed || !demoRunning) return

            if (demoIndex >= tollPoints.size) {
                finishDemo()
                return
            }

            // 가상 거리를 100m씩 감소시킴
            demoDistance =
                (demoDistance - 100).coerceAtLeast(0)

            val point = tollPoints[demoIndex]

            demoStatus =
                "${point.koreanName} 접근 시연: " +
                        "가상 잔여거리 ${demoDistance}m"

            // 시연상 500m 이내 진입 시 딱 한 번 안내
            if (
                demoDistance <= DEMO_TRIGGER_DISTANCE &&
                !demoAnnounced
            ) {
                demoAnnounced = true

                val message =
                    "You are approaching " +
                            "${point.englishName}. " +
                            "Please follow the signs for " +
                            "the appropriate toll payment lane. " +
                            "Drive safely."

                lastSpokenText = message

                Log.d(
                    TAG,
                    "SIM_APPROACH_TRIGGER " +
                            "point=${point.koreanName} " +
                            "virtualDistance=$demoDistance"
                )

                speakEnglish(message)
            }

            // 하나의 톨게이트를 통과한 상황
            if (demoDistance == 0) {

                Log.d(
                    TAG,
                    "SIM_POINT_PASSED ${point.koreanName}"
                )

                demoIndex++

                if (demoIndex >= tollPoints.size) {
                    finishDemo()
                    return
                }

                demoDistance = DEMO_START_DISTANCE
                demoAnnounced = false
            }

            handler.postDelayed(
                this,
                DEMO_TICK_MS
            )
        }
    }

    private fun startDemo() {

        if (
            destroyed ||
            demoRunning ||
            !ttsReady ||
            tollPoints.isEmpty()
        ) {
            return
        }

        demoIndex = 0
        demoDistance = DEMO_START_DISTANCE
        demoAnnounced = false
        demoRunning = true

        demoStatus = "가상 주행 시작"

        handler.removeCallbacks(demoTask)
        handler.post(demoTask)

        Log.d(TAG, "SIM_DEMO_STARTED")
    }

    private fun stopDemo() {

        demoRunning = false
        handler.removeCallbacks(demoTask)

        demoStatus = "시연 중지"

        try {
            tts?.stop()
        } catch (error: Exception) {
            Log.e(TAG, "TTS STOP FAILED", error)
        }
    }

    private fun finishDemo() {

        demoRunning = false
        handler.removeCallbacks(demoTask)

        demoStatus =
            "시연 완료: ${tollPoints.size}개 톨게이트 통과"

        Log.d(TAG, "SIM_DEMO_FINISHED")
    }

    // -------------------------------------------------
    // 영어 지명 변환
    // -------------------------------------------------

    private fun englishTollName(korean: String): String {
        return when {

            korean.contains("서울") ->
                "Seoul Toll Gate"

            korean.contains("청주") ->
                "Cheongju Toll Gate"

            korean.contains("남산") ->
                "Namsan Toll Gate"

            korean.contains("톨게이트") ->
                "the next toll gate"

            else ->
                "the next toll point"
        }
    }

    private fun speakEnglish(message: String) {

        if (!ttsReady || destroyed) return

        try {
            Log.d(TAG, "SPEAK_REQUEST: $message")

            tts?.speak(message)

        } catch (error: Exception) {
            Log.e(TAG, "SPEAK FAILED", error)
            updateTtsStatus(
                "SPEAK FAILED: ${error.message}"
            )
        }
    }

    // -------------------------------------------------
    // 화면 구성
    // -------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

                    Text(
                        "K-DRIVE GUIDE",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        "주행 접근 감지 실험실",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "실제 TBT 정보 감시와 " +
                                "가상 주행 시연을 구분해서 테스트합니다."
                    )

                    HorizontalDivider()

                    Text(
                        "A. 실제 NaviHelper 경로 정보",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("연결 상태: $naviStatus")

                    Button(
                        onClick = { requestTbt() },
                        enabled = naviConnected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("① 현재 TBT 조회")
                    }

                    Button(
                        onClick = {
                            if (monitorEnabled) {
                                stopMonitor()
                            } else {
                                startMonitor()
                            }
                        },
                        enabled = naviConnected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (monitorEnabled) {
                                "② TBT 자동 감시 중 - 중지"
                            } else {
                                "② TBT 자동 감시 시작 (5초 간격)"
                            }
                        )
                    }

                    Text(tbtStatus)
                    Text(tbtPreview)

                    Text(
                        "주의: TBT 원본 변화는 " +
                                "실제 차량 위치 변화와 같은 의미가 아닙니다."
                    )

                    HorizontalDivider()

                    Text(
                        "B. 경로에서 찾은 톨게이트",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("감지한 톨게이트: ${tollPoints.size}개")

                    if (tollPoints.isEmpty()) {

                        Text(
                            "경로 안내를 시작한 뒤 " +
                                    "① 현재 TBT 조회를 눌러주세요."
                        )

                    } else {

                        tollPoints.forEachIndexed { index, point ->

                            Text(
                                "${index + 1}. " +
                                        "${point.koreanName}\n" +
                                        "   ${point.englishName}\n" +
                                        "   SDK 거리 원본값: " +
                                        point.rawDistance
                            )
                        }
                    }

                    HorizontalDivider()

                    Text(
                        "C. 가상 주행 접근 안내 시연",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        "이 구역은 실제 GPS 이동이 아닌 " +
                                "시뮬레이션입니다. " +
                                "각 톨게이트까지의 가상 거리가 " +
                                "500m 이내가 되면 영어 음성을 재생합니다."
                    )

                    Text(
                        "TTS 상태: $ttsStatus"
                    )

                    Button(
                        onClick = { startDemo() },
                        enabled =
                            ttsReady &&
                                    tollPoints.isNotEmpty() &&
                                    !demoRunning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("③ 가상 주행 자동 시연 시작")
                    }

                    Button(
                        onClick = { stopDemo() },
                        enabled = demoRunning,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("④ 가상 주행 중지")
                    }

                    Text("시연 상태: $demoStatus")

                    if (demoRunning && demoIndex < tollPoints.size) {

                        Text(
                            "현재 접근 대상: " +
                                    tollPoints[demoIndex].koreanName
                        )

                        Text(
                            "가상 잔여거리: ${demoDistance}m",
                            style =
                                MaterialTheme.typography.headlineMedium
                        )
                    }

                    HorizontalDivider()

                    Text(
                        "마지막 영어 안내 문장",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(lastSpokenText)

                    Text(
                        "Logcat 검색 태그: KDRIVE_APPROACH"
                    )
                }
            }
        }

        initializeNavi()
        initializeTts()
    }

    // -------------------------------------------------
    // NaviHelper 초기화
    // -------------------------------------------------

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

                            requestTbt()

                        } catch (error: Exception) {

                            naviStatus =
                                "리스너 등록 실패: ${error.message}"

                            Log.e(TAG, "LISTENER FAILED", error)
                        }
                    }
                },

                onFailed = { error ->

                    runOnUiThread {
                        if (!destroyed) {
                            naviConnected = false

                            naviStatus =
                                "연결 실패: ${error.message}"
                        }
                    }
                }
            )

        } catch (error: Exception) {

            naviStatus =
                "초기화 실패: ${error.message}"

            Log.e(TAG, "NAVI INIT FAILED", error)
        }
    }

    // -------------------------------------------------
    // TTS 초기화
    // -------------------------------------------------

    private fun initializeTts() {

        try {
            val engine = TextToSpeech(
                applicationContext,
                Mode.ON_DEVICE
            )

            tts = engine

            engine.initialize(
                object : OnInitializeListener {

                    override fun onSuccess() {

                        if (destroyed) return

                        try {
                            engine.addEventListener(ttsListener)
                            ttsListenerRegistered = true

                            engine.setLocale(Locale.US)

                            runOnUiThread {
                                if (!destroyed) {
                                    ttsReady = true
                                    ttsStatus =
                                        "INITIALIZE SUCCESS - en-US"
                                }
                            }

                        } catch (error: Exception) {

                            Log.e(
                                TAG,
                                "TTS SETUP FAILED",
                                error
                            )

                            updateTtsStatus("TTS SETUP FAILED")
                        }
                    }

                    override fun onFailure(errorType: ErrorType) {

                        updateTtsStatus(
                            "INITIALIZE FAILED: $errorType"
                        )
                    }
                }
            )

        } catch (error: Exception) {

            Log.e(TAG, "TTS INIT FAILED", error)

            updateTtsStatus(
                "TTS 초기화 실패: ${error.message}"
            )
        }
    }

    // -------------------------------------------------
    // 종료 시 정리
    // -------------------------------------------------

    override fun onDestroy() {

        destroyed = true
        demoRunning = false
        monitorEnabled = false

        handler.removeCallbacksAndMessages(null)

        try {
            val helper = naviHelper

            if (helper != null) {

                if (naviListenerRegistered) {
                    helper.removeListener(naviListener)
                    naviListenerRegistered = false
                }

                helper.release()
            }

        } catch (error: Exception) {
            Log.w(TAG, "NAVI RELEASE FAILED", error)
        }

        try {
            val engine = tts

            if (engine != null) {

                if (ttsListenerRegistered) {
                    engine.removeEventListener(ttsListener)
                    ttsListenerRegistered = false
                }

                engine.release()
            }

        } catch (error: Exception) {
            Log.w(TAG, "TTS RELEASE FAILED", error)
        }

        super.onDestroy()
    }
}
