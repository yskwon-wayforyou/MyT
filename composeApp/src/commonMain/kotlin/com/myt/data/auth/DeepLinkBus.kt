package com.myt.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** Deep-link route ids from notifications / BT auto-launch (`myt.route` extras). */
object DeepLinkBus {
    private val _routes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val routes: SharedFlow<String> = _routes.asSharedFlow()

    suspend fun emit(route: String) {
        _routes.emit(route)
    }
}
