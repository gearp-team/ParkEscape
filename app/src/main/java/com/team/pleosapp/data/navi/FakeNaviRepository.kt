package com.team.pleosapp.data.navi

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeNaviRepository(
    initial: NaviLocation? = NaviLocation(37.4979, 127.0276, "서울 강남구 강남대로"),
) : NaviRepository {
    private val location = MutableStateFlow(initial)

    override val currentLocation: StateFlow<NaviLocation?> = location.asStateFlow()

    var refreshCount = 0
        private set

    override fun refreshCurrentLocation() {
        refreshCount++
    }

    fun setLocation(value: NaviLocation?) {
        location.value = value
    }
}
