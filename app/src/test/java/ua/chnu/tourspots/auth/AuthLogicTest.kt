package ua.chnu.tourspots.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthLogicTest {

    @Test
    fun passwordHashVerifiesOnlyTheOriginalPassword() {
        val stored = PasswordHasher.hash("secret123")
        assertNotEquals("secret123", stored.hash)
        assertTrue(PasswordHasher.verify("secret123", stored))
        assertFalse(PasswordHasher.verify("secret124", stored))
    }

    @Test
    fun samePasswordGetsDifferentSalts() {
        val a = PasswordHasher.hash("secret123")
        val b = PasswordHasher.hash("secret123")
        assertNotEquals(a.salt, b.salt)
        assertNotEquals(a.hash, b.hash)
    }

    @Test
    fun validatesName() {
        assertEquals(Validators.Error.EMPTY, Validators.name("  "))
        assertEquals(Validators.Error.NAME_TOO_SHORT, Validators.name("V"))
        assertNull(Validators.name("Вікторія"))
    }

    @Test
    fun validatesLogin() {
        assertEquals(Validators.Error.EMPTY, Validators.login(""))
        assertEquals(Validators.Error.LOGIN_FORMAT, Validators.login("ab"))
        assertEquals(Validators.Error.LOGIN_FORMAT, Validators.login("логін"))
        assertEquals(Validators.Error.LOGIN_FORMAT, Validators.login("with space"))
        assertNull(Validators.login("viktoria.a_13"))
    }

    @Test
    fun validatesPasswordAndConfirmation() {
        assertEquals(Validators.Error.EMPTY, Validators.password(""))
        assertEquals(Validators.Error.PASSWORD_TOO_SHORT, Validators.password("12345"))
        assertNull(Validators.password("123456"))
        assertEquals(Validators.Error.PASSWORDS_DIFFER, Validators.confirmation("123456", "123457"))
        assertNull(Validators.confirmation("123456", "123456"))
    }
}
