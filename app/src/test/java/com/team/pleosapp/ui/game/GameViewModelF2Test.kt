package com.team.pleosapp.ui.game

import com.team.pleosapp.data.climate.FakeClimateRepository
import com.team.pleosapp.data.drive.FakeDriveInputRepository
import com.team.pleosapp.data.driving.DrivingRestrictions
import com.team.pleosapp.data.fake.FakeAutoPlayer
import com.team.pleosapp.data.light.FakeCabinLightRepository
import com.team.pleosapp.data.record.FakeRecordStore
import com.team.pleosapp.data.seat.FakeSeatRepository
import com.team.pleosapp.data.settings.FakeSettingsRepository
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.domain.puzzle.BoardLength
import com.team.pleosapp.domain.puzzle.Clock
import com.team.pleosapp.domain.puzzle.FakeClock
import com.team.pleosapp.domain.puzzle.JudgeState
import com.team.pleosapp.domain.puzzle.Seeds
import com.team.pleosapp.domain.puzzle.SessionStatus
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** F2-ui: 덮어쓰기 확인, 시연 0단계 경고, S2 힌트, S3 복구 체크리스트, Fake 자동 풀이, B1 재개 수정. */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelF2Test {
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

    private fun newViewModel(demoStageCheck: Boolean = false, testClock: Clock = clock, autoPlay: Boolean = false) = GameViewModel(
        climate, light, drive, seats, records, settings, testClock,
        newSeed = { "test-seed" },
        tickIntervalMs = 0,
        autoPlayer = if (autoPlay) FakeAutoPlayer(climate, light, drive) else null,
        demoStageCheck = demoStageCheck,
    )

    private val listeners: Int get() = climate.activeCollectors + light.activeCollectors + drive.activeCollectors

    private fun playingDemo(vm: GameViewModel = newViewModel()): GameViewModel {
        vm.setDebugSeed(Seeds.DEMO)
        vm.onRestrictions(parked)
        vm.selectLength(BoardLength.FIVE)
        vm.startNew()
        assertEquals(Screen.READY, vm.state.value.screen)
        vm.confirmParked()
        assertEquals(Screen.PLAYING, vm.state.value.screen)
        return vm
    }

    /** 시연 판 ①~④를 푼다(D1 → L1 보라 → L3 2열 오른쪽 → C1 22℃). */
    private fun solveDemoContent() {
        drive.setSteeringAngle(-45f)
        clock.advance(1_500)
        drive.setSteeringAngle(0f)
        light.setAmbientColor(0x00AA00FF)
        light.setReadingLight(SeatPosition.REAR_RIGHT, true)
        climate.setTargetTemperature(22f)
    }

    private fun pressBrakeTwice() {
        clock.advance(500)
        drive.setBrakePressed(true)
        clock.advance(1_000)
        drive.setBrakePressed(false)
        clock.advance(1_000)
        drive.setBrakePressed(true)
    }

    // ---- 새 판 덮어쓰기 확인 --------------------------------------------------------------------

    @Test
    fun startWithSavedBoard_asksBeforeOverwriting() = runTest(dispatcher) {
        val vm = playingDemo()
        vm.onRestrictions(driving) // 저장(PAUSED_BY_GATE)
        vm.onRestrictions(parked)
        vm.goHome()
        assertEquals(Screen.HOME, vm.state.value.screen)
        assertNotNull(vm.state.value.saved)

        vm.startNew()
        assertTrue(vm.state.value.confirmOverwrite)
        assertEquals(Screen.HOME, vm.state.value.screen)
        vm.cancelOverwrite() // 취소하면 홈에 남고 저장된 판도 그대로
        assertFalse(vm.state.value.confirmOverwrite)
        assertEquals(Screen.HOME, vm.state.value.screen)
        assertEquals(SessionStatus.PAUSED_BY_GATE, records.loadSession()!!.status)

        vm.startNew()
        vm.onRestrictions(driving) // 대화상자는 정차 중에만
        assertFalse(vm.state.value.confirmOverwrite)
        vm.confirmOverwrite()
        assertEquals(Screen.HOME, vm.state.value.screen)

        vm.onRestrictions(parked)
        vm.startNew()
        vm.confirmOverwrite()
        assertEquals(Screen.READY, vm.state.value.screen)
        assertFalse(vm.state.value.ready!!.resume)
        vm.goHome() // S1에서 돌아오면 저장된 판은 남는다
        assertNotNull(vm.state.value.saved)
        assertEquals(SessionStatus.PAUSED_BY_GATE, records.loadSession()!!.status)

        vm.startNew()
        vm.confirmOverwrite()
        vm.confirmParked() // 새 판이 시작되면 저장된 판이 바뀐다
        assertEquals(Screen.PLAYING, vm.state.value.screen)
        val saved = records.loadSession()!!
        assertEquals(SessionStatus.PLAYING, saved.status)
        assertEquals(0, saved.currentIndex)
        assertNull(vm.state.value.saved)
    }

    @Test
    fun noSavedBoard_startsWithoutAsking() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.onRestrictions(parked)
        vm.startNew()
        assertFalse(vm.state.value.confirmOverwrite)
        assertEquals(Screen.READY, vm.state.value.screen)
    }

    // ---- 시연 0단계 경고 ----------------------------------------------------------------------

    @Test
    fun demoWarning_onlyInDemoModeWhenStageDiffers() = runTest(dispatcher) {
        val vm = newViewModel(demoStageCheck = true)
        vm.setDebugSeed(Seeds.DEMO)
        vm.onRestrictions(parked)
        vm.startNew() // Fake 기본값 = 0단계
        assertNull(vm.state.value.demoWarning)
        assertEquals(Screen.READY, vm.state.value.screen)
        vm.goHome()

        climate.setTargetTemperature(22f)
        light.setAmbientColor(FakeCabinLightRepository.WHITE)
        vm.startNew()
        assertEquals(listOf("운전석 온도", "앰비언트 색"), vm.state.value.demoWarning!!.map { it.item })
        assertEquals(Screen.HOME, vm.state.value.screen)
        vm.cancelDemoStart()
        assertNull(vm.state.value.demoWarning)
        assertEquals(Screen.HOME, vm.state.value.screen)

        vm.startNew()
        vm.onRestrictions(driving) // 정차 중에만
        assertNull(vm.state.value.demoWarning)
        vm.onRestrictions(parked)
        vm.startNew()
        vm.confirmDemoStart() // 그대로 시작
        assertEquals(Screen.READY, vm.state.value.screen)
        vm.goHome()

        vm.setDebugSeed(null) // 시드를 고정하지 않으면 묻지 않는다
        vm.startNew()
        assertNull(vm.state.value.demoWarning)
        assertEquals(Screen.READY, vm.state.value.screen)
    }

    @Test
    fun demoWarning_offInRelease() = runTest(dispatcher) {
        climate.setTargetTemperature(22f)
        val vm = newViewModel(demoStageCheck = false)
        vm.setDebugSeed(Seeds.DEMO)
        vm.onRestrictions(parked)
        vm.startNew()
        assertNull(vm.state.value.demoWarning)
        assertEquals(Screen.READY, vm.state.value.screen)
    }

    // ---- S2 ----------------------------------------------------------------------------------

    @Test
    fun hints_upToTwo_resetOnNextPuzzle() = runTest(dispatcher) {
        val vm = playingDemo()
        assertEquals(0, vm.state.value.play!!.hintsShown)
        vm.showHint()
        assertEquals(1, vm.state.value.play!!.hintsShown)
        vm.showHint()
        vm.showHint()
        assertEquals(2, vm.state.value.play!!.hintsShown)
        assertEquals(
            listOf("조타 키는 핸들이에요. 칸에 1초 머문 뒤 가운데로 돌려요", "핸들을 살짝 돌리면 바늘이 움직여요. 칸 안에서 1초 기다려요"),
            vm.state.value.play!!.clue.hints,
        )
        drive.setSteeringAngle(-45f)
        clock.advance(1_500)
        drive.setSteeringAngle(0f)
        assertEquals(2, vm.state.value.play!!.number)
        assertEquals(0, vm.state.value.play!!.hintsShown)
        // 암호형: 목표를 드러내지 않고 암호표를 보여 준다
        val play = vm.state.value.play!!
        assertFalse(play.showsTarget)
        assertNull((play.gauge as Gauge.AmbientColor).target)
        assertEquals(3, play.cipherTable.size)
        assertEquals("조타 키 왼쪽 2번", play.lastSolved)
        assertEquals(1, play.restoredCount)
    }

    @Test
    fun hints_resetForNewBoard() = runTest(dispatcher) {
        val vm = playingDemo()
        vm.showHint()
        vm.showHint()
        solveDemoContent()
        pressBrakeTwice()
        assertEquals(Screen.RESULT, vm.state.value.screen)
        vm.newBoard()
        vm.confirmParked()
        assertEquals(1, vm.state.value.play!!.number)
        assertEquals(0, vm.state.value.play!!.hintsShown) // 앞 판 1번 퍼즐의 힌트가 남지 않는다(리허설 관찰)
    }

    @Test
    fun gauge_followsListenerValues() = runTest(dispatcher) {
        val vm = playingDemo()
        assertEquals(0f, (vm.state.value.play!!.gauge as Gauge.Steering).angle)
        drive.setSteeringAngle(-30f)
        assertEquals(-30f, (vm.state.value.play!!.gauge as Gauge.Steering).angle)
        assertEquals(SignalId.D1, vm.state.value.play!!.puzzle.signal)
    }

    // ---- B1 알려진 문제 ----------------------------------------------------------------------

    @Test
    fun resumeWhilePressed_staleReleaseIsNotAPress() = runTest(dispatcher) {
        val vm = playingDemo()
        solveDemoContent()
        assertEquals(5, vm.state.value.play!!.number)
        vm.onRestrictions(driving)
        drive.setBrakePressed(true) // 밟은 채 P
        vm.onRestrictions(parked)
        vm.resumeSaved()
        vm.confirmParked()
        // 리스너가 낡은 뗌을 주고 곧바로 밟음을 준다(에뮬레이터 실측 0~1ms)
        drive.setBrakePressed(false)
        clock.advance(1)
        drive.setBrakePressed(true)
        assertEquals(JudgeState.Progress(0, 2), vm.state.value.play!!.judge)
        clock.advance(1_000)
        drive.setBrakePressed(false)
        clock.advance(1_000)
        drive.setBrakePressed(true)
        assertEquals(JudgeState.Progress(1, 2), vm.state.value.play!!.judge)
        assertEquals(1, vm.state.value.play!!.brake!!.count)
    }

    // ---- S3 복구 체크리스트 ------------------------------------------------------------------

    @Test
    fun result_checklistAutoChecks_onlyWhileParked() = runTest(dispatcher) {
        val vm = playingDemo()
        solveDemoContent()
        pressBrakeTwice()
        assertEquals(Screen.RESULT, vm.state.value.screen)
        val result = vm.state.value.result!!
        assertEquals(listOf(SignalId.D1, SignalId.L1, SignalId.L3, SignalId.C1), result.checklist.map { it.key.signal })
        assertEquals(listOf(true, false, false, false), result.checklist.map { it.restored }) // 핸들은 이미 가운데
        assertEquals(5, result.lengthMinutes)
        assertEquals(5, result.solvedCount)

        climate.setTargetTemperature(24f)
        assertTrue(vm.state.value.result!!.checklist.first { it.key.signal == SignalId.C1 }.restored)

        vm.onRestrictions(driving) // 정차가 아니면 리스너 해제
        assertEquals(0, listeners)
        light.setAmbientColor(FakeCabinLightRepository.CYAN)
        assertFalse(vm.state.value.result!!.checklist.first { it.key.signal == SignalId.L1 }.restored)

        vm.onRestrictions(parked) // 돌아오면 다시 읽어 갱신
        assertTrue(vm.state.value.result!!.checklist.first { it.key.signal == SignalId.L1 }.restored)
        assertTrue(listeners > 0)
        light.setReadingLight(SeatPosition.REAR_RIGHT, false)
        assertEquals(4, vm.state.value.result!!.restoredCount)

        vm.onAppStopped() // 앱을 떠나도 해제
        assertEquals(0, listeners)
        vm.onAppStarted()
        assertTrue(listeners > 0)

        vm.newBoard() // 새 판은 시드 없이
        assertNull(vm.state.value.demoSeed)
        assertEquals(Screen.READY, vm.state.value.screen)
        assertEquals(0, listeners)
    }

    @Test
    fun result_previousBestShown() = runTest(dispatcher) {
        records.recordEscape(5, 999_000)
        val vm = playingDemo()
        solveDemoContent()
        pressBrakeTwice()
        val result = vm.state.value.result!!
        assertTrue(result.newBest)
        assertEquals(999_000L, result.previousBestMs)
    }

    // ---- Fake 자동 풀이 ------------------------------------------------------------------------

    @Test
    fun autoPlay_solvesWholeBoardAndRestores() = runTest(dispatcher) {
        val vm = newViewModel(testClock = Clock { testScheduler.currentTime }, autoPlay = true)
        playingDemo(vm)
        advanceTimeBy(5_900)
        assertEquals(1, vm.state.value.play!!.number) // 6초 전에는 그대로
        advanceTimeBy(60_000)
        runCurrent()
        assertEquals(Screen.RESULT, vm.state.value.screen)
        assertTrue(vm.state.value.result!!.escaped)
        advanceTimeBy(7_000) // 결과 화면 6초 뒤 원래 값으로
        runCurrent()
        val result = vm.state.value.result!!
        assertEquals(result.checklist.size, result.restoredCount)
        vm.goHome()
    }

    @Test
    fun autoPlay_cancelledByGateClose() = runTest(dispatcher) {
        val vm = newViewModel(testClock = Clock { testScheduler.currentTime }, autoPlay = true)
        playingDemo(vm)
        advanceTimeBy(3_000)
        vm.onRestrictions(driving)
        advanceTimeBy(10_000)
        assertEquals(0f, drive.readSteeringAngle()) // 멈춘 동안은 신호를 바꾸지 않는다
        assertEquals(Screen.PAUSED, vm.state.value.screen)
        vm.onRestrictions(parked)
        vm.goHome()
    }
}
