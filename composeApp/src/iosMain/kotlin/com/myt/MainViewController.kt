package com.myt

import androidx.compose.ui.window.ComposeUIViewController
import com.myt.di.initKoin

private var koinStarted = false

fun MainViewController() = ComposeUIViewController {
    if (!koinStarted) {
        initKoin()
        koinStarted = true
    }
    App()
}
