package ua.chnu.tourspots.auth

import android.content.Context
import androidx.core.content.edit

/**
 * Lab 5: keeps the logged-in user and controls the authorization time.
 *
 * All deadlines are stored as wall-clock timestamps in SharedPreferences, so they survive
 * rotation, backgrounding and even a process restart (same idea as the Lab 4 timer).
 */
class SessionManager(context: Context, private val clock: () -> Long = System::currentTimeMillis) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // ---------- session ----------

    fun startSession(userId: Long) = prefs.edit {
        putLong(KEY_USER_ID, userId)
        putLong(KEY_SESSION_ENDS_AT, clock() + SESSION_DURATION_MS)
        remove(KEY_FAILED_ATTEMPTS)
        remove(KEY_LOCKED_UNTIL)
    }

    /** Id of the logged-in user, or null when nobody is logged in or the session has expired. */
    val userId: Long?
        get() = prefs.getLong(KEY_USER_ID, NO_USER).takeIf { it != NO_USER && sessionRemainingMs > 0 }

    val sessionRemainingMs: Long
        get() = (prefs.getLong(KEY_SESSION_ENDS_AT, 0L) - clock()).coerceAtLeast(0L)

    fun logout() = prefs.edit {
        remove(KEY_USER_ID)
        remove(KEY_SESSION_ENDS_AT)
    }

    // ---------- blocking repeated wrong logins (technical task 2.2) ----------

    val lockRemainingMs: Long
        get() = (prefs.getLong(KEY_LOCKED_UNTIL, 0L) - clock()).coerceAtLeast(0L)

    /** Registers a failed attempt; returns the number of attempts left before a lock. */
    fun registerFailedAttempt(): Int {
        val failed = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
        if (failed >= MAX_ATTEMPTS) {
            prefs.edit {
                putInt(KEY_FAILED_ATTEMPTS, 0)
                putLong(KEY_LOCKED_UNTIL, clock() + LOCK_DURATION_MS)
            }
            return 0
        }
        prefs.edit { putInt(KEY_FAILED_ATTEMPTS, failed) }
        return MAX_ATTEMPTS - failed
    }

    companion object {
        const val SESSION_DURATION_MS = 5 * 60 * 1000L
        const val LOGIN_WINDOW_MS = 60 * 1000L
        const val LOCK_DURATION_MS = 30 * 1000L
        const val MAX_ATTEMPTS = 3

        private const val PREFS = "session"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SESSION_ENDS_AT = "session_ends_at"
        private const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        private const val KEY_LOCKED_UNTIL = "locked_until"
        private const val NO_USER = -1L
    }
}
