
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
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.constants.NaviErrorCode

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocationCompareActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KDRIVE_LOCATION"
        private const val POLL_MS = 3000L
    }

    private val handler = Handler(Looper.getMainLooper())

    private var locationManager: LocationManager? = null
    private var naviHelper: NaviHelper? = null

    private var destroyed = false
    private var gpsListening = false
    private var naviListenerRegistered = false

    // Android GPS 정보
    private var gpsStatus by mutableStateOf("GPS 대기 중")
    private var gpsLatitude by mutableStateOf<Double?>(null)
    private var gpsLongitude by mutableStateOf<Double?>(null)
    private var gpsUpdateCount by mutableIntStateOf(0)
    private var gpsChangeCount by mutableIntStateOf(0)
    private var gpsLastTime by mutableStateOf("—")
    private var gpsSource by mutableStateOf("아직 정보 없음")

    // NaviHelper 정보
    private var naviConnected by mutableStateOf(false)
    private var naviStatus by mutableStateOf("초기화 중")
    private var naviLatitude by mutableStateOf<Double?>(null)
    private var naviLongitude by mutableStateOf<Double?>(null)
    private var naviAddress by mutableStateOf("—")
    private var naviUpdateCount by mutableIntStateOf(0)
    private var naviChangeCount by mutableIntStateOf(0)
    private var naviLastTime by mutableStateOf("—")
    private var naviRawText by mutableStateOf("아직 응답 없음")

    // 자동 조회
    private var pollingEnabled by mutableStateOf(false)

    private fun now(): String =
        SimpleDateFormat(
            "HH:mm:ss",
            Locale.KOREA
        ).format(Date())

    private fun formatCoordinate(
        latitude: Double?,
        longitude: Double?
    ): String {

        if (latitude == null || longitude == null) {
            return "아직 수신되지 않음"
        }

        return String.format(
            Locale.US,
            "위도 %.6f\n경도 %.6f",
            latitude,
            longitude
        )
    }

    private fun compareDistance(): String {

        val lat1 = gpsLatitude ?: return "비교 대기"
        val lon1 = gpsLongitude ?: return "비교 대기"

        val lat2 = naviLatitude ?: return "비교 대기"
        val lon2 = naviLongitude ?: return "비교 대기"

        val results = FloatArray(1)

        Location.distanceBetween(
            lat1,
            lon1,
            lat2,
            lon2,
            results
        )

        return String.format(
            Locale.US,
            "두 위치의 직선거리: %.1f m",
            results[0]
        )
    }

    // ---------------------------------------
    // Android GPS 위치 수신
    // ---------------------------------------

    private val gpsListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {

            runOnUiThread {

                if (destroyed) return@runOnUiThread

                updateGps(
                    location = location,
                    fromCallback = true
                )
            }
        }
    }

    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val granted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            if (granted) {
                startGps()
            } else {
                gpsStatus =
                    "정밀 위치 권한 거부됨. 권한을 허용해야 GPS 비교가 가능합니다."
            }
        }

    private fun hasGpsPermission(): Boolean {

        return checkSelfPermission(
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensureGpsPermission() {

        if (hasGpsPermission()) {
            startGps()
        } else {
            gpsStatus = "정밀 위치 권한 요청 중"

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startGps() {

        if (destroyed || gpsListening) return

        if (!hasGpsPermission()) {
            gpsStatus = "정밀 위치 권한이 없습니다"
            return
        }

        try {
            val manager =
                getSystemService(
                    Context.LOCATION_SERVICE
                ) as LocationManager

            locationManager = manager

            if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                gpsStatus = "GPS 제공자가 비활성화되어 있음"
                return
            }

            // Android GPS의 마지막 저장 위치
            val cached = manager.getLastKnownLocation(
                LocationManager.GPS_PROVIDER
            )

            if (cached != null) {
                updateGps(
                    location = cached,
                    fromCallback = false
                )
            }

            // 실제 위치 변경 이벤트 등록
            manager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                gpsListener,
                Looper.getMainLooper()
            )

            gpsListening = true
            gpsStatus = "GPS 위치 변경 감시 중"

            Log.d(TAG, "ANDROID_GPS_LISTENER_STARTED")

        } catch (error: Exception) {

            gpsStatus =
                "GPS 시작 실패: ${error.message}"

            Log.e(TAG, "GPS START FAILED", error)
        }
    }

    private fun updateGps(
        location: Location,
        fromCallback: Boolean
    ) {

        val oldLat = gpsLatitude
        val oldLon = gpsLongitude

        val newLat = location.latitude
        val newLon = location.longitude

        if (
            oldLat != null &&
            oldLon != null &&
            (
                    kotlin.math.abs(oldLat - newLat) > 0.0000001 ||
                            kotlin.math.abs(oldLon - newLon) > 0.0000001
                    )
        ) {
            gpsChangeCount++
        }

        gpsLatitude = newLat
        gpsLongitude = newLon

        gpsSource = if (fromCallback) {
            "GPS 변경 콜백"
        } else {
            "GPS 마지막 저장 위치"
        }

        if (fromCallback) {
            gpsUpdateCount++
        }

        gpsLastTime = now()

        Log.d(
            TAG,
            "ANDROID_GPS lat=$newLat lon=$newLon " +
                    "callback=$fromCallback " +
                    "changes=$gpsChangeCount"
        )
    }

    // ---------------------------------------
    // NaviHelper 현재 위치 수신
    // ---------------------------------------

    private val naviListener = object : NaviHelperEventListener {

        override fun onCurrentLocationInfo(
            currentLocationInfo: CurrentLocationInfo
        ) {

            runOnUiThread {

                if (destroyed) return@runOnUiThread

                val newLat = currentLocationInfo.latitude
                val newLon = currentLocationInfo.longitude

                val oldLat = naviLatitude
                val oldLon = naviLongitude

                if (
                    oldLat != null &&
                    oldLon != null &&
                    (
                            kotlin.math.abs(oldLat - newLat) > 0.0000001 ||
                                    kotlin.math.abs(oldLon - newLon) > 0.0000001
                            )
                ) {
                    naviChangeCount++
                }

                naviLatitude = newLat
                naviLongitude = newLon

                naviUpdateCount++
                naviLastTime = now()

                naviRawText =
                    currentLocationInfo.toString()

                naviAddress =
                    currentLocationInfo.address.toString()

                naviStatus = "현재 위치 응답 수신 완료"

                Log.d(
                    TAG,
                    "NAVI_LOCATION lat=$newLat lon=$newLon " +
                            "changes=$naviChangeCount"
                )
            }
        }

        override fun onError(errorCode: NaviErrorCode) {

            Log.e(TAG, "NAVI_ERROR=$errorCode")

            runOnUiThread {
                if (!destroyed) {
                    naviStatus = "오류: $errorCode"
                }
            }
        }
    }

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

                            requestNaviLocation()
                            startPolling()

                        } catch (error: Exception) {

                            naviStatus =
                                "리스너 등록 실패: ${error.message}"

                            Log.e(
                                TAG,
                                "NAVI LISTENER FAILED",
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

    private fun requestNaviLocation() {

        if (!naviConnected || destroyed) return

        try {
            naviHelper?.getCurrentLocationInfo()

            Log.d(TAG, "NAVI_LOCATION_REQUEST")

        } catch (error: Exception) {

            naviStatus =
                "위치 조회 실패: ${error.message}"

            Log.e(TAG, "NAVI REQUEST FAILED", error)
        }
    }

    // ---------------------------------------
    // NaviHelper 자동 조회
    // ---------------------------------------

    private val pollTask = object : Runnable {

        override fun run() {

            if (destroyed || !pollingEnabled) return

            requestNaviLocation()

            handler.postDelayed(
                this,
                POLL_MS
            )
        }
    }

    private fun startPolling() {

        if (!naviConnected || destroyed) return

        pollingEnabled = true

        handler.removeCallbacks(pollTask)
        handler.postDelayed(pollTask, POLL_MS)
    }

    private fun stopPolling() {

        pollingEnabled = false
        handler.removeCallbacks(pollTask)
    }

    // ---------------------------------------
    // 화면 구성
    // ---------------------------------------

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
                        "GPS / NaviHelper 위치 비교",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        "Android GPS와 NaviHelper가 " +
                                "서로 같은 현재 위치를 보고하는지 확인합니다."
                    )

                    HorizontalDivider()

                    Text(
                        "A. Android GPS 위치",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("상태: $gpsStatus")

                    Text(
                        formatCoordinate(
                            gpsLatitude,
                            gpsLongitude
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text("수신 출처: $gpsSource")
                    Text("변경 콜백 수신: ${gpsUpdateCount}회")
                    Text("좌표 변화: ${gpsChangeCount}회")
                    Text("마지막 표시 시각: $gpsLastTime")

                    Button(
                        onClick = {
                            ensureGpsPermission()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("① GPS 권한 및 수신 상태 확인")
                    }

                    HorizontalDivider()

                    Text(
                        "B. NaviHelper 현재 위치",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text("상태: $naviStatus")

                    Text(
                        formatCoordinate(
                            naviLatitude,
                            naviLongitude
                        ),
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text("주소: $naviAddress")
                    Text("위치 응답 횟수: ${naviUpdateCount}회")
                    Text("좌표 변화: ${naviChangeCount}회")
                    Text("마지막 응답 시각: $naviLastTime")

                    Button(
                        onClick = {
                            requestNaviLocation()
                        },
                        enabled = naviConnected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("② NaviHelper 현재 위치 조회")
                    }

                    Button(
                        onClick = {
                            if (pollingEnabled) {
                                stopPolling()
                            } else {
                                startPolling()
                            }
                        },
                        enabled = naviConnected,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (pollingEnabled) {
                                "③ NaviHelper 자동 조회 중 - 중지"
                            } else {
                                "③ NaviHelper 자동 조회 시작"
                            }
                        )
                    }

                    HorizontalDivider()

                    Text(
                        "C. 두 위치 비교",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        compareDistance(),
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        "Android GPS와 NaviHelper의 좌표가 " +
                                "서로 다르다면 위치 정보 전달 경로를 " +
                                "추가로 확인해야 합니다."
                    )

                    HorizontalDivider()

                    Text(
                        "D. NaviHelper 원본 정보",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(naviRawText)

                    HorizontalDivider()

                    Text(
                        "Logcat 태그: KDRIVE_LOCATION"
                    )
                }
            }
        }

        ensureGpsPermission()
        initializeNavi()
    }

    // ---------------------------------------
    // 종료
    // ---------------------------------------

    override fun onDestroy() {

        destroyed = true
        stopPolling()

        handler.removeCallbacksAndMessages(null)

        try {

            if (gpsListening) {
                locationManager?.removeUpdates(gpsListener)
                gpsListening = false
            }

        } catch (error: Exception) {

            Log.w(TAG, "GPS RELEASE FAILED", error)
        }

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

        super.onDestroy()
    }
}
