package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.ValueBounds
import com.team.pleosapp.domain.clue.ClueWriter
import com.team.pleosapp.domain.puzzle.Fixtures.CURRENT
import com.team.pleosapp.domain.puzzle.Fixtures.POOL
import com.team.pleosapp.domain.puzzle.Fixtures.board
import com.team.pleosapp.domain.puzzle.Fixtures.input
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PuzzleGeneratorTest {
    private val seeds = (1..200).map { "seed-$it" }

    // ---- 결정성 ----------------------------------------------------------------------------

    @Test
    fun sameInputs_sameBoard() {
        for (minutes in listOf(3, 5, 10)) {
            val a = board(input("alpha", minutes))
            val b = board(input("alpha", minutes))
            assertEquals(a, b)
        }
    }

    @Test
    fun participantOrder_doesNotMatter() {
        val a = board(input("alpha", 10, participants = linkedSetOf(SeatPosition.REAR_RIGHT, SeatPosition.DRIVER, SeatPosition.PASSENGER)))
        val b = board(input("alpha", 10, participants = linkedSetOf(SeatPosition.PASSENGER, SeatPosition.DRIVER, SeatPosition.REAR_RIGHT)))
        assertEquals(a, b)
    }

    @Test
    fun differentSeeds_differentBoards() {
        for (minutes in listOf(3, 5, 10)) {
            val boards = (1..30).map { board(input("s$it", minutes)) }
            assertEquals("minutes=$minutes", boards.size, boards.map { it.copy(seed = "") }.toSet().size)
        }
    }

    @Test
    fun differentSeedPair_differsSomewhere() {
        assertNotEquals(board(input("left")).puzzles, board(input("right")).puzzles)
    }

    // ---- 구성 -------------------------------------------------------------------------------

    @Test
    fun composition_followsBoardLength() {
        for (length in BoardLength.entries) {
            for (seed in seeds) {
                val b = board(input(seed, length.minutes))
                val label = "${length.minutes}min $seed"
                assertEquals(label, length.puzzleCount, b.puzzles.size)
                assertEquals(label, SignalId.B1, b.puzzles.last().signal)
                assertEquals(label, ClueType.SEAL, b.puzzles.last().type)
                assertTrue(label, b.puzzles.dropLast(1).none { it.signal == SignalId.B1 })
                assertTrue(label, b.puzzles.count { it.signal == SignalId.D1 } <= 1)
                assertTrue(label, b.puzzles.count { it.signal.isClimate } <= PuzzleGenerator.NON_EV_CLIMATE_MAX)
                val content = b.puzzles.dropLast(1)
                val reasoning = content.count { it.type == ClueType.CIPHER || it.type == ClueType.CHAIN }
                val direct = content.count { it.type == ClueType.DIRECT }
                // 연쇄형이 지시형으로 바뀐 퍼즐은 지시형으로 센다
                assertEquals(label, length.reasoning, reasoning + content.count { it.fallbackFromChain })
                assertEquals(label, length.direct, direct - content.count { it.fallbackFromChain })
                b.puzzles.forEachIndexed { i, p -> assertEquals(label, i, p.index) }
            }
        }
    }

    @Test
    fun reasoningBoards_haveCipherAndChain() {
        for (minutes in listOf(5, 10)) {
            for (seed in seeds) {
                val types = board(input(seed, minutes)).puzzles.map { it.type }
                assertTrue("$minutes $seed", ClueType.CIPHER in types)
                assertTrue("$minutes $seed", ClueType.CHAIN in types)
            }
        }
    }

    @Test
    fun readingLight_onlyForNonDriverParticipants() {
        for (seed in seeds) {
            val b = board(input(seed, 10, participants = setOf(SeatPosition.DRIVER, SeatPosition.REAR_LEFT)))
            val seats = b.puzzles.mapNotNull { (it.target as? Target.ReadingLight)?.seat }
            assertTrue(seed, seats.all { it == SeatPosition.REAR_LEFT })
            assertTrue(seed, seats.size <= 1)
        }
        for (seed in seeds) {
            val b = board(input(seed, 10, participants = setOf(SeatPosition.DRIVER)))
            assertTrue(seed, b.puzzles.none { it.signal == SignalId.L3 })
        }
    }

    @Test
    fun ev_allowsMoreClimateAndFullFanRange() {
        val boards = seeds.map { board(input(it, 10, isEv = true)) }
        assertTrue(boards.any { b -> b.puzzles.count { it.signal.isClimate } > PuzzleGenerator.NON_EV_CLIMATE_MAX })
        assertTrue(boards.flatMap { it.puzzles }.any { (it.target as? Target.FanSpeed)?.level?.let { l -> l > 4 } == true })
    }

    // ---- 신호별 제약 ------------------------------------------------------------------------

    @Test
    fun targets_respectSignalConstraints() {
        for (minutes in listOf(3, 5, 10)) {
            for (seed in seeds) {
                val b = board(input(seed, minutes))
                for ((puzzle, before) in Fixtures.projectedBefore(b, CURRENT)) {
                    val label = "$minutes $seed #${puzzle.index} $puzzle"
                    when (val t = puzzle.target) {
                        is Target.Temperature -> {
                            assertTrue(label, t.celsius in 18..28)
                            assertTrue(label, t.celsius >= POOL.temperature!!.min + 1 && t.celsius <= POOL.temperature!!.max - 1)
                            assertTrue(label, abs(t.celsius - before.targetTemperature!!) >= 2f)
                        }
                        is Target.FanSpeed -> {
                            assertTrue(label, t.level in POOL.fanSpeed!!.min..POOL.fanSpeed!!.max / 2)
                            assertTrue(label, t.level != before.fanSpeed)
                        }
                        is Target.Brightness -> {
                            assertTrue(label, t.level in POOL.ambientBrightness!!.min..POOL.ambientBrightness!!.max)
                            assertTrue(label, t.level != before.ambientBrightness)
                        }
                        is Target.Recirculation -> assertEquals(label, !before.recirculation!!, t.on)
                        is Target.AmbientColor -> assertFalse(label, ColorRule.matches(before.ambientColor!!, t.color))
                        is Target.ReadingLight -> assertEquals(label, false, before.readingLights[t.seat])
                        is Target.Defroster -> {
                            assertTrue(label, t.on)
                            assertEquals(label, false, before.defroster)
                        }
                        is Target.LowBeam -> {
                            assertTrue(label, t.on)
                            assertEquals(label, false, before.lowBeam)
                        }
                        is Target.Steering -> {
                            assertTrue(label, t.bin in 1..3)
                            assertEquals(label, -470f, t.boundsMin)
                            assertEquals(label, 500f, t.boundsMax)
                        }
                        is Target.BrakePresses -> assertTrue(label, t.count in 2..4)
                        Target.SealButton -> error("brake is supported in this pool")
                    }
                }
            }
        }
    }

    @Test
    fun chainResults_matchFormulaAndComeFromEarlierAnswer() {
        var chains = 0
        for (minutes in listOf(5, 10)) {
            for (seed in seeds) {
                val b = board(input(seed, minutes))
                for (p in b.puzzles.filter { it.type == ClueType.CHAIN }) {
                    val formula = p.chain!!
                    assertTrue(formula.sourceIndex < p.index)
                    val source = b.puzzles[formula.sourceIndex]
                    assertTrue(source.signal in setOf(SignalId.D1, SignalId.C1, SignalId.C2, SignalId.L2))
                    assertEquals(p.numericAnswer, formula.apply(source.numericAnswer!!))
                    assertEquals(source.numericAnswer, b.chainInput(p.index))
                    // 한 번만 계산하는 식(× m 또는 ± b)
                    assertTrue((formula.multiplier == 1) != (formula.offset == 0))
                    chains++
                }
            }
        }
        assertTrue(chains > 300)
    }

    @Test
    fun cipherSymbols_resolveInTableAndAreUnique() {
        for (seed in seeds) {
            val b = board(input(seed, 10, participants = setOf(SeatPosition.DRIVER, SeatPosition.PASSENGER, SeatPosition.REAR_RIGHT)))
            val symbols = b.cipherTable.map { it.symbol }
            assertEquals(seed, symbols.size, symbols.toSet().size)
            for (p in b.puzzles.filter { it.type == ClueType.CIPHER }) {
                val meaning = b.cipherTable.single { it.symbol == p.cipherSymbol }.meaning
                val expected = when (val t = p.target) {
                    is Target.Temperature -> CipherMeaning.Number(t.celsius)
                    is Target.FanSpeed -> CipherMeaning.Number(t.level)
                    is Target.Brightness -> CipherMeaning.Number(t.level)
                    is Target.Recirculation -> CipherMeaning.Recirculation(t.on)
                    is Target.AmbientColor -> CipherMeaning.Color(t.color)
                    is Target.ReadingLight -> CipherMeaning.Seat(t.seat)
                    is Target.Steering -> CipherMeaning.Steering(t.side, t.bin)
                    is Target.Defroster -> CipherMeaning.Switch(t.on)
                    is Target.LowBeam -> CipherMeaning.Switch(t.on)
                    else -> error("unexpected cipher target")
                }
                assertEquals(seed, expected, meaning)
            }
        }
    }

    @Test
    fun unknownFanSpeed_excludesC2() {
        for (seed in seeds) {
            val b = board(input(seed, 10, current = CURRENT.copy(fanSpeed = null)))
            assertTrue(seed, b.puzzles.none { it.signal == SignalId.C2 })
        }
    }

    @Test
    fun currentTemperatureOutsideComfort_stillGivesComfortTargets() {
        val b = seeds.map { board(input(it, 10, current = CURRENT.copy(targetTemperature = 30f))) }
        val temps = b.flatMap { it.puzzles }.mapNotNull { (it.target as? Target.Temperature)?.celsius }
        assertTrue(temps.isNotEmpty())
        assertTrue(temps.all { it in 18..28 })
    }

    // ---- 판 길이별 실현 가능성(리더 결정) -----------------------------------------------------

    private val driverOnly = setOf(SeatPosition.DRIVER)
    private val climateBounds = SignalPool(
        temperature = ValueBounds(15f, 30f),
        fanSpeed = ValueBounds(1, 8),
        recirculation = true,
        brake = true,
    )

    @Test
    fun minimalClimateAndDrive_nonEv_onlyThreeMinutes() {
        // 설명회 결과 CLIMATE + DRIVE만 남는 최소 구성: C1·C2·C3·D1·B1. 비EV는 공조 2 + D1 + 봉인 = 4개까지
        val pool = climateBounds.copy(steering = ValueBounds(-470f, 500f))
        assertEquals(4, PuzzleGenerator.maxPuzzleCount(driverOnly, pool, CURRENT))
        assertEquals(setOf(BoardLength.THREE), PuzzleGenerator.availableLengths(driverOnly, pool, CURRENT))
        assertEquals(GenerationResult.Unavailable(4), PuzzleGenerator.generate(input("x", 5, driverOnly, pool)))
        assertEquals(GenerationResult.Unavailable(4), PuzzleGenerator.generate(input("x", 10, driverOnly, pool)))
        for (seed in seeds) {
            val b = board(input(seed, 3, driverOnly, pool))
            assertEquals(seed, 3, b.puzzles.size)
            assertTrue(seed, b.puzzles.count { it.signal.isClimate } <= 2)
        }
    }

    @Test
    fun climateAndBrakeOnly_nonEv_threeMinutesWithTwoClimate() {
        // 기획 7절: 공조 3종 + 브레이크만으로도 판이 성립해야 한다
        assertEquals(setOf(BoardLength.THREE), PuzzleGenerator.availableLengths(driverOnly, climateBounds, CURRENT))
        for (seed in seeds) {
            val b = board(input(seed, 3, driverOnly, climateBounds))
            assertEquals(seed, listOf(true, true, false), b.puzzles.map { it.signal.isClimate })
            assertEquals(seed, SignalId.B1, b.puzzles.last().signal)
        }
    }

    @Test
    fun climateAndBrakeOnly_ev_upToFiveMinutes() {
        // EV는 공조 상한이 없고 종류당 2개: 3종 × 2 + 봉인 = 7 → 10분(8개)은 안 된다
        assertEquals(7, PuzzleGenerator.maxPuzzleCount(driverOnly, climateBounds, CURRENT, isEv = true))
        assertEquals(setOf(BoardLength.THREE, BoardLength.FIVE), PuzzleGenerator.availableLengths(driverOnly, climateBounds, CURRENT, isEv = true))
        for (seed in seeds) {
            val b = board(input(seed, 5, driverOnly, climateBounds, isEv = true))
            assertEquals(seed, 5, b.puzzles.size)
            val keys = b.puzzles.dropLast(1).map { it.key }
            assertTrue(seed, keys.groupingBy { it }.eachCount().values.all { it <= PuzzleGenerator.MAX_PER_KIND })
        }
    }

    @Test
    fun tooFewSignals_noLengthAvailable() {
        val steeringOnly = SignalPool(steering = ValueBounds(-470f, 500f), brake = true)
        assertEquals(2, PuzzleGenerator.maxPuzzleCount(driverOnly, steeringOnly, CURRENT))
        assertEquals(emptySet<BoardLength>(), PuzzleGenerator.availableLengths(driverOnly, steeringOnly, CURRENT))
        assertEquals(GenerationResult.Unavailable(2), PuzzleGenerator.generate(input("x", 3, driverOnly, steeringOnly)))
        assertEquals(emptySet<BoardLength>(), PuzzleGenerator.availableLengths(driverOnly, SignalPool(brake = true), CURRENT))
    }

    @Test
    fun fullPoolSoloDriver_allLengths_withRepeatsAtMostTwice() {
        // 비EV 공조 2(C4 포함) + L1·L2 각 2 + L4 1 + D1 1 = 8 + 봉인 = 9
        assertEquals(9, PuzzleGenerator.maxPuzzleCount(driverOnly, POOL, CURRENT))
        assertEquals(BoardLength.entries.toSet(), PuzzleGenerator.availableLengths(driverOnly, POOL, CURRENT))
        for (seed in seeds) {
            val b = board(input(seed, 10, driverOnly))
            assertEquals(seed, 8, b.puzzles.size)
            val counts = b.puzzles.dropLast(1).map { it.signal }.groupingBy { it }.eachCount()
            assertTrue(seed, counts.values.all { it <= PuzzleGenerator.MAX_PER_KIND })
            assertTrue(seed, b.puzzles.zipWithNext().none { (a, c) -> a.signal == c.signal })
        }
    }

    @Test
    fun singleContentSignal_noLengthAvailable() {
        // 봉인을 뺀 서로 다른 신호가 1종이면 같은 신호가 연달아 올 수밖에 없다(QA 2회차 실패 2)
        for (pool in listOf(
            SignalPool(ambientColor = true, brake = true),
            SignalPool(ambientBrightness = ValueBounds(0, 100), brake = true),
            SignalPool(recirculation = true, brake = true),
        )) {
            assertEquals(pool.toString(), emptySet<BoardLength>(), PuzzleGenerator.availableLengths(driverOnly, pool, CURRENT, isEv = true))
            assertTrue(pool.toString(), PuzzleGenerator.generate(input("x", 3, driverOnly, pool, isEv = true)) is GenerationResult.Unavailable)
        }
        // 2열 좌석 둘의 독서등만 있어도 같은 신호(L3)다
        val seatsOnly = SignalPool(readingLightSeats = setOf(SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT), brake = true)
        val passengers = setOf(SeatPosition.DRIVER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT)
        assertEquals(emptySet<BoardLength>(), PuzzleGenerator.availableLengths(passengers, seatsOnly, CURRENT))
    }

    @Test
    fun narrowTemperature_countsOnlyRealTargets() {
        // QA 2회차 실패 3 재현: 17~20℃면 목표는 18·19℃뿐이라 C1은 한 번만 낼 수 있다
        val narrow = SignalPool(temperature = ValueBounds(17f, 20f), brake = true)
        assertEquals(emptySet<BoardLength>(), PuzzleGenerator.availableLengths(driverOnly, narrow, CURRENT, isEv = true))
        val narrowWithColor = narrow.copy(ambientColor = true)
        assertEquals(4, PuzzleGenerator.maxPuzzleCount(driverOnly, narrowWithColor, CURRENT, isEv = true))
        assertEquals(setOf(BoardLength.THREE), PuzzleGenerator.availableLengths(driverOnly, narrowWithColor, CURRENT, isEv = true))
        // 16~23℃면 18 다음 20 이상이 남으므로 C1을 두 번 낼 수 있다(첫 목표는 다음 목표가 남는 값만 고른다)
        val twoTargets = SignalPool(temperature = ValueBounds(16f, 23f), ambientColor = true, brake = true)
        assertEquals(setOf(BoardLength.THREE, BoardLength.FIVE), PuzzleGenerator.availableLengths(driverOnly, twoTargets, CURRENT, isEv = true))
        for (seed in seeds) {
            val b = board(input(seed, 5, driverOnly, twoTargets, isEv = true))
            val temps = b.puzzles.mapNotNull { (it.target as? Target.Temperature)?.celsius }
            assertEquals(seed, 2, temps.size)
            assertTrue(seed, abs(temps[1] - temps[0]) >= 2)
        }
    }

    @Test
    fun property_availableLengthsAlwaysGenerate_andNoAdjacentRepeats() {
        // 핵심 속성(리더 결정): availableLengths가 가능하다고 한 길이는 어떤 시드로든 generate가 성공하고, 아니면 Unavailable이다
        val r = SeededRandom(2026)
        fun <T> pick(vararg options: T): T = options[r.nextInt(options.size)]
        val seats = listOf(SeatPosition.PASSENGER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT)
        var successes = 0
        repeat(3_000) { case ->
            val pool = SignalPool(
                temperature = pick(null, ValueBounds(15f, 30f), ValueBounds(17f, 20f), ValueBounds(16f, 23f), ValueBounds(18f, 21f)),
                fanSpeed = pick(null, ValueBounds(1, 8), ValueBounds(1, 2), ValueBounds(1, 3), ValueBounds(2, 2), ValueBounds(1, 1)),
                recirculation = r.nextBoolean(),
                ambientColor = r.nextBoolean(),
                ambientBrightness = pick(null, ValueBounds(0, 100), ValueBounds(5, 5), ValueBounds(0, 1)),
                readingLightSeats = seats.filter { r.nextBoolean() }.toSet() + SeatPosition.DRIVER,
                steering = pick(null, ValueBounds(-470f, 500f), ValueBounds(-4f, 4f), ValueBounds(-40f, 40f)),
                brake = r.nextBoolean(),
            )
            val participants = seats.filter { r.nextBoolean() }.toSet() + SeatPosition.DRIVER
            val current = VehicleSignals(
                targetTemperature = pick(null, 16f, 18f, 19f, 20f, 22f, 24f, 30f),
                fanSpeed = pick(null, 0, 1, 2, 4, 8),
                recirculation = pick(null, true, false),
                ambientColor = pick(null, 0x0000E5FF, 0x00AA00FF, 0x00FFFFFF, 0x00FF0000),
                ambientBrightness = pick(null, 0, 50, 100),
                readingLights = (seats + SeatPosition.DRIVER).associateWith { pick<Boolean?>(null, false, true) },
                steeringAngle = pick(null, 0f, -45f),
                brakePressed = false,
            )
            val isEv = r.nextBoolean()
            val demo = r.nextInt(10) == 0
            val available = PuzzleGenerator.availableLengths(participants, pool, current, isEv, demo)
            for (length in BoardLength.entries) {
                for (seed in listOf("p$case", "q$case")) {
                    val result = PuzzleGenerator.generate(GeneratorInput(seed, length.minutes, participants, pool, current, isEv, demo))
                    val label = "case=$case len=${length.minutes} seed=$seed avail=$available pool=$pool current=$current ev=$isEv demo=$demo"
                    assertEquals(label, length in available, result is GenerationResult.Success)
                    if (result !is GenerationResult.Success) continue
                    successes++
                    val p = result.board.puzzles
                    assertEquals(label, length.puzzleCount, p.size)
                    assertEquals(label, ClueType.SEAL, p.last().type)
                    assertTrue(label, p.zipWithNext().none { (a, b) -> a.signal == b.signal })
                    assertTrue(label, p.groupingBy { it.signal }.eachCount().values.all { it <= PuzzleGenerator.MAX_PER_KIND })
                    assertTrue(label, isEv || p.count { it.signal.isClimate } <= PuzzleGenerator.NON_EV_CLIMATE_MAX)
                    assertTrue(label, p.count { it.signal == SignalId.D1 } <= 1)
                }
            }
        }
        assertTrue("successes=$successes", successes > 3_000)
    }

    /** F2-ui QA 실패 2(리더 결정): 연쇄형은 식에 적힌 숫자가 곧 답이면 안 된다. 무작위 입력 수천 건으로 고정한다. */
    @Test
    fun property_chainTargetNeverEqualsWrittenConstant() {
        val r = SeededRandom(1027)
        fun <T> pick(vararg options: T): T = options[r.nextInt(options.size)]
        val seats = listOf(SeatPosition.PASSENGER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT)
        var chains = 0
        var fallbacks = 0
        repeat(3_000) { case ->
            val pool = SignalPool(
                temperature = pick(null, ValueBounds(15f, 30f), ValueBounds(16f, 23f), ValueBounds(18f, 28f)),
                fanSpeed = pick(null, ValueBounds(1, 8), ValueBounds(1, 3), ValueBounds(1, 10)),
                recirculation = r.nextBoolean(),
                ambientColor = r.nextBoolean(),
                ambientBrightness = pick(null, ValueBounds(0, 100), ValueBounds(0, 10), ValueBounds(1, 5)),
                readingLightSeats = seats.filter { r.nextBoolean() }.toSet() + SeatPosition.DRIVER,
                steering = pick(null, ValueBounds(-470f, 500f), ValueBounds(-40f, 40f)),
                brake = r.nextBoolean(),
            )
            val participants = seats.filter { r.nextBoolean() }.toSet() + SeatPosition.DRIVER
            val current = VehicleSignals(
                targetTemperature = pick(null, 18f, 20f, 22f, 24f, 26f),
                fanSpeed = pick(null, 1, 2, 3, 4),
                recirculation = pick(null, true, false),
                ambientColor = pick(null, 0x0000E5FF, 0x00FFFFFF),
                ambientBrightness = pick(null, 0, 1, 5, 50, 100),
                readingLights = (seats + SeatPosition.DRIVER).associateWith { pick<Boolean?>(null, false) },
                steeringAngle = 0f,
                brakePressed = false,
            )
            val isEv = r.nextBoolean()
            for (length in BoardLength.entries) {
                val result = PuzzleGenerator.generate(GeneratorInput("c$case", length.minutes, participants, pool, current, isEv))
                if (result !is GenerationResult.Success) continue
                val board = result.board
                board.puzzles.forEach { puzzle ->
                    if (puzzle.fallbackFromChain) fallbacks++
                    val formula = puzzle.chain ?: return@forEach
                    chains++
                    val input = board.puzzles[formula.sourceIndex].numericAnswer!!
                    val value = puzzle.numericAnswer!!
                    val label = "case=$case len=${length.minutes} formula=$formula input=$input value=$value"
                    assertEquals(label, formula.apply(input), value)
                    assertTrue(label, value != formula.writtenNumber) // 문장에 보이는 수(빼기는 절댓값)가 곧 답이 아니다
                    assertTrue(label, value.toString() !in ClueWriter.clue(board, puzzle.index).text.split(' ', '\u00A0'))
                    assertTrue(label, value != input) // 입력을 그대로 옮긴 답도 아니다
                    assertFalse(label, formula.multiplier == 1 && formula.offset == 0)
                    assertFalse(label, formula.isTrivialFor(input))
                }
            }
        }
        assertTrue("chains=$chains", chains > 1_000) // 항등식을 빼도 연쇄형은 계속 나온다(나머지는 지시형으로: fallbacks)
        assertTrue("fallbacks=$fallbacks", fallbacks > 0)
    }

    @Test
    fun chainFormula_trivialCases() {
        assertTrue(ChainFormula(0, 1, 26).isTrivialFor(0)) // 26 + 조명 출력(0) = 26
        assertTrue(ChainFormula(0, 4, 0).isTrivialFor(1)) // 환기 팬 단수(1) × 4 = 4
        assertTrue(ChainFormula(0, 3, 0).isTrivialFor(0)) // 0 × 3 = 0 = 입력
        assertTrue(ChainFormula(0, 1, 0).isTrivialFor(5))
        assertTrue(ChainFormula(0, 1, -1).isTrivialFor(2)) // 조타 구간 번호(2) − 1 = 1: 문장에 보이는 수(QA MINOR 3)
        assertTrue(ChainFormula(0, 1, -5).isTrivialFor(10)) // 10 − 5 = 5
        assertFalse(ChainFormula(0, 1, -1).isTrivialFor(3)) // 3 − 1 = 2
        assertFalse(ChainFormula(0, 1, 20).isTrivialFor(2)) // demo5: 20 + 2 = 22
        assertFalse(ChainFormula(0, 2, 0).isTrivialFor(2)) // demo5 대체: 2 × 2 = 4
        assertEquals(20, ChainFormula(0, 1, 20).constant)
        assertEquals(2, ChainFormula(0, 2, 0).constant)
        assertEquals(1, ChainFormula(0, 1, -1).writtenNumber)
    }

    @Test
    fun brakeUnsupported_sealStillCounts() {
        val noBrake = climateBounds.copy(brake = false)
        assertEquals(setOf(BoardLength.THREE), PuzzleGenerator.availableLengths(driverOnly, noBrake, CURRENT))
        assertEquals(Target.SealButton, board(input("seal", 3, driverOnly, noBrake)).puzzles.last().target)
    }

    @Test
    fun brakeUnsupported_usesScreenSeal() {
        val b = board(input("seal", pool = POOL.copy(brake = false)))
        assertEquals(Target.SealButton, b.puzzles.last().target)
    }

    @Test
    fun unusableSteeringBounds_excludeD1() {
        val b = seeds.map { board(input(it, 10, pool = POOL.copy(steering = ValueBounds(-4f, 4f)))) }
        assertTrue(b.all { board -> board.puzzles.none { it.signal == SignalId.D1 } })
    }

    // ---- 시연 모드 ---------------------------------------------------------------------------

    @Test
    fun demo5_matchesPlanScenario() {
        val b = board(input(Seeds.DEMO, 5, demoMode = true))
        val p = b.puzzles
        assertEquals(5, p.size)
        assertEquals(Puzzle(0, SignalId.D1, ClueType.DIRECT, Target.Steering(SteeringSide.LEFT, 2, -470f, 500f)), p[0])
        assertEquals(SignalId.L1, p[1].signal)
        assertEquals(ClueType.CIPHER, p[1].type)
        assertEquals(Target.AmbientColor(NamedColor.PURPLE), p[1].target)
        assertEquals(CipherMeaning.Color(NamedColor.PURPLE), b.cipherTable.single { it.symbol == p[1].cipherSymbol }.meaning)
        assertEquals(CipherSymbol.DIAMOND, p[1].cipherSymbol)
        assertEquals(Puzzle(2, SignalId.L3, ClueType.DIRECT, Target.ReadingLight(SeatPosition.REAR_RIGHT)), p[2])
        assertEquals(Puzzle(3, SignalId.C1, ClueType.CHAIN, Target.Temperature(22), chain = ChainFormula(0, 1, 20)), p[3])
        assertEquals(Puzzle(4, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)), p[4])
        assertTrue(b.demo)
    }

    @Test
    fun demo5_withoutRearRight_usesFanChain() {
        val b = board(input(Seeds.DEMO, 5, participants = setOf(SeatPosition.DRIVER), demoMode = true))
        assertEquals(Puzzle(2, SignalId.C2, ClueType.CHAIN, Target.FanSpeed(4), chain = ChainFormula(0, 2, 0)), b.puzzles[2])
        assertEquals(Target.Temperature(22), b.puzzles[3].target)
    }

    @Test
    fun demo5_whenTemperatureAlready22_fallsBackToDirect() {
        // 에뮬레이터 기본값 22.0℃ 그대로면 연쇄형 22℃가 현재값 조건(2℃ 이상)을 어긴다 → 지시형으로 바꾼다
        val b = board(input(Seeds.DEMO, 5, current = CURRENT.copy(targetTemperature = 22f), demoMode = true))
        val c1 = b.puzzles[3]
        assertEquals(ClueType.DIRECT, c1.type)
        assertTrue(c1.fallbackFromChain)
        assertEquals(Target.Temperature(24), c1.target)
    }

    @Test
    fun demoMode_ignoresCapabilityAndLimitsColors() {
        val boards = seeds.map { board(input(it, 10, pool = SignalPool(brake = true), demoMode = true)) }
        val colors = boards.flatMap { it.puzzles }.mapNotNull { (it.target as? Target.AmbientColor)?.color }.toSet()
        assertTrue(colors.isNotEmpty())
        assertTrue(colors.all { it == NamedColor.PURPLE || it == NamedColor.BLUE })
        assertTrue(boards.all { b -> b.puzzles.dropLast(1).all { it.target !is Target.BrakePresses } })
        assertTrue(boards.all { (it.puzzles.last().target as Target.BrakePresses).count == 2 })
    }

    // ---- 시드(고정값: Python 참조 구현과 같음, QA 확인) ----------------------------------------

    @Test
    fun dailySeed_andHash_areFixedValues() {
        assertEquals("day-2026-11-20", Seeds.daily(java.time.LocalDate.of(2026, 11, 20)))
        assertEquals(6444144698443013625L, Seeds.hash("demo5"))
        assertEquals(3009108143986234132L, Seeds.hash("day-2026-11-20"))
    }

    @Test
    fun seededRandom_isSplitMix64() {
        assertEquals(-2152535657050944081L, SeededRandom(0).nextLong()) // 0xE220A8397B1DCDAF
        val r = SeededRandom(7)
        repeat(1000) { assertTrue(r.nextInt(3) in 0..2) }
    }

    @Test
    fun goldenBoard_fixedInputGivesFixedBoard() {
        // 알고리즘·상수가 바뀌면 이 판이 바뀐다(같은 시드 = 같은 판 보장 확인용).
        // 제상·전조등 현재값을 모르면(C4·L4 제외) 두 신호를 넣기 전과 같은 판이 나와야 한다.
        val b = board(input("abc", 5, current = CURRENT.copy(defroster = null, lowBeam = null)))
        assertEquals(
            listOf(
                Puzzle(0, SignalId.D1, ClueType.DIRECT, Target.Steering(SteeringSide.RIGHT, 3, -470f, 500f)),
                Puzzle(1, SignalId.C3, ClueType.CIPHER, Target.Recirculation(true), cipherSymbol = CipherSymbol.HEART),
                Puzzle(2, SignalId.C1, ClueType.CHAIN, Target.Temperature(28), chain = ChainFormula(0, 1, 25)),
                Puzzle(3, SignalId.L1, ClueType.DIRECT, Target.AmbientColor(NamedColor.BLUE)),
                Puzzle(4, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)),
            ),
            b.puzzles,
        )
        assertEquals(
            listOf(
                CipherEntry(CipherSymbol.HEART, CipherMeaning.Recirculation(true)),
                CipherEntry(CipherSymbol.DOT, CipherMeaning.Recirculation(false)),
            ),
            b.cipherTable,
        )
    }
}
