package com.myt.di

import com.myt.config.TeslaConfig
import com.myt.config.loadTeslaConfig
import com.myt.data.bluetooth.BluetoothRepositoryImpl
import com.myt.data.fleet.KtorFleetRepository
import com.myt.data.poi.MockPoiRepository
import com.myt.data.settings.SettingsRepositoryImpl
import com.myt.data.token.TokenRepositoryImpl
import com.myt.domain.SpeedCamEngine
import com.myt.domain.repository.BluetoothRepository
import com.myt.domain.repository.FleetRepository
import com.myt.domain.repository.PoiRepository
import com.myt.domain.repository.SettingsRepository
import com.myt.domain.repository.TokenRepository
import com.myt.domain.usecase.AdaptiveLayoutUseCase
import com.myt.domain.usecase.AuthUseCase
import com.myt.domain.usecase.PresenceUseCase
import com.myt.domain.usecase.SpeedCamUseCase
import com.myt.domain.usecase.TelemetryUseCase
import com.myt.domain.usecase.VoiceNavUseCase
import com.myt.ui.GaugeViewModel
import com.russhwolf.settings.Settings
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
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
    single { loadTeslaConfig() }
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
    single { KtorFleetRepository(get(), get(), get()) }
    single<FleetRepository> { get<KtorFleetRepository>() }
    single<PoiRepository> { MockPoiRepository() }
    single<BluetoothRepository> { BluetoothRepositoryImpl(get()) }
    single<TokenRepository> { TokenRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
}

val domainModule = module {
    single { SpeedCamEngine(get()) }
    single { TelemetryUseCase(get(), get(), get()) }
    single { PresenceUseCase(get()) }
    single { SpeedCamUseCase(get()) }
    single { VoiceNavUseCase(get(), get(), get()) }
    single { AdaptiveLayoutUseCase() }
    single { AuthUseCase(get(), get(), get(), get(), get(), get()) }
}

val viewModelModule = module {
    viewModelOf(::GaugeViewModel)
}

expect fun platformModule(): org.koin.core.module.Module
