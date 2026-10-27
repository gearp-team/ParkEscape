package com.team.pleosapp.spike

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.api.model.capability.HvacTemperatureConfigArray
import ai.pleos.playground.vehicle.constant.area.SeatValue
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import ai.pleos.playground.vehicle.listener.AmbientLightBrightnessColorListener
import ai.pleos.playground.vehicle.listener.AmbientLightBrightnessListener
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.HvacFanSpeedListener
import ai.pleos.playground.vehicle.listener.HvacRecirculationStateListener
import ai.pleos.playground.vehicle.listener.HvacTemperatureListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import ai.pleos.playground.vehicle.listener.SeatOccupancyListener
import ai.pleos.playground.vehicle.listener.SteeringWheelAngleListener
import com.team.pleosapp.data.vehicle.describe

/**
 * 퍼즐 신호 8종(C1·C2·C3·L1·L2·L3·D1·B1)과 좌석 점유(F3)를 capability → get → register 순서로 훑고 모두 로그로 남긴다.
 * 로그 형식(태그 SPIKE):
 *   CAP <sig> access=<AccessType> areas=<area=bounds;...>
 *   GET <sig> area=<area> v=<값>
 *   CB  <sig> area=<area> v=<값>
 *   FAIL <단계> <sig> area=<area> e=<message>
 * 등록은 capability가 알려 준 area로만 한다. GETX는 비교용으로 단일 좌석 area를 직접 만들어 조회한 결과다.
 */
class SignalProbe(
    private val vehicle: Vehicle,
    private val onValue: (key: String, value: String) -> Unit,
) {
    private val cleanups = mutableListOf<() -> Unit>()

    fun start() {
        SpikeLog.i("PROBE start")
        probeTemperature()
        probeFanSpeed()
        probeRecirculation()
        probeAmbientColor()
        probeAmbientBrightness()
        probeReadingLight()
        probeSteering()
        probeBrake()
        probeSeatOccupancy()
        probeAmbientSwitch()
    }

    fun stop() {
        cleanups.forEach { it() }
        cleanups.clear()
        SpikeLog.i("PROBE stop")
    }

    // ---- C1 목표 온도 -------------------------------------------------------------------------
    private fun probeTemperature() {
        val hvac = vehicle.getHVAC()
        hvac.checkHvacTemperatureCapabilities(
            onComplete = { config: CapabilityConfig, map: Map<SeatArea, AreaBounds<Float>>, cfg: HvacTemperatureConfigArray? ->
                cap("C1", config, map.entries.joinToString(";") { "${it.key.str()}=${it.value.minValue}..${it.value.maxValue}" }, "cfg=$cfg")
                map.keys.forEach { area ->
                    hvac.getHvacTemperature(area, TemperatureUnit.CELSIUS, { v -> get("C1", area, v) }, { e -> fail("GET", "C1", area, e) })
                    val l = object : HvacTemperatureListener {
                        override fun onHvacTemperatureUpdated(temperature: Float?) = cb("C1", area, temperature)
                        override fun onFailed(e: Exception) = fail("CB", "C1", area, e)
                    }
                    hvac.registerHvacTemperature(area, l)
                    cleanups += { hvac.unregisterHvacTemperature(area, l) }
                }
                // 기획 2-1의 주의(묶음 area와 직접 만든 area가 어긋나면 미지원 오류)를 확인하기 위한 비교 조회
                val single = SeatArea(SeatValue.ROW_1_LEFT)
                hvac.getHvacTemperature(single, TemperatureUnit.CELSIUS, { v -> getx("C1", single, v) }, { e -> fail("GETX", "C1", single, e) })
            },
            onFailed = { e -> fail("CAP", "C1", null, e) },
        )
    }

    // ---- C2 풍량 ------------------------------------------------------------------------------
    private fun probeFanSpeed() {
        val hvac = vehicle.getHVAC()
        hvac.checkHvacFanSpeedCapabilities(
            onComplete = { config: CapabilityConfig, map: Map<SeatArea, AreaBounds<Int>> ->
                cap("C2", config, map.entries.joinToString(";") { "${it.key.str()}=${it.value.minValue}..${it.value.maxValue}" })
                map.keys.forEach { area ->
                    hvac.getHvacFanSpeed(area, { v -> get("C2", area, v) }, { e -> fail("GET", "C2", area, e) })
                    val l = object : HvacFanSpeedListener {
                        override fun onHvacFanSpeedUpdated(speed: Int?) = cb("C2", area, speed)
                        override fun onFailed(e: Exception) = fail("CB", "C2", area, e)
                    }
                    hvac.registerHvacFanSpeed(area, l)
                    cleanups += { hvac.unregisterHvacFanSpeed(area, l) }
                }
            },
            onFailed = { e -> fail("CAP", "C2", null, e) },
        )
    }

    // ---- C3 내외기 ----------------------------------------------------------------------------
    private fun probeRecirculation() {
        val hvac = vehicle.getHVAC()
        hvac.checkHvacRecirculationCapabilities(
            onComplete = { config: CapabilityConfig, areas: List<SeatArea> ->
                cap("C3", config, areas.joinToString(";") { it.str() })
                areas.forEach { area ->
                    hvac.getHvacRecirculationState(area, { v -> get("C3", area, v) }, { e -> fail("GET", "C3", area, e) })
                    val l = object : HvacRecirculationStateListener {
                        override fun onHvacAutoRecirculationStateUpdated(state: Boolean?) = cb("C3", area, state)
                        override fun onFailed(e: Exception) = fail("CB", "C3", area, e)
                    }
                    hvac.registerHvacRecirculationState(area, l)
                    cleanups += { hvac.unregisterHvacRecirculationState(area, l) }
                }
            },
            onFailed = { e -> fail("CAP", "C3", null, e) },
        )
    }

    // ---- L1 앰비언트 색 -----------------------------------------------------------------------
    private fun probeAmbientColor() {
        val light = vehicle.getLight()
        light.checkAmbientLightBrightnessColorCapabilities(
            onComplete = { config: CapabilityConfig, areas: List<SeatArea> ->
                cap("L1", config, areas.joinToString(";") { it.str() })
                areas.forEach { area ->
                    light.getAmbientLightBrightnessColor(area, { v -> get("L1", area, v?.let(::argb)) }, { e -> fail("GET", "L1", area, e) })
                    val l = object : AmbientLightBrightnessColorListener {
                        override fun onBrightnessColorUpdated(argbColor: Int?) = cb("L1", area, argbColor?.let(::argb))
                        override fun onFailed(e: Exception) = fail("CB", "L1", area, e)
                    }
                    light.registerAmbientLightBrightnessColorListener(area, l)
                    cleanups += { light.unregisterAmbientLightBrightnessColorListener(area, l) }
                }
            },
            onFailed = { e -> fail("CAP", "L1", null, e) },
        )
    }

    // ---- L2 앰비언트 밝기 ---------------------------------------------------------------------
    private fun probeAmbientBrightness() {
        val light = vehicle.getLight()
        light.checkAmbientLightBrightnessCapabilities(
            onComplete = { config: CapabilityConfig, map: Map<SeatArea, AreaBounds<Int>> ->
                cap("L2", config, map.entries.joinToString(";") { "${it.key.str()}=${it.value.minValue}..${it.value.maxValue}" })
                map.keys.forEach { area ->
                    light.getAmbientLightBrightness(area, { v -> get("L2", area, v) }, { e -> fail("GET", "L2", area, e) })
                    val l = object : AmbientLightBrightnessListener {
                        override fun onBrightnessUpdated(brightness: Int?) = cb("L2", area, brightness)
                        override fun onFailed(e: Exception) = fail("CB", "L2", area, e)
                    }
                    light.registerAmbientLightBrightnessListener(area, l)
                    cleanups += { light.unregisterAmbientLightBrightnessListener(area, l) }
                }
            },
            onFailed = { e -> fail("CAP", "L2", null, e) },
        )
    }

    // ---- L3 독서등 ----------------------------------------------------------------------------
    private fun probeReadingLight() {
        val light = vehicle.getLight()
        light.checkReadingLightStateCapabilities(
            onComplete = { config: CapabilityConfig, map: Map<SeatArea, List<LightState>> ->
                cap("L3", config, map.entries.joinToString(";") { "${it.key.str()}=${it.value}" })
                map.keys.forEach { area ->
                    light.getCurrentReadingLightState(area, { v -> get("L3", area, v) }, { e -> fail("GET", "L3", area, e) })
                    val l = object : LightStateListener {
                        override fun onLightStateUpdated(state: LightState?) = cb("L3", area, state)
                        override fun onFailed(e: Exception) = fail("CB", "L3", area, e)
                    }
                    light.registerReadingLightState(area, l)
                    cleanups += { light.unregisterReadingLightState(area, l) }
                }
                FOUR_SEATS.forEach { single ->
                    light.getCurrentReadingLightState(single, { v -> getx("L3", single, v) }, { e -> fail("GETX", "L3", single, e) })
                }
            },
            onFailed = { e -> fail("CAP", "L3", null, e) },
        )
    }

    // ---- D1 핸들 각도 -------------------------------------------------------------------------
    private fun probeSteering() {
        val wheel = vehicle.getSteeringWheel()
        wheel.checkSteeringWheelAngleCapabilities(
            onComplete = { config: CapabilityConfig, bounds: AreaBounds<Float> ->
                cap("D1", config, "global=${bounds.minValue}..${bounds.maxValue}")
                wheel.getCurrentSteeringWheelAngle({ v -> get("D1", null, v) }, { e -> fail("GET", "D1", null, e) })
                val l = object : SteeringWheelAngleListener {
                    override fun onSteeringWheelAngleUpdated(angle: Float?) = cb("D1", null, angle)
                    override fun onFailed(e: Exception) = fail("CB", "D1", null, e)
                }
                wheel.registerSteeringWheelAngle(l)
                cleanups += { wheel.unregisterSteeringWheelAngle(l) }
            },
            onFailed = { e -> fail("CAP", "D1", null, e) },
        )
    }

    // ---- B1 브레이크 페달 ---------------------------------------------------------------------
    private fun probeBrake() {
        val brake = vehicle.getBrake()
        brake.checkBrakePedalStatusCapabilities(
            onComplete = { config: CapabilityConfig ->
                cap("B1", config, "global")
                brake.getCurrentBrakePedalStatus({ v -> get("B1", null, v) }, { e -> fail("GET", "B1", null, e) })
                val l = object : BrakePedalStatusListener {
                    override fun onBrakePedalStatusUpdated(status: Boolean?) = cb("B1", null, status)
                    override fun onFailed(e: Exception) = fail("CB", "B1", null, e)
                }
                brake.registerBrakePedalStatus(l)
                cleanups += { brake.unregisterBrakePedalStatus(l) }
            },
            onFailed = { e -> fail("CAP", "B1", null, e) },
        )
    }

    // ---- F3 좌석 점유 -------------------------------------------------------------------------
    private fun probeSeatOccupancy() {
        val seat = vehicle.getSeat()
        seat.checkSeatOccupancyCapabilities(
            onComplete = { config: CapabilityConfig, areas: List<SeatArea> ->
                cap("F3", config, areas.joinToString(";") { it.str() })
                areas.forEach { area ->
                    seat.getCurrentSeatOccupancy(area, { v -> get("F3", area, v) }, { e -> fail("GET", "F3", area, e) })
                    val l = object : SeatOccupancyListener {
                        override fun onSeatOccupancyUpdated(occupancy: Boolean?) = cb("F3", area, occupancy)
                        override fun onFailed(e: Exception) = fail("CB", "F3", area, e)
                    }
                    seat.registerSeatOccupancy(area, l)
                    cleanups += { seat.unregisterSeatOccupancy(area, l) }
                }
                FOUR_SEATS.forEach { single ->
                    seat.getCurrentSeatOccupancy(single, { v -> getx("F3", single, v) }, { e -> fail("GETX", "F3", single, e) })
                }
            },
            onFailed = { e -> fail("CAP", "F3", null, e) },
        )
    }

    // ---- L1·L2 보조: 앰비언트 스위치(리스너 API 없음, get만) ------------------------------------
    /** capability를 조회하고 키마다 getAmbientLightSwitch를 부른다. am start --es probe ambient-switch로 다시 부를 수 있다. */
    fun probeAmbientSwitch() {
        val light = vehicle.getLight()
        light.checkAmbientLightSwitchCapabilities(
            onComplete = { config: CapabilityConfig, map: Map<SeatArea, List<LightSwitch>> ->
                cap("L1SW", config, map.entries.joinToString(";") { "${it.key.str()}=${it.value}" })
                map.keys.forEach { area ->
                    light.getAmbientLightSwitch(area, { v -> get("L1SW", area, v) }, { e -> fail("GET", "L1SW", area, e) })
                }
            },
            onFailed = { e -> fail("CAP", "L1SW", null, e) },
        )
    }

    // ---- 로그 ---------------------------------------------------------------------------------
    private fun cap(sig: String, config: CapabilityConfig, areas: String, extra: String = "") {
        SpikeLog.i("CAP $sig access=${config.accessType} areas=[$areas] $extra".trimEnd())
        onValue("$sig cap", "${config.accessType} [$areas]")
    }

    private fun get(sig: String, area: SeatArea?, v: Any?) {
        SpikeLog.i("GET $sig area=${area.str()} v=$v")
        onValue("$sig ${area.str()}", "get=$v")
    }

    private fun getx(sig: String, area: SeatArea, v: Any?) {
        SpikeLog.i("GETX $sig area=${area.str()} v=$v")
    }

    private fun cb(sig: String, area: SeatArea?, v: Any?) {
        SpikeLog.i("CB $sig area=${area.str()} v=$v thread=${Thread.currentThread().name}")
        onValue("$sig ${area.str()}", "cb=$v")
    }

    private fun fail(stage: String, sig: String, area: SeatArea?, e: Exception) {
        SpikeLog.w("FAIL $stage $sig area=${area.str()} e=${e.javaClass.simpleName}:${e.describe()}")
        if (stage != "GETX") onValue("$sig ${area.str()}", "fail $stage ${e.describe()}")
    }

    private companion object {
        val FOUR_SEATS = listOf(
            SeatArea(SeatValue.ROW_1_LEFT),
            SeatArea(SeatValue.ROW_1_RIGHT),
            SeatArea(SeatValue.ROW_2_LEFT),
            SeatArea(SeatValue.ROW_2_RIGHT),
        )
    }
}

private fun SeatArea?.str(): String = this?.values?.joinToString("+") ?: "global"

/** ARGB를 16진수와 HSV(색상°, 채도, 명도)로 함께 적는다. 앰비언트 색 판정(색상차·채도)을 확인하기 위해. */
private fun argb(color: Int): String {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color, hsv)
    return String.format(java.util.Locale.US, "0x%08X(h=%.1f,s=%.2f,v=%.2f)", color, hsv[0], hsv[1], hsv[2])
}
