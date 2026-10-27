package com.team.pleosapp.data.signal

/**
 * 좌석 위치. SDK의 SeatValue를 앱 안에서 쓰는 이름으로 옮긴 것이다(SDK 타입이 Repository 밖으로 나가지 않게).
 * 왼쪽 운전석(국내 판매 차량) 기준: DRIVER = ROW_1_LEFT, PASSENGER = ROW_1_RIGHT, REAR_* = ROW_2_*.
 * 에뮬레이터 Virtual Console의 driver·passenger·rear_left·rear_right와 같은 자리다(SPIKE-W1 실측).
 */
enum class SeatPosition {
    DRIVER,
    PASSENGER,
    REAR_LEFT,
    REAR_CENTER,
    REAR_RIGHT,
}

/** capability가 알려 준 값 범위(SDK AreaBounds를 옮긴 것). */
data class ValueBounds<out T>(val min: T, val max: T)

operator fun <T : Comparable<T>> ValueBounds<T>.contains(value: T): Boolean = value >= min && value <= max

/**
 * capability로 확인한 신호 하나의 지원 상태. 퍼즐 생성기의 "지원 신호 풀"은 supported인 신호만 쓴다.
 *
 * @property supported accessType이 READ 또는 READ_WRITE이고, 쓸 area가 capability 결과에 있을 때 true
 * @property zones 실제로 조회·등록에 쓰는 capability area 키가 덮는 좌석들. 키마다 한 원소다(전역 신호는 비어 있다).
 *   area를 직접 만들지 않고 capability 결과의 키를 그대로 쓰기 때문에, 묶음 키(예: 앞좌석 둘)는 좌석이 여럿인 집합으로 보인다.
 * @property bounds 값 범위. 범위가 없는 신호(내외기·색·독서등·브레이크·점유)는 null
 * @property reason 미지원이거나 조회에 실패한 이유(로그·디버그용)
 */
data class SignalCapability<out T>(
    val supported: Boolean,
    val zones: List<Set<SeatPosition>> = emptyList(),
    val bounds: ValueBounds<T>? = null,
    val reason: String? = null,
) {
    companion object {
        fun <T> unsupported(reason: String): SignalCapability<T> = SignalCapability(supported = false, reason = reason)
    }
}
