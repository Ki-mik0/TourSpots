package ua.chnu.tourspots.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Passwords are never stored in plain text: PBKDF2-HMAC-SHA256 with a random per-user salt. */
object PasswordHasher {

    data class Hashed(val hash: String, val salt: String)

    private const val ITERATIONS = 10_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16

    fun hash(password: String): Hashed {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return Hashed(encode(derive(password, salt)), encode(salt))
    }

    fun verify(password: String, stored: Hashed): Boolean {
        val expected = Base64.getDecoder().decode(stored.hash)
        val actual = derive(password, Base64.getDecoder().decode(stored.salt))
        return MessageDigest.isEqual(expected, actual)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}
