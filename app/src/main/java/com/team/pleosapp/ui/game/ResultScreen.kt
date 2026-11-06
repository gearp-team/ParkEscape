package com.team.pleosapp.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.pleosapp.R
import com.team.pleosapp.domain.puzzle.SignalId
import com.team.pleosapp.ui.components.HairLine
import com.team.pleosapp.ui.components.PrimaryButton
import com.team.pleosapp.ui.components.SecondaryButton
import com.team.pleosapp.ui.components.formatRecord
import com.team.pleosapp.ui.theme.PleosColors

data class ResultActions(val onNewBoard: () -> Unit, val onHome: () -> Unit)

/**
 * S3 결과(기획 4절). 가로 2단: 좌 결과·버튼, 우 복구 체크리스트. 세로 1단: 결과 → 체크리스트 → 버튼.
 * 체크리스트는 정차 중에만 리스너로 갱신된다(게이트가 닫히면 ViewModel이 해제하고, 돌아오면 다시 읽는다).
 */
@Composable
fun ResultScreen(result: ResultState?, gateOpen: Boolean, bestMs: Long?, actions: ResultActions, modifier: Modifier = Modifier) {
    if (result == null) return
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_MIN_WIDTH_DP.dp) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(700.dp).fillMaxHeight().padding(start = 64.dp, end = 64.dp, top = 56.dp, bottom = 48.dp)) {
                    StatusBadge(result.escaped, 112.dp)
                    Text(
                        stringResource(if (result.escaped) R.string.result_escaped else R.string.result_timed_out),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = 40.dp),
                    )
                    Text(detail(result), fontSize = 28.sp, lineHeight = 36.sp, color = PleosColors.TextSecondary, modifier = Modifier.padding(top = 8.dp))
                    Spacer(Modifier.weight(1f))
                    Text(stringResource(R.string.result_time_label, result.lengthMinutes), fontSize = 26.sp, lineHeight = 34.sp, color = PleosColors.TextMuted)
                    Text(
                        formatRecord(result.elapsedMs),
                        fontSize = 120.sp,
                        lineHeight = 132.sp,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                    )
                    RecordChip(result, bestMs, Modifier.padding(top = 20.dp))
                    Buttons(gateOpen, actions, Modifier.padding(top = 48.dp))
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(PleosColors.Divider))
                Column(Modifier.weight(1f).fillMaxHeight().padding(start = 64.dp, end = 64.dp, top = 56.dp, bottom = 48.dp)) {
                    Text(stringResource(R.string.result_checklist_title), style = MaterialTheme.typography.titleLarge)
                    ChecklistSubtitle(result, gateOpen, fontSize = 26, Modifier.padding(top = 8.dp))
                    Checklist(result, Modifier.padding(top = 32.dp).weight(1f), wide = true)
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 48.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    StatusBadge(result.escaped, 96.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            stringResource(if (result.escaped) R.string.result_escaped else R.string.result_timed_out),
                            fontSize = 48.sp,
                            lineHeight = 64.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                        Text(stringResource(R.string.result_time_label, result.lengthMinutes), fontSize = 24.sp, lineHeight = 32.sp, color = PleosColors.TextMuted)
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            formatRecord(result.elapsedMs),
                            fontSize = 72.sp,
                            lineHeight = 80.sp,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                        )
                        if (result.newBest) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(painterResource(R.drawable.ic_trophy), contentDescription = null, tint = PleosColors.Positive, modifier = Modifier.size(26.dp))
                                Text(stringResource(R.string.result_new_best), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = PleosColors.Positive)
                            }
                        }
                    }
                }
                if (!result.newBest) RecordChip(result, bestMs, Modifier.padding(top = 20.dp))
                HairLine(Modifier.padding(top = 28.dp))
                Text(
                    stringResource(R.string.result_checklist_title),
                    fontSize = 32.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 28.dp),
                )
                ChecklistSubtitle(result, gateOpen, fontSize = 24, Modifier.padding(top = 6.dp))
                Checklist(result, Modifier.padding(top = 20.dp).weight(1f), wide = false)
                Buttons(gateOpen, actions, Modifier.padding(top = 28.dp))
            }
        }
    }
}

@Composable
private fun detail(result: ResultState): String =
    if (result.escaped) stringResource(R.string.result_escaped_detail)
    else stringResource(R.string.result_timed_out_detail, result.solvedCount, result.puzzleCount)

@Composable
private fun StatusBadge(escaped: Boolean, size: Dp) {
    Box(
        Modifier.size(size).background(if (escaped) PleosColors.Active else PleosColors.TextMuted, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(if (escaped) R.drawable.ic_lock_open else R.drawable.ic_timer),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(size * 0.55f),
        )
    }
}

/** 최고 기록 칩: 새 기록이면 "새 최고 기록 · 이전 m:ss", 아니면 이 길이의 최고 기록, 건너뛴 판이면 기록 제외 안내. */
@Composable
private fun RecordChip(result: ResultState, bestMs: Long?, modifier: Modifier) {
    val (text, color, sub) = when {
        result.skipped -> Triple(stringResource(R.string.result_skipped), PleosColors.TextSecondary, null)
        result.newBest -> Triple(
            stringResource(R.string.result_new_best),
            PleosColors.Positive,
            result.previousBestMs?.let { stringResource(R.string.result_previous_best, formatRecord(it)) },
        )
        bestMs != null -> Triple(stringResource(R.string.result_best, result.lengthMinutes, formatRecord(bestMs)), PleosColors.TextSecondary, null)
        else -> return
    }
    Row(
        modifier.border(1.dp, PleosColors.Border, RoundedCornerShape(8.dp)).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(painterResource(R.drawable.ic_trophy), contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        Text(text, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold, color = color)
        sub?.let { Text(it, fontSize = 26.sp, lineHeight = 34.sp, color = PleosColors.TextMuted) }
    }
}

@Composable
private fun Buttons(gateOpen: Boolean, actions: ResultActions, modifier: Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PrimaryButton(stringResource(R.string.result_new_board), R.drawable.ic_play_arrow, actions.onNewBoard, Modifier.weight(1f), enabled = gateOpen)
        SecondaryButton(stringResource(R.string.result_home), R.drawable.ic_home, actions.onHome, Modifier.weight(1f), height = 112.dp, fontSize = 32)
    }
}

@Composable
private fun ChecklistSubtitle(result: ResultState, gateOpen: Boolean, fontSize: Int, modifier: Modifier) {
    val text = when {
        result.checklist.isEmpty() -> stringResource(R.string.result_checklist_empty)
        !gateOpen -> stringResource(R.string.result_checklist_paused)
        else -> stringResource(R.string.result_checklist_detail, result.restoredCount, result.checklist.size)
    }
    Text(text, fontSize = fontSize.sp, lineHeight = (fontSize + 8).sp, color = PleosColors.TextMuted, modifier = modifier)
}

@Composable
private fun Checklist(result: ResultState, modifier: Modifier, wide: Boolean) {
    val rows = result.rows
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val rowHeight = if (rows.isEmpty()) 0.dp else (maxHeight / rows.size).coerceIn(88.dp, if (wide) 150.dp else 130.dp)
        Column(Modifier.fillMaxWidth()) {
            if (rows.isNotEmpty()) HairLine()
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth().height(rowHeight),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(if (wide) 24.dp else 20.dp),
                ) {
                    Icon(painterResource(iconOf(row.signal)), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(if (wide) 40.dp else 36.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(row.title, fontSize = if (wide) 30.sp else 28.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
                        Text(row.detail, fontSize = if (wide) 26.sp else 24.sp, lineHeight = 34.sp, color = PleosColors.TextMuted)
                    }
                    if (row.restored) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = PleosColors.Positive, modifier = Modifier.size(32.dp))
                            Text(stringResource(R.string.result_restored), fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = PleosColors.Positive)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.size(12.dp).background(PleosColors.Negative, CircleShape))
                            Text(stringResource(R.string.result_restore_needed), fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                HairLine()
            }
        }
    }
}

@DrawableRes
fun iconOf(signal: SignalId): Int = when (signal) {
    SignalId.C1 -> R.drawable.ic_device_thermostat
    SignalId.C2 -> R.drawable.ic_mode_fan
    SignalId.C3 -> R.drawable.ic_car_fan_recirculate
    SignalId.L1, SignalId.L2 -> R.drawable.ic_light
    SignalId.L3 -> R.drawable.ic_lightbulb
    SignalId.C4 -> R.drawable.ic_defrost
    SignalId.L4 -> R.drawable.ic_headlight
    SignalId.D1 -> R.drawable.ic_steering
    SignalId.B1 -> R.drawable.ic_brake_pedal
}
