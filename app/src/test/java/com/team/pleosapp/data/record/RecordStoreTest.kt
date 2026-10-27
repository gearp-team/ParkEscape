package com.team.pleosapp.data.record

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.team.pleosapp.data.signal.SeatPosition
import com.team.pleosapp.domain.puzzle.FakeClock
import com.team.pleosapp.domain.puzzle.Fixtures
import com.team.pleosapp.domain.puzzle.GameSession
import com.team.pleosapp.domain.puzzle.SessionState
import com.team.pleosapp.domain.puzzle.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class RecordStoreTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val file: File by lazy { File(folder.root, "records.preferences_pb") }
    private val scopes = mutableListOf<CoroutineScope>()
    private val corrupt = mutableListOf<Throwable>()

    /** 앱과 같은 손상 처리기로 DataStore를 만든다. 새 scope를 쓰므로 앞의 것을 닫은 뒤 부르면 "앱 재시작"이 된다. */
    private fun newDataStore(): DataStore<Preferences> {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        scopes += scope
        return PreferenceDataStoreFactory.create(
            corruptionHandler = DataStoreRecordStore.corruptionHandler { corrupt += it },
            scope = scope,
        ) { file }
    }

    private suspend fun closeAll() {
        scopes.forEach { (it.coroutineContext.job as Job).cancelAndJoin() }
        scopes.clear()
    }

    @After
    fun tearDown() = runBlocking { closeAll() }

    /** 시연 판(demo5)을 한 퍼즐 풀고 주행으로 일시정지한 상태: 판·암호표·스냅샷·참가자·경과 시간·일시정지 원인이 다 들어 있다. */
    private fun sampleState(): SessionState {
        val board = Fixtures.board(Fixtures.input("demo5", 5, demoMode = true))
        val clock = FakeClock()
        val session = GameSession.start(board, setOf(SeatPosition.DRIVER, SeatPosition.REAR_RIGHT), Fixtures.CURRENT, clock)
        session.confirmParked(gateOpen = true)
        clock.advance(12_345)
        session.onSignals(Fixtures.CURRENT.copy(steeringAngle = -45f))
        clock.advance(1_200)
        session.onSignals(Fixtures.CURRENT.copy(steeringAngle = 0f))
        session.confirmSteering(0f)
        session.onGateClosed()
        return session.toSaved()
    }

    @Test
    fun codec_roundTrip() {
        val state = sampleState()
        assertEquals(SessionStatus.PAUSED_BY_GATE, state.status)
        assertEquals(1, state.currentIndex)
        assertEquals(13_545L, state.elapsedMs)
        assertEquals(state, SessionCodec.decode(SessionCodec.encode(state)))
    }

    @Test
    fun dataStore_saveLoadClear() = runBlocking {
        val store = DataStoreRecordStore(newDataStore()) { corrupt += it }
        assertNull(store.loadSession())
        val state = sampleState()
        store.saveSession(state)
        assertEquals(state, store.loadSession())
        store.clearSession()
        assertNull(store.loadSession())
        assertTrue(corrupt.isEmpty())
    }

    @Test
    fun dataStore_survivesRestart() = runBlocking {
        val state = sampleState()
        val first = DataStoreRecordStore(newDataStore())
        first.saveSession(state)
        assertTrue(first.recordEscape(5, 252_000))

        // 앱 재시작: DataStore를 닫고 같은 파일에 새 인스턴스를 만든다(메모리 캐시 없음)
        closeAll()
        val second = DataStoreRecordStore(newDataStore())
        assertEquals(state, second.loadSession())
        assertEquals(mapOf(5 to 252_000L), second.bestTimes.first())
        assertTrue(corrupt.isEmpty())
    }

    @Test
    fun dataStore_corruptSessionJsonIsDropped() = runBlocking {
        val dataStore = newDataStore()
        dataStore.edit { it[stringPreferencesKey("session")] = "{not json" }
        val store = DataStoreRecordStore(dataStore) { corrupt += it }
        assertNull(store.loadSession())
        assertEquals(1, corrupt.size)
        assertNull(dataStore.data.first()[stringPreferencesKey("session")])
    }

    @Test
    fun dataStore_corruptFileIsReplaced_andStoreKeepsWorking() = runBlocking {
        // 파일 자체가 깨짐(QA F2-engine 실패 3): 손상 처리기가 빈 값으로 바꾸고, 저장·기록이 다시 동작해야 한다
        file.writeBytes("garbage!! not a protobuf".toByteArray())
        val store = DataStoreRecordStore(newDataStore()) { corrupt += it }
        assertNull(store.loadSession())
        assertEquals(emptyMap<Int, Long>(), store.bestTimes.first())
        assertEquals(1, corrupt.size)

        val state = sampleState()
        store.saveSession(state)
        assertTrue(store.recordEscape(3, 150_000))
        assertEquals(state, store.loadSession())

        closeAll()
        val reopened = DataStoreRecordStore(newDataStore())
        assertEquals(state, reopened.loadSession())
        assertEquals(mapOf(3 to 150_000L), reopened.bestTimes.first())
    }

    @Test
    fun dataStore_bestTimesPerLength() = runBlocking {
        val store = DataStoreRecordStore(newDataStore()) { corrupt += it }
        assertEquals(emptyMap<Int, Long>(), store.bestTimes.first())
        assertTrue(store.recordEscape(5, 252_000))
        assertFalse(store.recordEscape(5, 260_000))
        assertFalse(store.recordEscape(5, 252_000))
        assertTrue(store.recordEscape(5, 240_000))
        assertTrue(store.recordEscape(3, 150_000))
        assertEquals(mapOf(3 to 150_000L, 5 to 240_000L), store.bestTimes.first())
    }

    @Test
    fun fake_matchesContract() = runBlocking {
        val store = FakeRecordStore()
        val state = sampleState()
        store.saveSession(state)
        assertEquals(state, store.loadSession())
        assertTrue(store.recordEscape(10, 500_000))
        assertFalse(store.recordEscape(10, 600_000))
        assertEquals(mapOf(10 to 500_000L), store.bestTimes.first())
        store.corruptSession("garbage")
        assertNull(store.loadSession())
    }
}
