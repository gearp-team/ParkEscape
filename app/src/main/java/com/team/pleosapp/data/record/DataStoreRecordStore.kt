package com.team.pleosapp.data.record

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import com.team.pleosapp.domain.puzzle.BoardLength
import com.team.pleosapp.domain.puzzle.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Preferences DataStore 구현. 세션은 JSON 문자열 하나([SessionCodec]), 최고 기록은 판 길이마다 Long 하나.
 * 재부팅 뒤에도 남는다. 같은 파일에 DataStore를 둘 만들면 안 되므로 [create]는 앱에서 한 번만 부른다(AppContainer).
 *
 * 손상 처리(기획 F1 "손상 시 저장 진행을 버리고 홈으로")
 * - 파일 자체가 깨지면 DataStore의 [corruptionHandler]가 빈 값으로 바꿔 쓴다(세션·최고 기록이 사라진다). 그 뒤 저장은 정상 동작한다.
 * - 세션 JSON만 깨지면 [loadSession]이 그 세션을 지우고 null을 준다.
 * 두 경우 모두 [onCorrupt]가 불린다.
 */
class DataStoreRecordStore(
    private val store: DataStore<Preferences>,
    private val onCorrupt: (Throwable) -> Unit = ::logCorrupt,
) : RecordStore {

    private val data: Flow<Preferences> = store.data.catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }

    override val bestTimes: Flow<Map<Int, Long>> = data.map { prefs ->
        BoardLength.entries.mapNotNull { length -> prefs[bestKey(length.minutes)]?.let { length.minutes to it } }.toMap()
    }

    override suspend fun saveSession(state: SessionState) {
        val text = SessionCodec.encode(state)
        store.edit { it[SESSION] = text }
    }

    override suspend fun loadSession(): SessionState? {
        val text = data.first()[SESSION] ?: return null
        return try {
            SessionCodec.decode(text)
        } catch (e: Exception) {
            onCorrupt(e)
            clearSession()
            null
        }
    }

    override suspend fun clearSession() {
        store.edit { it.remove(SESSION) }
    }

    override suspend fun recordEscape(lengthMinutes: Int, elapsedMs: Long): Boolean {
        var improved = false
        store.edit { prefs ->
            val key = bestKey(lengthMinutes)
            val best = prefs[key]
            if (best == null || elapsedMs < best) {
                prefs[key] = elapsedMs
                improved = true
            }
        }
        return improved
    }

    companion object {
        const val FILE_NAME = "records"
        private const val TAG = "RecordStore"
        private val SESSION = stringPreferencesKey("session")

        private fun bestKey(minutes: Int) = longPreferencesKey("best_ms_$minutes")

        private fun logCorrupt(e: Throwable) {
            Log.w(TAG, "records dropped: ${e.message ?: e.javaClass.simpleName}")
        }

        /** 파일이 깨지면 [onCorrupt]를 부르고 빈 값으로 바꿔 쓰는 처리기. 테스트도 같은 처리기로 DataStore를 만든다. */
        fun corruptionHandler(onCorrupt: (Throwable) -> Unit): ReplaceFileCorruptionHandler<Preferences> =
            ReplaceFileCorruptionHandler { e ->
                onCorrupt(e)
                emptyPreferences()
            }

        /** 앱용 저장소(files/datastore/records.preferences_pb). 앱 프로세스에서 한 번만 부른다. */
        fun create(context: Context, onCorrupt: (Throwable) -> Unit = ::logCorrupt): DataStoreRecordStore {
            val appContext = context.applicationContext
            val store = PreferenceDataStoreFactory.create(corruptionHandler = corruptionHandler(onCorrupt)) {
                appContext.preferencesDataStoreFile(FILE_NAME)
            }
            return DataStoreRecordStore(store, onCorrupt)
        }
    }
}
