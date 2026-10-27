package com.team.pleosapp.domain.puzzle

import kotlin.math.abs

/**
 * B1 화면 표시용. [windowRemainingMs]는 첫 누름부터 30초 창의 남은 시간, [gapRemainingMs]는 다음 누름까지 남은 시간(8초)이다.
 * 아직 누르지 않았거나 창이 지났으면 둘 다 null이고 [count]는 0이다.
 */
data class BrakeProgress(val count: Int, val target: Int, val windowRemainingMs: Long?, val gapRemainingMs: Long? = null)

/** 퍼즐 하나의 판정 상태. */
sealed class JudgeState {
    /** 아직 아니다(값이 목표와 다르거나 null). */
    data object Waiting : JudgeState()

    /** 여러 단계 퍼즐의 진행: B1은 누른 횟수/목표, D1은 칸 유지 여부(0 또는 1)/1. */
    data class Progress(val done: Int, val total: Int) : JudgeState()

    /**
     * D1: 콜백 값으로는 칸 유지 뒤 중앙에 돌아왔다. 등록 직후 첫 콜백이 낡은 값일 수 있으므로(QA 관찰)
     * 호출자가 `readSteeringAngle()`로 다시 읽어 [PuzzleJudge.confirmSteering]에 넘겨야 확정된다.
     */
    data object NeedsSteeringCheck : JudgeState()

    data object Solved : JudgeState()
}

/**
 * 퍼즐 하나의 판정기(기획 2-1 판정 규칙). 신호가 바뀔 때만 판정한다.
 * - 처음 받은 값은 "바뀐 값"으로 보고 판정한다(미리 맞춰 둔 값도 통과). 단 B1은 전환을 세므로 처음 값은 기준으로만 쓴다.
 * - 값이 null이면 불일치다(B1은 null을 "상태 모름"으로 보고 세지 않는다).
 * - 한 번 Solved가 되면 그대로다.
 *
 * 시간 [nowMs]는 호출자가 준다(GameSession은 경과 시간 기준. 일시정지한 동안은 흐르지 않는다).
 */
class PuzzleJudge(val puzzle: Puzzle) {
    private var lastValue: Any? = UNSET
    private var state: JudgeState = JudgeState.Waiting
    private val steering = (puzzle.target as? Target.Steering)?.let { SteeringTracker(it) }
    private val brake = (puzzle.target as? Target.BrakePresses)?.let { BrakeCounter(it.count) }

    val current: JudgeState get() = state

    /** 마지막으로 이 퍼즐의 신호 값이 바뀐 시각(건너뛰기 60초 판단용). 아직 없으면 null. */
    var lastChangeAtMs: Long? = null
        private set

    fun update(signals: VehicleSignals, nowMs: Long): JudgeState {
        if (state == JudgeState.Solved) return state
        val value = valueOf(signals)
        if (value == lastValue) return state
        lastValue = value
        lastChangeAtMs = nowMs
        state = evaluate(value, nowMs)
        return state
    }

    /** D1 확정: 다시 읽은 각도가 중앙(±5%)이면 Solved. 아니면 다음 중앙 복귀를 기다린다. */
    fun confirmSteering(angle: Float?): JudgeState {
        val tracker = steering ?: return state
        if (state != JudgeState.NeedsSteeringCheck) return state
        state = if (angle != null && tracker.isCentered(angle)) JudgeState.Solved else JudgeState.Progress(1, 1)
        return state
    }

    /** 브레이크 미지원 차량의 화면 "봉인 해제" 버튼. */
    fun pressSealButton(): JudgeState {
        if (puzzle.target == Target.SealButton) state = JudgeState.Solved
        return state
    }

    /** 화면 표시용: D1 목표 칸에 들어간 뒤 지난 시간(ms). 칸 밖이면 null. */
    fun steeringHoldMs(nowMs: Long): Long? = steering?.holdMs(nowMs)

    /** 화면 표시용: B1 누른 횟수와 창의 남은 시간. B1 퍼즐이 아니면 null. */
    fun brakeProgress(nowMs: Long): BrakeProgress? = brake?.progress(nowMs)

    private fun valueOf(signals: VehicleSignals): Any? = when (val t = puzzle.target) {
        is Target.Temperature -> signals.targetTemperature
        is Target.FanSpeed -> signals.fanSpeed
        is Target.Recirculation -> signals.recirculation
        is Target.AmbientColor -> signals.ambientColor?.let { it and 0xFFFFFF } // 알파 무시
        is Target.Brightness -> signals.ambientBrightness
        is Target.ReadingLight -> signals.readingLights[t.seat]
        is Target.Steering -> signals.steeringAngle?.let { if (it == 0f) 0f else it } // −0.0 = 0.0
        is Target.BrakePresses -> signals.brakePressed
        Target.SealButton -> null
    }

    private fun evaluate(value: Any?, nowMs: Long): JudgeState = when (val t = puzzle.target) {
        is Target.Temperature -> solvedIf((value as Float?)?.let { abs(it - t.celsius) <= TEMPERATURE_TOLERANCE } == true)
        is Target.FanSpeed -> solvedIf(value == t.level)
        is Target.Recirculation -> solvedIf(value == t.on)
        is Target.AmbientColor -> solvedIf((value as Int?)?.let { ColorRule.matches(it, t.color) } == true)
        is Target.Brightness -> solvedIf(value == t.level)
        is Target.ReadingLight -> solvedIf(value == true)
        is Target.Steering -> steering!!.update(value as Float?, nowMs)
        is Target.BrakePresses -> brake!!.update(value as Boolean?, nowMs)
        Target.SealButton -> state
    }

    private fun solvedIf(ok: Boolean): JudgeState = if (ok) JudgeState.Solved else JudgeState.Waiting

    /**
     * D1: 목표 칸에 들어간 시각을 기억하고, 칸을 떠날 때 1초 이상 머물렀으면 "유지함"으로 표시한다.
     * 유지한 뒤 중앙(±5%)에 오면 확인 요청. 값이 null이면 칸 밖으로 본다.
     */
    private class SteeringTracker(private val target: Target.Steering) {
        private var enteredAtMs: Long? = null
        private var held = false

        fun isCentered(angle: Float): Boolean = SteeringZones.isCentered(angle, target.boundsMin, target.boundsMax)

        fun holdMs(nowMs: Long): Long? = enteredAtMs?.let { nowMs - it }

        fun update(angle: Float?, nowMs: Long): JudgeState {
            val inTarget = angle != null &&
                SteeringZones.binOf(angle, target.boundsMin, target.boundsMax) == (target.side to target.bin)
            if (inTarget) {
                if (enteredAtMs == null) enteredAtMs = nowMs
            } else {
                val entered = enteredAtMs
                if (entered != null && nowMs - entered >= HOLD_MS) held = true
                enteredAtMs = null
            }
            return when {
                held && angle != null && isCentered(angle) -> JudgeState.NeedsSteeringCheck
                held -> JudgeState.Progress(1, 1)
                else -> JudgeState.Progress(0, 1)
            }
        }
    }

    /**
     * B1: false→true 전환을 센다. 앞 누름과 8초 넘게 벌어지거나 첫 누름에서 30초를 넘기면 이번 누름부터 다시 센다.
     * 실제로 false(뗌)를 관측한 뒤의 true만 누름이다(리더 결정): 밟은 채 퍼즐이 시작되거나 재개되면 떼었다가 밟아야 1회다.
     * 그 뗌이 150ms 이상 이어졌어야 한다(F2-ui 리더 결정, 경계 150ms는 셈). 밟은 채 재개하면 리스너가 낡은 false를 주고
     * 0~1ms 뒤 true를 주는데(에뮬레이터 실측), 이것과 페달 떨림을 같은 규칙으로 거른다. 짧은 뗌 뒤의 true는 계속 밟은 것으로 본다.
     * null(상태 모름)은 무시하고 마지막으로 관측한 상태를 유지한다.
     */
    private class BrakeCounter(private val target: Int) {
        private var pressed: Boolean? = null
        private var releasedAtMs: Long? = null
        private var count = 0
        private var firstAtMs = 0L
        private var lastAtMs: Long? = null

        fun update(value: Boolean?, nowMs: Long): JudgeState {
            when (value) {
                null -> Unit
                false -> if (pressed != false) releasedAtMs = nowMs
                true -> {
                    val released = releasedAtMs
                    if (pressed == false && released != null && nowMs - released >= MIN_RELEASE_MS) onPress(nowMs)
                }
            }
            if (value != null) pressed = value
            return if (count >= target) JudgeState.Solved else JudgeState.Progress(count, target)
        }

        /** 창이 지났으면(다음 누름에서 1부터 다시 센다) 화면에는 0으로 보인다. */
        fun progress(nowMs: Long): BrakeProgress {
            val last = lastAtMs
            val expired = count == 0 || last == null || nowMs - last > PRESS_GAP_MS || nowMs - firstAtMs > WINDOW_MS
            if (count >= target) return BrakeProgress(count, target, null)
            if (expired) return BrakeProgress(0, target, null)
            return BrakeProgress(count, target, WINDOW_MS - (nowMs - firstAtMs), PRESS_GAP_MS - (nowMs - last!!))
        }

        private fun onPress(nowMs: Long) {
            val last = lastAtMs
            val restart = count == 0 || last == null || nowMs - last > PRESS_GAP_MS || nowMs - firstAtMs > WINDOW_MS
            if (restart) {
                count = 1
                firstAtMs = nowMs
            } else {
                count++
            }
            lastAtMs = nowMs
        }
    }

    companion object {
        const val TEMPERATURE_TOLERANCE = 0.5f
        const val HOLD_MS = 1_000L
        const val PRESS_GAP_MS = 8_000L
        const val WINDOW_MS = 30_000L

        /** 이만큼 이상 뗀 뒤의 밟음만 센다. */
        const val MIN_RELEASE_MS = 150L
        private val UNSET = Any()
    }
}
