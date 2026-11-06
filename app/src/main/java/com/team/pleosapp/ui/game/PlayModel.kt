package com.team.pleosapp.ui.game

import com.team.pleosapp.data.light.argbToHsv
import com.team.pleosapp.data.light.hueDistance
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.domain.clue.ClueWriter
import com.team.pleosapp.domain.puzzle.BrakeProgress
import com.team.pleosapp.domain.puzzle.ClueType
import com.team.pleosapp.domain.puzzle.JudgeState
import com.team.pleosapp.domain.puzzle.NamedColor
import com.team.pleosapp.domain.puzzle.Puzzle
import com.team.pleosapp.domain.puzzle.RestoreItem
import com.team.pleosapp.domain.puzzle.SignalId
import com.team.pleosapp.domain.puzzle.SignalPool
import com.team.pleosapp.domain.puzzle.SteeringSide
import com.team.pleosapp.domain.puzzle.Target
import com.team.pleosapp.domain.puzzle.VehicleSignals
import java.util.Locale

/*
 * S2 게이지·S3 체크리스트의 표시 규칙. Android에 의존하지 않아 단위 테스트로 고정한다.
 * 핵심 규칙(기획 2-1·리더 지시): 지시형은 목표 구간을 보여 주고, 암호형·연쇄형은 목표를 드러내지 않고 현재값만 보여 준다.
 */

/** 게이지 눈금 범위. 지원 범위(capability)를 쓰고, 모르면 Virtual Console 범위를 쓴다. */
data class DisplayBounds(
    val temperature: ClosedFloatingPointRange<Float> = 16f..30f,
    val fan: IntRange = 1..8,
    val brightness: IntRange = 0..100,
) {
    companion object {
        fun from(pool: SignalPool?): DisplayBounds {
            val default = DisplayBounds()
            return DisplayBounds(
                temperature = pool?.temperature?.let { it.min..it.max } ?: default.temperature,
                fan = pool?.fanSpeed?.let { it.min..it.max } ?: default.fan,
                brightness = pool?.ambientBrightness?.let { it.min..it.max } ?: default.brightness,
            )
        }
    }
}

/** 현재 신호 게이지. [target]이 null이면 목표를 그리지 않는다. */
sealed class Gauge {
    /** C1 온도·C2 풍량·L2 밝기: 막대 눈금과 현재값. */
    data class Scale(
        val signal: SignalId,
        val current: Float?,
        val min: Float,
        val max: Float,
        val target: ClosedFloatingPointRange<Float>?,
    ) : Gauge()

    /** C3 내외기: 외기 / 내기 순환 두 칸. */
    data class Recirculation(val current: Boolean?, val target: Boolean?) : Gauge()

    /** C4 앞유리 제상·L4 전조등: 꺼짐 / 켜짐 두 칸. */
    data class Switch(val signal: SignalId, val current: Boolean?, val target: Boolean?) : Gauge()

    /** L1 앰비언트 색: 현재 색과 목표 후보 색 6개. */
    data class AmbientColor(val currentArgb: Int?, val target: NamedColor?) : Gauge()

    /** L3 독서등: 동승 좌석 셋의 켜짐. */
    data class ReadingLights(val seats: List<Pair<SeatPosition, Boolean?>>, val target: SeatPosition?) : Gauge()

    /** D1 핸들: 양쪽 3칸과 바늘. [held]면 칸 유지를 마쳐 가운데로 돌아오기를 기다린다. */
    data class Steering(
        val angle: Float?,
        val boundsMin: Float,
        val boundsMax: Float,
        val target: Pair<SteeringSide, Int>?,
        val held: Boolean,
        val holdMs: Long?,
    ) : Gauge()

    /** B1 봉인: 누른 횟수와 30초 창. */
    data class Brake(val progress: BrakeProgress) : Gauge()

    /** 브레이크 미지원 차량의 봉인 버튼. */
    data object SealButton : Gauge()
}

object Gauges {
    /** 동승 좌석(자리 단서 후보). 2열 가운데는 쓰지 않는다. */
    val PASSENGER_SEATS = listOf(SeatPosition.PASSENGER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT)

    /** 목표를 보여 주는 퍼즐인가: 지시형만(연쇄형을 지시형으로 바꾼 퍼즐 포함). */
    fun showsTarget(puzzle: Puzzle): Boolean = ClueWriter.effectiveType(puzzle) == ClueType.DIRECT

    fun of(
        puzzle: Puzzle,
        signals: VehicleSignals?,
        bounds: DisplayBounds,
        judge: JudgeState?,
        brake: BrakeProgress?,
        steeringHoldMs: Long?,
    ): Gauge {
        val show = showsTarget(puzzle)
        val s = signals ?: VehicleSignals()
        return when (val t = puzzle.target) {
            is Target.Temperature -> Gauge.Scale(
                SignalId.C1,
                s.targetTemperature,
                bounds.temperature.start,
                bounds.temperature.endInclusive,
                if (show) (t.celsius - 0.5f)..(t.celsius + 0.5f) else null,
            )
            is Target.FanSpeed -> Gauge.Scale(
                SignalId.C2,
                s.fanSpeed?.toFloat(),
                bounds.fan.first.toFloat(),
                bounds.fan.last.toFloat(),
                if (show) (t.level - 0.5f)..(t.level + 0.5f) else null,
            )
            is Target.Brightness -> Gauge.Scale(
                SignalId.L2,
                s.ambientBrightness?.toFloat(),
                bounds.brightness.first.toFloat(),
                bounds.brightness.last.toFloat(),
                if (show) t.level.toFloat()..t.level.toFloat() else null,
            )
            is Target.Recirculation -> Gauge.Recirculation(s.recirculation, if (show) t.on else null)
            is Target.Defroster -> Gauge.Switch(SignalId.C4, s.defroster, if (show) t.on else null)
            is Target.LowBeam -> Gauge.Switch(SignalId.L4, s.lowBeam, if (show) t.on else null)
            is Target.AmbientColor -> Gauge.AmbientColor(s.ambientColor, if (show) t.color else null)
            is Target.ReadingLight -> Gauge.ReadingLights(PASSENGER_SEATS.map { it to s.readingLights[it] }, if (show) t.seat else null)
            is Target.Steering -> Gauge.Steering(
                s.steeringAngle,
                t.boundsMin,
                t.boundsMax,
                if (show) t.side to t.bin else null,
                held = judge == JudgeState.Progress(1, 1) || judge == JudgeState.NeedsSteeringCheck,
                holdMs = steeringHoldMs,
            )
            is Target.BrakePresses -> Gauge.Brake(brake ?: BrakeProgress(0, t.count, null))
            Target.SealButton -> Gauge.SealButton
        }
    }
}

/** 색 이름(표시 전용, 판정과 무관). 채도가 낮으면 흰색 계열. */
object ColorNames {
    private val NAMES = listOf(
        0f to "빨강", 30f to "주황", 60f to "노랑", 90f to "연두", 120f to "초록", 180f to "청록",
        240f to "파랑", 280f to "보라", 320f to "분홍", 360f to "빨강",
    )

    fun describe(argb: Int?): String {
        if (argb == null) return "확인 중"
        val hsv = argbToHsv(argb)
        if (hsv.saturation < 0.3f || hsv.value < 0.2f) return "흰색 계열"
        return NAMES.minBy { hueDistance(hsv.hue, it.first) }.second
    }

    /** 목표 후보 색의 견본(#RRGGBB). 시연 프리셋(보라 #aa00ff, 파랑 #2979ff)과 같은 색상. */
    fun swatch(color: NamedColor): Int = when (color) {
        NamedColor.RED -> 0xFFFF1744.toInt()
        NamedColor.ORANGE -> 0xFFFF8000.toInt()
        NamedColor.YELLOW -> 0xFFFFD600.toInt()
        NamedColor.GREEN -> 0xFF00C853.toInt()
        NamedColor.BLUE -> 0xFF2979FF.toInt()
        NamedColor.PURPLE -> 0xFFAA00FF.toInt()
    }

    fun hex(argb: Int): String = String.format(Locale.US, "#%06X", argb and 0xFFFFFF)
}

/** S3 복구 체크리스트 한 줄의 글자. */
data class RestoreRow(val signal: SignalId, val title: String, val detail: String, val restored: Boolean)

object RestoreRows {
    fun of(items: List<RestoreItem>): List<RestoreRow> = items.map { item ->
        val signal = item.key.signal
        val title = when (signal) {
            SignalId.C1 -> "온도 설정"
            SignalId.C2 -> "풍량"
            SignalId.C3 -> "내외기"
            SignalId.L1 -> "실내 조명 색"
            SignalId.L2 -> "실내 조명 밝기"
            SignalId.L3 -> "독서등 · ${item.key.seat?.let(ClueWriter::seatName).orEmpty()}"
            SignalId.C4 -> "앞유리 제상"
            SignalId.L4 -> "전조등"
            SignalId.D1 -> "핸들"
            SignalId.B1 -> "브레이크"
        }
        val detail = if (signal == SignalId.D1) {
            if (item.restored) "가운데로 돌아왔어요" else "가운데로 돌려 주세요"
        } else {
            "원래 ${valueText(signal, item.original)} · 지금 ${valueText(signal, item.current)}"
        }
        RestoreRow(signal, title, detail, item.restored)
    }

    fun valueText(signal: SignalId, value: Any?): String {
        if (value == null) return "확인 중"
        return when (signal) {
            SignalId.C1 -> String.format(Locale.US, "%.1f℃", value as Float)
            SignalId.C2 -> "${value}단"
            SignalId.C3 -> if (value == true) "내기 순환" else "외기"
            SignalId.L1 -> "${ColorNames.describe(value as Int)}(${ColorNames.hex(value)})"
            SignalId.L2 -> value.toString()
            SignalId.L3, SignalId.C4, SignalId.L4 -> ClueWriter.switchName(value == true)
            SignalId.D1, SignalId.B1 -> value.toString()
        }
    }
}
