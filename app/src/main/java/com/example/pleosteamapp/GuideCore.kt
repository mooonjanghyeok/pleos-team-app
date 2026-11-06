package com.example.pleosteamapp   // ⚠️ MainActivity.kt 첫 줄의 package 줄로 바꾸세요

/*
 * K-DRIVE GUIDE 안내 엔진 (SDK와 상관없는 순수 로직)
 *
 * 흐름: GuideSignal(앞에 뭐가 몇 m) → GuideEngine(판단 + 중복 방지) → 화면 + 영어 음성
 * SDK(NaviHelper, TTS)와 연결하는 코드는 전부 NaviBridge.kt에만 있습니다.
 */

/** 앱이 다루는 도로 상황 종류. TBT type 목록을 확인한 뒤 여기에 하나씩 추가하세요. */
enum class Facility {
    TOLL_GATE,
    UNKNOWN
}

/** 내비(또는 시연 모드)에서 들어오는 "앞에 무엇이, 몇 m 남았는지" 정보 */
data class GuideSignal(
    val facility: Facility,
    val distanceM: Double,
    val name: String,
    val isDemo: Boolean = false
)

/** 안내 단계: FAR = 2km 전 1차 안내, NEAR = 500m 전 2차 안내 */
enum class Stage { FAR, NEAR }

/** 화면과 음성으로 실제 내보낼 안내 한 건 */
data class Announcement(
    val facility: Facility,
    val stage: Stage,
    val title: String,
    val body: List<String>,   // 화면에 한 줄씩 표시
    val speech: String,       // TTS로 읽을 문장
    val placeName: String,
    val isDemo: Boolean
)

/** TTS 연결용 약속. 실제 구현은 NaviBridge.kt (LogSpeaker / PleosSpeaker) */
interface Speaker {
    fun speak(text: String)
    fun stop()
}

/** 영어 안내 문구 모음. 새 상황을 추가할 때는 여기에 함수를 하나 더 만듭니다. */
object GuideScripts {

    fun build(facility: Facility, stage: Stage, placeName: String, isDemo: Boolean): Announcement? =
        when (facility) {
            Facility.TOLL_GATE -> tollGate(stage, placeName, isDemo)
            Facility.UNKNOWN -> null
        }

    private fun tollGate(stage: Stage, placeName: String, isDemo: Boolean): Announcement =
        when (stage) {
            Stage.FAR -> Announcement(
                facility = Facility.TOLL_GATE,
                stage = stage,
                title = "Toll Gate Ahead · 2 km",
                body = listOf(
                    "Hi-Pass lanes are only for cars with a Hi-Pass device.",
                    "No device? Choose a lane where you pay at the booth.",
                    "Rental car? Check if it has a Hi-Pass card."
                ),
                speech = "Toll gate in about two kilometers. " +
                        "Hi-Pass lanes are only for cars with a Hi-Pass device. " +
                        "If your car does not have one, choose a lane where you pay at the booth.",
                placeName = placeName,
                isDemo = isDemo
            )

            Stage.NEAR -> Announcement(
                facility = Facility.TOLL_GATE,
                stage = stage,
                title = "Toll Gate · 500 m",
                body = listOf(
                    "Slow down. Single Hi-Pass lanes: 30 km/h.",
                    "Wrong lane? Do NOT stop or reverse.",
                    "Drive through. Pay later at a convenience store, app, or rest area."
                ),
                speech = "Toll gate in five hundred meters. Slow down. " +
                        "If you entered a Hi-Pass lane by mistake, do not stop and do not reverse. " +
                        "Drive through safely. You can pay the toll later.",
                placeName = placeName,
                isDemo = isDemo
            )
        }
}

/**
 * 안내 엔진
 * - 2km 이하가 되면 1차, 500m 이하가 되면 2차 안내
 * - 같은 장소·같은 단계는 한 번만 말함 (중복 방지)
 * - 2km를 건너뛰고 바로 500m 이하가 오면 2차만 말하고 1차는 생략
 */
class GuideEngine(
    private val speaker: Speaker,
    private val listener: Listener
) {
    interface Listener {
        /** 신호가 들어올 때마다 호출 (상단 상태줄 갱신용) */
        fun onStatus(signal: GuideSignal?)

        /** 새 안내가 나갈 때만 호출 (안내 카드 갱신용) */
        fun onAnnouncement(announcement: Announcement)
    }

    private val alreadySpoken = mutableSetOf<String>()

    var lastAnnouncement: Announcement? = null
        private set

    fun onSignal(signal: GuideSignal?) {
        listener.onStatus(signal)
        if (signal == null || signal.facility == Facility.UNKNOWN) return
        if (signal.distanceM < 0) return

        val stage = when {
            signal.distanceM <= NEAR_DISTANCE_M -> Stage.NEAR
            signal.distanceM <= FAR_DISTANCE_M -> Stage.FAR
            else -> return
        }

        val key = keyOf(signal, stage)
        if (key in alreadySpoken) return
        alreadySpoken += key
        if (stage == Stage.NEAR) alreadySpoken += keyOf(signal, Stage.FAR)

        val announcement = GuideScripts.build(signal.facility, stage, signal.name, signal.isDemo) ?: return
        lastAnnouncement = announcement
        listener.onAnnouncement(announcement)
        speaker.stop()
        speaker.speak(announcement.speech)
    }

    fun replay() {
        lastAnnouncement?.let {
            speaker.stop()
            speaker.speak(it.speech)
        }
    }

    fun stop() = speaker.stop()

    fun reset() {
        alreadySpoken.clear()
        lastAnnouncement = null
    }

    private fun keyOf(s: GuideSignal, stage: Stage) = "${s.facility}|${s.name}|$stage|${s.isDemo}"

    companion object {
        const val FAR_DISTANCE_M = 2000.0
        const val NEAR_DISTANCE_M = 500.0
    }
}
