package com.myt.ui.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** M21 — Map route display (OSM tiles on Android/iOS, canvas fallback elsewhere). */
@Composable
expect fun MapRouteView(
    polylineEncoded: String?,
    modifier: Modifier = Modifier,
)
