package com.team.pleosapp.data.navi

import kotlinx.coroutines.flow.StateFlow

data class NaviLocation(
    val latitude: Double,
    val longitude: Double,
    val address: String,
)

interface NaviRepository {
    /** 내장 내비가 마지막으로 알려 준 현재 위치. 첫 응답 전에는 null. */
    val currentLocation: StateFlow<NaviLocation?>

    /** 현재 위치를 다시 요청한다. 결과는 [currentLocation]으로 들어온다. */
    fun refreshCurrentLocation()
}
