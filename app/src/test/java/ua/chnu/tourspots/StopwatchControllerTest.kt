package ua.chnu.tourspots

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StopwatchControllerTest {

    private var now = 0L
    private val stopwatch = StopwatchController { now }

    @Test
    fun measuresTimeWhileRunningOnly() {
        stopwatch.start()
        now += 5_000
        stopwatch.stop()
        now += 10_000
        assertEquals(5_000, stopwatch.elapsedMs)
    }

    @Test
    fun pauseAndResumeKeepPreviousRunningState() {
        stopwatch.start()
        now += 2_000
        stopwatch.pause()          // screen covered
        now += 60_000
        assertFalse(stopwatch.running)
        stopwatch.resume()         // back in focus
        assertTrue(stopwatch.running)
        now += 1_000
        assertEquals(3_000, stopwatch.elapsedMs)
    }

    @Test
    fun resumeDoesNotStartStoppedStopwatch() {
        stopwatch.pause()
        stopwatch.resume()
        assertFalse(stopwatch.running)
    }

    @Test
    fun resetClearsEverything() {
        stopwatch.start()
        now += 4_000
        stopwatch.reset()
        assertFalse(stopwatch.running)
        assertEquals(0, stopwatch.elapsedMs)
    }

    @Test
    fun formatsHoursMinutesSeconds() {
        assertEquals("0:00:00", StopwatchController.format(999))
        assertEquals("0:01:05", StopwatchController.format(65_000))
        assertEquals("2:03:04", StopwatchController.format((2 * 3600 + 3 * 60 + 4) * 1000L))
    }
}
