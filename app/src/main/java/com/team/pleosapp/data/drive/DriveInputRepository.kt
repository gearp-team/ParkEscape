package com.team.pleosapp.data.drive

import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.data.signal.ValueBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * 정차 중 운전 입력(퍼즐 D1 핸들, B1 브레이크). 읽기만 한다.
 */
interface DriveInputRepository {
    /** D1·B1 지원 범위. 처음 부를 때 capability를 조회하고, 끝까지 성공한 결과는 기억한다. */
    suspend fun support(): DriveInputSupport

    /**
     * D1 핸들 각도(도, SDK 값 그대로: 왼쪽이 음수). 같은 값은 거른다(에뮬레이터는 값이 그대로여도 약 3.1초마다 다시 보낸다).
     * 단위는 차마다 다르다: SDK 문서는 앞바퀴 각도라고 하지만 에뮬레이터는 핸들 회전각(±470~500)을 준다.
     * 그래서 퍼즐 목표는 절댓값이 아니라 [DriveInputSupport.steeringAngle]의 bounds 비율로 정한다. 미지원·오류면 null.
     */
    val steeringAngle: Flow<Float?>

    /** B1 브레이크 페달을 밟고 있으면 true(에뮬레이터는 1% 이상이면 true). 미지원·오류면 null. */
    val brakePressed: Flow<Boolean?>

    /** 현재 핸들 각도(중앙 복귀 확인·재동기화용). 실패하면 null. */
    suspend fun readSteeringAngle(): Float?

    /** 현재 브레이크 페달 상태(재개 때 B1 시작 상태로 쓴다). 실패하면 null. */
    suspend fun readBrakePressed(): Boolean?
}

data class DriveInputSupport(
    /** bounds는 양쪽이 비대칭일 수 있다(에뮬레이터 −470~500). 실제 값이 bounds를 조금 넘을 수도 있다(−500까지 관측). */
    val steeringAngle: SignalCapability<Float>,
    val brakePedal: SignalCapability<Nothing>,
)

/** 핸들 범위 폭이 이보다 좁으면(도) 비정상으로 보고 퍼즐 풀에서 뺀다(기획 2-1 D1). */
const val MIN_STEERING_SPAN_DEGREES = 10f

/**
 * 핸들 capability 범위를 퍼즐에 쓸 수 없는 이유. 쓸 수 있으면 null.
 * 목표 칸은 왼쪽(|min|)·오른쪽(max)을 각각 5~15%로 나누므로 0을 사이에 둔 범위여야 하고, 전체 폭이 10° 이상이어야 한다.
 */
fun steeringBoundsProblem(bounds: ValueBounds<Float>): String? = when {
    !(bounds.max - bounds.min >= MIN_STEERING_SPAN_DEGREES) -> "bounds too narrow: ${bounds.min}..${bounds.max}"
    bounds.min >= 0f || bounds.max <= 0f -> "bounds do not span center: ${bounds.min}..${bounds.max}"
    else -> null
}

/** −0.0을 0.0으로 바꾼다. 박싱된 Float의 equals는 둘을 다르게 보아서, 그대로 두면 같은 각도가 걸러지지 않는다. */
fun normalizeAngle(angle: Float?): Float? = if (angle == 0f) 0f else angle

/** 핸들 각도 Flow에서 연속으로 같은 값(−0.0과 0.0 포함)을 거른다. */
fun Flow<Float?>.distinctAngles(): Flow<Float?> = map(::normalizeAngle).distinctUntilChanged()
