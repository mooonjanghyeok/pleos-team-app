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
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.constant.area.DoorValue
import com.example.pleosteamapp.ui.theme.PleosTeamAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    private var screenState by mutableStateOf(DoorUiState())

    private var vehicle: Vehicle? = null

    // TTS 테스트 객체
    private var ttsProbe: PleosTtsProbe? = null
    private var naviProbe: NaviProbe? = null
    private var fusedProbe: FusedLocationProbe? = null

    private var androidGpsProbe: AndroidGpsProbe? = null
    private val selectedArea = DoorArea(DoorValue.ROW_1_LEFT)

    private val mainHandler = Handler(Looper.getMainLooper())

    private var requestId = 0
    private var disposed = false
    private var timeoutTask: Runnable? = null

    // 해당 차량이 0..1 범위를 보고했을 때만 닫힘/열림으로 표시합니다.
    private var binaryDoorRange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Pleos TTS 초기화 및 영어 음성 출력 테스트
        try {
            ttsProbe = PleosTtsProbe(applicationContext)
            ttsProbe?.start()
        } catch (error: Exception) {
            Log.e("PLEOS_TTS_TEST", "TTS 시작 실패", error)
        }
// NaviHelper 연결 및 데이터 확인

        // ① NaviHelper 연결 및 데이터 확인
        try {
            naviProbe = NaviProbe(applicationContext)
            naviProbe?.start()
        } catch (error: Exception) {
            Log.e("PLEOS_NAVI_TEST", "NaviHelper 시작 실패", error)
        }

        // ② FusedLocation 지원 여부 및 초기화 확인
        try {
            fusedProbe = FusedLocationProbe(applicationContext)
            fusedProbe?.start()
        } catch (error: Exception) {
            Log.e("PLEOS_FUSED_TEST", "시작 실패", error)
        }
// Android GPS 직접 수신 테스트
        try {
            androidGpsProbe = AndroidGpsProbe(this)
            androidGpsProbe?.start()
        } catch (error: Exception) {
            Log.e("ANDROID_GPS_TEST", "GPS 시작 실패", error)
        }
        // ③ 기존 화면 - 여기부터는 수정하지 않음
        setContent {

        PleosTeamAppTheme {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // 기존 차량 도어 화면
                    Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        KDriveGuideApp()
                        /*VehicleDoorScreen(
                            state = screenState,
                            onConnect = { connectAndCheckSupport() },
                            onRead = { readDoorPosition() }
                        )*/
                    }

                    // TTS 테스트 버튼
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

    private fun connectAndCheckSupport() {
        if (screenState.busy) return

        screenState = DoorUiState()
        val id = beginRequest("SDK 초기화 및 지원 확인 중")

        try {
            releaseVehicle()

            val session = Vehicle(applicationContext)
            vehicle = session
            session.initialize()

            // 초기화 호출이 끝났다는 이유만으로 연결 성공으로 표시하지 않습니다.
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
                                        "범위 ${bounds?.minValue}..${bounds?.maxValue}"
                        )

                        when {
                            !readable -> {
                                screenState = screenState.copy(
                                    status = "읽기 지원 확인 필요",
                                    canRead = false,
                                    error =
                                        "이 기능의 접근 수준이 읽기를 허용하지 않습니다. " +
                                                "반환된 지원 정보를 확인해주세요."
                                )
                            }

                            !areaBounds.containsKey(selectedArea) -> {
                                screenState = screenState.copy(
                                    status = "영역 지원 확인 필요",
                                    canRead = false,
                                    error =
                                        "ROW_1_LEFT 영역의 지원 정보를 받지 못했습니다."
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
                    failRequest(id, "문 상태 지원 확인", error)
                }
            )
        } catch (error: Exception) {
            failRequest(id, "SDK 초기화 또는 지원 확인", error)
        }
    }

    private fun readDoorPosition() {
        if (screenState.busy || !screenState.canRead) return

        val session = vehicle
        if (session == null) {
            screenState = screenState.copy(
                canRead = false,
                status = "연결 확인 필요",
                error = "① SDK 연결 · 지원 확인을 먼저 눌러주세요."
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
                                    "응답은 왔지만 현재 문 상태 값이 없습니다. " +
                                            "닫힘으로 판단하지 않습니다."
                            )
                        } else {
                            val label = when {
                                !binaryDoorRange ->
                                    "값 수신 — 원시값과 지원 범위 확인"

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
                    failRequest(id, "문 상태 조회", error)
                }
            )
        } catch (error: Exception) {
            failRequest(id, "문 상태 조회 요청", error)
        }
    }

    private fun beginRequest(message: String): Int {
        val id = ++requestId

        timeoutTask?.let { mainHandler.removeCallbacks(it) }

        screenState = screenState.copy(
            busy = true,
            status = message,
            doorLabel = "미조회",
            rawValue = "—",
            readAt = "—",
            error = ""
        )

        // 10초는 이 실습 화면의 대기 기준이며 SDK의 공식 제한값이 아닙니다.
        val timeout = Runnable {
            if (!disposed && requestId == id) {
                requestId++ // 늦게 도착하는 이전 응답은 화면에 반영하지 않음

                screenState = screenState.copy(
                    busy = false,
                    canRead = false,
                    status = "응답 대기 종료",
                    error =
                        "10초 안에 응답을 받지 못했습니다. " +
                                "Pleos Emulator 실행 상태를 확인한 뒤 " +
                                "① SDK 연결 · 지원 확인을 다시 눌러주세요."
                )
            }
        }

        timeoutTask = timeout
        mainHandler.postDelayed(timeout, 10_000L)
        return id
    }

    private fun finishRequest(id: Int, update: () -> Unit) {
        // SDK 콜백이 어느 스레드에서 오더라도 화면 갱신은 메인에서 처리합니다.
        mainHandler.post {
            if (!disposed && requestId == id) {
                requestId++
                timeoutTask?.let { mainHandler.removeCallbacks(it) }
                timeoutTask = null

                screenState = screenState.copy(busy = false)
                update()
            }
        }
    }

    private fun failRequest(id: Int, step: String, error: Exception) {
        Log.e("VehicleDoorTest", step, error)

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
            Log.w("VehicleDoorTest", "SDK 리소스 정리 중 오류", error)
        }
    }

    override fun onDestroy() {
        disposed = true
        requestId++

        mainHandler.removeCallbacksAndMessages(null)

        // 기존 Vehicle SDK 종료
        releaseVehicle()

        // TTS SDK 종료
        try {
            ttsProbe?.release()
        } catch (error: Exception) {
            Log.e("PLEOS_TTS_TEST", "TTS 종료 실패", error)
        } finally {
            ttsProbe = null
        }
// NaviHelper 종료
        try {
            naviProbe?.release()
        } catch (error: Exception) {
            Log.e("PLEOS_NAVI_TEST", "NaviHelper 종료 실패", error)
        } finally {
            naviProbe = null
            // FusedLocation SDK 종료
            try {
                fusedProbe?.release()
            } catch (error: Exception) {
                Log.e("PLEOS_FUSED_TEST", "종료 실패", error)
            } finally {
                fusedProbe = null
            }
            // Android GPS 수신 종료
            try {
                androidGpsProbe?.stop()
            } catch (error: Exception) {
                Log.e("ANDROID_GPS_TEST", "GPS 종료 실패", error)
            } finally {
                androidGpsProbe = null
            }
        }
        super.onDestroy()
    }
}