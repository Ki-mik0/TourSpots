package ua.chnu.tourspots.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import ua.chnu.tourspots.R
import ua.chnu.tourspots.data.TourDbHelper
import ua.chnu.tourspots.data.User
import ua.chnu.tourspots.data.UserDao

/**
 * Base screen that is available only to an authorized user (use-case diagram: "Авторизований
 * користувач"). Checks the session on start and every second while visible; when the session
 * runs out, the user is sent back to the login screen.
 */
abstract class AuthenticatedActivity : AppCompatActivity() {

    protected lateinit var session: SessionManager
    protected lateinit var currentUser: User

    private val handler = Handler(Looper.getMainLooper())
    private val sessionCheck = object : Runnable {
        override fun run() {
            if (ensureSession()) handler.postDelayed(this, CHECK_MS)
        }
    }

    /** Called instead of onCreate once the user is known to be logged in. */
    protected abstract fun onAuthenticatedCreate(savedInstanceState: Bundle?)

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        val user = session.userId?.let { UserDao(TourDbHelper.get(this)).findById(it) }
        if (user == null) {
            openLogin()
            return
        }
        currentUser = user
        onAuthenticatedCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        handler.post(sessionCheck)
    }

    override fun onPause() {
        handler.removeCallbacks(sessionCheck)
        super.onPause()
    }

    private fun ensureSession(): Boolean {
        if (session.sessionRemainingMs > 0) return true
        session.logout()
        Toast.makeText(this, R.string.session_expired, Toast.LENGTH_LONG).show()
        openLogin()
        return false
    }

    protected fun openLogin() {
        handler.removeCallbacks(sessionCheck)
        startActivity(
            Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private companion object {
        const val CHECK_MS = 1000L
    }
}
