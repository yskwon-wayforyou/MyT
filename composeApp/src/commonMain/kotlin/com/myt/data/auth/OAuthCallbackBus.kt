package com.myt.data.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object OAuthCallbackBus {
    private val _codes = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val codes: SharedFlow<String> = _codes.asSharedFlow()

    fun emitCode(code: String) {
        _codes.tryEmit(code)
    }
}
