package ua.chnu.tourspots.locations

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.SearchView
import com.google.android.material.chip.Chip
import ua.chnu.tourspots.R
import ua.chnu.tourspots.BottomNav
import ua.chnu.tourspots.auth.AuthenticatedActivity
import ua.chnu.tourspots.data.LocationDao
import ua.chnu.tourspots.data.TourDbHelper
import ua.chnu.tourspots.databinding.ActivityLocationListBinding

/** Technical task: feed of locations with search and filtering by category. */
class LocationListActivity : AuthenticatedActivity() {

    private lateinit var binding: ActivityLocationListBinding
    private lateinit var dao: LocationDao
    private val adapter = LocationAdapter { startActivity(LocationDetailActivity.intent(this, it.id)) }

    /** null = all categories. */
    private var categoryId: Int? = null
    private var query = ""

    override fun onAuthenticatedCreate(savedInstanceState: Bundle?) {
        binding = ActivityLocationListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        BottomNav.setup(this, binding.bottomNav, R.id.nav_feed)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        dao = LocationDao(TourDbHelper.get(this))

        categoryId = savedInstanceState?.getInt(KEY_CATEGORY, 0)?.takeIf { it > 0 }
            ?: intent.getIntExtra(EXTRA_CATEGORY_ID, 0).takeIf { it > 0 }
        query = savedInstanceState?.getString(KEY_QUERY).orEmpty()

        binding.rvLocations.adapter = adapter
        binding.fabAdd.setOnClickListener {
            startActivity(LocationEditActivity.intent(this, categoryId = categoryId))
        }
        buildCategoryChips()
    }

    private fun buildCategoryChips() {
        val names = listOf(getString(R.string.all_categories)) + Categories.names(this)
        names.forEachIndexed { index, name ->
            val chip = Chip(this).apply {
                id = View.generateViewId()
                text = name
                isCheckable = true
                isChecked = (index == 0 && categoryId == null) || index == categoryId
                setOnClickListener {
                    categoryId = index.takeIf { it > 0 }
                    reload()
                }
            }
            binding.chipsCategory.addView(chip)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_location_list, menu)
        val item = menu.findItem(R.id.action_search)
        val searchView = item.actionView as SearchView
        searchView.queryHint = getString(R.string.search_hint)
        if (query.isNotEmpty()) {
            item.expandActionView()
            searchView.setQuery(query, false)
        }
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String?) = false
            override fun onQueryTextChange(text: String?): Boolean {
                query = text.orEmpty()
                reload()
                return true
            }
        })
        return true
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) reload()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_CATEGORY, categoryId ?: 0)
        outState.putString(KEY_QUERY, query)
    }

    private fun reload() {
        val items = dao.list(categoryId = categoryId, query = query)
        adapter.submitList(items)
        binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
    }

    companion object {
        private const val EXTRA_CATEGORY_ID = "ua.chnu.tourspots.extra.CATEGORY_ID"
        private const val KEY_CATEGORY = "list.category"
        private const val KEY_QUERY = "list.query"

        fun intent(context: Context, categoryId: Int? = null): Intent =
            Intent(context, LocationListActivity::class.java).putExtra(EXTRA_CATEGORY_ID, categoryId ?: 0)
    }
}
