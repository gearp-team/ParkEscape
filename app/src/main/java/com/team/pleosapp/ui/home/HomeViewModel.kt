package com.team.pleosapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.team.pleosapp.AppContainer
import com.team.pleosapp.data.driving.DrivingRestrictions
import com.team.pleosapp.data.driving.DrivingStateRepository
import com.team.pleosapp.data.navi.NaviLocation
import com.team.pleosapp.data.navi.NaviRepository
import com.team.pleosapp.data.vehicle.VehicleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val evStateOfCharge: Int? = null,
    val location: NaviLocation? = null,
    val restrictions: DrivingRestrictions = DrivingRestrictions(),
)

class HomeViewModel(
    vehicleRepository: VehicleRepository,
    private val naviRepository: NaviRepository,
    drivingStateRepository: DrivingStateRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        vehicleRepository.evStateOfCharge.onStart { emit(null) },
        naviRepository.currentLocation,
        drivingStateRepository.restrictions,
    ) { stateOfCharge, location, restrictions ->
        HomeUiState(stateOfCharge, location, restrictions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshLocation() = naviRepository.refreshCurrentLocation()

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    container.vehicleRepository,
                    container.naviRepository,
                    container.drivingStateRepository,
                )
            }
        }
    }
}
