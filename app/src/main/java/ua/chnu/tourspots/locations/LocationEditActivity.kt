package ua.chnu.tourspots.locations

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.textfield.TextInputLayout
import ua.chnu.tourspots.R
import ua.chnu.tourspots.auth.AuthenticatedActivity
import ua.chnu.tourspots.data.LocationDao
import ua.chnu.tourspots.data.TourDbHelper
import ua.chnu.tourspots.databinding.ActivityLocationEditBinding
import ua.chnu.tourspots.locations.LocationValidator.Field

/**
 * Use case "Додавання туристичної локації" (and "Редагування локації"), following the activity
 * diagram: enter title / description / category → coordinates → optional photos → Save →
 * validation → either save and open the new location, or show errors and return to the form.
 */
class LocationEditActivity : AuthenticatedActivity() {

    private lateinit var binding: ActivityLocationEditBinding
    private lateinit var dao: LocationDao
    private lateinit var storage: PhotoStorage

    /** Id of the edited location, or 0 when a new one is being added. */
    private var locationId = 0L
    private var categoryPosition = 0
    private val pendingPhotos = ArrayList<String>()

    private val pickPhotos = registerForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_PHOTOS)) { uris ->
        uris.mapNotNull(storage::copyToPending).forEach { path ->
            pendingPhotos += path
            addThumbnail(path)
        }
    }

    override fun onAuthenticatedCreate(savedInstanceState: Bundle?) {
        binding = ActivityLocationEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        dao = LocationDao(TourDbHelper.get(this))
        storage = PhotoStorage(this)
        locationId = intent.getLongExtra(EXTRA_LOCATION_ID, 0L)
        setupCategoryDropdown(savedInstanceState?.getInt(KEY_CATEGORY) ?: 0)

        if (locationId != 0L) {
            binding.toolbar.setTitle(R.string.edit_location)
            val location = dao.find(locationId)
            if (location == null || location.authorId != currentUser.id) {
                // Only the author may edit (TT 2.2: checking access rights).
                Toast.makeText(this, R.string.error_not_author, Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            if (savedInstanceState == null) {
                binding.etTitle.setText(location.title)
                binding.etDescription.setText(location.description)
                selectCategory(Categories.positionForId(location.categoryId))
                binding.etLatitude.setText(location.latitude.toString())
                binding.etLongitude.setText(location.longitude.toString())
            }
            dao.photos(locationId).forEach { addThumbnail(it.path) }
        } else if (savedInstanceState == null) {
            val categoryId = intent.getIntExtra(EXTRA_CATEGORY_ID, 0)
            if (categoryId > 0) selectCategory(Categories.positionForId(categoryId))
        }

        savedInstanceState?.getStringArrayList(KEY_PENDING)?.let { saved ->
            pendingPhotos += saved
            saved.forEach(::addThumbnail)
        }

        binding.btnPickPhotos.setOnClickListener {
            pickPhotos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        binding.btnCheckOnMap.setOnClickListener {
            val (valid, errors) = LocationValidator.validate(readInput())
            showErrors(errors.filterKeys { it == Field.LATITUDE || it == Field.LONGITUDE })
            if (valid != null) MapLauncher.show(this, valid.latitude, valid.longitude, valid.title)
        }
        binding.btnSave.setOnClickListener { save() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putStringArrayList(KEY_PENDING, pendingPhotos)
        outState.putInt(KEY_CATEGORY, categoryPosition)
    }

    /** The dropdown shows the category name; the position is kept separately to map it to an id. */
    private fun setupCategoryDropdown(initialPosition: Int) {
        binding.spinnerCategory.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, Categories.names(this)),
        )
        binding.spinnerCategory.setOnItemClickListener { _, _, position, _ -> categoryPosition = position }
        selectCategory(initialPosition)
    }

    private fun selectCategory(position: Int) {
        categoryPosition = position
        binding.spinnerCategory.setText(Categories.names(this)[position], false)
    }

    private fun readInput() = LocationValidator.Input(
        title = binding.etTitle.text?.toString().orEmpty(),
        description = binding.etDescription.text?.toString().orEmpty(),
        latitude = binding.etLatitude.text?.toString().orEmpty(),
        longitude = binding.etLongitude.text?.toString().orEmpty(),
    )

    private fun save() {
        val (valid, errors) = LocationValidator.validate(readInput())
        showErrors(errors)
        if (valid == null) {
            // Alternative flow: show the error and go back to the form.
            Toast.makeText(this, R.string.error_fix_form, Toast.LENGTH_SHORT).show()
            return
        }
        val categoryId = Categories.idForPosition(categoryPosition)
        val photos = storage.commit(pendingPhotos)
        pendingPhotos.clear()
        pendingPhotos += photos   // keep the committed paths if saving fails below

        val savedId = try {
            if (locationId == 0L) {
                dao.insert(valid.title, valid.description, valid.latitude, valid.longitude, categoryId, currentUser.id, photos)
            } else {
                locationId.takeIf {
                    dao.update(it, currentUser.id, valid.title, valid.description, valid.latitude, valid.longitude, categoryId, photos)
                }
            }
        } catch (_: android.database.SQLException) {
            null
        }
        if (savedId == null) {
            Toast.makeText(this, R.string.error_save_failed, Toast.LENGTH_LONG).show()
            return
        }
        pendingPhotos.clear()
        Toast.makeText(
            this, if (locationId == 0L) R.string.location_created else R.string.location_updated, Toast.LENGTH_SHORT,
        ).show()
        if (locationId == 0L) startActivity(LocationDetailActivity.intent(this, savedId))
        finish()
    }

    private fun showErrors(errors: Map<Field, LocationValidator.Error>) {
        fun TextInputLayout.show(field: Field) {
            error = errors[field]?.let { getString(it.messageRes(field)) }
        }
        binding.tilTitle.show(Field.TITLE)
        binding.tilDescription.show(Field.DESCRIPTION)
        binding.tilLatitude.show(Field.LATITUDE)
        binding.tilLongitude.show(Field.LONGITUDE)
    }

    private fun LocationValidator.Error.messageRes(field: Field): Int = when (this) {
        LocationValidator.Error.EMPTY -> R.string.error_required
        LocationValidator.Error.TOO_LONG -> R.string.error_too_long
        LocationValidator.Error.NOT_A_NUMBER -> R.string.error_not_number
        LocationValidator.Error.OUT_OF_RANGE ->
            if (field == Field.LATITUDE) R.string.error_latitude_range else R.string.error_longitude_range
    }

    private fun addThumbnail(path: String) {
        val size = resources.getDimensionPixelSize(R.dimen.thumbnail_size)
        val image = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = size / 10 }
            scaleType = ImageView.ScaleType.CENTER_CROP
            importantForAccessibility = ViewGroup.IMPORTANT_FOR_ACCESSIBILITY_NO
            setImageBitmap(PhotoStorage.loadThumbnail(path, size))
        }
        binding.photoStrip.addView(image)
    }

    companion object {
        private const val EXTRA_LOCATION_ID = "ua.chnu.tourspots.extra.LOCATION_ID"
        private const val EXTRA_CATEGORY_ID = "ua.chnu.tourspots.extra.CATEGORY_ID"
        private const val KEY_PENDING = "edit.pendingPhotos"
        private const val KEY_CATEGORY = "edit.categoryPosition"
        private const val MAX_PHOTOS = 5

        fun intent(context: Context, locationId: Long = 0L, categoryId: Int? = null): Intent =
            Intent(context, LocationEditActivity::class.java)
                .putExtra(EXTRA_LOCATION_ID, locationId)
                .putExtra(EXTRA_CATEGORY_ID, categoryId ?: 0)
    }
}
