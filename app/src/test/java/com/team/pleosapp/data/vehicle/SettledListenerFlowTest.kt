package com.team.pleosapp.data.vehicle

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** C4·L4 리스너: 등록 직후의 낡은 값(에뮬레이터 실측: 켜짐 → 꺼짐이 몇 ms 사이에 온다)을 버리고 get 값으로 시작한다. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettledListenerFlowTest {

    @Test
    fun dropsRegistrationBurst_startsFromRead_thenPassesChanges() = runTest {
        var emit: ((Boolean?) -> Unit)? = null
        var unsubscribed = false
        var vehicle = false
        val flow = settledListenerFlow("t", "L4", read = { vehicle }, settleMs = 300, debugLog = {}) { e, _ ->
            emit = e
            return@settledListenerFlow { unsubscribed = true }
        }
        val values = mutableListOf<Boolean?>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { flow.toList(values) }
        runCurrent()
        emit!!(true) // 차량 서비스가 먼저 보내는 낡은 값
        emit!!(false)
        advanceTimeBy(299)
        assertEquals(emptyList<Boolean?>(), values) // 기다리는 동안은 아무것도 내보내지 않는다
        advanceTimeBy(2)
        assertEquals(listOf<Boolean?>(false), values) // get으로 읽은 현재 값이 첫 값
        vehicle = true
        emit!!(true)
        emit!!(true) // 같은 값은 한 번만
        emit!!(false)
        assertEquals(listOf<Boolean?>(false, true, false), values)
        assertFalse(unsubscribed)
        job.cancel()
        runCurrent()
        assertTrue(unsubscribed) // 등록 때와 같은 리스너로 해제
    }

    @Test
    fun unsupported_emitsNullOnce() = runTest {
        val flow = settledListenerFlow<Boolean>("t", "C4", read = { error("not called") }, debugLog = {}) { _, _ -> null }
        val values = mutableListOf<Boolean?>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) { flow.toList(values) }
        advanceTimeBy(1_000)
        assertEquals(listOf<Boolean?>(null), values)
        job.cancel()
    }
}
