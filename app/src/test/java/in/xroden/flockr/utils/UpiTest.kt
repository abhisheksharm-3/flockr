package `in`.xroden.flockr.utils

import java.math.BigDecimal
import org.junit.Assert.assertEquals
import org.junit.Test

class UpiTest {

    @Test
    fun `link carries the payee, two-decimal rupees and an encoded note`() {
        assertEquals(
            "upi://pay?pa=riya%40okaxis&pn=Riya%20Sen&am=560.10&cu=INR&tn=Flockr%3A%20Maple%20Street",
            upiPayLink("riya@okaxis", "Riya Sen", BigDecimal("560.1"), "Flockr: Maple Street"),
        )
    }
}
