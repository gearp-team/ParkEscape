package com.team.pleosapp.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.team.pleosapp.data.record.DataStoreRecordStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/** 앱 설정(기획 F1 설정 화면). 기기 안에만 저장한다. */
data class AppSettings(
    /** "내 차는 전기차"(기본 꺼짐). 꺼져 있으면 공조 퍼즐 판당 2개·풍량 상한·공회전 안내가 적용된다. */
    val isEv: Boolean = false,
    /** S1 공회전 안내의 "다시 보지 않기" */
    val idleNoticeDismissed: Boolean = false,
)

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setEv(isEv: Boolean)

    suspend fun setIdleNoticeDismissed(dismissed: Boolean)
}

/** Preferences DataStore 구현(files/datastore/settings.preferences_pb). 앱에서 한 번만 만든다(AppContainer). */
class DataStoreSettingsRepository(private val store: DataStore<Preferences>) : SettingsRepository {

    override val settings: Flow<AppSettings> = store.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> AppSettings(isEv = prefs[EV] ?: false, idleNoticeDismissed = prefs[IDLE_DISMISSED] ?: false) }

    override suspend fun setEv(isEv: Boolean) {
        store.edit { it[EV] = isEv }
    }

    override suspend fun setIdleNoticeDismissed(dismissed: Boolean) {
        store.edit { it[IDLE_DISMISSED] = dismissed }
    }

    companion object {
        private val EV = booleanPreferencesKey("is_ev")
        private val IDLE_DISMISSED = booleanPreferencesKey("idle_notice_dismissed")

        fun create(context: Context): DataStoreSettingsRepository {
            val appContext = context.applicationContext
            val store = PreferenceDataStoreFactory.create(corruptionHandler = DataStoreRecordStore.corruptionHandler { }) {
                appContext.preferencesDataStoreFile("settings")
            }
            return DataStoreSettingsRepository(store)
        }
    }
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)

    override val settings: Flow<AppSettings> = state.asStateFlow()

    override suspend fun setEv(isEv: Boolean) {
        state.value = state.value.copy(isEv = isEv)
    }

    override suspend fun setIdleNoticeDismissed(dismissed: Boolean) {
        state.value = state.value.copy(idleNoticeDismissed = dismissed)
    }
}
