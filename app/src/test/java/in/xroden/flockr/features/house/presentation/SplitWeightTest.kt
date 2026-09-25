package `in`.xroden.flockr.features.house.presentation

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** A member's default weight must fit `house_members.default_split_weight numeric(8, 3)` and be positive. */
class SplitWeightTest {

    @Test
    fun `whole and fractional weights within the column are accepted`() {
        assertNull(splitWeightError("1"))
        assertNull(splitWeightError("1.5"))
        assertNull(splitWeightError("2,25"))
        assertNull(splitWeightError("99999.999"))
    }

    @Test
    fun `weights the database would reject are refused before saving`() {
        assertNotNull(splitWeightError("0"))
        assertNotNull(splitWeightError("-1"))
        assertNotNull(splitWeightError("1.2345"))
        assertNotNull(splitWeightError("100000"))
        assertNotNull(splitWeightError("two"))
    }
}
