package com.team.pleosapp.data.light

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** HSV 색. hue 0~360°, saturation·value 0~1. */
data class Hsv(val hue: Float, val saturation: Float, val value: Float)

/**
 * ARGB(또는 RGB) 정수를 HSV로 바꾼다. 알파는 보지 않는다.
 * 앰비언트 색은 알파가 0x00으로 온다(에뮬레이터 실측: #aa00ff → 0x00AA00FF). 판정기에서도 이 함수를 쓴다.
 * android.graphics.Color.colorToHSV와 같은 식이지만 JVM 단위 테스트에서 돌도록 순수 Kotlin으로 썼다.
 */
fun argbToHsv(argb: Int): Hsv {
    val r = ((argb shr 16) and 0xFF) / 255f
    val g = ((argb shr 8) and 0xFF) / 255f
    val b = (argb and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val hue = when {
        delta == 0f -> 0f
        maxC == r -> 60f * (((g - b) / delta) % 6f)
        maxC == g -> 60f * ((b - r) / delta + 2f)
        else -> 60f * ((r - g) / delta + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val saturation = if (maxC == 0f) 0f else delta / maxC
    return Hsv(hue, saturation, maxC)
}

/** 알파를 0xFF로 채운 불투명 ARGB. 화면에 그대로 칠하거나 원래 색과 비교할 때 쓴다. */
fun opaqueArgb(argb: Int): Int = argb or OPAQUE_ALPHA

/** 두 색상(°) 사이의 원형 거리(0~180). */
fun hueDistance(a: Float, b: Float): Float {
    val d = abs(a - b) % 360f
    return if (d > 180f) 360f - d else d
}

private const val OPAQUE_ALPHA = 0xFF shl 24
