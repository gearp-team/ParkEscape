package com.team.pleosapp.data.driving

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDrivingStateRepository(initial: DrivingRestrictions = DrivingRestrictions()) : DrivingStateRepository {
    private val state = MutableStateFlow(initial)

    override val restrictions: Flow<DrivingRestrictions> = state

    fun setRestrictions(value: DrivingRestrictions) {
        state.value = value
    }
}
