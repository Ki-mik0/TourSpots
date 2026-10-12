package ua.chnu.tourspots

import android.app.Activity
import android.content.Intent
import com.google.android.material.bottomnavigation.BottomNavigationView
import ua.chnu.tourspots.locations.LocationListActivity
import ua.chnu.tourspots.locations.ProfileActivity

/**
 * Bottom navigation shared by the three top-level screens (Home, Feed, Profile).
 * Switching a tab opens its screen without animation and closes the current one.
 */
object BottomNav {

    fun setup(activity: Activity, nav: BottomNavigationView, selectedId: Int) {
        nav.selectedItemId = selectedId
        nav.setOnItemSelectedListener { item ->
            if (item.itemId != selectedId) {
                val target = when (item.itemId) {
                    R.id.nav_home -> MainActivity::class.java
                    R.id.nav_feed -> LocationListActivity::class.java
                    else -> ProfileActivity::class.java
                }
                activity.startActivity(Intent(activity, target).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT))
                activity.finish()
                activity.overridePendingTransition(0, 0)
            }
            true
        }
    }
}
