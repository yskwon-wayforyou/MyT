package com.myt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.myt.ui.GaugeViewModel
import com.myt.ui.gauge.GaugeScreen
import com.myt.ui.navigation.Route
import com.myt.ui.onboarding.HomeScreen
import com.myt.ui.onboarding.OnboardingScreen
import com.myt.ui.settings.SettingsScreen
import com.myt.ui.theme.MyTTheme
import com.myt.ui.voice.VoiceNavDialog
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    MyTTheme {
        val navController = rememberNavController()
        val gaugeViewModel: GaugeViewModel = koinViewModel()

        val gaugeState by gaugeViewModel.gaugeState.collectAsState()
        val isAuthenticated by gaugeViewModel.isAuthenticated.collectAsState()
        val configuredVin by gaugeViewModel.configuredVin.collectAsState()
        val isConnected = gaugeState.connection != com.myt.domain.model.ConnectionStatus.Disconnected

        LaunchedEffect(Unit) {
            gaugeViewModel.refreshAuthState()
        }

        LaunchedEffect(isConnected) {
            if (isConnected && navController.currentDestination?.route?.contains("Home") == true) {
                navController.navigate(Route.Gauge) {
                    popUpTo(Route.Home) { inclusive = false }
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = Route.Onboarding,
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
                    isConnected = isConnected,
                    onOpenGauge = { navController.navigate(Route.Gauge) },
                    onOpenSettings = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.Gauge> {
                GaugeScreen(
                    viewModel = gaugeViewModel,
                    onVoiceNav = { navController.navigate(Route.VoiceNav) },
                    onSettings = { navController.navigate(Route.Settings) },
                )
            }
            composable<Route.Settings> {
                SettingsScreen(
                    speedUnitKmh = true,
                    onSpeedUnitChange = { gaugeViewModel.setSpeedUnitKmh(it) },
                    onBack = { navController.popBackStack() },
                )
            }
            composable<Route.VoiceNav> {
                var destination by remember { mutableStateOf<String?>(null) }
                var listening by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    destination = gaugeViewModel.recognizeVoiceDestination()
                    listening = false
                }

                VoiceNavDialog(
                    destination = destination,
                    isListening = listening,
                    onConfirm = {
                        destination?.let { gaugeViewModel.sendVoiceDestination(it) }
                        navController.popBackStack()
                    },
                    onCancel = { navController.popBackStack() },
                )
            }
        }
    }
}
