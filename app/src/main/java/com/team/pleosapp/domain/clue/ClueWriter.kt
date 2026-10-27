package com.team.pleosapp.domain.clue

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.domain.puzzle.Board
import com.team.pleosapp.domain.puzzle.CipherMeaning
import com.team.pleosapp.domain.puzzle.CipherSymbol
import com.team.pleosapp.domain.puzzle.ClueType
import com.team.pleosapp.domain.puzzle.NamedColor
import com.team.pleosapp.domain.puzzle.Puzzle
import com.team.pleosapp.domain.puzzle.SignalId
import com.team.pleosapp.domain.puzzle.SteeringSide
import com.team.pleosapp.domain.puzzle.Target
import kotlin.math.abs

/**
 * S2 단서 카드 한 장(기획 2-1 단서 예, 테마 "우주선 비상 전원 복구").
 * @property typeLabel 유형 칩(지시형·암호형·연쇄형·마지막 봉인)
 * @property theme 칩 옆 작은 글자(계통 이름 · 회로 번호)
 * @property text 큰 단서 문장(한 줄 안팎)
 * @property symbol 암호형이면 단서에 나오는 기호(암호표에서 뜻을 찾는다)
 * @property hints 힌트 두 장: 첫째는 단서 해설, 둘째는 조작 위치
 * @property settingsNote 차량 설정 메뉴로 가야 하는 단서(L1·L2)의 안내
 */
data class Clue(
    val typeLabel: String,
    val theme: String,
    val text: String,
    val symbol: CipherSymbol?,
    val hints: List<String>,
    val settingsNote: String?,
)

/**
 * 판 구조(유형·신호·목표·암호표·연쇄식)를 한국어 단서로 바꾼다. 순수 함수라서 같은 판(= 같은 시드와 입력)이면 같은 문장이다.
 * 문장은 정차 중에만 보이지만 한 줄 안팎으로 짧게 쓴다(큰 글자 카드).
 *
 * 엔진은 연쇄형을 숫자 목표(C1·C2·L2)에만 만든다(기획 2-1 연쇄형 규칙). 그 밖의 신호에 연쇄형이 오거나 식이 없으면
 * 지시형 문장으로 쓴다. B1은 유형과 무관하게 봉인 문장이다.
 */
object ClueWriter {
    const val SETTINGS_NOTE = "조명을 켠 채로, 조명 설정으로 다녀오면 P 확인을 한 번 눌러요"

    /** 암호형 L1의 계통 이름(기호로 고른다: ◇ = 산소, 기획 2-1 단서 예). */
    private val SYSTEMS = listOf("산소", "항법", "통신", "동력", "냉각", "생명 유지")

    /** [brightnessRange]: 밝기 지원 범위. 지시형 L2가 양 끝이면 "최대로"·"최소로"라고 쓴다. */
    fun clue(board: Board, index: Int, brightnessRange: IntRange? = null): Clue {
        val puzzle = board.puzzles[index]
        val type = effectiveType(puzzle)
        val text = when (type) {
            ClueType.SEAL -> sealText(puzzle.target)
            ClueType.CIPHER -> cipherText(puzzle, puzzle.cipherSymbol!!)
            ClueType.CHAIN -> chainText(board, puzzle)
            ClueType.DIRECT -> directText(puzzle, brightnessRange)
        }
        return Clue(
            typeLabel = typeLabel(type),
            theme = theme(puzzle),
            text = text,
            symbol = if (type == ClueType.CIPHER) puzzle.cipherSymbol else null,
            hints = listOf(explainHint(board, puzzle, type), locationHint(puzzle, type)),
            settingsNote = if (puzzle.signal == SignalId.L1 || puzzle.signal == SignalId.L2) SETTINGS_NOTE else null,
        )
    }

    /** 문장을 실제로 쓰는 유형. 기호가 없는 암호형·식이 없는 연쇄형·B1 아닌 봉인은 지시형으로 쓴다. */
    fun effectiveType(puzzle: Puzzle): ClueType = when {
        puzzle.signal == SignalId.B1 -> ClueType.SEAL
        puzzle.type == ClueType.CIPHER && puzzle.cipherSymbol != null -> ClueType.CIPHER
        puzzle.type == ClueType.CHAIN && puzzle.chain != null && puzzle.signal in CHAIN_TARGETS -> ClueType.CHAIN
        else -> ClueType.DIRECT
    }

    fun typeLabel(type: ClueType): String = when (type) {
        ClueType.DIRECT -> "지시형"
        ClueType.CIPHER -> "암호형"
        ClueType.CHAIN -> "연쇄형"
        ClueType.SEAL -> "마지막 봉인"
    }

    /** 신호의 테마 이름. */
    fun system(signal: SignalId): String = when (signal) {
        SignalId.C1 -> "냉각 코일"
        SignalId.C2 -> "환기 팬"
        SignalId.C3 -> "격벽"
        SignalId.L1 -> "비상 조명"
        SignalId.L2 -> "조명 출력"
        SignalId.L3 -> "승무원 신호"
        SignalId.D1 -> "조타 키"
        SignalId.B1 -> "탈출 해치"
    }

    private fun theme(puzzle: Puzzle): String =
        if (puzzle.signal == SignalId.B1) system(SignalId.B1) else "${system(puzzle.signal)} · 비상 전원 ${puzzle.index + 1}번 회로"

    // ---- 문장 -----------------------------------------------------------------------------------

    private fun directText(puzzle: Puzzle, brightnessRange: IntRange?): String = when (val t = puzzle.target) {
        is Target.Temperature -> "냉각 코일을 ${t.celsius}℃로"
        is Target.FanSpeed -> "환기 팬을 ${t.level}단으로"
        is Target.Recirculation -> if (t.on) "외부 공기 차단 — 내기 순환" else "격벽 개방 — 외기 유입"
        is Target.AmbientColor -> "비상 조명을 ${colorWord(t.color)}으로"
        is Target.Brightness -> when (t.level) {
            brightnessRange?.last -> "조명 출력을 최대로"
            brightnessRange?.first -> "조명 출력을 최소로"
            else -> "조명 출력을 ${t.level}${roParticle(t.level)}"
        }
        is Target.ReadingLight -> "${seatName(t.seat)} 승무원: 독서등으로 신호"
        is Target.Steering -> "조타 키를 ${steeringName(t.side, t.bin)} 구간에 맞추고 중앙으로"
        is Target.BrakePresses, Target.SealButton -> sealText(t)
    }

    private fun cipherText(puzzle: Puzzle, symbol: CipherSymbol): String {
        val g = symbol.glyph
        return when (puzzle.target) {
            is Target.Temperature -> "냉각 코일을 ${g}℃로"
            is Target.FanSpeed -> "환기 팬을 ${g}단으로"
            is Target.Recirculation -> "격벽 상태 = $g"
            is Target.AmbientColor -> "비상 조명을 ${SYSTEMS[symbol.ordinal % SYSTEMS.size]} 계통 색($g)으로"
            is Target.Brightness -> "조명 출력 = $g"
            is Target.ReadingLight -> "$g 자리 승무원: 독서등으로 신호"
            is Target.Steering -> "조타 키를 $g 구간에 맞추고 중앙으로"
            is Target.BrakePresses, Target.SealButton -> sealText(puzzle.target)
        }
    }

    private fun chainText(board: Board, puzzle: Puzzle): String {
        val f = puzzle.chain!!
        val source = sourceName(board, f.sourceIndex)
        val expression = when {
            f.multiplier != 1 -> "$source × ${f.multiplier}"
            f.offset > 0 -> "${f.offset} + $source"
            else -> "$source − ${abs(f.offset)}"
        }
        val name = when (puzzle.signal) {
            SignalId.C1 -> "냉각 코일 온도"
            SignalId.C2 -> "환기 팬 단수"
            else -> "조명 출력"
        }
        return "$name = $expression"
    }

    private fun sealText(target: Target): String = when (target) {
        is Target.BrakePresses -> "브레이크를 ${countWord(target.count)} 번 밟아 해치 봉인 해제"
        else -> "봉인 해제 버튼을 눌러 해치를 여세요"
    }

    /** 연쇄식에 들어가는 앞 퍼즐 답의 이름. 판에 같은 신호가 둘이면 헷갈리지 않게 "n번 퍼즐 답"이라고 쓴다. */
    fun sourceName(board: Board, sourceIndex: Int): String {
        val source = board.puzzles[sourceIndex]
        val base = when (source.signal) {
            SignalId.D1 -> "조타 구간 번호"
            SignalId.C2 -> "환기 팬 단수"
            SignalId.C1 -> "냉각 코일 온도"
            SignalId.L2 -> "조명 출력"
            else -> "${sourceIndex + 1}번 퍼즐의 답"
        }
        val repeated = board.puzzles.count { it.signal == source.signal } > 1
        return if (repeated) "${sourceIndex + 1}번 퍼즐 답" else base
    }

    // ---- 힌트 -----------------------------------------------------------------------------------

    private fun explainHint(board: Board, puzzle: Puzzle, type: ClueType): String = when (type) {
        ClueType.CIPHER -> "암호표에서 ${puzzle.cipherSymbol!!.glyph} 기호를 찾아보세요"
        ClueType.CHAIN -> {
            val f = puzzle.chain!!
            val what = when (board.puzzles[f.sourceIndex].signal) {
                SignalId.D1 -> "맞춘 칸 번호"
                SignalId.C2 -> "맞춘 풍량 단수"
                SignalId.C1 -> "맞춘 온도"
                else -> "맞춘 밝기"
            }
            "${sourceName(board, f.sourceIndex)}${topicParticle(sourceName(board, f.sourceIndex))} ${f.sourceIndex + 1}번 퍼즐에서 $what${copula(what)}"
        }
        ClueType.SEAL -> if (puzzle.target is Target.BrakePresses) "브레이크를 끝까지 떼었다가 밟아야 한 번으로 세요" else "화면의 봉인 해제 버튼을 누르면 돼요"
        ClueType.DIRECT -> when (puzzle.signal) {
            SignalId.C1 -> "냉각 코일은 운전석 온도 설정이에요"
            SignalId.C2 -> "환기 팬은 공조 풍량이에요"
            SignalId.C3 -> "격벽은 내외기예요. 닫힘은 내기 순환, 열림은 외기예요"
            SignalId.L1 -> "비상 조명은 실내 앰비언트 조명이에요"
            SignalId.L2 -> "조명 출력은 실내 앰비언트 조명 밝기예요"
            SignalId.L3 -> "그 자리 독서등을 켜면 신호가 가요"
            SignalId.D1 -> "조타 키는 핸들이에요. 칸에 1초 머문 뒤 가운데로 돌려요"
            SignalId.B1 -> "브레이크를 끝까지 떼었다가 밟아야 한 번으로 세요"
        }
    }

    private fun locationHint(puzzle: Puzzle, type: ClueType): String = when (puzzle.signal) {
        SignalId.C1 -> "운전석 온도는 화면 아래 공조 바에서 바꿔요"
        SignalId.C2 -> "풍량은 공조 바의 팬 단계에서 바꿔요"
        SignalId.C3 -> "공조 화면의 내기 순환 버튼이에요"
        SignalId.L1 -> "실내 조명 색은 차 설정의 조명 메뉴에 있어요"
        SignalId.L2 -> "실내 조명 밝기는 차 설정의 조명 메뉴에 있어요"
        SignalId.L3 -> {
            val seat = (puzzle.target as? Target.ReadingLight)?.seat
            if (type == ClueType.DIRECT && seat != null) "${seatName(seat)} 머리 위 독서등 버튼이에요" else "자리마다 머리 위에 독서등 버튼이 있어요"
        }
        SignalId.D1 -> "핸들을 살짝 돌리면 바늘이 움직여요. 칸 안에서 1초 기다려요"
        SignalId.B1 -> if (puzzle.target is Target.BrakePresses) "누름 사이는 8초를 넘기지 마세요" else "화면 가운데 버튼이에요"
    }

    // ---- 짧은 글 --------------------------------------------------------------------------------

    /** "풀린 퍼즐" 줄과 결과 화면에 쓰는 짧은 결과. */
    fun solvedLabel(puzzle: Puzzle): String = when (val t = puzzle.target) {
        is Target.Temperature -> "냉각 코일 ${t.celsius}℃"
        is Target.FanSpeed -> "환기 팬 ${t.level}단"
        is Target.Recirculation -> if (t.on) "격벽 닫힘" else "격벽 열림"
        is Target.AmbientColor -> "비상 조명 ${colorName(t.color)}"
        is Target.Brightness -> "조명 출력 ${t.level}"
        is Target.ReadingLight -> "${seatName(t.seat)} 신호"
        is Target.Steering -> "조타 키 ${steeringName(t.side, t.bin)}"
        is Target.BrakePresses, Target.SealButton -> "해치 봉인 해제"
    }

    /** 앞 퍼즐을 건너뛰었을 때 연쇄형 입력값을 그대로 보여 주는 글(기획 2-1). 연쇄형이 아니거나 건너뛰지 않았으면 null. */
    fun chainInputReveal(board: Board, index: Int, skipped: Set<Int>): String? {
        val puzzle = board.puzzles.getOrNull(index) ?: return null
        if (effectiveType(puzzle) != ClueType.CHAIN) return null
        val f = puzzle.chain ?: return null
        if (f.sourceIndex !in skipped) return null
        val value = board.chainInput(index) ?: return null
        return "${f.sourceIndex + 1}번 퍼즐을 건너뛰어 값을 알려 드려요: ${sourceName(board, f.sourceIndex)} = $value"
    }

    /** 암호표 타일 아래 글자. */
    fun meaning(meaning: CipherMeaning): String = when (meaning) {
        is CipherMeaning.Color -> colorName(meaning.color)
        is CipherMeaning.Seat -> seatName(meaning.seat)
        is CipherMeaning.Steering -> steeringName(meaning.side, meaning.bin)
        is CipherMeaning.Recirculation -> if (meaning.on) "닫힘" else "열림"
        is CipherMeaning.Number -> meaning.value.toString()
    }

    fun seatName(seat: SeatPosition): String = when (seat) {
        SeatPosition.DRIVER -> "운전석"
        SeatPosition.PASSENGER -> "조수석"
        SeatPosition.REAR_LEFT -> "2열 왼쪽"
        SeatPosition.REAR_CENTER -> "2열 가운데"
        SeatPosition.REAR_RIGHT -> "2열 오른쪽"
    }

    fun steeringName(side: SteeringSide, bin: Int): String = "${if (side == SteeringSide.LEFT) "왼쪽" else "오른쪽"} ${bin}번"

    fun colorName(color: NamedColor): String = when (color) {
        NamedColor.RED -> "빨강"
        NamedColor.ORANGE -> "주황"
        NamedColor.YELLOW -> "노랑"
        NamedColor.GREEN -> "초록"
        NamedColor.BLUE -> "파랑"
        NamedColor.PURPLE -> "보라"
    }

    /** "보라색"처럼 문장 안에서 쓰는 색 이름. */
    private fun colorWord(color: NamedColor): String = when (color) {
        NamedColor.RED -> "빨간색"
        NamedColor.ORANGE -> "주황색"
        NamedColor.YELLOW -> "노란색"
        NamedColor.GREEN -> "초록색"
        NamedColor.BLUE -> "파란색"
        NamedColor.PURPLE -> "보라색"
    }

    private fun countWord(count: Int): String = when (count) {
        1 -> "한"
        2 -> "두"
        3 -> "세"
        4 -> "네"
        5 -> "다섯"
        else -> count.toString()
    }

    /**
     * 숫자 뒤 조사 "로/으로". 읽는 소리의 끝 받침이 ㄹ이거나 없으면 "로"다.
     * 끝자리 3(삼)·6(육), 0(영), 10의 배수(십·백·천)는 받침이 있어 "으로"다.
     */
    private fun roParticle(n: Int): String = if (n % 10 == 0 || n % 10 == 3 || n % 10 == 6) "으로" else "로"

    private fun hasFinalConsonant(word: String): Boolean {
        val c = word.lastOrNull() ?: return false
        if (c !in '가'..'힣') return false
        return (c - '가') % 28 != 0
    }

    private fun topicParticle(word: String) = if (hasFinalConsonant(word)) "은" else "는"

    private fun copula(word: String) = if (hasFinalConsonant(word)) "이에요" else "예요"

    private val CHAIN_TARGETS = setOf(SignalId.C1, SignalId.C2, SignalId.L2)
}
