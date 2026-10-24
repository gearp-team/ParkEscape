package com.team.pleosapp.data.vehicle

import kotlinx.coroutines.flow.Flow

interface VehicleRepository {
    /** EV 배터리 충전량(%). 미지원이거나 오류면 null. */
    val evStateOfCharge: Flow<Int?>
}
