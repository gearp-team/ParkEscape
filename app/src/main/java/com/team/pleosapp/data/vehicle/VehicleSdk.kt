package com.team.pleosapp.data.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.constant.area.SeatValue
import android.content.Context
import android.util.Log
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.ValueBounds
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * 앱 전체가 함께 쓰는 Vehicle SDK 연결. 처음 쓸 때 만들고 initialize()한다(비동기 bind. 연결 전 요청은 SDK가 큐에 쌓는다).
 * 앱 프로세스와 수명을 같이 하므로 release()는 부르지 않는다.
 */
class VehicleConnection(context: Context) {
    private val appContext = context.applicationContext

    val vehicle: Vehicle by lazy { Vehicle(appContext).also { it.initialize() } }
}

/** 콜백 응답을 기다리는 최대 시간. 서비스가 없거나 연결되지 않으면 이 시간 뒤 실패로 본다. */
internal const val SDK_TIMEOUT_MS = 5_000L

/**
 * onComplete/onFailed 콜백 API 한 번을 suspend로 감싼다. 시간 안에 답이 없으면 TimeoutException으로 실패한다.
 * SDK가 콜백을 두 번 부르더라도 첫 응답만 쓴다.
 */
internal suspend fun <T> awaitSdk(
    timeoutMs: Long = SDK_TIMEOUT_MS,
    call: (onComplete: (T) -> Unit, onFailed: (Exception) -> Unit) -> Unit,
): Result<T> = withTimeoutOrNull(timeoutMs) {
    suspendCancellableCoroutine { cont ->
        val done = AtomicBoolean(false)
        val fail: (Exception) -> Unit = { e -> if (done.compareAndSet(false, true)) cont.resume(Result.failure(e)) }
        try {
            call({ value -> if (done.compareAndSet(false, true)) cont.resume(Result.success(value)) }, fail)
        } catch (e: Exception) {
            // SDK는 퍼미션이 없으면 콜백 대신 예외를 바로 던진다(VehicleServiceManager). 실패 결과로 바꾼다.
            fail(e)
        }
    }
} ?: Result.failure(TimeoutException("no response in $timeoutMs ms"))

/**
 * 리스너 등록 하나를 Flow로 바꾼다. [subscribe]는 등록하고, 해제 함수를 돌려준다(등록 때와 같은 area·listener로 해제).
 * 등록할 수 없으면(미지원) null을 돌려주고, 그때 Flow는 null을 한 번 내보낸 뒤 값 없이 열려 있다.
 * 리스너의 onFailed는 로그를 남기고 null을 내보낸다.
 */
internal fun <T> listenerFlow(
    tag: String,
    name: String,
    subscribe: suspend (emit: (T?) -> Unit, fail: (Exception) -> Unit) -> (() -> Unit)?,
): Flow<T?> = callbackFlow {
    val fail: (Exception) -> Unit = { e ->
        Log.w(tag, "$name failed: ${e.describe()}")
        trySend(null)
    }
    val unsubscribe = try {
        subscribe({ value -> trySend(value) }, fail)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        // register*가 퍼미션 부족 등으로 예외를 바로 던지면 미지원처럼 null을 내보낸다.
        fail(e)
        return@callbackFlow awaitClose()
    }
    if (unsubscribe == null) trySend(null)
    awaitClose { unsubscribe?.invoke() }
}

/**
 * 좌석별 리스너 여러 개를 좌석 → 값 맵 하나의 Flow로 묶는다(독서등, 좌석 점유).
 * 등록하면 SDK가 좌석마다 현재 값을 한 번씩 보내므로, 모든 좌석이 한 번 이상 보고한 뒤부터 맵을 내보낸다
 * (일부만 들어온 맵으로 판단하지 않게). 오류가 난 좌석은 null. 등록할 좌석이 없으면 빈 맵을 한 번 내보낸다.
 * [register]는 등록하고 해제 함수를 돌려준다(등록 때와 같은 area·listener로 해제).
 */
internal fun <V> seatMapFlow(
    tag: String,
    name: String,
    resolveSeats: suspend () -> Map<SeatPosition, SeatArea>,
    register: (area: SeatArea, onValue: (V?) -> Unit, onFailed: (Exception) -> Unit) -> () -> Unit,
): Flow<Map<SeatPosition, V?>> = callbackFlow {
    val areas = resolveSeats()
    val values = HashMap<SeatPosition, V?>()
    val reported = HashSet<SeatPosition>()

    // 값 갱신과 내보내기를 같은 잠금 안에서 한다. 바인더 스레드 두 개가 겹치면 잠금 밖의 trySend는 순서가 뒤집힐 수 있다(F3 QA 지적).
    fun update(seat: SeatPosition, value: V?) = synchronized(values) {
        values[seat] = value
        reported += seat
        val complete = reported.size == areas.size
        Log.d(tag, "$name $seat=$value reported=${reported.size}/${areas.size} emit=$complete")
        if (complete) trySend(values.toMap())
    }

    if (areas.isEmpty()) trySend(emptyMap())
    val unsubscribers = areas.map { (seat, area) ->
        val fail: (Exception) -> Unit = { e ->
            Log.w(tag, "$name $seat failed: ${e.describe()}")
            update(seat, null)
        }
        try {
            register(area, { value -> update(seat, value) }, fail)
        } catch (e: Exception) {
            // register*가 예외를 바로 던진 좌석은 값 모름(null)으로 두고 해제할 것도 없다.
            fail(e)
            return@map {}
        }
    }
    awaitClose { unsubscribers.forEach { it() } }
}.distinctUntilChanged()

/**
 * capability 조회 결과를 기억한다. 조회가 모두 끝까지 성공했을 때만(complete) 저장하고,
 * 시간 초과·서비스 오류가 섞였으면 다음 호출에서 다시 조회한다.
 */
internal class CapabilityCache<T : Any>(private val load: suspend () -> Pair<T, Boolean>) {
    private val mutex = Mutex()
    private var cached: T? = null

    suspend fun get(): T = mutex.withLock {
        cached ?: load().let { (value, complete) ->
            if (complete) cached = value
            value
        }
    }
}

/**
 * Vehicle 2.3.2 AAR은 VehicleException을 난독화해 배포하고 ErrorCode enum이 없어서 `is VehicleException`을 쓸 수 없다.
 * message에 ErrorCode 이름(예: ERROR_NOT_SUPPORTED_PROPERTY)이 들어 있다. IllegalArgumentException 등도 들어온다.
 */
internal fun Exception.describe(): String = message ?: javaClass.simpleName

/** Result 실패 원인을 로그용 문자열로. Vehicle 오류는 message에 ErrorCode 이름이 들어 있다([describe]와 같은 규칙). */
internal fun Throwable.describeError(): String = message ?: javaClass.simpleName

internal fun AccessType.isReadable(): Boolean = this == AccessType.READ || this == AccessType.READ_WRITE

internal fun SeatValue.toSeatPosition(): SeatPosition? = when (this) {
    SeatValue.ROW_1_LEFT -> SeatPosition.DRIVER
    SeatValue.ROW_1_RIGHT -> SeatPosition.PASSENGER
    SeatValue.ROW_2_LEFT -> SeatPosition.REAR_LEFT
    SeatValue.ROW_2_CENTER -> SeatPosition.REAR_CENTER
    SeatValue.ROW_2_RIGHT -> SeatPosition.REAR_RIGHT
    else -> null // ROW_1_CENTER, 3열: 이 앱은 쓰지 않는다
}

internal fun SeatArea.toSeats(): Set<SeatPosition> = values.mapNotNull { it.toSeatPosition() }.toSet()

internal fun SeatArea.str(): String = values.joinToString("+")

/** capability 결과의 키 가운데 [seat]를 포함하는 것을 고른다(area를 직접 만들지 않는다. 기획 2-1). */
internal fun Collection<SeatArea>.zoneContaining(seat: SeatPosition): SeatArea? = firstOrNull { seat in it.toSeats() }

/** 좌석 하나만 덮는 키를 좌석별로 모은다(좌석별 신호: 독서등, 좌석 점유). */
internal fun Collection<SeatArea>.singleSeatZones(): Map<SeatPosition, SeatArea> =
    mapNotNull { area -> area.toSeats().singleOrNull()?.let { it to area } }.toMap()

internal fun <T> AreaBounds<T>.toValueBounds(): ValueBounds<T> = ValueBounds(minValue, maxValue)
