
package com.example.pleosteamapp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.DoorValue

import com.example.pleosteamapp.ui.theme.PleosTeamAppTheme

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private var screenState by mutableStateOf(DoorUiState())

    private var vehicle: Vehicle? = null

    // 기존 SDK 테스트 객체
    private var ttsProbe: PleosTtsProbe? = null
    private var naviProbe: NaviProbe? = null
    private var fusedProbe: FusedLocationProbe? = null
    private var androidGpsProbe: AndroidGpsProbe? = null

    private val selectedArea = DoorArea(
        DoorValue.ROW_1_LEFT
    )

    private val mainHandler = Handler(Looper.getMainLooper())

    private var requestId = 0
    private var disposed = false
    private var timeoutTask: Runnable? = null
    private var binaryDoorRange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ------------------------------------
        // 1. Pleos TTS 초기화
        // ------------------------------------
        try {
            ttsProbe = PleosTtsProbe(applicationContext)
            ttsProbe?.start()

            Log.d(
                "KDRIVE_TTS",
                "MainActivity: TTS start requested"
            )

        } catch (error: Exception) {
            Log.e(
                "PLEOS_TTS_TEST",
                "TTS 시작 실패",
                error
            )

            try {
                ttsProbe?.release()
            } catch (releaseError: Exception) {
                Log.w(
                    "PLEOS_TTS_TEST",
                    "TTS 정리 실패",
                    releaseError
                )
            }

            ttsProbe = null
        }

        // ------------------------------------
        // 2. NaviHelper 기존 테스트
        // ------------------------------------
        try {
            naviProbe = NaviProbe(applicationContext)
            naviProbe?.start()

        } catch (error: Exception) {
            Log.e(
                "PLEOS_NAVI_TEST",
                "NaviHelper 시작 실패",
                error
            )
        }

        // ------------------------------------
        // 3. FusedLocation 기존 테스트
        // ------------------------------------
        try {
            fusedProbe = FusedLocationProbe(applicationContext)
            fusedProbe?.start()

        } catch (error: Exception) {
            Log.e(
                "PLEOS_FUSED_TEST",
                "시작 실패",
                error
            )
        }

        // ------------------------------------
        // 4. Android GPS 기존 테스트
        // ------------------------------------
        try {
            androidGpsProbe = AndroidGpsProbe(this)
            androidGpsProbe?.start()

        } catch (error: Exception) {
            Log.e(
                "ANDROID_GPS_TEST",
                "GPS 시작 실패",
                error
            )
        }

        // ------------------------------------
        // 5. 실제 TTS 연결 (중요)
        // ------------------------------------

        // MainActivity의 TTS 객체를 그대로 재사용한다.
        // TTS 생성 실패 시에만 LogSpeaker로 대체한다.

        val guideSpeaker: Speaker =
            ttsProbe?.let {
                TtsSpeaker(it)
            } ?: LogSpeaker()

        Log.d(
            "KDRIVE_TTS",
            "MainActivity: speaker = " +
                    guideSpeaker.javaClass.simpleName
        )

        // ------------------------------------
        // 6. K-DRIVE 화면 연결
        // ------------------------------------
        setContent {

            PleosTeamAppTheme {

                Column(
                    modifier = Modifier.fillMaxSize()
                ) {

                    Box(
                        modifier = Modifier.weight(1f)
                    ) {

                        // 실제 TTS를 안내 엔진에 전달
                        KDriveGuideApp(
                            speaker = guideSpeaker
                        )

                        // 기존 도어 화면 보존
                        /*
                        VehicleDoorScreen(
                            state = screenState,
                            onConnect = {
                                connectAndCheckSupport()
                            },
                            onRead = {
                                readDoorPosition()
                            }
                        )
                        */
                    }

                    // ------------------------------------
                    // 7. 기존 TTS 수동 테스트 버튼
                    // ------------------------------------
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement =
                            Arrangement.spacedBy(12.dp)
                    ) {

                        Button(
                            onClick = {
                                ttsProbe?.speakEnglish()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("영어 음성 재생")
                        }

                        Button(
                            onClick = {
                                ttsProbe?.stopSpeech()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("음성 중지")
                        }
                    }
                }
            }
        }
    }

    // ------------------------------------
    // 기존 Vehicle SDK 테스트 로직
    // ------------------------------------

    private fun connectAndCheckSupport() {

        if (screenState.busy) return

        screenState = DoorUiState()

        val id = beginRequest(
            "SDK 초기화 및 지원 확인 중"
        )

        try {
            releaseVehicle()

            val session = Vehicle(applicationContext)
            vehicle = session

            session.initialize()

            session.getDoor().checkDoorPositionCapabilities(
                onComplete = { config, areaBounds ->

                    finishRequest(id) {

                        val readable =
                            config.accessType == AccessType.READ ||
                                    config.accessType == AccessType.READ_WRITE

                        val bounds = areaBounds[selectedArea]

                        screenState = screenState.copy(
                            capability =
                                "${config.accessType} / " +
                                        "범위 ${bounds?.minValue}.." +
                                        "${bounds?.maxValue}"
                        )

                        when {

                            !readable -> {
                                screenState = screenState.copy(
                                    status = "읽기 지원 확인 필요",
                                    canRead = false,
                                    error =
                                        "이 기능의 접근 수준이 " +
                                                "읽기를 허용하지 않습니다."
                                )
                            }

                            !areaBounds.containsKey(selectedArea) -> {
                                screenState = screenState.copy(
                                    status = "영역 지원 확인 필요",
                                    canRead = false,
                                    error =
                                        "ROW_1_LEFT 영역의 " +
                                                "지원 정보를 받지 못했습니다."
                                )
                            }

                            else -> {
                                binaryDoorRange =
                                    bounds?.minValue == 0 &&
                                            bounds?.maxValue == 1

                                screenState = screenState.copy(
                                    status = "읽기 지원 확인 완료",
                                    canRead = true,
                                    error = ""
                                )
                            }
                        }
                    }
                },

                onFailed = { error ->
                    failRequest(
                        id,
                        "문 상태 지원 확인",
                        error
                    )
                }
            )

        } catch (error: Exception) {
            failRequest(
                id,
                "SDK 초기화 또는 지원 확인",
                error
            )
        }
    }

    private fun readDoorPosition() {

        if (
            screenState.busy ||
            !screenState.canRead
        ) return

        val session = vehicle

        if (session == null) {

            screenState = screenState.copy(
                canRead = false,
                status = "연결 확인 필요",
                error =
                    "SDK 연결 및 지원 확인을 " +
                            "먼저 진행해주세요."
            )

            return
        }

        val id = beginRequest("문 상태 조회 중")

        try {
            session.getDoor().getCurrentDoorPosition(

                area = selectedArea,

                onComplete = { position: Int? ->

                    finishRequest(id) {

                        if (position == null) {

                            screenState = screenState.copy(
                                status = "사용 가능한 현재값 없음",
                                doorLabel = "확인 불가",
                                rawValue = "null",
                                error =
                                    "응답은 왔지만 현재 문 상태 " +
                                            "값이 없습니다."
                            )

                        } else {

                            val label = when {

                                !binaryDoorRange ->
                                    "값 수신 - 원시값 확인"

                                position == 0 -> "닫힘"
                                position == 1 -> "열림"

                                else -> "예상 범위 밖의 값"
                            }

                            screenState = screenState.copy(
                                status = "차량 값 조회 완료",
                                doorLabel = label,
                                rawValue = position.toString(),
                                readAt = SimpleDateFormat(
                                    "HH:mm:ss",
                                    Locale.KOREA
                                ).format(Date()),
                                error = ""
                            )
                        }
                    }
                },

                onFailed = { error ->
                    failRequest(
                        id,
                        "문 상태 조회",
                        error
                    )
                }
            )

        } catch (error: Exception) {
            failRequest(
                id,
                "문 상태 조회 요청",
                error
            )
        }
    }

    private fun beginRequest(message: String): Int {

        val id = ++requestId

        timeoutTask?.let {
            mainHandler.removeCallbacks(it)
        }

        screenState = screenState.copy(
            busy = true,
            status = message,
            doorLabel = "미조회",
            rawValue = "—",
            readAt = "—",
            error = ""
        )

        val timeout = Runnable {

            if (!disposed && requestId == id) {

                requestId++

                screenState = screenState.copy(
                    busy = false,
                    canRead = false,
                    status = "응답 대기 종료",
                    error =
                        "10초 안에 응답을 받지 못했습니다. " +
                                "Pleos Emulator 상태를 확인해주세요."
                )
            }
        }

        timeoutTask = timeout

        mainHandler.postDelayed(
            timeout,
            10_000L
        )

        return id
    }

    private fun finishRequest(
        id: Int,
        update: () -> Unit
    ) {

        mainHandler.post {

            if (!disposed && requestId == id) {

                requestId++

                timeoutTask?.let {
                    mainHandler.removeCallbacks(it)
                }

                timeoutTask = null

                screenState = screenState.copy(
                    busy = false
                )

                update()
            }
        }
    }

    private fun failRequest(
        id: Int,
        step: String,
        error: Exception
    ) {

        Log.e(
            "VehicleDoorTest",
            step,
            error
        )

        finishRequest(id) {

            screenState = screenState.copy(
                canRead = false,
                status = "$step 실패",
                doorLabel = "확인 불가",
                rawValue = "—",
                error =
                    "${error.javaClass.simpleName}: " +
                            (error.message ?: "상세 메시지 없음")
            )
        }
    }

    private fun releaseVehicle() {

        val oldSession = vehicle

        vehicle = null
        binaryDoorRange = false

        try {
            oldSession?.release()

        } catch (error: Exception) {

            Log.w(
                "VehicleDoorTest",
                "SDK 리소스 정리 중 오류",
                error
            )
        }
    }

    // ------------------------------------
    // 앱 종료 시 SDK 정리
    // ------------------------------------
    override fun onDestroy() {

        disposed = true
        requestId++

        mainHandler.removeCallbacksAndMessages(null)

        releaseVehicle()

        try {
            ttsProbe?.release()

        } catch (error: Exception) {
            Log.e(
                "PLEOS_TTS_TEST",
                "TTS 종료 실패",
                error
            )

        } finally {
            ttsProbe = null
        }

        try {
            naviProbe?.release()

        } catch (error: Exception) {
            Log.e(
                "PLEOS_NAVI_TEST",
                "NaviHelper 종료 실패",
                error
            )

        } finally {
            naviProbe = null
        }

        try {
            fusedProbe?.release()

        } catch (error: Exception) {
            Log.e(
                "PLEOS_FUSED_TEST",
                "FusedLocation 종료 실패",
                error
            )

        } finally {
            fusedProbe = null
        }

        try {
            androidGpsProbe?.stop()

        } catch (error: Exception) {
            Log.e(
                "ANDROID_GPS_TEST",
                "GPS 종료 실패",
                error
            )

        } finally {
            androidGpsProbe = null
        }

        super.onDestroy()
    }
}
