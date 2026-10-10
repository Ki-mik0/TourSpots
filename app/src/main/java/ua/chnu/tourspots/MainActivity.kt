package ua.chnu.tourspots

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.core.widget.doAfterTextChanged
import ua.chnu.tourspots.auth.AuthenticatedActivity
import ua.chnu.tourspots.databinding.ActivityMainBinding
import ua.chnu.tourspots.locations.Categories
import ua.chnu.tourspots.locations.LocationListActivity
import ua.chnu.tourspots.locations.ProfileActivity

/** Main screen with Labs 1–4; available only after authorization (Lab 5). */
class MainActivity : AuthenticatedActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mediaPlayer: MediaPlayer? = null

    private val stopwatch = StopwatchController()
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            updateSessionTime()
            updateTime()
            handler.postDelayed(this, TICK_MS)
        }
    }

    override fun onAuthenticatedCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate (restored = ${savedInstanceState != null})")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.header.tvGreeting.text = getString(R.string.hello_user, currentUser.name)

        savedInstanceState?.let(stopwatch::restoreFrom)

        binding.header.btnPlaySound.setOnClickListener { playIntroSound() }
        setupCategorySpinner()
        setupMessaging()
        setupStopwatch()
    }

    // ---------- Lab 2: drop-down list of categories ----------

    private fun setupCategorySpinner() {
        val card = binding.cardCategories
        // Variant 1: the reaction happens only after pressing the helper button Button1.
        card.button1.setOnClickListener {
            showCategory(card.spinnerCategory.selectedItemPosition)
        }

        // Variant 2: the reaction happens immediately when an item is selected (no button).
        card.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (card.switchAuto.isChecked) showCategory(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        card.switchAuto.setOnCheckedChangeListener { _, auto ->
            card.button1.isEnabled = !auto
            if (auto) showCategory(card.spinnerCategory.selectedItemPosition)
        }
        card.button1.isEnabled = !card.switchAuto.isChecked

        card.btnOpenLocations.setOnClickListener {
            val categoryId = Categories.idForPosition(card.spinnerCategory.selectedItemPosition)
            startActivity(LocationListActivity.intent(this, categoryId))
        }
    }

    /** Own reaction to the selected item: list of recommended places + category background. */
    private fun showCategory(position: Int) {
        val places = resources.getStringArray(resourceIdAt(R.array.category_locations, position))
        binding.cardCategories.tvResult.text = buildString {
            append(getString(R.string.recommendations_title, selectedCategoryName()))
            places.forEach { append("\n• ").append(it) }
        }
        binding.root.setBackgroundResource(categoryBackground(position))
    }

    private fun categoryBackground(position: Int): Int =
        resourceIdAt(R.array.category_backgrounds, position)

    /** Reads the resource reference stored at [index] of a typed <array> from arrays.xml. */
    private fun resourceIdAt(arrayRes: Int, index: Int): Int {
        val array = resources.obtainTypedArray(arrayRes)
        try {
            return array.getResourceId(index, 0)
        } finally {
            array.recycle()
        }
    }

    private fun selectedCategoryName(): String =
        binding.cardCategories.spinnerCategory.selectedItem.toString()

    // ---------- Lab 2: language selection in the top right corner ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        LocaleHelper.bindMenuSpinner(this, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_locations -> {
            startActivity(LocationListActivity.intent(this))
            true
        }
        R.id.action_profile -> {
            startActivity(Intent(this, ProfileActivity::class.java))
            true
        }
        R.id.action_logout -> {
            session.logout()
            openLogin()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    // ---------- Lab 5: authorization time ----------

    /**
     * Shows the remaining session time in the toolbar. When it runs out, AuthenticatedActivity
     * logs the user out automatically and opens the login screen.
     */
    private fun updateSessionTime() {
        binding.toolbar.subtitle =
            getString(R.string.session_left, StopwatchController.format(session.sessionRemainingMs + 999))
    }

    // ---------- Lab 3: sending a message ----------

    private fun setupMessaging() {
        val card = binding.cardMessage
        // a) to a new window of this app (explicit intent)
        card.btnSendInApp.setOnClickListener {
            val message = readMessage() ?: return@setOnClickListener
            startActivity(ReceiveMessageActivity.intent(this, message, selectedCategoryName()))
        }

        // b) via other apps whose activities can send messages (implicit ACTION_SEND intent)
        card.btnSendExternal.setOnClickListener {
            val message = readMessage() ?: return@setOnClickListener
            val send = Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject, selectedCategoryName()))
                .putExtra(Intent.EXTRA_TEXT, message)
            startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
        }

        card.etMessage.doAfterTextChanged { card.tilMessage.error = null }
    }

    /** Returns the trimmed message, or shows an error and returns null when it is empty. */
    private fun readMessage(): String? {
        val card = binding.cardMessage
        val message = card.etMessage.text?.toString()?.trim().orEmpty()
        if (message.isEmpty()) {
            card.tilMessage.error = getString(R.string.error_empty_message)
            return null
        }
        return message
    }

    // ---------- Lab 4: timer ----------

    private fun setupStopwatch() {
        val card = binding.cardStopwatch
        card.btnStart.setOnClickListener { stopwatch.start(); updateTime() }
        card.btnStop.setOnClickListener { stopwatch.stop(); updateTime() }
        card.btnReset.setOnClickListener { stopwatch.reset(); updateTime() }
        updateTime()
    }

    private fun updateTime() {
        binding.cardStopwatch.tvTime.text = StopwatchController.format(stopwatch.elapsedMs)
    }

    /** Rotation / window resize: the activity is re-created, the stopwatch state goes to the Bundle. */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        stopwatch.saveTo(outState)
        Log.d(TAG, "onSaveInstanceState: ${StopwatchController.format(stopwatch.elapsedMs)}")
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.d(TAG, "onRestoreInstanceState")
        // The background is not part of the saved view state: restore it if a category was shown.
        if (binding.cardCategories.tvResult.text != getString(R.string.result_hint)) {
            binding.root.setBackgroundResource(
                categoryBackground(binding.cardCategories.spinnerCategory.selectedItemPosition)
            )
        }
    }

    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: activity visible")
    }

    /** Back in the foreground (after a partial or full overlap) — continue if it was running. */
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: activity in focus")
        stopwatch.resume()
        handler.post(tick)
    }

    /**
     * Partially covered (dialog, incoming-call heads-up, multi-window focus loss) or about to be
     * hidden completely. When "pause when hidden" is on, the stopwatch stops and remembers that it
     * was running; otherwise it keeps measuring time and only the UI updates are stopped.
     */
    override fun onPause() {
        Log.d(TAG, "onPause: activity lost focus")
        handler.removeCallbacks(tick)
        if (binding.cardStopwatch.switchPauseHidden.isChecked && !isChangingConfigurations) {
            stopwatch.pause()
        }
        super.onPause()
    }

    override fun onStop() {
        Log.d(TAG, "onStop: activity fully covered")
        super.onStop()
    }

    // ---------- Lab 1: media ----------

    /** Another media type besides images — a sound from res/raw. */
    private fun playIntroSound() {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer.create(this, R.raw.intro_sound).apply {
            setOnCompletionListener { it.release(); mediaPlayer = null }
            start()
        }
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy (changing configurations = $isChangingConfigurations)")
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }

    private companion object {
        const val TAG = "MainActivity"
        const val TICK_MS = 250L
    }
}
