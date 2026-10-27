package com.team.pleosapp.spike

import ai.pleos.playground.vehicle.Vehicle
import android.car.Car
import android.car.drivingstate.CarUxRestrictions
import android.car.drivingstate.CarUxRestrictionsManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.team.pleosapp.data.driving.CarDrivingStateRepository
import kotlinx.coroutines.launch

/** 스파이크 로그. logcat 태그 SPIKE, 끝에 기기 시각(epoch ms)을 붙인다. */
object SpikeLog {
    private const val TAG = "SPIKE"
    fun i(msg: String) = Log.i(TAG, "$msg t=${System.currentTimeMillis()}")
    fun w(msg: String) = Log.w(TAG, "$msg t=${System.currentTimeMillis()}")
}

/**
 * SPIKE-W1 측정 화면(debug 빌드 전용, distractionOptimized 미선언).
 *
 * - 신호: [SignalProbe]가 퍼즐 신호 8종 + 좌석 점유를 capability → get → register로 훑는다.
 * - 규제 A: onCreate~onDestroy 동안 CarUxRestrictionsManager 리스너를 계속 붙여 둔다(앱이 가려져 있어도 받는지 확인).
 * - 규제 B: 본 코드의 CarDrivingStateRepository Flow를 repeatOnLifecycle(STARTED)로 구독한다(복귀 때 무엇이 오는지 확인).
 * - 그리기: 규제가 바뀐 뒤 화면이 PLAY/PAUSED로 처음 그려지는 시각을 DRAW로 남긴다(차단 화면 로그와 비교).
 * - 터치: 앱 창에 들어온 모든 터치(TOUCH)와 버튼 클릭(CLICK), 버튼의 화면 좌표(BTN)를 남긴다.
 * - 창: 앱 창 크기(WIN, dp)와 생명주기·포커스 변화(LIFE)를 남긴다.
 */
class SpikeActivity : ComponentActivity() {
    private var car: Car? = null
    private var uxManager: CarUxRestrictionsManager? = null
    private var probe: SignalProbe? = null

    private val gateOpen = mutableStateOf<Boolean?>(null)
    private val restrictionText = mutableStateOf("unknown")
    private val clicks = mutableIntStateOf(0)
    private val values = mutableStateMapOf<String, String>()

    /** 차단 화면이 실시간 흐림인지 정지 이미지인지 보려고, 주행 중에 배경색을 바꾼다(am start --es bg red|blue|none). */
    private val backgroundOverride = mutableStateOf<String?>(null)

    /** onStop 이후 게이트가 한 번이라도 닫혔는지(규제 A로 추적). */
    private var closedWhileAway = false

    @Volatile private var composedMode: String = ""
    private var drawnMode: String = ""
    private var lastButton: String = ""
    private val drawListener = ViewTreeObserver.OnDrawListener {
        val mode = composedMode
        if (mode != drawnMode) {
            drawnMode = mode
            SpikeLog.i("DRAW mode=$mode")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SpikeLog.i("LIFE onCreate")

        // 규제 A: 액티비티 수명 내내 등록
        car = Car.createCar(applicationContext)
        uxManager = car?.getCarManager(Car.CAR_UX_RESTRICTION_SERVICE) as? CarUxRestrictionsManager
        val manager = uxManager
        if (manager == null) {
            SpikeLog.w("REG src=A manager=null")
        } else {
            manager.currentCarUxRestrictions?.let { onRestrictionsA("A-init", it) }
            manager.registerListener { onRestrictionsA("A", it) }
        }

        // 규제 B: 본 코드의 Repository Flow를 STARTED 동안만 구독
        val repository = CarDrivingStateRepository(applicationContext)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                SpikeLog.i("REG src=B subscribe")
                repository.restrictions.collect {
                    SpikeLog.i(
                        "REG src=B requiresDO=${it.requiresDistractionOptimization} " +
                            "active=0x${Integer.toHexString(it.activeRestrictions)} life=${lifecycle.currentState}",
                    )
                }
            }
        }

        // 차량 신호
        val vehicle = Vehicle(applicationContext).also { it.initialize() }
        probe = SignalProbe(vehicle) { key, value -> runOnUiThread { values[key] = value } }.also { it.start() }

        window.decorView.viewTreeObserver.addOnDrawListener(drawListener)
        setContent { MaterialTheme { SpikeScreen() } }
    }

    private fun onRestrictionsA(src: String, r: CarUxRestrictions) {
        val open = !r.isRequiresDistractionOptimization && r.activeRestrictions == 0
        SpikeLog.i(
            "REG src=$src requiresDO=${r.isRequiresDistractionOptimization} " +
                "active=0x${Integer.toHexString(r.activeRestrictions)} open=$open life=${lifecycle.currentState} eventAgeMs=${eventAgeMs(r)}",
        )
        if (!open && !lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) closedWhileAway = true
        runOnUiThread {
            gateOpen.value = open
            restrictionText.value = "requiresDO=${r.isRequiresDistractionOptimization} active=0x${Integer.toHexString(r.activeRestrictions)}"
        }
    }

    /**
     * 이벤트가 만들어진 뒤 받기까지 걸린 시간. getTimeStamp()는 공개 스텁에 없어 toString()의 "time: <elapsedRealtimeNanos>"를 읽는다
     * (스파이크 확인용. 본 코드에서 쓰지 말 것).
     */
    private fun eventAgeMs(r: CarUxRestrictions): Long? =
        Regex("time: (\\d+)").find(r.toString())?.groupValues?.get(1)?.toLongOrNull()
            ?.let { (SystemClock.elapsedRealtimeNanos() - it) / 1_000_000 }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getStringExtra("probe") == "ambient-switch") probe?.probeAmbientSwitch()
        val bg = intent.getStringExtra("bg") ?: return
        backgroundOverride.value = bg.takeUnless { it == "none" }
        SpikeLog.i("BG $bg")
    }

    override fun onStart() {
        super.onStart()
        SpikeLog.i("LIFE onStart closedWhileAway=$closedWhileAway gateOpen=${gateOpen.value}")
    }

    override fun onResume() {
        super.onResume()
        SpikeLog.i("LIFE onResume")
    }

    override fun onPause() {
        SpikeLog.i("LIFE onPause")
        super.onPause()
    }

    override fun onStop() {
        SpikeLog.i("LIFE onStop")
        closedWhileAway = false
        super.onStop()
    }

    override fun onDestroy() {
        SpikeLog.i("LIFE onDestroy")
        window.decorView.viewTreeObserver.removeOnDrawListener(drawListener)
        probe?.stop()
        uxManager?.unregisterListener()
        car?.disconnect()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        SpikeLog.i("LIFE focus=$hasFocus")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        SpikeLog.i("LIFE config w=${newConfig.screenWidthDp}dp h=${newConfig.screenHeightDp}dp orientation=${newConfig.orientation}")
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration) {
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
        SpikeLog.i("LIFE multiWindow=$isInMultiWindowMode")
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN || ev.actionMasked == MotionEvent.ACTION_UP) {
            SpikeLog.i("TOUCH action=${MotionEvent.actionToString(ev.actionMasked)} raw=${ev.rawX.toInt()},${ev.rawY.toInt()}")
        }
        return super.dispatchTouchEvent(ev)
    }

    @Composable
    private fun SpikeScreen() {
        val open = gateOpen.value
        val mode = when (open) {
            true -> "PLAY"
            false -> "PAUSED"
            null -> "UNKNOWN"
        }
        val drawn = mode + (backgroundOverride.value?.let { "+bg=$it" } ?: "")
        SideEffect { composedMode = drawn }
        val background = when {
            backgroundOverride.value == "red" -> Color(0xFFD50000)
            backgroundOverride.value == "blue" -> Color(0xFF0D47A1)
            open == true -> Color(0xFF1B5E20)
            open == false -> Color(0xFF424242)
            else -> Color(0xFF263238)
        }
        BoxWithConstraints(Modifier.fillMaxSize().background(background).padding(24.dp)) {
            val w = maxWidth.value.toInt()
            val h = maxHeight.value.toInt()
            LaunchedEffect(w, h) {
                val c = resources.configuration
                SpikeLog.i("WIN content=${w}x${h}dp config=${c.screenWidthDp}x${c.screenHeightDp}dp density=${resources.displayMetrics.density}")
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(mode, color = Color.White, fontSize = 64.sp)
                Text("content ${w}x${h} dp · ${restrictionText.value}", color = Color.White, fontSize = 22.sp)
                Button(
                    onClick = {
                        clicks.intValue++
                        SpikeLog.i("CLICK n=${clicks.intValue} mode=$mode")
                    },
                    modifier = Modifier
                        .size(width = 420.dp, height = 160.dp)
                        .onGloballyPositioned { coords ->
                            val p = coords.positionOnScreen()
                            val cx = (p.x + coords.size.width / 2f).toInt()
                            val cy = (p.y + coords.size.height / 2f).toInt()
                            val text = "center=$cx,$cy size=${coords.size.width}x${coords.size.height}"
                            if (text != lastButton) {
                                lastButton = text
                                SpikeLog.i("BTN $text")
                            }
                        },
                ) {
                    Text("TAP TEST (${clicks.intValue})", fontSize = 32.sp)
                }
                values.entries.sortedBy { it.key }.forEach { (k, v) ->
                    Text("$k: $v", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}
