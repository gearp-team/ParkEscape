package com.team.pleosapp.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.pleosapp.BuildConfig
import com.team.pleosapp.R
import com.team.pleosapp.ui.components.HairLine
import com.team.pleosapp.ui.components.PrimaryButton
import com.team.pleosapp.ui.components.SecondaryButton
import com.team.pleosapp.ui.theme.PleosColors

/** 화면 전환 뿌리. [onPauseDrawn]은 S-P가 처음 그려질 때 불린다(측정 로그). */
@Composable
fun GameRoute(viewModel: GameViewModel, onPauseDrawn: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GameApp(state, viewModel, onPauseDrawn)
}

@Composable
private fun GameApp(state: GameUiState, vm: GameViewModel, onPauseDrawn: () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = PleosColors.Surface) {
        if (state.showsDrivingNotice(BuildConfig.DRIVING_NOTICE)) {
            // DO 선언 빌드: 시스템이 앱을 덮지 않으므로 앱이 주행 중 화면을 직접 대신한다(기획 4절 S-H).
            // 아래 화면(판 진행 중이었다면 이미 PAUSED_BY_GATE로 저장됨)은 그리지 않는다.
            DrivingNoticeScreen()
            return@Surface
        }
        Box(Modifier.fillMaxSize()) {
            when (state.screen) {
                Screen.HOME -> HomeScreen(
                    state,
                    remember(vm) { HomeActions(vm::selectLength, vm::startNew, vm::resumeSaved, vm::openSettings) },
                )
                Screen.READY -> state.ready?.let {
                    ReadyScreen(
                        it,
                        state.gateOpen,
                        state.gate,
                        remember(vm) { ReadyActions(vm::toggleSeat, vm::setDontShowIdleAgain, vm::confirmParked, vm::goHome) },
                    )
                }
                Screen.PLAYING -> PlayScreen(state.play, remember(vm) { PlayActions(vm::showHint, vm::skipPuzzle, vm::pressSeal) })
                Screen.PAUSED -> PauseScreen(state.saved, state.gateOpen, vm::resumeSaved, vm::goHome, onPauseDrawn)
                Screen.RESULT -> ResultScreen(
                    state.result,
                    state.gateOpen,
                    state.result?.let { state.bestTimes[it.lengthMinutes] },
                    remember(vm) { ResultActions(vm::newBoard, vm::goHome) },
                )
            }
            if (state.settingsOpen) SettingsPanel(state, vm)
            if (state.confirmOverwrite) {
                val saved = state.saved
                ConfirmPanel(
                    title = stringResource(R.string.overwrite_title),
                    detail = saved?.let { stringResource(R.string.overwrite_detail, it.lengthMinutes, it.puzzleNumber, it.puzzleCount) },
                    rows = emptyList(),
                    confirm = stringResource(R.string.overwrite_confirm),
                    onConfirm = vm::confirmOverwrite,
                    onCancel = vm::cancelOverwrite,
                )
            }
            state.demoWarning?.let { diffs ->
                ConfirmPanel(
                    title = stringResource(R.string.demo_warning_title),
                    detail = stringResource(R.string.demo_warning_detail),
                    rows = diffs.map { it.item to stringResource(R.string.demo_warning_row, it.current, it.expected) },
                    confirm = stringResource(R.string.demo_warning_confirm),
                    onConfirm = vm::confirmDemoStart,
                    onCancel = vm::cancelDemoStart,
                )
            }
        }
    }
}

/** S-H: 아이콘 하나와 한 줄. 터치 요소가 없다(빈 곳을 눌러도 아무것도 하지 않는다). */
@Composable
private fun DrivingNoticeScreen() {
    Column(
        Modifier.fillMaxSize().padding(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(160.dp).background(PleosColors.SurfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_pause), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(80.dp))
        }
        Text(stringResource(R.string.driving_notice), style = MaterialTheme.typography.headlineMedium)
    }
}

/**
 * 확인 대화상자(새 판 덮어쓰기, 시연 0단계 경고). 설정 패널처럼 앱 안에 그리고(별도 창 없음), 게이트가 닫히면 ViewModel이 닫는다.
 * 빈 곳을 누르면 취소다(동의로 보지 않는다).
 */
@Composable
private fun ConfirmPanel(
    title: String,
    detail: String?,
    rows: List<Pair<String, String>>,
    confirm: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66131417))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 800.dp)
                .fillMaxWidth()
                .padding(40.dp)
                .background(PleosColors.Surface, RoundedCornerShape(24.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = PleosColors.TextSecondary) }
            if (rows.isNotEmpty()) {
                Column {
                    HairLine()
                    rows.forEach { (item, value) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(item, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.widthIn(min = 200.dp))
                            Text(value, style = MaterialTheme.typography.bodySmall, color = PleosColors.TextSecondary, modifier = Modifier.weight(1f))
                        }
                        HairLine()
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SecondaryButton(stringResource(R.string.cancel), null, onCancel, Modifier.weight(1f), height = 100.dp, fontSize = 30)
                PrimaryButton(confirm, null, onConfirm, Modifier.weight(1f), height = 100.dp)
            }
        }
    }
}

/** 설정(기획 F1): 전기차 토글, 공회전 안내 다시 보기. 앱 안 패널로 둔다(별도 창 없음). */
@Composable
private fun SettingsPanel(state: GameUiState, vm: GameViewModel) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x66131417))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = vm::closeSettings),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
                .padding(40.dp)
                .background(PleosColors.Surface, RoundedCornerShape(24.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Text(stringResource(R.string.settings), style = MaterialTheme.typography.titleLarge)
            HairLine()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.settings_ev), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text(stringResource(R.string.settings_ev_detail), style = MaterialTheme.typography.bodySmall, color = PleosColors.TextMuted)
                }
                Switch(
                    checked = state.settings.isEv,
                    onCheckedChange = vm::setEv,
                    colors = SwitchDefaults.colors(checkedTrackColor = PleosColors.Selected),
                )
            }
            HairLine()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.settings_idle_reset), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
                    Text(stringResource(R.string.settings_idle_reset_detail), style = MaterialTheme.typography.bodySmall, color = PleosColors.TextMuted)
                }
                SecondaryButton(stringResource(R.string.settings_idle_reset), null, vm::resetIdleNotice, enabled = state.settings.idleNoticeDismissed)
            }
            HairLine()
            PrimaryButton(stringResource(R.string.settings_close), null, vm::closeSettings, Modifier.fillMaxWidth(), height = 100.dp)
        }
    }
}
