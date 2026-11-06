package com.example.pleosteamapp   // ⚠️ MainActivity.kt 첫 줄의 package 줄로 바꾸세요

import android.os.Handler
import android.os.Looper

/**
 * 시연 모드: 실제 내비 대신 "톨게이트까지 남은 거리"를 순서대로 흘려보냄
 * 2600m → ... → 100m → 통과
 * 2000m에서 1차 안내, 500m에서 2차 안내가 나와야 정상
 *
 * 화면에는 DEMO 표시가 붙습니다. 발표 때 "시연 모드"라고 밝히세요.
 */
class DemoFeed(
    private val engine: GuideEngine,
    private val stepMs: Long = 3500L,   // 1차 안내 음성이 끝나기 전에 2차가 끊지 않도록 여유 있게
    private val placeName: String = "Namsan Tunnel 1 Toll Gate"
) {
    private val handler = Handler(Looper.getMainLooper())
    private val distances = listOf(2600.0, 2300.0, 2000.0, 1500.0, 1000.0, 700.0, 500.0, 300.0, 100.0)
    private var index = 0
    private var running = false

    var onFinished: (() -> Unit)? = null

    private val step = object : Runnable {
        override fun run() {
            if (!running) return
            if (index < distances.size) {
                engine.onSignal(
                    GuideSignal(
                        facility = Facility.TOLL_GATE,
                        distanceM = distances[index],
                        name = placeName,
                        isDemo = true
                    )
                )
                index++
                handler.postDelayed(this, stepMs)
            } else {
                engine.onSignal(null)   // 톨게이트 통과
                running = false
                onFinished?.invoke()
            }
        }
    }

    fun start() {
        stop()
        engine.reset()
        index = 0
        running = true
        handler.post(step)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(step)
    }
}
