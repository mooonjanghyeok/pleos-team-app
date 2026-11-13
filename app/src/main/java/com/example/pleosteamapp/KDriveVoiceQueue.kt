package com.example.pleosteamapp

import java.util.ArrayDeque

/**
 * KDRIVE_VOICE_QUEUE_V3
 *
 * All methods and scheduler callbacks must run
 * on the Android main thread.
 *
 * SDK completion callbacks have no utterance ID.
 * Arbitrarily late callbacks cannot be fully
 * distinguished at this layer.
 *
 * Failure policy:
 * If stop fails or has no acknowledgement,
 * discard pending audio and reject new speech
 * until the TTS engine is reinitialized.
 */
internal data class KDriveQueueItem(
    val key: String,
    val text: String,
    val locale: String,
    val source: String,
    val createdAtMs: Long = System.nanoTime() / 1_000_000L
)

internal class KDriveVoiceQueue(
    private val playSdk: (KDriveQueueItem) -> Unit,
    private val stopSdk: () -> Unit,
    private val canSpeak: () -> Boolean,
    private val log: (String) -> Unit,
    private val postDelayed: (Runnable, Long) -> Unit,
    private val removeDelayed: (Runnable) -> Unit
) {
    companion object {
        private const val MAX_PENDING = 3
        private const val MAX_PENDING_AGE_MS = 15_000L
        private const val STOP_ACK_TIMEOUT_MS = 2_500L
        private const val STOP_SETTLE_MS = 250L
    }

    private val pending = ArrayDeque<KDriveQueueItem>()

    private var active: KDriveQueueItem? = null
    private var stopping = false
    private var settling = false
    private var disposed = false
    private var recoveryRequired = false

    private var requireStartBeforeFinish = false
    private var observedStart = false

    private var stopTimeoutTask: Runnable? = null
    private var settleTask: Runnable? = null

    val isBusy: Boolean
        get() =
            active != null ||
            stopping ||
            settling ||
            pending.isNotEmpty()

    val needsRecovery: Boolean
        get() = recoveryRequired

    fun hasSource(source: String): Boolean =
        active?.source == source ||
            pending.any { it.source == source }

    fun offer(
        key: String,
        text: String,
        locale: String,
        source: String
    ): Boolean {
        if (disposed || text.isBlank()) return false

        if (recoveryRequired) {
            log("VOICE_QUEUE_RECOVERY_BLOCK key=$key")
            return false
        }

        if (
            pending.any { it.key == key } ||
            (!stopping && active?.key == key)
        ) {
            log("VOICE_QUEUE_DUPLICATE key=$key")
            return false
        }

        val item = KDriveQueueItem(
            key = key,
            text = text,
            locale = locale,
            source = source
        )

        if (active != null || stopping || settling) {
            if (pending.size >= MAX_PENDING) {
                log("VOICE_QUEUE_FULL key=$key")
                return false
            }

            pending.addLast(item)
            log("VOICE_QUEUE_PENDING key=$key")
            return true
        }

        return begin(item)
    }

    private fun begin(item: KDriveQueueItem): Boolean {
        val allowed = try {
            canSpeak()
        } catch (error: Exception) {
            log("VOICE_QUEUE_GATE_ERROR ${error.message}")
            false
        }

        if (!allowed) {
            log("VOICE_QUEUE_BLOCKED key=${item.key}")
            return false
        }

        active = item
        observedStart = false

        return try {
            playSdk(item)
            log("VOICE_QUEUE_STARTED key=${item.key}")
            true
        } catch (error: Exception) {
            log(
                "VOICE_QUEUE_PLAY_ERROR " +
                    "key=${item.key} ${error.message}"
            )
            active = null
            observedStart = false
            false
        }
    }

    /**
     * Called from TTS EventListener.onStart.
     */
    fun onSdkStarted() {
        if (
            disposed ||
            recoveryRequired ||
            stopping ||
            settling
        ) return

        if (active != null) {
            observedStart = true
        }
    }

    /**
     * Called from TTS onDone/onStop/onError.
     *
     * Returns true if the callback was handled.
     */
    fun onFinished(event: String): Boolean {
        if (disposed || recoveryRequired) return false

        if (stopping) {
            if (event == "ERROR") {
                failClosed("SDK_ERROR_DURING_STOP")
                return true
            }

            clearStopTimeout()

            stopping = false
            active = null
            observedStart = false

            log("VOICE_QUEUE_STOP_ACK event=$event")
            beginSettle()
            return true
        }

        if (settling) {
            log(
                "VOICE_QUEUE_LATE_CALLBACK_IGNORED event=$event"
            )
            return false
        }

        val finished = active ?: return false

        if (event == "ERROR") {
            failClosed("PLAYBACK_ERROR")
            return true
        }

        if (requireStartBeforeFinish && !observedStart) {
            log(
                "VOICE_QUEUE_PRESTART_CALLBACK_IGNORED " +
                    "event=$event"
            )
            return false
        }

        active = null
        observedStart = false
        requireStartBeforeFinish = false

        log(
            "VOICE_QUEUE_FINISHED " +
                "key=${finished.key} event=$event"
        )

        next()
        return true
    }

    private fun next() {
        if (
            disposed ||
            recoveryRequired ||
            stopping ||
            settling ||
            active != null
        ) return

        while (pending.isNotEmpty()) {
            val item = pending.removeFirst()

            val age =
                System.nanoTime() / 1_000_000L -
                    item.createdAtMs

            if (age > MAX_PENDING_AGE_MS) {
                log("VOICE_QUEUE_EXPIRED key=${item.key}")
                continue
            }

            if (begin(item)) return
        }

        log("VOICE_QUEUE_IDLE")
    }

    private fun beginSettle() {
        if (disposed || recoveryRequired) return

        settling = true

        val task = Runnable {
            settleTask = null

            if (
                !disposed &&
                !recoveryRequired &&
                settling
            ) {
                settling = false
                requireStartBeforeFinish = true
                next()
            }
        }

        settleTask = task

        try {
            postDelayed(task, STOP_SETTLE_MS)
        } catch (error: Exception) {
            failClosed(
                "SETTLE_SCHEDULE_ERROR ${error.message}"
            )
        }
    }

    private fun stopCurrent(reason: String) {
        if (
            active == null ||
            stopping ||
            recoveryRequired ||
            disposed
        ) return

        stopping = true
        log("VOICE_QUEUE_INTERRUPT reason=$reason")

        val task = Runnable {
            stopTimeoutTask = null

            if (stopping && !disposed) {
                failClosed("STOP_ACK_TIMEOUT")
            }
        }

        stopTimeoutTask = task

        try {
            postDelayed(task, STOP_ACK_TIMEOUT_MS)
        } catch (error: Exception) {
            failClosed(
                "STOP_TIMER_ERROR ${error.message}"
            )
            return
        }

        try {
            stopSdk()
        } catch (error: Exception) {
            failClosed(
                "STOP_SDK_ERROR ${error.message}"
            )
        }
    }

    private fun clearStopTimeout() {
        stopTimeoutTask?.let {
            removeDelayed(it)
        }

        stopTimeoutTask = null
    }

    private fun clearSettle() {
        settleTask?.let {
            removeDelayed(it)
        }

        settleTask = null
    }

    private fun failClosed(reason: String) {
        clearStopTimeout()
        clearSettle()

        active = null
        pending.clear()

        observedStart = false
        requireStartBeforeFinish = false
        stopping = false
        settling = false
        recoveryRequired = true

        log(
            "VOICE_QUEUE_RECOVERY_REQUIRED reason=$reason"
        )
    }

    fun cancelSource(
        source: String,
        reason: String
    ) {
        if (disposed) return

        pending.removeAll {
            it.source == source
        }

        if (active?.source == source) {
            stopCurrent(reason)
        }

        if (
            active == null &&
            !stopping &&
            !settling
        ) {
            next()
        }

        log(
            "VOICE_QUEUE_CANCEL_SOURCE source=$source"
        )
    }

    fun cancelAll(reason: String) {
        if (disposed) return

        pending.clear()
        stopCurrent(reason)

        log(
            "VOICE_QUEUE_CANCEL_ALL reason=$reason"
        )
    }

    fun close() {
        if (disposed) return

        val wasPlaying =
            active != null || stopping

        disposed = true

        clearStopTimeout()
        clearSettle()

        active = null
        pending.clear()
        stopping = false
        settling = false

        if (wasPlaying) {
            try {
                stopSdk()
            } catch (error: Exception) {
                log(
                    "VOICE_QUEUE_CLOSE_STOP_ERROR " +
                        "${error.message}"
                )
            }
        }

        log("VOICE_QUEUE_CLOSED")
    }
}