package com.team.pleosapp.domain.clue

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.domain.puzzle.Board
import com.team.pleosapp.domain.puzzle.ChainFormula
import com.team.pleosapp.domain.puzzle.CipherEntry
import com.team.pleosapp.domain.puzzle.CipherMeaning
import com.team.pleosapp.domain.puzzle.CipherSymbol
import com.team.pleosapp.domain.puzzle.ClueType
import com.team.pleosapp.domain.puzzle.Fixtures
import com.team.pleosapp.domain.puzzle.NamedColor
import com.team.pleosapp.domain.puzzle.Puzzle
import com.team.pleosapp.domain.puzzle.Seeds
import com.team.pleosapp.domain.puzzle.SignalId
import com.team.pleosapp.domain.puzzle.SteeringSide
import com.team.pleosapp.domain.puzzle.Target
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClueWriterTest {

    /** 시연 판(기획 5절 demo5)의 단서는 기획 2-1 단서 예와 글자까지 같다. */
    @Test
    fun demoBoard_matchesPlanSentences() {
        val board = Fixtures.board(Fixtures.input(Seeds.DEMO, demoMode = true))
        val texts = board.puzzles.indices.map { ClueWriter.clue(board, it).text }
        assertEquals(
            listOf(
                "조타 키를 왼쪽 2번 구간에 맞추고 중앙으로",
                "비상 조명을 산소 계통 색(◇)으로",
                "2열 오른쪽 승무원: 독서등으로 신호",
                "냉각 코일 온도 = 20 + 조타 구간 번호",
                "브레이크를 두 번 밟아 해치 봉인 해제",
            ),
            texts,
        )
        val labels = board.puzzles.indices.map { ClueWriter.clue(board, it).typeLabel }
        assertEquals(listOf("지시형", "암호형", "지시형", "연쇄형", "마지막 봉인"), labels)
        assertEquals(CipherSymbol.DIAMOND, ClueWriter.clue(board, 1).symbol)
        assertEquals("보라", ClueWriter.meaning(board.cipherTable.first { it.symbol == CipherSymbol.DIAMOND }.meaning))
        // 조명 설정 메뉴로 가는 단서에만 P 확인 안내
        assertEquals(ClueWriter.SETTINGS_NOTE, ClueWriter.clue(board, 1).settingsNote)
        assertNull(ClueWriter.clue(board, 0).settingsNote)
    }

    @Test
    fun sameSeed_sameSentences_differentSeed_differentBoardText() {
        fun sentences(seed: String): List<String> {
            val board = Fixtures.board(Fixtures.input(seed, minutes = 10))
            return board.puzzles.indices.flatMap { ClueWriter.clue(board, it).let { c -> listOf(c.text, c.theme) + c.hints } } +
                board.cipherTable.map { "${it.symbol.glyph}=${ClueWriter.meaning(it.meaning)}" }
        }
        assertEquals(sentences("alpha"), sentences("alpha"))
        val variety = (1..20).map { sentences("seed-$it") }.toSet()
        assertTrue("시드가 달라지면 문장도 달라져야 한다", variety.size > 15)
    }

    /** 신호 8종 × 유형 3종(지시·암호·연쇄)을 모두 문장으로 만든다. 연쇄형은 숫자 목표(C1·C2·L2)만, 그 밖은 지시형 문장이다. */
    @Test
    fun everySignalTimesType_hasSentenceAndTwoHints() {
        for (signal in SignalId.entries) {
            for (type in listOf(ClueType.DIRECT, ClueType.CIPHER, ClueType.CHAIN)) {
                val board = boardWith(signal, type)
                val index = board.puzzles.size - (if (signal == SignalId.B1) 1 else 2)
                val clue = ClueWriter.clue(board, index, brightnessRange = 0..100)
                val label = "$signal/$type"
                assertTrue(label, clue.text.isNotBlank())
                assertTrue("$label 한 줄 안팎: ${clue.text}", clue.text.length <= 26)
                assertEquals(label, 2, clue.hints.size)
                assertTrue(label, clue.hints.all { it.isNotBlank() && it.length <= 40 })
                val effective = ClueWriter.effectiveType(board.puzzles[index])
                assertEquals(label, ClueWriter.typeLabel(effective), clue.typeLabel)
                when {
                    signal == SignalId.B1 -> {
                        assertEquals(label, ClueType.SEAL, effective)
                        assertTrue(label, clue.text.contains("봉인"))
                    }
                    type == ClueType.CIPHER -> {
                        assertEquals(label, CipherSymbol.STAR, clue.symbol)
                        assertTrue(label, clue.text.contains("☆"))
                        assertTrue(label, clue.hints[0].contains("☆"))
                    }
                    type == ClueType.CHAIN && signal in setOf(SignalId.C1, SignalId.C2, SignalId.L2) -> {
                        assertEquals(label, ClueType.CHAIN, effective)
                        assertTrue(label, clue.text.contains(" = ") && clue.text.contains("조타 구간 번호"))
                    }
                    else -> {
                        assertEquals(label, ClueType.DIRECT, effective)
                        assertNull(label, clue.symbol)
                    }
                }
            }
        }
    }

    @Test
    fun directSentences_sayTheTarget() {
        fun text(target: Target, signal: SignalId) = ClueWriter.clue(single(Puzzle(0, signal, ClueType.DIRECT, target)), 0, 0..100).text
        assertEquals("냉각 코일을 22℃로", text(Target.Temperature(22), SignalId.C1))
        assertEquals("환기 팬을 4단으로", text(Target.FanSpeed(4), SignalId.C2))
        assertEquals("외부 공기 차단 — 내기 순환", text(Target.Recirculation(true), SignalId.C3))
        assertEquals("격벽 개방 — 외기 유입", text(Target.Recirculation(false), SignalId.C3))
        assertEquals("비상 조명을 보라색으로", text(Target.AmbientColor(NamedColor.PURPLE), SignalId.L1))
        assertEquals("조명 출력을 최대로", text(Target.Brightness(100), SignalId.L2))
        assertEquals("조명 출력을 최소로", text(Target.Brightness(0), SignalId.L2))
        assertEquals("조명 출력을 40으로", text(Target.Brightness(40), SignalId.L2))
        assertEquals("조명 출력을 25로", text(Target.Brightness(25), SignalId.L2))
        assertEquals("조명 출력을 63으로", text(Target.Brightness(63), SignalId.L2))
        assertEquals("조수석 승무원: 독서등으로 신호", text(Target.ReadingLight(SeatPosition.PASSENGER), SignalId.L3))
        assertEquals("조타 키를 오른쪽 1번 구간에 맞추고 중앙으로", text(Target.Steering(SteeringSide.RIGHT, 1, -470f, 500f), SignalId.D1))
    }

    @Test
    fun chainSentences_useOneOperation() {
        val d1 = Puzzle(0, SignalId.D1, ClueType.DIRECT, Target.Steering(SteeringSide.LEFT, 3, -470f, 500f))
        fun chain(m: Int, b: Int, signal: SignalId, target: Target) =
            ClueWriter.clue(Board("s", 5, listOf(d1, Puzzle(1, signal, ClueType.CHAIN, target, chain = ChainFormula(0, m, b)), seal(2)), emptyList()), 1).text
        assertEquals("냉각 코일 온도 = 20 + 조타 구간 번호", chain(1, 20, SignalId.C1, Target.Temperature(23)))
        assertEquals("환기 팬 단수 = 조타 구간 번호 × 2", chain(2, 0, SignalId.C2, Target.FanSpeed(6)))
        assertEquals("조명 출력 = 조타 구간 번호 − 1", chain(1, -1, SignalId.L2, Target.Brightness(2)))
        val hint = ClueWriter.clue(Board("s", 5, listOf(d1, Puzzle(1, SignalId.C1, ClueType.CHAIN, Target.Temperature(23), chain = ChainFormula(0, 1, 20)), seal(2)), emptyList()), 1).hints[0]
        assertEquals("조타 구간 번호는 1번 퍼즐에서 맞춘 칸 번호예요", hint)
    }

    @Test
    fun chainInputReveal_onlyWhenSourceSkipped() {
        val d1 = Puzzle(0, SignalId.D1, ClueType.DIRECT, Target.Steering(SteeringSide.LEFT, 2, -470f, 500f))
        val c1 = Puzzle(1, SignalId.C1, ClueType.CHAIN, Target.Temperature(22), chain = ChainFormula(0, 1, 20))
        val board = Board("s", 5, listOf(d1, c1, seal(2)), emptyList())
        assertNull(ClueWriter.chainInputReveal(board, 1, emptySet()))
        assertEquals("1번 퍼즐을 건너뛰어 값을 알려 드려요: 조타 구간 번호 = 2", ClueWriter.chainInputReveal(board, 1, setOf(0)))
        assertNull(ClueWriter.chainInputReveal(board, 0, setOf(0)))
    }

    @Test
    fun cipherL3Hint_doesNotRevealSeat() {
        val puzzle = Puzzle(0, SignalId.L3, ClueType.CIPHER, Target.ReadingLight(SeatPosition.REAR_RIGHT), cipherSymbol = CipherSymbol.TRIANGLE)
        val clue = ClueWriter.clue(single(puzzle, listOf(CipherEntry(CipherSymbol.TRIANGLE, CipherMeaning.Seat(SeatPosition.REAR_RIGHT)))), 0)
        assertEquals("△ 자리 승무원: 독서등으로 신호", clue.text)
        assertFalse(clue.text.contains("2열 오른쪽"))
        assertFalse(clue.hints.any { it.contains("2열 오른쪽") })
        // 지시형은 조작 위치에 좌석을 말한다
        val direct = ClueWriter.clue(single(puzzle.copy(type = ClueType.DIRECT, cipherSymbol = null)), 0)
        assertTrue(direct.hints[1].contains("2열 오른쪽"))
    }

    @Test
    fun randomBoards_allPuzzlesWritable() {
        for (minutes in listOf(3, 5, 10)) {
            for (i in 1..50) {
                val board = Fixtures.board(Fixtures.input("r$minutes-$i", minutes = minutes))
                board.puzzles.indices.forEach { index ->
                    val clue = ClueWriter.clue(board, index, 0..100)
                    assertTrue(clue.text, clue.text.length <= 26)
                    if (clue.symbol != null) assertNotNull(board.cipherTable.firstOrNull { it.symbol == clue.symbol })
                }
                board.puzzles.forEach { assertTrue(ClueWriter.solvedLabel(it).isNotBlank()) }
            }
        }
    }

    // ---- 판 만들기 --------------------------------------------------------------------------------

    private fun seal(index: Int) = Puzzle(index, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2))

    private fun single(puzzle: Puzzle, table: List<CipherEntry> = emptyList()) =
        Board("s", 3, listOf(puzzle, seal(1)), table)

    /** [signal]·[type] 퍼즐 하나를 D1 지시형 뒤에 둔 판(연쇄형의 입력은 D1 칸 번호). B1은 마지막 봉인 하나. */
    private fun boardWith(signal: SignalId, type: ClueType): Board {
        val d1 = Puzzle(0, SignalId.D1, ClueType.DIRECT, Target.Steering(SteeringSide.LEFT, 2, -470f, 500f))
        if (signal == SignalId.B1) return Board("s", 3, listOf(d1, Puzzle(1, SignalId.B1, type, Target.BrakePresses(3))), emptyList())
        val target = when (signal) {
            SignalId.C1 -> Target.Temperature(22)
            SignalId.C2 -> Target.FanSpeed(4)
            SignalId.C3 -> Target.Recirculation(true)
            SignalId.L1 -> Target.AmbientColor(NamedColor.GREEN)
            SignalId.L2 -> Target.Brightness(20)
            SignalId.L3 -> Target.ReadingLight(SeatPosition.REAR_LEFT)
            SignalId.D1 -> Target.Steering(SteeringSide.RIGHT, 3, -470f, 500f)
            SignalId.B1 -> error("unreachable")
        }
        val puzzle = Puzzle(
            1,
            signal,
            type,
            target,
            cipherSymbol = if (type == ClueType.CIPHER) CipherSymbol.STAR else null,
            chain = if (type == ClueType.CHAIN) ChainFormula(0, 1, 20) else null,
        )
        return Board("s", 3, listOf(d1, puzzle, seal(2)), emptyList())
    }
}
