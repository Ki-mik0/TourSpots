package ua.chnu.tourspots.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Lab 5 / technical task: local SQLite database of the app.
 * Tables follow the conceptual class diagram: User, Category, Location, Review, Photo.
 */
class TourDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE users (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                login TEXT NOT NULL UNIQUE COLLATE NOCASE,
                password_hash TEXT NOT NULL,
                salt TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE categories (
                _id INTEGER PRIMARY KEY,
                name TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE locations (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                latitude REAL NOT NULL CHECK (latitude BETWEEN -90 AND 90),
                longitude REAL NOT NULL CHECK (longitude BETWEEN -180 AND 180),
                category_id INTEGER NOT NULL REFERENCES categories(_id),
                author_id INTEGER REFERENCES users(_id) ON DELETE SET NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE reviews (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                location_id INTEGER NOT NULL REFERENCES locations(_id) ON DELETE CASCADE,
                user_id INTEGER NOT NULL REFERENCES users(_id) ON DELETE CASCADE,
                text TEXT NOT NULL,
                rating INTEGER NOT NULL CHECK (rating BETWEEN 1 AND 5),
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE photos (
                _id INTEGER PRIMARY KEY AUTOINCREMENT,
                location_id INTEGER NOT NULL REFERENCES locations(_id) ON DELETE CASCADE,
                uri TEXT NOT NULL,
                uploaded_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_locations_category ON locations(category_id)")
        db.execSQL("CREATE INDEX idx_reviews_location ON reviews(location_id)")
        seed(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Only one schema version so far; future migrations go here step by step.
    }

    /** Categories (ids match the order of @array/categories) and a few sample locations. */
    private fun seed(db: SQLiteDatabase) {
        SEED_CATEGORIES.forEachIndexed { index, name ->
            db.insert("categories", null, ContentValues().apply {
                put("_id", index + 1)
                put("name", name)
            })
        }
        val now = System.currentTimeMillis()
        SEED_LOCATIONS.forEach { (title, description, coordinates, categoryId) ->
            db.insert("locations", null, ContentValues().apply {
                put("title", title)
                put("description", description)
                put("latitude", coordinates.first)
                put("longitude", coordinates.second)
                put("category_id", categoryId)
                put("created_at", now)
            })
        }
    }

    companion object {
        const val DB_NAME = "tourspots.db"
        const val DB_VERSION = 1

        private val SEED_CATEGORIES = listOf("Гори", "Озера", "Замки", "Музеї", "Парки")

        private data class Seed(
            val title: String,
            val description: String,
            val coordinates: Pair<Double, Double>,
            val categoryId: Int,
        )

        private val SEED_LOCATIONS = listOf(
            Seed("Говерла", "Найвища вершина України, 2061 м.", 48.1603 to 24.5000, 1),
            Seed("Піп Іван Чорногірський", "Вершина з руїнами обсерваторії «Білий слон».", 48.0469 to 24.6278, 1),
            Seed("Синевир", "Найбільше високогірне озеро Українських Карпат.", 48.6170 to 23.6830, 2),
            Seed("Світязь", "Найглибше озеро карстового походження в Україні.", 51.4950 to 23.8530, 2),
            Seed("Хотинська фортеця", "Середньовічна фортеця на березі Дністра.", 48.5214 to 26.4983, 3),
            Seed("Кам'янець-Подільська фортеця", "Фортеця XIV ст. над каньйоном Смотрича.", 48.6736 to 26.5636, 3),
            Seed("Резиденція митрополитів Буковини", "Об'єкт світової спадщини ЮНЕСКО, Чернівці.", 48.2964 to 25.9241, 4),
            Seed("Софіївка", "Дендрологічний парк кінця XVIII ст. в Умані.", 48.7633 to 30.2228, 5),
        )

        @Volatile
        private var instance: TourDbHelper? = null

        fun get(context: Context): TourDbHelper =
            instance ?: synchronized(this) {
                instance ?: TourDbHelper(context).also { instance = it }
            }
    }
}
