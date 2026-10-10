package ua.chnu.tourspots.data

import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import ua.chnu.tourspots.auth.PasswordHasher

data class User(val id: Long, val name: String, val login: String, val createdAt: Long)

/** Lab 5: storing users (name, login, password hash) in the database. */
class UserDao(private val db: TourDbHelper) {

    /** Inserts a new user; returns its id, or null when the login is already taken. */
    fun register(name: String, login: String, password: String): Long? {
        val hashed = PasswordHasher.hash(password)
        val values = ContentValues().apply {
            put("name", name)
            put("login", login)
            put("password_hash", hashed.hash)
            put("salt", hashed.salt)
            put("created_at", System.currentTimeMillis())
        }
        return try {
            db.writableDatabase.insertOrThrow("users", null, values)
        } catch (_: SQLiteConstraintException) {
            null
        }
    }

    fun isLoginTaken(login: String): Boolean =
        db.readableDatabase.rawQuery("SELECT 1 FROM users WHERE login = ?", arrayOf(login)).use { it.moveToFirst() }

    /** Returns the user when the login exists and the password matches. */
    fun authenticate(login: String, password: String): User? =
        db.readableDatabase.rawQuery(
            "SELECT _id, name, login, created_at, password_hash, salt FROM users WHERE login = ?",
            arrayOf(login),
        ).use { c ->
            if (!c.moveToFirst()) return null
            val matches = PasswordHasher.verify(password, PasswordHasher.Hashed(c.getString(4), c.getString(5)))
            if (matches) User(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)) else null
        }

    fun findById(id: Long): User? =
        db.readableDatabase.rawQuery(
            "SELECT _id, name, login, created_at FROM users WHERE _id = ?",
            arrayOf(id.toString()),
        ).use { c ->
            if (c.moveToFirst()) User(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)) else null
        }
}
