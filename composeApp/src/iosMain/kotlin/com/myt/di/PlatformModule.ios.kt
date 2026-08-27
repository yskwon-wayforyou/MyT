package com.myt.di

import com.myt.platform.AppInfoPlatform
import com.myt.platform.BatteryOptimizationPlatform
import com.myt.platform.LogExportPlatform
import com.myt.data.local.DatabaseDriverFactory
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
import org.koin.dsl.module
import com.myt.platform.CrashReporterPlatform

actual fun platformModule() = module {
    single { DatabaseDriverFactory() }
    single { SecureStoragePlatform(Unit) }
    single { BluetoothPlatform(Unit) }
    single { SpeechPlatform(Unit) }
    single { AudioAlertPlatform(Unit) }
    single { HapticPlatform(Unit) }
    single { ScreenPlatform(Unit) }
    single { DeviceLocationPlatform(Unit) }
    single { DeviceCommunicationsPlatform(Unit) }
    single { TextToSpeechPlatform(Unit) }
    single { AppInfoPlatform(Unit) }
    single { BatteryOptimizationPlatform(Unit) }
    single { com.myt.platform.LocalNotificationPlatform(Unit) }
    single { LogExportPlatform(Unit) }
    single { com.myt.debug.IosPersistentLogSink() }
    single<com.myt.debug.PersistentLogSink> { get<com.myt.debug.IosPersistentLogSink>() }
    single { com.myt.debug.IosPendingIssueStore() }
    single<com.myt.debug.PendingIssueStore> { get<com.myt.debug.IosPendingIssueStore>() }
    single { CrashReporterPlatform(Unit) }
    single { OAuthPlatform() }
}
