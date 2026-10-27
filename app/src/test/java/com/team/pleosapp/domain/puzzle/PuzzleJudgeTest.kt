package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.signal.SeatPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PuzzleJudgeTest {
    private fun judge(target: Target, signal: SignalId = SignalId.C1) = PuzzleJudge(Puzzle(0, signal, ClueType.DIRECT, target))

    // ---- C1 ±0.5℃ ----------------------------------------------------------------------------

    @Test
    fun temperature_toleranceIsHalfDegreeInclusive() {
        fun result(value: Float?) = judge(Target.Temperature(22)).update(VehicleSignals(targetTemperature = value), 0)
        assertEquals(JudgeState.Solved, result(21.5f))
        assertEquals(JudgeState.Solved, result(22.5f))
        assertEquals(JudgeState.Solved, result(22f))
        assertEquals(JudgeState.Waiting, result(21.4f))
        assertEquals(JudgeState.Waiting, result(22.6f))
        assertEquals(JudgeState.Waiting, result(null))
    }

    @Test
    fun judgesOnlyOnChange_andStaysSolved() {
        val j = judge(Target.Temperature(22))
        assertEquals(JudgeState.Waiting, j.update(VehicleSignals(targetTemperature = 24f), 0))
        assertEquals(0L, j.lastChangeAtMs)
        assertEquals(JudgeState.Waiting, j.update(VehicleSignals(targetTemperature = 24f), 5_000))
        assertEquals(0L, j.lastChangeAtMs) // 같은 값은 판정하지 않는다
        assertEquals(JudgeState.Solved, j.update(VehicleSignals(targetTemperature = 22f), 6_000))
        assertEquals(JudgeState.Solved, j.update(VehicleSignals(targetTemperature = 26f), 7_000))
    }

    // ---- 일치 판정 ---------------------------------------------------------------------------

    @Test
    fun fanRecirculationBrightnessReadingLight_exactMatch() {
        assertEquals(JudgeState.Solved, judge(Target.FanSpeed(4), SignalId.C2).update(VehicleSignals(fanSpeed = 4), 0))
        assertEquals(JudgeState.Waiting, judge(Target.FanSpeed(4), SignalId.C2).update(VehicleSignals(fanSpeed = null), 0))
        assertEquals(JudgeState.Solved, judge(Target.Recirculation(true), SignalId.C3).update(VehicleSignals(recirculation = true), 0))
        assertEquals(JudgeState.Waiting, judge(Target.Recirculation(true), SignalId.C3).update(VehicleSignals(recirculation = null), 0))
        assertEquals(JudgeState.Solved, judge(Target.Brightness(100), SignalId.L2).update(VehicleSignals(ambientBrightness = 100), 0))
        assertEquals(JudgeState.Waiting, judge(Target.Brightness(100), SignalId.L2).update(VehicleSignals(ambientBrightness = 90), 0))

        val reading = judge(Target.ReadingLight(SeatPosition.REAR_RIGHT), SignalId.L3)
        assertEquals(JudgeState.Waiting, reading.update(VehicleSignals(readingLights = mapOf(SeatPosition.REAR_LEFT to true)), 0))
        assertEquals(JudgeState.Solved, reading.update(VehicleSignals(readingLights = mapOf(SeatPosition.REAR_RIGHT to true)), 1))
    }

    @Test
    fun ambientColor_usesHueAndSaturationIgnoringAlpha() {
        fun result(color: NamedColor, argb: Int?) = judge(Target.AmbientColor(color), SignalId.L1).update(VehicleSignals(ambientColor = argb), 0)
        assertEquals(JudgeState.Solved, result(NamedColor.PURPLE, 0x00AA00FF)) // VC Purple, 알파 0x00
        assertEquals(JudgeState.Solved, result(NamedColor.PURPLE, 0xFFAA00FF.toInt()))
        assertEquals(JudgeState.Solved, result(NamedColor.BLUE, 0x002979FF)) // VC Blue h 217.6 (차 22.4°)
        assertEquals(JudgeState.Waiting, result(NamedColor.BLUE, 0x0000E5FF)) // Cyan h 186.1 (차 53.9°)
        assertEquals(JudgeState.Waiting, result(NamedColor.RED, 0x00FFFFFF)) // 흰색: 채도 0
        assertEquals(JudgeState.Waiting, result(NamedColor.RED, 0x00FFB3B3)) // 연분홍: 채도 0.3 미만
        assertEquals(JudgeState.Solved, result(NamedColor.RED, 0x00FFB2B2)) // 채도 0.30
        assertEquals(JudgeState.Waiting, result(NamedColor.PURPLE, null))
    }

    // ---- D1 칸·유지·중앙 복귀 -----------------------------------------------------------------

    private fun steeringJudge(side: SteeringSide = SteeringSide.LEFT, bin: Int = 2) =
        judge(Target.Steering(side, bin, -470f, 500f), SignalId.D1)

    private fun PuzzleJudge.angle(value: Float?, at: Long) = update(VehicleSignals(steeringAngle = value), at)

    @Test
    fun steering_binEdges() {
        assertEquals(null, SteeringZones.binOf(-23.5f, -470f, 500f)) // 5.0% = 중앙
        assertEquals(SteeringSide.LEFT to 1, SteeringZones.binOf(-23.6f, -470f, 500f))
        assertEquals(SteeringSide.LEFT to 1, SteeringZones.binOf(-39.0f, -470f, 500f)) // 8.298%
        assertEquals(SteeringSide.LEFT to 2, SteeringZones.binOf(-39.2f, -470f, 500f)) // 8.340%
        assertEquals(SteeringSide.LEFT to 2, SteeringZones.binOf(-54.8f, -470f, 500f)) // 11.66%
        assertEquals(SteeringSide.LEFT to 3, SteeringZones.binOf(-55.0f, -470f, 500f)) // 11.70%
        assertEquals(SteeringSide.LEFT to 3, SteeringZones.binOf(-70.5f, -470f, 500f)) // 15.0%
        assertEquals(null, SteeringZones.binOf(-70.6f, -470f, 500f))
        assertEquals(SteeringSide.RIGHT to 2, SteeringZones.binOf(45f, -470f, 500f)) // 오른쪽은 max 500 기준 9%
        assertEquals(null, SteeringZones.binOf(-600f, -470f, 500f)) // 범위로 잘라 100%
        assertEquals(true, SteeringZones.isCentered(25f, -470f, 500f)) // 오른쪽 5% = 25
        assertEquals(false, SteeringZones.isCentered(25.1f, -470f, 500f))
        val left2 = SteeringZones.binRange(SteeringSide.LEFT, 2, -470f, 500f) // 기획 5-1: 왼쪽 2번 칸 = SDK −39~−55
        assertEquals(-54.83f, left2.start, 0.01f)
        assertEquals(-39.17f, left2.endInclusive, 0.01f)
    }

    @Test
    fun steering_needsOneSecondInBinThenCenter_thenReread() {
        val j = steeringJudge()
        assertEquals(JudgeState.Progress(0, 1), j.angle(-45f, 0))
        assertEquals(JudgeState.Progress(0, 1), j.angle(-45f, 3_100)) // 3.1초 반복 콜백(같은 값)은 무시
        assertEquals(JudgeState.NeedsSteeringCheck, j.angle(0f, 3_200))
        // 다시 읽은 값이 아직 칸에 있으면(낡은 콜백) 확정하지 않는다
        assertEquals(JudgeState.Progress(1, 1), j.confirmSteering(-45f))
        assertEquals(JudgeState.Progress(1, 1), j.angle(-0.0f, 3_300)) // −0.0 = 0.0: 바뀌지 않았으니 판정하지 않음
        assertEquals(JudgeState.NeedsSteeringCheck, j.angle(5f, 3_400)) // 오른쪽 1%: 중앙
        assertEquals(JudgeState.Solved, j.confirmSteering(-0.0f))
    }

    @Test
    fun steering_holdBoundaryIsOneSecond() {
        val short = steeringJudge()
        short.angle(-45f, 0)
        assertEquals(JudgeState.Progress(0, 1), short.angle(0f, 999))

        val exact = steeringJudge()
        exact.angle(-45f, 0)
        assertEquals(JudgeState.NeedsSteeringCheck, exact.angle(0f, 1_000))
    }

    @Test
    fun steering_wrongSideOrBin_andNullResetHold() {
        val wrongSide = steeringJudge()
        wrongSide.angle(45f, 0) // 오른쪽 2번
        assertEquals(JudgeState.Progress(0, 1), wrongSide.angle(0f, 2_000))

        val nullBreak = steeringJudge()
        nullBreak.angle(-45f, 0)
        nullBreak.angle(null, 500) // 칸 밖으로 본다(500ms만 머묾)
        nullBreak.angle(-45f, 600)
        assertEquals(JudgeState.Progress(0, 1), nullBreak.angle(0f, 1_200))

        // 칸을 1초 넘게 유지한 뒤 다른 칸을 지나 중앙으로 와도 된다
        val passThrough = steeringJudge()
        passThrough.angle(-45f, 0)
        passThrough.angle(-30f, 1_500) // 1번 칸을 지나며 유지 확정
        assertEquals(JudgeState.NeedsSteeringCheck, passThrough.angle(-10f, 1_700))
        assertNull(passThrough.steeringHoldMs(1_800))
    }

    // ---- B1 횟수·간격·창 --------------------------------------------------------------------

    /** 브레이크를 뗀 상태(false)를 기준으로 받은 판정기. */
    private fun brakeJudge(count: Int) = PuzzleJudge(Puzzle(0, SignalId.B1, ClueType.SEAL, Target.BrakePresses(count))).also {
        it.update(VehicleSignals(brakePressed = false), -1_000)
    }

    private fun PuzzleJudge.press(at: Long, releaseAfter: Long = 1_000): JudgeState {
        update(VehicleSignals(brakePressed = true), at)
        return update(VehicleSignals(brakePressed = false), at + releaseAfter)
    }

    @Test
    fun brake_twoPresses() {
        val j = brakeJudge(2)
        assertEquals(JudgeState.Progress(0, 2), j.update(VehicleSignals(brakePressed = false), 0))
        assertEquals(JudgeState.Progress(1, 2), j.update(VehicleSignals(brakePressed = true), 100))
        assertEquals(JudgeState.Progress(1, 2), j.update(VehicleSignals(brakePressed = true), 2_000)) // 같은 값
        assertEquals(JudgeState.Progress(1, 2), j.update(VehicleSignals(brakePressed = false), 2_100))
        assertEquals(JudgeState.Solved, j.update(VehicleSignals(brakePressed = true), 4_200)) // vc.sh 실측 간격 약 4.2초
    }

    @Test
    fun brake_gapOfEightSecondsIsInclusive() {
        val ok = brakeJudge(2)
        ok.press(0)
        assertEquals(JudgeState.Solved, ok.update(VehicleSignals(brakePressed = true), 8_000))

        val late = brakeJudge(2)
        late.press(0)
        assertEquals(JudgeState.Progress(1, 2), late.update(VehicleSignals(brakePressed = true), 8_001)) // 다시 1부터
        late.update(VehicleSignals(brakePressed = false), 9_000)
        assertEquals(JudgeState.Solved, late.update(VehicleSignals(brakePressed = true), 10_000))
    }

    @Test
    fun brake_windowOfThirtySecondsIsInclusive() {
        val ok = brakeJudge(5)
        listOf(0L, 7_500L, 15_000L, 22_500L).forEach { ok.press(it) }
        assertEquals(JudgeState.Solved, ok.update(VehicleSignals(brakePressed = true), 30_000))

        val over = brakeJudge(5)
        listOf(0L, 8_000L, 16_000L, 24_000L).forEach { over.press(it) }
        assertEquals(JudgeState.Progress(1, 5), over.update(VehicleSignals(brakePressed = true), 32_000))
    }

    @Test
    fun brake_alreadyPressedAtStart_isNotAPress() {
        // 밟은 채 봉인 퍼즐이 시작되면 그것은 false→true 전환이 아니다(QA F2-engine 실패 1)
        val j = PuzzleJudge(Puzzle(0, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)))
        assertEquals(JudgeState.Progress(0, 2), j.update(VehicleSignals(brakePressed = true), 0))
        assertEquals(JudgeState.Progress(0, 2), j.update(VehicleSignals(brakePressed = false), 1_000))
        assertEquals(JudgeState.Progress(1, 2), j.update(VehicleSignals(brakePressed = true), 2_000))
        j.update(VehicleSignals(brakePressed = false), 3_000)
        assertEquals(JudgeState.Solved, j.update(VehicleSignals(brakePressed = true), 4_000))
    }

    @Test
    fun brake_releaseBoundaryCountsAt150ms() {
        val j = brakeJudge(2)
        j.update(VehicleSignals(brakePressed = true), 0)
        j.update(VehicleSignals(brakePressed = false), 50)
        assertEquals(JudgeState.Solved, j.update(VehicleSignals(brakePressed = true), 200)) // 150ms 뗌은 센다
    }

    @Test
    fun brake_releaseUnder150msIsIgnored() {
        val j = brakeJudge(2)
        j.update(VehicleSignals(brakePressed = true), 0)
        j.update(VehicleSignals(brakePressed = false), 50)
        assertEquals(JudgeState.Progress(1, 2), j.update(VehicleSignals(brakePressed = true), 199)) // 149ms 뗌 = 떨림
        j.update(VehicleSignals(brakePressed = false), 400)
        assertEquals(JudgeState.Progress(1, 2), j.update(VehicleSignals(brakePressed = true), 549))
        j.update(VehicleSignals(brakePressed = false), 600)
        assertEquals(JudgeState.Solved, j.update(VehicleSignals(brakePressed = true), 750))
    }

    @Test
    fun brake_staleReleaseOnResume_isNotAPress() {
        // F2-engine 알려진 문제: 밟은 채 재개하면 리스너가 낡은 false를 주고 0~1ms 뒤 true를 준다(에뮬레이터 실측)
        val j = PuzzleJudge(Puzzle(0, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)))
        assertEquals(JudgeState.Progress(0, 2), j.update(VehicleSignals(brakePressed = true), 0)) // readBrakePressed()
        assertEquals(JudgeState.Progress(0, 2), j.update(VehicleSignals(brakePressed = false), 5)) // 낡은 뗌
        assertEquals(JudgeState.Progress(0, 2), j.update(VehicleSignals(brakePressed = true), 6)) // 1ms 뒤 밟음: 세지 않는다
        assertEquals(JudgeState.Progress(0, 2), j.update(VehicleSignals(brakePressed = false), 2_000))
        assertEquals(JudgeState.Progress(1, 2), j.update(VehicleSignals(brakePressed = true), 4_000))
        j.update(VehicleSignals(brakePressed = false), 5_000)
        assertEquals(JudgeState.Solved, j.update(VehicleSignals(brakePressed = true), 6_000))
    }

    @Test
    fun brakeProgress_showsWindowAndResetsWhenExpired() {
        val j = brakeJudge(3)
        assertEquals(BrakeProgress(0, 3, null), j.brakeProgress(0))
        j.update(VehicleSignals(brakePressed = true), 1_000)
        assertEquals(BrakeProgress(1, 3, 30_000 - 4_000, 8_000 - 4_000), j.brakeProgress(5_000))
        assertEquals(BrakeProgress(0, 3, null), j.brakeProgress(9_001)) // 8초 넘게 안 밟음: 다음 누름은 1부터
        j.update(VehicleSignals(brakePressed = false), 2_000)
        j.update(VehicleSignals(brakePressed = true), 4_000)
        assertEquals(BrakeProgress(2, 3, 30_000 - 5_000, 8_000 - 2_000), j.brakeProgress(6_000))
        assertNull(PuzzleJudge(Puzzle(0, SignalId.C2, ClueType.DIRECT, Target.FanSpeed(3))).brakeProgress(0))
    }

    @Test
    fun brake_nullIsUnknownNotReleased() {
        // 상태를 모르는 null은 "뗌"이 아니다(리더 결정, QA 2회차 실패 1): null → true는 누름이 아니다
        val fromNull = PuzzleJudge(Puzzle(0, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)))
        assertEquals(JudgeState.Progress(0, 2), fromNull.update(VehicleSignals(brakePressed = null), 0))
        assertEquals(JudgeState.Progress(0, 2), fromNull.update(VehicleSignals(brakePressed = true), 100))
        fromNull.update(VehicleSignals(brakePressed = false), 1_000)
        assertEquals(JudgeState.Progress(1, 2), fromNull.update(VehicleSignals(brakePressed = true), 2_000))

        // 밟은 채 null이 끼어도 뗀 적이 없으면 세지 않는다
        val heldThroughNull = brakeJudge(2)
        heldThroughNull.update(VehicleSignals(brakePressed = true), 0) // 1회
        heldThroughNull.update(VehicleSignals(brakePressed = null), 1_000)
        assertEquals(JudgeState.Progress(1, 2), heldThroughNull.update(VehicleSignals(brakePressed = true), 2_000))

        // 뗀 것을 본 뒤라면 null을 지나 밟아도 1회다
        val releasedThenNull = brakeJudge(2)
        releasedThenNull.update(VehicleSignals(brakePressed = null), 0)
        assertEquals(JudgeState.Progress(1, 2), releasedThenNull.update(VehicleSignals(brakePressed = true), 500))
    }

    @Test
    fun sealButton_solvesOnlyByButton() {
        val j = PuzzleJudge(Puzzle(0, SignalId.B1, ClueType.SEAL, Target.SealButton))
        assertEquals(JudgeState.Waiting, j.update(VehicleSignals(brakePressed = true), 0))
        assertEquals(JudgeState.Solved, j.pressSealButton())
    }
}
