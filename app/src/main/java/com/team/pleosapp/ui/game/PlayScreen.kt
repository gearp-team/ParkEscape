package com.team.pleosapp.ui.game

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.pleosapp.BuildConfig
import com.team.pleosapp.R
import com.team.pleosapp.domain.clue.ClueWriter
import com.team.pleosapp.domain.puzzle.CipherEntry
import com.team.pleosapp.domain.puzzle.ClueType
import com.team.pleosapp.domain.puzzle.SignalId
import com.team.pleosapp.ui.components.HairLine
import com.team.pleosapp.ui.components.SecondaryButton
import com.team.pleosapp.ui.components.formatClock
import com.team.pleosapp.ui.theme.PleosColors

data class PlayActions(
    val onHint: () -> Unit,
    val onSkip: () -> Unit,
    val onSeal: () -> Unit,
)

/**
 * S2 진행(기획 4절). 가로 2단: 좌 진행·타이머·단서·암호표, 우 게이지·힌트. 세로 1단: 단서(+암호표) → 게이지 → 하단 바(진행·타이머·힌트).
 * 주행으로 바뀌면 ViewModel이 이 화면을 S-P로 바꾼다(퍼즐 내용은 S-P에 남기지 않는다).
 */
@Composable
fun PlayScreen(play: PlayState?, actions: PlayActions, modifier: Modifier = Modifier) {
    if (play == null) return
    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_MIN_WIDTH_DP.dp) WidePlay(play, actions) else NarrowPlay(play, actions)
    }
}

@Composable
private fun WidePlay(play: PlayState, actions: PlayActions) {
    val clue = play.clue
    val cipher = play.cipherTable
    Row(Modifier.fillMaxSize()) {
        Column(
            Modifier.width(1000.dp).fillMaxHeight().padding(start = 64.dp, end = 64.dp, top = 56.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(stringResource(R.string.play_progress, play.number, play.count), fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
                    ProgressSegments(play, height = 10)
                }
                Timer(play.remainingMs, fontSize = 60, iconSize = 44)
            }
            ClueCard(play, Modifier.fillMaxWidth().then(if (cipher.isEmpty()) Modifier.weight(1f) else Modifier), wide = true)
            if (cipher.isNotEmpty()) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    CipherTitle()
                    CipherTable(cipher, Modifier.weight(1f), glyphSize = 56, labelSize = 28)
                }
            }
        }
        Box(Modifier.width(1.dp).fillMaxHeight().background(PleosColors.Divider))
        Column(Modifier.weight(1f).fillMaxHeight().padding(start = 56.dp, end = 56.dp, top = 56.dp, bottom = 48.dp)) {
            SignalGauge(play, actions, wide = true)
            clue.settingsNote?.let { InfoNote(it, Modifier.padding(top = 24.dp)) }
            EmulatorNote(play, Modifier.padding(top = 16.dp))
            Spacer(Modifier.weight(1f))
            HelpCards(play, actions, Modifier.fillMaxWidth())
            HintButton(play, actions, Modifier.fillMaxWidth().padding(top = 20.dp), height = 100)
        }
    }
}

@Composable
private fun NarrowPlay(play: PlayState, actions: PlayActions) {
    val cipher = play.cipherTable
    Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 40.dp)) {
        ClueCard(play, Modifier.fillMaxWidth(), wide = false)
        if (cipher.isNotEmpty()) {
            Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CipherTitle()
                CipherTable(cipher, Modifier.height(if (cipher.size > 6) 220.dp else 132.dp), glyphSize = 44, labelSize = 24)
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(top = 32.dp, bottom = 24.dp)) {
            SignalGauge(play, actions, wide = false)
            play.clue.settingsNote?.let { InfoNote(it, Modifier.padding(top = 16.dp)) }
            EmulatorNote(play, Modifier.padding(top = 12.dp))
            Spacer(Modifier.weight(1f))
            HelpCards(play, actions, Modifier.fillMaxWidth())
        }
        HairLine()
        Row(Modifier.fillMaxWidth().padding(top = 24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            Column(Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.play_progress, play.number, play.count), fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold)
                ProgressSegments(play, height = 8)
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Timer(play.remainingMs, fontSize = 56, iconSize = 40) }
            HintButton(play, actions, Modifier, height = 88, short = true)
        }
    }
}

// ---- 진행·타이머 --------------------------------------------------------------------------------

@Composable
private fun ProgressSegments(play: PlayState, height: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (height >= 10) 10.dp else 6.dp)) {
        repeat(play.count) { i ->
            val color = when {
                i in play.skipped -> PleosColors.Border
                i < play.puzzle.index -> PleosColors.Text
                i == play.puzzle.index -> PleosColors.TextDisabled
                else -> PleosColors.Divider
            }
            Box(Modifier.weight(1f).height(height.dp).background(color, RoundedCornerShape((height / 2).dp)))
        }
    }
}

@Composable
private fun Timer(remainingMs: Long, fontSize: Int, iconSize: Int) {
    val urgent = remainingMs <= 30_000
    val label = stringResource(R.string.play_time_left)
    Row(
        Modifier.semantics(mergeDescendants = true) { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(painterResource(R.drawable.ic_timer), contentDescription = null, tint = if (urgent) PleosColors.Negative else PleosColors.Text, modifier = Modifier.size(iconSize.dp))
        Text(
            formatClock(remainingMs),
            fontSize = fontSize.sp,
            lineHeight = (fontSize + 12).sp,
            fontWeight = FontWeight.Bold,
            color = if (urgent) PleosColors.Negative else PleosColors.Text,
            style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
        )
    }
}

// ---- 단서 ---------------------------------------------------------------------------------------

/** 줄바꿈에서 떼지 않을 낱말 묶음(어절 단위 줄바꿈이 "조타 구간 / 번호"처럼 끊지 않게 띄어쓰기를 붙는 공백으로 바꾼다). */
private val TERMS = listOf(
    "조타 구간 번호", "냉각 코일 온도", "환기 팬 단수", "퍼즐 답", "조명 출력", "냉각 코일", "환기 팬", "비상 조명", "조타 키", "격벽 상태",
    "해치 봉인", "계통 색", "2열 오른쪽", "2열 왼쪽", "왼쪽 1번", "왼쪽 2번", "왼쪽 3번", "오른쪽 1번", "오른쪽 2번", "오른쪽 3번",
)

private fun keepTerms(text: String): String = TERMS.fold(text) { acc, term -> acc.replace(term, term.replace(' ', '\u00A0')) }

@Composable
private fun ClueCard(play: PlayState, modifier: Modifier, wide: Boolean) {
    val clue = play.clue
    val seal = play.puzzle.type == ClueType.SEAL
    Column(
        modifier
            .background(PleosColors.SurfaceLow, RoundedCornerShape(24.dp))
            .padding(horizontal = if (wide) 56.dp else 40.dp, vertical = if (wide) 48.dp else 40.dp),
        verticalArrangement = if (wide && play.cipherTable.isEmpty()) Arrangement.SpaceBetween else Arrangement.spacedBy(28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                Modifier
                    .background(PleosColors.Surface, RoundedCornerShape(8.dp))
                    .border(1.dp, PleosColors.Border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (seal) Icon(painterResource(R.drawable.ic_lock), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(28.dp))
                Text(clue.typeLabel, fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(clue.theme, fontSize = if (wide) 26.sp else 24.sp, lineHeight = 34.sp, color = PleosColors.TextMuted)
        }
        val textSize = when {
            !wide -> 48
            clue.symbol != null -> 56
            else -> 60
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(if (wide) 40.dp else 28.dp)) {
            clue.symbol?.let { symbol ->
                Box(
                    Modifier.size(if (wide) 140.dp else 112.dp).background(PleosColors.Selected, RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(symbol.glyph, fontSize = if (wide) 80.sp else 64.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            Text(keepTerms(clue.text), fontSize = textSize.sp, lineHeight = (textSize + 20).sp, fontWeight = FontWeight.ExtraBold, maxLines = 3)
        }
        if (wide) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(painterResource(R.drawable.ic_check), contentDescription = null, tint = PleosColors.TextMuted, modifier = Modifier.size(32.dp))
                val line = play.lastSolved?.let { stringResource(R.string.play_solved_line, it, play.restoredCount, play.count) }
                    ?: stringResource(R.string.play_solved_none, play.restoredCount, play.count)
                Text(line, fontSize = 26.sp, lineHeight = 34.sp, color = PleosColors.TextMuted)
            }
        }
    }
}

@Composable
private fun CipherTitle() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(stringResource(R.string.play_cipher_table), fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        Text(stringResource(R.string.play_cipher_note), fontSize = 24.sp, lineHeight = 32.sp, color = PleosColors.TextMuted)
    }
}

/** 암호표 타일(판 전체 표. 진짜 1 + 미끼 2씩, 기호는 겹치지 않는다). 4개까지 한 줄, 그보다 많으면 두 줄. */
@Composable
private fun CipherTable(entries: List<CipherEntry>, modifier: Modifier, glyphSize: Int, labelSize: Int) {
    val columns = if (entries.size <= 4) entries.size else (entries.size + 1) / 2
    val rows = entries.chunked(columns)
    Column(modifier.fillMaxWidth()) {
        HairLine()
        rows.forEachIndexed { r, row ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (c in 0 until columns) {
                    val entry = row.getOrNull(c)
                    Column(
                        Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                    ) {
                        if (entry != null) {
                            Text(entry.symbol.glyph, fontSize = glyphSize.sp, lineHeight = (glyphSize + 4).sp, fontWeight = FontWeight.Bold)
                            Text(ClueWriter.meaning(entry.meaning), fontSize = labelSize.sp, lineHeight = (labelSize + 8).sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                        }
                    }
                    if (c < columns - 1) Box(Modifier.width(1.dp).fillMaxHeight().background(PleosColors.Divider))
                }
            }
            if (r < rows.size - 1) HairLine()
        }
        HairLine()
    }
}

// ---- 힌트·건너뛰기 ------------------------------------------------------------------------------

@Composable
private fun HelpCards(play: PlayState, actions: PlayActions, modifier: Modifier) {
    val clue = play.clue
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        play.chainReveal?.let { NoteCard(R.drawable.ic_info, null, it) }
        clue.hints.take(play.hintsShown).forEachIndexed { i, hint ->
            NoteCard(R.drawable.ic_lightbulb, stringResource(R.string.play_hint_label, i + 1), hint)
        }
        if (play.canSkip) {
            Column(
                Modifier.fillMaxWidth().border(1.dp, PleosColors.Border, RoundedCornerShape(24.dp)).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.play_skip_note), fontSize = 24.sp, lineHeight = 32.sp, color = PleosColors.TextSecondary)
                SecondaryButton(stringResource(R.string.play_skip), R.drawable.ic_resume, actions.onSkip, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun NoteCard(@DrawableRes icon: Int, label: String?, text: String) {
    Row(
        Modifier.fillMaxWidth().background(PleosColors.SurfaceLow, RoundedCornerShape(24.dp)).padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(painterResource(icon), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(36.dp))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            label?.let { Text(it, fontSize = 24.sp, lineHeight = 32.sp, color = PleosColors.TextMuted) }
            Text(text, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun HintButton(play: PlayState, actions: PlayActions, modifier: Modifier, height: Int, short: Boolean = false) {
    val total = play.clue.hints.size
    val text = when {
        short -> stringResource(R.string.play_hint_short)
        play.hintsShown == 0 -> stringResource(R.string.play_hint_open, 0, total)
        play.hintsShown < total -> stringResource(R.string.play_hint_more, play.hintsShown, total)
        else -> stringResource(R.string.play_hint_done, total, total)
    }
    SecondaryButton(
        text = text,
        icon = R.drawable.ic_lightbulb,
        onClick = actions.onHint,
        modifier = modifier,
        enabled = play.hintsShown < total,
        height = height.dp,
        fontSize = if (short) 28 else 30,
    )
}

@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = PleosColors.TextMuted, modifier = Modifier.size(32.dp))
        Text(text, fontSize = 26.sp, lineHeight = 34.sp, color = PleosColors.TextMuted)
    }
}

// 에뮬레이터의 조명 브리지는 Virtual Console 세션이 있을 때만 돌아서, 그 밖에는 차량 설정 앱의 조명 변경이 반영되지 않는다. debug 실제 SDK 빌드에서만 안내한다.
private val SHOW_EMULATOR_NOTES = BuildConfig.DEBUG && !BuildConfig.USE_FAKE_SDK

@Composable
private fun EmulatorNote(play: PlayState, modifier: Modifier = Modifier) {
    if (!SHOW_EMULATOR_NOTES) return
    val note = when (play.puzzle.signal) {
        SignalId.L1, SignalId.L2 -> R.string.play_emulator_ambient_note
        SignalId.L3 -> R.string.play_emulator_reading_note
        else -> return
    }
    InfoNote(stringResource(note), modifier)
}

@Composable
internal fun ColumnScope.GaugeTitle(@DrawableRes icon: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Icon(painterResource(icon), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(40.dp))
        Text(title, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
    }
}
