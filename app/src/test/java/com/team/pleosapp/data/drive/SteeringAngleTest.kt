package com.team.pleosapp.data.drive

import com.team.pleosapp.data.signal.ValueBounds
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SteeringAngleTest {

    @Test
    fun repeatedSameValue_isDropped() = runTest {
        // 에뮬레이터는 값이 그대로여도 약 3.1초마다 같은 값을 다시 보낸다.
        val angles = flowOf<Float?>(-45f, -45f, -45f, 0f, 0f, 100f).distinctAngles().toList()

        assertEquals(listOf<Float?>(-45f, 0f, 100f), angles)
    }

    @Test
    fun negativeZero_equalsZero() = runTest {
        // 에뮬레이터는 중앙을 -0.0으로 준다. 박싱된 Float.equals는 -0.0과 0.0을 다르게 보므로 정규화가 필요하다.
        val angles = flowOf<Float?>(-0.0f, 0.0f, -0.0f, 45f).distinctAngles().toList()

        assertEquals(listOf<Float?>(0f, 45f), angles)
        assertEquals(0f, normalizeAngle(-0.0f))
    }

    @Test
    fun nullBetweenValues_isKeptOnce() = runTest {
        val angles = flowOf(10f, null, null, 10f).distinctAngles().toList()

        assertEquals(listOf(10f, null, 10f), angles)
        assertNull(normalizeAngle(null))
    }

    @Test
    fun fakeRepository_normalizesAngleAndUsesEmulatorBounds() = runTest {
        val repository = FakeDriveInputRepository(steeringAngle = -0.0f)

        assertEquals(0f, repository.readSteeringAngle())
        assertEquals(-470f, repository.support().steeringAngle.bounds?.min)
    }

    @Test
    fun steeringBounds_mustSpanCenterAndBeWideEnough() {
        assertNull(steeringBoundsProblem(ValueBounds(-470f, 500f))) // 에뮬레이터
        assertNull(steeringBoundsProblem(ValueBounds(-37f, 37f))) // 앞바퀴 각도로 오는 실차[가정]
        assertNotNull(steeringBoundsProblem(ValueBounds(-4f, 4f))) // 폭 8°
        assertNotNull(steeringBoundsProblem(ValueBounds(0f, 500f))) // 한쪽뿐
        assertNotNull(steeringBoundsProblem(ValueBounds(0f, 0f)))
        assertNotNull(steeringBoundsProblem(ValueBounds(Float.NaN, Float.NaN)))
    }
}
