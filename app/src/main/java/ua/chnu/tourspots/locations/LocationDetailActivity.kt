package ua.chnu.tourspots.locations

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ua.chnu.tourspots.R
import ua.chnu.tourspots.auth.AuthenticatedActivity
import ua.chnu.tourspots.data.Location
import ua.chnu.tourspots.data.LocationDao
import ua.chnu.tourspots.data.TourDbHelper
import ua.chnu.tourspots.databinding.ActivityLocationDetailBinding
import ua.chnu.tourspots.databinding.DialogReviewBinding
import ua.chnu.tourspots.databinding.ItemReviewBinding
import java.util.Date
import java.util.Locale

/** Detail page of a location: description, photos, average rating, reviews, map. */
class LocationDetailActivity : AuthenticatedActivity() {

    private lateinit var binding: ActivityLocationDetailBinding
    private lateinit var dao: LocationDao
    private var location: Location? = null

    override fun onAuthenticatedCreate(savedInstanceState: Bundle?) {
        binding = ActivityLocationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        binding.toolbar.setNavigationOnClickListener { finish() }
        dao = LocationDao(TourDbHelper.get(this))

        binding.btnShowOnMap.setOnClickListener {
            location?.let { MapLauncher.show(this, it.latitude, it.longitude, it.title) }
        }
        binding.btnShare.setOnClickListener { location?.let(::share) }
        binding.btnAddReview.setOnClickListener { showReviewDialog() }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) load()
    }

    private fun load() {
        val loaded = dao.find(intent.getLongExtra(EXTRA_LOCATION_ID, 0L))
        if (loaded == null) {
            finish()
            return
        }
        location = loaded
        invalidateOptionsMenu()

        with(binding) {
            toolbar.title = loaded.title
            ivHeader.setImageResource(Categories.background(this@LocationDetailActivity, loaded.categoryId))
            tvTitle.text = loaded.title
            tvMeta.text = getString(
                R.string.location_meta,
                Categories.name(this@LocationDetailActivity, loaded.categoryId),
                loaded.authorName ?: getString(R.string.author_system),
            )
            ratingAverage.rating = loaded.averageRating.toFloat()
            tvAverage.text = if (loaded.reviewCount == 0) getString(R.string.no_rating)
            else getString(
                R.string.average_rating,
                String.format(Locale.getDefault(), "%.1f", loaded.averageRating),
                resources.getQuantityString(R.plurals.review_count, loaded.reviewCount, loaded.reviewCount),
            )
            tvDescription.text = loaded.description
            tvCoordinates.text = getString(R.string.coordinates_value, MapLauncher.formatCoordinates(loaded.latitude, loaded.longitude))
        }
        showPhotos(loaded.id)
        showReviews(loaded.id)
    }

    private fun showPhotos(locationId: Long) {
        val strip = binding.photoStrip
        strip.removeAllViews()
        val photos = dao.photos(locationId)
        binding.photoScroll.visibility = if (photos.isEmpty()) View.GONE else View.VISIBLE
        val size = resources.getDimensionPixelSize(R.dimen.photo_size)
        photos.forEach { photo ->
            strip.addView(ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = size / 12 }
                scaleType = ImageView.ScaleType.CENTER_CROP
                importantForAccessibility = ViewGroup.IMPORTANT_FOR_ACCESSIBILITY_NO
                setImageBitmap(PhotoStorage.loadThumbnail(photo.path, size))
            })
        }
        // The first photo, if any, replaces the category picture in the header.
        photos.firstOrNull()?.let { first ->
            PhotoStorage.loadThumbnail(first.path, resources.displayMetrics.widthPixels)
                ?.let(binding.ivHeader::setImageBitmap)
        }
    }

    private fun showReviews(locationId: Long) {
        val list = binding.reviewList
        list.removeAllViews()
        val reviews = dao.reviews(locationId)
        binding.tvNoReviews.visibility = if (reviews.isEmpty()) View.VISIBLE else View.GONE
        val dateFormat = DateFormat.getMediumDateFormat(this)
        reviews.forEach { review ->
            val item = ItemReviewBinding.inflate(layoutInflater, list, false)
            item.tvAuthor.text = review.userName
            item.rating.rating = review.rating.toFloat()
            item.tvText.text = review.text
            item.tvDate.text = dateFormat.format(Date(review.createdAt))
            list.addView(item.root)
        }
    }

    /** Use case "Додавання відгуку та оцінки". */
    private fun showReviewDialog() {
        val current = location ?: return
        val form = DialogReviewBinding.inflate(layoutInflater)
        dao.reviews(current.id).firstOrNull { it.userId == currentUser.id }?.let { own ->
            form.rating.rating = own.rating.toFloat()
            form.etText.setText(own.text)
        }
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_review)
            .setView(form.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()
        // Validate before closing the dialog.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val text = form.etText.text?.toString()?.trim().orEmpty()
            val rating = form.rating.rating.toInt()
            when {
                text.isEmpty() -> form.tilText.error = getString(R.string.error_required)
                rating !in 1..5 -> Toast.makeText(this, R.string.error_rating, Toast.LENGTH_SHORT).show()
                else -> {
                    val isNew = dao.saveReview(current.id, currentUser.id, text, rating)
                    Toast.makeText(
                        this, if (isNew) R.string.review_added else R.string.review_updated, Toast.LENGTH_SHORT,
                    ).show()
                    dialog.dismiss()
                    load()
                }
            }
        }
    }

    private fun share(location: Location) {
        val text = getString(
            R.string.share_location_text, location.title, location.description,
            MapLauncher.formatCoordinates(location.latitude, location.longitude),
        )
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, getString(R.string.share_chooser_title)))
    }

    // ---------- edit / delete: only for the author ----------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_location_detail, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val isAuthor = location?.authorId == currentUser.id
        menu.findItem(R.id.action_edit).isVisible = isAuthor
        menu.findItem(R.id.action_delete).isVisible = isAuthor
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val current = location ?: return super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_edit -> {
                startActivity(LocationEditActivity.intent(this, locationId = current.id))
                true
            }
            R.id.action_delete -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.delete_location_title)
                    .setMessage(getString(R.string.delete_location_message, current.title))
                    .setPositiveButton(R.string.delete) { _, _ -> delete(current) }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun delete(location: Location) {
        val photoPaths = dao.photos(location.id).map { it.path }
        if (dao.delete(location.id, currentUser.id)) {
            PhotoStorage(this).delete(photoPaths)
            Toast.makeText(this, R.string.location_deleted, Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    companion object {
        private const val EXTRA_LOCATION_ID = "ua.chnu.tourspots.extra.LOCATION_ID"

        fun intent(context: Context, locationId: Long): Intent =
            Intent(context, LocationDetailActivity::class.java).putExtra(EXTRA_LOCATION_ID, locationId)
    }
}
