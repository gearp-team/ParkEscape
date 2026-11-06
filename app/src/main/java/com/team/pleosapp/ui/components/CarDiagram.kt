package com.team.pleosapp.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.pleosapp.R
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.ui.theme.PleosColors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

/** car_top 이미지 가로/세로(689×1344). */
private const val CAR_ASPECT = 689f / 1344f

/** 시안(Main·Ready) 기준: 이미지 좌우 여백 = 이미지 폭의 103/359, 위아래 여백 = 이미지 높이의 30/700. */
private const val SIDE_MARGIN = 103f / 359f
private const val TOP_MARGIN = 30f / 700f

/**
 * 차 위 조작 위치(기획 시안 run_meta design.seatAnchors). anchor = 이미지 안 비율(x, y),
 * bubbleY = 옆 원형 아이콘의 세로 위치(이미지 높이 비율).
 */
private data class ControlSpot(
    @DrawableRes val icon: Int,
    @StringRes val label: Int,
    val left: Boolean,
    val bubbleY: Float,
    /** 세로 1단(시안 Home-Port)의 원 위치. 원이 작고 글자와 다음 원이 가까워서 위아래로 더 벌린다. */
    val bubbleYPortrait: Float,
    val anchorX: Float,
    val anchorY: Float,
)

/*
 * 한쪽에 원이 셋이다(왼쪽: 전조등·브레이크·핸들, 오른쪽: 앞유리 제상·공조·실내 조명). 전조등·앞유리 제상은 위급할 때 쓰는 조작이라
 * 차 앞쪽(그림 위)에 둔다(2026-11-06). 원 사이 간격은 원 + 이름 높이(가로 2단 약 0.17, 세로 1단 약 0.29 × 그림 높이)보다 넓게 잡고,
 * 같은 쪽의 선이 서로 엇갈리지 않게 원 순서를 기준점의 위아래 순서와 맞췄다.
 */
private val CONTROL_SPOTS = listOf(
    ControlSpot(R.drawable.ic_headlight, R.string.control_headlight, left = true, bubbleY = 0.110f, bubbleYPortrait = 0.050f, anchorX = 0.200f, anchorY = 0.075f),
    ControlSpot(R.drawable.ic_brake_pedal, R.string.control_brake, left = true, bubbleY = 0.330f, bubbleYPortrait = 0.370f, anchorX = 0.288f, anchorY = 0.265f),
    ControlSpot(R.drawable.ic_steering, R.string.control_steering, left = true, bubbleY = 0.550f, bubbleYPortrait = 0.700f, anchorX = 0.341f, anchorY = 0.310f),
    ControlSpot(R.drawable.ic_defrost, R.string.control_defrost, left = false, bubbleY = 0.110f, bubbleYPortrait = 0.050f, anchorX = 0.500f, anchorY = 0.225f),
    ControlSpot(R.drawable.ic_mode_fan, R.string.control_climate, left = false, bubbleY = 0.330f, bubbleYPortrait = 0.370f, anchorX = 0.500f, anchorY = 0.298f),
    ControlSpot(R.drawable.ic_lightbulb, R.string.control_cabin_light, left = false, bubbleY = 0.550f, bubbleYPortrait = 0.700f, anchorX = 0.500f, anchorY = 0.525f),
)

/** 좌석 위치(이미지 비율): 1열 y 40.6%, 2열 y 64.4%, 왼쪽 x 36.3%, 오른쪽 x 63.7%. */
private fun seatAnchor(seat: SeatPosition): Pair<Float, Float> = when (seat) {
    SeatPosition.DRIVER -> 0.363f to 0.406f
    SeatPosition.PASSENGER -> 0.637f to 0.406f
    SeatPosition.REAR_LEFT -> 0.363f to 0.644f
    SeatPosition.REAR_RIGHT -> 0.637f to 0.644f
    SeatPosition.REAR_CENTER -> 0.5f to 0.644f
}

private class CarFrame(imageHeight: Dp, sideMargin: Dp? = null) {
    val imageWidth = imageHeight * CAR_ASPECT
    val imageHeight = imageHeight
    val margin = sideMargin ?: (imageWidth * SIDE_MARGIN)
    val pad = imageHeight * TOP_MARGIN
    val width = imageWidth + margin * 2
    val height = imageHeight + pad * 2

    fun x(fraction: Float): Dp = margin + imageWidth * fraction
    fun y(fraction: Float): Dp = pad + imageHeight * fraction
}

@Composable
private fun CarImage(frame: CarFrame, description: String) {
    Image(
        painter = painterResource(R.drawable.car_top),
        contentDescription = description,
        modifier = Modifier.offset(frame.margin, frame.pad).size(frame.imageWidth, frame.imageHeight),
    )
}

/**
 * S0 홈: 위에서 본 차와 조작 위치(전조등·브레이크·핸들·앞유리 제상·공조·실내 조명).
 * [gap]은 원형 아이콘과 차 이미지 사이 간격(시안: 가로 2단 31, 세로 1단 70).
 */
@Composable
fun CarControlsDiagram(
    imageHeight: Dp,
    modifier: Modifier = Modifier,
    bubble: Dp = 72.dp,
    labelSize: Int = 24,
    gap: Dp = 31.dp,
    portrait: Boolean = false,
) {
    val frame = CarFrame(imageHeight, sideMargin = bubble + gap)
    fun bubbleY(spot: ControlSpot) = if (portrait) spot.bubbleYPortrait else spot.bubbleY
    Box(modifier.size(frame.width, frame.height)) {
        CarImage(frame, stringResource(R.string.car_top_description))
        Canvas(Modifier.size(frame.width, frame.height)) {
            CONTROL_SPOTS.forEach { spot ->
                val startX = if (spot.left) bubble.toPx() else (frame.width - bubble).toPx()
                val start = Offset(startX, frame.y(bubbleY(spot)).toPx())
                val end = Offset(frame.x(spot.anchorX).toPx(), frame.y(spot.anchorY).toPx())
                drawLine(PleosColors.TextDisabled, start, end, strokeWidth = 2.dp.toPx())
                drawCircle(Color.White, radius = 8.5.dp.toPx(), center = end)
                drawCircle(PleosColors.Text, radius = 7.dp.toPx(), center = end)
                drawCircle(Color.White, radius = 8.5.dp.toPx(), center = end, style = Stroke(3.dp.toPx()))
            }
        }
        CONTROL_SPOTS.forEach { spot ->
            val x = if (spot.left) 0.dp else frame.width - bubble
            Column(
                Modifier.offset(x, frame.y(bubbleY(spot)) - bubble / 2).requiredWidth(bubble),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    Modifier.size(bubble).shadow(8.dp, CircleShape).background(Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(spot.icon), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(bubble / 2))
                }
                Text(
                    stringResource(spot.label),
                    fontSize = labelSize.sp,
                    lineHeight = (labelSize + 8).sp,
                    fontWeight = FontWeight.SemiBold,
                    softWrap = false,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.requiredWidth(bubble * 2.4f),
                )
            }
        }
    }
}

/** 좌석 하나의 표시 상태. */
data class SeatMark(val seat: SeatPosition, val selected: Boolean, val enabled: Boolean, val label: String)

/** S1 준비: 위에서 본 차와 좌석 4개(누르면 참가·비어 있음 전환). */
@Composable
fun CarSeatsDiagram(
    imageHeight: Dp,
    seats: List<SeatMark>,
    onToggle: (SeatPosition) -> Unit,
    modifier: Modifier = Modifier,
    button: Dp = 80.dp,
    minGap: Dp = 12.dp,
) {
    val frame = CarFrame(imageHeight)
    // 차 그림이 작아지면(세로 1단) 좌석 원이 서로 겹치지 않게 중심 간격을 지름 + [minGap] 이상으로 벌린다
    val minHalf = (button + minGap) / 2
    val center = frame.x(0.5f) to frame.y((seatAnchor(SeatPosition.DRIVER).second + seatAnchor(SeatPosition.REAR_LEFT).second) / 2)
    fun spread(at: Dp, mid: Dp): Dp {
        val d = (at - mid).value
        return mid + (sign(d) * max(abs(d), minHalf.value)).dp
    }
    Box(modifier.size(frame.width, frame.height)) {
        CarImage(frame, stringResource(R.string.car_top_description))
        seats.forEach { mark ->
            val (ax, ay) = seatAnchor(mark.seat)
            val x = spread(frame.x(ax), center.first)
            val y = spread(frame.y(ay), center.second)
            val description = stringResource(if (mark.selected) R.string.seat_button_joined else R.string.seat_button_empty, mark.label)
            Button(
                onClick = { onToggle(mark.seat) },
                enabled = mark.enabled,
                shape = CircleShape,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                modifier = Modifier
                    .offset(x - button / 2, y - button / 2)
                    .size(button)
                    .shadow(if (mark.selected) 10.dp else 8.dp, CircleShape)
                    .semantics {
                        role = Role.Checkbox
                        selected = mark.selected
                        contentDescription = description
                    },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (mark.selected) PleosColors.Selected else Color.White,
                    contentColor = if (mark.selected) Color.White else PleosColors.TextMuted,
                    disabledContainerColor = if (mark.selected) PleosColors.Selected else PleosColors.SurfaceHigh,
                    disabledContentColor = if (mark.selected) Color.White else PleosColors.TextDisabled,
                ),
            ) {
                Icon(
                    painterResource(if (mark.selected) R.drawable.ic_person else R.drawable.ic_add),
                    contentDescription = null,
                    modifier = Modifier.size(button / 2),
                )
            }
        }
    }
}
