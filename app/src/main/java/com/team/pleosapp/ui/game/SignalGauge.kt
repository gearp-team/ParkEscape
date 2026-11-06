package com.team.pleosapp.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.pleosapp.R
import com.team.pleosapp.domain.clue.ClueWriter
import com.team.pleosapp.domain.puzzle.NamedColor
import com.team.pleosapp.domain.puzzle.PuzzleJudge
import com.team.pleosapp.domain.puzzle.SignalId
import com.team.pleosapp.domain.puzzle.SteeringSide
import com.team.pleosapp.domain.puzzle.SteeringZones
import com.team.pleosapp.ui.components.PrimaryButton
import com.team.pleosapp.ui.theme.PleosColors
import java.util.Locale

/** 목표 구간 색(파랑 16% 채움 + 3dp 테두리, 시안 Play-Direct-Land). */
private val TargetFill = Color(0x290064FF)

/**
 * 현재 신호 게이지. 지시형만 목표 구간을 그리고(Gauge.target 등이 null이 아님), 암호형·연쇄형은 현재값만 보여 준다.
 * 값은 리스너 이벤트로만 바뀐다(폴링·애니메이션 없음).
 */
@Composable
fun ColumnScope.SignalGauge(play: PlayState, actions: PlayActions, wide: Boolean) {
    when (val g = play.gauge) {
        is Gauge.Scale -> ScaleGauge(g, wide)
        is Gauge.Recirculation -> RecirculationGauge(g, wide)
        is Gauge.Switch -> SwitchGauge(g, wide)
        is Gauge.AmbientColor -> ColorGauge(g, wide)
        is Gauge.ReadingLights -> ReadingGauge(g, wide)
        is Gauge.Steering -> SteeringGauge(g, wide)
        is Gauge.Brake -> BrakeGauge(g, wide)
        Gauge.SealButton -> {
            GaugeTitle(R.drawable.ic_lock, stringResource(R.string.play_gauge_seal))
            Text(stringResource(R.string.play_seal_note), fontSize = 26.sp, lineHeight = 34.sp, color = PleosColors.TextMuted, modifier = Modifier.padding(top = 24.dp))
            PrimaryButton(stringResource(R.string.play_seal_button), R.drawable.ic_lock_open, actions.onSeal, Modifier.fillMaxWidth().padding(top = 32.dp))
        }
    }
    if (play.gauge !is Gauge.Brake && play.gauge != Gauge.SealButton) {
        // 핸들은 칸에 1초 머문 뒤 가운데로 돌아와야 넘어간다(F2-ui QA MINOR 1)
        val steering = play.gauge is Gauge.Steering
        val note = when {
            steering && play.showsTarget -> R.string.play_note_steering_direct
            steering -> R.string.play_note_steering_hidden
            play.showsTarget -> R.string.play_note_direct
            else -> R.string.play_note_hidden
        }
        Text(
            stringResource(note),
            fontSize = 26.sp,
            lineHeight = 34.sp,
            color = PleosColors.TextMuted,
            modifier = Modifier.padding(top = 24.dp),
        )
    }
}

@Composable
private fun BigValue(text: String, wide: Boolean, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        fontSize = if (wide) 96.sp else 72.sp,
        lineHeight = if (wide) 112.sp else 84.sp,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
    )
}

// ---- C1·C2·L2 ---------------------------------------------------------------------------------

@Composable
private fun ColumnScope.ScaleGauge(g: Gauge.Scale, wide: Boolean) {
    val (icon, title) = when (g.signal) {
        SignalId.C1 -> R.drawable.ic_device_thermostat to R.string.play_gauge_temperature
        SignalId.C2 -> R.drawable.ic_mode_fan to R.string.play_gauge_fan
        else -> R.drawable.ic_light to R.string.play_gauge_brightness
    }
    GaugeTitle(icon, stringResource(title))
    val unknown = stringResource(R.string.play_value_unknown)
    fun format(v: Float): String = when (g.signal) {
        SignalId.C1 -> String.format(Locale.US, "%.1f℃", v)
        SignalId.C2 -> "${v.toInt()}단"
        else -> v.toInt().toString()
    }
    BigValue(g.current?.let(::format) ?: unknown, wide, Modifier.padding(top = 20.dp))
    Track(
        current = g.current,
        min = g.min,
        max = g.max,
        target = g.target,
        minLabel = format(g.min),
        maxLabel = format(g.max),
        modifier = Modifier.padding(top = if (wide) 64.dp else 40.dp),
    )
}

/** 막대 눈금: 회색 막대, 목표 구간(있을 때만), 현재값 손잡이. */
@Composable
private fun Track(
    current: Float?,
    min: Float,
    max: Float,
    target: ClosedFloatingPointRange<Float>?,
    minLabel: String,
    maxLabel: String,
    modifier: Modifier,
) {
    fun frac(v: Float) = ((v - min) / (max - min)).coerceIn(0f, 1f)
    BoxWithConstraints(modifier.fillMaxWidth().height(140.dp)) {
        val w = maxWidth
        if (target != null) {
            val start = frac(target.start)
            val end = frac(target.endInclusive)
            val zoneWidth = maxOf(w * (end - start), 24.dp)
            val zoneX = (w * ((start + end) / 2f) - zoneWidth / 2).coerceIn(0.dp, w - zoneWidth)
            Text(
                stringResource(R.string.play_target_zone),
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = PleosColors.Positive,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(160.dp).offset(x = (zoneX + zoneWidth / 2 - 80.dp).coerceIn(0.dp, w - 160.dp)),
            )
            Box(
                Modifier
                    .offset(x = zoneX, y = 48.dp)
                    .size(zoneWidth, 44.dp)
                    .background(TargetFill, RoundedCornerShape(10.dp))
                    .border(3.dp, PleosColors.Positive, RoundedCornerShape(10.dp)),
            )
        }
        Box(Modifier.offset(y = 64.dp).fillMaxWidth().height(12.dp).background(PleosColors.Divider, RoundedCornerShape(6.dp)))
        if (current != null) Knob(Modifier.offset(x = w * frac(current) - 22.dp, y = 48.dp))
        Text(minLabel, fontSize = 24.sp, color = PleosColors.TextMuted, modifier = Modifier.offset(y = 104.dp))
        Text(maxLabel, fontSize = 24.sp, color = PleosColors.TextMuted, modifier = Modifier.align(Alignment.TopEnd).offset(y = 104.dp))
    }
}

@Composable
private fun Knob(modifier: Modifier) {
    Box(
        modifier
            .size(44.dp)
            .shadow(4.dp, CircleShape)
            .background(Color.White, CircleShape)
            .padding(5.dp)
            .background(PleosColors.Text, CircleShape),
    )
}

// ---- C3 ---------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.RecirculationGauge(g: Gauge.Recirculation, wide: Boolean) {
    GaugeTitle(R.drawable.ic_car_fan_recirculate, stringResource(R.string.play_gauge_recirculation))
    val outside = stringResource(R.string.play_recirc_outside)
    val inside = stringResource(R.string.play_recirc_inside)
    BigValue(
        when (g.current) {
            null -> stringResource(R.string.play_value_unknown)
            true -> inside
            false -> outside
        },
        wide,
        Modifier.padding(top = 20.dp),
    )
    Row(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(false to outside, true to inside).forEach { (value, label) ->
            StateTile(label, selected = g.current == value, target = g.target == value, Modifier.weight(1f))
        }
    }
}

// ---- C4·L4 -------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.SwitchGauge(g: Gauge.Switch, wide: Boolean) {
    val (icon, title) = when (g.signal) {
        SignalId.C4 -> R.drawable.ic_defrost to R.string.play_gauge_defroster
        else -> R.drawable.ic_headlight to R.string.play_gauge_low_beam
    }
    GaugeTitle(icon, stringResource(title))
    val off = stringResource(R.string.play_light_off)
    val on = stringResource(R.string.play_light_on)
    BigValue(
        when (g.current) {
            null -> stringResource(R.string.play_value_unknown)
            true -> on
            false -> off
        },
        wide,
        Modifier.padding(top = 20.dp),
    )
    Row(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        listOf(false to off, true to on).forEach { (value, label) ->
            StateTile(label, selected = g.current == value, target = g.target == value, Modifier.weight(1f))
        }
    }
}

/** 상태 칸: 현재 상태는 진하게, 목표(지시형만)는 파란 테두리. */
@Composable
private fun StateTile(label: String, selected: Boolean, target: Boolean, modifier: Modifier) {
    Box(
        modifier
            .height(120.dp)
            .background(if (selected) PleosColors.Selected else PleosColors.SurfaceLow, RoundedCornerShape(20.dp))
            .then(if (target) Modifier.border(4.dp, PleosColors.Positive, RoundedCornerShape(20.dp)) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else PleosColors.Text)
            if (target) {
                Text(
                    stringResource(R.string.play_target_zone),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else PleosColors.Positive,
                )
            }
        }
    }
}

// ---- L1 ---------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.ColorGauge(g: Gauge.AmbientColor, wide: Boolean) {
    GaugeTitle(R.drawable.ic_light, stringResource(R.string.play_gauge_color))
    Row(Modifier.padding(top = 32.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        val swatch = g.currentArgb?.let { Color(it or 0xFF000000.toInt()) } ?: PleosColors.SurfaceHigh
        Box(Modifier.size(if (wide) 120.dp else 96.dp).background(swatch, CircleShape).border(2.dp, PleosColors.Divider, CircleShape))
        Text(ColorNames.describe(g.currentArgb), fontSize = if (wide) 48.sp else 40.sp, lineHeight = 64.sp, fontWeight = FontWeight.Bold)
    }
    Row(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        NamedColor.entries.forEach { color ->
            val isTarget = g.target == color
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .size(64.dp)
                        .then(if (isTarget) Modifier.border(4.dp, PleosColors.Positive, CircleShape) else Modifier)
                        .padding(8.dp)
                        .background(Color(ColorNames.swatch(color)), CircleShape),
                )
                Text(
                    ClueWriter.colorName(color),
                    fontSize = 22.sp,
                    lineHeight = 30.sp,
                    fontWeight = if (isTarget) FontWeight.Bold else FontWeight.Normal,
                    color = if (isTarget) PleosColors.Positive else PleosColors.TextMuted,
                )
            }
        }
    }
}

// ---- L3 ---------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.ReadingGauge(g: Gauge.ReadingLights, wide: Boolean) {
    GaugeTitle(R.drawable.ic_lightbulb, stringResource(R.string.play_gauge_reading))
    Column(Modifier.fillMaxWidth().padding(top = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        g.seats.forEach { (seat, on) ->
            val isTarget = g.target == seat
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(if (wide) 100.dp else 88.dp)
                    .background(PleosColors.SurfaceLow, RoundedCornerShape(20.dp))
                    .then(if (isTarget) Modifier.border(4.dp, PleosColors.Positive, RoundedCornerShape(20.dp)) else Modifier)
                    .padding(horizontal = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(ClueWriter.seatName(seat), fontSize = 30.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (isTarget) {
                    Text(stringResource(R.string.play_target_zone), fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = PleosColors.Positive, modifier = Modifier.padding(end = 20.dp))
                }
                val text = when (on) {
                    null -> stringResource(R.string.play_value_unknown)
                    true -> stringResource(R.string.play_light_on)
                    false -> stringResource(R.string.play_light_off)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.size(14.dp).background(if (on == true) PleosColors.Active else PleosColors.TextDisabled, CircleShape))
                    Text(text, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---- D1 ---------------------------------------------------------------------------------------

/** 화면에 그리는 핸들 범위: 한쪽 범위의 ±20%(칸은 5~15%). */
private const val STEERING_VIEW = 0.2

@Composable
private fun ColumnScope.SteeringGauge(g: Gauge.Steering, wide: Boolean) {
    GaugeTitle(R.drawable.ic_steering, stringResource(R.string.play_gauge_steering))
    val angle = g.angle
    val bin = angle?.let { SteeringZones.binOf(it, g.boundsMin, g.boundsMax) }
    val left = stringResource(R.string.play_steering_left)
    val right = stringResource(R.string.play_steering_right)
    val valueText = when {
        angle == null -> stringResource(R.string.play_value_unknown)
        SteeringZones.isCentered(angle, g.boundsMin, g.boundsMax) -> stringResource(R.string.play_steering_center)
        bin != null -> ClueWriter.steeringName(bin.first, bin.second)
        else -> if (angle < 0) left else right
    }
    BigValue(valueText, wide, Modifier.padding(top = 20.dp))
    SteeringTrack(g, Modifier.padding(top = 40.dp))
    val status = when {
        g.held -> stringResource(R.string.play_steering_return)
        g.holdMs != null -> stringResource(R.string.play_steering_holding, String.format(Locale.US, "%.1f", minOf(g.holdMs, PuzzleJudge.HOLD_MS) / 1000f))
        else -> stringResource(R.string.play_steering_hold)
    }
    Text(status, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, color = if (g.held) PleosColors.Positive else PleosColors.Text, modifier = Modifier.padding(top = 24.dp))
}

/** 왼쪽(음수) ← 가운데 → 오른쪽. 칸 경계는 판을 만들 때의 범위 비율이다. */
@Composable
private fun SteeringTrack(g: Gauge.Steering, modifier: Modifier) {
    fun x(ratio: Double): Float = ((ratio + STEERING_VIEW) / (2 * STEERING_VIEW)).toFloat().coerceIn(0f, 1f)
    val edges = SteeringZones.BIN_EDGES
    BoxWithConstraints(modifier.fillMaxWidth().height(150.dp)) {
        val w = maxWidth
        Box(Modifier.offset(y = 40.dp).fillMaxWidth().height(48.dp).background(PleosColors.SurfaceLow, RoundedCornerShape(12.dp)))
        // 가운데(±5%)
        val cx0 = w * x(-SteeringZones.CENTER_RATIO)
        val cx1 = w * x(SteeringZones.CENTER_RATIO)
        Box(Modifier.offset(x = cx0, y = 40.dp).size(cx1 - cx0, 48.dp).background(PleosColors.SurfaceHigh))
        for (side in SteeringSide.entries) {
            for (b in 1..3) {
                val lo = edges[b - 1]
                val hi = edges[b]
                val (r0, r1) = if (side == SteeringSide.LEFT) -hi to -lo else lo to hi
                val x0 = w * x(r0)
                val x1 = w * x(r1)
                val isTarget = g.target == (side to b)
                ZoneBox(x0, x1 - x0, isTarget)
                Text(
                    "$b",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isTarget) PleosColors.Positive else PleosColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(x = x0, y = 100.dp).width(x1 - x0),
                )
            }
        }
        if (g.target != null) {
            Text(
                stringResource(R.string.play_target_zone),
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = PleosColors.Positive,
                modifier = Modifier.align(if (g.target.first == SteeringSide.LEFT) Alignment.TopStart else Alignment.TopEnd),
            )
        }
        g.angle?.let { a ->
            val ratio = SteeringZones.ratio(a, g.boundsMin, g.boundsMax) * if (a < 0) -1 else 1
            Knob(Modifier.offset(x = w * x(ratio) - 22.dp, y = 42.dp))
        }
        Text(stringResource(R.string.play_steering_left), fontSize = 22.sp, color = PleosColors.TextMuted, modifier = Modifier.align(Alignment.BottomStart))
        Text(stringResource(R.string.play_steering_right), fontSize = 22.sp, color = PleosColors.TextMuted, modifier = Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun ZoneBox(x: Dp, width: Dp, target: Boolean) {
    Box(
        Modifier
            .offset(x = x, y = 40.dp)
            .size(width, 48.dp)
            .padding(horizontal = 2.dp)
            .background(if (target) TargetFill else PleosColors.Divider, RoundedCornerShape(8.dp))
            .then(if (target) Modifier.border(3.dp, PleosColors.Positive, RoundedCornerShape(8.dp)) else Modifier),
    )
}

// ---- B1 ---------------------------------------------------------------------------------------

@Composable
private fun ColumnScope.BrakeGauge(g: Gauge.Brake, wide: Boolean) {
    val p = g.progress
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.ic_brake_pedal), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(36.dp))
            Text(stringResource(R.string.play_gauge_brake), fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)
        }
        Text("${p.count} / ${p.target}", fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold)
    }
    val circle = when {
        p.target <= 2 -> if (wide) 180.dp else 168.dp
        p.target == 3 -> 140.dp
        else -> 112.dp
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(if (p.target <= 2) 48.dp else 28.dp, Alignment.CenterHorizontally),
    ) {
        repeat(p.target) { i ->
            if (i < p.count) {
                Box(Modifier.size(circle).background(PleosColors.Selected, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_lock_open), contentDescription = null, tint = Color.White, modifier = Modifier.size(circle / 2))
                }
            } else {
                Box(Modifier.size(circle), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawCircle(
                            color = PleosColors.Border,
                            radius = size.minDimension / 2 - 2.dp.toPx(),
                            style = Stroke(width = 4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))),
                        )
                    }
                    Text("${i + 1}", fontSize = (circle.value * 0.4f).sp, fontWeight = FontWeight.Bold, color = PleosColors.TextDisabled)
                }
            }
        }
    }
    val window = p.windowRemainingMs
    if (window == null) {
        Text(stringResource(R.string.play_brake_start), fontSize = 24.sp, lineHeight = 32.sp, color = PleosColors.TextMuted)
    } else {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Text(stringResource(R.string.play_brake_window), fontSize = 24.sp, lineHeight = 32.sp, color = PleosColors.TextMuted, modifier = Modifier.weight(1f))
            val seconds = ((minOf(window, p.gapRemainingMs ?: window) + 999) / 1000).toInt()
            Text(stringResource(R.string.play_brake_left, seconds), fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 16.dp))
        }
        val fraction = (window.toFloat() / PuzzleJudge.WINDOW_MS).coerceIn(0f, 1f)
        Box(Modifier.padding(top = 12.dp).fillMaxWidth().height(10.dp).background(PleosColors.Divider, RoundedCornerShape(5.dp))) {
            Box(Modifier.fillMaxWidth(fraction).height(10.dp).background(PleosColors.Positive, RoundedCornerShape(5.dp)))
        }
    }
}
