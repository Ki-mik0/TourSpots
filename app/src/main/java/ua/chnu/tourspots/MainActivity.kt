package ua.chnu.tourspots

import android.media.MediaPlayer
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ua.chnu.tourspots.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnPlaySound.setOnClickListener { playIntroSound() }
    }

    /** Lab 1: another media type besides images — a sound from res/raw. */
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
