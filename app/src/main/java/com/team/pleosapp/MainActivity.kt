package com.team.pleosapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.team.pleosapp.ui.home.HomeRoute
import com.team.pleosapp.ui.home.HomeViewModel
import com.team.pleosapp.ui.theme.PleosAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as PleosApplication).container
        setContent {
            PleosAppTheme {
                val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(container))
                HomeRoute(viewModel, isUsingFakeSdk = container.isUsingFakeSdk)
            }
        }
    }
}
