package ua.chnu.tourspots.locations

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ua.chnu.tourspots.locations.LocationValidator.Error
import ua.chnu.tourspots.locations.LocationValidator.Field
import ua.chnu.tourspots.locations.LocationValidator.Input

class LocationValidatorTest {

    @Test
    fun acceptsValidInputAndTrimsIt() {
        val (valid, errors) = LocationValidator.validate(Input("  Говерла ", " Вершина ", "48.1603", "24,5"))
        assertTrue(errors.isEmpty())
        assertNotNull(valid)
        assertEquals("Говерла", valid!!.title)
        assertEquals("Вершина", valid.description)
        assertEquals(48.1603, valid.latitude, 1e-9)
        assertEquals(24.5, valid.longitude, 1e-9)   // comma accepted as decimal separator
    }

    @Test
    fun reportsEmptyFields() {
        val (valid, errors) = LocationValidator.validate(Input("", " ", "", ""))
        assertNull(valid)
        assertEquals(
            mapOf(Field.TITLE to Error.EMPTY, Field.DESCRIPTION to Error.EMPTY,
                Field.LATITUDE to Error.EMPTY, Field.LONGITUDE to Error.EMPTY),
            errors,
        )
    }

    @Test
    fun rejectsWrongCoordinateFormatAndRange() {
        val (valid, errors) = LocationValidator.validate(Input("A", "B", "abc", "181"))
        assertNull(valid)
        assertEquals(Error.NOT_A_NUMBER, errors[Field.LATITUDE])
        assertEquals(Error.OUT_OF_RANGE, errors[Field.LONGITUDE])
        assertEquals(Error.OUT_OF_RANGE, LocationValidator.validate(Input("A", "B", "-90.5", "0")).second[Field.LATITUDE])
        assertEquals(Error.NOT_A_NUMBER, LocationValidator.validate(Input("A", "B", "NaN", "0")).second[Field.LATITUDE])
    }

    @Test
    fun acceptsBoundaryCoordinates() {
        val (valid, _) = LocationValidator.validate(Input("A", "B", "-90", "180"))
        assertNotNull(valid)
    }

    @Test
    fun rejectsTooLongTitle() {
        val (_, errors) = LocationValidator.validate(Input("x".repeat(LocationValidator.MAX_TITLE + 1), "B", "0", "0"))
        assertEquals(Error.TOO_LONG, errors[Field.TITLE])
    }
}
