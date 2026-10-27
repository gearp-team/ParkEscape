package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.signal.SeatPosition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.abs

/** 판 길이별 퍼즐 수 K와 구성(기획 2-1). 마지막 한 개는 항상 봉인(B1)이다. */
enum class BoardLength(val minutes: Int, val puzzleCount: Int, val direct: Int, val reasoning: Int) {
    THREE(3, 3, 1, 1),
    FIVE(5, 5, 2, 2),
    TEN(10, 8, 3, 4),
    ;

    val limitMs: Long get() = minutes * 60_000L

    companion object {
        fun of(minutes: Int): BoardLength =
            entries.firstOrNull { it.minutes == minutes } ?: throw IllegalArgumentException("unsupported board length: $minutes")
    }
}

/** 단서 유형. 지시형·암호형·연쇄형은 "추리" 여부로 나뉘고, 봉인은 마지막 B1이다. */
@Serializable
enum class ClueType { DIRECT, CIPHER, CHAIN, SEAL }

/** 앰비언트 목표 색(기획 2-1 L1). 판정: 색상차 ≤ 30°, 채도 ≥ 0.3. */
@Serializable
enum class NamedColor(val hue: Float) {
    RED(0f),
    ORANGE(30f),
    YELLOW(60f),
    GREEN(120f),
    BLUE(240f),
    PURPLE(280f),
}

/** 핸들 방향. SDK 값 기준으로 왼쪽이 음수다. */
@Serializable
enum class SteeringSide { LEFT, RIGHT }

/** 암호표 기호. 한 판 안에서 겹치지 않는다(최대 12개 = 10분 판 암호형 4개 × 진짜 1 + 미끼 2). */
@Serializable
enum class CipherSymbol(val glyph: String) {
    DIAMOND("◇"),
    TRIANGLE("△"),
    CIRCLE("○"),
    SQUARE("■"),
    STAR("☆"),
    HEART("♡"),
    CLUB("♣"),
    SPADE("♠"),
    BULLSEYE("◎"),
    DOWN_TRIANGLE("▽"),
    UP_TRIANGLE("▲"),
    DOT("●"),
}

/** 퍼즐 목표. 판정기는 이 값과 신호를 비교한다. */
@Serializable
sealed class Target {
    /** C1: 목표 온도(정수 ℃). 판정 ±0.5℃ */
    @Serializable
    @SerialName("temperature")
    data class Temperature(val celsius: Int) : Target()

    /** C2: 풍량 단수(일치) */
    @Serializable
    @SerialName("fanSpeed")
    data class FanSpeed(val level: Int) : Target()

    /** C3: 내기 순환이면 true(일치) */
    @Serializable
    @SerialName("recirculation")
    data class Recirculation(val on: Boolean) : Target()

    /** L1: 앰비언트 색(색상차 ≤ 30°, 채도 ≥ 0.3) */
    @Serializable
    @SerialName("ambientColor")
    data class AmbientColor(val color: NamedColor) : Target()

    /** L2: 앰비언트 밝기(일치) */
    @Serializable
    @SerialName("brightness")
    data class Brightness(val level: Int) : Target()

    /** L3: 해당 좌석 독서등 켜짐 */
    @Serializable
    @SerialName("readingLight")
    data class ReadingLight(val seat: SeatPosition) : Target()

    /**
     * D1: 방향·칸(1~3)에 1초 이상 머문 뒤 중앙(±5%)으로 복귀. 칸 경계는 판을 만들 때의 capability 범위로 정한다.
     */
    @Serializable
    @SerialName("steering")
    data class Steering(val side: SteeringSide, val bin: Int, val boundsMin: Float, val boundsMax: Float) : Target()

    /** B1: 브레이크 false→true 전환 횟수(누름 간격 8초, 전체 창 30초) */
    @Serializable
    @SerialName("brakePresses")
    data class BrakePresses(val count: Int) : Target()

    /** B1 미지원 차량의 봉인: 화면 "봉인 해제" 버튼(기획 3절 대체 경로) */
    @Serializable
    @SerialName("sealButton")
    data object SealButton : Target()
}

/** 암호표 한 줄의 뜻. */
@Serializable
sealed class CipherMeaning {
    @Serializable
    @SerialName("color")
    data class Color(val color: NamedColor) : CipherMeaning()

    @Serializable
    @SerialName("seat")
    data class Seat(val seat: SeatPosition) : CipherMeaning()

    @Serializable
    @SerialName("steering")
    data class Steering(val side: SteeringSide, val bin: Int) : CipherMeaning()

    /** 격벽(내외기): 닫힘 = 내기 순환(true), 열림 = 외기(false) */
    @Serializable
    @SerialName("recirculation")
    data class Recirculation(val on: Boolean) : CipherMeaning()

    @Serializable
    @SerialName("number")
    data class Number(val value: Int) : CipherMeaning()
}

@Serializable
data class CipherEntry(val symbol: CipherSymbol, val meaning: CipherMeaning)

/** 연쇄형 식: 목표 = (sourceIndex 퍼즐의 답) × multiplier + offset. 한 번만 계산하는 식(× 또는 ±)만 만든다. */
@Serializable
data class ChainFormula(val sourceIndex: Int, val multiplier: Int, val offset: Int) {
    fun apply(input: Int): Int = input * multiplier + offset

    /** 문장에 적히는 상수(부호 포함): "× m"이면 m, "± b"이면 b. */
    val constant: Int get() = if (multiplier != 1) multiplier else offset

    /** 문장에 실제로 보이는 수: "− 1"이면 1(ClueWriter는 빼기를 "− |b|"로 쓴다). */
    val writtenNumber: Int get() = abs(constant)

    /**
     * 계산하지 않아도 답이 보이는 식인가(F2-ui QA 실패 2·MINOR 3, 리더 결정): 곱하기 1·더하기 0, 결과가 문장에 보이는 수와 같음
     * (입력 0에 "+ b", 입력 1에 "× m", 입력 2에 "− 1"), 결과가 입력과 같음(입력 0에 "× m"). 생성기는 이런 식을 쓰지 않는다.
     */
    fun isTrivialFor(input: Int): Boolean {
        val result = apply(input)
        return (multiplier == 1 && offset == 0) || multiplier == 0 || result == writtenNumber || result == input
    }
}

/**
 * 퍼즐 하나. 단서 문장은 화면(F2-ui)이 이 구조로 만든다.
 * @property cipherSymbol 암호형이면 단서에 나오는 기호(암호표에서 뜻을 찾는다)
 * @property chain 연쇄형이면 식
 * @property fallbackFromChain 연쇄형을 만들 수 없어 지시형으로 바꿨으면 true(기획 2-1)
 */
@Serializable
data class Puzzle(
    val index: Int,
    val signal: SignalId,
    val type: ClueType,
    val target: Target,
    val cipherSymbol: CipherSymbol? = null,
    val chain: ChainFormula? = null,
    val fallbackFromChain: Boolean = false,
) {
    val key: SignalKey
        get() = SignalKey(signal, (target as? Target.ReadingLight)?.seat)

    /** 연쇄형의 입력이 될 수 있는 답(D1 칸 번호, C2 단수, C1 온도, L2 밝기). 그 밖은 null. */
    val numericAnswer: Int?
        get() = when (target) {
            is Target.Steering -> target.bin
            is Target.FanSpeed -> target.level
            is Target.Temperature -> target.celsius
            is Target.Brightness -> target.level
            else -> null
        }
}

/**
 * 한 판. 시드·참가 좌석·지원 신호 풀(+ 현재값)이 같으면 같은 판이다(기획 2-1). 퍼즐 수는 항상 판 길이의 K다.
 */
@Serializable
data class Board(
    val seed: String,
    val lengthMinutes: Int,
    val puzzles: List<Puzzle>,
    val cipherTable: List<CipherEntry>,
    val demo: Boolean = false,
) {
    /** 연쇄형 단서의 입력값(앞 퍼즐을 건너뛰었을 때 힌트 카드에 그대로 보여 준다). */
    fun chainInput(index: Int): Int? = puzzles.getOrNull(index)?.chain?.let { puzzles.getOrNull(it.sourceIndex)?.numericAnswer }
}
