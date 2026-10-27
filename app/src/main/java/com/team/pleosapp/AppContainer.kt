package com.team.pleosapp

import android.content.Context
import com.team.pleosapp.data.climate.ClimateRepository
import com.team.pleosapp.data.climate.FakeClimateRepository
import com.team.pleosapp.data.climate.PleosClimateRepository
import com.team.pleosapp.data.drive.DriveInputRepository
import com.team.pleosapp.data.drive.FakeDriveInputRepository
import com.team.pleosapp.data.drive.PleosDriveInputRepository
import com.team.pleosapp.data.driving.CarDrivingStateRepository
import com.team.pleosapp.data.driving.DrivingStateRepository
import com.team.pleosapp.data.driving.FakeDrivingStateRepository
import com.team.pleosapp.data.fake.AutoPlayer
import com.team.pleosapp.data.fake.FakeAutoPlayer
import com.team.pleosapp.data.light.CabinLightRepository
import com.team.pleosapp.data.light.FakeCabinLightRepository
import com.team.pleosapp.data.light.PleosCabinLightRepository
import com.team.pleosapp.data.record.DataStoreRecordStore
import com.team.pleosapp.data.record.RecordStore
import com.team.pleosapp.data.seat.FakeSeatRepository
import com.team.pleosapp.data.seat.PleosSeatRepository
import com.team.pleosapp.data.seat.SeatRepository
import com.team.pleosapp.data.settings.DataStoreSettingsRepository
import com.team.pleosapp.data.settings.SettingsRepository
import com.team.pleosapp.data.vehicle.VehicleConnection

/**
 * 앱 전역 의존성. Pleos SDK·Car API 호출은 모두 여기서 만든 Repository를 거친다.
 * gradle.properties의 pleos.useFakeSdk=true면 Fake 구현으로 바꿔 끼운다.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val isUsingFakeSdk: Boolean = BuildConfig.USE_FAKE_SDK

    /** Vehicle SDK 연결 하나를 모든 Vehicle Repository가 함께 쓴다(처음 쓸 때 initialize). */
    private val vehicleConnection by lazy { VehicleConnection(appContext) }

    /** 퍼즐 C1·C2·C3: 공조 설정값 */
    val climateRepository: ClimateRepository by lazy {
        if (isUsingFakeSdk) FakeClimateRepository() else PleosClimateRepository(vehicleConnection)
    }

    /** 퍼즐 L1·L2·L3: 앰비언트 색·밝기, 독서등 */
    val cabinLightRepository: CabinLightRepository by lazy {
        if (isUsingFakeSdk) FakeCabinLightRepository() else PleosCabinLightRepository(vehicleConnection)
    }

    /** 퍼즐 D1·B1: 핸들 각도, 브레이크 페달 */
    val driveInputRepository: DriveInputRepository by lazy {
        if (isUsingFakeSdk) FakeDriveInputRepository() else PleosDriveInputRepository(vehicleConnection)
    }

    /** F3: 좌석 점유(참가자) */
    val seatRepository: SeatRepository by lazy {
        if (isUsingFakeSdk) FakeSeatRepository() else PleosSeatRepository(vehicleConnection)
    }

    /** 설정(전기차·공회전 안내). */
    val settingsRepository: SettingsRepository by lazy { DataStoreSettingsRepository.create(appContext) }

    /** 진행 중인 판·최고 기록(기기 내 DataStore). SDK와 무관해 Fake 빌드에서도 같은 저장소를 쓴다. */
    val recordStore: RecordStore by lazy { DataStoreRecordStore.create(appContext) }

    /**
     * Fake 빌드 전용 자동 풀이 스크립트(퍼즐마다 6초 뒤 목표값으로 바꾼다, 영상 예비용). 실제 SDK 빌드에서는 null이다.
     * debug 인텐트 `--ez autoPlay false`로 끈다(MainActivity).
     */
    val autoPlayer: AutoPlayer? by lazy {
        if (!isUsingFakeSdk) return@lazy null
        FakeAutoPlayer(
            climateRepository as FakeClimateRepository,
            cabinLightRepository as FakeCabinLightRepository,
            driveInputRepository as FakeDriveInputRepository,
        )
    }

    val drivingStateRepository: DrivingStateRepository by lazy {
        if (isUsingFakeSdk) FakeDrivingStateRepository() else CarDrivingStateRepository(appContext)
    }
}
