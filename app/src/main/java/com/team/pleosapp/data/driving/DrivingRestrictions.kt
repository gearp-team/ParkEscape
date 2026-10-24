package com.team.pleosapp.data.driving

import android.car.drivingstate.CarUxRestrictions

/**
 * CarUxRestrictions를 앱에서 쓰기 쉽게 옮긴 값. 지역별 규칙은 하드코딩하지 말고 플랫폼이 내려 주는 비트만 따른다.
 */
data class DrivingRestrictions(
    val requiresDistractionOptimization: Boolean = false,
    val activeRestrictions: Int = CarUxRestrictions.UX_RESTRICTIONS_BASELINE,
) {
    val noKeyboard: Boolean get() = has(CarUxRestrictions.UX_RESTRICTIONS_NO_KEYBOARD)
    val noVideo: Boolean get() = has(CarUxRestrictions.UX_RESTRICTIONS_NO_VIDEO)
    val noSetup: Boolean get() = has(CarUxRestrictions.UX_RESTRICTIONS_NO_SETUP)
    val limitContent: Boolean get() = has(CarUxRestrictions.UX_RESTRICTIONS_LIMIT_CONTENT)
    val limitStringLength: Boolean get() = has(CarUxRestrictions.UX_RESTRICTIONS_LIMIT_STRING_LENGTH)

    private fun has(flag: Int): Boolean = (activeRestrictions and flag) != 0

    companion object {
        /** 규제 상태를 알 수 없을 때 쓰는 안전한 기본값. */
        val FullyRestricted = DrivingRestrictions(
            requiresDistractionOptimization = true,
            activeRestrictions = CarUxRestrictions.UX_RESTRICTIONS_FULLY_RESTRICTED,
        )
    }
}
