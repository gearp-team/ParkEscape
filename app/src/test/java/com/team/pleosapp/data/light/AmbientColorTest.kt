package com.team.pleosapp.data.light

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AmbientColorTest {
    private val delta = 0.1f

    @Test
    fun alphaZero_isIgnored() {
        // SDK는 알파를 0x00으로 준다(에뮬레이터 실측 0x00AA00FF).
        assertEquals(argbToHsv(0xFFAA00FF.toInt()), argbToHsv(0x00AA00FF))
    }

    @Test
    fun virtualConsolePresets_matchMeasuredHue() {
        assertHsv(argbToHsv(0x00AA00FF), hue = 280.0f, saturation = 1f, value = 1f) // Purple
        assertHsv(argbToHsv(0x002979FF), hue = 217.6f, saturation = 0.84f, value = 1f) // Blue
        assertHsv(argbToHsv(0x0000E5FF), hue = 186.1f, saturation = 1f, value = 1f) // Cyan(기본값)
    }

    @Test
    fun whiteAndBlack_haveNoSaturation() {
        assertHsv(argbToHsv(0x00FFFFFF), hue = 0f, saturation = 0f, value = 1f)
        assertHsv(argbToHsv(0x00000000), hue = 0f, saturation = 0f, value = 0f)
    }

    @Test
    fun primaries_andNegativeHueWrap() {
        assertHsv(argbToHsv(0x00FF0000), hue = 0f, saturation = 1f, value = 1f)
        assertHsv(argbToHsv(0x0000FF00), hue = 120f, saturation = 1f, value = 1f)
        assertHsv(argbToHsv(0x000000FF), hue = 240f, saturation = 1f, value = 1f)
        // 빨강이 최대이고 초록 < 파랑이면 음수 색상이 나와 360을 더한다.
        assertHsv(argbToHsv(0x00FF0080), hue = 329.9f, saturation = 1f, value = 1f)
        // 주황(30°)
        assertHsv(argbToHsv(0x00FF8000), hue = 30.1f, saturation = 1f, value = 1f)
    }

    @Test
    fun hueDistance_isCircular() {
        assertEquals(20f, hueDistance(350f, 10f), delta)
        assertEquals(22.4f, hueDistance(217.6f, 240f), delta)
        assertEquals(180f, hueDistance(0f, 180f), delta)
        assertEquals(0f, hueDistance(0f, 360f), delta)
    }

    @Test
    fun opaqueArgb_fillsAlpha() {
        assertEquals(0xFFAA00FF.toInt(), opaqueArgb(0x00AA00FF))
        assertEquals(0xFFFFFFFF.toInt(), opaqueArgb(0xFFFFFFFF.toInt()))
    }

    @Test
    fun fakeRepository_emitsOpaqueColor() = runTest {
        val repository = FakeCabinLightRepository()

        // 기본값은 시연 0단계와 같은 Cyan(SDK 형식 0x0000E5FF)·밝기 100이고, 내보낼 때 알파를 채운다.
        assertEquals(0xFF00E5FF.toInt(), repository.ambientColor.first())
        assertEquals(100, repository.readAmbientBrightness())
        repository.setAmbientColor(FakeCabinLightRepository.WHITE)
        assertEquals(0xFFFFFFFF.toInt(), repository.readAmbientColor())
        repository.setAmbientColor(0x00AA00FF)
        assertEquals(0xFFAA00FF.toInt(), repository.readAmbientColor())
    }

    private fun assertHsv(actual: Hsv, hue: Float, saturation: Float, value: Float) {
        assertEquals("hue", hue, actual.hue, delta)
        assertEquals("saturation", saturation, actual.saturation, 0.01f)
        assertEquals("value", value, actual.value, 0.01f)
    }
}
