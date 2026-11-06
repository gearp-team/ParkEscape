package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.ValueBounds
import com.team.pleosapp.domain.clue.ClueWriter
import com.team.pleosapp.domain.clue.SafetyTips
import com.team.pleosapp.domain.puzzle.Fixtures.CURRENT
import com.team.pleosapp.domain.puzzle.Fixtures.POOL
import com.team.pleosapp.domain.puzzle.Fixtures.board
import com.team.pleosapp.domain.puzzle.Fixtures.input
import com.team.pleosapp.ui.game.RestoreRows
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** C4 앞유리 제상·L4 전조등(2026-11-06 추가)과 "실제 상황 팁": 생성기 제약(C3·C4 같은 판 금지 포함), 판정, 원상 복구, 팁 표시 조건, 시연 시드 demo-safety. */
class SafetySignalsTest {
    private val seeds = (1..200).map { "seed-$it" }
    private val driverOnly = setOf(SeatPosition.DRIVER)

    // ---- 생성기 제약 ----------------------------------------------------------------------------

    @Test
    fun c4L4_onlyTargetOn_atMostOnce_neverChain() {
        var c4 = 0
        var l4 = 0
        for (minutes in listOf(3, 5, 10)) {
            for (seed in seeds) {
                val b = board(input(seed, minutes, participants = driverOnly))
                for (signal in listOf(SignalId.C4, SignalId.L4)) {
                    val puzzles = b.puzzles.filter { it.signal == signal }
                    assertTrue("$minutes $seed $signal", puzzles.size <= 1)
                    puzzles.forEach { p ->
                        assertTrue("$seed $p", p.type == ClueType.DIRECT || p.type == ClueType.CIPHER)
                        assertFalse("$seed $p", p.fallbackFromChain)
                        when (val t = p.target) {
                            is Target.Defroster -> assertTrue(t.on)
                            is Target.LowBeam -> assertTrue(t.on)
                            else -> error("unexpected target $t")
                        }
                    }
                }
                c4 += b.puzzles.count { it.signal == SignalId.C4 }
                l4 += b.puzzles.count { it.signal == SignalId.L4 }
            }
        }
        assertTrue("C4가 실제로 나온다: $c4", c4 > 50)
        assertTrue("L4가 실제로 나온다: $l4", l4 > 50)
    }

    @Test
    fun alreadyOnOrUnknown_excludesSignalFromBoard() {
        for ((label, current) in listOf(
            "on" to CURRENT.copy(defroster = true, lowBeam = true),
            "unknown" to CURRENT.copy(defroster = null, lowBeam = null),
        )) {
            for (seed in seeds) {
                val b = board(input(seed, 10, participants = driverOnly, current = current))
                assertTrue("$label $seed", b.puzzles.none { it.signal == SignalId.C4 || it.signal == SignalId.L4 })
            }
        }
        // 지원하지 않으면(풀에 없으면) 꺼져 있어도 내지 않는다
        val noSafety = POOL.copy(defroster = false, lowBeam = false)
        for (seed in seeds) {
            val b = board(input(seed, 10, participants = driverOnly, pool = noSafety))
            assertTrue(seed, b.puzzles.none { it.signal == SignalId.C4 || it.signal == SignalId.L4 })
        }
    }

    @Test
    fun nonEv_climateCapIncludesC4() {
        for (seed in seeds) {
            val b = board(input(seed, 10, participants = driverOnly))
            assertTrue(seed, b.puzzles.count { it.signal.isClimate } <= PuzzleGenerator.NON_EV_CLIMATE_MAX)
        }
        // C1(2) + C4(1): 비EV는 공조 합계 2 + 봉인 = 3 → 3분 판만. EV도 3 + 봉인 = 4 → 3분 판만
        val pool = SignalPool(temperature = ValueBounds(15f, 30f), defroster = true, brake = true)
        assertEquals(setOf(BoardLength.THREE), PuzzleGenerator.availableLengths(driverOnly, pool, CURRENT))
        assertEquals(setOf(BoardLength.THREE), PuzzleGenerator.availableLengths(driverOnly, pool, CURRENT, isEv = true))
    }

    @Test
    fun c3AndC4_neverOnSameBoard() {
        val participants = listOf(driverOnly, Fixtures.DRIVER_AND_REAR_RIGHT, SeatPosition.entries.toSet())
        var withC3 = 0
        var withC4 = 0
        for (isEv in listOf(false, true)) {
            for (people in participants) {
                for (minutes in PuzzleGenerator.availableLengths(people, POOL, CURRENT, isEv).map { it.minutes }) {
                    for (seed in seeds) {
                        val signals = board(input(seed, minutes, participants = people, isEv = isEv)).puzzles.map { it.signal }.toSet()
                        assertFalse("$isEv $people $minutes $seed $signals", SignalId.C3 in signals && SignalId.C4 in signals)
                        if (SignalId.C3 in signals) withC3++
                        if (SignalId.C4 in signals) withC4++
                    }
                }
            }
        }
        // 한쪽이 늘 이기지 않는다(판마다 한쪽을 고른다)
        assertTrue("C3 판 $withC3", withC3 > 300)
        assertTrue("C4 판 $withC4", withC4 > 300)
    }

    @Test
    fun c3c4Conflict_countsBetterSideForAvailability() {
        // C3·C4만 있으면 한 판에 한 종류뿐이라 "서로 다른 신호 2종" 조건에 걸린다
        val onlyC3C4 = SignalPool(recirculation = true, defroster = true, brake = true)
        assertEquals(emptySet<BoardLength>(), PuzzleGenerator.availableLengths(driverOnly, onlyC3C4, CURRENT, isEv = true))
        // C3·C4 + L4: (C3 2 + L4 1) 쪽이 (C4 1 + L4 1)보다 많다 → 3 + 봉인 = 4 → 3분 판만, 생성은 두 쪽 모두 가능
        val withLowBeam = SignalPool(recirculation = true, defroster = true, lowBeam = true, brake = true)
        assertEquals(4, PuzzleGenerator.maxPuzzleCount(driverOnly, withLowBeam, CURRENT, isEv = true))
        val boards = seeds.map { board(input(it, 3, participants = driverOnly, pool = withLowBeam)) }
        boards.forEach { b -> assertFalse(b.toString(), b.puzzles.any { it.signal == SignalId.C3 } && b.puzzles.any { it.signal == SignalId.C4 }) }
        assertTrue(boards.any { b -> b.puzzles.any { it.signal == SignalId.C3 } })
        assertTrue(boards.any { b -> b.puzzles.any { it.signal == SignalId.C4 } })
        // 전체 풀 EV: C4를 빼는 쪽(C1·C2·C3 각 2 = 6)이 크다 → 6 + L1 2 + L2 2 + L4 1 + D1 1 = 12 + 봉인 = 13
        assertEquals(13, PuzzleGenerator.maxPuzzleCount(driverOnly, POOL, CURRENT, isEv = true))
    }

    @Test
    fun c4AndL4Alone_makeThreeMinuteBoard() {
        val pool = SignalPool(defroster = true, lowBeam = true, brake = true)
        assertEquals(setOf(BoardLength.THREE), PuzzleGenerator.availableLengths(driverOnly, pool, CURRENT))
        val b = board(input("x", 3, participants = driverOnly, pool = pool))
        assertEquals(setOf(SignalId.C4, SignalId.L4), b.puzzles.dropLast(1).map { it.signal }.toSet())
        // 하나만 꺼져 있으면 서로 다른 신호 2종 조건에 걸려 시작할 수 없다
        assertEquals(emptySet<BoardLength>(), PuzzleGenerator.availableLengths(driverOnly, pool, CURRENT.copy(lowBeam = true)))
    }

    @Test
    fun cipher_switchMeaningWithOneDecoy() {
        var seen = 0
        for (seed in seeds) {
            val b = board(input(seed, 10, participants = driverOnly))
            for (p in b.puzzles.filter { it.type == ClueType.CIPHER && (it.signal == SignalId.C4 || it.signal == SignalId.L4) }) {
                assertEquals(seed, CipherMeaning.Switch(true), b.cipherTable.single { it.symbol == p.cipherSymbol }.meaning)
                assertTrue(seed, b.cipherTable.any { it.meaning == CipherMeaning.Switch(false) })
                seen++
            }
        }
        assertTrue("암호형 C4·L4가 나온다: $seen", seen > 0)
    }

    @Test
    fun availableLengths_alwaysGenerate_withSafetySignals() {
        for (isEv in listOf(false, true)) {
            for (minutes in PuzzleGenerator.availableLengths(driverOnly, POOL, CURRENT, isEv).map { it.minutes }) {
                for (seed in seeds.take(50)) {
                    val result = PuzzleGenerator.generate(input(seed, minutes, participants = driverOnly, isEv = isEv))
                    assertTrue("$isEv $minutes $seed", result is GenerationResult.Success)
                    val b = (result as GenerationResult.Success).board
                    assertTrue(b.puzzles.zipWithNext().none { (a, c) -> a.signal == c.signal })
                }
            }
        }
    }

    // ---- 시연 시드 demo-safety --------------------------------------------------------------

    @Test
    fun demoSafety_fixedBoard() {
        val b = board(input(Seeds.DEMO_SAFETY, 5, pool = SignalPool(brake = true), demoMode = true))
        assertEquals(
            listOf(
                Puzzle(0, SignalId.L4, ClueType.DIRECT, Target.LowBeam(true)),
                Puzzle(1, SignalId.D1, ClueType.DIRECT, Target.Steering(SteeringSide.LEFT, 2, -470f, 500f)),
                Puzzle(2, SignalId.L1, ClueType.CIPHER, Target.AmbientColor(NamedColor.PURPLE), cipherSymbol = CipherSymbol.DIAMOND),
                Puzzle(3, SignalId.C4, ClueType.CIPHER, Target.Defroster(true), cipherSymbol = CipherSymbol.SQUARE),
                Puzzle(4, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)),
            ),
            b.puzzles,
        )
        assertEquals(
            listOf(
                CipherEntry(CipherSymbol.DIAMOND, CipherMeaning.Color(NamedColor.PURPLE)),
                CipherEntry(CipherSymbol.SQUARE, CipherMeaning.Switch(true)),
                CipherEntry(CipherSymbol.CIRCLE, CipherMeaning.Color(NamedColor.BLUE)),
                CipherEntry(CipherSymbol.STAR, CipherMeaning.Switch(false)),
                CipherEntry(CipherSymbol.TRIANGLE, CipherMeaning.Color(NamedColor.GREEN)),
            ),
            b.cipherTable,
        )
        assertTrue(b.demo)
        assertEquals(Seeds.DEMO_SAFETY, b.seed)
        // 5분 판 구성(지시형 2 · 암호형 2 · 봉인 1), C3 없음, 비EV 공조 상한 안, 팁은 L4·C4
        assertEquals(listOf(ClueType.DIRECT, ClueType.DIRECT, ClueType.CIPHER, ClueType.CIPHER, ClueType.SEAL), b.puzzles.map { it.type })
        assertTrue(b.puzzles.none { it.signal == SignalId.C3 })
        assertEquals(1, b.puzzles.count { it.signal.isClimate })
        assertEquals(listOf(SignalId.L4, SignalId.C4), b.puzzles.filter { SafetyTips.of(it.signal) != null }.map { it.signal })
        // 기호는 겹치지 않고, 암호형 기호는 모두 표에서 찾을 수 있다
        assertEquals(b.cipherTable.size, b.cipherTable.map { it.symbol }.toSet().size)
        b.puzzles.mapNotNull { it.cipherSymbol }.forEach { sym -> assertTrue(b.cipherTable.any { it.symbol == sym }) }
        // 같은 입력이면 같은 판(현재값과 무관한 고정 판), 다른 길이는 일반 생성
        assertEquals(b, board(input(Seeds.DEMO_SAFETY, 5, current = CURRENT.copy(recirculation = true), demoMode = true)))
        assertEquals(8, board(input(Seeds.DEMO_SAFETY, 10, demoMode = true)).puzzles.size)
    }

    @Test
    fun demoSafety_clues() {
        val b = board(input(Seeds.DEMO_SAFETY, 5, demoMode = true))
        assertEquals("외부 표지등을 켜라: 전조등", ClueWriter.clue(b, 0).text)
        assertEquals("조타 키를 왼쪽 2번 구간에 맞추고 중앙으로", ClueWriter.clue(b, 1).text)
        assertEquals("비상 조명을 산소 계통 색(◇)으로", ClueWriter.clue(b, 2).text)
        assertEquals(ClueWriter.SETTINGS_NOTE, ClueWriter.clue(b, 2).settingsNote)
        assertEquals("시야 확보 장치를 ■ 상태로: 앞유리 제상", ClueWriter.clue(b, 3).text)
        assertEquals("켜짐", ClueWriter.meaning(CipherMeaning.Switch(true)))
        assertEquals("시야 확보 장치 켜짐", ClueWriter.solvedLabel(b.puzzles[3]))
        assertEquals("외부 표지등 켜짐", ClueWriter.solvedLabel(b.puzzles[0]))
        val direct = Board("s", 3, listOf(Puzzle(0, SignalId.C4, ClueType.DIRECT, Target.Defroster(true)), b.puzzles[4].copy(index = 1)), emptyList())
        assertEquals("시야 확보 장치를 켜라: 앞유리 제상", ClueWriter.clue(direct, 0).text)
        assertNull(ClueWriter.clue(direct, 0).settingsNote)
    }

    // ---- 판정 ---------------------------------------------------------------------------------

    @Test
    fun judge_switchSignals() {
        val defrost = PuzzleJudge(Puzzle(0, SignalId.C4, ClueType.DIRECT, Target.Defroster(true)))
        assertEquals(JudgeState.Waiting, defrost.update(VehicleSignals(defroster = false), 0))
        assertEquals(JudgeState.Waiting, defrost.update(VehicleSignals(defroster = null), 1)) // 모르면 불일치
        assertEquals(JudgeState.Waiting, defrost.update(VehicleSignals(defroster = false, lowBeam = true), 2)) // 다른 신호는 보지 않는다
        assertEquals(JudgeState.Solved, defrost.update(VehicleSignals(defroster = true), 3))
        assertEquals(JudgeState.Solved, defrost.update(VehicleSignals(defroster = false), 4)) // 한 번 풀리면 그대로

        val beam = PuzzleJudge(Puzzle(0, SignalId.L4, ClueType.CIPHER, Target.LowBeam(true), cipherSymbol = CipherSymbol.STAR))
        assertEquals(JudgeState.Waiting, beam.update(VehicleSignals(lowBeam = false, defroster = true), 0))
        assertEquals(JudgeState.Solved, beam.update(VehicleSignals(lowBeam = true), 1))
        // 미리 켜 둔 값도 첫 값에서 통과(다른 신호와 같은 규칙)
        assertEquals(JudgeState.Solved, PuzzleJudge(Puzzle(0, SignalId.L4, ClueType.DIRECT, Target.LowBeam(true))).update(VehicleSignals(lowBeam = true), 0))
    }

    @Test
    fun session_solvesSafetyBoardAndRecordsSolvedRange() {
        val b = board(input(Seeds.DEMO_SAFETY, 5, demoMode = true))
        val clock = FakeClock()
        val s = GameSession.start(b, Fixtures.DRIVER_AND_REAR_RIGHT, CURRENT, clock)
        s.confirmParked(gateOpen = true)
        var v = CURRENT
        assertEquals(SessionEvent.None, s.onSignals(v))
        assertTrue(s.lastSolved.isEmpty())
        v = v.copy(lowBeam = true)
        assertEquals(SessionEvent.PuzzleSolved(0), s.onSignals(v))
        assertEquals(0..0, s.lastSolved)
        v = v.copy(steeringAngle = -45f)
        s.onSignals(v)
        clock.advance(1_500)
        v = v.copy(steeringAngle = 0f)
        s.onSignals(v)
        assertEquals(SessionEvent.PuzzleSolved(1), s.confirmSteering(0f))
        // 제상을 미리 켜 둔 채 앰비언트를 보라로 바꾸면 ③·④가 한 번에 풀린다
        v = v.copy(defroster = true)
        s.onSignals(v)
        v = v.copy(ambientColor = 0x00AA00FF)
        assertEquals(SessionEvent.PuzzleSolved(2), s.onSignals(v))
        assertEquals(2..3, s.lastSolved)
        assertEquals(SignalId.C4, SafetyTips.solvedTipSignal(b, s.lastSolved.first, s.lastSolved.last + 1, emptySet()))
        assertEquals(4, s.state.currentIndex)
    }

    // ---- 원상 복구 ----------------------------------------------------------------------------

    @Test
    fun restoreChecklist_switchSignalsBackToStart() {
        val b = board(input(Seeds.DEMO_SAFETY, 5, demoMode = true))
        val s = GameSession.start(b, Fixtures.DRIVER_AND_REAR_RIGHT, CURRENT, FakeClock())
        val after = CURRENT.copy(lowBeam = true, defroster = true, ambientColor = 0x00AA00FF)
        val items = s.restoreChecklist(after)
        assertEquals(listOf(SignalId.L4, SignalId.D1, SignalId.L1, SignalId.C4), items.map { it.key.signal })
        assertEquals(listOf(false, true, false, false), items.map { it.restored })
        val restored = s.restoreChecklist(CURRENT)
        assertTrue(restored.all { it.restored })
        // 값을 모르면 체크하지 않는다, 시작 값을 모르면 줄을 만들지 않는다
        assertFalse(s.restoreChecklist(CURRENT.copy(lowBeam = null)).single { it.key.signal == SignalId.L4 }.restored)
        val unknownStart = GameSession.start(b, Fixtures.DRIVER_AND_REAR_RIGHT, CURRENT.copy(defroster = null), FakeClock())
        assertTrue(unknownStart.restoreChecklist(CURRENT).none { it.key.signal == SignalId.C4 })

        val rows = RestoreRows.of(items)
        assertEquals("전조등", rows[0].title)
        assertEquals("원래 꺼짐 · 지금 켜짐", rows[0].detail)
        assertEquals("앞유리 제상", rows[3].title)
        assertEquals("원래 꺼짐 · 지금 켜짐", rows[3].detail)
    }

    // ---- 실제 상황 팁 ---------------------------------------------------------------------------

    @Test
    fun tips_exactTextOnlyForC4L4C3() {
        assertEquals("비나 추위로 앞유리에 김이 서리면 바로 이 버튼 — 시야부터 확보하세요", SafetyTips.of(SignalId.C4))
        assertEquals("터널에 들어가거나 갑자기 어두워지면 전조등 — 내 차가 보여야 안전합니다", SafetyTips.of(SignalId.L4))
        assertEquals("터널이나 앞차 매연이 심할 때는 내기 순환으로 바깥 공기를 막으세요", SafetyTips.of(SignalId.C3))
        val withTip = setOf(SignalId.C4, SignalId.L4, SignalId.C3)
        SignalId.entries.filter { it !in withTip }.forEach { assertNull(it.name, SafetyTips.of(it)) }
        assertEquals(4_000L, SafetyTips.SHOW_MS)
    }

    @Test
    fun tips_solvedRangeSkipsSkippedAndTiplessPuzzles() {
        val b = board(input(Seeds.DEMO_SAFETY, 5, demoMode = true)) // L4, D1, L1, C4, B1
        assertEquals(SignalId.L4, SafetyTips.solvedTipSignal(b, 0, 1, emptySet()))
        assertNull(SafetyTips.solvedTipSignal(b, 1, 2, emptySet())) // D1은 팁 없음
        assertNull(SafetyTips.solvedTipSignal(b, 2, 3, emptySet())) // L1도 팁 없음
        assertNull(SafetyTips.solvedTipSignal(b, 0, 1, setOf(0))) // 건너뛴 퍼즐은 풀린 것이 아니다
        assertEquals(SignalId.C4, SafetyTips.solvedTipSignal(b, 1, 4, emptySet()))
        assertEquals(SignalId.L4, SafetyTips.solvedTipSignal(b, 0, 4, setOf(3))) // 팁 있는 마지막 퍼즐을 고른다
        assertNull(SafetyTips.solvedTipSignal(b, 4, 5, emptySet())) // 봉인은 팁 없음
        assertNull(SafetyTips.solvedTipSignal(b, 5, 9, emptySet())) // 범위 밖
    }

    @Test
    fun pool_fromAndDemo_includeSafetySignals() {
        assertEquals(SignalId.entries.toSet(), SignalPool.EMULATOR.supportedSignals)
        assertEquals(SignalId.entries.toSet(), SignalPool.demo(SignalPool()).supportedSignals)
        assertFalse(SignalId.C4 in SignalPool(temperature = ValueBounds(15f, 30f)).supportedSignals)
        assertTrue(SignalId.C4.isClimate)
        assertFalse(SignalId.L4.isClimate)
    }
}
