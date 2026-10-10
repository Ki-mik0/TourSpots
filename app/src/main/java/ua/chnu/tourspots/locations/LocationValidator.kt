package ua.chnu.tourspots.locations

/**
 * Checks the "add location" form (activity diagram: "Система валідує дані").
 * Kept free of Android classes so it can be unit-tested on the JVM.
 */
object LocationValidator {

    enum class Field { TITLE, DESCRIPTION, LATITUDE, LONGITUDE }
    enum class Error { EMPTY, TOO_LONG, NOT_A_NUMBER, OUT_OF_RANGE }

    data class Input(val title: String, val description: String, val latitude: String, val longitude: String)
    data class Valid(val title: String, val description: String, val latitude: Double, val longitude: Double)

    const val MAX_TITLE = 100
    const val MAX_DESCRIPTION = 2000

    /** Returns either the cleaned values or the errors per field. */
    fun validate(input: Input): Pair<Valid?, Map<Field, Error>> {
        val errors = mutableMapOf<Field, Error>()
        val title = input.title.trim()
        val description = input.description.trim()

        when {
            title.isEmpty() -> errors[Field.TITLE] = Error.EMPTY
            title.length > MAX_TITLE -> errors[Field.TITLE] = Error.TOO_LONG
        }
        when {
            description.isEmpty() -> errors[Field.DESCRIPTION] = Error.EMPTY
            description.length > MAX_DESCRIPTION -> errors[Field.DESCRIPTION] = Error.TOO_LONG
        }
        val latitude = coordinate(input.latitude, 90.0, Field.LATITUDE, errors)
        val longitude = coordinate(input.longitude, 180.0, Field.LONGITUDE, errors)

        if (errors.isNotEmpty() || latitude == null || longitude == null) return null to errors
        return Valid(title, description, latitude, longitude) to errors
    }

    private fun coordinate(raw: String, limit: Double, field: Field, errors: MutableMap<Field, Error>): Double? {
        val text = raw.trim().replace(',', '.')
        if (text.isEmpty()) {
            errors[field] = Error.EMPTY
            return null
        }
        val value = text.toDoubleOrNull()
        if (value == null || value.isNaN()) {
            errors[field] = Error.NOT_A_NUMBER
            return null
        }
        if (value < -limit || value > limit) {
            errors[field] = Error.OUT_OF_RANGE
            return null
        }
        return value
    }
}
