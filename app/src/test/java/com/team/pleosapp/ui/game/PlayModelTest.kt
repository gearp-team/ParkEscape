package com.team.pleosapp.ui.game

import com.team.pleosapp.data.seat.SeatOccupancy
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.domain.puzzle.BrakeProgress
import com.team.pleosapp.domain.puzzle.ChainFormula
import com.team.pleosapp.domain.puzzle.CipherSymbol
import com.team.pleosapp.domain.puzzle.ClueType
import com.team.pleosapp.domain.puzzle.JudgeState
import com.team.pleosapp.domain.puzzle.NamedColor
import com.team.pleosapp.domain.puzzle.Puzzle
import com.team.pleosapp.domain.puzzle.RestoreItem
import com.team.pleosapp.domain.puzzle.SignalId
import com.team.pleosapp.domain.puzzle.SignalKey
import com.team.pleosapp.domain.puzzle.SteeringSide
import com.team.pleosapp.domain.puzzle.Target
import com.team.pleosapp.domain.puzzle.VehicleSignals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayModelTest {
    private val bounds = DisplayBounds()
    private val signals = VehicleSignals(
        targetTemperature = 24f,
        fanSpeed = 2,
        recirculation = false,
        ambientColor = 0x0000E5FF,
        ambientBrightness = 100,
        readingLights = mapOf(SeatPosition.PASSENGER to false, SeatPosition.REAR_LEFT to false, SeatPosition.REAR_RIGHT to true),
        steeringAngle = -45f,
        brakePressed = false,
    )

    private val targets: Map<SignalId, Target> = mapOf(
        SignalId.C1 to Target.Temperature(22),
        SignalId.C2 to Target.FanSpeed(4),
        SignalId.C3 to Target.Recirculation(true),
        SignalId.L1 to Target.AmbientColor(NamedColor.PURPLE),
        SignalId.L2 to Target.Brightness(40),
        SignalId.L3 to Target.ReadingLight(SeatPosition.REAR_RIGHT),
        SignalId.D1 to Target.Steering(SteeringSide.LEFT, 2, -470f, 500f),
    )

    private fun gauge(signal: SignalId, type: ClueType): Gauge {
        val puzzle = Puzzle(
            1,
            signal,
            type,
            targets.getValue(signal),
            cipherSymbol = if (type == ClueType.CIPHER) CipherSymbol.DIAMOND else null,
            chain = if (type == ClueType.CHAIN) ChainFormula(0, 1, 20) else null,
        )
        return Gauges.of(puzzle, signals, bounds, JudgeState.Waiting, null, null)
    }

    /** 리더 지시: 지시형은 목표 구간을 보여 주고, 암호형·연쇄형은 목표를 드러내지 않고 현재값만 보여 준다. */
    @Test
    fun targetShownOnlyForDirect() {
        for (signal in targets.keys) {
            assertNotNull("$signal 지시형", targetOf(gauge(signal, ClueType.DIRECT)))
            assertNull("$signal 암호형", targetOf(gauge(signal, ClueType.CIPHER)))
            val chain = gauge(signal, ClueType.CHAIN)
            if (signal in setOf(SignalId.C1, SignalId.C2, SignalId.L2)) {
                assertNull("$signal 연쇄형", targetOf(chain))
            } else {
                // 엔진은 이 신호로 연쇄형을 만들지 않는다. 식이 와도 지시형 문장으로 쓰므로 목표도 보인다
                assertNotNull("$signal 연쇄형(지시형으로 씀)", targetOf(chain))
            }
        }
        // 연쇄형을 지시형으로 바꾼 퍼즐은 지시형처럼 목표를 보인다
        val fallback = Puzzle(1, SignalId.C1, ClueType.DIRECT, Target.Temperature(22), fallbackFromChain = true)
        assertNotNull(targetOf(Gauges.of(fallback, signals, bounds, null, null, null)))
    }

    @Test
    fun gauges_showCurrentValues() {
        val c1 = gauge(SignalId.C1, ClueType.CIPHER) as Gauge.Scale
        assertEquals(24f, c1.current)
        assertEquals(16f, c1.min)
        assertEquals(30f, c1.max)
        val direct = gauge(SignalId.C1, ClueType.DIRECT) as Gauge.Scale
        assertEquals(21.5f..22.5f, direct.target)
        val l3 = gauge(SignalId.L3, ClueType.CIPHER) as Gauge.ReadingLights
        assertEquals(listOf(SeatPosition.PASSENGER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT), l3.seats.map { it.first })
        assertEquals(true, l3.seats.last().second)
        val d1 = gauge(SignalId.D1, ClueType.DIRECT) as Gauge.Steering
        assertEquals(SteeringSide.LEFT to 2, d1.target)
        assertEquals(-45f, d1.angle)
        // 현재값을 아직 모르면(null) 게이지도 모름
        val unknown = Gauges.of(Puzzle(0, SignalId.C2, ClueType.DIRECT, Target.FanSpeed(4)), null, bounds, null, null, null) as Gauge.Scale
        assertNull(unknown.current)
    }

    @Test
    fun steeringHeld_andBrakeProgress() {
        val puzzle = Puzzle(0, SignalId.D1, ClueType.CIPHER, targets.getValue(SignalId.D1), cipherSymbol = CipherSymbol.CIRCLE)
        assertTrue((Gauges.of(puzzle, signals, bounds, JudgeState.Progress(1, 1), null, null) as Gauge.Steering).held)
        assertTrue(!(Gauges.of(puzzle, signals, bounds, JudgeState.Progress(0, 1), null, 400) as Gauge.Steering).held)
        val seal = Puzzle(4, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2))
        assertEquals(Gauge.Brake(BrakeProgress(0, 2, null)), Gauges.of(seal, signals, bounds, null, null, null))
        assertEquals(Gauge.Brake(BrakeProgress(1, 2, 21_000, 5_000)), Gauges.of(seal, signals, bounds, null, BrakeProgress(1, 2, 21_000, 5_000), null))
        assertEquals(Gauge.SealButton, Gauges.of(Puzzle(4, SignalId.B1, ClueType.SEAL, Target.SealButton), signals, bounds, null, null, null))
    }

    @Test
    fun colorNames_forDisplay() {
        assertEquals("청록", ColorNames.describe(0x0000E5FF))
        assertEquals("흰색 계열", ColorNames.describe(0x00FFFFFF))
        assertEquals("보라", ColorNames.describe(0x00AA00FF))
        assertEquals("파랑", ColorNames.describe(0x002979FF))
        assertEquals("확인 중", ColorNames.describe(null))
        assertEquals("#00E5FF", ColorNames.hex(0x0000E5FF))
    }

    @Test
    fun restoreRows_text() {
        val rows = RestoreRows.of(
            listOf(
                RestoreItem(SignalKey(SignalId.C1), 24f, 22f, false),
                RestoreItem(SignalKey(SignalId.L1), 0x0000E5FF, 0x00AA00FF, false),
                RestoreItem(SignalKey(SignalId.L3, SeatPosition.REAR_RIGHT), false, false, true),
                RestoreItem(SignalKey(SignalId.D1), 0f, 0f, true),
                RestoreItem(SignalKey(SignalId.C2), 2, null, false),
            ),
        )
        assertEquals(RestoreRow(SignalId.C1, "온도 설정", "원래 24.0℃ · 지금 22.0℃", false), rows[0])
        assertEquals("원래 청록(#00E5FF) · 지금 보라(#AA00FF)", rows[1].detail)
        assertEquals("독서등 · 2열 오른쪽", rows[2].title)
        assertEquals("원래 꺼짐 · 지금 꺼짐", rows[2].detail)
        assertEquals("가운데로 돌아왔어요", rows[3].detail)
        assertEquals("원래 2단 · 지금 확인 중", rows[4].detail)
    }

    @Test
    fun demoStageCheck_listsOnlyDifferences() {
        val stage0 = VehicleSignals(targetTemperature = 24f, fanSpeed = 2, ambientColor = 0x0000E5FF)
        // 에뮬레이터: 운전석 점유(VC) → 원시값이 뒤집혀 운전석 false, 비어 있는 좌석 true, 2열 오른쪽(2단계에서 켬) false
        val emulator = SeatOccupancy(
            mapOf(SeatPosition.DRIVER to false, SeatPosition.PASSENGER to true, SeatPosition.REAR_LEFT to true, SeatPosition.REAR_RIGHT to false),
        )
        assertEquals(emptyList<DemoDiff>(), DemoStageCheck.differences(stage0, emulator))

        // 기본값 그대로(22℃·흰색·풍량 3) + VC 운전석 비움 → 극성이 반대로 잡혀 조수석·2열 왼쪽이 점유로 보인다
        val wrongSeat = SeatOccupancy(
            mapOf(SeatPosition.DRIVER to true, SeatPosition.PASSENGER to true, SeatPosition.REAR_LEFT to true, SeatPosition.REAR_RIGHT to false),
        )
        val diffs = DemoStageCheck.differences(VehicleSignals(targetTemperature = 22f, fanSpeed = 3, ambientColor = 0x00FFFFFF), wrongSeat)
        assertEquals(listOf("운전석 온도", "앰비언트 색", "풍량", "좌석 점유"), diffs.map { it.item })
        assertEquals("조수석·2열 왼쪽도 점유로 보임", diffs[3].current) // 운전석이 비었다고 단정하지 않는다(F3 QA MINOR)
        assertEquals(DemoDiff("운전석 온도", "22.0℃", "24.0℃"), diffs[0])
        assertEquals("흰색 계열(#FFFFFF)", diffs[1].current)
        // 알파는 보지 않는다
        assertTrue(DemoStageCheck.differences(stage0.copy(ambientColor = 0xFF00E5FF.toInt()), emulator).isEmpty())
        // 풍량 꺼짐(null)도 다르다
        assertEquals("풍량", DemoStageCheck.differences(stage0.copy(fanSpeed = null), emulator).single().item)
    }

    private fun targetOf(g: Gauge): Any? = when (g) {
        is Gauge.Scale -> g.target
        is Gauge.Recirculation -> g.target
        is Gauge.AmbientColor -> g.target
        is Gauge.ReadingLights -> g.target
        is Gauge.Steering -> g.target
        is Gauge.Brake -> g.progress
        Gauge.SealButton -> Unit
    }
}
