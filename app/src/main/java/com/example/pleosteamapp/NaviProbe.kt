
package com.example.pleosteamapp

import android.content.Context
import android.util.Log

import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.data.RouteStateInfo
import ai.pleos.playground.navi.data.TBTInfo
import ai.pleos.playground.navi.constants.NaviErrorCode

class NaviProbe(context: Context) {

    private val tag = "PLEOS_NAVI_TEST"

    // 1. NaviHelper 객체 생성
    private val navi = NaviHelper(
        context.applicationContext
    )

    private var listenerAdded = false
    private var released = false

    // 2. 데이터 수신 리스너
    private val listener = object : NaviHelperEventListener {

        // 현재 위치
        override fun onCurrentLocationInfo(
            currentLocationInfo: CurrentLocationInfo
        ) {
            Log.d(tag, "LOCATION = $currentLocationInfo")
        }

        // 경로 안내 상태
        override fun onRouteStateInfo(
            routeStateInfo: RouteStateInfo
        ) {
            Log.d(tag, "ROUTE = $routeStateInfo")
        }

        // 다음 방향 안내

        override fun onTBTInfo(tbtInfoList: List<TBTInfo>) {
            Log.d(tag, "TBT = $tbtInfoList")
        }


        // 내비게이션 오류
        override fun onError(errorCode: NaviErrorCode) {
            Log.e(tag, "NAVI ERROR = $errorCode")
        }
    }

    // 3. SDK 초기화
    fun start() {
        Log.d(tag, "INITIALIZING")

        try {
            navi.initialize(
                onConnected = {
                    if (!released) {
                        Log.d(tag, "CONNECTED")

                        navi.addListener(listener)
                        listenerAdded = true

                        requestData()
                    }
                },
                onFailed = { error ->
                    Log.e(tag, "CONNECT FAILED", error)
                }
            )
        } catch (error: Exception) {
            Log.e(tag, "INITIALIZE EXCEPTION", error)
        }
    }

    // 4. 내비게이션 정보 요청
    fun requestData() {
        if (released || !listenerAdded) return

        try {
            Log.d(tag, "REQUESTING DATA")

            navi.getCurrentLocationInfo()
            navi.getRouteStateInfo()
            navi.getTBTInfo()

        } catch (error: Exception) {
            Log.e(tag, "REQUEST FAILED", error)
        }
    }

    // 5. SDK 종료
    fun release() {
        if (released) return
        released = true

        try {
            if (listenerAdded) {
                navi.removeListener(listener)
                listenerAdded = false
            }
        } finally {
            navi.release()
        }
    }
}
