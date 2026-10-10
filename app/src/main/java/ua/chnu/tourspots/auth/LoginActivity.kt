package ua.chnu.tourspots.auth

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import ua.chnu.tourspots.LocaleHelper
import ua.chnu.tourspots.MainActivity
import ua.chnu.tourspots.R
import ua.chnu.tourspots.StopwatchController
import ua.chnu.tourspots.data.TourDbHelper
import ua.chnu.tourspots.data.UserDao
import ua.chnu.tourspots.databinding.ActivityLoginBinding

/**
 * Lab 5: authorization. The time for entering credentials is controlled by a timer:
 * when the login window runs out, the form is cleared and the window starts again.
 * After [SessionManager.MAX_ATTEMPTS] wrong attempts the form is blocked for a while.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var session: SessionManager
    private lateinit var users: UserDao

    /** Deadline of the current login window on the monotonic clock (survives rotation via Bundle). */
    private var windowEndsAt = 0L
    private var wasLocked = false

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            onTimerTick()
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val registerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringExtra(RegisterActivity.EXTRA_LOGIN)?.let { login ->
            binding.etLogin.setText(login)
            binding.etPassword.requestFocus()
            restartLoginWindow()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = SessionManager(this)
        if (session.userId != null) {
            openMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        users = UserDao(TourDbHelper.get(this))

        windowEndsAt = savedInstanceState?.getLong(KEY_WINDOW_ENDS_AT)
            ?: (SystemClock.elapsedRealtime() + SessionManager.LOGIN_WINDOW_MS)

        binding.btnLogin.setOnClickListener { attemptLogin() }
        binding.btnOpenRegister.setOnClickListener {
            registerLauncher.launch(Intent(this, RegisterActivity::class.java))
        }
        binding.etLogin.doAfterTextChanged { binding.tilLogin.error = null }
        binding.etPassword.doAfterTextChanged { binding.tilPassword.error = null }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_login, menu)
        LocaleHelper.bindMenuSpinner(this, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) handler.post(tick)
    }

    override fun onPause() {
        handler.removeCallbacks(tick)
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(KEY_WINDOW_ENDS_AT, windowEndsAt)
    }

    private fun attemptLogin() {
        if (session.lockRemainingMs > 0) return
        val login = binding.etLogin.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString().orEmpty()
        binding.tilLogin.error = if (login.isEmpty()) getString(R.string.error_required) else null
        binding.tilPassword.error = if (password.isEmpty()) getString(R.string.error_required) else null
        if (login.isEmpty() || password.isEmpty()) return

        val user = users.authenticate(login, password)
        if (user != null) {
            session.startSession(user.id)
            Toast.makeText(this, getString(R.string.welcome_user, user.name), Toast.LENGTH_SHORT).show()
            openMain()
            return
        }

        binding.etPassword.text = null
        val attemptsLeft = session.registerFailedAttempt()
        showStatus(
            if (attemptsLeft > 0) getString(R.string.error_wrong_credentials, attemptsLeft)
            else null
        )
        onTimerTick()
    }

    /** Updates the login-window countdown and the lock state once per tick. */
    private fun onTimerTick() {
        val lockMs = session.lockRemainingMs
        val locked = lockMs > 0
        binding.btnLogin.isEnabled = !locked
        binding.etLogin.isEnabled = !locked
        binding.etPassword.isEnabled = !locked
        if (locked) {
            showStatus(getString(R.string.error_locked, StopwatchController.format(lockMs + 999)))
            restartLoginWindow()   // the login window does not run while the form is blocked
        } else if (wasLocked) {
            showStatus(null)
        }
        wasLocked = locked

        val remaining = windowEndsAt - SystemClock.elapsedRealtime()
        if (remaining <= 0) {
            binding.etLogin.text = null
            binding.etPassword.text = null
            Toast.makeText(this, R.string.login_window_expired, Toast.LENGTH_LONG).show()
            restartLoginWindow()
            return
        }
        binding.tvLoginTimer.text = getString(R.string.login_time_left, StopwatchController.format(remaining + 999))
        binding.progressLoginWindow.progress = (remaining * 1000 / SessionManager.LOGIN_WINDOW_MS).toInt()
    }

    private fun restartLoginWindow() {
        windowEndsAt = SystemClock.elapsedRealtime() + SessionManager.LOGIN_WINDOW_MS
    }

    private fun showStatus(message: String?) {
        binding.tvLoginStatus.text = message.orEmpty()
        binding.tvLoginStatus.visibility = if (message == null) View.GONE else View.VISIBLE
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private companion object {
        const val KEY_WINDOW_ENDS_AT = "login.windowEndsAt"
        const val TICK_MS = 250L
    }
}
