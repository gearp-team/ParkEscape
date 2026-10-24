package com.team.pleosapp.data.driving

import kotlinx.coroutines.flow.Flow

interface DrivingStateRepository {
    /** 현재 주행 규제 상태. 구독하면 현재 값을 먼저 한 번 내보낸다. */
    val restrictions: Flow<DrivingRestrictions>
}
