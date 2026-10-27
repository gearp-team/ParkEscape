package com.team.pleosapp.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.team.pleosapp.R
import com.team.pleosapp.ui.components.FirstDrawLogger
import com.team.pleosapp.ui.components.PrimaryButton
import com.team.pleosapp.ui.components.SecondaryButton
import com.team.pleosapp.ui.components.StatusDot
import com.team.pleosapp.ui.components.formatClock
import com.team.pleosapp.ui.theme.PleosColors

/**
 * S-P 일시정지(시안 Pause-Closed·Pause-Open). 퍼즐 내용·암호표·게이지는 그리지 않는다.
 * 게이트가 닫힌 동안 "이어하기"·"홈"은 앱에서도 비활성이다(차단 화면이 터치를 막아도 이중 안전장치).
 */
@Composable
fun PauseScreen(
    summary: SavedSummary?,
    gateOpen: Boolean,
    onResume: () -> Unit,
    onHome: () -> Unit,
    onFirstDraw: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FirstDrawLogger(onFirstDraw, modifier.fillMaxSize()) {
        BoxWithConstraints(Modifier.fillMaxSize().padding(64.dp), contentAlignment = Alignment.Center) {
            val narrow = maxWidth < 900.dp
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
                if (gateOpen) StatusDot(PleosColors.Active, stringResource(R.string.gate_open_short))
                Box(Modifier.size(160.dp).background(PleosColors.SurfaceHigh, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(
                        painterResource(if (gateOpen) R.drawable.ic_resume else R.drawable.ic_pause),
                        contentDescription = null,
                        tint = PleosColors.Text,
                        modifier = Modifier.size(80.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(if (gateOpen) R.string.pause_open_title else R.string.pause_closed_title),
                        style = if (narrow) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center,
                    )
                    summary?.let {
                        val args = arrayOf(it.lengthMinutes, it.puzzleNumber, it.puzzleCount, formatClock(it.remainingMs))
                        Text(
                            if (gateOpen) stringResource(R.string.pause_open_detail, *args) else stringResource(R.string.pause_closed_detail, *args),
                            style = MaterialTheme.typography.bodyLarge,
                            color = PleosColors.TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                val buttons: @Composable () -> Unit = {
                    if (gateOpen) {
                        PrimaryButton(stringResource(R.string.resume), R.drawable.ic_resume, onResume, Modifier.width(400.dp))
                    } else {
                        SecondaryButton(stringResource(R.string.resume), R.drawable.ic_resume, onResume, Modifier.width(400.dp), enabled = false, height = 112.dp, fontSize = 32)
                    }
                    SecondaryButton(stringResource(R.string.home), R.drawable.ic_home, onHome, Modifier.width(280.dp), enabled = gateOpen, height = 112.dp, fontSize = 32)
                }
                if (narrow) {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) { buttons() }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) { buttons() }
                }
                Text(
                    stringResource(if (gateOpen) R.string.pause_open_note else R.string.pause_closed_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = PleosColors.TextMuted,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
