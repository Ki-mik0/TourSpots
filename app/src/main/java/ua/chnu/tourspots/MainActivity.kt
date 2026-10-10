package ua.chnu.tourspots

import android.media.MediaPlayer
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import ua.chnu.tourspots.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        binding.btnPlaySound.setOnClickListener { playIntroSound() }
        setupCategorySpinner()
    }

    // ---------- Lab 2: drop-down list of categories ----------

    private fun setupCategorySpinner() {
        // Variant 1: the reaction happens only after pressing the helper button Button1.
        binding.button1.setOnClickListener {
            showCategory(binding.spinnerCategory.selectedItemPosition)
        }

        // Variant 2: the reaction happens immediately when an item is selected (no button).
        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (binding.switchAuto.isChecked) showCategory(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        binding.switchAuto.setOnCheckedChangeListener { _, auto ->
            binding.button1.isEnabled = !auto
            if (auto) showCategory(binding.spinnerCategory.selectedItemPosition)
        }
        binding.button1.isEnabled = !binding.switchAuto.isChecked
    }

    /** Own reaction to the selected item: list of recommended places + category background. */
    private fun showCategory(position: Int) {
        val categoryName = binding.spinnerCategory.selectedItem.toString()
        val places = resources.obtainTypedArray(R.array.category_locations).use { arrays ->
            resources.getStringArray(arrays.getResourceId(position, 0))
        }
        binding.tvResult.text = buildString {
            append(getString(R.string.recommendations_title, categoryName))
            places.forEach { append("\n• ").append(it) }
        }
        binding.root.setBackgroundResource(categoryBackground(position))
    }

    private fun categoryBackground(position: Int): Int =
        resources.obtainTypedArray(R.array.category_backgrounds).use {
            it.getResourceId(position, R.drawable.bg_main)
        }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // The background is not part of the saved view state: restore it if a category was shown.
        if (binding.tvResult.text != getString(R.string.result_hint)) {
            binding.root.setBackgroundResource(categoryBackground(binding.spinnerCategory.selectedItemPosition))
        }
    }

    // ---------- Lab 2: language selection in the top right corner ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val spinner = menu.findItem(R.id.action_language).actionView as Spinner
        spinner.adapter = LocaleHelper.Adapter(this)
        spinner.setSelection(LocaleHelper.currentIndex(), false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position != LocaleHelper.currentIndex()) {
                    LocaleHelper.apply(LocaleHelper.languages[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
        return true
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
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
