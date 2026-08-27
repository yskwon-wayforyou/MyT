package com.myt.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myt.domain.usecase.DebugLogUseCase
import com.myt.debug.DebugLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DebugLogViewModel(
    private val debugLogUseCase: DebugLogUseCase,
    private val debugLogger: DebugLogger,
) : ViewModel() {
    val entries = debugLogUseCase.entries

    private val _enabled = MutableStateFlow(debugLogUseCase.isEnabled)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    init {
        debugLogger.i("DebugLog", "Debug log screen opened")
    }

    fun setEnabled(value: Boolean) {
        debugLogUseCase.isEnabled = value
        _enabled.value = value
        _status.value = if (value) "로그 수집 켜짐" else "로그 수집 꺼짐"
    }

    fun exportViaGmail() {
        viewModelScope.launch {
            _status.value = "내보내는 중…"
            debugLogUseCase.exportViaEmail(
                extraContext = mapOf(
                    "screen" to "DebugLogScreen",
                ),
            ).fold(
                onSuccess = { _status.value = "Gmail/메일 앱을 확인해 주세요" },
                onFailure = { _status.value = "내보내기 실패: ${it.message}" },
            )
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            debugLogUseCase.clear()
            _status.value = "로그를 비웠습니다"
        }
    }
}
