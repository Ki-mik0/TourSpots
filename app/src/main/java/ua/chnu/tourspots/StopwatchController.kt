package ua.chnu.tourspots

import android.os.Bundle
import android.os.SystemClock
import java.util.Locale

/**
 * Lab 4: stopwatch state, kept separate from the activity.
 *
 * Time is measured against a monotonic clock (elapsedRealtime) instead of counting ticks,
 * so it never drifts and survives activity re-creation (rotation, resize) exactly.
 */
class StopwatchController(private val clock: () -> Long = SystemClock::elapsedRealtime) {

    var running = false
        private set

    /** Whether the stopwatch was running before the screen got (partially) covered. */
    var wasRunning = false
        private set

    private var accumulatedMs = 0L
    private var startedAt = 0L

    val elapsedMs: Long
        get() = accumulatedMs + if (running) clock() - startedAt else 0L

    fun start() {
        if (running) return
        startedAt = clock()
        running = true
    }

    fun stop() {
        if (!running) return
        accumulatedMs += clock() - startedAt
        running = false
    }

    fun reset() {
        running = false
        wasRunning = false
        accumulatedMs = 0L
    }

    /** onPause: the activity is partially or fully covered by another window. */
    fun pause() {
        wasRunning = running
        stop()
    }

    /** onResume: the activity is visible and in focus again. */
    fun resume() {
        if (wasRunning) start()
        wasRunning = false
    }

    fun saveTo(state: Bundle) {
        state.putBoolean(KEY_RUNNING, running)
        state.putBoolean(KEY_WAS_RUNNING, wasRunning)
        state.putLong(KEY_ACCUMULATED, accumulatedMs)
        state.putLong(KEY_STARTED_AT, startedAt)
    }

    fun restoreFrom(state: Bundle) {
        running = state.getBoolean(KEY_RUNNING)
        wasRunning = state.getBoolean(KEY_WAS_RUNNING)
        accumulatedMs = state.getLong(KEY_ACCUMULATED)
        startedAt = state.getLong(KEY_STARTED_AT)
    }

    companion object {
        private const val KEY_RUNNING = "stopwatch.running"
        private const val KEY_WAS_RUNNING = "stopwatch.wasRunning"
        private const val KEY_ACCUMULATED = "stopwatch.accumulated"
        private const val KEY_STARTED_AT = "stopwatch.startedAt"

        /** Formats milliseconds as h:mm:ss. */
        fun format(ms: Long): String {
            val totalSeconds = ms / 1000
            return String.format(
                Locale.ROOT, "%d:%02d:%02d",
                totalSeconds / 3600, totalSeconds % 3600 / 60, totalSeconds % 60,
            )
        }
    }
}
