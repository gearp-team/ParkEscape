package com.team.pleosapp.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Vehicle SDK 구현. initialize()는 비동기 bind이고, 연결 전에 들어온 요청은 SDK가 큐에 쌓았다가 실행한다.
 * 앱 프로세스와 수명을 같이 하므로 release()는 부르지 않는다.
 */
class PleosVehicleRepository(context: Context) : VehicleRepository {
    private val vehicle = Vehicle(context.applicationContext).also { it.initialize() }

    override val evStateOfCharge: Flow<Int?> = callbackFlow {
        val battery = vehicle.getEvBattery()
        val listener = object : EvBatteryStateOfChargeListener {
            override fun onEvBatteryStateOfChargeUpdated(value: Int?) {
                trySend(value)
            }

            override fun onFailed(e: Exception) {
                Log.w(TAG, "EV state of charge failed: ${e.describe()}")
                trySend(null)
            }
        }
        battery.registerEvBatteryStateOfCharge(listener)
        // 해제는 등록 때와 같은 listener로 해야 한다.
        awaitClose { battery.unregisterEvBatteryStateOfCharge(listener) }
    }

    private companion object {
        const val TAG = "PleosVehicleRepo"
    }
}

/**
 * Vehicle 2.3.2 AAR은 VehicleException을 난독화해 배포하고 ErrorCode enum이 없어서 `is VehicleException`을 쓸 수 없다.
 * message에 ErrorCode 이름(예: ERROR_NOT_SUPPORTED_PROPERTY)이 들어 있다. IllegalArgumentException 등도 들어온다.
 */
internal fun Exception.describe(): String = message ?: javaClass.simpleName
