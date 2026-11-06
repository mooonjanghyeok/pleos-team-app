package com.example.pleosteamapp   // ⚠️ MainActivity.kt 첫 줄의 package 줄로 바꾸세요

import ai.pleos.playground.navi.data.TBTInfo
import android.os.Handler
import android.os.Looper
import android.util.Log
// TBTInfo에 빨간 줄이 뜨면 TBTInfo 글자 위에 커서를 두고 Alt+Enter → Import 선택
// (패키지는 ai.pleos.playground.navi.data 쪽일 가능성이 큽니다)

/*
 * SDK 연결 전용 파일.
 * Pleos SDK가 바뀌거나 이름이 다르면 이 파일만 고치면 됩니다.
 */

private const val TAG = "KDRIVE"

/**
 * [1] TBTInfo → GuideSignal 변환
 * 지금까지 확인된 값: type = TURN_TOLL, distance = 2300.0, description = 남산1호터널톨게이트
 */
fun tbtToSignal(tbt: TBTInfo?): GuideSignal? {
    if (tbt == null) return null

    val facility = when (tbt.type.toString()) {
        "TURN_TOLL" -> Facility.TOLL_GATE
        // TBT type 목록을 확인한 뒤 여기에 추가하세요. 예:
        // "TUNNEL" -> Facility.TUNNEL
        else -> Facility.UNKNOWN
    }

    return GuideSignal(
        facility = facility,
        distanceM = tbt.distance.toDouble(),
        name = tbt.description ?: ""
    )
}

/**
 * [2] TBT type에 어떤 값들이 있는지 Logcat에 전부 찍기 (한 번만 쓰고 지워도 됨)
 * Logcat 검색창에 KDRIVE 입력해서 보세요.
 */
fun logAllTbtTypes(tbt: TBTInfo?) {
    if (tbt == null) {
        Log.d(TAG, "TBT가 null입니다. 경로 안내 중에 호출하세요.")
        return
    }
    val type: Any = tbt.type ?: return
    Log.d(TAG, "TBT type 클래스: ${type.javaClass.name}")
    val all = type.javaClass.enumConstants ?: type.javaClass.superclass?.enumConstants
    if (all == null) {
        Log.d(TAG, "enum이 아닙니다. Android Studio에서 type 위에 Ctrl+클릭해서 직접 확인하세요.")
    } else {
        all.forEach { Log.d(TAG, "TBT type 값: $it") }
    }
}

/** [3] 일정 간격으로 TBT를 요청하는 타이머 (기본 3초. 너무 짧게 하면 성능 기준에 걸림) */
class TbtPoller(
    private val intervalMs: Long = 3000L,
    private val requestOnce: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var running = false

    private val tick = object : Runnable {
        override fun run() {
            if (!running) return
            try {
                requestOnce()
            } catch (e: Exception) {
                Log.e(TAG, "TBT 요청 실패", e)
            }
            handler.postDelayed(this, intervalMs)
        }
    }

    fun start() {
        if (running) return
        running = true
        handler.post(tick)
    }

    fun stop() {
        running = false
        handler.removeCallbacks(tick)
    }
}

/** [4] 테스트용 Speaker: 소리 대신 Logcat에 문장을 찍음. TTS 연결 전에 먼저 이걸로 확인 */
class LogSpeaker : Speaker {
    override fun speak(text: String) {
        Log.d(TAG, "🔊 SPEAK: $text")
    }

    override fun stop() {
        Log.d(TAG, "🔇 STOP")
    }
}

/**
 * [5] 실제 TTS Speaker: 영어 재생·중지에 성공했던 코드(PleosTtsProbe.kt)를 여기에 연결
 * 생성자에 기존 TTS 객체를 받도록 바꾸고, 아래 TODO 두 곳에 성공했던 호출을 넣으세요.
 */
class PleosSpeaker(
    // private val tts: PleosTtsProbe
) : Speaker {
    override fun speak(text: String) {
        // TODO: 영어 재생에 성공했던 호출. 예) tts.speak(text)
        Log.d(TAG, "PleosSpeaker.speak 아직 연결 안 됨: $text")
    }

    override fun stop() {
        // TODO: 중지에 성공했던 호출. 예) tts.stop()
    }
}
