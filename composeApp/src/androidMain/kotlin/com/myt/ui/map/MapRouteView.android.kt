package com.myt.ui.map

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun MapRouteView(
    polylineEncoded: String?,
    modifier: Modifier,
) {
    val html = remember(polylineEncoded) {
        buildOsmMapHtml(decodeRoutePoints(polylineEncoded))
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
        },
    )
}
