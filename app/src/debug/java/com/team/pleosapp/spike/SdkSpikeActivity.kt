package com.team.pleosapp.spike

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.SeatArea
import ai.pleos.playground.vehicle.api.model.capability.AreaBounds
import ai.pleos.playground.vehicle.api.model.capability.CapabilityConfig
import ai.pleos.playground.vehicle.api.model.capability.HvacTemperatureConfigArray
import ai.pleos.playground.vehicle.constant.control.LightState
import ai.pleos.playground.vehicle.constant.control.LightSwitch
import ai.pleos.playground.vehicle.constant.unit.TemperatureUnit
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.vehicle.awaitSdk
import com.team.pleosapp.data.vehicle.describeError
import com.team.pleosapp.data.vehicle.str
import com.team.pleosapp.data.vehicle.zoneContaining
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * SDK 스파이크(debug 빌드 전용). 설명회(10/28)에서 공모전은 퍼미션 선언만으로 모든 SDK를 쓸 수 있다고 해,
 * 그동안 쓰지 않던 TextToSpeech(ON_DEVICE)와 Vehicle 제어(set*)가 에뮬레이터에서 실제로 되는지 확인한다.
 * 결과는 logcat 태그 SDKSPIKE로 남긴다. 제어로 바꾼 값은 확인 뒤 원래 값으로 되돌린다.
 *   adb shell am start -n com.team.pleosapp/.spike.SdkSpikeActivity --es step caps   (제어 capability의 accessType)
 *   ... --es step tts    (ON_DEVICE 초기화 → 한국어 문장 읽기)
 *   ... --es step hvac   (목표 온도·풍량 set → 다시 읽기 → 원복)
 *   ... --es step light  (앰비언트 색 set, 조수석 독서등 set → 다시 읽기 → 원복)
 */
class SdkSpikeActivity : ComponentActivity() {
    private val lines = mutableStateListOf<String>()
    private val vehicle by lazy { Vehicle(applicationContext).also { it.initialize() } }
    private var tts: TextToSpeech? = null
    private val started = SystemClock.elapsedRealtime()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF1B1F24)).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text("SDK SPIKE", color = Color.White, fontSize = 36.sp)
                    lines.takeLast(30).forEach { Text(it, color = Color(0xFFB8E0C8), fontSize = 20.sp) }
                }
            }
        }
        run(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        run(intent)
    }

    override fun onDestroy() {
        tts?.release()
        super.onDestroy()
    }

    private fun run(intent: Intent?) {
        when (val step = intent?.getStringExtra("step") ?: "caps") {
            "caps" -> lifecycleScope.launch { caps() }
            "tts" -> speakTest(intent?.getStringExtra("text") ?: "냉각 코일을 이십이 도로 맞추세요. 맞으면 다음 단서가 열립니다.")
            "hvac" -> lifecycleScope.launch { hvac() }
            "light" -> lifecycleScope.launch { light() }
            else -> log("unknown step $step")
        }
    }

    // ---- 제어 capability ----

    private suspend fun caps() {
        val hvac = vehicle.getHVAC()
        val light = vehicle.getLight()
        awaitSdk<Pair<CapabilityConfig, Map<SeatArea, AreaBounds<Float>>>> { ok, fail ->
            hvac.checkHvacTemperatureCapabilities(
                onComplete = { c: CapabilityConfig, m: Map<SeatArea, AreaBounds<Float>>, _: HvacTemperatureConfigArray? -> ok(c to m) },
                onFailed = fail,
            )
        }.report("CAP C1 temperature") { (c, m) -> "access=${c.accessType} areas=${m.keys.map { it.str() }}" }
        awaitSdk<Pair<CapabilityConfig, Map<SeatArea, AreaBounds<Int>>>> { ok, fail ->
            hvac.checkHvacFanSpeedCapabilities(onComplete = { c: CapabilityConfig, m: Map<SeatArea, AreaBounds<Int>> -> ok(c to m) }, onFailed = fail)
        }.report("CAP C2 fan") { (c, m) -> "access=${c.accessType} areas=${m.keys.map { it.str() }}" }
        awaitSdk<Pair<CapabilityConfig, List<SeatArea>>> { ok, fail ->
            light.checkAmbientLightBrightnessColorCapabilities(onComplete = { c: CapabilityConfig, a: List<SeatArea> -> ok(c to a) }, onFailed = fail)
        }.report("CAP L1 ambient color") { (c, a) -> "access=${c.accessType} areas=${a.map { it.str() }}" }
        awaitSdk<Pair<CapabilityConfig, Map<SeatArea, List<LightSwitch>>>> { ok, fail ->
            light.checkReadingLightSwitchCapabilities(onComplete = { c: CapabilityConfig, m: Map<SeatArea, List<LightSwitch>> -> ok(c to m) }, onFailed = fail)
        }.report("CAP L3 reading switch") { (c, m) -> "access=${c.accessType} areas=${m.map { (k, v) -> "${k.str()}=$v" }}" }
    }

    // ---- TextToSpeech ----

    private fun speakTest(text: String) {
        val existing = tts
        if (existing != null) {
            log("TTS speak (reuse) \"$text\"")
            runCatching { existing.speak(text) }.onFailure { log("TTS speak threw ${it.describeError()}") }
            return
        }
        log("TTS init ON_DEVICE")
        val engine = TextToSpeech(applicationContext, Mode.ON_DEVICE)
        tts = engine
        runCatching {
            engine.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    log("TTS init onSuccess version=${runCatching { TextToSpeech.version }.getOrNull()}")
                    runCatching {
                        engine.addEventListener(events)
                        // Locale.KOREAN("ko")은 엔진이 "unsupported locale: ko-"로 거절한다(실측). 국가까지 넣는다.
                        engine.setLocale(Locale.KOREA)
                        log("TTS speak \"$text\"")
                        engine.speak(text)
                    }.onFailure { log("TTS speak threw ${it.describeError()}") }
                }

                override fun onFailure(errorType: ErrorType) = log("TTS init onFailure $errorType")
            })
        }.onFailure { log("TTS initialize threw ${it.describeError()}") }
    }

    private val events = object : EventListener {
        override fun onReady() = log("TTS onReady")
        override fun onStart() = log("TTS onStart")
        override fun onUpdatedRms(rms: Double) = Unit
        override fun onDone() = log("TTS onDone")
        override fun onStop() = log("TTS onStop")
        override fun onError(errorType: ErrorType) = log("TTS onError $errorType")
    }

    // ---- 공조 제어 ----

    private suspend fun hvac() {
        val hvac = vehicle.getHVAC()
        val tempArea = awaitSdk<Map<SeatArea, AreaBounds<Float>>> { ok, fail ->
            hvac.checkHvacTemperatureCapabilities(
                onComplete = { _: CapabilityConfig, m: Map<SeatArea, AreaBounds<Float>>, _: HvacTemperatureConfigArray? -> ok(m) },
                onFailed = fail,
            )
        }.getOrNull()?.keys?.zoneContaining(SeatPosition.DRIVER)
        if (tempArea == null) {
            log("HVAC no driver temperature area")
        } else {
            val before = awaitSdk<Float?> { ok, fail -> hvac.getHvacTemperature(tempArea, TemperatureUnit.CELSIUS, ok, fail) }.getOrNull()
            val target = if (before != null && before < 27f) before + 1f else 22f
            log("HVAC C1 before=$before set=$target area=${tempArea.str()}")
            awaitSdk<Pair<SeatArea, Float?>> { ok, fail ->
                hvac.setHvacTemperature(tempArea, target, TemperatureUnit.CELSIUS, onComplete = { a, v -> ok(a to v) }, onFailed = fail)
            }.report("HVAC C1 set") { (_, v) -> "onComplete value=$v" }
            delay(1_500)
            val after = awaitSdk<Float?> { ok, fail -> hvac.getHvacTemperature(tempArea, TemperatureUnit.CELSIUS, ok, fail) }.getOrNull()
            log("HVAC C1 reread=$after ${if (after == target) "APPLIED" else "NOT APPLIED"}")
            if (before != null) {
                awaitSdk<Pair<SeatArea, Float?>> { ok, fail ->
                    hvac.setHvacTemperature(tempArea, before, TemperatureUnit.CELSIUS, onComplete = { a, v -> ok(a to v) }, onFailed = fail)
                }.report("HVAC C1 restore") { (_, v) -> "value=$v" }
            }
        }

        val fanArea = awaitSdk<Map<SeatArea, AreaBounds<Int>>> { ok, fail ->
            hvac.checkHvacFanSpeedCapabilities(onComplete = { _: CapabilityConfig, m: Map<SeatArea, AreaBounds<Int>> -> ok(m) }, onFailed = fail)
        }.getOrNull()?.keys?.zoneContaining(SeatPosition.DRIVER)
        if (fanArea == null) {
            log("HVAC no driver fan area")
            return
        }
        val fanBefore = awaitSdk<Int?> { ok, fail -> hvac.getHvacFanSpeed(fanArea, ok, fail) }.getOrNull()
        val fanTarget = if (fanBefore == 3) 4 else 3
        log("HVAC C2 before=$fanBefore set=$fanTarget area=${fanArea.str()}")
        awaitSdk<Pair<SeatArea, Int?>> { ok, fail ->
            hvac.setHvacFanSpeed(fanArea, fanTarget, onComplete = { a, v -> ok(a to v) }, onFailed = fail)
        }.report("HVAC C2 set") { (_, v) -> "onComplete value=$v" }
        delay(1_500)
        val fanAfter = awaitSdk<Int?> { ok, fail -> hvac.getHvacFanSpeed(fanArea, ok, fail) }.getOrNull()
        log("HVAC C2 reread=$fanAfter ${if (fanAfter == fanTarget) "APPLIED" else "NOT APPLIED"}")
        if (fanBefore != null) {
            awaitSdk<Pair<SeatArea, Int?>> { ok, fail ->
                hvac.setHvacFanSpeed(fanArea, fanBefore, onComplete = { a, v -> ok(a to v) }, onFailed = fail)
            }.report("HVAC C2 restore") { (_, v) -> "value=$v" }
        }
    }

    // ---- 조명 제어 ----

    private suspend fun light() {
        val light = vehicle.getLight()
        val colorArea = awaitSdk<List<SeatArea>> { ok, fail ->
            light.checkAmbientLightBrightnessColorCapabilities(onComplete = { _: CapabilityConfig, a: List<SeatArea> -> ok(a) }, onFailed = fail)
        }.getOrNull()?.let { it.zoneContaining(SeatPosition.DRIVER) ?: it.firstOrNull() }
        if (colorArea == null) {
            log("LIGHT no ambient color area")
        } else {
            val before = awaitSdk<Int?> { ok, fail -> light.getAmbientLightBrightnessColor(colorArea, ok, fail) }.getOrNull()
            val target = 0xFFAA00FF.toInt() // 보라
            log("LIGHT L1 before=${before?.hex()} set=${target.hex()} area=${colorArea.str()}")
            awaitSdk<Pair<SeatArea, Int>> { ok, fail ->
                light.setAmbientLightBrightnessColor(colorArea, target, onComplete = { a, v -> ok(a to v) }, onFailed = fail)
            }.report("LIGHT L1 set") { (_, v) -> "onComplete value=${v.hex()}" }
            delay(1_500)
            val after = awaitSdk<Int?> { ok, fail -> light.getAmbientLightBrightnessColor(colorArea, ok, fail) }.getOrNull()
            val applied = after != null && (after and 0xFFFFFF) == (target and 0xFFFFFF)
            log("LIGHT L1 reread=${after?.hex()} ${if (applied) "APPLIED" else "NOT APPLIED"}")
            if (before != null) {
                awaitSdk<Pair<SeatArea, Int>> { ok, fail ->
                    light.setAmbientLightBrightnessColor(colorArea, before or 0xFF000000.toInt(), onComplete = { a, v -> ok(a to v) }, onFailed = fail)
                }.report("LIGHT L1 restore") { (_, v) -> "value=${v.hex()}" }
            }
        }

        val readingArea = awaitSdk<Map<SeatArea, List<LightSwitch>>> { ok, fail ->
            light.checkReadingLightSwitchCapabilities(onComplete = { _: CapabilityConfig, m: Map<SeatArea, List<LightSwitch>> -> ok(m) }, onFailed = fail)
        }.getOrNull()?.keys?.zoneContaining(SeatPosition.PASSENGER)
        if (readingArea == null) {
            log("LIGHT no passenger reading switch area")
            return
        }
        val stateBefore = awaitSdk<LightState?> { ok, fail -> light.getCurrentReadingLightState(readingArea, ok, fail) }.getOrNull()
        val switchTarget = if (stateBefore == LightState.ON) LightSwitch.OFF else LightSwitch.ON
        log("LIGHT L3 before=$stateBefore set=$switchTarget area=${readingArea.str()}")
        awaitSdk<LightSwitch?> { ok, fail ->
            light.setReadingLightSwitch(readingArea, switchTarget, onComplete = ok, onFailed = fail)
        }.report("LIGHT L3 set") { "onComplete value=$it" }
        delay(1_500)
        val stateAfter = awaitSdk<LightState?> { ok, fail -> light.getCurrentReadingLightState(readingArea, ok, fail) }.getOrNull()
        val switchAfter = awaitSdk<LightSwitch?> { ok, fail -> light.getReadingLightSwitch(readingArea, ok, fail) }.getOrNull()
        log("LIGHT L3 reread state=$stateAfter switch=$switchAfter")
        val restore = if (stateBefore == LightState.ON) LightSwitch.ON else LightSwitch.OFF
        awaitSdk<LightSwitch?> { ok, fail ->
            light.setReadingLightSwitch(readingArea, restore, onComplete = ok, onFailed = fail)
        }.report("LIGHT L3 restore") { "value=$it" }
    }

    // ---- 기록 ----

    private fun <T> Result<T>.report(label: String, ok: (T) -> String) {
        fold(onSuccess = { log("$label OK ${ok(it)}") }, onFailure = { log("$label FAILED ${it.describeError()}") })
    }

    private fun Int.hex(): String = "#%08X".format(this)

    private fun log(message: String) {
        val line = "+${SystemClock.elapsedRealtime() - started}ms $message"
        Log.i(TAG, line)
        runOnUiThread { lines += line }
    }

    private companion object {
        const val TAG = "SDKSPIKE"
    }
}
