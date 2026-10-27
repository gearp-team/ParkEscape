package com.team.pleosapp.data.light

import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.data.signal.ValueBounds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * SDK 없이 쓰는 실내 조명. 기본값은 시연 0단계 초기값(기획 5-1: 앰비언트 Cyan `#00e5ff`, 밝기 100, 독서등 모두 꺼짐)이고,
 * 지원 범위는 에뮬레이터 실측값(밝기 0~100, 독서등 1열·2열 좌우 4좌석)이다. 값은 setter로 바꾼다.
 */
class FakeCabinLightRepository(
    ambientColor: Int? = CYAN,
    ambientBrightness: Int? = 100,
    readingLights: Map<SeatPosition, Boolean?> = READING_SEATS.associateWith { false },
    private val support: CabinLightSupport = DEFAULT_SUPPORT,
) : CabinLightRepository {
    // 리스너로 전달되는 값(Flow). 실제 차 값은 아래 vehicle*에 둔다.
    private val colorState = MutableStateFlow(ambientColor)
    private val brightnessState = MutableStateFlow(ambientBrightness)
    private val readingState = MutableStateFlow(readingLights)
    private var vehicleColor = ambientColor
    private var vehicleBrightness = ambientBrightness
    private var vehicleReading = readingLights

    /**
     * true면 차 값은 바뀌어도 리스너 이벤트를 보내지 않는다(F3 QA 실패 1 재현: 판이 멈춘 동안 차량 서비스가 독서등·앰비언트
     * 이벤트를 보내지 않는 에뮬레이터 동작). read*()는 Pleos 구현처럼 읽은 값을 Flow(공유 상태)에도 써 넣는다.
     */
    var dropEvents: Boolean = false

    override suspend fun support(): CabinLightSupport = support

    override val ambientColor: Flow<Int?> = colorState.map { it?.let(::opaqueArgb) }
    override val ambientBrightness: Flow<Int?> = brightnessState
    override val readingLights: Flow<Map<SeatPosition, Boolean?>> = readingState

    override suspend fun readAmbientColor(): Int? {
        colorState.value = vehicleColor
        return vehicleColor?.let(::opaqueArgb)
    }

    override suspend fun readAmbientBrightness(): Int? {
        brightnessState.value = vehicleBrightness
        return vehicleBrightness
    }

    override suspend fun readReadingLights(): Map<SeatPosition, Boolean?> {
        readingState.value = vehicleReading
        return vehicleReading
    }

    /** SDK처럼 알파 0x00인 값을 넣어도 된다(내보낼 때 불투명으로 바꾼다). */
    fun setAmbientColor(argb: Int?) {
        vehicleColor = argb
        if (!dropEvents) colorState.value = argb
    }

    fun setAmbientBrightness(value: Int?) {
        vehicleBrightness = value
        if (!dropEvents) brightnessState.value = value
    }

    fun setReadingLight(seat: SeatPosition, on: Boolean?) {
        vehicleReading = vehicleReading + (seat to on)
        if (!dropEvents) readingState.value = vehicleReading
    }

    /** 지금 값을 구독 중인 수집자 수(리스너 해제 확인용). */
    val activeCollectors: Int get() = colorState.subscriptionCount.value + brightnessState.subscriptionCount.value + readingState.subscriptionCount.value

    companion object {
        /** 에뮬레이터 기본 앰비언트 색 Cyan `#00e5ff`(SDK 형식, 알파 0x00). */
        const val CYAN = 0x0000E5FF

        /** SDK가 주는 흰색(알파 0x00). 테스트에서 값을 바꿀 때 쓴다. */
        const val WHITE = 0x00FFFFFF

        private val READING_SEATS = listOf(
            SeatPosition.DRIVER,
            SeatPosition.PASSENGER,
            SeatPosition.REAR_LEFT,
            SeatPosition.REAR_RIGHT,
        )

        val DEFAULT_SUPPORT = CabinLightSupport(
            ambientColor = SignalCapability(true, listOf(SeatPosition.entries.toSet())),
            ambientBrightness = SignalCapability(true, listOf(SeatPosition.entries.toSet()), ValueBounds(0, 100)),
            readingLight = SignalCapability(true, READING_SEATS.map { setOf(it) }),
        )
    }
}
