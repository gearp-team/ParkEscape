package com.team.pleosapp.data.record

import com.team.pleosapp.domain.puzzle.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 메모리 저장소(테스트·미리보기용). 세션도 [SessionCodec]으로 한 번 인코딩해 두어 실제 저장과 같은 왕복을 거친다. */
class FakeRecordStore(bestTimes: Map<Int, Long> = emptyMap()) : RecordStore {
    private val best = MutableStateFlow(bestTimes)
    private var sessionJson: String? = null

    override val bestTimes: Flow<Map<Int, Long>> = best.asStateFlow()

    override suspend fun saveSession(state: SessionState) {
        sessionJson = SessionCodec.encode(state)
    }

    override suspend fun loadSession(): SessionState? = sessionJson?.let {
        runCatching { SessionCodec.decode(it) }.getOrElse {
            sessionJson = null
            null
        }
    }

    override suspend fun clearSession() {
        sessionJson = null
    }

    override suspend fun recordEscape(lengthMinutes: Int, elapsedMs: Long): Boolean {
        val current = best.value[lengthMinutes]
        if (current != null && elapsedMs >= current) return false
        best.value = best.value + (lengthMinutes to elapsedMs)
        return true
    }

    /** 손상된 저장값을 흉내 낸다(테스트용). */
    fun corruptSession(text: String) {
        sessionJson = text
    }
}
