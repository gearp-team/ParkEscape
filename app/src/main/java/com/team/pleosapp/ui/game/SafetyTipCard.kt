package com.team.pleosapp.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.pleosapp.R
import com.team.pleosapp.ui.theme.PleosColors

/**
 * "실제 상황 팁" 카드. 퍼즐이 풀리면 화면 위쪽 가운데에 4초 동안 뜬다(ViewModel이 내린다). 터치 요소가 없어 아래 화면 조작을 막지 않는다.
 * 모양은 Pleos Toast(앱 패널 안 여백 24, 최대 너비 792, 어두운 바탕)를 따르되, 다음 단서와 함께 눈에 띄도록 위쪽에 둔다.
 */
@Composable
fun SafetyTipCard(tip: TipCard, modifier: Modifier = Modifier) {
    Row(
        modifier
            .widthIn(max = 792.dp)
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .background(PleosColors.Selected, RoundedCornerShape(24.dp))
            .padding(horizontal = 32.dp, vertical = 24.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(Modifier.size(72.dp).background(Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(iconOf(tip.signal)), contentDescription = null, tint = PleosColors.Text, modifier = Modifier.size(40.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.tip_title),
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.72f),
            )
            Text(tip.text, fontSize = 30.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
