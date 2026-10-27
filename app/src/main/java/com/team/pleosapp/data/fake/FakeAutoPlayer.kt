package com.team.pleosapp.data.fake

import com.team.pleosapp.data.climate.FakeClimateRepository
import com.team.pleosapp.data.drive.FakeDriveInputRepository
import com.team.pleosapp.data.light.FakeCabinLightRepository
import com.team.pleosapp.domain.puzzle.NamedColor
import com.team.pleosapp.domain.puzzle.Puzzle
import com.team.pleosapp.domain.puzzle.SteeringZones
import com.team.pleosapp.domain.puzzle.Target
import com.team.pleosapp.domain.puzzle.VehicleSignals
import kotlinx.coroutines.delay

/**
 * 퍼즐을 대신 푸는 스크립트(기획 2절 F2 Fake 데이터 열: 영상 예비용). Fake SDK 빌드에서만 만든다.
 * 화면은 퍼즐이 바뀔 때마다 [SOLVE_AFTER_MS] 뒤 [solve]를, 결과 화면에서는 같은 시간 뒤 [restore]를 부른다.
 */
interface AutoPlayer {
    /** 신호를 목표값으로 바꾼다. 봉인 버튼(브레이크 미지원)은 바꿀 신호가 없어 false를 돌려준다(화면이 버튼을 대신 누른다). */
    suspend fun solve(puzzle: Puzzle): Boolean

    /** 판이 바꾼 신호를 시작 때 값으로 되돌린다(복구 체크리스트 시연). */
    suspend fun restore(snapshot: VehicleSignals)

    companion object {
        const val SOLVE_AFTER_MS = 6_000L
    }
}

class FakeAutoPlayer(
    private val climate: FakeClimateRepository,
    private val light: FakeCabinLightRepository,
    private val drive: FakeDriveInputRepository,
) : AutoPlayer {

    override suspend fun solve(puzzle: Puzzle): Boolean {
        when (val t = puzzle.target) {
            is Target.Temperature -> climate.setTargetTemperature(t.celsius.toFloat())
            is Target.FanSpeed -> climate.setFanSpeed(t.level)
            is Target.Recirculation -> climate.setRecirculation(t.on)
            is Target.AmbientColor -> light.setAmbientColor(argbOf(t.color))
            is Target.Brightness -> light.setAmbientBrightness(t.level)
            is Target.ReadingLight -> light.setReadingLight(t.seat, true)
            is Target.Steering -> {
                val range = SteeringZones.binRange(t.side, t.bin, t.boundsMin, t.boundsMax)
                drive.setSteeringAngle((range.start + range.endInclusive) / 2f)
                delay(STEERING_HOLD_MS)
                drive.setSteeringAngle(0f)
            }
            is Target.BrakePresses -> repeat(t.count) {
                drive.setBrakePressed(false)
                delay(BRAKE_STEP_MS)
                drive.setBrakePressed(true)
                delay(BRAKE_STEP_MS)
            }.also { drive.setBrakePressed(false) }
            Target.SealButton -> return false
        }
        return true
    }

    override suspend fun restore(snapshot: VehicleSignals) {
        snapshot.targetTemperature?.let(climate::setTargetTemperature)
        snapshot.fanSpeed?.let(climate::setFanSpeed)
        snapshot.recirculation?.let(climate::setRecirculation)
        snapshot.ambientColor?.let(light::setAmbientColor)
        snapshot.ambientBrightness?.let(light::setAmbientBrightness)
        snapshot.readingLights.forEach { (seat, on) -> if (on != null) light.setReadingLight(seat, on) }
        drive.setSteeringAngle(0f)
    }

    private fun argbOf(color: NamedColor): Int = when (color) {
        NamedColor.RED -> 0x00FF1744
        NamedColor.ORANGE -> 0x00FF8000
        NamedColor.YELLOW -> 0x00FFD600
        NamedColor.GREEN -> 0x0000C853
        NamedColor.BLUE -> 0x002979FF
        NamedColor.PURPLE -> 0x00AA00FF
    }

    private companion object {
        const val STEERING_HOLD_MS = 1_500L
        const val BRAKE_STEP_MS = 500L
    }
}
