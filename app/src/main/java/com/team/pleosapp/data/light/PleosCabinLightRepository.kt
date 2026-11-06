package com.team.pleosapp.data.light

import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.listener.AmbientLightBrightnessColorListener
import ai.pleos.playground.vehicle.listener.AmbientLightBrightnessListener
import ai.pleos.playground.vehicle.listener.LightStateListener
import android.util.Log
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.data.vehicle.CapabilityCache
import com.team.pleosapp.data.vehicle.VehicleConnection
import com.team.pleosapp.data.vehicle.awaitSdk
import com.team.pleosapp.data.vehicle.describe
import com.team.pleosapp.data.vehicle.describeError
import com.team.pleosapp.data.vehicle.isReadable
import com.team.pleosapp.data.vehicle.settledListenerFlow
import com.team.pleosapp.data.vehicle.singleSeatZones
import com.team.pleosapp.data.vehicle.str
import com.team.pleosapp.data.vehicle.toSeats
import com.team.pleosapp.data.vehicle.toValueBounds
import com.team.pleosapp.data.vehicle.zoneContaining
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Vehicle SDK Light 구현. 에뮬레이터: 앰비언트 색·밝기는 5좌석 묶음 area, 독서등은 좌석별 area 4개(READ 전용),
 * 전조등(하향등)은 전역 신호.
 */
class PleosCabinLightRepository(connection: VehicleConnection) : CabinLightRepository {
    private val light by lazy { connection.vehicle.getLight() }

    private class Resolved(
        val support: CabinLightSupport,
        val colorArea: SeatArea?,
        val brightnessArea: SeatArea?,
        val readingLightAreas: Map<SeatPosition, SeatArea>,
        val lowBeamSupported: Boolean,
    )

    private val cache = CapabilityCache { query() }

    override suspend fun support(): CabinLightSupport = cache.get().support

    private suspend fun query(): Pair<Resolved, Boolean> {
        var complete = true

        var colorArea: SeatArea? = null
        val color: SignalCapability<Nothing> = awaitSdk<Pair<CapabilityConfig, List<SeatArea>>> { ok, fail ->
            light.checkAmbientLightBrightnessColorCapabilities(
                onComplete = { config: CapabilityConfig, areas: List<SeatArea> -> ok(config to areas) },
                onFailed = fail,
            )
        }.fold(
            onSuccess = { (config, areas) ->
                // 앰비언트는 실내 전체 조명이라 운전석을 포함하는 키가 없으면 첫 키를 쓴다.
                val area = areas.zoneContaining(SeatPosition.DRIVER) ?: areas.firstOrNull()
                when {
                    !config.accessType.isReadable() -> SignalCapability.unsupported("access=${config.accessType}")
                    area == null -> SignalCapability.unsupported("no area")
                    else -> {
                        colorArea = area
                        SignalCapability(true, listOf(area.toSeats()))
                    }
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )

        var brightnessArea: SeatArea? = null
        val brightness: SignalCapability<Int> = awaitSdk<Pair<CapabilityConfig, Map<SeatArea, AreaBounds<Int>>>> { ok, fail ->
            light.checkAmbientLightBrightnessCapabilities(
                onComplete = { config: CapabilityConfig, map: Map<SeatArea, AreaBounds<Int>> -> ok(config to map) },
                onFailed = fail,
            )
        }.fold(
            onSuccess = { (config, map) ->
                val area = map.keys.zoneContaining(SeatPosition.DRIVER) ?: map.keys.firstOrNull()
                when {
                    !config.accessType.isReadable() -> SignalCapability.unsupported("access=${config.accessType}")
                    area == null -> SignalCapability.unsupported("no area")
                    else -> {
                        brightnessArea = area
                        SignalCapability(true, listOf(area.toSeats()), map.getValue(area).toValueBounds())
                    }
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )

        var readingAreas: Map<SeatPosition, SeatArea> = emptyMap()
        val reading: SignalCapability<Nothing> = awaitSdk<Pair<CapabilityConfig, Map<SeatArea, List<LightState>>>> { ok, fail ->
            light.checkReadingLightStateCapabilities(
                onComplete = { config: CapabilityConfig, map: Map<SeatArea, List<LightState>> -> ok(config to map) },
                onFailed = fail,
            )
        }.fold(
            onSuccess = { (config, map) ->
                // ON을 판정할 수 있는 좌석 하나짜리 키만 쓴다(묶음 키는 어느 자리인지 가릴 수 없다).
                val usable = map.filterValues { states -> states.isEmpty() || LightState.ON in states }.keys
                val zones = usable.singleSeatZones()
                when {
                    !config.accessType.isReadable() -> SignalCapability.unsupported("access=${config.accessType}")
                    zones.isEmpty() -> SignalCapability.unsupported("no single-seat area in ${map.keys.map { it.str() }}")
                    else -> {
                        readingAreas = zones
                        SignalCapability(true, zones.keys.map { setOf(it) })
                    }
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )

        val lowBeam: SignalCapability<Nothing> = awaitSdk<Pair<CapabilityConfig, List<LightState>>> { ok, fail ->
            light.checkLowBeamLightStateCapabilities(
                onComplete = { config: CapabilityConfig, states: List<LightState> -> ok(config to states) },
                onFailed = fail,
            )
        }.fold(
            onSuccess = { (config, states) ->
                when {
                    !config.accessType.isReadable() -> SignalCapability.unsupported("access=${config.accessType}")
                    // 켜짐을 알려 줄 수 없는 차량은 판정할 수 없다(목록이 비어 있으면 제한 없음으로 본다)
                    states.isNotEmpty() && LightState.ON !in states -> SignalCapability.unsupported("states=$states")
                    else -> SignalCapability<Nothing>(true)
                }
            },
            onFailure = { e ->
                complete = false
                SignalCapability.unsupported("capability failed: ${e.describeError()}")
            },
        )

        val support = CabinLightSupport(color, brightness, reading, lowBeam)
        Log.i(TAG, "support: $support")
        return Resolved(support, colorArea, brightnessArea, readingAreas, lowBeam.supported) to complete
    }

    /*
     * 앰비언트 색(L1)·밝기(L2)도 독서등처럼 앱 수명 동안 한 번만 등록해 공유하고, 밝기를 먼저 등록한다(F2-ui QA 실패 1, 리더 결정).
     * SDK 결함: PropertyCallback.isEmptyPleosPropertyId(Vehicle 2.3.2 소스 PropertyCallback.kt:215-224)가 속성 이름 앞부분 일치
     * (startsWith)로 리스너를 세서, INTERIOR_LIGHTS_AMBIENT_BRIGHTNESS가 ..._BRIGHTNESS_COLOR 리스너까지 제 것으로 센다.
     * 그래서 색 리스너가 있는 채로 밝기를 붙이면 차량 서비스 구독(register)을 건너뛰어 밝기 이벤트가 오지 않고,
     * 색 리스너가 남은 채로 밝기를 떼면 구독이 풀리지 않는다. 밝기 → 색 순서로 한 번 등록하고 해제하지 않으면 둘 다 받는다.
     * 화면은 PLAYING·결과 화면(정차)일 때만 이 Flow를 구독하므로 멈춘 동안 들어온 값은 쓰지 않는다. SDK가 고쳐지면 다른 신호처럼 해제한다.
     */
    private val colorUpdates = MutableSharedFlow<Int?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val brightnessUpdates = MutableSharedFlow<Int?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val ambientMutex = Mutex()
    private var colorRegistered = false
    private var brightnessRegistered = false

    private val colorListener = object : AmbientLightBrightnessColorListener {
        override fun onBrightnessColorUpdated(argbColor: Int?) {
            colorUpdates.tryEmit(argbColor?.let(::opaqueArgb))
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "L1 ambient color failed: ${e.describe()}")
            colorUpdates.tryEmit(null)
        }
    }

    private val brightnessListener = object : AmbientLightBrightnessListener {
        override fun onBrightnessUpdated(brightness: Int?) {
            brightnessUpdates.tryEmit(brightness)
        }

        override fun onFailed(e: Exception) {
            Log.w(TAG, "L2 ambient brightness failed: ${e.describe()}")
            brightnessUpdates.tryEmit(null)
        }
    }

    /** 아직 등록하지 않은 앰비언트 리스너를 밝기 → 색 순서로 등록한다. 미지원이면 null을 한 번 내보낸다(listenerFlow와 같은 약속). */
    private suspend fun ensureAmbientRegistered() = ambientMutex.withLock {
        if (colorRegistered && brightnessRegistered) return@withLock
        val resolved = cache.get()
        if (!brightnessRegistered) {
            val area = resolved.brightnessArea
            brightnessRegistered = area != null && register("L2 ambient brightness", brightnessUpdates) {
                light.registerAmbientLightBrightnessListener(area, brightnessListener)
            }
            if (area == null) brightnessUpdates.tryEmit(null)
        }
        if (!colorRegistered) {
            val area = resolved.colorArea
            colorRegistered = area != null && register("L1 ambient color", colorUpdates) {
                light.registerAmbientLightBrightnessColorListener(area, colorListener)
            }
            if (area == null) colorUpdates.tryEmit(null)
        }
    }

    /** register*가 예외를 바로 던지면(퍼미션 등) 값 모름(null)을 내보내고 false. */
    private inline fun register(name: String, updates: MutableSharedFlow<Int?>, block: () -> Unit): Boolean = try {
        block()
        Log.i(TAG, "$name registered (shared)")
        true
    } catch (e: Exception) {
        Log.w(TAG, "$name register failed: ${e.describe()}")
        updates.tryEmit(null)
        false
    }

    override val ambientColor: Flow<Int?> = flow {
        ensureAmbientRegistered()
        emitAll(colorUpdates)
    }

    override val ambientBrightness: Flow<Int?> = flow {
        ensureAmbientRegistered()
        emitAll(brightnessUpdates)
    }

    /*
     * 독서등도 앱 수명 동안 한 번만 등록하고 공유한다. Vehicle 2.3.2의 `unregisterReadingLightState`가 해제 대신
     * `serviceManager.addListener`를 부르는 SDK 결함이 있어서(소스 LightImpl.unregisterReadingLightState, F1 에뮬레이터 로그로 확인),
     * 해제를 부르면 리스너가 다시 등록되고 재개할 때마다 쌓인다. SDK가 고쳐지면 다른 신호처럼 해제한다.
     *
     * 공유 값은 낡을 수 있다(F3 QA 실패 1): 판이 멈춰 다른 신호 리스너를 해제한 동안 차량 서비스가 독서등·앰비언트 이벤트도 보내지 않는다
     * (에뮬레이터 실측, 재개 때 register*가 다시 불린 뒤부터 다시 온다). 그래서 read*()가 get으로 읽은 값을 공유 상태에 그대로 써 넣는다.
     * 화면은 재개·새 판·결과 화면 재진입 때 readAll()로 먼저 읽은 뒤 Flow를 구독하므로, 판정과 게이지가 새 값에서 출발한다.
     */
    private val readingLock = Any()
    private val readingValues = HashMap<SeatPosition, Boolean?>()
    private val readingReported = HashSet<SeatPosition>()
    private var readingSeats: Set<SeatPosition>? = null
    private val readingState = MutableStateFlow<Map<SeatPosition, Boolean?>?>(null)
    private val readingMutex = Mutex()
    private var readingRegistered = false
    private val readingListeners = mutableListOf<LightStateListener>() // 등록한 객체를 앱 수명 동안 붙잡아 둔다

    /** 좌석 하나의 값을 반영한다. 모든 좌석이 한 번 이상 보고한 뒤부터 맵을 내보낸다. 값 갱신과 내보내기는 같은 잠금 안에서 한다. */
    private fun reportReading(seat: SeatPosition, value: Boolean?) = synchronized(readingLock) {
        val seats = readingSeats ?: return@synchronized
        if (seat !in seats) return@synchronized
        readingValues[seat] = value
        readingReported += seat
        if (readingReported.containsAll(seats)) readingState.value = readingValues.toMap()
    }

    private suspend fun ensureReadingRegistered() = readingMutex.withLock {
        if (readingRegistered) return@withLock
        val areas = cache.get().readingLightAreas
        if (areas.isEmpty()) {
            readingState.value = emptyMap() // 독서등 미지원: 빈 맵(다음 구독 때 다시 조회한다)
            return@withLock
        }
        synchronized(readingLock) { readingSeats = areas.keys }
        readingRegistered = true
        areas.forEach { (seat, area) ->
            val listener = object : LightStateListener {
                override fun onLightStateUpdated(state: LightState?) = reportReading(seat, state.isOn())
                override fun onFailed(e: Exception) {
                    Log.w(TAG, "L3 reading light $seat failed: ${e.describe()}")
                    reportReading(seat, null)
                }
            }
            readingListeners += listener
            try {
                light.registerReadingLightState(area, listener)
            } catch (e: Exception) {
                Log.w(TAG, "L3 reading light $seat register failed: ${e.describe()}")
                reportReading(seat, null)
            }
        }
        Log.i(TAG, "L3 reading light registered (shared) seats=${areas.keys.sorted()}")
        // 첫 값 보완: SDK는 첫 리스너를 register 뒤에 맵에 넣고, 등록 직후 오는 첫 값 묶음을 놓친 좌석은 다음 변화까지 비어 있다.
        // 그러면 맵이 끝내 나오지 않아 판 전체가 멈출 수 있으므로(F3 QA 지적) get으로 한 번 채운다.
        val missing = synchronized(readingLock) { areas.keys - readingReported }
        missing.forEach { seat -> reportReading(seat, readReadingLight(seat, areas.getValue(seat))) }
    }

    override val readingLights: Flow<Map<SeatPosition, Boolean?>> = flow {
        ensureReadingRegistered()
        emitAll(readingState.filterNotNull())
    }

    /*
     * 전조등(L4)은 다른 신호처럼 PLAYING 동안만 등록하고 해제한다. Vehicle 2.3.2 LightImpl의 register/unregisterLowBeamLightState는
     * addListener/removeListener를 짝으로 부르고(독서등 같은 해제 결함 없음), 속성 이름 LOW_BEAM_LIGHTS_STATE로 시작하는 다른 속성이 없어
     * PropertyCallback의 이름 앞부분 일치 결함(앰비언트)에도 걸리지 않는다.
     * 다만 등록 직후 차량 서비스가 예전 값(켜짐)을 먼저 보내는 것을 에뮬레이터에서 확인해서, 잠깐 기다린 뒤 get으로 첫 값을 정한다
     * (settledListenerFlow 설명. 그대로 쓰면 전조등 퍼즐이 판 시작과 동시에 풀렸다).
     */
    override val lowBeam: Flow<Boolean?> = settledListenerFlow(TAG, "L4 low beam", read = ::readLowBeam) { emit, fail ->
        if (!cache.get().lowBeamSupported) return@settledListenerFlow null
        val listener = object : LightStateListener {
            override fun onLightStateUpdated(state: LightState?) = emit(state.isLowBeamOn())
            override fun onFailed(e: Exception) = fail(e)
        }
        light.registerLowBeamLightState(listener)
        return@settledListenerFlow { light.unregisterLowBeamLightState(listener) }
    }

    override suspend fun readLowBeam(): Boolean? {
        if (!cache.get().lowBeamSupported) return null
        return awaitSdk<LightState?> { ok, fail -> light.getCurrentLowBeamLightState(ok, fail) }
            .onFailure { Log.w(TAG, "L4 read failed: ${it.describeError()}") }
            .getOrNull()
            .isLowBeamOn()
    }

    override suspend fun readAmbientColor(): Int? {
        val area = cache.get().colorArea ?: return null
        val result = awaitSdk<Int?> { ok, fail -> light.getAmbientLightBrightnessColor(area, ok, fail) }
            .onFailure { Log.w(TAG, "L1 read failed: ${it.describeError()}") }
        val value = result.getOrNull()?.let(::opaqueArgb)
        if (result.isSuccess) colorUpdates.tryEmit(value) // 공유 상태도 지금 값으로(위 설명)
        return value
    }

    override suspend fun readAmbientBrightness(): Int? {
        val area = cache.get().brightnessArea ?: return null
        val result = awaitSdk<Int?> { ok, fail -> light.getAmbientLightBrightness(area, ok, fail) }
            .onFailure { Log.w(TAG, "L2 read failed: ${it.describeError()}") }
        val value = result.getOrNull()
        if (result.isSuccess) brightnessUpdates.tryEmit(value)
        return value
    }

    override suspend fun readReadingLights(): Map<SeatPosition, Boolean?> {
        val values = cache.get().readingLightAreas.mapValues { (seat, area) -> readReadingLight(seat, area) }
        values.forEach { (seat, value) -> reportReading(seat, value) } // 등록 전이면 무시된다
        return values
    }

    private suspend fun readReadingLight(seat: SeatPosition, area: SeatArea): Boolean? =
        awaitSdk<LightState?> { ok, fail -> light.getCurrentReadingLightState(area, ok, fail) }
            .onFailure { Log.w(TAG, "L3 $seat read failed: ${it.describeError()}") }
            .getOrNull()
            .isOn()

    private companion object {
        const val TAG = "PleosCabinLightRepo"
    }
}

/** 전조등: ON → true, OFF·DAYTIME_RUNNING(주간주행등만 켜짐) → false, 값 없음 → null. */
private fun LightState?.isLowBeamOn(): Boolean? = when (this) {
    LightState.ON -> true
    LightState.OFF, LightState.DAYTIME_RUNNING -> false
    null -> null
}

/** ON → true, OFF → false. 그 밖(DAYTIME_RUNNING, null)은 독서등 판정에 쓸 수 없어 null. */
private fun LightState?.isOn(): Boolean? = when (this) {
    LightState.ON -> true
    LightState.OFF -> false
    else -> null
}
