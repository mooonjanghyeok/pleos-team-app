
package com.example.pleosteamapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
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

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener

import java.util.Locale
import kotlin.math.abs

class GpsTriggerLabActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KDRIVE_GPS_TRIGGER"

        // 테스트 기준점 B - 실제 톨게이트 아님
        private const val TARGET_LAT = 37.399000
        private const val TARGET_LON = 127.120000

        private const val ARM_DISTANCE_M = 1000f
        private const val TRIGGER_DISTANCE_M = 500f
    }

    private var locationManager: LocationManager? = null
    private var gpsRegistered = false

    private var tts: TextToSpeech? = null
    private var ttsListenerRegistered = false
    private var ttsReady by mutableStateOf(false)

    private var destroyed = false

    // 화면 상태
    private var gpsStatus by mutableStateOf("GPS 연결 대기")
    private var ttsStatus by mutableStateOf("TTS 초기화 대기")
    private var testStatus by mutableStateOf(
        "먼저 위치 A를 입력해 주세요."
    )

    private var currentLat by mutableStateOf<Double?>(null)
    private var currentLon by mutableStateOf<Double?>(null)
    private var distanceMeters by mutableStateOf<Float?>(null)

    private var callbackCount by mutableIntStateOf(0)
    private var coordinateChanges by mutableIntStateOf(0)

    // 테스트 조건
    private var armed by mutableStateOf(false)
    private var announced by mutableStateOf(false)

    private var lastEnglish by mutableStateOf(
        "아직 영어 안내 없음"
    )

    // -----------------------------------
    // Android GPS 수신
    // -----------------------------------

    private val gpsListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {

            if (destroyed) return

            callbackCount++

            val oldLat = currentLat
            val oldLon = currentLon

            val lat = location.latitude
            val lon = location.longitude

            if (
                oldLat != null &&
                oldLon != null &&
                (
                        abs(oldLat - lat) > 0.0000001 ||
                                abs(oldLon - lon) > 0.0000001
                        )
            ) {
                coordinateChanges++
            }

            currentLat = lat
            currentLon = lon

            val result = FloatArray(1)

            Location.distanceBetween(
                lat,
                lon,
                TARGET_LAT,
                TARGET_LON,
                result
            )

            val distance = result[0]
            distanceMeters = distance

            gpsStatus = "GPS 변경 콜백 수신 중"

            // 1. 기준점에서 충분히 떨어진 상태 확인
            if (
                !armed &&
                !announced &&
                distance >= ARM_DISTANCE_M
            ) {
                armed = true

                testStatus =
                    "대기 조건 충족. 이제 위치 B로 이동하세요."

                Log.d(
                    TAG,
                    "TEST_ARMED distance=$distance"
                )
            }

            // 2. 시험 기준점 500m 안으로 진입
            if (
                armed &&
                !announced &&
                distance <= TRIGGER_DISTANCE_M
            ) {

                if (ttsReady) {
                    triggerVoice(distance)
                } else {
                    testStatus =
                        "접근 감지됨. TTS 초기화 대기 중"
                }
            }

            Log.d(
                TAG,
                "GPS_UPDATE lat=$lat lon=$lon " +
                        "distance=$distance " +
                        "armed=$armed announced=$announced"
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
                gpsStatus = "정밀 위치 권한이 거부됨"
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

            if (!manager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
                )
            ) {
                gpsStatus = "GPS 제공자가 비활성화됨"
                return
            }

            // Permission may have been revoked after the UI check.
            if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                gpsStatus = "정밀 위치 권한이 없습니다"
                return
            }

            // Permission may have been revoked after the UI check.
            if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                gpsStatus = "정밀 위치 권한이 없습니다"
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
            gpsStatus = "GPS 위치 수신 대기 중"

            Log.d(TAG, "GPS_LISTENER_STARTED")

        } catch (error: SecurityException) {
            gpsRegistered = false
            gpsStatus = "GPS 위치 권한이 취소되었습니다"
            Log.w(TAG, "GPS_PERMISSION_REVOKED", error)
        } catch (error: SecurityException) {
            gpsRegistered = false
            gpsStatus = "GPS 위치 권한이 취소되었습니다"
            Log.w(TAG, "GPS_PERMISSION_REVOKED", error)
        } catch (error: Exception) {

            gpsStatus =
                "GPS 시작 실패: ${error.message}"

            Log.e(TAG, "GPS_START_FAILED", error)
        }
    }

    // -----------------------------------
    // 영어 음성 출력
    // -----------------------------------

    private val ttsListener = object : EventListener {

        override fun onReady() {
            updateTtsStatus("TTS READY")
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
                                "TTS_SETUP_FAILED",
                                error
                            )

                            updateTtsStatus(
                                "TTS 설정 실패"
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
                "TTS 초기화 실패: ${error.message}"
            )
        }
    }

    private fun triggerVoice(distance: Float) {

        if (destroyed || !ttsReady || announced) return

        val message =
            "You are approaching simulated checkpoint B. " +
                    "This is a GPS triggered demonstration, " +
                    "not a real toll gate alert."

        try {
            tts?.speak(message) ?: return

            announced = true

            lastEnglish = message

            testStatus =
                "GPS 접근 트리거 성공! " +
                        "한 번만 영어 안내를 요청했습니다."

            Log.d(
                TAG,
                "GPS_PROXIMITY_TRIGGER " +
                        "distance=$distance"
            )

            Log.d(
                TAG,
                "SPEAK_REQUEST: $message"
            )

        } catch (error: Exception) {

            Log.e(TAG, "SPEAK_FAILED", error)

            testStatus =
                "음성 요청 실패: ${error.message}"
        }
    }

    // -----------------------------------
    // 테스트 초기화
    // -----------------------------------

    private fun resetTest() {

        armed = false
        announced = false

        testStatus =
            "초기화 완료. 위치 A를 입력해 주세요."

        lastEnglish = "아직 영어 안내 없음"

        try {
            tts?.stop()
        } catch (error: Exception) {
            Log.e(TAG, "STOP_FAILED", error)
        }

        Log.d(TAG, "TEST_RESET")
    }

    // -----------------------------------
    // 화면
    // -----------------------------------

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
                        Arrangement.spacedBy(14.dp)
                ) {

                    Text(
                        "K-DRIVE GUIDE",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        "Android GPS 접근 안내 검증",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "실제 Android GPS 변경 콜백으로 " +
                                "시험 기준점 접근을 판단합니다. " +
                                "이 기준점은 실제 톨게이트가 아닙니다."
                    )

                    HorizontalDivider()

                    Text(
                        "A. 현재 Android GPS",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(gpsStatus)

                    Text(
                        "위도: ${currentLat ?: "미수신"}"
                    )

                    Text(
                        "경도: ${currentLon ?: "미수신"}"
                    )

                    Text("GPS 콜백: ${callbackCount}회")
                    Text("좌표 변화: ${coordinateChanges}회")

                    HorizontalDivider()

                    Text(
                        "B. 시험 기준점 B",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("위도: $TARGET_LAT")
                    Text("경도: $TARGET_LON")

                    Text(
                        "기준점까지 직선거리: " +
                                if (distanceMeters != null) {
                                    String.format(
                                        Locale.US,
                                        "%.1f m",
                                        distanceMeters
                                    )
                                } else {
                                    "계산 대기"
                                },
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "이 거리는 GPS 좌표 사이의 직선거리이며 " +
                                "도로를 따라 남은 주행거리가 아닙니다."
                    )

                    HorizontalDivider()

                    Text(
                        "C. 접근 안내 상태",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("1km 이상 거리 확인: $armed")
                    Text("음성 안내 요청 완료: $announced")
                    Text(testStatus)

                    Text("TTS 상태: $ttsStatus")

                    Button(
                        onClick = { resetTest() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("① 테스트 초기화")
                    }

                    HorizontalDivider()

                    Text(
                        "D. 마지막 영어 안내",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(lastEnglish)

                    Text(
                        "테스트 순서:\n" +
                                "1. 위치 A 입력\n" +
                                "2. 대기 조건 충족 확인\n" +
                                "3. 위치 B 입력\n" +
                                "4. 자동 영어 안내 확인"
                    )

                    Text("Logcat: KDRIVE_GPS_TRIGGER")
                }
            }
        }

        initializeTts()
        ensureGpsPermission()
    }

    override fun onDestroy() {

        destroyed = true

        try {
            if (gpsRegistered) {
                locationManager?.removeUpdates(gpsListener)
                gpsRegistered = false
            }
        } catch (error: Exception) {
            Log.w(TAG, "GPS_RELEASE_FAILED", error)
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
            Log.w(TAG, "TTS_RELEASE_FAILED", error)
        }

        super.onDestroy()
    }
}
