package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.signal.SeatPosition
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.max

/** 단조 시계(ms). 테스트에서 바꿔 끼운다. 앱에서는 SystemClock.elapsedRealtime 등을 넘긴다. */
fun interface Clock {
    fun nowMs(): Long

    companion object {
        val MONOTONIC = Clock { System.nanoTime() / 1_000_000 }
    }
}

/** 기획 2-2 상태. */
@Serializable
enum class SessionStatus {
    /** S1 준비: P 확인 전 */
    PREPARING,
    PLAYING,

    /** 규제 콜백(주행 전환)으로 멈춤 */
    PAUSED_BY_GATE,

    /** 게이트가 열린 채 앱 전환(onStop)으로 멈춤 */
    PAUSED_BY_APP,
    ESCAPED,
    TIMED_OUT,
    ;

    val isPaused: Boolean get() = this == PAUSED_BY_GATE || this == PAUSED_BY_APP
    val isFinished: Boolean get() = this == ESCAPED || this == TIMED_OUT
}

/**
 * 저장·복원 단위(RecordStore). 기획 2-2 GameSession 필드에 판 전체를 넣었다(판을 다시 만들지 않고 그대로 복원하려고).
 * 판정기의 진행(B1 누른 횟수, D1 칸 유지)은 저장하지 않는다. 재개하면 그 퍼즐의 진행은 처음부터다.
 */
@Serializable
data class SessionState(
    val board: Board,
    val participants: Set<SeatPosition>,
    /** 판 시작 때 신호 값(원상 복구 목표) */
    val snapshot: VehicleSignals,
    val status: SessionStatus = SessionStatus.PREPARING,
    val currentIndex: Int = 0,
    val elapsedMs: Long = 0,
    /** 건너뛴 퍼즐 번호. 하나라도 있으면 기록에서 제외한다. */
    val skippedIndices: Set<Int> = emptySet(),
    val gatePauses: Int = 0,
    val appPauses: Int = 0,
) {
    val lengthMinutes: Int get() = board.lengthMinutes
    val seed: String get() = board.seed
    val limitMs: Long get() = BoardLength.of(board.lengthMinutes).limitMs
}

/** 상태 기계가 알리는 사건. 화면은 이것으로 연출·저장 시점을 정한다. */
sealed class SessionEvent {
    data object None : SessionEvent()

    /** 현재 퍼즐의 판정 상태가 바뀌었다(B1 횟수, D1 칸 유지, D1 확인 요청 등). */
    data class JudgeChanged(val index: Int, val state: JudgeState) : SessionEvent()

    data class PuzzleSolved(val index: Int) : SessionEvent()

    data class PuzzleSkipped(val index: Int) : SessionEvent()

    data class Escaped(val elapsedMs: Long) : SessionEvent()

    data object TimedOut : SessionEvent()

    data class Paused(val status: SessionStatus) : SessionEvent()

    data object Resumed : SessionEvent()
}

/** 복구 체크리스트 한 줄. [original]·[current]는 신호 값 그대로(온도 Float, 색 ARGB Int 등). */
data class RestoreItem(val key: SignalKey, val original: Any?, val current: Any?, val restored: Boolean)

/**
 * 판 하나의 상태 기계(기획 2-2). 스레드 안전하지 않다(메인 스레드에서만 부른다).
 *
 * 전이
 * - PREPARING →(P 확인 + 게이트 열림)→ PLAYING →(마지막 퍼즐)→ ESCAPED, (시간 다 씀)→ TIMED_OUT
 * - PLAYING →(게이트 닫힘)→ PAUSED_BY_GATE, PLAYING →(앱 전환)→ PAUSED_BY_APP
 * - 두 일시정지 모두 "게이트 열림 + P 확인"([confirmParked])으로만 재개한다(v2 리더 결정). 게이트가 열렸다는 이유만으로 재개하지 않는다.
 * 타이머는 PLAYING 동안만 흐른다.
 */
class GameSession private constructor(initial: SessionState, private val clock: Clock) {
    var state: SessionState = initial
        private set

    private var runningSinceMs: Long? = null
    private var judge: PuzzleJudge? = null
    private var lastSignals: VehicleSignals? = null
    private var signalFailed = false

    val status: SessionStatus get() = state.status

    val currentPuzzle: Puzzle? get() = state.board.puzzles.getOrNull(state.currentIndex)

    val judgeState: JudgeState? get() = judge?.current

    /** 기록에 남길 수 있는 결과인가: 탈출 성공이고 건너뛴 퍼즐이 없음. */
    val isRecordEligible: Boolean get() = status == SessionStatus.ESCAPED && state.skippedIndices.isEmpty()

    fun elapsedMs(): Long = state.elapsedMs + (runningSinceMs?.let { clock.nowMs() - it } ?: 0L)

    fun remainingMs(): Long = max(0L, state.limitMs - elapsedMs())

    /** 저장용 상태. PLAYING이면 지금까지의 경과 시간을 반영한다. */
    fun toSaved(): SessionState = state.copy(elapsedMs = elapsedMs())

    /**
     * S1 "기어 P·주차 브레이크 확인". 게이트가 열려 있어야 하고 PREPARING 또는 일시정지일 때만 PLAYING으로 간다.
     * 재개하면 판정기를 새로 만들므로, 호출자는 리스너를 다시 등록하고 get*으로 읽은 현재값을 [onSignals]로 넘긴다.
     * 브레이크도 `readBrakePressed()`로 읽어 넘긴다: 밟은 채 재개하면 그 상태가 B1의 시작 상태가 되어 떼었다가 밟아야 1회다.
     */
    fun confirmParked(gateOpen: Boolean): SessionEvent {
        if (!gateOpen) return SessionEvent.None
        if (status != SessionStatus.PREPARING && !status.isPaused) return SessionEvent.None
        state = state.copy(status = SessionStatus.PLAYING)
        runningSinceMs = clock.nowMs()
        startJudge()
        return SessionEvent.Resumed
    }

    /** 규제 콜백에서 게이트가 닫혔다(requiresDO 또는 activeRestrictions ≠ 0). PLAYING이면 PAUSED_BY_GATE. */
    fun onGateClosed(): SessionEvent = pause(SessionStatus.PAUSED_BY_GATE)

    /** 게이트가 열린 채 onStop. PLAYING이면 PAUSED_BY_APP. */
    fun onAppStopped(): SessionEvent = pause(SessionStatus.PAUSED_BY_APP)

    /** 신호가 바뀔 때마다 부른다(Repository Flow를 묶은 값). PLAYING이 아니면 무시한다. */
    fun onSignals(signals: VehicleSignals): SessionEvent {
        if (status != SessionStatus.PLAYING) return SessionEvent.None
        lastSignals = signals
        checkTimeout()?.let { return it }
        val j = judge ?: return SessionEvent.None
        val before = j.current
        val after = j.update(signals, elapsedMs())
        return handle(before, after)
    }

    /** D1이 [JudgeState.NeedsSteeringCheck]일 때 readSteeringAngle()로 다시 읽은 값을 넘긴다. */
    fun confirmSteering(angle: Float?): SessionEvent {
        if (status != SessionStatus.PLAYING) return SessionEvent.None
        checkTimeout()?.let { return it }
        val j = judge ?: return SessionEvent.None
        val before = j.current
        return handle(before, j.confirmSteering(angle))
    }

    /** 브레이크 미지원 차량의 화면 봉인 버튼. */
    fun pressSealButton(): SessionEvent {
        if (status != SessionStatus.PLAYING) return SessionEvent.None
        checkTimeout()?.let { return it }
        val j = judge ?: return SessionEvent.None
        val before = j.current
        return handle(before, j.pressSealButton())
    }

    /** 1초마다 등 주기적으로 불러 시간 초과를 확인한다(신호가 없어도 끝나야 하므로). */
    fun tick(): SessionEvent = if (status == SessionStatus.PLAYING) checkTimeout() ?: SessionEvent.None else SessionEvent.None

    /** 리스너 onFailed를 받았다: 현재 퍼즐을 바로 건너뛸 수 있게 한다. */
    fun reportSignalFailure() {
        signalFailed = true
    }

    /** 건너뛰기 허용: 리스너 실패, 또는 이 퍼즐 신호가 60초 동안 바뀌지 않음(플레이 시간 기준). 봉인 퍼즐은 건너뛸 수 없다. */
    fun canSkip(): Boolean {
        if (status != SessionStatus.PLAYING) return false
        val puzzle = currentPuzzle ?: return false
        if (puzzle.type == ClueType.SEAL) return false
        if (signalFailed) return true
        val since = judge?.lastChangeAtMs ?: activatedAtMs
        return elapsedMs() - since >= SKIP_AFTER_MS
    }

    fun skipCurrent(): SessionEvent {
        if (!canSkip()) return SessionEvent.None
        val index = state.currentIndex
        state = state.copy(skippedIndices = state.skippedIndices + index)
        advance()
        return SessionEvent.PuzzleSkipped(index)
    }

    /** D1 칸 유지 표시용(ms). */
    fun steeringHoldMs(): Long? = judge?.steeringHoldMs(elapsedMs())

    /** B1 누른 횟수·창 표시용. */
    fun brakeProgress(): BrakeProgress? = judge?.brakeProgress(elapsedMs())

    /**
     * 원상 복구 체크리스트(기획 F2·S3). 판이 바꾼 신호마다 원래 값과 현재 값을 비교해 자동 체크한다.
     * D1은 "핸들 중앙" 한 줄(중앙 ±5%면 체크). B1은 원복이 필요 없다. 원래 값을 모르는 신호는 줄을 만들지 않는다.
     */
    fun restoreChecklist(current: VehicleSignals): List<RestoreItem> {
        val snapshot = state.snapshot
        return state.board.puzzles.map { it.key to it.target }.distinctBy { it.first }.mapNotNull { (key, target) ->
            when (key.signal) {
                SignalId.C1 -> item(key, snapshot.targetTemperature, current.targetTemperature) { o, c ->
                    abs(o - c) <= RESTORE_TEMPERATURE_TOLERANCE
                }
                SignalId.C2 -> item(key, snapshot.fanSpeed, current.fanSpeed) { o, c -> o == c }
                SignalId.C3 -> item(key, snapshot.recirculation, current.recirculation) { o, c -> o == c }
                SignalId.L1 -> item(key, snapshot.ambientColor, current.ambientColor) { o, c -> ColorRule.sameRgb(o, c) }
                SignalId.L2 -> item(key, snapshot.ambientBrightness, current.ambientBrightness) { o, c -> o == c }
                SignalId.L3 -> item(key, key.seat?.let { snapshot.readingLights[it] }, key.seat?.let { current.readingLights[it] }) { o, c -> o == c }
                SignalId.D1 -> {
                    val steering = target as Target.Steering
                    val angle = current.steeringAngle
                    RestoreItem(key, 0f, angle, angle != null && SteeringZones.isCentered(angle, steering.boundsMin, steering.boundsMax))
                }
                SignalId.B1 -> null
            }
        }
    }

    private inline fun <T : Any> item(key: SignalKey, original: T?, current: T?, same: (T, T) -> Boolean): RestoreItem? {
        if (original == null) return null
        return RestoreItem(key, original, current, current != null && same(original, current))
    }

    // ---- 내부 ---------------------------------------------------------------------------------

    private var activatedAtMs: Long = 0L

    private fun startJudge() {
        val puzzle = currentPuzzle
        judge = puzzle?.let { PuzzleJudge(it) }
        activatedAtMs = state.elapsedMs + (runningSinceMs?.let { clock.nowMs() - it } ?: 0L)
        signalFailed = false
    }

    private fun pause(to: SessionStatus): SessionEvent {
        if (status != SessionStatus.PLAYING) return SessionEvent.None
        state = state.copy(
            status = to,
            elapsedMs = elapsedMs(),
            gatePauses = state.gatePauses + if (to == SessionStatus.PAUSED_BY_GATE) 1 else 0,
            appPauses = state.appPauses + if (to == SessionStatus.PAUSED_BY_APP) 1 else 0,
        )
        runningSinceMs = null
        judge = null
        lastSignals = null
        return SessionEvent.Paused(to)
    }

    private fun handle(before: JudgeState, after: JudgeState): SessionEvent = when {
        after == JudgeState.Solved -> {
            val index = state.currentIndex
            advance()
            if (status == SessionStatus.ESCAPED) SessionEvent.Escaped(state.elapsedMs) else SessionEvent.PuzzleSolved(index)
        }
        after != before -> SessionEvent.JudgeChanged(state.currentIndex, after)
        else -> SessionEvent.None
    }

    /** 다음 퍼즐로. 마지막이었으면 ESCAPED(타이머 정지). 다음 판정기는 마지막으로 받은 신호로 한 번 판정한다. */
    private fun advance() {
        val next = state.currentIndex + 1
        if (next >= state.board.puzzles.size) {
            state = state.copy(status = SessionStatus.ESCAPED, currentIndex = next, elapsedMs = elapsedMs())
            runningSinceMs = null
            judge = null
            return
        }
        state = state.copy(currentIndex = next)
        startJudge()
        lastSignals?.let { judge?.update(it, elapsedMs()) }
        if (judge?.current == JudgeState.Solved) advance()
    }

    private fun checkTimeout(): SessionEvent? {
        if (elapsedMs() < state.limitMs) return null
        state = state.copy(status = SessionStatus.TIMED_OUT, elapsedMs = state.limitMs)
        runningSinceMs = null
        judge = null
        return SessionEvent.TimedOut
    }

    companion object {
        const val SKIP_AFTER_MS = 60_000L
        const val RESTORE_TEMPERATURE_TOLERANCE = 0.25f

        /** 새 판. 상태는 PREPARING이고 [snapshot]이 원상 복구 목표가 된다. */
        fun start(board: Board, participants: Set<SeatPosition>, snapshot: VehicleSignals, clock: Clock = Clock.MONOTONIC): GameSession =
            GameSession(SessionState(board, participants, snapshot), clock)

        /**
         * 저장된 상태에서 복원한다. PLAYING으로 저장돼 있었으면(앱이 PLAYING 중 종료됨) 원인을 모르므로 PAUSED_BY_APP으로 두고
         * P 확인을 거쳐 재개하게 한다.
         */
        fun restore(saved: SessionState, clock: Clock = Clock.MONOTONIC): GameSession {
            val status = if (saved.status == SessionStatus.PLAYING) SessionStatus.PAUSED_BY_APP else saved.status
            return GameSession(saved.copy(status = status), clock)
        }
    }
}
