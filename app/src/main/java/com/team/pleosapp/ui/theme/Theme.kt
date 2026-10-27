package com.team.pleosapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.team.pleosapp.R

/**
 * Pleos 디자인 기준(라이트 테마). 값은 `_workspace/03_plan/design-canvas/pleos-tokens.md`
 * (키트 docs/connect/guide/docs-design 원본 이미지에서 읽은 값)를 그대로 옮겼다.
 */
object PleosColors {
    /** 바탕 surface_basic = basic_00 */
    val Surface = Color(0xFFFFFFFF)

    /** 묶음 영역 background_primary / surface_low = basic_50 */
    val SurfaceLow = Color(0xFFF7F8FA)

    /** 칩·눌림 배경 surface_high = basic_100 */
    val SurfaceHigh = Color(0xFFEDEEF2)

    /** 구분선 basic_200 */
    val Divider = Color(0xFFE0E1E6)

    /** 칩 테두리 basic_300 */
    val Border = Color(0xFFC7C9CE)

    /** 비활성 글자 basic_400 */
    val TextDisabled = Color(0xFFA4A8AE)

    /** 글자 text_dark_primary */
    val Text = Color(0xFF131417)

    /** 보조 글자 static_dark_200 */
    val TextSecondary = Color(0xFF383A3D)

    /** 흐린 글자 static_dark_300 */
    val TextMuted = Color(0xFF6E6F72)

    /** 주 버튼 button_filled_enabled = basic_600 */
    val ButtonFilled = Color(0xFF44464E)

    /** 주 버튼 눌림 */
    val ButtonFilledPressed = Color(0xFF313236)

    /** 보조 버튼 button_basic_enabled(근사) */
    val ButtonBasic = Color(0xFFF2F3F5)

    /** 선택 상태(세그먼트·스위치) */
    val Selected = Color(0xFF131417)

    /** 활성·완료 informative_positive */
    val Positive = Color(0xFF0064FF)

    /** 정차·성공 informative_active */
    val Active = Color(0xFF02C265)

    /** 주의 점 informative_negative */
    val Negative = Color(0xFFFE3D16)
}

/**
 * Pleos 기준 서체 Asta Sans(SIL OFL 1.1, 라이선스 assets/licenses/AstaSans-OFL.txt).
 * 가변 서체 하나(wght 300~800)를 굵기마다 wght 축 값으로 불러 토큰 굵기(400·500·600·700·800)가 그대로 적용되게 한다.
 */
@OptIn(ExperimentalTextApi::class) // FontVariation.Settings: 가변 서체 축 지정(API 26+에서 동작, minSdk 28)
private val Family = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        Font(
            resId = R.font.asta_sans,
            weight = FontWeight(weight),
            variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
        )
    },
)

/**
 * 글자 단계(Pleos Typography: Headline·Title·Body·Label). 160dpi라 1sp = 1px.
 * 색은 넣지 않는다(LocalContentColor를 따르게: 버튼 안 글자는 버튼 색, 그 밖은 onSurface = #131417). */
private fun style(size: Int, line: Int, weight: FontWeight) =
    TextStyle(fontFamily = Family, fontSize = size.sp, lineHeight = line.sp, fontWeight = weight, lineBreak = KoreanLineBreak)

/**
 * 한국어 줄바꿈: 어절(띄어쓰기) 단위로 끊는다("맞추/고"처럼 낱말 가운데서 끊지 않는다). Phrase는 API 33 이상에서만 동작하고
 * 그 아래에서는 기본 줄바꿈이다. 글자에 따로 크기를 준 Text도 LocalTextStyle(bodyLarge)을 물려받아 함께 적용된다.
 */
private val KoreanLineBreak = LineBreak(
    strategy = LineBreak.Strategy.HighQuality,
    strictness = LineBreak.Strictness.Normal,
    wordBreak = LineBreak.WordBreak.Phrase,
)

val PleosTypography = Typography(
    displayLarge = style(96, 112, FontWeight.Bold),
    headlineLarge = style(60, 80, FontWeight.ExtraBold),
    headlineMedium = style(56, 72, FontWeight.ExtraBold),
    headlineSmall = style(48, 64, FontWeight.Bold),
    titleLarge = style(40, 52, FontWeight.Bold),
    titleMedium = style(36, 44, FontWeight.Bold),
    titleSmall = style(32, 40, FontWeight.Bold),
    bodyLarge = style(30, 38, FontWeight.Normal),
    bodyMedium = style(28, 36, FontWeight.Normal),
    bodySmall = style(26, 34, FontWeight.Normal),
    labelLarge = style(24, 32, FontWeight.SemiBold),
    labelMedium = style(22, 30, FontWeight.SemiBold),
    labelSmall = style(20, 28, FontWeight.SemiBold),
)

/** 버튼 모서리 20~24, 세그먼트 12·16. 그림자는 차 위 조작 아이콘에만 쓴다. */
val PleosShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

private val ColorScheme = lightColorScheme(
    primary = PleosColors.ButtonFilled,
    onPrimary = PleosColors.Surface,
    secondary = PleosColors.Positive,
    onSecondary = PleosColors.Surface,
    tertiary = PleosColors.Active,
    background = PleosColors.Surface,
    onBackground = PleosColors.Text,
    surface = PleosColors.Surface,
    onSurface = PleosColors.Text,
    surfaceVariant = PleosColors.SurfaceLow,
    onSurfaceVariant = PleosColors.TextMuted,
    outline = PleosColors.Divider,
    outlineVariant = PleosColors.Border,
    error = PleosColors.Negative,
)

/** Pleos 라이트 테마. 차량 화면은 라이트 테마 기준으로 설계했다(다크 테마는 이후 단계). */
@Composable
fun PleosAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ColorScheme, typography = PleosTypography, shapes = PleosShapes, content = content)
}
