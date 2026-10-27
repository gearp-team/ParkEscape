package com.team.pleosapp.data.drive

import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.data.signal.ValueBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** SDK 없이 쓰는 핸들·브레이크. 기본값은 중앙·브레이크 뗌, 지원 범위는 에뮬레이터 실측값(−470~500)이다. */
class FakeDriveInputRepository(
    steeringAngle: Float? = 0f,
    brakePressed: Boolean? = false,
    private val support: DriveInputSupport = DEFAULT_SUPPORT,
) : DriveInputRepository {
    private val angleState = MutableStateFlow(steeringAngle)
    private val brakeState = MutableStateFlow(brakePressed)

    override suspend fun support(): DriveInputSupport = support

    override val steeringAngle: Flow<Float?> = angleState.distinctAngles()
    override val brakePressed: Flow<Boolean?> = brakeState

    override suspend fun readSteeringAngle(): Float? = normalizeAngle(angleState.value)

    override suspend fun readBrakePressed(): Boolean? = brakeState.value

    fun setSteeringAngle(value: Float?) {
        angleState.value = value
    }

    fun setBrakePressed(value: Boolean?) {
        brakeState.value = value
    }

    /** 지금 값을 구독 중인 수집자 수(리스너 해제 확인용). */
    val activeCollectors: Int get() = angleState.subscriptionCount.value + brakeState.subscriptionCount.value

    /** 브레이크만 구독 중인 수집자 수(결과 화면이 B1을 구독하지 않는지 확인용). */
    val activeBrakeCollectors: Int get() = brakeState.subscriptionCount.value

    companion object {
        val DEFAULT_SUPPORT = DriveInputSupport(
            steeringAngle = SignalCapability(true, bounds = ValueBounds(-470f, 500f)),
            brakePedal = SignalCapability(true),
        )
    }
}
