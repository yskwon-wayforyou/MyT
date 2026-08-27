package com.myt.di

import com.myt.config.TeslaConfigStore
import com.myt.data.bluetooth.BluetoothRepositoryImpl
import com.myt.data.fleet.KtorFleetRepository
import com.myt.data.history.LocalChargeSessionRecorder
import com.myt.data.history.LocalTripRecorder
import com.myt.data.history.SqlHistoryRepository
import com.myt.data.local.DatabaseDriverFactory
import com.myt.data.local.createDatabase
import com.myt.data.poi.MockPoiRepository
import com.myt.data.poi.PoiSeedRepository
import com.myt.data.poi.SqlPoiRepository
import com.myt.data.quota.FleetUsageRepositoryImpl
import com.myt.data.settings.SettingsRepositoryImpl
import com.myt.data.token.TokenRepositoryImpl
import com.myt.domain.SpeedCamEngine
import com.myt.domain.simulation.DriveSimController
import com.myt.domain.quota.FleetUsageRepository
import com.myt.domain.repository.BluetoothRepository
import com.myt.domain.repository.ChargeSessionRecorder
import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.HistoryRepository
import com.myt.domain.repository.PoiRepository
import com.myt.domain.repository.SettingsRepository
import com.myt.domain.repository.TokenRepository
import com.myt.domain.repository.TripRecorder
import com.myt.debug.DebugLogger
import com.myt.debug.ErrorIssueEnqueuer
import com.myt.debug.GitHubIssueClient
import com.myt.domain.usecase.CrashIssueSyncUseCase
import com.myt.domain.usecase.DebugLogUseCase
import com.myt.domain.usecase.AdaptiveLayoutUseCase
import com.myt.domain.usecase.AuthUseCase
import com.myt.domain.usecase.FleetQuotaUseCase
import com.myt.domain.usecase.HistoryUseCase
import com.myt.config.HaIntegrationConfigStore
import com.myt.phase2.BillingGateway
import com.myt.phase2.GaugeWidgetSnapshotProvider
import com.myt.phase2.InAppPushNotifier
import com.myt.phase2.InMemoryWatchCompanionBridge
import com.myt.phase2.LocalBillingGateway
import com.myt.phase2.PushNotifier
import com.myt.phase2.WatchCompanionBridge
import com.myt.phase2.WidgetSnapshotProvider
import com.myt.phase3.BatteryAnalyticsUseCase
import com.myt.phase3.CarbonBadgeUseCase
import com.myt.phase3.DataPortability
import com.myt.phase3.DemoLiveCameraClient
import com.myt.phase3.HaRestStateBridge
import com.myt.phase3.HomeAssistantBridge
import com.myt.phase3.LiveCameraClient
import com.myt.phase3.SqlDataPortability
import com.myt.domain.usecase.PoiDataStatusUseCase
import com.myt.domain.usecase.PoiOtaSyncUseCase
import com.myt.domain.usecase.RoadSnapUseCase
import com.myt.domain.usecase.PresenceUseCase
import com.myt.domain.usecase.SpeedCamUseCase
import com.myt.domain.automation.AutomationRepository
import com.myt.domain.automation.ClimateScheduleEngine
import com.myt.domain.automation.ClimateScheduleRepository
import com.myt.domain.automation.LocalAutomationEngine
import com.myt.domain.automation.LocalAutomationRepository
import com.myt.domain.automation.SettingsClimateScheduleRepository
import com.myt.domain.control.DemoVehicleControlGateway
import com.myt.domain.control.FleetVehicleControlGateway
import com.myt.domain.control.SafetyGatedVehicleControl
import com.myt.domain.control.SelectingVehicleControlGateway
import com.myt.domain.control.VehicleControlGateway
import com.myt.domain.usecase.TelemetryUseCase
import com.myt.domain.usecase.VoiceCommandUseCase
import com.myt.platform.DeviceCommunications
import com.myt.platform.PlatformDeviceCommunications
import com.myt.platform.PlatformTextToSpeech
import com.myt.platform.SpeechPlatformRecognizer
import com.myt.platform.SpeechRecognizer
import com.myt.platform.TextToSpeech
import com.myt.domain.usecase.VoiceNavUseCase
import com.myt.phase15.HybridTelemetryStreamClient
import com.myt.phase15.TelemetryStreamClient
import com.myt.data.poi.PoiBootstrapUseCase
import com.myt.ui.GaugeViewModel
import com.myt.ui.analytics.AnalyticsViewModel
import com.myt.ui.debug.DebugLogViewModel
import com.myt.ui.history.HistoryViewModel
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) {
    startKoin {
        appDeclaration()
        modules(platformModule(), *appModules().toTypedArray())
    }
}

fun appModules() = listOf(
    configModule,
    dataModule,
    domainModule,
    viewModelModule,
)

val configModule = module {
    single { TeslaConfigStore() }
    single { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
}

val dataModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single<Settings> { Settings() }
    single { createDatabase(get<DatabaseDriverFactory>()) }
    single<HistoryRepository> { SqlHistoryRepository(get()) }
    single<FleetUsageRepository> { FleetUsageRepositoryImpl(get()) }
    single { ErrorIssueEnqueuer(get(), get(), get()) }
    single {
        DebugLogger(
            settings = get(),
            fileSink = get(),
            onErrorIssue = { tag, message, throwable ->
                get<ErrorIssueEnqueuer>().enqueue(tag, message, throwable)
            },
        )
    }
    single { DebugLogUseCase(get(), get(), get(), get(), get(), get()) }
    single { GitHubIssueClient(get()) }
    single { CrashIssueSyncUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { FleetQuotaUseCase(get(), get(), get()) }
    single { KtorFleetRepository(get(), get(), get(), get(), get(), get()) }
    single<FleetRepository> { get<KtorFleetRepository>() }
    single<TripRecorder> { LocalTripRecorder(get(), get()) }
    single<ChargeSessionRecorder> { LocalChargeSessionRecorder(get(), get()) }
    single { SqlPoiRepository(get()) }
    single { PoiBootstrapUseCase(get()) }
    single<PoiRepository> { PoiSeedRepository(get(), MockPoiRepository()) }
    single<BluetoothRepository> { BluetoothRepositoryImpl(get()) }
    single<TokenRepository> { TokenRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}

val domainModule = module {
    single { SpeedCamEngine(get()) }
    single { TelemetryUseCase(get(), get(), get(), get(), get(), get(), get(), get()) }
    single { PresenceUseCase(get()) }
    single { SpeedCamUseCase(get(), get(), get()) }
    single { VoiceNavUseCase(get(), get(), get()) }
    single<SpeechRecognizer> { SpeechPlatformRecognizer(get()) }
    single<TextToSpeech> { PlatformTextToSpeech(get()) }
    single<DeviceCommunications> { PlatformDeviceCommunications(get()) }
    single { VoiceCommandUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { HistoryUseCase(get()) }
    single { AdaptiveLayoutUseCase() }
    single { DriveSimController(get(), get(), get(), get()) }
    single { AuthUseCase(get(), get(), get(), get(), get(), get(), get()) }
    single { PoiOtaSyncUseCase(get(), get(), get()) }
    single { PoiDataStatusUseCase(get(), get()) }
    single { HaIntegrationConfigStore(get()) }
    single<DataPortability> { SqlDataPortability(get(), get()) }
    single<HomeAssistantBridge> { get<HaRestStateBridge>() }
    single { HaRestStateBridge(get()) }
    single { BatteryAnalyticsUseCase(get()) }
    single { CarbonBadgeUseCase(get()) }
    single { DemoLiveCameraClient() }
    single<LiveCameraClient> { get<DemoLiveCameraClient>() }
    single<AutomationRepository> { LocalAutomationRepository() }
    single<ClimateScheduleRepository> { SettingsClimateScheduleRepository(get()) }
    single<PushNotifier> { InAppPushNotifier() }
    single { LocalAutomationEngine(get(), get(), get()) }
    single {
        ClimateScheduleEngine(
            repository = get(),
            controlGateway = get(),
            settingsRepository = get(),
            pushNotifier = get(),
            scope = get(),
        )
    }
    single<BillingGateway> { LocalBillingGateway(get()) }
    single<WatchCompanionBridge> { InMemoryWatchCompanionBridge() }
    single<WidgetSnapshotProvider> {
        GaugeWidgetSnapshotProvider(
            soc = { get<TelemetryUseCase>().gaugeState.value.socPercent },
            rangeKm = { get<TelemetryUseCase>().gaugeState.value.rangeKm },
            locked = { get<TelemetryUseCase>().gaugeState.value.locked },
            clockMs = { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() },
        )
    }
    single { RoadSnapUseCase(get(), get()) }
    single { DemoVehicleControlGateway(get(), get()) }
    single { FleetVehicleControlGateway(get(), get()) }
    single<VehicleControlGateway> {
        SafetyGatedVehicleControl(
            gateway = SelectingVehicleControlGateway(
                fleet = get(),
                demo = get(),
                telemetryUseCase = get(),
                tokenRepository = get(),
            ),
            isDriving = {
                val state = get<TelemetryUseCase>().gaugeState.value
                state.speedKmh >= 3f || state.gear == com.myt.domain.model.Gear.DRIVE
            },
        )
    }
    single<TelemetryStreamClient> {
        HybridTelemetryStreamClient(
            httpClient = get(),
            fleetRepository = get(),
            scope = get(),
            wssUrlProvider = { get<TeslaConfigStore>().current().telemetryWssUrl },
            debugLogger = get(),
        )
    }
}

val viewModelModule = module {
    viewModelOf(::GaugeViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::DebugLogViewModel)
    viewModelOf(::AnalyticsViewModel)
}

expect fun platformModule(): org.koin.core.module.Module
