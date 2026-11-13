package com.example.pleosteamapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper

enum class KDriveSpeechKind {
    MANUAL_STUDY,
    AUTOMATIC_GUIDANCE
}

data class KDriveSpeechDecision(
    val allowed: Boolean,
    val reason: String
)

class KDriveSpeechController(
    context: Context,
    private val report: (String) -> Unit
) {
    private val audioManager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val handler = Handler(Looper.getMainLooper())

    private var registered = false
    private var lastSummary = ""

    var navigationUsageCount: Int = 0
        private set

    var observationCount: Int = 0
        private set

    var currentSummary: String = "No observation"
        private set

    private val playbackCallback =
        object : AudioManager.AudioPlaybackCallback() {

            override fun onPlaybackConfigChanged(
                configs: MutableList<AudioPlaybackConfiguration>?
            ) {
                observe(configs.orEmpty(), "CALLBACK")
            }
        }

    private fun observe(
        configs: List<AudioPlaybackConfiguration>,
        source: String
    ) {
        val nav = configs.count {
            it.audioAttributes.usage ==
                AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
        }

        val usageCounts = configs
            .groupingBy { it.audioAttributes.usage }
            .eachCount()
            .toSortedMap()

        val summary = if (usageCounts.isEmpty()) {
            "no active playback reported"
        } else {
            usageCounts.entries.joinToString {
                "usage=${it.key}:count=${it.value}"
            }
        }

        navigationUsageCount = nav
        currentSummary = summary
        observationCount++

        if (summary != lastSummary) {
            lastSummary = summary

            report(
                "AUDIO_CHANGE source=$source " +
                    "navUsage=$nav total=${configs.size} [$summary]"
            )
        }
    }

    fun startObserving() {
        if (registered) return

        audioManager.registerAudioPlaybackCallback(
            playbackCallback,
            handler
        )

        registered = true
        report("MONITOR_STARTED")
        refresh()
    }

    fun refresh() {
        try {
            observe(
                audioManager.activePlaybackConfigurations,
                "QUERY"
            )
        } catch (e: Exception) {
            report("AUDIO_QUERY_ERROR ${e.message}")
        }
    }

    fun evaluate(
        kind: KDriveSpeechKind,
        parkedVerified: Boolean,
        policyApproved: Boolean = false,
        eventVerified: Boolean = false,
        navigationIdleVerified: Boolean = false
    ): KDriveSpeechDecision {

        if (navigationUsageCount > 0) {
            return KDriveSpeechDecision(
                false,
                "NAVIGATION_USAGE_OBSERVED"
            )
        }

        if (kind == KDriveSpeechKind.MANUAL_STUDY) {
            return if (parkedVerified) {
                KDriveSpeechDecision(
                    true,
                    "MANUAL_STUDY_ALLOWED"
                )
            } else {
                KDriveSpeechDecision(
                    false,
                    "PARKED_STATE_NOT_VERIFIED"
                )
            }
        }

        if (!policyApproved) {
            return KDriveSpeechDecision(
                false,
                "AUTOMATIC_POLICY_NOT_APPROVED"
            )
        }

        if (!eventVerified) {
            return KDriveSpeechDecision(
                false,
                "GUIDANCE_EVENT_NOT_VERIFIED"
            )
        }

        if (!navigationIdleVerified) {
            return KDriveSpeechDecision(
                false,
                "NAVIGATION_IDLE_NOT_VERIFIED"
            )
        }

        return KDriveSpeechDecision(
            true,
            "AUTOMATIC_GUIDANCE_ALLOWED"
        )
    }

    // 검증된 내비 음성 시작 이벤트가 확보된 후 연결할 함수.
    // AUDIO_CHANGE만으로 기본 내비의 신원을 확정하지 않는다.
    fun onVerifiedNavigationVoiceStart(
        stopOurSpeech: () -> Unit
    ) {
        report("VERIFIED_NAV_VOICE_STARTED")

        try {
            stopOurSpeech()
            report("OUR_SPEECH_STOP_REQUESTED")
        } catch (e: Exception) {
            report("OUR_SPEECH_STOP_ERROR ${e.message}")
        }
    }

    fun close() {
        if (!registered) return

        audioManager.unregisterAudioPlaybackCallback(
            playbackCallback
        )

        registered = false
        report("MONITOR_CLOSED")
    }
}