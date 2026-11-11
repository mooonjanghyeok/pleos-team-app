
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
import androidx.compose.ui.graphics.Color
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

import com.example.pleosteamapp.ui.theme.PleosTeamAppTheme

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NaviDestinationActivity : ComponentActivity() {

    private val naviTag = "NaviDestinationTest"
    private val voiceTag = "KDRIVE_AUTO_VOICE"

    private lateinit var naviHelper: NaviHelper
    private lateinit var tts: TextToSpeech

    private val handler = Handler(Looper.getMainLooper())

    // 기존 목적지 조회 상태
    private var connected by mutableStateOf(false)
    private var status by mutableStateOf("SDK 초기화 중")
    private var destinationText by mutableStateOf("아직 조회하지 않음")
    private var eventText by mutableStateOf("아직 변경 이벤트 없음")
    private var eventCount by mutableIntStateOf(0)

    private var listenerRegistered = false
    private var destroyed = false
    private var awaitingResponse = false
    private var requestNumber = 0

    // 새로 추가한 TBT 상태
    private var tbtStatus by mutableStateOf("경로 정보 미조회")
    private var tbtText by mutableStateOf("TBT 조회 버튼을 눌러주세요.")
    private var tollText by mutableStateOf("톨게이트 확인 전")

    private var lastTbtItems: List<TBTInfo> = emptyList()

    // 영어 자동 안내 설정
    private var ttsReady by mutableStateOf(false)
    private var ttsStatus by mutableStateOf("TTS 준비 중")
    private var autoVoiceEnabled by mutableStateOf(true)

    private var ttsCreated = false
    private var ttsListenerRegistered = false

    // 같은 톨게이트 정보를 반복해서 읽지 않기 위한 기록
    private var lastAnnouncedTollKey: String? = null

    private fun now(): String =
        SimpleDateFormat(
            "HH:mm:ss",
            Locale.KOREA
        ).format(Date())

    // ----------------------------------------
    // 1. 기존 NaviHelper 이벤트 + TBT 이벤트
    // ----------------------------------------

    private val naviListener = object : NaviHelperEventListener {

        override fun onDestinationChanged() {
            Log.d(naviTag, "onDestinationChanged")

            runOnUiThread {
                if (!destroyed) {
                    eventCount++
                    eventText =
                        "목적지 변경 이벤트 ${eventCount}회 / ${now()}"

                    // 새 목적지에서는 새 안내를 허용
                    lastAnnouncedTollKey = null
                    lastTbtItems = emptyList()
                    tbtStatus = "새 경로 정보 대기 중"
                    tbtText = "경로 탐색 완료 후 조회합니다."
                    tollText = "톨게이트 확인 전"

                    if (connected) {
                        requestDestinationInfo()
                    }
                }
            }
        }

        override fun onDestinationInfo(
            destinationInfo: DestinationInfo
        ) {
            Log.d(
                naviTag,
                "onDestinationInfo: $destinationInfo"
            )

            runOnUiThread {
                if (!destroyed) {
                    awaitingResponse = false
                    status = "목적지 정보 수신 완료 / ${now()}"
                    destinationText = destinationInfo.toString()

                    // 목적지 수신 후 경로 정보 조회 시도
                    requestTbtInfo()
                }
            }
        }

        // 경로 탐색 완료 시 TBT 재조회
        override fun onRoutingCompleted() {
            Log.d(naviTag, "onRoutingCompleted")

            runOnUiThread {
                if (!destroyed && connected) {
                    requestTbtInfo()
                }
            }
        }

        // PC2에서 검증한 List<TBTInfo> 형식
        override fun onTBTInfo(
            tbtInfoList: List<TBTInfo>
        ) {
            Log.d(
                naviTag,
                "TBT RECEIVED = $tbtInfoList"
            )

            runOnUiThread {
                if (destroyed) return@runOnUiThread

                lastTbtItems = tbtInfoList

                tbtStatus =
                    "경로 안내 ${tbtInfoList.size}개 수신 / ${now()}"

                tbtText = if (tbtInfoList.isEmpty()) {
                    "안내 데이터가 없습니다. 경로 안내가 활성화되어 있는지 확인하세요."
                } else {
                    tbtInfoList.take(20).joinToString("\n") {
                        "${it.type} / ${it.description} / 거리값 ${it.distance}"
                    }
                }

                // 경로 전체의 톨게이트 정보 확인
                val tollItems = tbtInfoList.filter {
                    it.type.toString() == "TURN_TOLL"
                }

                tollText = if (tollItems.isEmpty()) {
                    "현재 수신된 TBT에 톨게이트 정보 없음"
                } else {
                    "경로에 톨게이트 ${tollItems.size}개 확인\n" +
                            tollItems.joinToString("\n") {
                                it.description
                            }
                }

                // 톨게이트가 포함됐으면 자동 안내 시도
                maybeSpeakTollAutomatically()
            }
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.e(
                naviTag,
                "onError: $errorCode"
            )

            runOnUiThread {
                if (!destroyed) {
                    awaitingResponse = false
                    status = "내비게이션 오류: $errorCode"
                }
            }
        }
    }

    // ----------------------------------------
    // 2. TTS 이벤트 리스너
    // ----------------------------------------

    private val ttsListener = object : EventListener {

        override fun onReady() {
            updateVoiceStatus("TTS READY")
        }

        override fun onStart() {
            updateVoiceStatus("PLAYBACK START")
        }

        override fun onUpdatedRms(rms: Double) {
            // 오디오 레벨
        }

        override fun onDone() {
            updateVoiceStatus("PLAYBACK DONE")
        }

        override fun onStop() {
            updateVoiceStatus("PLAYBACK STOP")
        }

        override fun onError(errorType: ErrorType) {
            Log.e(voiceTag, "TTS ERROR = $errorType")
            updateVoiceStatus("TTS ERROR = $errorType")
        }
    }

    private fun updateVoiceStatus(message: String) {
        Log.d(voiceTag, message)

        runOnUiThread {
            if (!destroyed) {
                ttsStatus = message
            }
        }
    }

    // ----------------------------------------
    // 3. 화면
    // ----------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            PleosTeamAppTheme {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    Text(
                        "K-DRIVE GUIDE - 목적지 및 음성 안내",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text("NaviHelper: $status")

                    // 기존 목적지 조회 버튼
                    Button(
                        onClick = {
                            requestDestinationInfo()
                        },
                        enabled = connected,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E40AF),
                            contentColor = Color.White
                        )
                    ) {
                        Text("① 현재 목적지 조회")
                    }

                    Text(
                        "목적지 조회 결과",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(destinationText)

                    HorizontalDivider()

                    Text(
                        "목적지 변경 이벤트",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(eventText)

                    HorizontalDivider()

                    // 새로 추가한 TBT 기능
                    Text(
                        "경로 안내(TBT) 분석",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Button(
                        onClick = {
                            requestTbtInfo()
                        },
                        enabled = connected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("② 경로 정보(TBT) 조회")
                    }

                    Text(tbtStatus)
                    Text(tbtText)

                    HorizontalDivider()

                    Text(
                        "톨게이트 감지 결과",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(tollText)

                    HorizontalDivider()

                    Text(
                        "영어 음성 안내",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("TTS 상태: $ttsStatus")

                    Button(
                        onClick = {
                            autoVoiceEnabled = !autoVoiceEnabled

                            if (autoVoiceEnabled) {
                                maybeSpeakTollAutomatically()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (autoVoiceEnabled) {
                                "③ 자동 영어 안내 ON"
                            } else {
                                "③ 자동 영어 안내 OFF"
                            }
                        )
                    }

                    Button(
                        onClick = {
                            speakTollNotice(manual = true)
                        },
                        enabled = ttsReady,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("④ 영어 안내 수동 재생")
                    }

                    Button(
                        onClick = {
                            if (ttsReady) {
                                try {
                                    tts.stop()
                                } catch (error: Exception) {
                                    Log.e(
                                        voiceTag,
                                        "음성 중지 실패",
                                        error
                                    )
                                }
                            }
                        },
                        enabled = ttsReady,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("⑤ 영어 음성 중지")
                    }

                    Text(
                        "현재 버전은 경로에 톨게이트가 포함되어 있음을 안내합니다. " +
                                "실시간 톨게이트 접근거리 감지 기능은 아직 구현되지 않았습니다."
                    )

                    HorizontalDivider()

                    Text(
                        "기본 내비게이션에서 목적지를 설정하고 " +
                                "이 화면으로 돌아와 조회 버튼을 눌러주세요."
                    )

                    // 기존 돌아가기 버튼 유지
                    Button(
                        onClick = {
                            finish()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF475569),
                            contentColor = Color.White
                        )
                    ) {
                        Text("⑥ 기존 Vehicle SDK 화면으로 돌아가기")
                    }
                }
            }
        }

        initializeTts()
        initializeNavi()
    }

    // ----------------------------------------
    // 4. NaviHelper 초기화
    // ----------------------------------------

    private fun initializeNavi() {
        try {
            naviHelper = NaviHelper(applicationContext)

            naviHelper.initialize(
                onConnected = {
                    runOnUiThread {
                        if (!destroyed) {
                            try {
                                naviHelper.addListener(naviListener)
                                listenerRegistered = true
                                connected = true

                                status = "연결 완료 / ${now()}"

                            } catch (error: Exception) {
                                status =
                                    "리스너 등록 실패: ${error.message}"
                            }
                        }
                    }
                },
                onFailed = { error ->
                    runOnUiThread {
                        if (!destroyed) {
                            connected = false
                            status =
                                "SDK 연결 실패: ${error.message}"
                        }
                    }
                }
            )

        } catch (error: Exception) {
            status = "SDK 초기화 실패: ${error.message}"
        }
    }

    // ----------------------------------------
    // 5. 기존 목적지 조회
    // ----------------------------------------

    private fun requestDestinationInfo() {
        if (!connected || destroyed) return

        val id = ++requestNumber

        awaitingResponse = true
        status = "목적지 정보 요청 중 / ${now()}"
        destinationText = "응답 대기 중..."

        try {
            naviHelper.getDestinationInfo()

            handler.postDelayed({
                if (
                    !destroyed &&
                    awaitingResponse &&
                    requestNumber == id
                ) {
                    awaitingResponse = false
                    status = "10초 동안 목적지 응답을 받지 못함"
                    destinationText =
                        "미응답입니다. 목적지가 없다는 뜻으로 " +
                                "단정할 수는 없습니다."
                }
            }, 10_000L)

        } catch (error: Exception) {
            awaitingResponse = false
            status =
                "목적지 조회 요청 실패: ${error.message}"
        }
    }

    // ----------------------------------------
    // 6. TBT 조회
    // ----------------------------------------

    private fun requestTbtInfo() {
        if (!connected || destroyed) return

        try {
            tbtStatus = "TBT 정보 조회 요청 중"

            Log.d(naviTag, "REQUEST TBT")

            naviHelper.getTBTInfo()

        } catch (error: Exception) {
            tbtStatus =
                "TBT 조회 요청 실패: ${error.message}"

            Log.e(naviTag, "GET TBT FAILED", error)
        }
    }

    // ----------------------------------------
    // 7. TTS 초기화
    // ----------------------------------------

    private fun initializeTts() {
        try {
            tts = TextToSpeech(
                applicationContext,
                Mode.ON_DEVICE
            )
            ttsCreated = true

            tts.initialize(object : OnInitializeListener {

                override fun onSuccess() {
                    if (destroyed) return

                    try {
                        tts.addEventListener(ttsListener)
                        ttsListenerRegistered = true

                        tts.setLocale(Locale.US)
                        ttsReady = true

                        updateVoiceStatus(
                            "INITIALIZE SUCCESS - en-US"
                        )

                        runOnUiThread {
                            if (!destroyed) {
                                maybeSpeakTollAutomatically()
                            }
                        }

                    } catch (error: Exception) {
                        Log.e(
                            voiceTag,
                            "TTS 설정 실패",
                            error
                        )
                        updateVoiceStatus("TTS SETUP FAILED")
                    }
                }

                override fun onFailure(errorType: ErrorType) {
                    updateVoiceStatus(
                        "INITIALIZE FAILED = $errorType"
                    )
                }
            })

        } catch (error: Exception) {
            Log.e(voiceTag, "TTS 초기화 실패", error)
            updateVoiceStatus("TTS INITIALIZE FAILED")
        }
    }

    // ----------------------------------------
    // 8. 영어 톨게이트 안내
    // ----------------------------------------

    private fun maybeSpeakTollAutomatically() {
        if (!autoVoiceEnabled || !ttsReady || destroyed) return

        val tollItems = lastTbtItems.filter {
            it.type.toString() == "TURN_TOLL"
        }

        if (tollItems.isEmpty()) return

        // 같은 경로 정보에서 반복 음성 방지
        val key = tollItems.joinToString("|") {
            it.description
        }

        if (key == lastAnnouncedTollKey) return

        speakTollNotice(manual = false)

        lastAnnouncedTollKey = key
    }

    private fun speakTollNotice(manual: Boolean) {
        if (!ttsReady || destroyed) return

        val tollItems = lastTbtItems.filter {
            it.type.toString() == "TURN_TOLL"
        }

        val message = if (tollItems.isNotEmpty()) {
            "Your planned route includes a toll point. " +
                    "Please check the available payment methods " +
                    "and follow the road signs. " +
                    "Drive safely."
        } else {
            "I do not have confirmed toll information " +
                    "for the current route yet."
        }

        try {
            Log.d(
                voiceTag,
                if (manual) {
                    "MANUAL SPEAK REQUEST"
                } else {
                    "AUTO SPEAK REQUEST"
                }
            )

            tts.speak(message)

        } catch (error: Exception) {
            Log.e(voiceTag, "SPEAK FAILED", error)
            updateVoiceStatus("SPEAK FAILED")
        }
    }

    // ----------------------------------------
    // 9. 종료 및 리소스 해제
    // ----------------------------------------

    override fun onDestroy() {
        destroyed = true
        connected = false

        handler.removeCallbacksAndMessages(null)

        if (::naviHelper.isInitialized) {
            try {
                if (listenerRegistered) {
                    naviHelper.removeListener(naviListener)
                    listenerRegistered = false
                }
            } catch (error: Exception) {
                Log.w(
                    naviTag,
                    "NaviHelper 리스너 해제 실패",
                    error
                )
            }

            try {
                naviHelper.release()
            } catch (error: Exception) {
                Log.w(
                    naviTag,
                    "NaviHelper 종료 실패",
                    error
                )
            }
        }

        if (ttsCreated && ::tts.isInitialized) {
            try {
                if (ttsListenerRegistered) {
                    tts.removeEventListener(ttsListener)
                    ttsListenerRegistered = false
                }
            } catch (error: Exception) {
                Log.w(
                    voiceTag,
                    "TTS 리스너 해제 실패",
                    error
                )
            }

            try {
                tts.release()
            } catch (error: Exception) {
                Log.w(
                    voiceTag,
                    "TTS 종료 실패",
                    error
                )
            }
        }

        super.onDestroy()
    }
}
