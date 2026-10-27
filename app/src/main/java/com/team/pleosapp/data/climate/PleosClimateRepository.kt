package com.team.pleosapp.data.climate

import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.api.model.capability.HvacTemperatureConfigArray
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.HvacFanSpeedListener
import ai.pleos.playground.vehicle.listener.HvacRecirculationStateListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener
import android.util.Log
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.data.signal.ValueBounds
import com.team.pleosapp.data.vehicle.CapabilityCache
import com.team.pleosapp.data.vehicle.VehicleConnection
import com.team.pleosapp.data.vehicle.awaitSdk
import com.team.pleosapp.data.vehicle.describeError
import com.team.pleosapp.data.vehicle.isReadable
import com.team.pleosapp.data.vehicle.listenerFlow
import com.team.pleosapp.data.vehicle.str
import com.team.pleosapp.data.vehicle.toSeats
import com.team.pleosapp.data.vehicle.toValueBounds
import com.team.pleosapp.data.vehicle.zoneContaining
import kotlinx.coroutines.flow.Flow

/** Vehicle SDK HVAC 구현. 에뮬레이터: 온도는 좌석별 area(15~30℃), 풍량은 앞좌석 묶음(1~8), 내외기는 5좌석 묶음. */
class PleosClimateRepository(connection: VehicleConnection) : ClimateRepository {
    private val hvac by lazy { connection.vehicle.getHVAC() }

    /** support()와 함께, 등록·조회에 쓸 SDK area를 들고 있다(SDK 타입은 이 클래스 밖으로 내보내지 않는다). */
    private class Resolved(
        val support: ClimateSupport,
        val temperatureArea: SeatArea?,
        val fanArea: SeatArea?,
        val fanBounds: ValueBounds<Int>?,
        val recirculationArea: SeatArea?,
    )

    private val cache = CapabilityCache { query() }

    override suspend fun support(): ClimateSupport = cache.get().support

    private suspend fun query(): Pair<Resolved, Boolean> {
        var complete = true

        var temperatureArea: SeatArea? = null
        var step: Float? = null
        val temperature: SignalCapability<Float> = awaitSdk<Triple<CapabilityConfig, Map<SeatArea, AreaBounds<Float>>, HvacTemperatureConfigArray?>> { ok, fail ->
            hvac.checkHvacTemperatureCapabilities(
                onComplete = { config: CapabilityConfig, map: Map<SeatArea, AreaBounds<Float>>, cfg: HvacTemperatureConfigArray? ->
                    ok(Triple(config, map, cfg))
                },
                onFailed = fail,
            )
        }.fold(
            onSuccess = { (config, map, cfg) ->
                step = cfg?.incrementCelsius
                val area = map.keys.zoneContaining(SeatPosition.DRIVER)
                when {
                    !config.accessType.isReadable() -> SignalCapability.unsupported("access=${config.accessType}")
                    area == null -> SignalCapability.unsupported("no driver area in ${map.keys.map { it.str() }}")
                    else -> {
                        temperatureArea = area
                        SignalCapability(true, listOf(area.toSeats()), map.getValue(area).toValueBounds())
                    }
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )

        var fanArea: SeatArea? = null
        val fan: SignalCapability<Int> = awaitSdk<Pair<CapabilityConfig, Map<SeatArea, AreaBounds<Int>>>> { ok, fail ->
            hvac.checkHvacFanSpeedCapabilities(
                onComplete = { config: CapabilityConfig, map: Map<SeatArea, AreaBounds<Int>> -> ok(config to map) },
                onFailed = fail,
            )
        }.fold(
            onSuccess = { (config, map) ->
                val area = map.keys.zoneContaining(SeatPosition.DRIVER)
                when {
                    !config.accessType.isReadable() -> SignalCapability.unsupported("access=${config.accessType}")
                    area == null -> SignalCapability.unsupported("no driver area in ${map.keys.map { it.str() }}")
                    else -> {
                        fanArea = area
                        SignalCapability(true, listOf(area.toSeats()), map.getValue(area).toValueBounds())
                    }
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )

        var recirculationArea: SeatArea? = null
        val recirculation: SignalCapability<Nothing> = awaitSdk<Pair<CapabilityConfig, List<SeatArea>>> { ok, fail ->
            hvac.checkHvacRecirculationCapabilities(
                onComplete = { config: CapabilityConfig, areas: List<SeatArea> -> ok(config to areas) },
                onFailed = fail,
            )
        }.fold(
            onSuccess = { (config, areas) ->
                val area = areas.zoneContaining(SeatPosition.DRIVER)
                when {
                    !config.accessType.isReadable() -> SignalCapability.unsupported("access=${config.accessType}")
                    area == null -> SignalCapability.unsupported("no driver area in ${areas.map { it.str() }}")
                    else -> {
                        recirculationArea = area
                        SignalCapability<Nothing>(true, listOf(area.toSeats()))
                    }
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )

        val support = ClimateSupport(temperature, step, fan, recirculation)
        Log.i(TAG, "support: $support")
        return Resolved(support, temperatureArea, fanArea, fan.bounds, recirculationArea) to complete
    }

    override val targetTemperature: Flow<Float?> = listenerFlow(TAG, "C1 target temperature") { emit, fail ->
        val area = cache.get().temperatureArea ?: return@listenerFlow null
        val listener = object : HvacTemperatureListener {
            override fun onHvacTemperatureUpdated(temperature: Float?) = emit(temperature)
            override fun onFailed(e: Exception) = fail(e)
        }
        hvac.registerHvacTemperature(area, listener)
        return@listenerFlow { hvac.unregisterHvacTemperature(area, listener) }
    }

    override val fanSpeed: Flow<Int?> = listenerFlow(TAG, "C2 fan speed") { emit, fail ->
        val resolved = cache.get()
        val area = resolved.fanArea ?: return@listenerFlow null
        val listener = object : HvacFanSpeedListener {
            override fun onHvacFanSpeedUpdated(speed: Int?) = emit(normalizeFanSpeed(speed, resolved.fanBounds))
            override fun onFailed(e: Exception) = fail(e)
        }
        hvac.registerHvacFanSpeed(area, listener)
        return@listenerFlow { hvac.unregisterHvacFanSpeed(area, listener) }
    }

    override val recirculation: Flow<Boolean?> = listenerFlow(TAG, "C3 recirculation") { emit, fail ->
        val area = cache.get().recirculationArea ?: return@listenerFlow null
        // 이름에 Auto가 들어 있지만 registerHvacRecirculationState에 쓰는 리스너 타입이다.
        val listener = object : HvacRecirculationStateListener {
            override fun onHvacAutoRecirculationStateUpdated(state: Boolean?) = emit(state)
            override fun onFailed(e: Exception) = fail(e)
        }
        hvac.registerHvacRecirculationState(area, listener)
        return@listenerFlow { hvac.unregisterHvacRecirculationState(area, listener) }
    }

    override suspend fun readTargetTemperature(): Float? {
        val area = cache.get().temperatureArea ?: return null
        return awaitSdk<Float?> { ok, fail -> hvac.getHvacTemperature(area, TemperatureUnit.CELSIUS, ok, fail) }
            .onFailure { Log.w(TAG, "C1 read failed: ${it.describeError()}") }
            .getOrNull()
    }

    override suspend fun readFanSpeed(): Int? {
        val resolved = cache.get()
        val area = resolved.fanArea ?: return null
        return awaitSdk<Int?> { ok, fail -> hvac.getHvacFanSpeed(area, ok, fail) }
            .onFailure { Log.w(TAG, "C2 read failed: ${it.describeError()}") }
            .getOrNull()
            .let { normalizeFanSpeed(it, resolved.fanBounds) }
    }

    override suspend fun readRecirculation(): Boolean? {
        val area = cache.get().recirculationArea ?: return null
        return awaitSdk<Boolean?> { ok, fail -> hvac.getHvacRecirculationState(area, ok, fail) }
            .onFailure { Log.w(TAG, "C3 read failed: ${it.describeError()}") }
            .getOrNull()
    }

    private companion object {
        const val TAG = "PleosClimateRepo"
    }
}
