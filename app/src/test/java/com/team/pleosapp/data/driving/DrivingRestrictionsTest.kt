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

    @Test
    fun parkedGate_onlyWithoutDoAndBits() {
        assertTrue(DrivingRestrictions().isParked) // 정차 0x0
        assertFalse(DrivingRestrictions(true, CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO or CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP).isParked) // 공회전·주행 0x50
        assertFalse(DrivingRestrictions(true, CarUxRestrictions.UX_RESTRICTIONS_BASELINE).isParked) // DO만 필요
        assertFalse(DrivingRestrictions(false, CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO).isParked) // 비트만 있음
        assertFalse(DrivingRestrictions.FullyRestricted.isParked) // 값 모름 = 닫힘
    }
}
