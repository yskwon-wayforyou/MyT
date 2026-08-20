package com.myt

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.myt.data.auth.OAuthCallbackBus
import com.myt.domain.usecase.AuthUseCase
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val authUseCase: AuthUseCase by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleOAuthIntent(intent)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleOAuthIntent(intent)
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
