package com.example.pleosteamapp

import java.util.ArrayDeque

// KDRIVE_VOICE_QUEUE_V2
// Called on the Android main thread.

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
    private val log: (String) -> Unit
) {
    private val pending = ArrayDeque<KDriveQueueItem>()
    private var active: KDriveQueueItem? = null
    private var stopping = false
    private var disposed = false

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

        if (active?.key == key || pending.any { it.key == key }) {
            log("VOICE_QUEUE_DUPLICATE key=$key")
            return false
        }

        val item = KDriveQueueItem(key, text, locale, source)

        if (active != null || stopping) {
            if (pending.size >= 3) {
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
        if (!canSpeak()) {
            log("VOICE_QUEUE_BLOCKED key=${item.key}")
            return false
        }

        active = item

        return try {
            playSdk(item)
            log("VOICE_QUEUE_STARTED key=${item.key}")
            true
        } catch (error: Exception) {
            log("VOICE_QUEUE_ERROR ${error.message}")
            active = null
            false
        }
    }

    fun onFinished(event: String) {
        if (disposed) return

        if (stopping) {
            stopping = false
            active = null
            log("VOICE_QUEUE_STOP_ACK event=$event")
        } else {
            val finished = active ?: return
            active = null
            log("VOICE_QUEUE_FINISHED key=${finished.key} event=$event")
        }

        next()
    }

    private fun next() {
        if (disposed || stopping || active != null) return

        while (pending.isNotEmpty()) {
            val item = pending.removeFirst()

            val age = System.nanoTime() / 1_000_000L -
                item.createdAtMs

            if (age > 15000L) {
                log("VOICE_QUEUE_EXPIRED key=${item.key}")
                continue
            }

            if (begin(item)) return
        }

        log("VOICE_QUEUE_IDLE")
    }

    private fun stopCurrent(reason: String) {
        if (active == null || stopping) return

        stopping = true
        log("VOICE_QUEUE_INTERRUPT reason=$reason")

        try {
            stopSdk()
        } catch (error: Exception) {
            log("VOICE_QUEUE_STOP_ERROR ${error.message}")
        }
    }

    fun cancelSource(source: String, reason: String) {
        if (disposed) return

        pending.removeAll { it.source == source }

        if (active?.source == source) {
            stopCurrent(reason)
        }

        if (active == null && !stopping) next()

        log("VOICE_QUEUE_CANCEL_SOURCE source=$source")
    }

    fun cancelAll(reason: String) {
        if (disposed) return

        pending.clear()
        stopCurrent(reason)
        log("VOICE_QUEUE_CANCEL_ALL reason=$reason")
    }

    fun close() {
        if (disposed) return

        cancelAll("DESTROY")
        disposed = true
        pending.clear()
        log("VOICE_QUEUE_CLOSED")
    }
}