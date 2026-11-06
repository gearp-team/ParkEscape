package com.team.pleosapp.ui.game

import com.team.pleosapp.data.seat.SeatOccupancy
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.domain.clue.ClueWriter
import com.team.pleosapp.domain.puzzle.ColorRule
import com.team.pleosapp.domain.puzzle.VehicleSignals
import java.util.Locale
import kotlin.math.abs

/** 시연 0단계와 다른 항목 하나. */
data class DemoDiff(val item: String, val current: String, val expected: String)

/**
 * debug 시연 모드(시드 고정)로 시작할 때 시연 대본 5-1의 0단계와 다른 현재값을 찾는다(F2-ui 리더 결정).
 * 대상: 운전석 온도 24℃, 앰비언트 Cyan(#00E5FF, RGB만), 풍량 2단, 앞유리 제상 꺼짐, 전조등 꺼짐,
 * 좌석 점유(운전석만, 2단계에서 2열 오른쪽).
 *
 * 운전석 점유는 직접 읽을 수 없다(데이터 계층이 운전석을 기준으로 극성을 보정하므로 보정값의 운전석은 늘 점유다).
 * 조수석·2열 왼쪽이 점유로 보이면 다르다고 본다: VC에서 운전석을 비워 극성이 반대로 잡힌 경우(에뮬레이터 실측)와
 * 실제로 그 자리를 점유로 켜 둔 경우가 모두 이렇게 보이므로, 어느 쪽인지 단정하지 않고 보이는 그대로 말한다.
 */
object DemoStageCheck {
    const val TEMPERATURE = 24f
    const val AMBIENT_CYAN = 0x00E5FF
    const val FAN_SPEED = 2

    fun differences(signals: VehicleSignals, occupancy: SeatOccupancy): List<DemoDiff> = buildList {
        val t = signals.targetTemperature
        if (t == null || abs(t - TEMPERATURE) > 0.25f) {
            add(DemoDiff("운전석 온도", t?.let { String.format(Locale.US, "%.1f℃", it) } ?: "확인 안 됨", "24.0℃"))
        }
        val color = signals.ambientColor
        if (color == null || !ColorRule.sameRgb(color, AMBIENT_CYAN)) {
            add(DemoDiff("앰비언트 색", color?.let { "${ColorNames.describe(it)}(${ColorNames.hex(it)})" } ?: "확인 안 됨", "청록(#00E5FF)"))
        }
        val fan = signals.fanSpeed
        if (fan != FAN_SPEED) add(DemoDiff("풍량", fan?.let { "${it}단" } ?: "확인 안 됨(꺼짐 포함)", "2단"))
        if (signals.defroster != false) add(DemoDiff("앞유리 제상", switchText(signals.defroster), "꺼짐"))
        if (signals.lowBeam != false) add(DemoDiff("전조등", switchText(signals.lowBeam), "꺼짐(VC Off)"))
        val corrected = occupancy.corrected
        val extraSeats = listOf(SeatPosition.PASSENGER, SeatPosition.REAR_LEFT).filter { corrected[it] == true }
        if (!occupancy.polarityKnown || extraSeats.isNotEmpty()) {
            val now = if (!occupancy.polarityKnown) {
                "운전석 값을 읽지 못함"
            } else {
                "${extraSeats.joinToString("·") { ClueWriter.seatName(it) }}도 점유로 보임"
            }
            add(DemoDiff("좌석 점유", now, "운전석만(VC 운전석 점유, 나머지 비움)"))
        }
    }

    private fun switchText(on: Boolean?): String = on?.let(ClueWriter::switchName) ?: "확인 안 됨"
}
