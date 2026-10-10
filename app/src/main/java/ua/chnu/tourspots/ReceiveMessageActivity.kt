package ua.chnu.tourspots

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ua.chnu.tourspots.databinding.ActivityReceiveMessageBinding

/** Lab 3a: a new window of this app that receives the message via an explicit intent. */
class ReceiveMessageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityReceiveMessageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        binding.tvMessage.text = intent.getStringExtra(EXTRA_MESSAGE)
        binding.tvCategory.text = getString(R.string.message_about, intent.getStringExtra(EXTRA_CATEGORY))
    }

    companion object {
        const val EXTRA_MESSAGE = "ua.chnu.tourspots.extra.MESSAGE"
        const val EXTRA_CATEGORY = "ua.chnu.tourspots.extra.CATEGORY"

        fun intent(context: Context, message: String, category: String): Intent =
            Intent(context, ReceiveMessageActivity::class.java)
                .putExtra(EXTRA_MESSAGE, message)
                .putExtra(EXTRA_CATEGORY, category)
    }
}
