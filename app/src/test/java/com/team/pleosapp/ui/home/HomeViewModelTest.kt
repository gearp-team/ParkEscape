package com.team.pleosapp.ui.home

import android.car.drivingstate.CarUxRestrictions
import com.team.pleosapp.data.driving.DrivingRestrictions
import com.team.pleosapp.data.driving.FakeDrivingStateRepository
import com.team.pleosapp.data.navi.FakeNaviRepository
import com.team.pleosapp.data.navi.NaviLocation
import com.team.pleosapp.data.vehicle.FakeVehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val vehicle = FakeVehicleRepository(stateOfCharge = 55)
    private val navi = FakeNaviRepository(NaviLocation(37.0, 127.0, "테스트 주소"))
    private val driving = FakeDrivingStateRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_combinesAllRepositories() = runTest(dispatcher) {
        val viewModel = HomeViewModel(vehicle, navi, driving)
        backgroundScope.launch { viewModel.uiState.collect {} }

        val state = viewModel.uiState.value
        assertEquals(55, state.evStateOfCharge)
        assertEquals("테스트 주소", state.location?.address)
        assertEquals(DrivingRestrictions(), state.restrictions)
    }

    @Test
    fun uiState_followsRepositoryUpdates() = runTest(dispatcher) {
        val viewModel = HomeViewModel(vehicle, navi, driving)
        backgroundScope.launch { viewModel.uiState.collect {} }

        vehicle.setStateOfCharge(null)
        driving.setRestrictions(
            DrivingRestrictions(true, CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT),
        )

        val state = viewModel.uiState.value
        assertNull(state.evStateOfCharge)
        assertTrue(state.restrictions.limitContent)
    }

    @Test
    fun refreshLocation_asksNaviRepository() {
        val viewModel = HomeViewModel(vehicle, navi, driving)

        viewModel.refreshLocation()

        assertEquals(1, navi.refreshCount)
    }
}
