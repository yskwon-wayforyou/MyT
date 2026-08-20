package com.myt.di

import com.myt.platform.AudioAlertPlatform
import com.myt.platform.BluetoothPlatform
import com.myt.platform.HapticPlatform
import com.myt.platform.OAuthPlatform
import com.myt.platform.ScreenPlatform
import com.myt.platform.SecureStoragePlatform
import com.myt.platform.SpeechPlatform
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single { SecureStoragePlatform(androidContext()) }
    single { BluetoothPlatform(androidContext()) }
    single { SpeechPlatform(androidContext()) }
    single { AudioAlertPlatform(androidContext()) }
    single { HapticPlatform(androidContext()) }
    single { ScreenPlatform(androidContext()) }
    single { OAuthPlatform() }
}
