package com.team.pleosapp.data.light

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import kotlinx.coroutines.flow.Flow

/**
 * 조명(퍼즐 L1·L2·L3 실내 조명, L4 전조등). 읽기만 한다(setAmbientLight*·setLowBeamLightSwitch 등 set* 호출 없음).
 * 외부 등화는 전조등(하향등) 상태만 읽는다. 상향등·안개등·미등 API는 쓰지 않는다.
 */
interface CabinLightRepository {
    /** L1·L2·L3·L4 지원 범위. 처음 부를 때 capability를 조회하고, 끝까지 성공한 결과는 기억한다. */
    suspend fun support(): CabinLightSupport

    /**
     * L1 앰비언트 색(불투명 ARGB). SDK는 알파를 0x00으로 주므로 0xFF로 채워 내보낸다.
     * HSV 판정은 [argbToHsv]로 한다. 미지원·오류면 null.
     */
    val ambientColor: Flow<Int?>

    /** L2 앰비언트 밝기. 미지원·오류면 null. */
    val ambientBrightness: Flow<Int?>

    /**
     * L3 좌석별 독서등(켜짐 true). 지원하는 좌석이 모두 한 번씩 값을 보낸 뒤부터 내보낸다.
     * 값을 모르는 좌석(오류, ON·OFF 외 상태)은 null. 지원 좌석이 없으면 빈 맵.
     */
    val readingLights: Flow<Map<SeatPosition, Boolean?>>

    /** L4 전조등(하향등)이 켜져 있으면 true. 꺼짐·주간주행등은 false. 미지원·오류면 null. */
    val lowBeam: Flow<Boolean?>

    /** 원상 복구 스냅샷용 현재 값 읽기. 실패하면 null. */
    suspend fun readAmbientColor(): Int?

    suspend fun readAmbientBrightness(): Int?

    suspend fun readReadingLights(): Map<SeatPosition, Boolean?>

    suspend fun readLowBeam(): Boolean?
}

data class CabinLightSupport(
    val ambientColor: SignalCapability<Nothing>,
    val ambientBrightness: SignalCapability<Int>,
    /** 좌석 하나만 덮는 독서등 area. zones의 원소마다 좌석 하나다(에뮬레이터: 1열·2열 좌우 4좌석). */
    val readingLight: SignalCapability<Nothing>,
    /** 전조등(하향등) 상태. 전역 신호라 zones는 비어 있다. */
    val lowBeam: SignalCapability<Nothing>,
) {
    /** 독서등 자리 단서를 낼 수 있는 좌석. */
    val readingLightSeats: Set<SeatPosition>
        get() = if (readingLight.supported) readingLight.zones.flatten().toSet() else emptySet()
}
