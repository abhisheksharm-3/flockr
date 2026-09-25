package `in`.xroden.flockr.features.house.model

import `in`.xroden.flockr.utils.formatWithHouseConfig
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * A house's settings must change what the app shows. 30 December 2025 falls in week-based year 2026,
 * so it catches a layout read with `YYYY` or `DD` by mistake.
 */
class HouseConfigTest {

    private fun config(dateFormat: String = "yyyy-MM-dd", timezone: String = "UTC") =
        HouseConfig(houseId = "h", dateFormat = dateFormat, timezone = timezone)

    private val newYearsEveWeek = LocalDate(2025, 12, 30)

    @Test
    fun `each stored layout formats as chosen`() {
        assertEquals("30/12/2025", newYearsEveWeek.formatWithHouseConfig(config("dd/MM/yyyy")))
        assertEquals("12/30/2025", newYearsEveWeek.formatWithHouseConfig(config("MM/dd/yyyy")))
        assertEquals("2025-12-30", newYearsEveWeek.formatWithHouseConfig(config("yyyy-MM-dd")))
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
