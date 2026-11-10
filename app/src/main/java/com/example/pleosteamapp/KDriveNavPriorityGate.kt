package com.example.pleosteamapp

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.AudioPlaybackConfiguration
import android.os.Handler
import android.os.Looper
import android.os.SystemClock

class KDriveNavPriorityGate(
    context: Context,
    private val log: (String) -> Unit,
    private val stopOurTts: () -> Unit
) {
    companion object {
        private const val NAV_USAGE =
            AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE

        private const val QUIET_PERIOD_MS = 1500L
    }

    private val manager =
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val handler = Handler(Looper.getMainLooper())

    private var registered = false
    private var navCount = 0
    private var lastNavEnd = -1L
    private var lastSummary = "UNINITIALIZED"

    private val callback = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(
            configs: MutableList<AudioPlaybackConfiguration>?
        ) {
            update(configs.orEmpty(), "CALLBACK")
        }
    }

    private fun update(
        configs: List<AudioPlaybackConfiguration>,
        source: String
    ) {
        val previous = navCount

        navCount = configs.count {
            it.audioAttributes.usage == NAV_USAGE
        }

        val summary = configs
            .groupingBy { it.audioAttributes.usage }
            .eachCount()
            .toSortedMap()
            .entries
            .joinToString {
                "usage=${it.key}:count=${it.value}"
            }

        if (previous != navCount || lastSummary != summary) {
            lastSummary = summary

            log(
                "AUDIO_STATE source=$source " +
                "navCount=$navCount [$summary]"
            )
        }

        if (previous == 0 && navCount > 0) {
            log("NAV_AUDIO_START_DETECTED")

            try {
                stopOurTts()
                log("OUR_TTS_STOP_REQUESTED")
            } catch (e: Exception) {
                log("OUR_TTS_STOP_ERROR ${e.message}")
            }
        }

        if (previous > 0 && navCount == 0) {
            lastNavEnd = SystemClock.elapsedRealtime()
            log("NAV_AUDIO_END_DETECTED")
        }
    }

    fun start() {
        if (registered) return

        try {
            manager.registerAudioPlaybackCallback(
                callback,
                handler
            )

            registered = true
            log("GATE_STARTED")
            refresh()
        } catch (e: Exception) {
            registered = false
            log("GATE_START_FAILED ${e.message}")
        }
    }

    private fun refresh(): Boolean {
        if (!registered) return false

        return try {
            update(
                manager.activePlaybackConfigurations,
                "QUERY"
            )
            true
        } catch (e: Exception) {
            log("AUDIO_QUERY_FAILED ${e.message}")
            false
        }
    }

    fun maySpeak(): Boolean {
        if (!refresh()) {
            log("SPEAK_BLOCKED STATE_UNKNOWN")
            return false
        }

        if (navCount > 0) {
            log("SPEAK_BLOCKED NAV_ACTIVE")
            return false
        }

        if (lastNavEnd >= 0L) {
            val elapsed =
                SystemClock.elapsedRealtime() - lastNavEnd

            if (elapsed < QUIET_PERIOD_MS) {
                log("SPEAK_BLOCKED QUIET_PERIOD")
                return false
            }
        }

        log("SPEAK_ALLOWED")
        return true
    }

    fun close() {
        if (!registered) return

        try {
            manager.unregisterAudioPlaybackCallback(callback)
        } catch (e: Exception) {
            log("GATE_CLOSE_ERROR ${e.message}")
        }

        registered = false
        log("GATE_CLOSED")
    }
}