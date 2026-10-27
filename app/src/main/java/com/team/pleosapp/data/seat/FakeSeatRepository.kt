package com.team.pleosapp.data.seat

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * SDK 없이 쓰는 좌석 점유. 기본값은 기획 F3의 Fake 데이터(운전석 + 2열 오른쪽 점유)이고,
 * 원시값은 표준 방향(true = 점유)으로 둔다. 에뮬레이터처럼 뒤집힌 값을 시험하려면 [setRaw]로 넣는다.
 */
class FakeSeatRepository(
    raw: Map<SeatPosition, Boolean?> = DEFAULT_RAW,
    private val support: SignalCapability<Nothing> = DEFAULT_SUPPORT,
) : SeatRepository {
    private val state = MutableStateFlow(raw)

    override suspend fun support(): SignalCapability<Nothing> = support

    override val occupancy: Flow<SeatOccupancy> = state.map(::SeatOccupancy)

    override suspend fun readOccupancy(): SeatOccupancy = SeatOccupancy(state.value)

    fun setRaw(raw: Map<SeatPosition, Boolean?>) {
        state.value = raw
    }

    fun setSeat(seat: SeatPosition, rawValue: Boolean?) {
        state.value = state.value + (seat to rawValue)
    }

    companion object {
        val DEFAULT_RAW: Map<SeatPosition, Boolean?> = mapOf(
            SeatPosition.DRIVER to true,
            SeatPosition.PASSENGER to false,
            SeatPosition.REAR_LEFT to false,
            SeatPosition.REAR_CENTER to false,
            SeatPosition.REAR_RIGHT to true,
        )

        val DEFAULT_SUPPORT: SignalCapability<Nothing> = SignalCapability(true, SeatPosition.entries.map { setOf(it) })
    }
}
