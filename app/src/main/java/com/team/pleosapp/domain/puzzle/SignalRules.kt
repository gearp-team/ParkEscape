package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.light.argbToHsv
import com.team.pleosapp.data.light.hueDistance
import kotlin.math.abs

/**
 * 핸들 구간 계산(기획 2-1 D1). 값은 SDK 기준(왼쪽 음수)이고, 계산 전에 capability 범위로 자른다.
 * 한쪽 범위 R = 왼쪽 |min|, 오른쪽 max. 비율 r = |각도| / R.
 *   중앙: r ≤ 5%,  1번: 5% < r < 8.33%,  2번: 8.33% ≤ r < 11.67%,  3번: 11.67% ≤ r ≤ 15%,  그 너머: 칸 밖
 */
object SteeringZones {
    const val CENTER_RATIO = 0.05

    /** 칸 경계(비율): 5%, 8.33%, 11.67%, 15% */
    val BIN_EDGES = doubleArrayOf(0.05, 0.05 + 0.1 / 3, 0.05 + 0.2 / 3, 0.15)

    /** 범위가 쓸 수 있는 모양인지(0을 사이에 두고 폭 10° 이상). F2-data `steeringBoundsProblem`과 같은 조건이다. */
    fun isUsable(min: Float, max: Float): Boolean = (max - min) >= 10f && min < 0f && max > 0f

    private fun clamp(angle: Float, min: Float, max: Float): Float = angle.coerceIn(min, max)

    /** 한쪽 범위에서의 비율(0 이상). */
    fun ratio(angle: Float, min: Float, max: Float): Double {
        val a = clamp(angle, min, max)
        return if (a < 0f) abs(a) / abs(min).toDouble() else a / max.toDouble()
    }

    fun isCentered(angle: Float, min: Float, max: Float): Boolean = ratio(angle, min, max) <= CENTER_RATIO

    /** 각도가 들어 있는 칸(방향, 1~3). 중앙이거나 15%를 넘으면 null. */
    fun binOf(angle: Float, min: Float, max: Float): Pair<SteeringSide, Int>? {
        val r = ratio(angle, min, max)
        val side = if (clamp(angle, min, max) < 0f) SteeringSide.LEFT else SteeringSide.RIGHT
        val bin = when {
            r <= BIN_EDGES[0] -> return null
            r < BIN_EDGES[1] -> 1
            r < BIN_EDGES[2] -> 2
            r <= BIN_EDGES[3] -> 3
            else -> return null
        }
        return side to bin
    }

    /** 화면 게이지용: 칸의 SDK 값 범위(작은 값 → 큰 값). */
    fun binRange(side: SteeringSide, bin: Int, min: Float, max: Float): ClosedFloatingPointRange<Float> {
        require(bin in 1..3) { "bin must be 1..3: $bin" }
        val range = if (side == SteeringSide.LEFT) abs(min) else max
        val lo = (BIN_EDGES[bin - 1] * range).toFloat()
        val hi = (BIN_EDGES[bin] * range).toFloat()
        return if (side == SteeringSide.LEFT) -hi..-lo else lo..hi
    }
}

/** 앰비언트 색 판정(기획 2-1 L1): 알파는 보지 않고 RGB→HSV, 색상차 ≤ 30°, 채도 ≥ 0.3. */
object ColorRule {
    const val MAX_HUE_DISTANCE = 30f
    const val MIN_SATURATION = 0.3f

    fun matches(argb: Int, color: NamedColor): Boolean {
        val hsv = argbToHsv(argb)
        return hsv.saturation >= MIN_SATURATION && hueDistance(hsv.hue, color.hue) <= MAX_HUE_DISTANCE
    }

    /** RGB만 비교(알파 무시). 복구 체크리스트에 쓴다. */
    fun sameRgb(a: Int, b: Int): Boolean = (a and 0xFFFFFF) == (b and 0xFFFFFF)
}
