package com.team.pleosapp.data.drive

import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import android.util.Log
import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.data.vehicle.CapabilityCache
import com.team.pleosapp.data.vehicle.VehicleConnection
import com.team.pleosapp.data.vehicle.awaitSdk
import com.team.pleosapp.data.vehicle.describeError
import com.team.pleosapp.data.vehicle.isReadable
import com.team.pleosapp.data.vehicle.listenerFlow
import com.team.pleosapp.data.vehicle.toValueBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Vehicle SDK SteeringWheel·Brake 구현. 둘 다 area 없는 전역 신호다. */
class PleosDriveInputRepository(connection: VehicleConnection) : DriveInputRepository {
    private val wheel by lazy { connection.vehicle.getSteeringWheel() }
    private val brake by lazy { connection.vehicle.getBrake() }

    private val cache = CapabilityCache { query() }

    override suspend fun support(): DriveInputSupport = cache.get()

    private suspend fun query(): Pair<DriveInputSupport, Boolean> {
        var complete = true

        val steering: SignalCapability<Float> = awaitSdk<Pair<CapabilityConfig, AreaBounds<Float>>> { ok, fail ->
            wheel.checkSteeringWheelAngleCapabilities(
                onComplete = { config: CapabilityConfig, bounds: AreaBounds<Float> -> ok(config to bounds) },
                onFailed = fail,
            )
        }.fold(
            onSuccess = { (config, bounds) ->
                val problem = steeringBoundsProblem(bounds.toValueBounds())
                when {
                    !config.accessType.isReadable() -> SignalCapability.unsupported("access=${config.accessType}")
                    problem != null -> SignalCapability.unsupported(problem)
                    else -> SignalCapability(true, bounds = bounds.toValueBounds())
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )

        val brakePedal: SignalCapability<Nothing> = awaitSdk<CapabilityConfig> { ok, fail ->
            brake.checkBrakePedalStatusCapabilities(onComplete = { config: CapabilityConfig -> ok(config) }, onFailed = fail)
        }.fold(
            onSuccess = { config ->
                if (config.accessType.isReadable()) {
                    SignalCapability(true)
                } else {
                    SignalCapability.unsupported("access=${config.accessType}")
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )

        val support = DriveInputSupport(steering, brakePedal)
        Log.i(TAG, "support: $support")
        return support to complete
    }

    override val steeringAngle: Flow<Float?> = listenerFlow(TAG, "D1 steering angle") { emit, fail ->
        if (!cache.get().steeringAngle.supported) return@listenerFlow null
        val listener = object : SteeringWheelAngleListener {
            override fun onSteeringWheelAngleUpdated(angle: Float?) = emit(angle)
            override fun onFailed(e: Exception) = fail(e)
        }
        wheel.registerSteeringWheelAngle(listener)
        return@listenerFlow { wheel.unregisterSteeringWheelAngle(listener) }
    }.distinctAngles()

    override val brakePressed: Flow<Boolean?> = listenerFlow(TAG, "B1 brake pedal") { emit, fail ->
        if (!cache.get().brakePedal.supported) return@listenerFlow null
        val listener = object : BrakePedalStatusListener {
            override fun onBrakePedalStatusUpdated(status: Boolean?) = emit(status)
            override fun onFailed(e: Exception) = fail(e)
        }
        brake.registerBrakePedalStatus(listener)
        return@listenerFlow { brake.unregisterBrakePedalStatus(listener) }
    }.distinctUntilChanged()

    override suspend fun readSteeringAngle(): Float? {
        if (!cache.get().steeringAngle.supported) return null
        return awaitSdk<Float?> { ok, fail -> wheel.getCurrentSteeringWheelAngle(ok, fail) }
            .onFailure { Log.w(TAG, "D1 read failed: ${it.describeError()}") }
            .getOrNull()
            .let(::normalizeAngle)
    }

    override suspend fun readBrakePressed(): Boolean? {
        if (!cache.get().brakePedal.supported) return null
        return awaitSdk<Boolean?> { ok, fail -> brake.getCurrentBrakePedalStatus(ok, fail) }
            .onFailure { Log.w(TAG, "B1 read failed: ${it.describeError()}") }
            .getOrNull()
    }

    private companion object {
        const val TAG = "PleosDriveInputRepo"
    }
}
