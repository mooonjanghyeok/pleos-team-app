
package com.example.pleosteamapp

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData

class FusedLocationProbe(context: Context) {

    private val tag = "PLEOS_FUSED_TEST"
    private val appContext = context.applicationContext

    private val permission =
        "pleos.car.permission.FUSED_LOCATION"

    private val fusedLocation = FusedLocation(appContext)

    private var initialized = false
    private var listenerRegistered = false

    // 위치 변경 이벤트 수신
    private val listener = object : FusedLocationListener {

        override fun onChanged(location: LocationData?) {

            if (location == null) {
                Log.w(tag, "LOCATION = null")
                return
            }

            Log.d(tag, "LOCATION UPDATE = $location")
        }

        override fun onError(exception: Exception) {
            Log.e(tag, "LOCATION ERROR", exception)
        }
    }

    fun start() {
        try {
            Log.d(tag, "CHECKING SUPPORT")

            val supported = fusedLocation.isSupported()

            Log.d(tag, "IS_SUPPORTED = $supported")

            if (!supported) {
                return
            }

            fusedLocation.initialize()
            initialized = true

            Log.d(tag, "INITIALIZE SUCCESS")

            // 위치 권한 확인
            val granted =
                appContext.checkSelfPermission(permission) ==
                        PackageManager.PERMISSION_GRANTED

            Log.d(tag, "PERMISSION GRANTED = $granted")

            if (!granted) {
                Log.w(tag, "LOCATION PERMISSION REQUIRED")
                return
            }

            // 실제 위치 업데이트 수신 시작
            fusedLocation.registerFusedLocationCallback(listener)

            listenerRegistered = true

            Log.d(tag, "LISTENER REGISTERED")
            // SDK 1.1.3의 실제 위치 조회 함수 형식 확인
            Log.d(tag, "LAST_KNOWN REQUEST")
            fusedLocation.getLastKnownLocation(listener)
            val methods = fusedLocation.javaClass.methods
                .filter { it.name == "getLastKnownLocation" }

            if (methods.isEmpty()) {
                Log.e(tag, "LAST_KNOWN_METHOD_NOT_FOUND")
            } else {
                methods.forEach { method ->
                    Log.d(
                        tag,
                        "LAST_KNOWN_SIGNATURE = ${method.toGenericString()}"
                    )
                }
            }

        } catch (error: Exception) {
            Log.e(tag, "FUSED LOCATION FAILED", error)
        }
    }

    fun release() {

        if (listenerRegistered) {
            try {
                fusedLocation.unregisterFusedLocationCallback(listener)
            } catch (error: Exception) {
                Log.e(tag, "UNREGISTER FAILED", error)
            }
            listenerRegistered = false
        }

        if (initialized) {
            try {
                fusedLocation.release()
                Log.d(tag, "RELEASED")
            } catch (error: Exception) {
                Log.e(tag, "RELEASE FAILED", error)
            }
            initialized = false
        }
    }
}
