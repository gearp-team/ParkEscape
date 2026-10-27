package com.team.pleosapp.spike

import android.os.Bundle
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.team.pleosapp.PleosApplication
import com.team.pleosapp.data.light.argbToHsv
import com.team.pleosapp.data.seat.SeatOccupancy
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * F2-data 확인 화면(debug 빌드 전용). AppContainer의 새 Repository 4종(공조·실내 조명·운전 입력·좌석)만 써서
 * 지원 범위(support), 현재값 읽기(read*), Flow 값을 logcat 태그 DATA로 남긴다.
 *   DATA SUPPORT <영역> <결과>
 *   DATA READ <신호> v=<값>
 *   DATA <신호> v=<값>          (C1·C2·C3·L1·L2·L3·D1·B1·F3)
 * 실행: adb shell am start -n com.team.pleosapp/.spike.DataCheckActivity
 */
class DataCheckActivity : ComponentActivity() {
    private val values = mutableStateMapOf<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PleosApplication).container
        log("START fake=${container.isUsingFakeSdk}")

        lifecycleScope.launch {
            log("SUPPORT climate ${container.climateRepository.support()}")
            log("SUPPORT light ${container.cabinLightRepository.support()}")
            log("SUPPORT drive ${container.driveInputRepository.support()}")
            log("SUPPORT seat ${container.seatRepository.support()}")
            log("READ C1 v=${container.climateRepository.readTargetTemperature()}")
            log("READ C2 v=${container.climateRepository.readFanSpeed()}")
            log("READ C3 v=${container.climateRepository.readRecirculation()}")
            log("READ L1 v=${container.cabinLightRepository.readAmbientColor()?.let(::color)}")
            log("READ L2 v=${container.cabinLightRepository.readAmbientBrightness()}")
            log("READ L3 v=${container.cabinLightRepository.readReadingLights()}")
            log("READ D1 v=${container.driveInputRepository.readSteeringAngle()}")
            log("READ B1 v=${container.driveInputRepository.readBrakePressed()}")
            log("READ F3 v=${occupancy(container.seatRepository.readOccupancy())}")
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { container.climateRepository.targetTemperature.collect { show("C1", it) } }
                launch { container.climateRepository.fanSpeed.collect { show("C2", it) } }
                launch { container.climateRepository.recirculation.collect { show("C3", it) } }
                launch { container.cabinLightRepository.ambientColor.collect { show("L1", it?.let(::color)) } }
                launch { container.cabinLightRepository.ambientBrightness.collect { show("L2", it) } }
                launch { container.cabinLightRepository.readingLights.collect { show("L3", it) } }
                launch { container.driveInputRepository.steeringAngle.collect { show("D1", it) } }
                launch { container.driveInputRepository.brakePressed.collect { show("B1", it) } }
                launch { container.seatRepository.occupancy.collect { show("F3", occupancy(it)) } }
            }
        }

        setContent {
            MaterialTheme {
                Column(
                    Modifier.fillMaxSize().background(Color(0xFF102027)).padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("DATA CHECK", color = Color.White, fontSize = 40.sp)
                    values.entries.sortedBy { it.key }.forEach { (k, v) ->
                        Text("$k: $v", color = Color.White, fontSize = 20.sp)
                    }
                }
            }
        }
    }

    private fun show(signal: String, value: Any?) {
        log("$signal v=$value")
        values[signal] = value.toString()
    }

    private fun log(message: String) {
        Log.i(TAG, "$message t=${System.currentTimeMillis()}")
    }

    private fun color(argb: Int): String {
        val hsv = argbToHsv(argb)
        return String.format(Locale.US, "0x%08X(h=%.1f,s=%.2f)", argb, hsv.hue, hsv.saturation)
    }

    private fun occupancy(o: SeatOccupancy): String =
        "raw=${o.raw} polarityKnown=${o.polarityKnown} inverted=${o.inverted} participants=${o.participants.sorted()}"

    private companion object {
        const val TAG = "DATA"
    }
}
