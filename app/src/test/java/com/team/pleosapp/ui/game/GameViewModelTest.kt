package com.team.pleosapp.ui.game

import com.team.pleosapp.data.climate.FakeClimateRepository
import com.team.pleosapp.data.drive.FakeDriveInputRepository
import com.team.pleosapp.data.driving.DrivingRestrictions
import com.team.pleosapp.data.light.FakeCabinLightRepository
import com.team.pleosapp.data.record.FakeRecordStore
import com.team.pleosapp.data.seat.FakeSeatRepository
import com.team.pleosapp.data.settings.FakeSettingsRepository
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.domain.puzzle.BoardLength
import com.team.pleosapp.domain.puzzle.FakeClock
import com.team.pleosapp.domain.puzzle.GameSession
import com.team.pleosapp.domain.puzzle.JudgeState
import com.team.pleosapp.domain.puzzle.Seeds
import com.team.pleosapp.domain.puzzle.SessionStatus
import com.team.pleosapp.domain.puzzle.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val clock = FakeClock()
    private val climate = FakeClimateRepository()
    private val light = FakeCabinLightRepository()
    private val drive = FakeDriveInputRepository()
    private val seats = FakeSeatRepository()
    private val records = FakeRecordStore()
    private val settings = FakeSettingsRepository()
    private val logs = mutableListOf<String>()

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

    private fun newViewModel(seed: String = "test-seed") = GameViewModel(
        climate, light, drive, seats, records, settings, clock,
        newSeed = { seed },
        log = { logs += it },
        tickIntervalMs = 0,
    )

    private val listeners: Int get() = climate.activeCollectors + light.activeCollectors + drive.activeCollectors

    /** 정차 → 5분 판 → S1 → P 확인까지 진행한 ViewModel. */
    private fun playing(vm: GameViewModel = newViewModel()): GameViewModel {
        vm.onRestrictions(parked)
        vm.selectLength(BoardLength.FIVE)
        vm.startNew()
        assertEquals(Screen.READY, vm.state.value.screen)
        vm.confirmParked()
        assertEquals(Screen.PLAYING, vm.state.value.screen)
        return vm
    }

    // ---- 게이트 -------------------------------------------------------------------------------

    @Test
    fun gateUnknownOrClosed_blocksStartAndConfirm() = runTest(dispatcher) {
        val vm = newViewModel()
        assertEquals(GateState.UNKNOWN, vm.state.value.gate)
        assertFalse(vm.state.value.canStart)
        vm.startNew()
        assertEquals(Screen.HOME, vm.state.value.screen)

        vm.onRestrictions(driving) // 공회전·주행
        assertEquals(GateState.CLOSED, vm.state.value.gate)
        assertFalse(vm.state.value.canStart)

        vm.onRestrictions(DrivingRestrictions.FullyRestricted) // 값 모름
        assertFalse(vm.state.value.canStart)

        vm.onRestrictions(parked)
        assertTrue(vm.state.value.canStart)
        vm.startNew()
        vm.onRestrictions(driving) // S1에서 닫히면 P 확인이 눌리지 않는다
        vm.confirmParked()
        assertEquals(Screen.READY, vm.state.value.screen)
        assertNull(records.loadSession())
    }

    @Test
    fun gateClosed_closesSettingsAndShowsDrivingNoticeOnlyInFlagBuild() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.onRestrictions(parked)
        vm.openSettings()
        assertTrue(vm.state.value.settingsOpen)
        assertFalse(vm.state.value.showsDrivingNotice(drivingNoticeBuild = true))

        vm.onRestrictions(driving)
        assertFalse(vm.state.value.settingsOpen) // 주행 중 설정 패널이 남지 않는다
        assertTrue(vm.state.value.showsDrivingNotice(drivingNoticeBuild = true))
        assertFalse(vm.state.value.showsDrivingNotice(drivingNoticeBuild = false)) // 기본 빌드는 시스템 차단 화면에 맡긴다
        vm.openSettings()
        assertFalse(vm.state.value.settingsOpen) // 닫힌 게이트에서는 열리지 않는다

        vm.onRestrictions(DrivingRestrictions.FullyRestricted) // 값 모름도 닫힘
        assertTrue(vm.state.value.showsDrivingNotice(drivingNoticeBuild = true))
        vm.onRestrictions(parked)
        assertFalse(vm.state.value.showsDrivingNotice(drivingNoticeBuild = true))
        assertFalse(vm.state.value.settingsOpen) // 정차해도 패널이 저절로 다시 열리지 않는다
    }

    @Test
    fun gateClosedWhilePlaying_pausesSavesAndReleasesListeners() = runTest(dispatcher) {
        val vm = playing()
        assertTrue("listeners registered", listeners > 0)
        clock.advance(12_000)

        vm.onRestrictions(driving)

        val st = vm.state.value
        assertEquals(Screen.PAUSED, st.screen)
        assertNull(st.play)
        assertEquals(0, listeners) // 리스너 해제
        val saved = records.loadSession()!!
        assertEquals(SessionStatus.PAUSED_BY_GATE, saved.status)
        assertEquals(12_000L, saved.elapsedMs)
        assertEquals(1, saved.gatePauses)
        assertTrue(logs.any { it.startsWith("callback gate closed") })
        assertTrue(logs.any { it.startsWith("listeners released reason=gate closed") })
        assertTrue(logs.any { it.startsWith("saved reason=gate closed") })

        clock.advance(60_000) // 주행 중: 타이머 정지
        assertEquals(12_000L, records.loadSession()!!.elapsedMs)
        assertNotNull(st.saved)
        assertEquals(5 * 60_000L - 12_000L, st.saved!!.remainingMs)
    }

    @Test
    fun pausedByGate_resumesOnlyWithOpenGateAndParkConfirm() = runTest(dispatcher) {
        val vm = playing()
        clock.advance(5_000)
        vm.onRestrictions(driving)

        vm.resumeSaved() // 게이트 닫힘: 이어하기 안 됨
        assertEquals(Screen.PAUSED, vm.state.value.screen)
        vm.goHome() // 닫힌 동안 홈도 안 됨
        assertEquals(Screen.PAUSED, vm.state.value.screen)

        vm.onRestrictions(parked) // 게이트만 열려서는 재개하지 않는다
        assertEquals(Screen.PAUSED, vm.state.value.screen)
        assertEquals(0, listeners)

        vm.resumeSaved()
        val ready = vm.state.value.ready!!
        assertEquals(Screen.READY, vm.state.value.screen)
        assertTrue(ready.resume)
        assertEquals(0, listeners)

        vm.confirmParked()
        assertEquals(Screen.PLAYING, vm.state.value.screen)
        assertTrue(listeners > 0)
        assertEquals(1, vm.state.value.play!!.number) // 같은 퍼즐
        clock.advance(1_000)
        vm.onRestrictions(driving)
        assertEquals(6_000L, records.loadSession()!!.elapsedMs) // 같은 남은 시간에서 이어짐
    }

    @Test
    fun appStopped_pausesByAppAndAsksParkConfirm() = runTest(dispatcher) {
        val vm = playing()
        clock.advance(3_000)
        vm.onAppStopped()

        assertEquals(Screen.READY, vm.state.value.screen) // 돌아오면 S1 P 확인을 바로 띄운다
        assertTrue(vm.state.value.ready!!.resume)
        assertEquals(0, listeners)
        assertEquals(SessionStatus.PAUSED_BY_APP, records.loadSession()!!.status)

        vm.onRestrictions(driving) // 떠나 있는 동안 주행이었다면 P 확인이 막힌다
        vm.confirmParked()
        assertEquals(Screen.READY, vm.state.value.screen)
        vm.onRestrictions(parked)
        vm.confirmParked()
        assertEquals(Screen.PLAYING, vm.state.value.screen)
    }

    @Test
    fun onStopOutsidePlay_doesNothing() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.onRestrictions(parked)
        vm.onAppStopped()
        assertEquals(Screen.HOME, vm.state.value.screen)
        assertNull(records.loadSession())
    }

    // ---- 복원 ---------------------------------------------------------------------------------

    @Test
    fun relaunch_restoresSavedSessionToHomeResume() = runTest(dispatcher) {
        val first = playing()
        clock.advance(20_000)
        first.onRestrictions(driving) // 저장됨(PAUSED_BY_GATE)

        // 앱을 완전히 껐다 켬: 같은 저장소로 새 ViewModel
        val second = newViewModel()
        val st = second.state.value
        assertEquals(Screen.HOME, st.screen)
        assertNotNull(st.saved)
        assertEquals(5, st.saved!!.lengthMinutes)
        assertEquals(5 * 60_000L - 20_000L, st.saved!!.remainingMs)
        assertFalse(st.canResume) // 게이트 값 전

        second.onRestrictions(parked)
        second.resumeSaved()
        second.confirmParked()
        assertEquals(Screen.PLAYING, second.state.value.screen)
    }

    @Test
    fun relaunchFromPlayingSave_becomesPausedByApp() = runTest(dispatcher) {
        playing()
        clock.advance(4_000)
        // PLAYING 중 저장(퍼즐 판정 시점 등)만 남고 프로세스가 죽은 경우
        val saved = records.loadSession()!!
        assertEquals(SessionStatus.PLAYING, saved.status)
        assertEquals(SessionStatus.PAUSED_BY_APP, GameSession.restore(saved, clock).status)
        val vm = newViewModel()
        assertNotNull(vm.state.value.saved)
        assertFalse(vm.state.value.saved!!.pausedByGate)
    }

    // ---- 판 길이 활성 --------------------------------------------------------------------------

    @Test
    fun availableLengths_followSupportAndEvSetting() = runTest(dispatcher) {
        val vm = newViewModel()
        assertTrue(vm.state.value.supportLoaded)
        assertEquals(BoardLength.entries.toSet(), vm.state.value.availableLengths)

        // 공조 3종 + 브레이크만(비EV) → 3분만
        val climateOnly = GameViewModel(
            climate,
            FakeCabinLightRepository(support = FakeCabinLightRepository.DEFAULT_SUPPORT.copy(
                ambientColor = SignalCapability.unsupported("x"),
                ambientBrightness = SignalCapability.unsupported("x"),
                readingLight = SignalCapability.unsupported("x"),
            )),
            FakeDriveInputRepository(support = FakeDriveInputRepository.DEFAULT_SUPPORT.copy(steeringAngle = SignalCapability.unsupported("x"))),
            seats, records, FakeSettingsRepository(), clock, tickIntervalMs = 0,
        )
        assertEquals(setOf(BoardLength.THREE), climateOnly.state.value.availableLengths)
        assertEquals(BoardLength.THREE, climateOnly.state.value.selectedLength) // 고른 길이도 가능한 길이로
        climateOnly.selectLength(BoardLength.TEN)
        assertEquals(BoardLength.THREE, climateOnly.state.value.selectedLength)

        climateOnly.setEv(true) // EV: 공조 종류당 2 → 3·5분
        assertEquals(setOf(BoardLength.THREE, BoardLength.FIVE), climateOnly.state.value.availableLengths)
    }

    // ---- S1 좌석·공회전 안내 -------------------------------------------------------------------

    @Test
    fun ready_usesDetectedSeatsAsInitialAndAllowsToggle() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.onRestrictions(parked)
        vm.startNew()
        val ready = vm.state.value.ready!!
        // FakeSeatRepository 기본: 운전석 + 2열 오른쪽
        assertEquals(setOf(SeatPosition.DRIVER, SeatPosition.REAR_RIGHT), ready.seats.filter { it.selected }.map { it.seat }.toSet())
        assertFalse(ready.seats.single { it.seat == SeatPosition.DRIVER }.selectable)
        assertTrue(ready.showIdleNotice) // 비EV, 다시 보지 않기 전

        vm.toggleSeat(SeatPosition.REAR_RIGHT)
        vm.toggleSeat(SeatPosition.PASSENGER)
        vm.toggleSeat(SeatPosition.DRIVER) // 운전석은 바꿀 수 없다
        assertEquals(setOf(SeatPosition.DRIVER, SeatPosition.PASSENGER), vm.state.value.ready!!.seats.filter { it.selected }.map { it.seat }.toSet())

        vm.setDontShowIdleAgain(true)
        vm.confirmParked()
        assertTrue(settings.settings.first().idleNoticeDismissed)
        assertEquals(setOf(SeatPosition.DRIVER, SeatPosition.PASSENGER), records.loadSession()!!.participants)
    }

    @Test
    fun idleNotice_hiddenForEvOrDismissed() = runTest(dispatcher) {
        settings.setEv(true)
        val vm = newViewModel()
        vm.onRestrictions(parked)
        vm.startNew()
        assertFalse(vm.state.value.ready!!.showIdleNotice)
    }

    // ---- 진행: 핸들 재확인·탈출 ----------------------------------------------------------------

    @Test
    fun demoBoard_playsToEscape_withSteeringReread() = runTest(dispatcher) {
        // 시연 판: ① D1 왼쪽 2번 → ② L1 보라(◇) → ③ L3 2열 오른쪽 → ④ C1 22℃ → ⑤ B1 두 번
        val vm = newViewModel()
        vm.setDebugSeed(Seeds.DEMO)
        playing(vm)
        assertEquals(Target.Steering::class, vm.state.value.play!!.puzzle.target::class)

        drive.setSteeringAngle(-45f)
        clock.advance(1_500)
        drive.setSteeringAngle(0f) // 콜백 → 재확인(readSteeringAngle = 0) → 통과
        assertEquals(2, vm.state.value.play!!.number)

        light.setAmbientColor(0x00AA00FF)
        assertEquals(3, vm.state.value.play!!.number)
        light.setReadingLight(SeatPosition.REAR_RIGHT, true)
        assertEquals(4, vm.state.value.play!!.number)
        climate.setTargetTemperature(22f)
        assertEquals(5, vm.state.value.play!!.number)

        clock.advance(500) // 뗀 상태가 150ms 이상 이어진 뒤 밟는다
        drive.setBrakePressed(true)
        clock.advance(1_000)
        drive.setBrakePressed(false)
        clock.advance(1_000)
        drive.setBrakePressed(true)
        assertEquals(Screen.RESULT, vm.state.value.screen)
        assertTrue(vm.state.value.result!!.escaped)
        assertTrue(vm.state.value.result!!.newBest)
        assertNull(vm.state.value.result!!.previousBestMs)
        assertEquals(0, drive.activeBrakeCollectors) // 판 리스너는 해제, 결과 화면은 B1을 구독하지 않는다
        assertTrue(listeners > 0) // S3 복구 체크리스트 리스너
        assertNull(records.loadSession()) // 끝난 판은 지운다
        assertEquals(mapOf(5 to vm.state.value.result!!.elapsedMs), vm.state.value.bestTimes)

        vm.goHome()
        assertEquals(Screen.HOME, vm.state.value.screen)
        assertNull(vm.state.value.saved)
        assertEquals(0, listeners)
    }

    @Test
    fun resume_readsBrakeAsStartState() = runTest(dispatcher) {
        val vm = newViewModel()
        vm.setDebugSeed(Seeds.DEMO)
        playing(vm)
        drive.setSteeringAngle(-45f)
        clock.advance(1_500)
        drive.setSteeringAngle(0f)
        light.setAmbientColor(0x00AA00FF)
        light.setReadingLight(SeatPosition.REAR_RIGHT, true)
        climate.setTargetTemperature(22f)
        assertEquals(5, vm.state.value.play!!.number) // 봉인

        vm.onRestrictions(driving)
        drive.setBrakePressed(true) // 주행에서 돌아와 밟은 채 P
        vm.onRestrictions(parked)
        vm.resumeSaved()
        vm.confirmParked()
        assertEquals(JudgeState.Progress(0, 2), vm.state.value.play!!.judge)
    }
}
