package com.team.pleosapp.domain.puzzle

import java.time.LocalDate
import kotlin.random.Random

/**
 * 시드로 정해지는 난수(SplitMix64). 알고리즘을 직접 두어 Kotlin·JVM 버전이 바뀌어도 같은 시드는 같은 판을 만든다
 * (F4 오늘의 판: 같은 날 두 기기에서 같은 시드 → 같은 수열).
 */
class SeededRandom(seed: Long) {
    private var state: Long = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL // 0x9E3779B97F4A7C15
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L // 0xBF58476D1CE4E5B9
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L // 0x94D049BB133111EB
        return z xor (z ushr 31)
    }

    /** 0 이상 [bound] 미만의 정수(치우침 없는 거절 표본). */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive: $bound" }
        val limit = Long.MAX_VALUE - (Long.MAX_VALUE % bound)
        while (true) {
            val v = nextLong() ushr 1
            if (v < limit) return (v % bound).toInt()
        }
    }

    /** [from]..[to] 사이 정수(양 끝 포함). */
    fun nextIntIn(from: Int, to: Int): Int = from + nextInt(to - from + 1)

    fun nextBoolean(): Boolean = nextLong() < 0

    fun <T> pick(items: List<T>): T = items[nextInt(items.size)]

    fun <T> shuffled(items: List<T>): List<T> {
        val list = items.toMutableList()
        for (i in list.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            val tmp = list[i]
            list[i] = list[j]
            list[j] = tmp
        }
        return list
    }
}

object Seeds {
    /** 시연 시드(기획 5절). 디버그 빌드에서 인텐트 extra `seed`로 넣는다. */
    const val DEMO = "demo5"

    /** 시드 문자열 → 64비트(FNV-1a). */
    fun hash(seed: String): Long {
        var h = -0x340d631b7bdddcdbL // 0xCBF29CE484222325
        for (b in seed.toByteArray(Charsets.UTF_8)) {
            h = h xor (b.toLong() and 0xFF)
            h *= 0x100000001b3L
        }
        return h
    }

    /** 새 판용 무작위 시드(8자리 영숫자). */
    fun random(random: Random = Random.Default): String {
        val alphabet = "abcdefghjkmnpqrstuvwxyz23456789"
        return (1..8).map { alphabet[random.nextInt(alphabet.length)] }.joinToString("")
    }

    /** F4 오늘의 판: 날짜만으로 정해지는 시드. */
    fun daily(date: LocalDate): String = "day-$date"
}
