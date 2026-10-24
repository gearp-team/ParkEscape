package com.team.pleosapp

import android.content.Context
import com.team.pleosapp.data.driving.CarDrivingStateRepository
import com.team.pleosapp.data.driving.DrivingStateRepository
import com.team.pleosapp.data.driving.FakeDrivingStateRepository
import com.team.pleosapp.data.navi.FakeNaviRepository
import com.team.pleosapp.data.navi.NaviRepository
import com.team.pleosapp.data.navi.PleosNaviRepository
import com.team.pleosapp.data.vehicle.FakeVehicleRepository
import com.team.pleosapp.data.vehicle.PleosVehicleRepository
import com.team.pleosapp.data.vehicle.VehicleRepository

/**
 * 앱 전역 의존성. Pleos SDK·Car API 호출은 모두 여기서 만든 Repository를 거친다.
 * gradle.properties의 pleos.useFakeSdk=true면 Fake 구현으로 바꿔 끼운다.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val isUsingFakeSdk: Boolean = BuildConfig.USE_FAKE_SDK

    val vehicleRepository: VehicleRepository by lazy {
        if (isUsingFakeSdk) FakeVehicleRepository() else PleosVehicleRepository(appContext)
    }

    val naviRepository: NaviRepository by lazy {
        if (isUsingFakeSdk) FakeNaviRepository() else PleosNaviRepository(appContext)
    }

    val drivingStateRepository: DrivingStateRepository by lazy {
        if (isUsingFakeSdk) FakeDrivingStateRepository() else CarDrivingStateRepository(appContext)
    }
}
