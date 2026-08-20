package com.myt.di

import com.myt.platform.AudioAlertPlatform
import com.myt.platform.BluetoothPlatform
import com.myt.platform.HapticPlatform
import com.myt.platform.OAuthPlatform
import com.myt.platform.ScreenPlatform
import com.myt.platform.SecureStoragePlatform
import com.myt.platform.SpeechPlatform
import org.koin.dsl.module

actual fun platformModule() = module {
    single { SecureStoragePlatform(Unit) }
    single { BluetoothPlatform(Unit) }
    single { SpeechPlatform(Unit) }
    single { AudioAlertPlatform(Unit) }
    single { HapticPlatform(Unit) }
    single { ScreenPlatform(Unit) }
    single { OAuthPlatform() }
}
