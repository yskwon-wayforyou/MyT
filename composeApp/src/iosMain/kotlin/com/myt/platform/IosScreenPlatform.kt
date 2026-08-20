package com.myt.platform

actual class ScreenPlatform actual constructor(context: Any) {
    actual fun keepScreenOn(enable: Boolean) = Unit

    actual fun currentWidthDp(): Int = 390

    actual fun currentHeightDp(): Int = 844
}
