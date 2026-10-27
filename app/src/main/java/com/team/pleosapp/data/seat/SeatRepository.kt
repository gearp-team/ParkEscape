package com.team.pleosapp.data.seat

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import kotlinx.coroutines.flow.Flow

/** 좌석 점유(F3 참가자). 읽기만 한다. */
interface SeatRepository {
    /** 좌석 점유 지원 범위. zones의 원소마다 좌석 하나다. 처음 부를 때 capability를 조회하고, 끝까지 성공한 결과는 기억한다. */
    suspend fun support(): SignalCapability<Nothing>

    /** 지원 좌석이 모두 한 번씩 값을 보낸 뒤부터, 좌석 값이 바뀔 때마다 내보낸다. 지원 좌석이 없으면 빈 원시값. */
    val occupancy: Flow<SeatOccupancy>

    /** 현재 점유 읽기(S1 준비 화면 진입 때 등). */
    suspend fun readOccupancy(): SeatOccupancy
}

/**
 * 좌석 점유 원시값과 보정값.
 *
 * SDK는 VHAL 원시값이 1이면 true를 준다(`PropertyCallback`: `value?.toInt() == 1`). 에뮬레이터에서는 Virtual Console "점유"가
 * false로, "비어 있음"이 true로 온다(SPIKE-W1). AAOS 표준(VACANT 1, OCCUPIED 2)에 대 보면 "비어 있음"을 true로 주는 것으로 보이지만
 * 실차가 어느 쪽인지는 모른다. 그래서 "판을 하는 동안 운전석에는 항상 사람이 있다"를 기준으로,
 * 운전석이 false로 읽히면 모든 좌석의 방향을 뒤집는다. 실차 값이 표준대로라면 운전석이 true라서 뒤집지 않는다.
 * 운전석 값을 모르면(null, 또는 운전석 area가 없어 키가 없음) 방향을 정할 수 없으므로 다른 좌석 값도 쓰지 않는다
 * (기획 F3: 값 불명이면 초기 선택은 운전석만).
 *
 * @property raw SDK가 준 좌석별 값(오류·미보고 좌석은 null)
 */
data class SeatOccupancy(val raw: Map<SeatPosition, Boolean?>) {
    /** 운전석 원시값이 있어 방향을 정할 수 있으면 true. */
    val polarityKnown: Boolean get() = raw[SeatPosition.DRIVER] != null

    /** 운전석 원시값이 false면 방향을 뒤집는다. 방향을 모르면([polarityKnown] false) false다. */
    val inverted: Boolean get() = raw[SeatPosition.DRIVER] == false

    /**
     * 방향을 보정한 좌석별 점유(true = 사람이 있음). 운전석은 항상 true로 본다.
     * 방향을 모르면 운전석 말고는 모두 null(모름)이다.
     */
    val corrected: Map<SeatPosition, Boolean?>
        get() = raw.mapValues { (_, value) ->
            when {
                !polarityKnown -> null
                inverted -> value?.not()
                else -> value
            }
        } + (SeatPosition.DRIVER to true)

    /**
     * 퍼즐 참가 좌석: 운전석 + 보정값이 true인 좌석. 뒷좌석 가운데는 퍼즐에 쓰지 않는다
     * (Virtual Console로 바꿀 수 없고, 독서등 area도 없다).
     */
    val participants: Set<SeatPosition>
        get() = corrected.filterValues { it == true }.keys - SeatPosition.REAR_CENTER
}
