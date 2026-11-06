
package com.example.pleosteamapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class AndroidGpsProbe(
    private val activity: ComponentActivity
) {

    private val tag = "ANDROID_GPS_TEST"

    private val locationManager =
        activity.getSystemService(Context.LOCATION_SERVICE)
                as LocationManager

    private var listening = false

    // GPS 위치 변경 수신
    private val locationListener = object : LocationListener {

        override fun onLocationChanged(location: Location) {

            Log.d(
                tag,
                "ANDROID_GPS_UPDATE " +
                        "latitude=${location.latitude}, " +
                        "longitude=${location.longitude}, " +
                        "accuracy=${location.accuracy}, " +
                        "time=${location.time}"
            )
        }

        override fun onProviderEnabled(provider: String) {
            Log.d(tag, "PROVIDER ENABLED = $provider")
        }

        override fun onProviderDisabled(provider: String) {
            Log.w(tag, "PROVIDER DISABLED = $provider")
        }
    }

    // Android 위치 권한 요청
    private val permissionLauncher =
        activity.registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            Log.d(tag, "FINE PERMISSION = $fineGranted")

            if (fineGranted) {
                registerGps()
            } else {
                Log.w(tag, "PRECISE GPS PERMISSION DENIED")
            }
        }

    // GPS 테스트 시작
    fun start() {

        Log.d(tag, "ANDROID GPS START")

        val fineGranted = ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {
            registerGps()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // GPS 위치 업데이트 등록
    @SuppressLint("MissingPermission")
    private fun registerGps() {

        if (listening) return

        try {
            val provider = LocationManager.GPS_PROVIDER

            val enabled =
                locationManager.isProviderEnabled(provider)

            Log.d(tag, "GPS PROVIDER ENABLED = $enabled")

            if (!enabled) {
                Log.w(tag, "GPS PROVIDER DISABLED")
                return
            }

            // 기존에 저장된 GPS 위치 확인
            val last =
                locationManager.getLastKnownLocation(provider)

            if (last != null) {
                Log.d(
                    tag,
                    "ANDROID_GPS_LAST " +
                            "latitude=${last.latitude}, " +
                            "longitude=${last.longitude}, " +
                            "time=${last.time}"
                )
            } else {
                Log.w(tag, "ANDROID_GPS_LAST = null")
            }

            // GPS 위치 변경 이벤트 등록
            locationManager.requestLocationUpdates(
                provider,
                1000L,
                0f,
                locationListener,
                Looper.getMainLooper()
            )

            listening = true

            Log.d(tag, "GPS LISTENER REGISTERED")

        } catch (error: Exception) {
            Log.e(tag, "GPS REGISTER FAILED", error)
        }
    }

    // GPS 테스트 종료
    fun stop() {

        if (!listening) return

        locationManager.removeUpdates(locationListener)
        listening = false

        Log.d(tag, "GPS LISTENER REMOVED")
    }
}
