package com.myt

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.myt.data.auth.DeepLinkBus
import com.myt.data.auth.OAuthCallbackBus
import com.myt.debug.DebugLogger
import com.myt.domain.usecase.AuthUseCase
import com.myt.domain.usecase.FleetQuotaUseCase
import com.myt.platform.OrientationController
import com.myt.service.VehiclePresenceLauncher
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val authUseCase: AuthUseCase by inject()
    private val quotaUseCase: FleetQuotaUseCase by inject()
    private val debugLogger: DebugLogger by inject()
    private var orientationController: OrientationController? = null

    private val locationPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* Telemetry / map use GPS when granted */ }

    private val micPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* optional — voice screen shows error if denied */ }

    private val btPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* PresenceService starts after grant on next monitoring cycle */ }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* LocalNotificationPlatform posts when granted */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        orientationController = OrientationController(this).also { it.start() }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
        requestLocationPermissionsIfNeeded()
        requestBluetoothPermissionsIfNeeded()
        requestNotificationPermissionIfNeeded()
        handleOAuthIntent(intent)
        handleDeepLinkIntent(intent)
        setContent {
            App()
        }
    }

    override fun onDestroy() {
        orientationController?.stop()
        orientationController = null
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestLocationPermissionsIfNeeded() {
        val needed = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ).filter {
            ContextCompat.checkSelfPermission(this, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            locationPermissions.launch(needed.toTypedArray())
        }
    }

    private fun requestBluetoothPermissionsIfNeeded() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S) return
        val needed = listOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
        ).filter {
            ContextCompat.checkSelfPermission(this, it) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            btPermissions.launch(needed.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        quotaUseCase.appInForeground = true
        debugLogger.d("Lifecycle", "MainActivity onResume")
    }

    override fun onPause() {
        debugLogger.d("Lifecycle", "MainActivity onPause")
        quotaUseCase.appInForeground = false
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
        handleDeepLinkIntent(intent)
    }

    private fun handleDeepLinkIntent(intent: Intent?) {
        val route = intent?.getStringExtra(VehiclePresenceLauncher.EXTRA_ROUTE) ?: return
        lifecycleScope.launch {
            DeepLinkBus.emit(route)
        }
        intent.removeExtra(VehiclePresenceLauncher.EXTRA_ROUTE)
    }

    private fun handleOAuthIntent(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme != "myt" || uri.host != "auth") return
        val code = uri.getQueryParameter("code") ?: return
        lifecycleScope.launch {
            authUseCase.handleOAuthCallback(code)
                .onSuccess { OAuthCallbackBus.emitCode(code) }
        }
        intent?.data = null
    }
}
