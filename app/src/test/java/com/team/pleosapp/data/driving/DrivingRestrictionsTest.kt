package com.team.pleosapp.data.driving

import android.car.drivingstate.CarUxRestrictions
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DrivingRestrictionsTest {

    @Test
    fun baseline_hasNoRestrictions() {
        val restrictions = DrivingRestrictions()

        assertFalse(restrictions.noKeyboard)
        assertFalse(restrictions.noVideo)
        assertFalse(restrictions.noSetup)
        assertFalse(restrictions.limitContent)
        assertFalse(restrictions.limitStringLength)
    }

    @Test
    fun activeBits_mapToMatchingProperties() {
        val restrictions = DrivingRestrictions(
            requiresDistractionOptimization = true,
            activeRestrictions = CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD or
                CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO,
        )

        assertTrue(restrictions.noKeyboard)
        assertTrue(restrictions.noVideo)
        assertFalse(restrictions.noSetup)
        assertFalse(restrictions.limitContent)
    }

    @Test
    fun fullyRestricted_blocksEverything() {
        val restrictions = DrivingRestrictions.FullyRestricted

        assertTrue(restrictions.requiresDistractionOptimization)
        assertTrue(restrictions.noKeyboard)
        assertTrue(restrictions.noVideo)
        assertTrue(restrictions.noSetup)
        assertTrue(restrictions.limitContent)
        assertTrue(restrictions.limitStringLength)
    }
}
