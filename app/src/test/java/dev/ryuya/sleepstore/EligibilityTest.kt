package dev.ryuya.sleepstore

import org.junit.Assert.*
import org.junit.Test

open class EligibilityTest {
    @Test
    open fun manualDisableAndUnknownSignalsAreNeverOverridden() {
        for (state in intArrayOf(0, 1, 2, 3, 5, -1)) for (signal in
            booleanArrayOf(false, true)) assertFalse(Eligibility.isDeepSleeping(state, signal))
        assertFalse(Eligibility.isDeepSleeping(4, false))
        assertTrue(Eligibility.isDeepSleeping(4, true))
    }
}
