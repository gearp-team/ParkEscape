package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.domain.puzzle.Fixtures.CURRENT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSessionTest {
    private val clock = FakeClock()

    /** ① C1 지시형 22℃ → ② D1 왼쪽 2번 → ③ L1 보라 → ④ L3 2열 오른쪽 → ⑤ B1 두 번 (5분 판) */
    private val board = Board(
        seed = "test",
        lengthMinutes = 5,
        puzzles = listOf(
            Puzzle(0, SignalId.C1, ClueType.DIRECT, Target.Temperature(22)),
            Puzzle(1, SignalId.D1, ClueType.DIRECT, Target.Steering(SteeringSide.LEFT, 2, -470f, 500f)),
            Puzzle(2, SignalId.L1, ClueType.DIRECT, Target.AmbientColor(NamedColor.PURPLE)),
            Puzzle(3, SignalId.L3, ClueType.DIRECT, Target.ReadingLight(SeatPosition.REAR_RIGHT)),
            Puzzle(4, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)),
        ),
        cipherTable = emptyList(),
    )

    private fun newSession() = GameSession.start(board, Fixtures.DRIVER_AND_REAR_RIGHT, CURRENT, clock)

    private fun GameSession.playing(): GameSession = also {
        assertEquals(SessionEvent.Resumed, confirmParked(gateOpen = true))
    }

    // ---- 전이 ---------------------------------------------------------------------------------

    @Test
    fun start_requiresParkConfirmWithOpenGate() {
        val s = newSession()
        assertEquals(SessionStatus.PREPARING, s.status)
        assertEquals(SessionEvent.None, s.confirmParked(gateOpen = false))
        assertEquals(SessionStatus.PREPARING, s.status)
        assertEquals(SessionEvent.Resumed, s.confirmParked(gateOpen = true))
        assertEquals(SessionStatus.PLAYING, s.status)
    }

    @Test
    fun pausedByGate_resumesOnlyWithParkConfirm_andTimerStops() {
        val s = newSession().playing()
        clock.advance(10_000)
        assertEquals(SessionEvent.Paused(SessionStatus.PAUSED_BY_GATE), s.onGateClosed())
        assertEquals(10_000L, s.elapsedMs())

        clock.advance(60_000) // 주행 중: 타이머 정지
        assertEquals(10_000L, s.elapsedMs())
        // 게이트가 다시 열려도(신호가 와도) 저절로 재개하지 않는다
        assertEquals(SessionEvent.None, s.onSignals(CURRENT.copy(targetTemperature = 22f)))
        assertEquals(SessionStatus.PAUSED_BY_GATE, s.status)
        assertEquals(0, s.state.currentIndex)
        assertEquals(SessionEvent.None, s.confirmParked(gateOpen = false))
        assertEquals(SessionStatus.PAUSED_BY_GATE, s.status)

        assertEquals(SessionEvent.Resumed, s.confirmParked(gateOpen = true))
        clock.advance(5_000)
        assertEquals(15_000L, s.elapsedMs())
        assertEquals(1, s.state.gatePauses)
    }

    @Test
    fun pausedByApp_alsoNeedsParkConfirm() {
        val s = newSession().playing()
        clock.advance(3_000)
        assertEquals(SessionEvent.Paused(SessionStatus.PAUSED_BY_APP), s.onAppStopped())
        clock.advance(20_000)
        assertEquals(3_000L, s.elapsedMs())
        assertEquals(SessionEvent.None, s.onSignals(CURRENT.copy(targetTemperature = 22f)))
        assertEquals(SessionStatus.PAUSED_BY_APP, s.status)
        assertEquals(SessionEvent.None, s.confirmParked(gateOpen = false))

        s.confirmParked(gateOpen = true)
        // 재개 뒤 get*으로 다시 읽은 값(떠나 있는 동안 바뀐 값)이 판정된다
        assertEquals(SessionEvent.PuzzleSolved(0), s.onSignals(CURRENT.copy(targetTemperature = 22f)))
        assertEquals(1, s.state.appPauses)
    }

    @Test
    fun gateClosedWhilePreparing_doesNothing() {
        val s = newSession()
        assertEquals(SessionEvent.None, s.onGateClosed())
        assertEquals(SessionStatus.PREPARING, s.status)
    }

    // ---- 풀기·탈출·시간 초과 -------------------------------------------------------------------

    @Test
    fun fullBoard_escapesWithElapsedTime() {
        val s = newSession().playing()
        var signals = CURRENT
        fun send(change: (VehicleSignals) -> VehicleSignals): SessionEvent {
            signals = change(signals)
            return s.onSignals(signals)
        }

        clock.advance(10_000)
        assertEquals(SessionEvent.PuzzleSolved(0), send { it.copy(targetTemperature = 22f) })

        clock.advance(2_000)
        send { it.copy(steeringAngle = -45f) }
        clock.advance(1_500)
        assertEquals(SessionEvent.JudgeChanged(1, JudgeState.NeedsSteeringCheck), send { it.copy(steeringAngle = 0f) })
        assertEquals(SessionEvent.PuzzleSolved(1), s.confirmSteering(0f)) // readSteeringAngle() 재확인

        clock.advance(5_000)
        assertEquals(SessionEvent.PuzzleSolved(2), send { it.copy(ambientColor = 0x00AA00FF) })
        clock.advance(5_000)
        assertEquals(SessionEvent.PuzzleSolved(3), send { it.copy(readingLights = it.readingLights + (SeatPosition.REAR_RIGHT to true)) })

        clock.advance(1_000)
        assertEquals(SessionEvent.JudgeChanged(4, JudgeState.Progress(1, 2)), send { it.copy(brakePressed = true) })
        clock.advance(1_000)
        send { it.copy(brakePressed = false) }
        clock.advance(2_000)
        assertEquals(SessionEvent.Escaped(27_500), send { it.copy(brakePressed = true) })
        assertEquals(SessionStatus.ESCAPED, s.status)
        clock.advance(10_000)
        assertEquals(27_500L, s.elapsedMs()) // 끝나면 타이머가 멈춘다
        assertTrue(s.isRecordEligible)
    }

    @Test
    fun valueAlreadySetBeforePuzzleStarts_countsWhenItBecomesCurrent() {
        val s = newSession().playing()
        // 앰비언트를 미리 보라로 맞춰 둔다(③의 답). ①·②를 풀면 ③은 마지막으로 받은 신호로 바로 통과한다.
        val purple = CURRENT.copy(ambientColor = 0x00AA00FF)
        s.onSignals(purple)
        assertEquals(SessionEvent.PuzzleSolved(0), s.onSignals(purple.copy(targetTemperature = 22f)))
        s.onSignals(purple.copy(targetTemperature = 22f, steeringAngle = -45f))
        clock.advance(1_000)
        s.onSignals(purple.copy(targetTemperature = 22f, steeringAngle = 0f))
        s.confirmSteering(0f)
        assertEquals(3, s.state.currentIndex)
    }

    @Test
    fun sealStartedWithBrakeHeld_needsTwoRealPresses() {
        val twoStep = board.copy(
            puzzles = listOf(
                Puzzle(0, SignalId.C1, ClueType.DIRECT, Target.Temperature(22)),
                Puzzle(1, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)),
            ),
        )
        val s = GameSession.start(twoStep, setOf(SeatPosition.DRIVER), CURRENT, clock).playing()
        // 브레이크를 밟은 채로 ①을 푼다 → ② 봉인은 0/2에서 시작해야 한다
        assertEquals(SessionEvent.PuzzleSolved(0), s.onSignals(CURRENT.copy(targetTemperature = 22f, brakePressed = true)))
        assertEquals(JudgeState.Progress(0, 2), s.judgeState)
        clock.advance(1_000)
        s.onSignals(CURRENT.copy(targetTemperature = 22f, brakePressed = false))
        clock.advance(1_000)
        assertEquals(SessionEvent.JudgeChanged(1, JudgeState.Progress(1, 2)), s.onSignals(CURRENT.copy(targetTemperature = 22f, brakePressed = true)))
        assertEquals(SessionStatus.PLAYING, s.status)
        clock.advance(1_000)
        s.onSignals(CURRENT.copy(targetTemperature = 22f, brakePressed = false))
        clock.advance(1_000)
        assertEquals(SessionEvent.Escaped(4_000), s.onSignals(CURRENT.copy(targetTemperature = 22f, brakePressed = true)))
    }

    @Test
    fun resumeWithBrakeHeld_needsReleaseThenPress() {
        // 주행에서 돌아와 밟은 채 P → 바로 이어하기(리더 결정): readBrakePressed()로 다시 읽은 true가 시작 상태다
        val twoStep = board.copy(
            puzzles = listOf(
                Puzzle(0, SignalId.C1, ClueType.DIRECT, Target.Temperature(22)),
                Puzzle(1, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)),
            ),
        )
        val solved = CURRENT.copy(targetTemperature = 22f)
        for (resync in listOf<Boolean?>(true, null)) {
            val s = GameSession.start(twoStep, setOf(SeatPosition.DRIVER), CURRENT, clock).playing()
            assertEquals(SessionEvent.PuzzleSolved(0), s.onSignals(solved))
            s.onGateClosed()
            s.confirmParked(gateOpen = true)
            s.onSignals(solved.copy(brakePressed = resync)) // get* 재동기화(null = 읽기 실패)
            assertEquals("resync=$resync", JudgeState.Progress(0, 2), s.onSignals(solved.copy(brakePressed = true)).let { s.judgeState })
            clock.advance(1_000)
            s.onSignals(solved.copy(brakePressed = false))
            clock.advance(1_000)
            assertEquals("resync=$resync", SessionEvent.JudgeChanged(1, JudgeState.Progress(1, 2)), s.onSignals(solved.copy(brakePressed = true)))
            clock.advance(1_000)
            s.onSignals(solved.copy(brakePressed = false))
            clock.advance(1_000)
            assertTrue("resync=$resync", s.onSignals(solved.copy(brakePressed = true)) is SessionEvent.Escaped)
        }
    }

    @Test
    fun timeout_stopsAtLimit() {
        val s = newSession().playing()
        clock.advance(4 * 60_000L)
        assertEquals(SessionEvent.None, s.tick())
        clock.advance(60_000L)
        assertEquals(SessionEvent.TimedOut, s.tick())
        assertEquals(SessionStatus.TIMED_OUT, s.status)
        assertEquals(5 * 60_000L, s.elapsedMs())
        assertEquals(SessionEvent.None, s.onSignals(CURRENT.copy(targetTemperature = 22f)))
        assertFalse(s.isRecordEligible)
    }

    // ---- 건너뛰기 ------------------------------------------------------------------------------

    @Test
    fun skip_after60sWithoutChange_excludesFromRecords() {
        val s = newSession().playing()
        s.onSignals(CURRENT)
        clock.advance(59_999)
        assertFalse(s.canSkip())
        clock.advance(1)
        assertTrue(s.canSkip())
        assertEquals(SessionEvent.PuzzleSkipped(0), s.skipCurrent())
        assertEquals(setOf(0), s.state.skippedIndices)
        assertEquals(1, s.state.currentIndex)

        // 리스너 실패면 바로 건너뛸 수 있다
        assertFalse(s.canSkip())
        s.reportSignalFailure()
        assertTrue(s.canSkip())
    }

    @Test
    fun sealPuzzle_cannotBeSkipped() {
        val sealOnly = board.copy(puzzles = listOf(Puzzle(0, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2))))
        val s = GameSession.start(sealOnly, setOf(SeatPosition.DRIVER), CURRENT, clock).playing()
        clock.advance(120_000)
        assertFalse(s.canSkip())
    }

    // ---- 저장·복원 -----------------------------------------------------------------------------

    @Test
    fun restoreFromPlaying_becomesPausedByAppAndKeepsProgress() {
        val s = newSession().playing()
        clock.advance(7_000)
        s.onSignals(CURRENT.copy(targetTemperature = 22f))
        val saved = s.toSaved()
        assertEquals(SessionStatus.PLAYING, saved.status)
        assertEquals(7_000L, saved.elapsedMs)

        val restored = GameSession.restore(saved, clock)
        assertEquals(SessionStatus.PAUSED_BY_APP, restored.status)
        assertEquals(1, restored.state.currentIndex)
        clock.advance(30_000)
        assertEquals(7_000L, restored.elapsedMs())
        assertEquals(SessionEvent.Resumed, restored.confirmParked(gateOpen = true))
        clock.advance(1_000)
        assertEquals(8_000L, restored.elapsedMs())
    }

    // ---- 복구 체크리스트 ----------------------------------------------------------------------

    @Test
    fun restoreChecklist_autoChecksWhenSignalsReturn() {
        val s = newSession()
        val changed = CURRENT.copy(
            targetTemperature = 22f,
            ambientColor = 0x00AA00FF,
            readingLights = CURRENT.readingLights + (SeatPosition.REAR_RIGHT to true),
            steeringAngle = -45f,
        )
        val before = s.restoreChecklist(changed).associateBy { it.key }
        assertEquals(setOf(SignalId.C1, SignalId.D1, SignalId.L1, SignalId.L3), before.keys.map { it.signal }.toSet())
        assertTrue(before.values.none { it.restored })
        assertEquals(24f, before.getValue(SignalKey(SignalId.C1)).original)
        assertEquals(22f, before.getValue(SignalKey(SignalId.C1)).current)

        val back = changed.copy(
            targetTemperature = 24.2f, // 0.25℃ 안
            ambientColor = 0xFF00E5FF.toInt(), // 알파만 다른 같은 Cyan
            readingLights = CURRENT.readingLights,
            steeringAngle = 10f, // 오른쪽 2%: 중앙
        )
        assertTrue(s.restoreChecklist(back).all { it.restored })
        assertFalse(s.restoreChecklist(back.copy(targetTemperature = 23.5f)).single { it.key.signal == SignalId.C1 }.restored)
        assertFalse(s.restoreChecklist(back.copy(ambientColor = null)).single { it.key.signal == SignalId.L1 }.restored)
    }

    @Test
    fun restoreChecklist_skipsUnknownOriginal() {
        val s = GameSession.start(board, Fixtures.DRIVER_AND_REAR_RIGHT, CURRENT.copy(targetTemperature = null), clock)
        assertTrue(s.restoreChecklist(CURRENT).none { it.key.signal == SignalId.C1 })
    }
}
