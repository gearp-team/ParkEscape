package com.team.pleosapp.data.climate

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.data.signal.ValueBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * SDK 없이 쓰는 공조. 기본값은 시연 0단계 초기값(목표 온도 24℃, 풍량 2단, 외기, 앞유리 제상 꺼짐)이고,
 * 지원 범위는 에뮬레이터 실측값(온도 15~30℃·0.5 간격, 풍량 1~8)이다. 값은 setter로 바꾼다.
 */
class FakeClimateRepository(
    targetTemperature: Float? = 24f,
    fanSpeed: Int? = 2,
    recirculation: Boolean? = false,
    defroster: Boolean? = false,
    private val support: ClimateSupport = DEFAULT_SUPPORT,
) : ClimateRepository {
    private val temperatureState = MutableStateFlow(targetTemperature)
    private val fanState = MutableStateFlow(fanSpeed)
    private val recirculationState = MutableStateFlow(recirculation)
    private val defrosterState = MutableStateFlow(defroster)

    override suspend fun support(): ClimateSupport = support

    override val targetTemperature: Flow<Float?> = temperatureState
    override val fanSpeed: Flow<Int?> = fanState.map { normalizeFanSpeed(it, support.fanSpeed.bounds) }
    override val recirculation: Flow<Boolean?> = recirculationState
    override val defroster: Flow<Boolean?> = defrosterState

    override suspend fun readTargetTemperature(): Float? = temperatureState.value
    override suspend fun readFanSpeed(): Int? = normalizeFanSpeed(fanState.value, support.fanSpeed.bounds)
    override suspend fun readRecirculation(): Boolean? = recirculationState.value
    override suspend fun readDefroster(): Boolean? = defrosterState.value

    fun setTargetTemperature(value: Float?) {
        temperatureState.value = value
    }

    fun setFanSpeed(value: Int?) {
        fanState.value = value
    }

    fun setRecirculation(value: Boolean?) {
        recirculationState.value = value
    }

    fun setDefroster(value: Boolean?) {
        defrosterState.value = value
    }

    /** 지금 값을 구독 중인 수집자 수(리스너 해제 확인용). */
    val activeCollectors: Int
        get() = temperatureState.subscriptionCount.value + fanState.subscriptionCount.value +
            recirculationState.subscriptionCount.value + defrosterState.subscriptionCount.value

    companion object {
        private val FRONT = setOf(SeatPosition.DRIVER, SeatPosition.PASSENGER)
        private val CABIN = SeatPosition.entries.toSet()

        val DEFAULT_SUPPORT = ClimateSupport(
            targetTemperature = SignalCapability(true, listOf(setOf(SeatPosition.DRIVER)), ValueBounds(15f, 30f)),
            temperatureStepCelsius = 0.5f,
            fanSpeed = SignalCapability(true, listOf(FRONT), ValueBounds(1, 8)),
            recirculation = SignalCapability(true, listOf(CABIN)),
            defroster = SignalCapability(true),
        )
    }
}
