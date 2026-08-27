package com.myt.platform

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.myt.MyTApplication

actual class OAuthPlatform {
    actual fun openAuthorizationUrl(url: String) {
        val context = MyTApplication.instance
        val uri = Uri.parse(url)
        runCatching {
            val tabs = CustomTabsIntent.Builder().build()
            tabs.intent.data = uri
            tabs.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(tabs.intent)
        }.recoverCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.getOrThrow()
    }

    companion object {
        const val ACTION_OAUTH_CALLBACK = "com.myt.OAUTH_CALLBACK"
    }
}
