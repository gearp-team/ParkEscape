package com.team.pleosapp.domain.puzzle

import com.team.pleosapp.data.climate.ClimateSupport
import com.team.pleosapp.data.drive.DriveInputSupport
import com.team.pleosapp.data.light.CabinLightSupport
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.ValueBounds
import kotlinx.serialization.Serializable

/**
 * 퍼즐 신호 10종(기획 2-1). C = 공조, L = 조명(L1~L3 실내, L4 전조등), D1 = 핸들, B1 = 브레이크(봉인, 항상 마지막).
 * C4(앞유리 제상)·L4(전조등)는 위급할 때 쓰는 조작을 손에 익히려고 넣었다(2026-11-06 사용자 결정). 목표는 늘 "켜짐"이다.
 * 이 패키지(domain.puzzle)는 Android에 의존하지 않는 순수 Kotlin이다.
 */
@Serializable
enum class SignalId {
    /** 목표 온도(℃, 운전석 쪽 설정값) */
    C1,

    /** 풍량 단수 */
    C2,

    /** 내외기(true = 내기 순환) */
    C3,

    /** 앞유리 제상(true = 켜짐) */
    C4,

    /** 앰비언트 색 */
    L1,

    /** 앰비언트 밝기 */
    L2,

    /** 독서등(좌석별 자리 단서) */
    L3,

    /** 전조등(하향등, true = 켜짐). 상향등은 쓰지 않는다(맞은편 차를 눈부시게 할 수 있다). */
    L4,

    /** 핸들 각도 */
    D1,

    /** 브레이크 페달(봉인 해제) */
    B1,
    ;

    /** 공조 신호. 비EV는 판당 공조 퍼즐 2개까지다(C4 포함). */
    val isClimate: Boolean get() = this == C1 || this == C2 || this == C3 || this == C4
}

/** 신호 하나를 가리키는 키. L3만 좌석을 갖는다. 복구 체크리스트·스냅샷의 단위다. */
@Serializable
data class SignalKey(val signal: SignalId, val seat: SeatPosition? = null)

/**
 * 판정·스냅샷에 쓰는 차량 신호 묶음. F2-data Repository Flow 값을 그대로 담는다(값을 모르면 null).
 * 앰비언트 색은 ARGB(알파는 보지 않는다), 독서등은 좌석별 켜짐 여부다. 제상·전조등은 켜짐 여부다.
 */
@Serializable
data class VehicleSignals(
    val targetTemperature: Float? = null,
    val fanSpeed: Int? = null,
    val recirculation: Boolean? = null,
    val ambientColor: Int? = null,
    val ambientBrightness: Int? = null,
    val readingLights: Map<SeatPosition, Boolean?> = emptyMap(),
    val steeringAngle: Float? = null,
    val brakePressed: Boolean? = null,
    val defroster: Boolean? = null,
    val lowBeam: Boolean? = null,
)

/**
 * 판 생성기가 쓰는 "지원 신호 풀". F2-data `support()` 결과에서 만든다([from]).
 * 범위가 있는 신호는 범위가 null이면 미지원이다.
 */
data class SignalPool(
    val temperature: ValueBounds<Float>? = null,
    val fanSpeed: ValueBounds<Int>? = null,
    val recirculation: Boolean = false,
    val ambientColor: Boolean = false,
    val ambientBrightness: ValueBounds<Int>? = null,
    /** 독서등 area가 있는 좌석(자리 단서를 낼 수 있는 좌석) */
    val readingLightSeats: Set<SeatPosition> = emptySet(),
    val steering: ValueBounds<Float>? = null,
    val brake: Boolean = false,
    /** C4 앞유리 제상 */
    val defroster: Boolean = false,
    /** L4 전조등(하향등) */
    val lowBeam: Boolean = false,
) {
    /** 지원하는 신호 종류(L3는 독서등 좌석이 하나라도 있으면 포함). */
    val supportedSignals: Set<SignalId>
        get() = buildSet {
            if (temperature != null) add(SignalId.C1)
            if (fanSpeed != null) add(SignalId.C2)
            if (recirculation) add(SignalId.C3)
            if (defroster) add(SignalId.C4)
            if (ambientColor) add(SignalId.L1)
            if (ambientBrightness != null) add(SignalId.L2)
            if (readingLightSeats.isNotEmpty()) add(SignalId.L3)
            if (lowBeam) add(SignalId.L4)
            if (steering != null) add(SignalId.D1)
            if (brake) add(SignalId.B1)
        }

    companion object {
        /** F2-data Repository의 support() 결과로 풀을 만든다. 미지원·조회 실패 신호는 빠진다. */
        fun from(climate: ClimateSupport?, light: CabinLightSupport?, drive: DriveInputSupport?): SignalPool = SignalPool(
            temperature = climate?.targetTemperature?.takeIf { it.supported }?.bounds,
            fanSpeed = climate?.fanSpeed?.takeIf { it.supported }?.bounds,
            recirculation = climate?.recirculation?.supported == true,
            ambientColor = light?.ambientColor?.supported == true,
            ambientBrightness = light?.ambientBrightness?.takeIf { it.supported }?.bounds,
            readingLightSeats = light?.readingLightSeats.orEmpty(),
            steering = drive?.steeringAngle?.takeIf { it.supported }?.bounds,
            brake = drive?.brakePedal?.supported == true,
            defroster = climate?.defroster?.supported == true,
            lowBeam = light?.lowBeam?.supported == true,
        )

        /** 에뮬레이터 실측 capability(SPIKE-W1). 시연 모드에서 실제 범위를 모를 때 쓴다. */
        val EMULATOR = SignalPool(
            temperature = ValueBounds(15f, 30f),
            fanSpeed = ValueBounds(1, 8),
            recirculation = true,
            ambientColor = true,
            ambientBrightness = ValueBounds(0, 100),
            readingLightSeats = setOf(SeatPosition.DRIVER, SeatPosition.PASSENGER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT),
            steering = ValueBounds(-470f, 500f),
            brake = true,
            defroster = true,
            lowBeam = true,
        )

        /**
         * 시연 모드 풀: 10종 전부를 지원으로 고정한다(기획 2-1: capability 결과와 무관, 판정은 실제 신호).
         * 범위는 실제 capability가 있으면 그것을, 없으면 에뮬레이터 값을 쓴다.
         */
        fun demo(actual: SignalPool): SignalPool = SignalPool(
            temperature = actual.temperature ?: EMULATOR.temperature,
            fanSpeed = actual.fanSpeed ?: EMULATOR.fanSpeed,
            recirculation = true,
            ambientColor = true,
            ambientBrightness = actual.ambientBrightness ?: EMULATOR.ambientBrightness,
            readingLightSeats = EMULATOR.readingLightSeats + actual.readingLightSeats,
            steering = actual.steering ?: EMULATOR.steering,
            brake = true,
            defroster = true,
            lowBeam = true,
        )
    }
}
