package com.team.pleosapp.data.seat

import com.team.pleosapp.data.signal.SeatPosition.DRIVER
import com.team.pleosapp.data.signal.SeatPosition.PASSENGER
import com.team.pleosapp.data.signal.SeatPosition.REAR_CENTER
import com.team.pleosapp.data.signal.SeatPosition.REAR_LEFT
import com.team.pleosapp.data.signal.SeatPosition.REAR_RIGHT
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SeatOccupancyTest {

    @Test
    fun driverReadAsFalse_invertsEverySeat() {
        // 에뮬레이터 실측: VC에서 운전석·2열 오른쪽을 "점유"로 두면 SDK는 둘을 false, 나머지를 true로 준다.
        val occupancy = SeatOccupancy(
            mapOf(DRIVER to false, PASSENGER to true, REAR_LEFT to true, REAR_CENTER to true, REAR_RIGHT to false),
        )

        assertTrue(occupancy.polarityKnown)
        assertTrue(occupancy.inverted)
        assertEquals(
            mapOf(DRIVER to true, PASSENGER to false, REAR_LEFT to false, REAR_CENTER to false, REAR_RIGHT to true),
            occupancy.corrected,
        )
        assertEquals(setOf(DRIVER, REAR_RIGHT), occupancy.participants)
    }

    @Test
    fun driverReadAsTrue_keepsDirection() {
        val occupancy = SeatOccupancy(
            mapOf(DRIVER to true, PASSENGER to true, REAR_LEFT to false, REAR_CENTER to false, REAR_RIGHT to false),
        )

        assertFalse(occupancy.inverted)
        assertEquals(setOf(DRIVER, PASSENGER), occupancy.participants)
    }

    @Test
    fun emulatorDefault_withoutDriverSet_readsEveryoneOccupied() {
        // VC 기본값(모두 비어 있음)은 SDK에서 5좌석 모두 true로 온다. 운전석이 true라 뒤집지 않지만, 운전석이 점유로 잡히지 않은
        // 이 상태는 실제로는 "모두 점유"로 읽힌다. 시연 0단계에서 VC 운전석을 "점유"로 두어야 보정이 맞다(spike_w1.md).
        val occupancy = SeatOccupancy(
            mapOf(DRIVER to true, PASSENGER to true, REAR_LEFT to true, REAR_CENTER to true, REAR_RIGHT to true),
        )

        assertFalse(occupancy.inverted)
        assertEquals(setOf(DRIVER, PASSENGER, REAR_LEFT, REAR_RIGHT), occupancy.participants)
    }

    @Test
    fun rearCenter_neverParticipates() {
        val occupancy = SeatOccupancy(mapOf(DRIVER to true, REAR_CENTER to true))

        assertTrue(occupancy.corrected.getValue(REAR_CENTER) == true)
        assertFalse(REAR_CENTER in occupancy.participants)
    }

    @Test
    fun unknownOrUnsupported_fallsBackToDriverOnly() {
        assertEquals(setOf(DRIVER), SeatOccupancy(emptyMap()).participants)

        // 운전석 값이 null이면 방향을 정할 수 없으므로 다른 좌석 값(PASSENGER true)도 쓰지 않는다.
        val unknownDriver = SeatOccupancy(mapOf(DRIVER to null, PASSENGER to true, REAR_LEFT to false))
        assertFalse(unknownDriver.polarityKnown)
        assertFalse(unknownDriver.inverted)
        assertEquals(setOf(DRIVER), unknownDriver.participants)
        assertEquals(mapOf(DRIVER to true, PASSENGER to null, REAR_LEFT to null), unknownDriver.corrected)
    }

    @Test
    fun noDriverArea_fallsBackToDriverOnly() {
        // 점유 capability에 운전석 단일 area가 없는 차(뒷좌석만 감지 등): raw에 DRIVER 키가 없다.
        val noDriverKey = SeatOccupancy(mapOf(REAR_LEFT to true, REAR_RIGHT to true, PASSENGER to false))

        assertFalse(noDriverKey.polarityKnown)
        assertEquals(setOf(DRIVER), noDriverKey.participants)
        assertEquals(null, noDriverKey.corrected[REAR_LEFT])
        assertEquals(true, noDriverKey.corrected[DRIVER])
    }

    @Test
    fun fakeRepository_defaultIsDriverAndRearRight() = runTest {
        val repository = FakeSeatRepository()

        assertEquals(setOf(DRIVER, REAR_RIGHT), repository.occupancy.first().participants)

        repository.setSeat(REAR_LEFT, true)
        assertEquals(setOf(DRIVER, REAR_LEFT, REAR_RIGHT), repository.readOccupancy().participants)
    }
}
