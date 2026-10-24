package com.team.pleosapp.data.driving

import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** AAOS CarUxRestrictionsManager 구현. 테스트: Virtual Console 기어 D/P 또는 driving_regulation adb 명령. */
class CarDrivingStateRepository(context: Context) : DrivingStateRepository {
    private val appContext = context.applicationContext

    override val restrictions: Flow<DrivingRestrictions> = callbackFlow {
        val car: Car? = Car.createCar(appContext)
        val manager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
        if (manager == null) {
            Log.w(TAG, "CarUxRestrictionsManager unavailable; assuming fully restricted")
            trySend(DrivingRestrictions.FullyRestricted)
            awaitClose { car?.disconnect() }
            return@callbackFlow
        }

        trySend(manager.currentCarUxRestrictions?.toDrivingRestrictions() ?: DrivingRestrictions.FullyRestricted)
        manager.registerListener { trySend(it.toDrivingRestrictions()) }
        awaitClose {
            manager.unregisterListener()
            car.disconnect()
        }
    }

    private companion object {
        const val TAG = "CarDrivingStateRepo"
    }
}

private fun CarUxRestrictions.toDrivingRestrictions() =
    DrivingRestrictions(isRequiresDistractionOptimization, activeRestrictions)
