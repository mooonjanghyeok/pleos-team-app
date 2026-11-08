
package com.example.pleosteamapp

import ai.pleos.playground.navi.data.TBTInfo

/**
 * K-DRIVE GUIDE 공통 상황 종류.
 *
 * ROUTE 관련 종류는 NaviHelper로 분류할 수 있고,
 * MANUAL 관련 종류는 별도의 교통규칙 가이드에서 사용한다.
 */
enum class KDriveSituation(val koreanTitle: String) {

    TOLL("톨게이트"),
    SERVICE_AREA("고속도로 휴게소"),
    DROWSY_REST("졸음쉼터"),

    ROUNDABOUT_ENTER("회전교차로 진입"),
    ROUNDABOUT_EXIT("회전교차로 진출"),

    HIGHWAY_ENTER("고속도로 진입"),
    HIGHWAY_EXIT("고속도로 진출"),

    UNPROTECTED_LEFT("비보호 좌회전"),
    RIGHT_TURN_CROSSWALK("횡단보도 우회전"),
    HIPASS("하이패스"),
    BUS_LANE("버스전용차로"),
    FUEL("주유"),
    PARKING("주차")
}

enum class KDriveGuideSource {
    NAVI_TBT,
    SIMULATION
}

/**
 * UI에 전달할 공통 안내 데이터.
 *
 * previewEnglish: 경로 사전 안내
 * approachEnglish: 접근 조건이 확인된 후 사용하는 안내
 *
 * rawDistance는 SDK 원본 문자열로 보관한다.
 * 아직 실제 접근거리로 간주하지 않는다.
 */
data class KDriveGuideCue(
    val id: String,
    val situation: KDriveSituation,
    val locationLabel: String,
    val tbtType: String,
    val rawDistance: String,
    val previewEnglish: String,
    val approachEnglish: String,
    val source: KDriveGuideSource
)

/**
 * NaviHelper TBT 데이터를 안내 상황으로 변환한다.
 *
 * 이 클래스는 위치를 직접 감지하거나
 * TTS를 재생하지 않는다.
 */
class KDriveGuideEngine {

    fun classifyTbt(
        tbtInfoList: List<TBTInfo>
    ): List<KDriveGuideCue> {

        return tbtInfoList.mapIndexedNotNull { index, item ->

            val typeName = item.type.toString()

            val situation = detectSituation(typeName)
                ?: return@mapIndexedNotNull null

            val rawDescription =
                item.description.toString().trim()

            val locationLabel =
                rawDescription.takeIf {
                    it.isNotBlank() &&
                            !it.equals("null", ignoreCase = true)
                } ?: situation.koreanTitle

            val sentences = makeSentences(
                situation,
                locationLabel
            )

            KDriveGuideCue(
                id = "$index:$typeName:$locationLabel",
                situation = situation,
                locationLabel = locationLabel,
                tbtType = typeName,
                rawDistance = item.distance.toString(),
                previewEnglish = sentences.first,
                approachEnglish = sentences.second,
                source = KDriveGuideSource.NAVI_TBT
            )
        }
    }

    /**
     * SDK 안내 유형 구분.
     *
     * 일반 좌회전은 비보호 좌회전으로 판단하지 않는다.
     * 일반 우회전은 횡단보도 우회전으로 판단하지 않는다.
     */
    private fun detectSituation(
        typeName: String
    ): KDriveSituation? {

        return when {

            typeName == "TURN_TOLL" ->
                KDriveSituation.TOLL

            typeName == "TURN_ENTER_SERVICEAREA" ->
                KDriveSituation.SERVICE_AREA

            typeName == "TURN_RESTAREA" ->
                KDriveSituation.DROWSY_REST

            typeName.startsWith(
                "TURN_ENTER_ROUNDABOUT_"
            ) ->
                KDriveSituation.ROUNDABOUT_ENTER

            typeName == "TURN_EXIT_ROUNDABOUT" ->
                KDriveSituation.ROUNDABOUT_EXIT

            typeName.startsWith(
                "TURN_ENTER_IC_"
            ) ->
                KDriveSituation.HIGHWAY_ENTER

            typeName.startsWith(
                "TURN_EXIT_IC_"
            ) ->
                KDriveSituation.HIGHWAY_EXIT

            else -> null
        }
    }

    /**
     * 경로 미리보기용 문장과 접근 안내용 문장을
     * 구분해서 생성한다.
     */
    private fun makeSentences(
        situation: KDriveSituation,
        locationLabel: String
    ): Pair<String, String> {

        return when (situation) {

            KDriveSituation.TOLL -> {

                val englishName = englishTollName(
                    locationLabel
                )

                Pair(
                    "Your route includes $englishName.",
                    "You are approaching $englishName. " +
                            "Follow the signs for the " +
                            "appropriate toll payment lane."
                )
            }

            KDriveSituation.SERVICE_AREA -> Pair(
                "Your route includes a highway service area.",
                "You are approaching a highway service area. " +
                        "Follow the signs if you plan to stop."
            )

            KDriveSituation.DROWSY_REST -> Pair(
                "Your route includes a drowsiness rest area.",
                "A drowsiness rest area is coming up. " +
                        "If you feel tired, stop and rest safely."
            )

            KDriveSituation.ROUNDABOUT_ENTER -> Pair(
                "Your route includes a roundabout.",
                "You are approaching a roundabout. " +
                        "Slow down and yield to vehicles " +
                        "already circulating."
            )

            KDriveSituation.ROUNDABOUT_EXIT -> Pair(
                "Your route includes a roundabout exit.",
                "When exiting the roundabout, " +
                        "use your right turn signal " +
                        "and watch for pedestrians."
            )

            KDriveSituation.HIGHWAY_ENTER -> Pair(
                "Your route includes a highway entrance.",
                "You are approaching a highway entrance. " +
                        "Follow the navigation instructions " +
                        "and road signs."
            )

            KDriveSituation.HIGHWAY_EXIT -> Pair(
                "Your route includes a highway exit.",
                "You are approaching a highway exit. " +
                        "Follow the navigation instructions " +
                        "and road signs."
            )

            else -> Pair("", "")
        }
    }

    private fun englishTollName(
        koreanName: String
    ): String {

        return when {

            koreanName.contains("서울") ->
                "Seoul Toll Gate"

            koreanName.contains("청주") ->
                "Cheongju Toll Gate"

            else ->
                "a toll gate"
        }
    }

    /**
     * 회전교차로 등의 가상 접근 시연용 데이터 생성.
     *
     * 실제 TBT 수신 결과가 아니라는 점을
     * source 값으로 구분한다.
     */
    fun makeSimulationCue(
        situation: KDriveSituation
    ): KDriveGuideCue? {

        val supported = when (situation) {
            KDriveSituation.TOLL,
            KDriveSituation.SERVICE_AREA,
            KDriveSituation.DROWSY_REST,
            KDriveSituation.ROUNDABOUT_ENTER,
            KDriveSituation.ROUNDABOUT_EXIT,
            KDriveSituation.HIGHWAY_ENTER,
            KDriveSituation.HIGHWAY_EXIT -> true

            else -> false
        }

        if (!supported) return null

        val sentences = makeSentences(
            situation,
            situation.koreanTitle
        )

        return KDriveGuideCue(
            id = "SIM:${situation.name}",
            situation = situation,
            locationLabel = situation.koreanTitle,
            tbtType = "SIMULATED_EVENT",
            rawDistance = "NOT_AVAILABLE",
            previewEnglish = sentences.first,
            approachEnglish = sentences.second,
            source = KDriveGuideSource.SIMULATION
        )
    }

    /**
     * 실제 자동 감지와 별도로
     * 2번 팀원이 선택형 가이드에서 사용할 주제.
     */
    fun manualGuideTopics(): List<KDriveSituation> {

        return listOf(
            KDriveSituation.UNPROTECTED_LEFT,
            KDriveSituation.RIGHT_TURN_CROSSWALK,
            KDriveSituation.HIPASS,
            KDriveSituation.BUS_LANE,
            KDriveSituation.FUEL,
            KDriveSituation.PARKING
        )
    }
}

/**
 * 같은 안내를 반복하지 않도록 관리한다.
 *
 * 실제 접근 여부는 GPS/경로 연동 측에서 검증해야 한다.
 * 이 클래스는 위치를 자체 판단하지 않는다.
 */
class KDriveSpeechGate {

    private val announcedKeys = mutableSetOf<String>()

    private fun key(
        cue: KDriveGuideCue
    ): String {

        return "${cue.situation.name}|${cue.locationLabel}"
    }

    fun canAnnounce(
        cue: KDriveGuideCue,
        approachConfirmed: Boolean
    ): Boolean {

        if (!approachConfirmed) return false

        return !announcedKeys.contains(key(cue))
    }

    /**
     * TTS 재생 요청이 정상 접수된 후 호출한다.
     */
    fun markAnnounced(
        cue: KDriveGuideCue
    ) {
        announcedKeys.add(key(cue))
    }

    /**
     * 새 경로로 변경했을 때 호출한다.
     */
    fun resetForNewRoute() {
        announcedKeys.clear()
    }
}
