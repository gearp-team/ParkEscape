package com.team.pleosapp.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.team.pleosapp.R
import com.team.pleosapp.data.driving.DrivingRestrictions
import com.team.pleosapp.data.navi.NaviLocation
import com.team.pleosapp.ui.theme.PleosAppTheme

@Composable
fun HomeRoute(viewModel: HomeViewModel, isUsingFakeSdk: Boolean) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, isUsingFakeSdk = isUsingFakeSdk, onRefreshLocation = viewModel::refreshLocation)
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    isUsingFakeSdk: Boolean,
    onRefreshLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        // 1 window(portrait)·2 window(landscape) 모두 대응: 화면 방향이 아니라 앱 창의 실제 크기로 배치를 고른다.
        BoxWithConstraints(Modifier.fillMaxSize().padding(32.dp)) {
            val isWide = maxWidth > maxHeight
            Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                StatusRow(state.restrictions, isUsingFakeSdk)
                if (isWide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        BatteryCard(state.evStateOfCharge, Modifier.weight(1f))
                        LocationCard(state.location, state.restrictions, onRefreshLocation, Modifier.weight(1f))
                    }
                } else {
                    BatteryCard(state.evStateOfCharge, Modifier.fillMaxWidth())
                    LocationCard(state.location, state.restrictions, onRefreshLocation, Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun StatusRow(restrictions: DrivingRestrictions, isUsingFakeSdk: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val drivingLabel = if (restrictions.requiresDistractionOptimization) {
            R.string.driving_state_restricted
        } else {
            R.string.driving_state_parked
        }
        StatusBadge(
            text = stringResource(drivingLabel),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        if (isUsingFakeSdk) {
            StatusBadge(
                text = stringResource(R.string.fake_sdk_badge),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun StatusBadge(text: String, containerColor: Color, contentColor: Color) {
    Surface(color = containerColor, contentColor = contentColor, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun BatteryCard(stateOfCharge: Int?, modifier: Modifier) {
    InfoCard(title = stringResource(R.string.battery_title), modifier = modifier) {
        Text(
            text = stateOfCharge?.let { stringResource(R.string.battery_value, it) }
                ?: stringResource(R.string.value_unknown),
            style = MaterialTheme.typography.displayMedium,
        )
    }
}

@Composable
private fun LocationCard(
    location: NaviLocation?,
    restrictions: DrivingRestrictions,
    onRefresh: () -> Unit,
    modifier: Modifier,
) {
    InfoCard(title = stringResource(R.string.location_title), modifier = modifier) {
        if (location == null) {
            Text(stringResource(R.string.value_unknown), style = MaterialTheme.typography.headlineMedium)
        } else {
            Text(
                text = stringResource(R.string.location_coordinates, location.latitude, location.longitude),
                style = MaterialTheme.typography.headlineSmall,
            )
            // 주행 중 콘텐츠 제한(LIMIT_CONTENT)이면 긴 텍스트를 숨긴다.
            if (!restrictions.limitContent) {
                Text(
                    text = location.address,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Button(onClick = onRefresh, modifier = Modifier.heightIn(min = 64.dp)) {
            Text(stringResource(R.string.location_refresh), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun InfoCard(title: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = modifier) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Preview(name = "Landscape", widthDp = 1280, heightDp = 720)
@Preview(name = "Portrait", widthDp = 720, heightDp = 1280)
@Composable
private fun HomeScreenPreview() {
    PleosAppTheme {
        HomeScreen(
            state = HomeUiState(
                evStateOfCharge = 82,
                location = NaviLocation(37.4979, 127.0276, "서울 강남구 강남대로"),
            ),
            isUsingFakeSdk = true,
            onRefreshLocation = {},
        )
    }
}
