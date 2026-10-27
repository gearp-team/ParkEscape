package com.team.pleosapp.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.pleosapp.ui.theme.PleosColors
import java.util.Locale

/** 주 버튼(filled). 높이 112, 모서리 24(Pleos 버튼 Large). */
@Composable
fun PrimaryButton(
    text: String,
    @DrawableRes icon: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 112.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PleosColors.ButtonFilled,
            contentColor = Color.White,
            disabledContainerColor = PleosColors.ButtonBasic,
            disabledContentColor = PleosColors.TextDisabled,
        ),
    ) {
        ButtonContent(text, icon, iconSize = 44.dp, fontSize = 34, weight = FontWeight.Bold)
    }
}

/** 보조 버튼(basic). */
@Composable
fun SecondaryButton(
    text: String,
    @DrawableRes icon: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 88.dp,
    fontSize: Int = 28,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(height),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PleosColors.ButtonBasic,
            contentColor = PleosColors.Text,
            disabledContainerColor = PleosColors.ButtonBasic,
            disabledContentColor = PleosColors.TextDisabled,
        ),
    ) {
        ButtonContent(text, icon, iconSize = 36.dp, fontSize = fontSize, weight = FontWeight.SemiBold)
    }
}

@Composable
private fun RowScope.ButtonContent(text: String, @DrawableRes icon: Int?, iconSize: Dp, fontSize: Int, weight: FontWeight) {
    if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(iconSize))
        Box(Modifier.width(12.dp))
    }
    Text(text, fontSize = fontSize.sp, fontWeight = weight, lineHeight = (fontSize + 8).sp, color = Color.Unspecified)
}

/** 아이콘만 있는 정사각 버튼(설정·뒤로). */
@Composable
fun IconSquareButton(@DrawableRes icon: Int, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 88.dp) {
    Button(
        onClick = onClick,
        modifier = modifier.size(size).semantics { contentDescription = label },
        shape = RoundedCornerShape(if (size >= 88.dp) 24.dp else 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PleosColors.ButtonBasic, contentColor = PleosColors.Text),
    ) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(size * 0.45f))
    }
}

/** 판 길이 세그먼트. 선택 #131417, 고를 수 없는 칸은 흐린 글자. */
@Composable
fun <T> Segmented(
    options: List<T>,
    selected: T,
    enabled: (T) -> Boolean,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 108.dp,
    fontSize: Int = 32,
    sublabel: (T) -> String? = { null },
) {
    Row(
        modifier
            .fillMaxWidth()
            .height(height)
            .border(2.dp, PleosColors.Divider, RoundedCornerShape(16.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val isEnabled = enabled(option)
            Button(
                onClick = { onSelect(option) },
                enabled = isEnabled,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics {
                        role = Role.Tab
                        this.selected = isSelected
                    },
                shape = RoundedCornerShape(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) PleosColors.Selected else Color.Transparent,
                    contentColor = if (isSelected) Color.White else PleosColors.Text,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = PleosColors.TextDisabled,
                ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        label(option),
                        fontSize = fontSize.sp,
                        lineHeight = (fontSize + 6).sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                        color = Color.Unspecified,
                    )
                    sublabel(option)?.let {
                        Text(it, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Normal, color = Color.Unspecified)
                    }
                }
            }
        }
    }
}

/** 상태 점 + 글자(정차 중 = 초록). */
@Composable
fun StatusDot(color: Color, text: String, modifier: Modifier = Modifier, fontSize: Int = 26) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(14.dp).background(color, CircleShape))
        Text(text, fontSize = fontSize.sp, lineHeight = (fontSize + 8).sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 1px 구분선. */
@Composable
fun HairLine(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth().height(1.dp).background(PleosColors.Divider))
}

/** [content]가 처음 그려지는 순간 한 번 [onFirstDraw]를 부른다(F1 완료 조건 "콜백 뒤 100ms 안에 S-P를 그린다" 측정용). */
@Composable
fun FirstDrawLogger(onFirstDraw: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val done = remember { booleanArrayOf(false) }
    Box(
        modifier.drawWithContent {
            drawContent()
            if (!done[0]) {
                done[0] = true
                onFirstDraw()
            }
        },
    ) { content() }
}

/** 남은 시간 m:ss. */
fun formatClock(ms: Long): String {
    val total = (ms.coerceAtLeast(0) + 999) / 1000
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

/** 기록 m:ss(내림). */
fun formatRecord(ms: Long): String {
    val total = ms.coerceAtLeast(0) / 1000
    return String.format(Locale.US, "%d:%02d", total / 60, total % 60)
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleLarge)
}
