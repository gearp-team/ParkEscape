package com.team.pleosapp.ui.game

import com.team.pleosapp.data.climate.FakeClimateRepository
import com.team.pleosapp.data.drive.FakeDriveInputRepository
import com.team.pleosapp.data.driving.DrivingRestrictions
import com.team.pleosapp.data.fake.FakeAutoPlayer
import com.team.pleosapp.data.light.FakeCabinLightRepository
import com.team.pleosapp.data.record.FakeRecordStore
import com.team.pleosapp.data.seat.FakeSeatRepository
import com.team.pleosapp.data.settings.FakeSettingsRepository
import com.team.pleosapp.domain.clue.SafetyTips
import com.team.pleosapp.domain.puzzle.BoardLength
import com.team.pleosapp.domain.puzzle.Clock
import com.team.pleosapp.domain.puzzle.FakeClock
import com.team.pleosapp.domain.puzzle.Seeds
import com.team.pleosapp.domain.puzzle.SignalId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 시연 시드 demo-safety(L4 → D1 → L1 → C4 → B1)로 C4·L4 판정·복구와 "실제 상황 팁" 카드의 표시 조건을 확인한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelSafetyTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = FakeClock()
    private val climate = FakeClimateRepository()
    private val light = FakeCabinLightRepository()
    private val drive = FakeDriveInputRepository()
    private val seats = FakeSeatRepository()
    private val records = FakeRecordStore()
    private val settings = FakeSettingsRepository()

    private val parked = DrivingRestrictions()
    private val driving = DrivingRestrictions(requiresDistractionOptimization = true, activeRestrictions = 0x50)

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun newViewModel(testClock: Clock = clock, autoPlay: Boolean = false, demoStageCheck: Boolean = false) = GameViewModel(
        climate, light, drive, seats, records, settings, testClock,
        newSeed = { "test-seed" },
        tickIntervalMs = 0,
        autoPlayer = if (autoPlay) FakeAutoPlayer(climate, light, drive) else null,
        demoStageCheck = demoStageCheck,
    )

    private fun playingSafety(vm: GameViewModel = newViewModel()): GameViewModel {
        vm.setDebugSeed(Seeds.DEMO_SAFETY)
        vm.onRestrictions(parked)
        vm.selectLength(BoardLength.FIVE)
        vm.startNew()
        vm.confirmParked()
        assertEquals(Screen.PLAYING, vm.state.value.screen)
        assertEquals(listOf(SignalId.L4, SignalId.D1, SignalId.L1, SignalId.C4, SignalId.B1), vm.state.value.play!!.board.puzzles.map { it.signal })
        return vm
    }

    private fun solveSteering() {
        drive.setSteeringAngle(-45f)
        clock.advance(1_500)
        drive.setSteeringAngle(0f)
    }

    private fun solveAmbientPurple() = light.setAmbientColor(0x00AA00FF)

    private fun pressBrakeTwice() {
        clock.advance(500)
        drive.setBrakePressed(true)
        clock.advance(1_000)
        drive.setBrakePressed(false)
        clock.advance(1_000)
        drive.setBrakePressed(true)
    }

    @Test
    fun lowBeamSolve_showsTipForFourSeconds_thenHides() = runTest(dispatcher) {
        val vm = playingSafety()
        assertNull(vm.state.value.visibleTip)
        val first = vm.state.value.play!!
        assertTrue(first.showsTarget)
        assertEquals(Gauge.Switch(SignalId.L4, false, true), first.gauge)

        light.setLowBeam(true)
        assertEquals(2, vm.state.value.play!!.number)
        assertEquals(TipCard(SignalId.L4, SafetyTips.of(SignalId.L4)!!), vm.state.value.visibleTip)
        advanceTimeBy(3_900)
        assertTrue(vm.state.value.visibleTip != null)
        advanceTimeBy(200)
        assertNull(vm.state.value.tip)
        vm.onRestrictions(driving)
        vm.onRestrictions(parked)
        vm.goHome()
    }

    @Test
    fun puzzleWithoutTip_showsNothing() = runTest(dispatcher) {
        val vm = playingSafety()
        light.setLowBeam(true)
        advanceTimeBy(SafetyTips.SHOW_MS + 100)
        solveSteering() // D1은 팁 없음
        assertEquals(3, vm.state.value.play!!.number)
        assertNull(vm.state.value.tip)

        // ③ 암호형 L1: 목표를 드러내지 않고 판 전체 암호표(5칸)를 보여 준다
        val ambient = vm.state.value.play!!
        assertFalse(ambient.showsTarget)
        assertNull((ambient.gauge as Gauge.AmbientColor).target)
        assertEquals(5, ambient.cipherTable.size)
        solveAmbientPurple() // L1도 팁 없음
        assertEquals(4, vm.state.value.play!!.number)
        assertNull(vm.state.value.tip)

        // ④ 암호형 C4: 목표를 드러내지 않는다. 켜면 제상 팁
        val defrost = vm.state.value.play!!
        assertFalse(defrost.showsTarget)
        assertEquals(Gauge.Switch(SignalId.C4, false, null), defrost.gauge)
        climate.setDefroster(true)
        assertEquals(TipCard(SignalId.C4, SafetyTips.of(SignalId.C4)!!), vm.state.value.visibleTip)
        assertEquals(5, vm.state.value.play!!.number)
        advanceTimeBy(SafetyTips.SHOW_MS + 100)
        assertNull(vm.state.value.tip)
        vm.onRestrictions(driving)
        vm.onRestrictions(parked)
        vm.goHome()
    }

    @Test
    fun newTipReplacesOld_andRestartsTimer() = runTest(dispatcher) {
        val vm = playingSafety()
        climate.setDefroster(true) // ④를 미리 맞춰 둔다
        light.setLowBeam(true)
        assertEquals(SignalId.L4, vm.state.value.visibleTip!!.signal)
        advanceTimeBy(2_000)
        solveSteering()
        solveAmbientPurple() // ③이 풀리며 미리 켠 ④도 함께 풀린다 → 제상 팁이 전조등 팁을 바로 바꾼다
        assertEquals(5, vm.state.value.play!!.number)
        assertEquals(SignalId.C4, vm.state.value.visibleTip!!.signal)
        advanceTimeBy(2_500)
        assertEquals(SignalId.C4, vm.state.value.visibleTip!!.signal) // 새 팁은 다시 4초
        advanceTimeBy(1_600)
        assertNull(vm.state.value.tip)
        vm.onRestrictions(driving)
        vm.onRestrictions(parked)
        vm.goHome()
    }

    @Test
    fun gateClosed_hidesTipAndDoesNotBringItBack() = runTest(dispatcher) {
        val vm = playingSafety()
        light.setLowBeam(true)
        assertTrue(vm.state.value.visibleTip != null)
        vm.onRestrictions(driving) // 주행으로 바뀌면 바로 내린다
        assertNull(vm.state.value.tip)
        assertNull(vm.state.value.visibleTip)
        vm.onRestrictions(parked)
        assertNull(vm.state.value.visibleTip) // 정차로 돌아와도 다시 띄우지 않는다
        vm.goHome()
    }

    @Test
    fun visibleTip_onlyWhenParkedOnPlayOrResult() {
        val tip = TipCard(SignalId.C4, "x")
        val base = GameUiState(gate = GateState.OPEN, tip = tip)
        assertEquals(tip, base.copy(screen = Screen.PLAYING).visibleTip)
        assertEquals(tip, base.copy(screen = Screen.RESULT).visibleTip)
        assertNull(base.copy(screen = Screen.PAUSED).visibleTip)
        assertNull(base.copy(screen = Screen.HOME).visibleTip)
        assertNull(base.copy(screen = Screen.PLAYING, gate = GateState.CLOSED).visibleTip)
        assertNull(base.copy(screen = Screen.RESULT, gate = GateState.UNKNOWN).visibleTip)
    }

    @Test
    fun escapeRightAfterTip_keepsTipOnResult_andChecklistRestoresSwitches() = runTest(dispatcher) {
        val vm = playingSafety()
        light.setLowBeam(true)
        solveSteering()
        solveAmbientPurple()
        climate.setDefroster(true)
        assertEquals(5, vm.state.value.play!!.number)
        pressBrakeTwice()
        assertEquals(Screen.RESULT, vm.state.value.screen)
        assertEquals(SignalId.C4, vm.state.value.visibleTip!!.signal) // 탈출 직전 팁이 결과 화면에 남는다
        advanceTimeBy(SafetyTips.SHOW_MS + 100)
        assertNull(vm.state.value.visibleTip)

        val rows = vm.state.value.result!!.rows
        assertEquals(listOf(SignalId.L4, SignalId.D1, SignalId.L1, SignalId.C4), rows.map { it.signal })
        assertEquals(listOf(false, true, false, false), rows.map { it.restored })
        light.setLowBeam(false)
        climate.setDefroster(false)
        light.setAmbientColor(FakeCabinLightRepository.CYAN)
        assertTrue(vm.state.value.result!!.rows.all { it.restored })
        vm.goHome()
    }

    @Test
    fun demoStageCheck_warnsWhenSafetySignalsLeftOn() = runTest(dispatcher) {
        light.setLowBeam(true)
        climate.setDefroster(true)
        val vm = newViewModel(demoStageCheck = true)
        vm.setDebugSeed(Seeds.DEMO_SAFETY)
        vm.onRestrictions(parked)
        vm.startNew()
        assertEquals(listOf("앞유리 제상", "전조등"), vm.state.value.demoWarning!!.map { it.item })
        vm.cancelDemoStart()
    }

    @Test
    fun autoPlay_solvesSafetyBoardAndRestores() = runTest(dispatcher) {
        val vm = newViewModel(testClock = Clock { testScheduler.currentTime }, autoPlay = true)
        playingSafety(vm)
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(Screen.RESULT, vm.state.value.screen)
        assertTrue(vm.state.value.result!!.escaped)
        advanceTimeBy(7_000)
        runCurrent()
        val result = vm.state.value.result!!
        assertEquals(4, result.checklist.size)
        assertEquals(result.checklist.size, result.restoredCount)
        assertEquals(false, light.readLowBeam())
        assertEquals(false, climate.readDefroster())
        vm.goHome()
    }
}
