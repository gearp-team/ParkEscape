package com.team.pleosapp.ui.game

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.team.pleosapp.AppContainer
import com.team.pleosapp.BuildConfig
import com.team.pleosapp.data.climate.ClimateRepository
import com.team.pleosapp.data.drive.DriveInputRepository
import com.team.pleosapp.data.fake.AutoPlayer
import com.team.pleosapp.data.driving.DrivingRestrictions
import com.team.pleosapp.data.light.CabinLightRepository
import com.team.pleosapp.data.record.RecordStore
import com.team.pleosapp.data.seat.SeatRepository
import com.team.pleosapp.data.settings.AppSettings
import com.team.pleosapp.data.settings.SettingsRepository
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.domain.clue.Clue
import com.team.pleosapp.domain.clue.ClueWriter
import com.team.pleosapp.domain.puzzle.Board
import com.team.pleosapp.domain.puzzle.BoardLength
import com.team.pleosapp.domain.puzzle.BrakeProgress
import com.team.pleosapp.domain.puzzle.CipherEntry
import com.team.pleosapp.domain.puzzle.ClueType
import com.team.pleosapp.domain.puzzle.Clock
import com.team.pleosapp.domain.puzzle.GameSession
import com.team.pleosapp.domain.puzzle.GenerationResult
import com.team.pleosapp.domain.puzzle.GeneratorInput
import com.team.pleosapp.domain.puzzle.JudgeState
import com.team.pleosapp.domain.puzzle.Puzzle
import com.team.pleosapp.domain.puzzle.PuzzleGenerator
import com.team.pleosapp.domain.puzzle.RestoreItem
import com.team.pleosapp.domain.puzzle.Seeds
import com.team.pleosapp.domain.puzzle.SessionEvent
import com.team.pleosapp.domain.puzzle.SessionStatus
import com.team.pleosapp.domain.puzzle.SignalId
import com.team.pleosapp.domain.puzzle.SignalPool
import com.team.pleosapp.domain.puzzle.VehicleSignals
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** 화면(기획 4절). S0 홈, S1 준비, S2 진행, S-P 일시정지, S3 결과. */
enum class Screen { HOME, READY, PLAYING, PAUSED, RESULT }

/** 정차 게이트. 값을 아직 못 받았거나 모르면 닫힘과 같다. */
enum class GateState { UNKNOWN, OPEN, CLOSED }

/** 홈·준비 화면에 띄우는 안내. */
enum class Notice {
    /** 판을 만들 수 없었다(시작 직전에 조건이 바뀜) */
    BOARD_UNAVAILABLE,
}

/** 저장된 판 요약(S0 "저장된 판", S-P 문구). */
data class SavedSummary(
    val lengthMinutes: Int,
    val puzzleNumber: Int,
    val puzzleCount: Int,
    val remainingMs: Long,
    val pausedByGate: Boolean,
)

data class SeatChoice(val seat: SeatPosition, val selected: Boolean, val selectable: Boolean)

/** S1 준비. [resume]이면 판을 새로 만들지 않고 저장된 판을 이어한다(좌석은 바꿀 수 없다). */
data class ReadyState(
    val resume: Boolean,
    val lengthMinutes: Int,
    val puzzleCount: Int,
    val seats: List<SeatChoice>,
    val showIdleNotice: Boolean,
    val dontShowIdleAgain: Boolean = false,
    val summary: SavedSummary? = null,
) {
    val participantCount: Int get() = seats.count { it.selected }

    /** 고를 수 있는 동승 좌석(독서등 capability가 있는 좌석)이 하나라도 있는가. 없으면 운전석 1명이고 자리 단서가 없다. */
    val passengerSeatsAvailable: Boolean get() = seats.any { it.selectable }
}

/**
 * S2 진행(시안 Play-Direct-Land·Play-Cipher-Land·Play-Seal-Port). 단서 문장·게이지는 판 구조와 현재값에서 순수 함수로 만든다.
 * @property signals 리스너가 마지막으로 준 현재값(게이지). 재개 직후에는 get*으로 다시 읽은 값이다.
 * @property hintsShown 이 퍼즐에서 펼친 힌트 수(0~2). 퍼즐이 바뀌면 0이다.
 */
data class PlayState(
    val board: Board,
    val puzzle: Puzzle,
    val number: Int,
    val count: Int,
    val remainingMs: Long,
    val judge: JudgeState?,
    val signals: VehicleSignals?,
    val bounds: DisplayBounds,
    val hintsShown: Int,
    val canSkip: Boolean,
    val skipped: Set<Int>,
    val brake: BrakeProgress?,
    val steeringHoldMs: Long?,
) {
    val clue: Clue get() = ClueWriter.clue(board, puzzle.index, bounds.brightness)
    val gauge: Gauge get() = Gauges.of(puzzle, signals, bounds, judge, brake, steeringHoldMs)

    /** 지시형만 목표 구간을 보여 준다. */
    val showsTarget: Boolean get() = Gauges.showsTarget(puzzle)

    /** 암호형 퍼즐에서만 판의 암호표를 보여 준다. */
    val cipherTable: List<CipherEntry> get() = if (ClueWriter.effectiveType(puzzle) == ClueType.CIPHER) board.cipherTable else emptyList()

    /** 연쇄형의 앞 퍼즐을 건너뛰었으면 입력값을 그대로 보여 준다(힌트와 별도로 늘 보인다). */
    val chainReveal: String? get() = ClueWriter.chainInputReveal(board, puzzle.index, skipped)

    /** 풀린(건너뛰지 않은) 퍼즐 수. */
    val restoredCount: Int get() = (0 until puzzle.index).count { it !in skipped }

    /** 바로 앞 퍼즐의 짧은 결과("격벽 닫힘"). 첫 퍼즐이면 null. */
    val lastSolved: String? get() = board.puzzles.getOrNull(puzzle.index - 1)?.let { if (it.index in skipped) null else ClueWriter.solvedLabel(it) }
}

/**
 * S3 결과(시안 Result-Land·Result-Port).
 * @property previousBestMs 이 판 전의 같은 길이 최고 기록(없으면 null)
 * @property checklist 복구 체크리스트(원래 값·지금 값·자동 체크). 정차 중에만 리스너로 갱신한다.
 */
data class ResultState(
    val escaped: Boolean,
    val elapsedMs: Long,
    val newBest: Boolean,
    val skipped: Boolean,
    val lengthMinutes: Int = 0,
    val previousBestMs: Long? = null,
    val solvedCount: Int = 0,
    val puzzleCount: Int = 0,
    val checklist: List<RestoreItem> = emptyList(),
) {
    val rows: List<RestoreRow> get() = RestoreRows.of(checklist)
    val restoredCount: Int get() = checklist.count { it.restored }
}

data class GameUiState(
    val screen: Screen = Screen.HOME,
    val gate: GateState = GateState.UNKNOWN,
    val selectedLength: BoardLength = BoardLength.FIVE,
    val availableLengths: Set<BoardLength> = emptySet(),
    val supportLoaded: Boolean = false,
    val bestTimes: Map<Int, Long> = emptyMap(),
    val saved: SavedSummary? = null,
    val ready: ReadyState? = null,
    val play: PlayState? = null,
    val result: ResultState? = null,
    val settings: AppSettings = AppSettings(),
    val settingsOpen: Boolean = false,
    val notice: Notice? = null,
    val demoSeed: String? = null,
    /** 저장된 판이 있을 때 "시작"을 누르면 띄우는 덮어쓰기 확인(정차 중에만). */
    val confirmOverwrite: Boolean = false,
    /** debug 시연 모드로 시작할 때 0단계와 다른 현재값(있으면 그대로 시작할지 묻는다). */
    val demoWarning: List<DemoDiff>? = null,
) {
    val gateOpen: Boolean get() = gate == GateState.OPEN
    val canStart: Boolean get() = gateOpen && selectedLength in availableLengths
    val canResume: Boolean get() = gateOpen && saved != null

    /** 기능 플래그 pleos.drivingNotice 빌드(DO 선언)에서 게이트가 닫혀 있으면 모든 화면 대신 S-H(한 줄, 터치 요소 없음)를 그린다. */
    fun showsDrivingNotice(drivingNoticeBuild: Boolean): Boolean = drivingNoticeBuild && !gateOpen
}

/**
 * 판 하나의 흐름을 묶는 컨트롤러(기획 F1·2-2).
 *
 * - 게이트: [onRestrictions]에서 정차(requiresDO == false && 규제 0)를 판정한다. PLAYING 중에 닫히면 같은 순간
 *   PAUSED_BY_GATE 전환 → 신호 리스너 해제 → S-P 전환 → 저장을 한다(로그 태그 F1GATE로 시각을 남긴다).
 * - onStop(설정 변경으로 다시 만들어지는 경우 제외)은 PAUSED_BY_APP + 리스너 해제 + 저장이고, 돌아오면 S1 P 확인을 바로 띄운다.
 * - 두 일시정지 모두 "게이트 열림 + 이어하기 + S1 P 확인"으로만 재개한다. 재개하면 get*으로 현재값(브레이크 포함)을 다시 읽어
 *   판정기의 시작 상태로 넘긴 뒤 리스너를 다시 등록한다.
 * - 리스너는 PLAYING 동안만, 그 판에 쓰는 신호만 등록한다. 1초마다 tick()으로 시간 초과를 본다.
 * - 앱을 다시 켜면 저장된 판을 S0 "이어하기"로 복원한다. 창 크기 변경(액티비티 재생성)은 ViewModel이 그대로 이어 간다.
 */
class GameViewModel(
    private val climate: ClimateRepository,
    private val light: CabinLightRepository,
    private val drive: DriveInputRepository,
    private val seats: SeatRepository,
    private val records: RecordStore,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val newSeed: () -> String = { Seeds.random() },
    private val log: (String) -> Unit = {},
    /** 1초 tick 간격. 0이면 자동 tick을 하지 않는다(단위 테스트). */
    private val tickIntervalMs: Long = 1_000L,
    /** Fake 빌드의 자동 풀이(퍼즐마다 6초 뒤). 실제 SDK 빌드는 null. */
    private val autoPlayer: AutoPlayer? = null,
    /** debug 빌드에서만 켠다: 시연 모드 시작 때 0단계 확인 경고. */
    private val demoStageCheck: Boolean = false,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var session: GameSession? = null
    private var pool: SignalPool? = null
    private var signalJob: Job? = null
    private var tickJob: Job? = null
    private var autoJob: Job? = null
    private var autoPlayEnabled = true

    /** 게이지에 보일 현재값(리스너가 마지막으로 준 값). */
    private var currentSignals: VehicleSignals? = null

    /** (퍼즐 번호, 펼친 힌트 수). 퍼즐이 바뀌면 0부터. */
    private var hints = -1 to 0

    /** S3 복구 체크리스트를 갱신하는 끝난 판과 그 리스너. */
    private var resultSession: GameSession? = null
    private var resultJob: Job? = null

    init {
        viewModelScope.launch { records.bestTimes.collect { best -> _state.update { it.copy(bestTimes = best) } } }
        viewModelScope.launch {
            _state.update { it.copy(settings = settingsRepository.settings.first()) }
            restoreSaved()
            refreshAvailability()
            settingsRepository.settings.collect { s -> _state.update { it.copy(settings = s) } }
        }
    }

    // ---- 게이트·생명주기 ------------------------------------------------------------------------

    /** 규제 콜백(화면이 STARTED인 동안 MainActivity가 넘긴다). */
    fun onRestrictions(restrictions: DrivingRestrictions) {
        val open = restrictions.isParked
        // 닫히면 설정 패널도 닫는다(주행 중 설정 조작 금지. DO 플래그 빌드에서는 S-H가 화면을 대신한다)
        // 대화상자(덮어쓰기 확인·시연 경고)도 정차 중에만 뜬다
        _state.update {
            it.copy(
                gate = if (open) GateState.OPEN else GateState.CLOSED,
                settingsOpen = it.settingsOpen && open,
                confirmOverwrite = it.confirmOverwrite && open,
                demoWarning = it.demoWarning?.takeIf { open },
            )
        }
        if (_state.value.screen == Screen.RESULT) {
            // S3도 정차가 아니면 리스너를 해제하고, 정차로 돌아오면 다시 읽어 체크를 갱신한다
            if (open) resumeResult() else stopResultListening("gate closed")
            return
        }
        if (open) return
        val s = session ?: return
        if (s.status != SessionStatus.PLAYING) return
        log(
            "callback gate closed requiresDO=${restrictions.requiresDistractionOptimization} " +
                "active=0x${Integer.toHexString(restrictions.activeRestrictions)}",
        )
        s.onGateClosed()
        stopListening("gate closed")
        _state.update { it.copy(screen = Screen.PAUSED, play = null, saved = summary(s)) }
        log("paused status=${s.status} index=${s.state.currentIndex} elapsedMs=${s.state.elapsedMs}")
        save(s, "gate closed")
    }

    /** onStop(설정 변경이 아닐 때). 게이트가 열린 채 앱을 떠난 경우다. */
    fun onAppStopped() {
        if (_state.value.screen == Screen.RESULT) stopResultListening("app stopped")
        val s = session ?: return
        if (s.status != SessionStatus.PLAYING) return
        s.onAppStopped()
        stopListening("app stopped")
        _state.update { it.copy(screen = Screen.READY, play = null, ready = resumeReady(s), saved = summary(s)) }
        log("paused status=${s.status} index=${s.state.currentIndex}")
        save(s, "app stopped")
    }

    /** onStart: 홈에 있으면 판 길이 가능 여부를 다시 본다(차 상태가 바뀌었을 수 있다). */
    fun onAppStarted() {
        when (_state.value.screen) {
            Screen.HOME -> viewModelScope.launch { refreshAvailability() }
            Screen.RESULT -> resumeResult()
            else -> Unit
        }
    }

    // ---- S0 홈 ----------------------------------------------------------------------------------

    fun selectLength(length: BoardLength) {
        if (length in _state.value.availableLengths) _state.update { it.copy(selectedLength = length) }
    }

    fun openSettings() = _state.update { it.copy(settingsOpen = it.gateOpen) }

    fun closeSettings() = _state.update { it.copy(settingsOpen = false) }

    fun setEv(isEv: Boolean) {
        viewModelScope.launch {
            settingsRepository.setEv(isEv)
            _state.update { it.copy(settings = it.settings.copy(isEv = isEv)) }
            refreshAvailability()
        }
    }

    /** 공회전 안내 다시 보기. */
    fun resetIdleNotice() {
        viewModelScope.launch { settingsRepository.setIdleNoticeDismissed(false) }
    }

    /** 디버그 빌드의 고정 시드(시연 모드: 신호 풀 8종 고정). null이면 해제. */
    fun setDebugSeed(seed: String?) {
        _state.update { it.copy(demoSeed = seed) }
        viewModelScope.launch { refreshAvailability() }
    }

    fun dismissNotice() = _state.update { it.copy(notice = null) }

    /**
     * "N분 판 시작" → S1. 게이트가 열려 있고 고른 길이를 만들 수 있을 때만.
     * 저장된 판이 있으면 먼저 덮어쓰기를 확인한다(리더 결정). 확인하면 S1로 가고, 저장된 판은 새 판이 P 확인으로 시작될 때 지워진다
     * (S1에서 홈으로 돌아가면 그대로 남는다).
     */
    fun startNew() {
        val st = _state.value
        if (!st.canStart || st.screen != Screen.HOME) return
        if (session?.status?.isPaused == true) {
            _state.update { it.copy(confirmOverwrite = true) }
            return
        }
        checkDemoThenOpenReady()
    }

    fun confirmOverwrite() {
        if (!_state.value.confirmOverwrite) return
        _state.update { it.copy(confirmOverwrite = false) }
        if (_state.value.canStart) checkDemoThenOpenReady()
    }

    fun cancelOverwrite() = _state.update { it.copy(confirmOverwrite = false) }

    /** 시연 경고에서 "그대로 시작". */
    fun confirmDemoStart() {
        if (_state.value.demoWarning == null) return
        _state.update { it.copy(demoWarning = null) }
        if (_state.value.canStart) viewModelScope.launch { openReady() }
    }

    fun cancelDemoStart() = _state.update { it.copy(demoWarning = null) }

    /** Fake 빌드 자동 풀이 켜기·끄기(debug 인텐트). */
    fun setAutoPlay(enabled: Boolean) {
        autoPlayEnabled = enabled
        if (!enabled) autoJob?.cancel()
    }

    private fun checkDemoThenOpenReady() {
        viewModelScope.launch {
            if (demoStageCheck && _state.value.demoSeed != null) {
                val diffs = DemoStageCheck.differences(readAll(), seats.readOccupancy())
                if (diffs.isNotEmpty()) {
                    log("demo stage 0 differs: ${diffs.joinToString { "${it.item}=${it.current}" }}")
                    if (_state.value.gateOpen && _state.value.screen == Screen.HOME) _state.update { it.copy(demoWarning = diffs) }
                    return@launch
                }
            }
            openReady()
        }
    }

    private suspend fun openReady() {
        val st = _state.value
        if (!st.canStart || st.screen != Screen.HOME) return
        val selectable = selectableSeats()
        val detected = seats.readOccupancy().participants
        val initial = detected.filter { it in selectable }.toSet() + SeatPosition.DRIVER
        val length = st.selectedLength
        if (!_state.value.gateOpen || _state.value.screen != Screen.HOME) return // 읽는 사이 게이트가 닫힘
        _state.update {
            it.copy(
                screen = Screen.READY,
                notice = null,
                ready = ReadyState(
                    resume = false,
                    lengthMinutes = length.minutes,
                    puzzleCount = length.puzzleCount,
                    seats = seatChoices(initial, selectable),
                    showIdleNotice = !it.settings.isEv && !it.settings.idleNoticeDismissed,
                ),
            )
        }
    }

    /** S3 "새 판": 시드 없이(시연 시드도 풀고) 홈의 판 길이로 S1을 연다. */
    fun newBoard() {
        val st = _state.value
        if (st.screen != Screen.RESULT || !st.gateOpen) return
        stopResultListening("new board")
        resultSession = null
        _state.update { it.copy(screen = Screen.HOME, result = null, demoSeed = null) }
        viewModelScope.launch {
            refreshAvailability()
            startNew()
        }
    }

    /** S0·S-P "이어하기" → S1 P 확인. 게이트가 열려 있어야 한다. */
    fun resumeSaved() {
        val s = session ?: return
        if (!_state.value.gateOpen || !s.status.isPaused) return
        _state.update { it.copy(screen = Screen.READY, ready = resumeReady(s)) }
    }

    // ---- S1 준비 --------------------------------------------------------------------------------

    fun toggleSeat(seat: SeatPosition) {
        _state.update { st ->
            val ready = st.ready ?: return@update st
            if (ready.resume) return@update st
            st.copy(
                ready = ready.copy(
                    seats = ready.seats.map { if (it.seat == seat && it.selectable) it.copy(selected = !it.selected) else it },
                ),
            )
        }
    }

    fun setDontShowIdleAgain(checked: Boolean) {
        _state.update { st -> st.ready?.let { st.copy(ready = it.copy(dontShowIdleAgain = checked)) } ?: st }
    }

    /** "기어 P·주차 브레이크를 확인했어요". 게이트가 열려 있을 때만 판을 시작하거나 이어 간다. */
    fun confirmParked() {
        val st = _state.value
        val ready = st.ready ?: return
        if (!st.gateOpen || st.screen != Screen.READY) return
        viewModelScope.launch {
            if (ready.dontShowIdleAgain) settingsRepository.setIdleNoticeDismissed(true)
            if (ready.resume) resume() else startBoard(ready)
        }
    }

    /** S1·S-P "홈". 저장된 판은 그대로 남는다. */
    fun goHome() {
        val screen = _state.value.screen
        if (screen == Screen.PAUSED && !_state.value.gateOpen) return
        if (screen == Screen.PLAYING) return
        if (screen == Screen.RESULT) {
            stopResultListening("home")
            resultSession = null
        }
        _state.update { it.copy(screen = Screen.HOME, ready = null, result = null, saved = session?.let(::summary)) }
        viewModelScope.launch { refreshAvailability() }
    }

    // ---- S2 진행 --------------------------------------------------------------------------------

    /** 힌트 한 장 더(퍼즐마다 최대 2장: 단서 해설 → 조작 위치). */
    fun showHint() {
        val s = session ?: return
        if (s.status != SessionStatus.PLAYING) return
        val index = s.state.currentIndex
        val shown = if (hints.first == index) hints.second else 0
        hints = index to minOf(HINT_COUNT, shown + 1)
        refreshPlay()
    }

    fun skipPuzzle() {
        val s = session ?: return
        handle(s, s.skipCurrent())
    }

    fun pressSeal() {
        val s = session ?: return
        handle(s, s.pressSealButton())
    }

    // ---- 내부 -----------------------------------------------------------------------------------

    private suspend fun restoreSaved() {
        val saved = records.loadSession() ?: return
        if (saved.status.isFinished) {
            records.clearSession()
            return
        }
        val s = GameSession.restore(saved, clock)
        session = s
        _state.update { it.copy(saved = summary(s)) }
        log("restored status=${s.status} index=${s.state.currentIndex} elapsedMs=${s.state.elapsedMs}")
    }

    /** 지원 범위·현재값으로 판 길이 가능 여부를 계산한다. S0에서는 참가자를 운전석만으로 본다(참가자가 늘면 가능 길이는 줄지 않는다). */
    private suspend fun refreshAvailability() {
        val pool = SignalPool.from(climate.support(), light.support(), drive.support()).also { pool = it }
        val current = readAll()
        val st = _state.value
        val available = PuzzleGenerator.availableLengths(setOf(SeatPosition.DRIVER), pool, current, st.settings.isEv, st.demoSeed != null)
        _state.update {
            val selected = when {
                it.selectedLength in available -> it.selectedLength
                BoardLength.FIVE in available -> BoardLength.FIVE
                else -> available.minByOrNull { l -> l.minutes } ?: it.selectedLength
            }
            it.copy(availableLengths = available, supportLoaded = true, selectedLength = selected)
        }
    }

    private suspend fun readAll(): VehicleSignals = VehicleSignals(
        targetTemperature = climate.readTargetTemperature(),
        fanSpeed = climate.readFanSpeed(),
        recirculation = climate.readRecirculation(),
        ambientColor = light.readAmbientColor(),
        ambientBrightness = light.readAmbientBrightness(),
        readingLights = light.readReadingLights(),
        steeringAngle = drive.readSteeringAngle(),
        brakePressed = drive.readBrakePressed(),
    )

    /** 자리 단서를 낼 수 있는 좌석(독서등 area가 있는 좌석). 2열 가운데는 선택지에 없다. 시연 모드는 4좌석 모두. */
    private fun selectableSeats(): Set<SeatPosition> {
        val base = pool ?: SignalPool()
        val effective = if (_state.value.demoSeed != null) SignalPool.demo(base) else base
        return effective.readingLightSeats - SeatPosition.DRIVER - SeatPosition.REAR_CENTER
    }

    private fun seatChoices(selected: Set<SeatPosition>, selectable: Set<SeatPosition>): List<SeatChoice> =
        SEAT_ORDER.map { seat ->
            if (seat == SeatPosition.DRIVER) SeatChoice(seat, selected = true, selectable = false)
            else SeatChoice(seat, selected = seat in selected, selectable = seat in selectable)
        }

    private fun resumeReady(s: GameSession): ReadyState {
        val length = BoardLength.of(s.state.lengthMinutes)
        return ReadyState(
            resume = true,
            lengthMinutes = length.minutes,
            puzzleCount = s.state.board.puzzles.size,
            seats = seatChoices(s.state.participants, emptySet()),
            showIdleNotice = false,
            summary = summary(s),
        )
    }

    private suspend fun startBoard(ready: ReadyState) {
        val pool = pool ?: SignalPool.from(climate.support(), light.support(), drive.support()).also { pool = it }
        val participants = ready.seats.filter { it.selected }.map { it.seat }.toSet()
        val snapshot = readAll()
        val st = _state.value
        val seed = st.demoSeed ?: newSeed()
        val input = GeneratorInput(seed, ready.lengthMinutes, participants, pool, snapshot, st.settings.isEv, st.demoSeed != null)
        val result = PuzzleGenerator.generate(input)
        if (result !is GenerationResult.Success) {
            log("board unavailable seed=$seed result=$result")
            _state.update { it.copy(screen = Screen.HOME, ready = null, notice = Notice.BOARD_UNAVAILABLE) }
            refreshAvailability()
            return
        }
        if (!_state.value.gateOpen || _state.value.screen != Screen.READY) return
        val s = GameSession.start(result.board, participants, snapshot, clock)
        session = s
        hints = -1 to 0 // 새 판은 힌트를 처음부터(앞 판의 같은 번호 퍼즐과 섞이지 않게)
        s.confirmParked(gateOpen = true)
        log(
            "start seed=$seed length=${ready.lengthMinutes} participants=${participants.sorted()} " +
                "puzzles=${result.board.puzzles.map { "${it.signal}:${it.type}" }} targets=${result.board.puzzles.map { it.target }}",
        )
        startPlaying(s, snapshot)
        save(s, "start")
    }

    private suspend fun resume() {
        val s = session ?: return
        if (s.confirmParked(gateOpen = _state.value.gateOpen) != SessionEvent.Resumed) return
        // 재개: get*으로 현재값을 다시 읽어 판정기의 시작 상태로 쓴다(브레이크는 readBrakePressed)
        val resync = readAll()
        if (s.status != SessionStatus.PLAYING) return // 읽는 사이 게이트가 닫힘
        log("resume index=${s.state.currentIndex} elapsedMs=${s.state.elapsedMs} brake=${resync.brakePressed}")
        startPlaying(s, resync)
        save(s, "resume")
    }

    private fun startPlaying(s: GameSession, current: VehicleSignals) {
        _state.update { it.copy(screen = Screen.PLAYING, ready = null, saved = null) }
        currentSignals = current
        handle(s, s.onSignals(current))
        if (s.status == SessionStatus.PLAYING) {
            startListening(s)
            scheduleAuto(s)
        }
        refreshPlay()
    }

    private fun startListening(s: GameSession) {
        stopListening(null)
        val signals = s.state.board.puzzles.map { it.signal }.toSet()
        signalJob = viewModelScope.launch {
            var reread = false
            signalsFlow(signals).collect { values ->
                currentSignals = values
                handle(s, s.onSignals(values))
                if (!reread) {
                    reread = true
                    // 시작·재개 때의 get과 리스너가 다시 값을 받기 시작하는 사이(약 10~15ms)에 바뀐 조명은 이벤트가 오지 않는다
                    // (F3 QA MINOR 4). 첫 값을 받은 뒤 한 번 더 읽어 공유 상태를 맞춘다. 읽은 값은 같은 Flow로 들어온다.
                    launch { rereadLights(signals) }
                }
            }
        }
        if (tickIntervalMs > 0) {
            tickJob = viewModelScope.launch {
                while (isActive) {
                    delay(tickIntervalMs)
                    handle(s, s.tick())
                }
            }
        }
        log("listeners registered signals=${signals.sorted()}")
    }

    private suspend fun rereadLights(signals: Set<SignalId>) {
        if (SignalId.L1 in signals) light.readAmbientColor()
        if (SignalId.L2 in signals) light.readAmbientBrightness()
        if (SignalId.L3 in signals) light.readReadingLights()
    }

    private fun stopListening(reason: String?) {
        val had = signalJob != null || tickJob != null
        signalJob?.cancel()
        tickJob?.cancel()
        autoJob?.cancel()
        signalJob = null
        tickJob = null
        autoJob = null
        if (had && reason != null) log("listeners released reason=$reason")
    }

    /** 판에 쓰는 신호만 묶는다(최소 수집). 묶은 Flow는 모든 신호가 한 번씩 값을 낸 뒤부터 내보낸다. */
    private fun signalsFlow(signals: Set<SignalId>): Flow<VehicleSignals> {
        val parts = buildList<Flow<(VehicleSignals) -> VehicleSignals>> {
            if (SignalId.C1 in signals) add(climate.targetTemperature.map { v -> { s: VehicleSignals -> s.copy(targetTemperature = v) } })
            if (SignalId.C2 in signals) add(climate.fanSpeed.map { v -> { s: VehicleSignals -> s.copy(fanSpeed = v) } })
            if (SignalId.C3 in signals) add(climate.recirculation.map { v -> { s: VehicleSignals -> s.copy(recirculation = v) } })
            if (SignalId.L1 in signals) add(light.ambientColor.map { v -> { s: VehicleSignals -> s.copy(ambientColor = v) } })
            if (SignalId.L2 in signals) add(light.ambientBrightness.map { v -> { s: VehicleSignals -> s.copy(ambientBrightness = v) } })
            if (SignalId.L3 in signals) add(light.readingLights.map { v -> { s: VehicleSignals -> s.copy(readingLights = v) } })
            if (SignalId.D1 in signals) add(drive.steeringAngle.map { v -> { s: VehicleSignals -> s.copy(steeringAngle = v) } })
            if (SignalId.B1 in signals) add(drive.brakePressed.map { v -> { s: VehicleSignals -> s.copy(brakePressed = v) } })
        }
        return combine(parts) { updates -> updates.fold(VehicleSignals()) { acc, apply -> apply(acc) } }
    }

    private fun handle(s: GameSession, event: SessionEvent) {
        if (s !== session) return
        when (event) {
            is SessionEvent.JudgeChanged -> if (event.state == JudgeState.NeedsSteeringCheck) {
                // 콜백만으로 확정하지 않고 다시 읽어 확인한다(등록 직후 첫 콜백이 낡은 값일 수 있다)
                viewModelScope.launch { handle(s, s.confirmSteering(drive.readSteeringAngle())) }
            }
            is SessionEvent.PuzzleSolved -> {
                save(s, "solved #${event.index}")
                scheduleAuto(s)
            }
            is SessionEvent.PuzzleSkipped -> {
                save(s, "skipped #${event.index}")
                scheduleAuto(s)
            }
            is SessionEvent.Escaped, SessionEvent.TimedOut -> finish(s)
            else -> Unit
        }
        refreshPlay()
    }

    private fun finish(s: GameSession) {
        stopListening("finished")
        val escaped = s.status == SessionStatus.ESCAPED
        val skipped = s.state.skippedIndices.isNotEmpty()
        val previousBest = _state.value.bestTimes[s.state.lengthMinutes]
        val puzzles = s.state.board.puzzles.size
        val solved = (0 until minOf(s.state.currentIndex, puzzles)).count { it !in s.state.skippedIndices }
        val result = ResultState(
            escaped = escaped,
            elapsedMs = s.state.elapsedMs,
            newBest = false,
            skipped = skipped,
            lengthMinutes = s.state.lengthMinutes,
            previousBestMs = previousBest,
            solvedCount = solved,
            puzzleCount = puzzles,
            checklist = currentSignals?.let(s::restoreChecklist).orEmpty(),
        )
        _state.update { it.copy(screen = Screen.RESULT, play = null, saved = null, result = result) }
        log("finished status=${s.status} elapsedMs=${s.state.elapsedMs}")
        session = null
        resultSession = s
        if (_state.value.gateOpen) startResultListening(s)
        autoPlayer?.takeIf { autoPlayEnabled }?.let { player ->
            autoJob = viewModelScope.launch {
                delay(AutoPlayer.SOLVE_AFTER_MS)
                if (resultSession === s) player.restore(s.state.snapshot)
            }
        }
        viewModelScope.launch {
            val newBest = if (s.isRecordEligible) records.recordEscape(s.state.lengthMinutes, s.state.elapsedMs) else false
            records.clearSession()
            _state.update { st -> st.result?.let { st.copy(result = it.copy(newBest = newBest)) } ?: st }
        }
    }

    // ---- S3 복구 체크리스트 ------------------------------------------------------------------

    /** 판이 바꾼 신호(B1 제외)만 구독해 체크리스트를 갱신한다. 정차일 때만 부른다. */
    private fun startResultListening(s: GameSession) {
        stopResultListening(null)
        val signals = s.state.board.puzzles.map { it.signal }.toSet() - SignalId.B1
        if (signals.isEmpty()) return
        resultJob = viewModelScope.launch {
            signalsFlow(signals).collect { values -> updateChecklist(s, values) }
        }
        log("result listeners registered signals=${signals.sorted()}")
    }

    private fun stopResultListening(reason: String?) {
        val had = resultJob != null
        resultJob?.cancel()
        resultJob = null
        if (had && reason != null) log("result listeners released reason=$reason")
    }

    /** 정차로 돌아왔거나 앱으로 돌아왔을 때: get*으로 다시 읽어 체크를 갱신하고 리스너를 다시 건다. */
    private fun resumeResult() {
        val s = resultSession ?: return
        if (resultJob != null || !_state.value.gateOpen) return
        viewModelScope.launch {
            updateChecklist(s, readAll())
            val st = _state.value
            if (resultSession === s && st.gateOpen && st.screen == Screen.RESULT && resultJob == null) startResultListening(s)
        }
    }

    private fun updateChecklist(s: GameSession, values: VehicleSignals) {
        if (resultSession !== s) return
        _state.update { st -> st.result?.let { st.copy(result = it.copy(checklist = s.restoreChecklist(values))) } ?: st }
    }

    /** Fake 빌드: 현재 퍼즐을 6초 뒤 목표값으로 바꾼다. 봉인 버튼은 대신 누른다. */
    private fun scheduleAuto(s: GameSession) {
        autoJob?.cancel()
        val player = autoPlayer?.takeIf { autoPlayEnabled } ?: return
        if (s.status != SessionStatus.PLAYING) return
        val puzzle = s.currentPuzzle ?: return
        autoJob = viewModelScope.launch {
            delay(AutoPlayer.SOLVE_AFTER_MS)
            if (s !== session || s.status != SessionStatus.PLAYING || s.currentPuzzle != puzzle) return@launch
            log("auto solve #${puzzle.index} ${puzzle.signal}")
            if (!player.solve(puzzle)) handle(s, s.pressSealButton())
        }
    }

    private fun refreshPlay() {
        val s = session
        if (s == null || s.status != SessionStatus.PLAYING || _state.value.screen != Screen.PLAYING) return
        val puzzle = s.currentPuzzle ?: return
        val index = s.state.currentIndex
        val effectivePool = pool?.let { if (s.state.board.demo) SignalPool.demo(it) else it }
        _state.update {
            it.copy(
                play = PlayState(
                    board = s.state.board,
                    puzzle = puzzle,
                    number = index + 1,
                    count = s.state.board.puzzles.size,
                    remainingMs = s.remainingMs(),
                    judge = s.judgeState,
                    signals = currentSignals,
                    bounds = DisplayBounds.from(effectivePool),
                    hintsShown = if (hints.first == index) hints.second else 0,
                    canSkip = s.canSkip(),
                    skipped = s.state.skippedIndices,
                    brake = s.brakeProgress(),
                    steeringHoldMs = s.steeringHoldMs(),
                ),
            )
        }
    }

    /** 일시정지·판정 때마다 저장한다. 저장할 값은 부른 순간의 상태다. */
    private fun save(s: GameSession, reason: String) {
        val saved = s.toSaved()
        viewModelScope.launch {
            records.saveSession(saved)
            log("saved reason=$reason status=${saved.status} index=${saved.currentIndex} elapsedMs=${saved.elapsedMs}")
        }
    }

    private fun summary(s: GameSession) = SavedSummary(
        lengthMinutes = s.state.lengthMinutes,
        puzzleNumber = minOf(s.state.currentIndex + 1, s.state.board.puzzles.size),
        puzzleCount = s.state.board.puzzles.size,
        remainingMs = s.remainingMs(),
        pausedByGate = s.status == SessionStatus.PAUSED_BY_GATE,
    )

    override fun onCleared() {
        stopListening(null)
        stopResultListening(null)
    }

    companion object {
        const val TAG = "F1GATE"
        const val HINT_COUNT = 2
        private val SEAT_ORDER = listOf(SeatPosition.DRIVER, SeatPosition.PASSENGER, SeatPosition.REAR_LEFT, SeatPosition.REAR_RIGHT)

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GameViewModel(
                    climate = container.climateRepository,
                    light = container.cabinLightRepository,
                    drive = container.driveInputRepository,
                    seats = container.seatRepository,
                    records = container.recordStore,
                    settingsRepository = container.settingsRepository,
                    clock = Clock { SystemClock.elapsedRealtime() },
                    log = { Log.i(TAG, "$it t=${System.currentTimeMillis()}") },
                    autoPlayer = container.autoPlayer,
                    demoStageCheck = BuildConfig.DEBUG,
                )
            }
        }
    }
}
