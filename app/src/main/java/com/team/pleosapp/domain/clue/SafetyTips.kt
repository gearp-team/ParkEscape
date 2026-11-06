package com.team.pleosapp.domain.clue

import com.team.pleosapp.domain.puzzle.Board
import com.team.pleosapp.domain.puzzle.SignalId

/**
 * "실제 상황 팁" 카드(2026-11-06 사용자 결정: 놀면서 위급할 때 쓸 조작을 손에 익힌다).
 * 퍼즐이 풀리면 그 신호에 팁이 있을 때 한 줄을 잠깐 보여 준다. 문구는 정확성 때문에 사용자가 정한 문장을 그대로 쓴다.
 * 팁이 있는 신호는 C4 앞유리 제상, L4 전조등, C3 내외기뿐이다.
 */
object SafetyTips {
    /** 카드를 보여 주는 시간(ms). */
    const val SHOW_MS = 4_000L

    fun of(signal: SignalId): String? = when (signal) {
        SignalId.C4 -> "비나 추위로 앞유리에 김이 서리면 바로 이 버튼 — 시야부터 확보하세요"
        SignalId.L4 -> "터널에 들어가거나 갑자기 어두워지면 전조등 — 내 차가 보여야 안전합니다"
        SignalId.C3 -> "터널이나 앞차 매연이 심할 때는 내기 순환으로 바깥 공기를 막으세요"
        else -> null
    }

    /**
     * 이번 판정으로 풀린 퍼즐([fromIndex] 이상 [untilIndex] 미만, 건너뛴 퍼즐 제외) 가운데 팁이 있는 마지막 퍼즐의 신호.
     * 한 번의 판정으로 여러 퍼즐이 넘어갈 수 있어서(다음 퍼즐이 이미 맞춰져 있던 경우) 범위로 받는다. 없으면 null.
     */
    fun solvedTipSignal(board: Board, fromIndex: Int, untilIndex: Int, skipped: Set<Int>): SignalId? =
        (fromIndex until minOf(untilIndex, board.puzzles.size))
            .filter { it !in skipped }
            .map { board.puzzles[it].signal }
            .lastOrNull { of(it) != null }
}
