package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.signal.SeatPosition
import kotlin.math.abs

/** 테스트 공용 값: 에뮬레이터 capability와 시연 0단계 초기값. */
internal object Fixtures {
    val POOL = SignalPool.EMULATOR

    /** 시연 0단계: 운전석 24℃, 풍량 2, 외기, 앰비언트 Cyan(알파 0x00), 밝기 100, 독서등 모두 꺼짐, 핸들 중앙, 브레이크 뗌 */
    val CURRENT = VehicleSignals(
        targetTemperature = 24f,
        fanSpeed = 2,
        recirculation = false,
        ambientColor = 0x0000E5FF,
        ambientBrightness = 100,
        readingLights = mapOf(
            SeatPosition.DRIVER to false,
            SeatPosition.PASSENGER to false,
            SeatPosition.REAR_LEFT to false,
            SeatPosition.REAR_RIGHT to false,
        ),
        steeringAngle = 0f,
        brakePressed = false,
    )

    val DRIVER_AND_REAR_RIGHT = setOf(SeatPosition.DRIVER, SeatPosition.REAR_RIGHT)

    fun input(
        seed: String,
        minutes: Int = 5,
        participants: Set<SeatPosition> = DRIVER_AND_REAR_RIGHT,
        pool: SignalPool = POOL,
        current: VehicleSignals = CURRENT,
        isEv: Boolean = false,
        demoMode: Boolean = false,
    ) = GeneratorInput(seed, minutes, participants, pool, current, isEv, demoMode)

    fun board(input: GeneratorInput): Board =
        (PuzzleGenerator.generate(input) as GenerationResult.Success).board

    /** 판을 앞에서부터 풀 때 각 퍼즐 직전의 예상 현재값을 함께 돌려준다(생성기 제약 검증용). */
    fun projectedBefore(board: Board, current: VehicleSignals): List<Pair<Puzzle, VehicleSignals>> {
        var signals = current
        return board.puzzles.map { puzzle ->
            val before = signals
            signals = when (val t = puzzle.target) {
                is Target.Temperature -> signals.copy(targetTemperature = t.celsius.toFloat())
                is Target.FanSpeed -> signals.copy(fanSpeed = t.level)
                is Target.Recirculation -> signals.copy(recirculation = t.on)
                is Target.AmbientColor -> signals.copy(ambientColor = hueToArgb(t.color.hue))
                is Target.Brightness -> signals.copy(ambientBrightness = t.level)
                is Target.ReadingLight -> signals.copy(readingLights = signals.readingLights + (t.seat to true))
                is Target.Steering -> signals.copy(steeringAngle = 0f)
                else -> signals
            }
            puzzle to before
        }
    }

    /** 채도·명도 1인 색(알파 0x00). */
    fun hueToArgb(hue: Float): Int {
        val c = 1f
        val x = c * (1 - abs((hue / 60f) % 2 - 1))
        val (r, g, b) = when {
            hue < 60 -> Triple(c, x, 0f)
            hue < 120 -> Triple(x, c, 0f)
            hue < 180 -> Triple(0f, c, x)
            hue < 240 -> Triple(0f, x, c)
            hue < 300 -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
    }
}

/** 손으로 돌리는 시계. */
internal class FakeClock(var now: Long = 1_000L) : Clock {
    override fun nowMs(): Long = now

    fun advance(ms: Long) {
        now += ms
    }
}
