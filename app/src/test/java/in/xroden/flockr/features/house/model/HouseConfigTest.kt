package `in`.xroden.flockr.features.house.model

import `in`.xroden.flockr.utils.formatWithHouseConfig
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A house's settings must change what the app shows. The date layout was once stored in upper case,
 * which as a java.time pattern means week-based year and day of year, so these pin the reading.
 */
class HouseConfigTest {

    private fun config(dateFormat: String = "yyyy-MM-dd", timezone: String = "UTC") =
        HouseConfig(id = "c", houseId = "h", dateFormat = dateFormat, timezone = timezone)

    private val newYearsEveWeek = LocalDate(2025, 12, 30)

    @Test
    fun `upper-case stored layout reads as calendar year and day of month`() {
        assertEquals("2025-12-30", newYearsEveWeek.formatWithHouseConfig(config("YYYY-MM-DD")))
    }

    @Test
    fun `each stored layout formats as chosen`() {
        assertEquals("30/12/2025", newYearsEveWeek.formatWithHouseConfig(config("dd/MM/yyyy")))
        assertEquals("12/30/2025", newYearsEveWeek.formatWithHouseConfig(config("MM/DD/YYYY")))
    }

    @Test
    fun `an unrecognised layout falls back to ISO rather than failing`() {
        assertEquals("2025-12-30", newYearsEveWeek.formatWithHouseConfig(config("d.M.yy")))
    }

    @Test
    fun `an unknown time zone falls back to the device's`() {
        assertEquals(TimeZone.currentSystemDefault(), config(timezone = "Mars/Olympus").timeZone())
        assertEquals(TimeZone.of("Asia/Kolkata"), config(timezone = "Asia/Kolkata").timeZone())
    }

    @Test
    fun `a missing config uses the documented defaults`() {
        val none: HouseConfig? = null
        assertEquals(DEFAULT_CURRENCY_CODE, none.currency())
        assertEquals(DateLayout.ISO, none.dateLayout())
    }
}
