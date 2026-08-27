package com.myt

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.myt.domain.history.TripHistoryItem
import com.myt.phase2.InAppToastBus
import com.myt.ui.AppStateMachine
import com.myt.ui.GaugeViewModel
import com.myt.ui.analytics.AnalyticsScreen
import com.myt.ui.analytics.AnalyticsViewModel
import com.myt.ui.commercial.CommercialHubScreen
import com.myt.ui.debug.DebugLogScreen
import com.myt.ui.debug.DebugLogViewModel
import com.myt.ui.gauge.GaugeScreen
import com.myt.ui.history.HistoryScreen
import com.myt.ui.history.HistoryViewModel
import com.myt.ui.history.TripRouteScreen
import com.myt.ui.navigation.Route
import com.myt.ui.onboarding.HomeScreen
import com.myt.ui.onboarding.OnboardingScreen
import com.myt.ui.onboarding.TeslaSplash
import com.myt.ui.settings.SettingsScreen
import com.myt.ui.theme.MyTTheme
import com.myt.ui.voice.VoiceAssistantDialog
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val gaugeViewModel: GaugeViewModel = koinViewModel()
    val darkTheme by gaugeViewModel.darkTheme.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        InAppToastBus.toasts.collect { toast ->
            snackbarHostState.showSnackbar("${toast.title}: ${toast.body}")
        }
    }

    MyTTheme(darkTheme = darkTheme) {
        Box(Modifier.fillMaxSize()) {
            val navController = rememberNavController()

            val gaugeState by gaugeViewModel.gaugeState.collectAsState()
            val isAuthenticated by gaugeViewModel.isAuthenticated.collectAsState()
            val configuredVin by gaugeViewModel.configuredVin.collectAsState()
            val sessionReady by gaugeViewModel.sessionReady.collectAsState()
            val onboardingComplete by gaugeViewModel.onboardingComplete.collectAsState()
            val bluetoothPresent by gaugeViewModel.bluetoothPresent.collectAsState()
            val isFleetConnected = gaugeState.connection == com.myt.domain.model.ConnectionStatus.FleetConnected ||
                gaugeState.connection == com.myt.domain.model.ConnectionStatus.Sleeping

            LaunchedEffect(Unit) {
                gaugeViewModel.refreshAuthState()
            }

            if (!sessionReady) {
                TeslaSplash()
            } else {
                val startDestination = AppStateMachine.startDestination(onboardingComplete)

                LaunchedEffect(onboardingComplete, bluetoothPresent, isFleetConnected) {
                    if (AppStateMachine.shouldAutoOpenGauge(onboardingComplete, bluetoothPresent, isFleetConnected)) {
                        navController.navigate(Route.Gauge) {
                            launchSingleTop = true
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = startDestination,
                ) {
                    composable<Route.Onboarding> {
                        OnboardingScreen(
                            isAuthenticated = isAuthenticated,
                            configuredVin = configuredVin,
                            oauthConfigured = gaugeViewModel.oauthConfigured,
                            onTeslaLogin = { gaugeViewModel.startTeslaLogin() },
                            onComplete = { vin ->
                                gaugeViewModel.completeOnboarding(vin)
                                navController.navigate(Route.Home) {
                                    popUpTo(Route.Onboarding) { inclusive = true }
                                }
                            },
                        )
                    }
                    composable<Route.Home> {
                        HomeScreen(
                            isConnected = isFleetConnected || bluetoothPresent,
                            onOpenGauge = { navController.navigate(Route.Gauge) },
                            onOpenHistory = { navController.navigate(Route.History) },
                            onOpenSettings = { navController.navigate(Route.Settings) },
                        )
                    }
                    composable<Route.Gauge> {
                        GaugeScreen(
                            viewModel = gaugeViewModel,
                            onVoiceNav = { navController.navigate(Route.VoiceNav) },
                            onHistory = { navController.navigate(Route.History) },
                            onSettings = { navController.navigate(Route.Settings) },
                            onDebug = { navController.navigate(Route.DebugLogs) },
                            onAnalytics = { navController.navigate(Route.Analytics) },
                            onCommercial = { navController.navigate(Route.Commercial) },
                        )
                    }
                    composable<Route.Analytics> {
                        val analyticsViewModel: AnalyticsViewModel = koinViewModel()
                        AnalyticsScreen(
                            viewModel = analyticsViewModel,
                            configuredVin = configuredVin,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<Route.Commercial> {
                        CommercialHubScreen(
                            billing = koinInject(),
                            watchBridge = koinInject(),
                            widgetProvider = koinInject(),
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<Route.History> {
                        val historyViewModel: HistoryViewModel = koinViewModel()
                        HistoryScreen(
                            viewModel = historyViewModel,
                            onBack = { navController.popBackStack() },
                            onOpenTripRoute = { tripId ->
                                navController.navigate(Route.TripRoute(tripId))
                            },
                        )
                    }
                    composable<Route.TripRoute> { entry ->
                        val tripId = entry.toRoute<Route.TripRoute>().tripId
                        val historyViewModel: HistoryViewModel = koinViewModel()
                        var trip by remember { mutableStateOf<TripHistoryItem?>(null) }
                        LaunchedEffect(tripId) {
                            trip = historyViewModel.loadTrip(tripId)
                        }
                        trip?.let {
                            TripRouteScreen(
                                trip = it,
                                onBack = { navController.popBackStack() },
                            )
                        } ?: TeslaSplash()
                    }
                    composable<Route.Settings> {
                        val teslaConfig by gaugeViewModel.teslaConfig.collectAsState()
                        val haConfig by gaugeViewModel.haIntegrationConfig.collectAsState()
                        val speedUnitKmh by gaugeViewModel.speedUnitKmh.collectAsState()
                        val gaugePrefs by gaugeViewModel.gaugePrefs.collectAsState()
                        SettingsScreen(
                            teslaConfig = teslaConfig,
                            speedUnitKmh = speedUnitKmh,
                            darkTheme = darkTheme,
                            gaugePrefs = gaugePrefs,
                            onSpeedUnitChange = { gaugeViewModel.setSpeedUnitKmh(it) },
                            onDarkThemeChange = { gaugeViewModel.setDarkTheme(it) },
                            onGaugePrefsChange = { gaugeViewModel.updateGaugePrefs(it) },
                            onSaveTeslaConfig = { gaugeViewModel.saveTeslaConfig(it) },
                            haConfig = haConfig,
                            onSaveHaConfig = { gaugeViewModel.saveHaConfig(it) },
                            onOpenDebugLogs = { navController.navigate(Route.DebugLogs) },
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<Route.DebugLogs> {
                        val debugLogViewModel: DebugLogViewModel = koinViewModel()
                        DebugLogScreen(
                            viewModel = debugLogViewModel,
                            onBack = { navController.popBackStack() },
                        )
                    }
                    composable<Route.VoiceNav> {
                        var transcript by remember { mutableStateOf<String?>(null) }
                        var status by remember { mutableStateOf("듣는 중…") }
                        var listening by remember { mutableStateOf(true) }

                        LaunchedEffect(Unit) {
                            when (val result = gaugeViewModel.executeVoiceCommand()) {
                                is com.myt.domain.usecase.VoiceCommandResult.Speak -> {
                                    transcript = result.message
                                    status = "읽어 드렸습니다"
                                    listening = false
                                }
                                is com.myt.domain.usecase.VoiceCommandResult.Navigate -> {
                                    listening = false
                                    navController.popBackStack()
                                    when (result.route) {
                                        "history" -> navController.navigate(Route.History)
                                        "settings" -> navController.navigate(Route.Settings)
                                        else -> Unit
                                    }
                                }
                                is com.myt.domain.usecase.VoiceCommandResult.Sent -> {
                                    status = "완료"
                                    listening = false
                                    navController.popBackStack()
                                }
                                is com.myt.domain.usecase.VoiceCommandResult.Failed -> {
                                    status = result.message
                                    listening = false
                                }
                            }
                        }

                        VoiceAssistantDialog(
                            transcript = transcript,
                            status = status,
                            isListening = listening,
                            onDismiss = { navController.popBackStack() },
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
            )
        }
    }
}
