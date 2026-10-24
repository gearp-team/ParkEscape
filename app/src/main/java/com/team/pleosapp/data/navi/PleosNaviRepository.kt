package com.team.pleosapp.data.navi

import ai.pleos.playground.navi.constants.NaviErrorCode
import ai.pleos.playground.navi.data.CurrentLocationInfo
import ai.pleos.playground.navi.helper.NaviHelper
import ai.pleos.playground.navi.helper.listener.NaviHelperEventListener
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NaviHelper 구현. 조회 API는 값을 반환하지 않고, 결과를 리스너 콜백(바인더 스레드)으로 보낸다.
 * 앱 프로세스와 수명을 같이 하므로 release()는 부르지 않는다.
 */
class PleosNaviRepository(context: Context) : NaviRepository {
    private val naviHelper = NaviHelper(context.applicationContext)
    private val location = MutableStateFlow<NaviLocation?>(null)

    @Volatile
    private var connected = false

    private val listener = object : NaviHelperEventListener {
        override fun onCurrentLocationInfo(currentLocationInfo: CurrentLocationInfo) {
            location.value = NaviLocation(
                latitude = currentLocationInfo.latitude,
                longitude = currentLocationInfo.longitude,
                address = currentLocationInfo.address,
            )
        }

        override fun onError(errorCode: NaviErrorCode) {
            Log.w(TAG, "NaviHelper error: $errorCode")
        }
    }

    override val currentLocation: StateFlow<NaviLocation?> = location.asStateFlow()

    init {
        naviHelper.initialize(
            onConnected = {
                connected = true
                naviHelper.addListener(listener)
                naviHelper.getCurrentLocationInfo()
            },
            onFailed = { Log.w(TAG, "NaviHelper connection failed", it) },
        )
    }

    override fun refreshCurrentLocation() {
        if (connected) naviHelper.getCurrentLocationInfo()
    }

    private companion object {
        const val TAG = "PleosNaviRepo"
    }
}
