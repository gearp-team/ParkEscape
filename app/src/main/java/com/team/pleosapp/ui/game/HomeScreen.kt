package com.team.pleosapp.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import com.team.pleosapp.R
import com.team.pleosapp.domain.puzzle.BoardLength
import com.team.pleosapp.ui.components.CarControlsDiagram
import com.team.pleosapp.ui.components.HairLine
import com.team.pleosapp.ui.components.IconSquareButton
import com.team.pleosapp.ui.components.PrimaryButton
import com.team.pleosapp.ui.components.SecondaryButton
import com.team.pleosapp.ui.components.Segmented
import com.team.pleosapp.ui.components.StatusDot
import com.team.pleosapp.ui.components.formatClock
import com.team.pleosapp.ui.components.formatRecord
import com.team.pleosapp.ui.theme.PleosColors

/** S0 홈 화면의 동작. */
data class HomeActions(
    val onSelectLength: (BoardLength) -> Unit,
    val onStart: () -> Unit,
    val onResume: () -> Unit,
    val onSettings: () -> Unit,
)

/** 창 폭 1200dp 이상이면 2단(기획 4절 공통 규칙). */
const val WIDE_MIN_WIDTH_DP = 1200

/** S0 홈(시안 Main·Home-Port). */
@Composable
fun HomeScreen(state: GameUiState, actions: HomeActions, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_MIN_WIDTH_DP.dp) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(660.dp).fillMaxHeight().padding(start = 48.dp, end = 48.dp, top = 56.dp, bottom = 48.dp)) {
                    TitleRow(titleSize = 40)
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CarControlsDiagram(imageHeight = min(700.dp, this@BoxWithConstraints.maxHeight - 420.dp))
                    }
                    Text(
                        stringResource(R.string.home_controls_caption),
                        style = MaterialTheme.typography.bodySmall,
                        color = PleosColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    GateLine(state.gate, Modifier.padding(top = 24.dp).align(Alignment.CenterHorizontally))
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(PleosColors.Divider))
                Column(Modifier.weight(1f).fillMaxHeight().padding(start = 72.dp, end = 72.dp, top = 56.dp, bottom = 48.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.home_length_title), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(R.string.home_length_subtitle), style = MaterialTheme.typography.bodySmall, color = PleosColors.TextMuted)
                        }
                        IconSquareButton(R.drawable.ic_settings, stringResource(R.string.settings), actions.onSettings)
                    }
                    LengthSegment(state, actions, Modifier.padding(top = 32.dp), height = 108, fontSize = 32)
                    Spacer(Modifier.height(32.dp))
                    HairLine()
                    Row(Modifier.height(IntrinsicSize.Min)) {
                        InfoCell(R.drawable.ic_key, puzzleCountText(state.selectedLength), puzzleMixText(state.selectedLength), Modifier.weight(1f).padding(end = 32.dp))
                        Box(Modifier.width(1.dp).fillMaxHeight().background(PleosColors.Divider))
                        InfoCell(R.drawable.ic_trophy, stringResource(R.string.home_best), bestText(state), Modifier.weight(1f).padding(start = 32.dp), valueColor = bestColor(state))
                    }
                    HairLine()
                    state.saved?.let { SavedRow(state, actions) }
                    ThemeRow()
                    Spacer(Modifier.weight(1f))
                    AvailabilityNote(state)
                    StartButton(state, actions, Modifier.fillMaxWidth())
                    Text(
                        stringResource(R.string.home_start_hint),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                        color = PleosColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 48.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) { TitleRow(titleSize = 36) }
                    IconSquareButton(R.drawable.ic_settings, stringResource(R.string.settings), actions.onSettings, size = 80.dp)
                }
                Box(Modifier.fillMaxWidth().padding(top = 16.dp), contentAlignment = Alignment.Center) {
                    CarControlsDiagram(imageHeight = 360.dp, bubble = 64.dp, labelSize = 22, gap = 70.dp, portrait = true)
                }
                GateLine(state.gate, Modifier.padding(top = 12.dp).align(Alignment.CenterHorizontally), fontSize = 24)
                HairLine(Modifier.padding(top = 28.dp))
                Text(stringResource(R.string.home_length_title), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 28.dp))
                LengthSegment(state, actions, Modifier.padding(top = 16.dp), height = 100, fontSize = 30)
                Row(Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconLabel(R.drawable.ic_key, puzzleCountText(state.selectedLength))
                    IconLabel(R.drawable.ic_trophy, stringResource(R.string.home_best) + " " + bestText(state))
                }
                state.saved?.let {
                    HairLine(Modifier.padding(top = 24.dp))
                    SavedRow(state, actions)
                }
                Spacer(Modifier.weight(1f))
                AvailabilityNote(state)
                StartButton(state, actions, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun TitleRow(titleSize: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(painterResource(R.drawable.ic_key), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(titleSize.dp))
        Text(stringResource(R.string.game_title), fontSize = titleSize.sp, lineHeight = (titleSize + 12).sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun GateLine(gate: GateState, modifier: Modifier = Modifier, fontSize: Int = 26) {
    when (gate) {
        GateState.OPEN -> StatusDot(PleosColors.Active, stringResource(R.string.gate_open), modifier, fontSize)
        GateState.CLOSED -> StatusDot(PleosColors.TextDisabled, stringResource(R.string.gate_closed), modifier, fontSize)
        GateState.UNKNOWN -> StatusDot(PleosColors.TextDisabled, stringResource(R.string.gate_unknown), modifier, fontSize)
    }
}

@Composable
private fun LengthSegment(state: GameUiState, actions: HomeActions, modifier: Modifier, height: Int, fontSize: Int) {
    val labels = BoardLength.entries.associateWith { stringResource(R.string.home_length_option, it.minutes) }
    val none = stringResource(R.string.home_best_none_short)
    // 판 길이별 최고 기록을 각 칸 아래에 둔다(기획 S0·F4 일부)
    val bests = BoardLength.entries.associateWith { state.bestTimes[it.minutes]?.let(::formatRecord) ?: none }
    Segmented(
        options = BoardLength.entries,
        selected = state.selectedLength,
        enabled = { it in state.availableLengths },
        label = { labels.getValue(it) },
        onSelect = actions.onSelectLength,
        modifier = modifier,
        height = height.dp,
        fontSize = fontSize,
        sublabel = { bests.getValue(it) },
    )
}

@Composable
private fun InfoCell(icon: Int, title: String, value: String, modifier: Modifier, valueColor: androidx.compose.ui.graphics.Color = PleosColors.TextMuted) {
    Column(modifier.padding(vertical = 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(40.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor)
    }
}

@Composable
private fun IconLabel(icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(32.dp))
        Text(text, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SavedRow(state: GameUiState, actions: HomeActions) {
    val saved = state.saved ?: return
    Row(Modifier.fillMaxWidth().padding(vertical = 28.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.home_saved_title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(
                stringResource(R.string.home_saved_detail, saved.lengthMinutes, saved.puzzleNumber, saved.puzzleCount, formatClock(saved.remainingMs)),
                style = MaterialTheme.typography.bodySmall,
                color = PleosColors.TextMuted,
            )
        }
        SecondaryButton(stringResource(R.string.resume), R.drawable.ic_resume, actions.onResume, enabled = state.canResume)
    }
    HairLine()
}

@Composable
private fun ThemeRow() {
    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.home_theme_title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
        Text(stringResource(R.string.home_theme_detail), style = MaterialTheme.typography.bodySmall, color = PleosColors.TextMuted)
    }
    HairLine()
}

@Composable
private fun AvailabilityNote(state: GameUiState) {
    val text = when {
        !state.supportLoaded -> stringResource(R.string.home_loading)
        state.availableLengths.isEmpty() -> stringResource(R.string.home_no_length)
        state.notice == Notice.BOARD_UNAVAILABLE -> stringResource(R.string.home_board_unavailable)
        state.selectedLength !in state.availableLengths -> stringResource(R.string.home_length_unavailable)
        else -> null
    } ?: return
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = PleosColors.Negative,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
    )
}

@Composable
private fun StartButton(state: GameUiState, actions: HomeActions, modifier: Modifier) {
    PrimaryButton(
        text = stringResource(R.string.home_start, state.selectedLength.minutes),
        icon = R.drawable.ic_play_arrow,
        onClick = actions.onStart,
        enabled = state.canStart,
        modifier = modifier,
    )
}

@Composable
private fun puzzleCountText(length: BoardLength) = stringResource(R.string.home_puzzle_count, length.puzzleCount)

@Composable
private fun puzzleMixText(length: BoardLength) = stringResource(R.string.home_puzzle_mix, length.direct, length.reasoning)

@Composable
private fun bestText(state: GameUiState): String =
    state.bestTimes[state.selectedLength.minutes]?.let(::formatRecord) ?: stringResource(R.string.home_best_none)

private fun bestColor(state: GameUiState) =
    if (state.bestTimes[state.selectedLength.minutes] != null) PleosColors.Positive else PleosColors.TextMuted
