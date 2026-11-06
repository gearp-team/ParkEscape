package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.light.hueDistance
import com.team.pleosapp.data.signal.SeatPosition
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor

/**
 * 판 생성 입력. 기획 2-1의 세 입력(시드·참가 좌석·지원 신호 풀)에 현재값([current])과 설정을 더했다.
 * 현재값은 "현재값과 달라야 한다" 조건과 원상 복구 스냅샷에 쓰이므로, 같은 판을 다시 만들려면 이것도 같아야 한다.
 */
data class GeneratorInput(
    val seed: String,
    val lengthMinutes: Int,
    /** S1 좌석도에서 확정한 참가 좌석(운전석 포함) */
    val participants: Set<SeatPosition>,
    val pool: SignalPool,
    val current: VehicleSignals,
    /** "내 차는 전기차" 설정. false면 공조 퍼즐 판당 2개·풍량 상한 = 범위 상한의 절반 */
    val isEv: Boolean = false,
    /** 디버그 시드 고정(시연 모드): 신호 풀을 10종 전부로 고정하고 앰비언트 목표를 VC 프리셋 색으로 제한한다 */
    val demoMode: Boolean = false,
)

sealed class GenerationResult {
    data class Success(val board: Board) : GenerationResult()

    /**
     * 이 조건으로는 고른 판 길이의 퍼즐 수 K를 제약 안에서 채울 수 없다. [maxPuzzles]는 만들 수 있는 최대 퍼즐 수(봉인 포함).
     * 화면은 [PuzzleGenerator.availableLengths]로 가능한 길이만 열어 두고, 3분 판도 안 되면 "이 차에서는 지원하는 조작이 부족해요"를 띄운다.
     */
    data class Unavailable(val maxPuzzles: Int) : GenerationResult()
}

/**
 * 생성형 퍼즐 판(기획 2-1). 같은 입력이면 같은 판(조합·순서·목표값·암호표)을 만든다.
 *
 * 규칙 요약
 * - K: 3분 3개 / 5분 5개 / 10분 8개. 구성은 [BoardLength]. 마지막은 항상 B1 봉인(브레이크 미지원이면 화면 버튼).
 * - D1은 판당 1개. 비EV는 공조(C1~C3) 퍼즐 판당 2개, 풍량 목표는 범위 상한의 절반 이하.
 * - L3는 운전석이 아닌 참가 좌석 가운데 독서등 area가 있고 지금 꺼져 있는 좌석만.
 * - C4(앞유리 제상)·L4(전조등)는 목표가 "켜짐" 하나다. 판 시작 때 꺼져 있을 때만 내고(켜져 있거나 모르면 뺀다) 판당 1개, 지시형·암호형만.
 * - C4와 C3(내외기)는 같은 판에 내지 않는다(리더 결정 2026-11-06). 실차는 앞유리 제상을 켜면 외기·A/C로 바뀔 수 있고 김 서림에는
 *   외기가 맞아서, "제상 뒤 내기 순환" 판은 막히거나 잘못된 조합을 가르친다. 둘 다 낼 수 있으면 판마다 한쪽만 고르고([Rules.variants]),
 *   시작 가능 규칙도 두 경우 가운데 더 많이 채우는 쪽으로 센다.
 *   C4는 공조라 비EV 공조 판당 2개에 들어간다.
 * - C1 목표: capability 양 끝에서 1℃ 이상 안쪽 ∩ 18~28℃ 정수, 현재값과 2℃ 이상 차이.
 * - 연쇄형: 앞 퍼즐의 답(D1 칸, C1 온도, C2 단수, L2 밝기)으로 C1·C2·L2 목표를 계산한다. 식은 "× m" 또는 "± b" 한 번.
 *   결과가 범위·제약을 지키는 식이 없으면 지시형으로 바꾼다.
 * - 현재값을 모르는(null) 신호는 그 판에서 뺀다(리더 결정: 풍량 null → C2 제외를 모든 신호에 적용).
 * - 서로 다른 신호로 K를 못 채우면 같은 신호를 한 번 더 쓸 수 있다(신호당 최대 2개, 비EV 공조 합계 2개). 같은 신호는 연달아 오지 않는다.
 * - 시작 가능 여부는 판 길이별 실현 가능성이다(리더 결정): 신호별로 실제로 낼 수 있는 목표 수로 K(봉인 포함)를 채울 수 있고,
 *   봉인을 뺀 서로 다른 신호가 2종 이상인 길이만 가능하다. 가능한 길이는 어떤 시드로든 생성에 성공한다(속성 테스트).
 *   봉인은 브레이크 미지원이면 화면 버튼이라 항상 1개로 센다. 짧은 판은 만들지 않는다.
 */
object PuzzleGenerator {
    const val NON_EV_CLIMATE_MAX = 2

    /** 다시 쓸 수 있는 신호(C1·C2·C3·L1·L2)의 판당 최대 개수 */
    const val MAX_PER_KIND = 2
    val COMFORT_CELSIUS = 18..28
    const val MIN_TEMPERATURE_GAP = 2f
    const val CIPHER_DECOYS = 2
    /** 다양성(암호형·연쇄형)을 채울 때까지 다시 뽑는 횟수. C4·L4가 늘어 연쇄형이 드물어져 40에서 80으로 올렸다(시드 200개 속성 테스트). */
    private const val MAX_ATTEMPTS = 80

    private val DEMO_COLORS = listOf(NamedColor.PURPLE, NamedColor.BLUE)
    private val CHAIN_TARGETS = setOf(SignalId.C1, SignalId.C2, SignalId.L2)
    private val CHAIN_SOURCES = setOf(SignalId.D1, SignalId.C1, SignalId.C2, SignalId.L2)
    private val REPEATABLE = setOf(SignalId.C1, SignalId.C2, SignalId.C3, SignalId.L1, SignalId.L2)
    private val PASSENGER_SEATS = listOf(SeatPosition.PASSENGER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT)

    fun generate(input: GeneratorInput): GenerationResult {
        val length = BoardLength.of(input.lengthMinutes)
        val pool = if (input.demoMode) SignalPool.demo(input.pool) else input.pool
        val rules = Rules(pool, input.isEv, input.demoMode, input.current.targetTemperature, input.current.fanSpeed)
        val participants = input.participants.sorted()
        val kinds = rules.candidates(Projection(input.current), participants)
        val maxPuzzles = rules.contentCapacity(kinds) + 1
        if (maxPuzzles < length.puzzleCount) return GenerationResult.Unavailable(maxPuzzles)

        if (input.demoMode && input.seed == Seeds.DEMO && length == BoardLength.FIVE) {
            return GenerationResult.Success(demoBoard(rules, input, participants))
        }
        if (input.demoMode && input.seed == Seeds.DEMO_SAFETY && length == BoardLength.FIVE) {
            return GenerationResult.Success(safetyDemoBoard(rules))
        }

        val rng = SeededRandom(Seeds.hash(input.seed))
        val chainPossible = kinds.any { it.signal in CHAIN_TARGETS } && kinds.any { it.signal in CHAIN_SOURCES } && kinds.size >= 2
        var best: Board? = null
        repeat(MAX_ATTEMPTS) {
            val board = buildBoard(rules, input, length, kinds, rng) ?: return@repeat
            if (meetsVariety(board, length, chainPossible)) return GenerationResult.Success(board)
            if (best == null || score(board) > score(best!!)) best = board
        }
        return best?.let { GenerationResult.Success(it) } ?: GenerationResult.Unavailable(maxPuzzles)
    }

    /** 이 조건으로 만들 수 있는 최대 퍼즐 수(봉인 포함). */
    fun maxPuzzleCount(
        participants: Set<SeatPosition>,
        pool: SignalPool,
        current: VehicleSignals,
        isEv: Boolean = false,
        demoMode: Boolean = false,
    ): Int {
        val rules = Rules(if (demoMode) SignalPool.demo(pool) else pool, isEv, demoMode, current.targetTemperature, current.fanSpeed)
        return rules.contentCapacity(rules.candidates(Projection(current), participants.sorted())) + 1
    }

    /** 고를 수 있는 판 길이(홈 S0에서 이 길이만 활성). 비어 있으면 "이 차에서는 지원하는 조작이 부족해요". */
    fun availableLengths(
        participants: Set<SeatPosition>,
        pool: SignalPool,
        current: VehicleSignals,
        isEv: Boolean = false,
        demoMode: Boolean = false,
    ): Set<BoardLength> {
        val max = maxPuzzleCount(participants, pool, current, isEv, demoMode)
        return BoardLength.entries.filter { it.puzzleCount <= max }.toSet()
    }

    // ---- 한 번 시도 --------------------------------------------------------------------------

    private fun buildBoard(rules: Rules, input: GeneratorInput, length: BoardLength, kinds: List<SignalKey>, rng: SeededRandom): Board? {
        val contentCount = length.puzzleCount - 1
        // C3·C4가 함께 후보면 한쪽만 쓰는 후보 목록을 고른다(채울 수 있는 쪽만. 충돌이 없으면 rng를 쓰지 않아 기존 판이 그대로다)
        val feasible = rules.variants(kinds).filter { rules.capacityOf(it) >= contentCount }
        val usable = when (feasible.size) {
            0 -> return null
            1 -> feasible.single()
            else -> rng.pick(feasible)
        }
        val sequence = chooseSignals(rules, usable, contentCount, rng)
        if (sequence.size < contentCount) return null
        val reasoning = minOf(length.reasoning, sequence.size)
        val slots = rng.shuffled(List(reasoning) { true } + List(sequence.size - reasoning) { false })

        val projection = Projection(input.current)
        val symbols = ArrayDeque(rng.shuffled(CipherSymbol.entries))
        val table = mutableListOf<CipherEntry>()
        val puzzles = mutableListOf<Puzzle>()

        for ((i, key) in sequence.withIndex()) {
            // 같은 신호가 뒤에 또 나오면, 그때도 목표를 낼 수 있는 값만 고른다(C1 2℃ 간격, C2 후보 수)
            val repeatsLater = key in sequence.subList(i + 1, sequence.size)
            val wantsReasoning = slots[i]
            val chainable = key.signal in CHAIN_TARGETS && puzzles.any { it.numericAnswer != null && it.signal in CHAIN_SOURCES }
            val type = when {
                !wantsReasoning -> ClueType.DIRECT
                chainable && rng.nextBoolean() -> ClueType.CHAIN
                else -> ClueType.CIPHER
            }
            val puzzle = when (type) {
                ClueType.CHAIN -> chainPuzzle(rules, projection, key, i, puzzles, repeatsLater, rng)
                    ?: directPuzzle(rules, projection, key, i, repeatsLater, rng)?.copy(fallbackFromChain = true)
                ClueType.CIPHER -> directPuzzle(rules, projection, key, i, repeatsLater, rng)?.let { base ->
                    val symbol = symbols.removeFirstOrNull() ?: return@let base // 기호가 모자라면 지시형으로 둔다
                    table += CipherEntry(symbol, meaningOf(base.target))
                    decoys(rules, base.target, rng).take(CIPHER_DECOYS).forEach { meaning ->
                        symbols.removeFirstOrNull()?.let { table += CipherEntry(it, meaning) }
                    }
                    base.copy(type = ClueType.CIPHER, cipherSymbol = symbol)
                }
                else -> directPuzzle(rules, projection, key, i, repeatsLater, rng)
            } ?: return null
            puzzles += puzzle
            projection.apply(puzzle.target)
        }
        puzzles += sealPuzzle(rules, puzzles.size, rng)
        return Board(
            seed = input.seed,
            lengthMinutes = length.minutes,
            puzzles = puzzles,
            cipherTable = rng.shuffled(table),
            demo = input.demoMode,
        )
    }

    /**
     * 신호 순서를 정한다. 먼저 서로 다른 신호(L3는 좌석이 달라도 한 신호)를 하나씩 쓰고, 모자라면 같은 신호를 한 번 더 쓴다.
     * 상한은 [Rules.capacityOf]와 같다: 신호마다 실제로 낼 수 있는 목표 수(최대 2), 비EV 공조 합계 2, D1 1.
     * [kinds]에는 C3·C4가 함께 들어 있지 않다([buildBoard]가 [Rules.variants]에서 하나를 고른다).
     * 그래서 가능한 길이면 항상 [count]개를 채우고, 같은 신호가 연달아 오지 않게 늘어놓을 수 있다.
     */
    private fun chooseSignals(rules: Rules, kinds: List<SignalKey>, count: Int, rng: SeededRandom): List<SignalKey> {
        val keyLimit = kinds.associateWith { rules.keyCapacity(it) }
        val chosen = mutableListOf<SignalKey>()
        fun allowed(key: SignalKey): Boolean = when {
            chosen.count { it == key } >= keyLimit.getValue(key) -> false
            chosen.count { it.signal == key.signal } >= MAX_PER_KIND -> false
            key.signal.isClimate && !rules.isEv -> chosen.count { it.signal.isClimate } < NON_EV_CLIMATE_MAX
            else -> true
        }
        for (key in rng.shuffled(kinds)) {
            if (chosen.size == count) break
            if (chosen.none { it.signal == key.signal } && allowed(key)) chosen += key
        }
        while (chosen.size < count) {
            val options = kinds.filter { allowed(it) }
            if (options.isEmpty()) break
            chosen += rng.pick(options)
        }
        return separateRepeats(rng.shuffled(chosen))
    }

    /**
     * 같은 신호(L3는 좌석이 달라도 같은 신호)가 연달아 오지 않게 다시 늘어놓는다(결정적). 남은 것 가운데 직전과 다른 신호이면서
     * 가장 많이 남은 신호를 고르고, 같으면 원래(섞인) 순서가 앞선 것을 고른다. 반복이 없으면 순서가 그대로다.
     * 신호마다 2개 이하이고 내용이 4개 이상이거나, 내용 2개가 서로 다른 신호면 항상 떼어 놓을 수 있다.
     */
    private fun separateRepeats(list: List<SignalKey>): List<SignalKey> {
        val remaining = list.toMutableList()
        val out = mutableListOf<SignalKey>()
        while (remaining.isNotEmpty()) {
            val counts = remaining.groupingBy { it.signal }.eachCount()
            val last = out.lastOrNull()?.signal
            val pick = remaining.filter { it.signal != last }.maxByOrNull { counts.getValue(it.signal) } ?: remaining.first()
            out += pick
            remaining.remove(pick)
        }
        return out
    }

    private fun directPuzzle(rules: Rules, p: Projection, key: SignalKey, index: Int, repeatsLater: Boolean, rng: SeededRandom): Puzzle? {
        val target: Target = when (key.signal) {
            SignalId.C1 -> rules.temperatureTargets(p.temperature, repeatsLater).takeIf { it.isNotEmpty() }?.let { Target.Temperature(rng.pick(it)) }
            SignalId.C2 -> rules.fanTargets(p.fan, repeatsLater).takeIf { it.isNotEmpty() }?.let { Target.FanSpeed(rng.pick(it)) }
            SignalId.C3 -> p.recirculation?.let { Target.Recirculation(!it) }
            SignalId.L1 -> rules.colorTargets(p).takeIf { it.isNotEmpty() }?.let { Target.AmbientColor(rng.pick(it)) }
            SignalId.L2 -> rules.brightnessDirect(p.brightness)?.let { Target.Brightness(it) }
            SignalId.L3 -> key.seat?.let { Target.ReadingLight(it) }
            SignalId.C4 -> Target.Defroster(on = true).takeIf { p.defroster == false }
            SignalId.L4 -> Target.LowBeam(on = true).takeIf { p.lowBeam == false }
            SignalId.D1 -> rules.steeringBounds()?.let { (min, max) ->
                Target.Steering(rng.pick(SteeringSide.entries), rng.nextIntIn(1, 3), min, max)
            }
            SignalId.B1 -> null
        } ?: return null
        return Puzzle(index, key.signal, ClueType.DIRECT, target)
    }

    private fun chainPuzzle(
        rules: Rules,
        p: Projection,
        key: SignalKey,
        index: Int,
        earlier: List<Puzzle>,
        repeatsLater: Boolean,
        rng: SeededRandom,
    ): Puzzle? {
        val valid = when (key.signal) {
            SignalId.C1 -> rules.temperatureTargets(p.temperature, repeatsLater)
            SignalId.C2 -> rules.fanTargets(p.fan, repeatsLater)
            SignalId.L2 -> rules.brightnessTargets(p.brightness)
            else -> return null
        }.toSet()
        val options = mutableListOf<ChainFormula>()
        for (source in earlier) {
            if (source.signal !in CHAIN_SOURCES) continue
            val x = source.numericAnswer ?: continue
            for ((m, b) in formulaShapes(key.signal)) {
                val formula = ChainFormula(source.index, m, b)
                if (formula.apply(x) in valid && !formula.isTrivialFor(x)) options += formula
            }
        }
        if (options.isEmpty()) return null
        val formula = rng.pick(options)
        val value = formula.apply(earlier[formula.sourceIndex].numericAnswer!!)
        val target = when (key.signal) {
            SignalId.C1 -> Target.Temperature(value)
            SignalId.C2 -> Target.FanSpeed(value)
            else -> Target.Brightness(value)
        }
        return Puzzle(index, key.signal, ClueType.CHAIN, target, chain = formula)
    }

    /** 한 번만 계산하는 식: "× m"(m ≥ 2) 또는 "± b"(b ≠ 0). 밝기는 0~100이라 ×5·×10·×20·×25도 쓴다. */
    private fun formulaShapes(signal: SignalId): List<Pair<Int, Int>> = buildList {
        val maxOffset = if (signal == SignalId.L2) 100 else 40
        for (b in -maxOffset..maxOffset) if (b != 0) add(1 to b)
        val multipliers = if (signal == SignalId.L2) listOf(2, 3, 5, 10, 20, 25) else listOf(2, 3)
        for (m in multipliers) add(m to 0)
    }

    private fun sealPuzzle(rules: Rules, index: Int, rng: SeededRandom): Puzzle {
        val target = when {
            !rules.pool.brake -> Target.SealButton
            rules.demo -> Target.BrakePresses(2)
            else -> Target.BrakePresses(rng.nextIntIn(2, 4))
        }
        return Puzzle(index, SignalId.B1, ClueType.SEAL, target)
    }

    private fun meaningOf(target: Target): CipherMeaning = when (target) {
        is Target.Temperature -> CipherMeaning.Number(target.celsius)
        is Target.FanSpeed -> CipherMeaning.Number(target.level)
        is Target.Brightness -> CipherMeaning.Number(target.level)
        is Target.Recirculation -> CipherMeaning.Recirculation(target.on)
        is Target.AmbientColor -> CipherMeaning.Color(target.color)
        is Target.ReadingLight -> CipherMeaning.Seat(target.seat)
        is Target.Steering -> CipherMeaning.Steering(target.side, target.bin)
        is Target.Defroster -> CipherMeaning.Switch(target.on)
        is Target.LowBeam -> CipherMeaning.Switch(target.on)
        is Target.BrakePresses, Target.SealButton -> error("seal puzzles are never ciphers")
    }

    /** 암호표 미끼: 같은 종류의 다른 뜻. */
    private fun decoys(rules: Rules, target: Target, rng: SeededRandom): List<CipherMeaning> = rng.shuffled(
        when (target) {
            is Target.Temperature -> COMFORT_CELSIUS.filter { it != target.celsius }.map { CipherMeaning.Number(it) }
            is Target.FanSpeed -> rules.pool.fanSpeed?.let { b -> (b.min..b.max).filter { it != target.level }.map { CipherMeaning.Number(it) } }.orEmpty()
            is Target.Brightness -> rules.brightnessTargets(null).filter { it != target.level }.map { CipherMeaning.Number(it) }
            is Target.Recirculation -> listOf(CipherMeaning.Recirculation(!target.on))
            is Target.AmbientColor -> NamedColor.entries.filter { it != target.color }.map { CipherMeaning.Color(it) }
            is Target.ReadingLight -> PASSENGER_SEATS.filter { it != target.seat }.map { CipherMeaning.Seat(it) }
            is Target.Steering -> SteeringSide.entries.flatMap { side -> (1..3).map { side to it } }
                .filter { it != target.side to target.bin }
                .map { (side, bin) -> CipherMeaning.Steering(side, bin) }
            is Target.Defroster -> listOf(CipherMeaning.Switch(!target.on))
            is Target.LowBeam -> listOf(CipherMeaning.Switch(!target.on))
            is Target.BrakePresses, Target.SealButton -> emptyList()
        },
    )

    private fun meetsVariety(board: Board, length: BoardLength, chainPossible: Boolean): Boolean {
        val types = board.puzzles.map { it.type }.toSet()
        return when {
            length.reasoning >= 2 -> ClueType.CIPHER in types && (ClueType.CHAIN in types || !chainPossible)
            else -> ClueType.CIPHER in types || ClueType.CHAIN in types
        }
    }

    private fun score(board: Board): Int =
        board.puzzles.size * 10 + board.puzzles.map { it.type }.filter { it == ClueType.CIPHER || it == ClueType.CHAIN }.toSet().size

    // ---- 시연 판(기획 5절) ------------------------------------------------------------------

    /**
     * 시드 demo5 · 5분 판: ① D1 지시형 "왼쪽 2번 칸" → ② L1 암호형 ◇=보라 → ③ L3 지시형 2열 오른쪽
     * (2열 오른쪽이 참가 좌석이 아니면 C2 연쇄형 "조타 구간 × 2" = 4단) → ④ C1 연쇄형 "20 + 조타 구간" = 22℃ → ⑤ B1 두 번.
     * ③·④가 현재값 조건을 어기면(예: 운전석 온도가 이미 22℃) 기획 2-1대로 지시형으로 바꾼다.
     */
    private fun demoBoard(rules: Rules, input: GeneratorInput, participants: List<SeatPosition>): Board {
        val (min, max) = rules.steeringBounds() ?: (-470f to 500f)
        val p = Projection(input.current)
        val puzzles = mutableListOf<Puzzle>()

        puzzles += Puzzle(0, SignalId.D1, ClueType.DIRECT, Target.Steering(SteeringSide.LEFT, 2, min, max))
        p.apply(puzzles.last().target)

        puzzles += Puzzle(1, SignalId.L1, ClueType.CIPHER, Target.AmbientColor(NamedColor.PURPLE), cipherSymbol = CipherSymbol.DIAMOND)
        p.apply(puzzles.last().target)

        val rearRight = SeatPosition.REAR_RIGHT
        puzzles += if (rearRight in participants && rearRight in rules.pool.readingLightSeats &&
            input.current.readingLights[rearRight] != true
        ) {
            Puzzle(2, SignalId.L3, ClueType.DIRECT, Target.ReadingLight(rearRight))
        } else {
            demoChain(SignalId.C2, ChainFormula(0, 2, 0), rules.fanTargets(p.fan), 2, puzzles)
        }
        p.apply(puzzles.last().target)

        puzzles += demoChain(SignalId.C1, ChainFormula(0, 1, 20), rules.temperatureTargets(p.temperature), 3, puzzles)
        p.apply(puzzles.last().target)

        puzzles += Puzzle(4, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2))
        val table = listOf(
            CipherEntry(CipherSymbol.DIAMOND, CipherMeaning.Color(NamedColor.PURPLE)),
            CipherEntry(CipherSymbol.CIRCLE, CipherMeaning.Color(NamedColor.BLUE)),
            CipherEntry(CipherSymbol.TRIANGLE, CipherMeaning.Color(NamedColor.GREEN)),
        )
        return Board(input.seed, BoardLength.FIVE.minutes, puzzles, table, demo = true)
    }

    /**
     * 시드 demo-safety · 5분 판(위급할 때 쓰는 조작을 손에 익히는 시연): ① L4 지시형 전조등 → ② D1 지시형 "왼쪽 2번 칸"
     * → ③ L1 암호형 ◇ = 보라(비상 조명) → ④ C4 암호형 ■ = 켜짐(앞유리 제상) → ⑤ B1 두 번.
     * 지시형 2(L4·D1) · 암호형 2(L1·C4) · 봉인 1이고, C3는 넣지 않는다(C4와 같은 판 금지). 팁은 ①·④에서 나온다.
     * 0단계 초기값(앰비언트 청록, 제상 꺼짐, 전조등 꺼짐, 핸들 중앙)을 가정한다. 전조등·제상이 이미 켜져 있으면 그 퍼즐은 시작하자마자
     * 풀린다(시작 전 0단계 경고가 알려 준다). 앰비언트 목표는 VC 프리셋으로 재현되는 보라다.
     */
    private fun safetyDemoBoard(rules: Rules): Board {
        val (min, max) = rules.steeringBounds() ?: (-470f to 500f)
        val puzzles = listOf(
            Puzzle(0, SignalId.L4, ClueType.DIRECT, Target.LowBeam(on = true)),
            Puzzle(1, SignalId.D1, ClueType.DIRECT, Target.Steering(SteeringSide.LEFT, 2, min, max)),
            Puzzle(2, SignalId.L1, ClueType.CIPHER, Target.AmbientColor(NamedColor.PURPLE), cipherSymbol = CipherSymbol.DIAMOND),
            Puzzle(3, SignalId.C4, ClueType.CIPHER, Target.Defroster(on = true), cipherSymbol = CipherSymbol.SQUARE),
            Puzzle(4, SignalId.B1, ClueType.SEAL, Target.BrakePresses(2)),
        )
        val table = listOf(
            CipherEntry(CipherSymbol.DIAMOND, CipherMeaning.Color(NamedColor.PURPLE)),
            CipherEntry(CipherSymbol.SQUARE, CipherMeaning.Switch(true)),
            CipherEntry(CipherSymbol.CIRCLE, CipherMeaning.Color(NamedColor.BLUE)),
            CipherEntry(CipherSymbol.STAR, CipherMeaning.Switch(false)),
            CipherEntry(CipherSymbol.TRIANGLE, CipherMeaning.Color(NamedColor.GREEN)),
        )
        return Board(Seeds.DEMO_SAFETY, BoardLength.FIVE.minutes, puzzles, table, demo = true)
    }

    /** 시연 판의 연쇄형. 결과가 제약 밖이면 계산값에 가장 가까운 유효 목표의 지시형으로 바꾼다. */
    private fun demoChain(signal: SignalId, formula: ChainFormula, valid: List<Int>, index: Int, earlier: List<Puzzle>): Puzzle {
        val input = earlier[formula.sourceIndex].numericAnswer!!
        val value = formula.apply(input)
        fun target(v: Int): Target = if (signal == SignalId.C1) Target.Temperature(v) else Target.FanSpeed(v)
        if (value in valid && !formula.isTrivialFor(input)) return Puzzle(index, signal, ClueType.CHAIN, target(value), chain = formula)
        val nearest = valid.minWithOrNull(compareBy<Int> { abs(it - value) }.thenBy { -it })
            ?: value
        return Puzzle(index, signal, ClueType.DIRECT, target(nearest), fallbackFromChain = true)
    }

    // ---- 신호별 제약 ------------------------------------------------------------------------

    /** 판을 만드는 동안 각 신호의 "예상 현재값". 퍼즐이 풀리면 그 신호는 목표값이 된다. */
    private class Projection(current: VehicleSignals) {
        var temperature: Float? = current.targetTemperature
        var fan: Int? = current.fanSpeed
        var recirculation: Boolean? = current.recirculation
        var colorArgb: Int? = current.ambientColor
        var colorTarget: NamedColor? = null
        var brightness: Int? = current.ambientBrightness
        val readingLights: MutableMap<SeatPosition, Boolean?> = current.readingLights.toMutableMap()
        var steering: Float? = current.steeringAngle
        var defroster: Boolean? = current.defroster
        var lowBeam: Boolean? = current.lowBeam

        fun apply(target: Target) {
            when (target) {
                is Target.Temperature -> temperature = target.celsius.toFloat()
                is Target.FanSpeed -> fan = target.level
                is Target.Recirculation -> recirculation = target.on
                is Target.AmbientColor -> colorTarget = target.color
                is Target.Brightness -> brightness = target.level
                is Target.ReadingLight -> readingLights[target.seat] = true
                is Target.Steering -> steering = 0f // 중앙 복귀까지가 퍼즐이다
                is Target.Defroster -> defroster = target.on
                is Target.LowBeam -> lowBeam = target.on
                is Target.BrakePresses, Target.SealButton -> Unit
            }
        }
    }

    private class Rules(
        val pool: SignalPool,
        val isEv: Boolean,
        val demo: Boolean,
        val currentTemperature: Float?,
        val currentFan: Int?,
    ) {

        /** 이 판에 쓸 수 있는 신호(현재값을 아는 것만). L3는 좌석마다 하나. 순서는 결정적이다. */
        fun candidates(p: Projection, participants: List<SeatPosition>): List<SignalKey> = buildList {
            if (p.temperature != null && temperatureTargets(p.temperature).isNotEmpty()) add(SignalKey(SignalId.C1))
            if (p.fan != null && fanTargets(p.fan).isNotEmpty()) add(SignalKey(SignalId.C2))
            if (pool.recirculation && p.recirculation != null) add(SignalKey(SignalId.C3))
            if (pool.defroster && p.defroster == false) add(SignalKey(SignalId.C4))
            if (pool.ambientColor && p.colorArgb != null && colorTargets(p).isNotEmpty()) add(SignalKey(SignalId.L1))
            if (p.brightness != null && brightnessDirect(p.brightness) != null) add(SignalKey(SignalId.L2))
            participants
                .filter { it != SeatPosition.DRIVER && it in pool.readingLightSeats && p.readingLights[it] == false }
                .forEach { add(SignalKey(SignalId.L3, it)) }
            if (pool.lowBeam && p.lowBeam == false) add(SignalKey(SignalId.L4))
            if (steeringBounds() != null && p.steering != null) add(SignalKey(SignalId.D1))
        }

        /**
         * 봉인을 뺀 퍼즐을 최대 몇 개 만들 수 있는가([chooseSignals]와 같은 상한).
         * - 신호마다 [keyCapacity]의 합, 단 신호당 최대 2개(L3는 좌석이 여럿이어도 2개), 비EV 공조 합계 2개, D1 1개.
         * - 봉인을 뺀 서로 다른 신호가 2종 미만이면 같은 신호가 연달아 올 수밖에 없으므로 1개로 본다(어떤 길이도 불가).
         * - C3·C4가 함께 후보면 둘 중 하나만 쓰는 두 경우([variants]) 가운데 큰 쪽이다.
         * 예) 비EV + 공조 3종 + D1 → 2 + 1 = 3(+ 봉인 = 4): 3분 판만 가능.
         */
        fun contentCapacity(kinds: List<SignalKey>): Int = variants(kinds).maxOf { capacityOf(it) }

        /** 같은 판에 함께 낼 수 없는 신호(C3·C4)를 나눈 후보 목록들. 충돌이 없으면 [kinds] 하나다. 순서는 결정적이다. */
        fun variants(kinds: List<SignalKey>): List<List<SignalKey>> {
            val signals = kinds.map { it.signal }.toSet()
            if (SignalId.C3 !in signals || SignalId.C4 !in signals) return listOf(kinds)
            return listOf(kinds.filter { it.signal != SignalId.C4 }, kinds.filter { it.signal != SignalId.C3 })
        }

        /** 충돌 없는 후보 목록 하나로 만들 수 있는 최대 퍼즐 수(봉인 제외). */
        fun capacityOf(kinds: List<SignalKey>): Int {
            val perSignal = kinds.groupBy { it.signal }.mapValues { (_, keys) -> minOf(MAX_PER_KIND, keys.sumOf { keyCapacity(it) }) }
            val climate = perSignal.filterKeys { it.isClimate }.values.sum()
            val total = (if (isEv) climate else minOf(NON_EV_CLIMATE_MAX, climate)) + perSignal.filterKeys { !it.isClimate }.values.sum()
            return if (perSignal.count { it.value > 0 } < 2) minOf(total, 1) else total
        }

        /**
         * C1 목표: capability 양 끝 1℃ 안쪽 ∩ 18~28℃ 정수, 현재값과 2℃ 이상 차이.
         * [keepFollowUp]이면 그 값으로 바꾼 뒤에도 다음 C1 목표가 남는 값만(같은 판에서 C1을 또 쓸 때).
         */
        fun temperatureTargets(current: Float?, keepFollowUp: Boolean = false): List<Int> {
            val b = pool.temperature ?: return emptyList()
            val lo = maxOf(COMFORT_CELSIUS.first, ceil(b.min + 1f).toInt())
            val hi = minOf(COMFORT_CELSIUS.last, floor(b.max - 1f).toInt())
            val targets = (lo..hi).filter { current == null || abs(it - current) >= MIN_TEMPERATURE_GAP }
            return if (keepFollowUp) targets.filter { temperatureTargets(it.toFloat()).isNotEmpty() } else targets
        }

        /** 풍량 목표: 범위 안 ∩ 비EV 상한(범위 상한의 절반) 이하, 범위로 자른 현재값과 다름. [keepFollowUp]은 C1과 같다. */
        fun fanTargets(current: Int?, keepFollowUp: Boolean = false): List<Int> {
            val b = pool.fanSpeed ?: return emptyList()
            val cap = if (isEv) b.max else b.max / 2
            val clamped = current?.coerceIn(b.min, b.max)
            val targets = (b.min..cap).filter { it != clamped }
            return if (keepFollowUp) targets.filter { fanTargets(it).isNotEmpty() } else targets
        }

        /**
         * 이 신호(키)로 한 판에 낼 수 있는 퍼즐 수(0~2). 실제로 낼 수 있는 목표로 센다(QA F2-engine 2회차 실패 3).
         * C1·C2는 첫 목표 뒤에도 목표가 남을 때만 2, C3·L1·L2는 2(되돌리기·다른 색·반대쪽 끝), L3 좌석·D1·C4·L4는 1.
         */
        fun keyCapacity(key: SignalKey): Int = when (key.signal) {
            SignalId.C1 -> if (temperatureTargets(currentTemperature, keepFollowUp = true).isNotEmpty()) 2 else 1
            SignalId.C2 -> if (fanTargets(currentFan, keepFollowUp = true).isNotEmpty()) 2 else 1
            SignalId.C3, SignalId.L1, SignalId.L2 -> MAX_PER_KIND
            SignalId.L3, SignalId.D1, SignalId.C4, SignalId.L4 -> 1
            SignalId.B1 -> 0
        }

        /** 밝기 목표 후보: 범위가 넓으면 10 단위. */
        fun brightnessTargets(current: Int?): List<Int> {
            val b = pool.ambientBrightness ?: return emptyList()
            val step = if (b.max - b.min >= 20) 10 else 1
            return (b.min..b.max step step).filter { it != current }
        }

        /** 밝기 지시형: 현재값의 반대쪽 끝(기획 2-1 L2). */
        fun brightnessDirect(current: Int?): Int? {
            val b = pool.ambientBrightness ?: return null
            if (b.max <= b.min) return null
            val target = if (current == null || current < (b.min + b.max) / 2.0) b.max else b.min
            return target.takeIf { it != current }
        }

        /** 앰비언트 목표 색: 지금 색으로 이미 통과하는 색은 뺀다. 시연 모드는 VC 프리셋으로 재현되는 보라·파랑만. */
        fun colorTargets(p: Projection): List<NamedColor> {
            val palette = if (demo) DEMO_COLORS else NamedColor.entries
            return palette.filter { color ->
                val projected = p.colorTarget
                when {
                    projected != null -> hueDistance(projected.hue, color.hue) > ColorRule.MAX_HUE_DISTANCE
                    p.colorArgb != null -> !ColorRule.matches(p.colorArgb!!, color)
                    else -> true
                }
            }
        }

        fun steeringBounds(): Pair<Float, Float>? =
            pool.steering?.takeIf { SteeringZones.isUsable(it.min, it.max) }?.let { it.min to it.max }
    }
}
