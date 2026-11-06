package com.team.pleosapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.team.pleosapp.data.driving.DrivingRestrictions
import com.team.pleosapp.data.driving.FakeDrivingStateRepository
import com.team.pleosapp.ui.game.GameRoute
import com.team.pleosapp.ui.game.GameViewModel
import com.team.pleosapp.ui.theme.PleosAppTheme
import kotlinx.coroutines.launch

/**
 * 하나뿐인 화면 액티비티. distractionOptimized를 선언하지 않으므로 주행(규제 0x0 아님)에서는 시스템이 앱을 차단 화면으로 덮는다.
 * 그때 onPause/onStop이 오지 않으므로 규제 Flow를 직접 구독해 GameViewModel.onRestrictions로 넘긴다(기획 F1, 2-2).
 */
class MainActivity : ComponentActivity() {
    private val container by lazy { (application as PleosApplication).container }
    private val viewModel: GameViewModel by viewModels { GameViewModel.factory(container) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDebugIntent(intent)

        // 규제 Flow는 화면이 STARTED인 동안 구독한다(돌아오면 현재값을 바로 다시 받는다, 실측)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                container.drivingStateRepository.restrictions.collect { viewModel.onRestrictions(it) }
            }
        }

        setContent {
            PleosAppTheme {
                GameRoute(viewModel, onPauseDrawn = { Log.i(GameViewModel.TAG, "draw S-P t=${System.currentTimeMillis()}") })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDebugIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        viewModel.onAppStarted()
    }

    override fun onStop() {
        // 창 크기 변경 등으로 다시 만들어지는 경우는 ViewModel이 판을 그대로 이어 간다(일시정지하지 않는다)
        if (!isChangingConfigurations) viewModel.onAppStopped()
        super.onStop()
    }

    /**
     * 디버그 빌드 전용(기획 5절 시연).
     * - `--es seed demo5`: 시드 고정(시연 모드, 신호 풀 10종 고정). `demo-safety`는 전조등·핸들·앰비언트 색·앞유리 제상·브레이크 5분 판
     * - `--es fakeGate park|idle|drive`: Fake SDK 빌드에서 주행 상태를 바꾼다(Virtual Console 대신)
     * - `--ez autoPlay false`: Fake SDK 빌드의 자동 풀이(퍼즐마다 6초 뒤)를 끈다
     */
    private fun handleDebugIntent(intent: Intent?) {
        if (!BuildConfig.DEBUG || intent == null) return
        intent.getStringExtra("seed")?.let { viewModel.setDebugSeed(it.ifBlank { null }) }
        if (intent.hasExtra("autoPlay")) viewModel.setAutoPlay(intent.getBooleanExtra("autoPlay", true))
        val fake = container.drivingStateRepository as? FakeDrivingStateRepository ?: return
        when (intent.getStringExtra("fakeGate")) {
            "park" -> fake.setRestrictions(DrivingRestrictions())
            "idle", "drive" -> fake.setRestrictions(DrivingRestrictions(requiresDistractionOptimization = true, activeRestrictions = 0x50))
        }
    }
}
