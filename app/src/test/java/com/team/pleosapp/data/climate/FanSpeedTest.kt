package com.team.pleosapp.data.climate

import com.team.pleosapp.data.signal.ValueBounds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FanSpeedTest {
    private val bounds = ValueBounds(1, 8) // 에뮬레이터 capability

    @Test
    fun outOfBounds_isNull() {
        // VC는 0(꺼짐)을 보낼 수 있지만 capability는 1~8이다.
        assertNull(normalizeFanSpeed(0, bounds))
        assertNull(normalizeFanSpeed(9, bounds))
    }

    @Test
    fun insideBounds_isKept() {
        assertEquals(1, normalizeFanSpeed(1, bounds))
        assertEquals(5, normalizeFanSpeed(5, bounds))
        assertEquals(8, normalizeFanSpeed(8, bounds))
    }

    @Test
    fun unknownBoundsOrValue() {
        assertEquals(0, normalizeFanSpeed(0, null))
        assertNull(normalizeFanSpeed(null, bounds))
    }

    @Test
    fun fakeRepository_appliesBounds() = runTest {
        val repository = FakeClimateRepository()

        assertEquals(2, repository.fanSpeed.first())
        repository.setFanSpeed(0)
        assertNull(repository.fanSpeed.first())
        assertNull(repository.readFanSpeed())
    }
}
