package ua.chnu.tourspots.data

import android.content.ContentValues
import android.database.Cursor

data class Location(
    val id: Long,
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val categoryId: Int,
    val authorId: Long?,
    val authorName: String?,
    val createdAt: Long,
    /** Average rating of all reviews (Location.calculateRating() in the class diagram), 0 if none. */
    val averageRating: Double,
    val reviewCount: Int,
)

data class Review(
    val id: Long,
    val userId: Long,
    val userName: String,
    val text: String,
    val rating: Int,
    val createdAt: Long,
)

data class Photo(val id: Long, val path: String)

/** Technical task: storing and reading tourist locations, their photos and reviews. */
class LocationDao(private val db: TourDbHelper) {

    /**
     * Locations, newest first, optionally filtered by category, author and a search query.
     * The query is matched in Kotlin because SQLite LIKE is case-insensitive only for ASCII,
     * and most titles are in Ukrainian.
     */
    fun list(categoryId: Int? = null, authorId: Long? = null, query: String = ""): List<Location> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        categoryId?.let { where += "l.category_id = ?"; args += it.toString() }
        authorId?.let { where += "l.author_id = ?"; args += it.toString() }
        val sql = SELECT_LOCATIONS +
            (if (where.isEmpty()) "" else " WHERE " + where.joinToString(" AND ")) +
            " ORDER BY l.created_at DESC, l._id DESC"
        val all = db.readableDatabase.rawQuery(sql, args.toTypedArray()).use { c ->
            generateSequence { if (c.moveToNext()) c.toLocation() else null }.toList()
        }
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter { it.title.contains(q, ignoreCase = true) || it.description.contains(q, ignoreCase = true) }
    }

    fun find(id: Long): Location? =
        db.readableDatabase.rawQuery("$SELECT_LOCATIONS WHERE l._id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) c.toLocation() else null
        }

    /** Inserts a location together with its photos in one transaction (TT 2.3: data integrity). */
    fun insert(
        title: String, description: String, latitude: Double, longitude: Double,
        categoryId: Int, authorId: Long, photoPaths: List<String>,
    ): Long = transaction {
        val id = it.insertOrThrow("locations", null, ContentValues().apply {
            put("title", title)
            put("description", description)
            put("latitude", latitude)
            put("longitude", longitude)
            put("category_id", categoryId)
            put("author_id", authorId)
            put("created_at", System.currentTimeMillis())
        })
        insertPhotos(id, photoPaths)
        id
    }

    /** Updates a location; only its author may do it (TT 2.2: access rights). */
    fun update(
        id: Long, authorId: Long, title: String, description: String, latitude: Double, longitude: Double,
        categoryId: Int, newPhotoPaths: List<String>,
    ): Boolean = transaction {
        val updated = it.update("locations", ContentValues().apply {
            put("title", title)
            put("description", description)
            put("latitude", latitude)
            put("longitude", longitude)
            put("category_id", categoryId)
        }, "_id = ? AND author_id = ?", arrayOf(id.toString(), authorId.toString()))
        if (updated == 1) insertPhotos(id, newPhotoPaths)
        updated == 1
    }

    /** Deletes the author's location; photos and reviews are removed by ON DELETE CASCADE. */
    fun delete(id: Long, authorId: Long): Boolean =
        db.writableDatabase.delete("locations", "_id = ? AND author_id = ?", arrayOf(id.toString(), authorId.toString())) == 1

    fun photos(locationId: Long): List<Photo> =
        db.readableDatabase.rawQuery(
            "SELECT _id, uri FROM photos WHERE location_id = ? ORDER BY _id", arrayOf(locationId.toString()),
        ).use { c -> generateSequence { if (c.moveToNext()) Photo(c.getLong(0), c.getString(1)) else null }.toList() }

    fun reviews(locationId: Long): List<Review> =
        db.readableDatabase.rawQuery(
            """
            SELECT r._id, r.user_id, u.name, r.text, r.rating, r.created_at
            FROM reviews r JOIN users u ON u._id = r.user_id
            WHERE r.location_id = ? ORDER BY r.created_at DESC
            """.trimIndent(),
            arrayOf(locationId.toString()),
        ).use { c ->
            generateSequence {
                if (c.moveToNext()) Review(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getInt(4), c.getLong(5))
                else null
            }.toList()
        }

    /** One review per user and location: a second one replaces the first. Returns true if it was new. */
    fun saveReview(locationId: Long, userId: Long, text: String, rating: Int): Boolean = transaction {
        val values = ContentValues().apply {
            put("text", text)
            put("rating", rating)
            put("created_at", System.currentTimeMillis())
        }
        val updated = it.update(
            "reviews", values, "location_id = ? AND user_id = ?", arrayOf(locationId.toString(), userId.toString()),
        )
        if (updated == 0) {
            values.put("location_id", locationId)
            values.put("user_id", userId)
            it.insertOrThrow("reviews", null, values)
        }
        updated == 0
    }

    private fun insertPhotos(locationId: Long, paths: List<String>) {
        val now = System.currentTimeMillis()
        paths.forEach { path ->
            db.writableDatabase.insertOrThrow("photos", null, ContentValues().apply {
                put("location_id", locationId)
                put("uri", path)
                put("uploaded_at", now)
            })
        }
    }

    private inline fun <T> transaction(block: (android.database.sqlite.SQLiteDatabase) -> T): T {
        val database = db.writableDatabase
        database.beginTransaction()
        try {
            return block(database).also { database.setTransactionSuccessful() }
        } finally {
            database.endTransaction()
        }
    }

    private fun Cursor.toLocation() = Location(
        id = getLong(0),
        title = getString(1),
        description = getString(2),
        latitude = getDouble(3),
        longitude = getDouble(4),
        categoryId = getInt(5),
        authorId = if (isNull(6)) null else getLong(6),
        authorName = if (isNull(7)) null else getString(7),
        createdAt = getLong(8),
        averageRating = getDouble(9),
        reviewCount = getInt(10),
    )

    private companion object {
        const val SELECT_LOCATIONS = """
            SELECT l._id, l.title, l.description, l.latitude, l.longitude, l.category_id, l.author_id,
                   u.name, l.created_at,
                   COALESCE((SELECT AVG(r.rating) FROM reviews r WHERE r.location_id = l._id), 0),
                   (SELECT COUNT(*) FROM reviews r WHERE r.location_id = l._id)
            FROM locations l LEFT JOIN users u ON u._id = l.author_id
        """
    }
}
