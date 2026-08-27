package com.myt.di

import com.myt.platform.AudioAlertPlatform
import com.myt.platform.BluetoothPlatform
import com.myt.platform.DeviceCommunicationsPlatform
import com.myt.platform.DeviceLocationPlatform
import com.myt.platform.HapticPlatform
import com.myt.platform.OAuthPlatform
import com.myt.platform.ScreenPlatform
import com.myt.platform.SecureStoragePlatform
import com.myt.platform.SpeechPlatform
import com.myt.platform.TextToSpeechPlatform
import com.myt.platform.AppInfoPlatform
import com.myt.platform.BatteryOptimizationPlatform
import com.myt.platform.LocalNotificationPlatform
import com.myt.platform.LogExportPlatform
import com.myt.platform.CrashReporterPlatform
import com.myt.data.local.DatabaseDriverFactory
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual fun platformModule() = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { SecureStoragePlatform(androidContext()) }
    single { BluetoothPlatform(androidContext()) }
    single { SpeechPlatform(androidContext()) }
    single { AudioAlertPlatform(androidContext()) }
    single { HapticPlatform(androidContext()) }
    single { ScreenPlatform(androidContext()) }
    single { DeviceLocationPlatform(androidContext()) }
    single { DeviceCommunicationsPlatform(androidContext()) }
    single { TextToSpeechPlatform(androidContext()) }
    single { AppInfoPlatform(androidContext()) }
    single { BatteryOptimizationPlatform(androidContext()) }
    single { LocalNotificationPlatform(androidContext()) }
    single { LogExportPlatform(androidContext()) }
    single { com.myt.debug.AndroidPersistentLogSink(androidContext()) }
    single<com.myt.debug.PersistentLogSink> { get<com.myt.debug.AndroidPersistentLogSink>() }
    single { com.myt.debug.AndroidPendingIssueStore(androidContext()) }
    single<com.myt.debug.PendingIssueStore> { get<com.myt.debug.AndroidPendingIssueStore>() }
    single {
        CrashReporterPlatform(androidContext()).also { reporter ->
            reporter.attachLogSink(get())
        }
    }
    single { OAuthPlatform() }
}
