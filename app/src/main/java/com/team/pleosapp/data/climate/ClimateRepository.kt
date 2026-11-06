package com.team.pleosapp.data.climate

import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.data.signal.ValueBounds
import com.team.pleosapp.data.signal.contains
import kotlinx.coroutines.flow.Flow

/**
 * 공조 설정값(퍼즐 C1·C2·C3·C4). 읽기만 한다(set* 호출 없음).
 * 온도·풍량·내외기는 capability 키 가운데 운전석(ROW_1_LEFT)을 포함하는 area를, 제상은 앞유리(FRONT_WINDSHIELD)를 포함하는 area를 쓴다.
 */
interface ClimateRepository {
    /** C1·C2·C3·C4 지원 범위. 처음 부를 때 capability를 조회하고, 끝까지 성공한 결과는 기억한다. */
    suspend fun support(): ClimateSupport

    /** C1 운전석 쪽 목표 온도(℃, 설정값). 구독하면 현재 값을 먼저 한 번 내보낸다. 미지원·오류면 null. */
    val targetTemperature: Flow<Float?>

    /** C2 풍량 단수. capability 범위 밖(예: 0 = 꺼짐)이거나 미지원·오류면 null. */
    val fanSpeed: Flow<Int?>

    /** C3 내기 순환이면 true, 외기면 false. 미지원·오류면 null. */
    val recirculation: Flow<Boolean?>

    /** C4 앞유리 제상이 켜져 있으면 true. 미지원·오류면 null. */
    val defroster: Flow<Boolean?>

    /** 원상 복구 스냅샷용 현재 값 읽기. 실패하면 null. */
    suspend fun readTargetTemperature(): Float?

    suspend fun readFanSpeed(): Int?

    suspend fun readRecirculation(): Boolean?

    suspend fun readDefroster(): Boolean?
}

data class ClimateSupport(
    val targetTemperature: SignalCapability<Float>,
    /** 차량 온도 설정 간격(℃, 예: 0.5). 차량이 주지 않으면 null. */
    val temperatureStepCelsius: Float?,
    val fanSpeed: SignalCapability<Int>,
    val recirculation: SignalCapability<Nothing>,
    /** 앞유리 제상. area가 좌석이 아니라 창이라서 zones는 비어 있다. */
    val defroster: SignalCapability<Nothing>,
)

/**
 * 풍량 값을 capability 범위로 거른다. 에뮬레이터는 capability 1~8인데 0(꺼짐)도 보낸다(SPIKE-W1).
 * 범위를 모르면 그대로 둔다.
 */
fun normalizeFanSpeed(value: Int?, bounds: ValueBounds<Int>?): Int? = when {
    value == null -> null
    bounds == null -> value
    value in bounds -> value
    else -> null
}
