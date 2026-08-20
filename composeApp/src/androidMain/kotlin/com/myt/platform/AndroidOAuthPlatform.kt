package com.myt.platform

import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.myt.MyTApplication

actual class OAuthPlatform {
    actual fun openAuthorizationUrl(url: String) {
        val context = MyTApplication.instance
        CustomTabsIntent.Builder().build().launchUrl(context, Uri.parse(url))
    }

    companion object {
        const val ACTION_OAUTH_CALLBACK = "com.myt.OAUTH_CALLBACK"
    }
}
