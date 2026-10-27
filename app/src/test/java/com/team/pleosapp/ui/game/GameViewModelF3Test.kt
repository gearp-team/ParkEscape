package com.team.pleosapp.ui.game

import com.team.pleosapp.data.climate.FakeClimateRepository
import com.team.pleosapp.data.drive.FakeDriveInputRepository
import com.team.pleosapp.data.driving.DrivingRestrictions
import com.team.pleosapp.data.light.CabinLightSupport
import com.team.pleosapp.data.light.FakeCabinLightRepository
import com.team.pleosapp.data.record.FakeRecordStore
import com.team.pleosapp.data.seat.FakeSeatRepository
import com.team.pleosapp.data.settings.FakeSettingsRepository
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.data.signal.SignalCapability
import com.team.pleosapp.domain.clue.ClueWriter
import com.team.pleosapp.domain.puzzle.CipherMeaning
import com.team.pleosapp.domain.puzzle.ClueType
import com.team.pleosapp.domain.puzzle.FakeClock
import com.team.pleosapp.domain.puzzle.Fixtures
import com.team.pleosapp.domain.puzzle.JudgeState
import com.team.pleosapp.domain.puzzle.PuzzleJudge
import com.team.pleosapp.domain.puzzle.Seeds
import com.team.pleosapp.domain.puzzle.SignalId
import com.team.pleosapp.domain.puzzle.Target
import com.team.pleosapp.domain.puzzle.VehicleSignals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

/** F3: 좌석도 참가자 선택과 자리 단서(기획 2절 F3, 2-1 L3). */
@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelF3Test {
    private val dispatcher = UnconfinedTestDispatcher()
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

    /** 한 대의 차(Fake 저장소 묶음)와 ViewModel. */
    private class Car(
        seatRaw: Map<SeatPosition, Boolean?> = FakeSeatRepository.DEFAULT_RAW,
        lightSupport: CabinLightSupport = FakeCabinLightRepository.DEFAULT_SUPPORT,
        seed: String = "f3-seed",
    ) {
        val clock = FakeClock()
        val climate = FakeClimateRepository()
        val light = FakeCabinLightRepository(support = lightSupport)
        val drive = FakeDriveInputRepository()
        val seats = FakeSeatRepository(raw = seatRaw)
        val records = FakeRecordStore()
        val vm = GameViewModel(climate, light, drive, seats, records, FakeSettingsRepository(), clock, newSeed = { seed }, tickIntervalMs = 0)

        val ready: ReadyState get() = vm.state.value.ready!!
        val selected: Set<SeatPosition> get() = ready.seats.filter { it.selected }.map { it.seat }.toSet()
    }

    private fun lightSupport(readingSeats: List<SeatPosition>?): CabinLightSupport =
        FakeCabinLightRepository.DEFAULT_SUPPORT.copy(
            readingLight = if (readingSeats == null) SignalCapability.unsupported("none") else SignalCapability(true, readingSeats.map { setOf(it) }),
        )

    @Test
    fun seatsWithoutReadingLight_areNotSelectable_evenIfOccupied() = runTest(dispatcher) {
        // 점유: 운전석 + 2열 오른쪽(Fake 기본). 독서등은 운전석·조수석에만 있다
        val car = Car(lightSupport = lightSupport(listOf(SeatPosition.DRIVER, SeatPosition.PASSENGER)))
        car.vm.onRestrictions(parked)
        car.vm.startNew()
        val rearRight = car.ready.seats.single { it.seat == SeatPosition.REAR_RIGHT }
        assertFalse(rearRight.selectable)
        assertFalse(rearRight.selected) // 앉아 있어도 자리 단서를 낼 수 없는 좌석은 초기 선택에 넣지 않는다
        assertTrue(car.ready.seats.single { it.seat == SeatPosition.PASSENGER }.selectable)
        assertFalse(car.ready.seats.single { it.seat == SeatPosition.REAR_LEFT }.selectable)
        car.vm.toggleSeat(SeatPosition.REAR_RIGHT) // 눌러도 바뀌지 않는다
        assertEquals(setOf(SeatPosition.DRIVER), car.selected)
        car.vm.toggleSeat(SeatPosition.PASSENGER)
        assertEquals(setOf(SeatPosition.DRIVER, SeatPosition.PASSENGER), car.selected)
        assertTrue(car.ready.passengerSeatsAvailable)
    }

    @Test
    fun noReadingLightAtAll_driverOnly_noSeatClue() = runTest(dispatcher) {
        val car = Car(lightSupport = lightSupport(null))
        car.vm.onRestrictions(parked)
        car.vm.startNew()
        assertFalse(car.ready.passengerSeatsAvailable) // 화면: "운전석 1명으로 해요"
        assertEquals(1, car.ready.participantCount)
        assertTrue(car.ready.seats.none { it.selectable })
        car.vm.confirmParked()
        assertEquals(Screen.PLAYING, car.vm.state.value.screen)
        val saved = car.records.loadSession()!!
        assertEquals(setOf(SeatPosition.DRIVER), saved.participants)
        assertTrue(saved.board.puzzles.none { it.signal == SignalId.L3 })
    }

    @Test
    fun driverIsAlwaysSelected_evenWhenPolarityUnknown() = runTest(dispatcher) {
        // 운전석 원시값 null: 극성을 알 수 없으므로 초기 선택은 운전석만(다른 좌석 값은 쓰지 않는다)
        val car = Car(seatRaw = mapOf(SeatPosition.DRIVER to null, SeatPosition.PASSENGER to true, SeatPosition.REAR_RIGHT to true))
        car.vm.onRestrictions(parked)
        car.vm.startNew()
        assertEquals(setOf(SeatPosition.DRIVER), car.selected)
        car.vm.toggleSeat(SeatPosition.DRIVER)
        assertEquals(setOf(SeatPosition.DRIVER), car.selected)
        // 2열 가운데는 선택지에 없다
        assertEquals(listOf(SeatPosition.DRIVER, SeatPosition.PASSENGER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT), car.ready.seats.map { it.seat })
    }

    @Test
    fun invertedPolarity_driverFalse_flipsEverySeat() = runTest(dispatcher) {
        // 에뮬레이터: VC "점유" = SDK false. 운전석 false → 뒤집어 조수석(원시 false)만 점유로 본다
        val car = Car(
            seatRaw = mapOf(
                SeatPosition.DRIVER to false,
                SeatPosition.PASSENGER to false,
                SeatPosition.REAR_LEFT to true,
                SeatPosition.REAR_CENTER to true,
                SeatPosition.REAR_RIGHT to true,
            ),
        )
        car.vm.onRestrictions(parked)
        car.vm.startNew()
        assertEquals(setOf(SeatPosition.DRIVER, SeatPosition.PASSENGER), car.selected)
    }

    @Test
    fun tapOnly_givesSameBoardAsDetectedSeats() = runTest(dispatcher) {
        // 점유 신호로 2열 오른쪽이 잡힌 차
        val detected = Car()
        detected.vm.onRestrictions(parked)
        detected.vm.startNew()
        assertEquals(setOf(SeatPosition.DRIVER, SeatPosition.REAR_RIGHT), detected.selected)
        detected.vm.confirmParked()

        // 점유 신호가 없는 차(운전석 값 모름): 좌석도 탭만으로 고른다
        val tapped = Car(seatRaw = emptyMap())
        tapped.vm.onRestrictions(parked)
        tapped.vm.startNew()
        assertEquals(setOf(SeatPosition.DRIVER), tapped.selected)
        tapped.vm.toggleSeat(SeatPosition.REAR_RIGHT)
        tapped.vm.confirmParked()

        assertEquals(detected.records.loadSession()!!.board, tapped.records.loadSession()!!.board)
        assertEquals(detected.records.loadSession()!!.participants, tapped.records.loadSession()!!.participants)
    }

    @Test
    fun occupancyChangeDuringBoard_keepsBoard_nextBoardUsesNewValue() = runTest(dispatcher) {
        val car = Car()
        car.vm.onRestrictions(parked)
        car.vm.startNew()
        car.vm.confirmParked()
        val board = car.records.loadSession()!!.board
        assertEquals(setOf(SeatPosition.DRIVER, SeatPosition.REAR_RIGHT), car.records.loadSession()!!.participants)

        car.seats.setSeat(SeatPosition.PASSENGER, true) // 판 도중 조수석에 사람이 앉음
        car.vm.onRestrictions(driving)
        car.vm.onRestrictions(parked)
        car.vm.resumeSaved()
        assertTrue(car.ready.resume)
        assertEquals(setOf(SeatPosition.DRIVER, SeatPosition.REAR_RIGHT), car.selected) // 이어하는 판은 그대로
        car.vm.confirmParked()
        assertEquals(board, car.records.loadSession()!!.board)

        car.vm.onRestrictions(driving)
        car.vm.onRestrictions(parked)
        car.vm.goHome()
        car.vm.startNew()
        car.vm.confirmOverwrite()
        assertFalse(car.ready.resume)
        assertEquals(setOf(SeatPosition.DRIVER, SeatPosition.PASSENGER, SeatPosition.REAR_RIGHT), car.selected) // 다음 판부터 반영
    }

    @Test
    fun seatClue_judgedOnlyByThatSeatsReadingLight() = runTest(dispatcher) {
        val car = Car()
        car.vm.setDebugSeed(Seeds.DEMO)
        car.vm.onRestrictions(parked)
        car.vm.startNew()
        car.vm.confirmParked()
        // demo5 ① D1 → ② L1 → ③ L3 2열 오른쪽
        car.drive.setSteeringAngle(-45f)
        car.clock.advance(1_500)
        car.drive.setSteeringAngle(0f)
        car.light.setAmbientColor(0x00AA00FF)
        val play = car.vm.state.value.play!!
        assertEquals(Target.ReadingLight(SeatPosition.REAR_RIGHT), play.puzzle.target)
        assertEquals("2열 오른쪽 승무원: 독서등으로 신호", play.clue.text)

        car.light.setReadingLight(SeatPosition.PASSENGER, true) // 다른 자리
        car.light.setReadingLight(SeatPosition.REAR_LEFT, true)
        car.light.setReadingLight(SeatPosition.DRIVER, true)
        assertEquals(3, car.vm.state.value.play!!.number)
        car.light.setReadingLight(SeatPosition.REAR_RIGHT, true)
        assertEquals(4, car.vm.state.value.play!!.number)
    }

    @Test
    fun driverOnlyParticipants_neverGetSeatClue() = runTest(dispatcher) {
        for (i in 1..30) {
            val car = Car(seatRaw = mapOf(SeatPosition.DRIVER to true), seed = "solo-$i")
            car.vm.onRestrictions(parked)
            car.vm.startNew()
            assertEquals(setOf(SeatPosition.DRIVER), car.selected)
            car.vm.confirmParked()
            assertTrue(car.records.loadSession()!!.board.puzzles.none { it.signal == SignalId.L3 })
        }
    }

    // ---- F3 QA 실패 1: 멈춘 동안 이벤트가 오지 않아 공유 값이 낡는 경우(Fake dropEvents) ----------------------------

    /** 재현 A: 멈춘 동안 켠 독서등·바꾼 색이 재개 뒤 판정에 반영된다(재개 때 다시 읽은 값이 공유 상태에 들어간다). */
    @Test
    fun pausedChanges_reachJudgeAfterResume() = runTest(dispatcher) {
        val car = Car()
        car.vm.setDebugSeed(Seeds.DEMO)
        car.vm.onRestrictions(parked)
        car.vm.startNew()
        car.vm.confirmParked()
        assertEquals(1, car.vm.state.value.play!!.number) // ① D1

        car.vm.onRestrictions(driving)
        car.light.dropEvents = true // 멈춘 동안 독서등·앰비언트 이벤트가 오지 않는다(에뮬레이터 실측)
        car.light.setReadingLight(SeatPosition.REAR_RIGHT, true)
        car.light.setAmbientColor(0x00AA00FF)
        car.vm.onRestrictions(parked)
        car.vm.resumeSaved()
        car.vm.confirmParked()
        car.light.dropEvents = false // 재개해 리스너를 다시 걸면 이벤트가 다시 온다

        car.drive.setSteeringAngle(-45f)
        car.clock.advance(1_500)
        car.drive.setSteeringAngle(0f)
        // ② L1(이미 보라) → ③ L3(이미 켜짐)이 바로 풀리고 ④ C1
        assertEquals(4, car.vm.state.value.play!!.number)
        assertEquals(true, car.vm.state.value.play!!.signals!!.readingLights[SeatPosition.REAR_RIGHT])
    }

    /** 재현 B: 앞 판에서 켰다가 멈춘 동안 끈 독서등의 낡은 값 때문에 새 판의 자리 단서가 저절로 풀리지 않는다. */
    @Test
    fun staleSharedValue_doesNotSolveNewBoardSeatClue() = runTest(dispatcher) {
        // 첫 퍼즐이 2열 오른쪽 자리 단서인 시드를 찾는다(참가자 운전석 + 2열 오른쪽, Fake 기본값)
        val seed = (1..300).map { "l3-first-$it" }.first { candidate ->
            val probe = Car(seed = candidate)
            probe.vm.onRestrictions(parked)
            probe.vm.startNew()
            probe.vm.confirmParked()
            probe.vm.state.value.play!!.puzzle.target == Target.ReadingLight(SeatPosition.REAR_RIGHT)
        }
        val car = Car(seed = seed)
        car.vm.onRestrictions(parked)
        car.vm.startNew()
        car.vm.confirmParked()
        car.light.setReadingLight(SeatPosition.REAR_RIGHT, true)
        assertEquals(2, car.vm.state.value.play!!.number)

        car.vm.onRestrictions(driving)
        car.light.dropEvents = true
        car.light.setReadingLight(SeatPosition.REAR_RIGHT, false) // 멈춘 동안 끔(이벤트 없음)
        car.vm.onRestrictions(parked)
        car.vm.goHome()
        car.vm.startNew()
        car.vm.confirmOverwrite()
        car.vm.confirmParked()
        val play = car.vm.state.value.play!!
        assertEquals(Target.ReadingLight(SeatPosition.REAR_RIGHT), play.puzzle.target) // 같은 시드라 같은 판
        assertEquals(1, play.number) // 저절로 풀리지 않는다
        assertEquals(false, play.signals!!.readingLights[SeatPosition.REAR_RIGHT]) // 게이지도 지금 값(꺼짐)
    }

    /** 결과 화면 재진입: 주행 동안 되돌린 값이 정차 뒤 체크리스트에 반영된다. */
    @Test
    fun resultChecklist_usesFreshValuesAfterGateReopens() = runTest(dispatcher) {
        val car = Car()
        car.vm.setDebugSeed(Seeds.DEMO)
        car.vm.onRestrictions(parked)
        car.vm.startNew()
        car.vm.confirmParked()
        car.drive.setSteeringAngle(-45f)
        car.clock.advance(1_500)
        car.drive.setSteeringAngle(0f)
        car.light.setAmbientColor(0x00AA00FF)
        car.light.setReadingLight(SeatPosition.REAR_RIGHT, true)
        car.climate.setTargetTemperature(22f)
        car.clock.advance(500)
        car.drive.setBrakePressed(true)
        car.clock.advance(1_000)
        car.drive.setBrakePressed(false)
        car.clock.advance(1_000)
        car.drive.setBrakePressed(true)
        assertEquals(Screen.RESULT, car.vm.state.value.screen)

        car.vm.onRestrictions(driving)
        car.light.dropEvents = true
        car.light.setAmbientColor(FakeCabinLightRepository.CYAN)
        car.light.setReadingLight(SeatPosition.REAR_RIGHT, false)
        car.vm.onRestrictions(parked)
        val rows = car.vm.state.value.result!!.checklist.associate { it.key.signal to it.restored }
        assertEquals(true, rows[SignalId.L1])
        assertEquals(true, rows[SignalId.L3])
    }

    /** 암호형 자리 단서: 단서에는 기호만 나오고, 암호표가 가리키는 좌석의 독서등으로만 풀린다. 게이지도 목표 좌석을 드러내지 않는다. */
    @Test
    fun cipherSeatClue_decodedSeatSolves_decoysDoNot() {
        val participants = setOf(SeatPosition.DRIVER, SeatPosition.PASSENGER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT)
        var checked = 0
        for (i in 1..400) {
            val board = Fixtures.board(Fixtures.input("cipher-l3-$i", minutes = 10, participants = participants))
            val puzzle = board.puzzles.firstOrNull { it.signal == SignalId.L3 && it.type == ClueType.CIPHER } ?: continue
            checked++
            val seat = (puzzle.target as Target.ReadingLight).seat
            val entry = board.cipherTable.single { it.symbol == puzzle.cipherSymbol }
            assertEquals(CipherMeaning.Seat(seat), entry.meaning) // 암호표의 뜻 = 목표 좌석
            val decoys = board.cipherTable.mapNotNull { (it.meaning as? CipherMeaning.Seat)?.seat }.filter { it != seat }
            assertTrue(decoys.isNotEmpty())
            val clue = ClueWriter.clue(board, puzzle.index)
            assertFalse(clue.text.contains(ClueWriter.seatName(seat)))
            assertTrue(clue.text.contains(puzzle.cipherSymbol!!.glyph))
            assertNull((Gauges.of(puzzle, null, DisplayBounds(), null, null, null) as Gauge.ReadingLights).target)

            val judge = PuzzleJudge(puzzle)
            decoys.forEach { judge.update(VehicleSignals(readingLights = mapOf(it to true)), 0) }
            assertEquals(JudgeState.Waiting, judge.current)
            assertEquals(JudgeState.Solved, judge.update(VehicleSignals(readingLights = decoys.associateWith { true } + (seat to true)), 1))
        }
        assertTrue("checked=$checked", checked >= 20)
        assertNotNull(checked)
    }
}
