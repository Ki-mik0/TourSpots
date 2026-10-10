package ua.chnu.tourspots.auth

/** Input checks for the registration / login forms (technical task 2.1: control of entered data). */
object Validators {

    enum class Error { EMPTY, NAME_TOO_SHORT, LOGIN_FORMAT, PASSWORD_TOO_SHORT, PASSWORDS_DIFFER }

    const val MIN_PASSWORD_LENGTH = 6
    private val LOGIN_REGEX = Regex("^[A-Za-z0-9_.]{3,20}$")

    fun name(value: String): Error? = when {
        value.isBlank() -> Error.EMPTY
        value.trim().length < 2 -> Error.NAME_TOO_SHORT
        else -> null
    }

    fun login(value: String): Error? = when {
        value.isBlank() -> Error.EMPTY
        !LOGIN_REGEX.matches(value) -> Error.LOGIN_FORMAT
        else -> null
    }

    fun password(value: String): Error? = when {
        value.isEmpty() -> Error.EMPTY
        value.length < MIN_PASSWORD_LENGTH -> Error.PASSWORD_TOO_SHORT
        else -> null
    }

    fun confirmation(password: String, confirmation: String): Error? =
        if (password != confirmation) Error.PASSWORDS_DIFFER else null
}
