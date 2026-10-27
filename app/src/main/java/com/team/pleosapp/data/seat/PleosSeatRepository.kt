package com.team.pleosapp.data.seat

import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import android.util.Log
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.data.vehicle.CapabilityCache
import com.team.pleosapp.data.vehicle.VehicleConnection
import com.team.pleosapp.data.vehicle.awaitSdk
import com.team.pleosapp.data.vehicle.describeError
import com.team.pleosapp.data.vehicle.isReadable
import com.team.pleosapp.data.vehicle.seatMapFlow
import com.team.pleosapp.data.vehicle.singleSeatZones
import com.team.pleosapp.data.vehicle.str
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Vehicle SDK Seat 구현. 에뮬레이터: 좌석별 area 5개(1열 좌우, 2열 좌·가운데·우), READ 전용. */
class PleosSeatRepository(connection: VehicleConnection) : SeatRepository {
    private val seat by lazy { connection.vehicle.getSeat() }

    private class Resolved(val support: SignalCapability<Nothing>, val areas: Map<SeatPosition, SeatArea>)

    private val cache = CapabilityCache { query() }

    override suspend fun support(): SignalCapability<Nothing> = cache.get().support

    private suspend fun query(): Pair<Resolved, Boolean> {
        var complete = true
        var areas: Map<SeatPosition, SeatArea> = emptyMap()
        val support: SignalCapability<Nothing> = awaitSdk<Pair<CapabilityConfig, List<SeatArea>>> { ok, fail ->
            seat.checkSeatOccupancyCapabilities(
                onComplete = { config: CapabilityConfig, list: List<SeatArea> -> ok(config to list) },
                onFailed = fail,
            )
        }.fold(
            onSuccess = { (config, list) ->
                val zones = list.singleSeatZones()
                when {
                    !config.accessType.isReadable() -> SignalCapability.unsupported("access=${config.accessType}")
                    zones.isEmpty() -> SignalCapability.unsupported("no single-seat area in ${list.map { it.str() }}")
                    else -> {
                        areas = zones
                        SignalCapability(true, zones.keys.map { setOf(it) })
                    }
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )
        Log.i(TAG, "support: $support")
        return Resolved(support, areas) to complete
    }

    override val occupancy: Flow<SeatOccupancy> = seatMapFlow(
        TAG,
        "F3 seat occupancy",
        resolveSeats = { cache.get().areas },
    ) { area, report, reportFailure ->
        val listener = object : SeatOccupancyListener {
            override fun onSeatOccupancyUpdated(occupancy: Boolean?) = report(occupancy)
            override fun onFailed(e: Exception) = reportFailure(e)
        }
        seat.registerSeatOccupancy(area, listener)
        return@seatMapFlow { seat.unregisterSeatOccupancy(area, listener) }
    }.map(::SeatOccupancy)

    override suspend fun readOccupancy(): SeatOccupancy = SeatOccupancy(
        cache.get().areas.mapValues { (position, area) ->
            awaitSdk<Boolean?> { ok, fail -> seat.getCurrentSeatOccupancy(area, ok, fail) }
                .onFailure { Log.w(TAG, "F3 $position read failed: ${it.describeError()}") }
                .getOrNull()
        },
    )

    private companion object {
        const val TAG = "PleosSeatRepo"
    }
}
