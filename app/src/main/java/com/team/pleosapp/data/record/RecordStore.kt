package com.team.pleosapp.data.record

import com.team.pleosapp.domain.puzzle.SessionState
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 진행 중인 판과 판 길이별 최고 기록 저장소(기획 F2·2-2). 기기 안에만 저장한다(서버 없음).
 * 저장 시점은 호출자(화면·ViewModel)가 정한다: 전이마다, 퍼즐 판정마다, onPause/onStop마다, 규제 콜백의 일시정지 때.
 */
interface RecordStore {
    /** 판 길이(분) → 최고 기록(ms). 기록이 없는 길이는 맵에 없다. */
    val bestTimes: Flow<Map<Int, Long>>

    suspend fun saveSession(state: SessionState)

    /** 저장된 판. 없거나 손상됐으면 null(손상된 값은 지운다: "DataStore 손상 시 저장 진행을 버리고 홈으로"). */
    suspend fun loadSession(): SessionState?

    suspend fun clearSession()

    /** 탈출 기록을 반영한다. 그 판 길이의 최고 기록을 새로 세웠으면 true. 건너뛴 판은 호출하지 않는다. */
    suspend fun recordEscape(lengthMinutes: Int, elapsedMs: Long): Boolean
}

/** 세션 JSON 형식. 형식을 바꾸면 [VERSION]을 올리고, 다른 버전은 버린다. */
object SessionCodec {
    const val VERSION = 1

    @Serializable
    private data class Envelope(val version: Int, val state: SessionState)

    private val json = Json { ignoreUnknownKeys = true }

    fun encode(state: SessionState): String = json.encodeToString(Envelope.serializer(), Envelope(VERSION, state))

    /** 형식이 틀리거나 버전이 다르면 예외. */
    fun decode(text: String): SessionState {
        val envelope = json.decodeFromString(Envelope.serializer(), text)
        require(envelope.version == VERSION) { "unsupported session version ${envelope.version}" }
        return envelope.state
    }
}
