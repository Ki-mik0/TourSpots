package ua.chnu.tourspots.locations

import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import ua.chnu.tourspots.R
import ua.chnu.tourspots.auth.AuthenticatedActivity
import ua.chnu.tourspots.data.LocationDao
import ua.chnu.tourspots.data.TourDbHelper
import ua.chnu.tourspots.databinding.ActivityProfileBinding
import java.util.Date

/** Use case "Перегляд профілю": user data and the list of locations they added. */
class ProfileActivity : AuthenticatedActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var dao: LocationDao
    private val adapter = LocationAdapter { startActivity(LocationDetailActivity.intent(this, it.id)) }

    override fun onAuthenticatedCreate(savedInstanceState: Bundle?) {
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        dao = LocationDao(TourDbHelper.get(this))

        binding.tvName.text = currentUser.name
        binding.tvLogin.text = getString(R.string.login_value, currentUser.login)
        binding.tvSince.text = getString(
            R.string.member_since, DateFormat.getMediumDateFormat(this).format(Date(currentUser.createdAt)),
        )
        binding.rvLocations.adapter = adapter
        binding.btnLogout.setOnClickListener {
            session.logout()
            openLogin()
        }
    }

    override fun onResume() {
        super.onResume()
        if (!::binding.isInitialized) return
        val mine = dao.list(authorId = currentUser.id)
        adapter.submitList(mine)
        binding.tvMyLocations.text = getString(R.string.my_locations, mine.size)
        binding.tvEmpty.visibility = if (mine.isEmpty()) View.VISIBLE else View.GONE
    }
}
