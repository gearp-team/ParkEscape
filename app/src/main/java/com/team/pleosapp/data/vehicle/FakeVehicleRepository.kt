package com.team.pleosapp.data.vehicle

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVehicleRepository(stateOfCharge: Int? = 82) : VehicleRepository {
    private val state = MutableStateFlow(stateOfCharge)

    override val evStateOfCharge: Flow<Int?> = state

    fun setStateOfCharge(value: Int?) {
        state.value = value
    }
}
