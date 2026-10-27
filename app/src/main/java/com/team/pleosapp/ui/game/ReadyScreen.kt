package com.team.pleosapp.ui.game

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.team.pleosapp.R
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.ui.components.CarSeatsDiagram
import com.team.pleosapp.ui.components.HairLine
import com.team.pleosapp.ui.components.IconSquareButton
import com.team.pleosapp.ui.components.PrimaryButton
import com.team.pleosapp.ui.components.SeatMark
import com.team.pleosapp.ui.components.formatClock
import com.team.pleosapp.ui.theme.PleosColors

data class ReadyActions(
    val onToggleSeat: (SeatPosition) -> Unit,
    val onDontShowIdle: (Boolean) -> Unit,
    val onConfirm: () -> Unit,
    val onHome: () -> Unit,
)

/** S1 준비(시안 Ready-Land·Ready-Port). P 확인 버튼은 게이트가 열려 있을 때만 눌린다. */
@Composable
fun ReadyScreen(ready: ReadyState, gateOpen: Boolean, gate: GateState, actions: ReadyActions, modifier: Modifier = Modifier) {
    val marks = ready.seats.map { SeatMark(it.seat, it.selected, enabled = it.selectable && !ready.resume, label = seatLabel(it.seat)) }
    val subtitle = ready.summary?.let {
        stringResource(R.string.ready_subtitle_resume, it.puzzleNumber, it.puzzleCount, formatClock(it.remainingMs))
    } ?: stringResource(R.string.ready_subtitle, ready.lengthMinutes, ready.puzzleCount)
    val caption = stringResource(
        when {
            ready.resume -> R.string.ready_seat_caption_resume
            // 독서등 capability가 있는 동승 좌석이 하나도 없으면 운전석 1명으로 하고 자리 단서를 뺀다(기획 F3 실패·미지원)
            !ready.passengerSeatsAvailable -> R.string.ready_seat_caption_driver_only
            else -> R.string.ready_seat_caption
        },
    )

    BoxWithConstraints(modifier.fillMaxSize()) {
        if (maxWidth >= WIDE_MIN_WIDTH_DP.dp) {
            Row(Modifier.fillMaxSize()) {
                Column(Modifier.width(660.dp).fillMaxHeight().padding(start = 48.dp, end = 48.dp, top = 56.dp, bottom = 48.dp)) {
                    ParticipantsTitle(ready, titleSize = 40)
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CarSeatsDiagram(min(700.dp, this@BoxWithConstraints.maxHeight - 360.dp), marks, actions.onToggleSeat)
                    }
                    Text(caption, style = MaterialTheme.typography.bodySmall, color = PleosColors.TextMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(PleosColors.Divider))
                Column(Modifier.weight(1f).fillMaxHeight().padding(start = 72.dp, end = 72.dp, top = 56.dp, bottom = 48.dp)) {
                    Header(subtitle, actions.onHome, titleSize = 40)
                    Column(Modifier.padding(top = 32.dp)) {
                        HairLine()
                        ready.seats.forEach { SeatRow(it, enabled = !ready.resume, onToggle = actions.onToggleSeat) }
                    }
                    if (ready.showIdleNotice) IdleNotice(ready, actions, Modifier.padding(top = 32.dp))
                    Spacer(Modifier.weight(1f))
                    GateLine(gate, Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp))
                    PrimaryButton(stringResource(R.string.ready_confirm), R.drawable.ic_check, actions.onConfirm, Modifier.fillMaxWidth(), enabled = gateOpen)
                    Text(
                        stringResource(R.string.ready_confirm_hint),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                        color = PleosColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
        } else {
            Column(Modifier.fillMaxSize().padding(start = 40.dp, end = 40.dp, top = 40.dp, bottom = 48.dp)) {
                Header(subtitle, actions.onHome, titleSize = 36)
                if (ready.showIdleNotice) IdleNotice(ready, actions, Modifier.padding(top = 24.dp))
                Box(Modifier.padding(top = 28.dp)) { ParticipantsTitle(ready, titleSize = 32) }
                // 남은 높이에 맞춰 차 그림을 줄인다(공회전 안내가 있으면 그만큼 작아진다). 아래 72dp는 안내 문장 자리
                BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    val imageHeight = min(480.dp, maxHeight - 72.dp)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CarSeatsDiagram(imageHeight, marks, actions.onToggleSeat, button = 72.dp, minGap = 8.dp) // 원이 차 그림 안에 들어오게(F1 QA 관찰)
                        Text(
                            caption,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Normal),
                            color = PleosColors.TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
                GateLine(gate, Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp, bottom = 12.dp), fontSize = 24)
                PrimaryButton(stringResource(R.string.ready_confirm), R.drawable.ic_check, actions.onConfirm, Modifier.fillMaxWidth(), enabled = gateOpen)
            }
        }
    }
}

@Composable
private fun ParticipantsTitle(ready: ReadyState, titleSize: Int) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.ready_participants), style = if (titleSize >= 40) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall)
        Text(
            stringResource(R.string.ready_participant_count, ready.participantCount),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = PleosColors.Positive,
        )
    }
}

@Composable
private fun Header(subtitle: String, onHome: () -> Unit, titleSize: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        IconSquareButton(R.drawable.ic_arrow_back, stringResource(R.string.back_home), onHome, size = if (titleSize >= 40) 88.dp else 80.dp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.ready_title), style = if (titleSize >= 40) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PleosColors.TextMuted)
        }
    }
}

/** 좌석 목록 한 줄. 고를 수 있는 좌석이면 줄 전체가 좌석 원과 같은 토글이다(높이 96dp). */
@Composable
private fun SeatRow(choice: SeatChoice, enabled: Boolean, onToggle: (SeatPosition) -> Unit) {
    val muted = !choice.selected
    Row(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .toggleable(
                value = choice.selected,
                enabled = enabled && choice.selectable,
                role = Role.Checkbox,
                onValueChange = { onToggle(choice.seat) },
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_airline_seat_recline_normal),
            contentDescription = null,
            tint = PleosColors.Text,
            modifier = Modifier.size(40.dp).alpha(if (muted) 0.4f else 1f),
        )
        Text(
            seatLabel(choice.seat),
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = if (muted) PleosColors.TextMuted else PleosColors.Text,
            modifier = Modifier.weight(1f),
        )
        val (status, color) = when {
            choice.selected -> stringResource(R.string.seat_joined) to PleosColors.Positive
            !choice.selectable && choice.seat != SeatPosition.DRIVER -> stringResource(R.string.seat_unavailable) to PleosColors.TextDisabled
            else -> stringResource(R.string.seat_empty) to PleosColors.TextMuted
        }
        Text(status, style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (choice.selected) FontWeight.SemiBold else FontWeight.Normal), color = color)
    }
    HairLine()
}

@Composable
private fun IdleNotice(ready: ReadyState, actions: ReadyActions, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().background(PleosColors.SurfaceLow, RoundedCornerShape(24.dp)).padding(horizontal = 32.dp, vertical = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(painterResource(R.drawable.ic_info), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(40.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.idle_title), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
            Text(stringResource(R.string.idle_body), style = MaterialTheme.typography.bodySmall, color = PleosColors.TextSecondary)
            // 체크 상자와 글자를 한 줄 토글로 묶어 줄 전체(높이 80dp)를 누를 수 있게 한다
            Row(
                Modifier
                    .heightIn(min = 80.dp)
                    .toggleable(value = ready.dontShowIdleAgain, role = Role.Checkbox, onValueChange = actions.onDontShowIdle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = ready.dontShowIdleAgain,
                    onCheckedChange = null,
                    colors = CheckboxDefaults.colors(checkedColor = PleosColors.Selected),
                )
                Text(
                    stringResource(R.string.idle_dont_show),
                    style = MaterialTheme.typography.bodySmall,
                    color = PleosColors.TextSecondary,
                    modifier = Modifier.padding(start = 12.dp, end = 16.dp),
                )
            }
        }
    }
}

@Composable
fun seatLabel(seat: SeatPosition): String = stringResource(
    when (seat) {
        SeatPosition.DRIVER -> R.string.seat_driver
        SeatPosition.PASSENGER -> R.string.seat_passenger
        SeatPosition.REAR_LEFT -> R.string.seat_rear_left
        SeatPosition.REAR_RIGHT, SeatPosition.REAR_CENTER -> R.string.seat_rear_right
    },
)
