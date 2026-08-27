package com.myt.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.myt.platform.WidgetSnapshotPublisher

class MyTGlanceWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotPublisher.read(context)
        provideContent {
            GlanceTheme {
                WidgetContent(
                    soc = snapshot?.socPercent ?: 0,
                    rangeKm = snapshot?.rangeKm ?: 0,
                    locked = snapshot?.locked,
                )
            }
        }
    }
}

@Composable
private fun WidgetContent(soc: Int, rangeKm: Int, locked: Boolean?) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0xFF121820)))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = "MyT",
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
            ),
        )
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "$soc%",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF30D158)),
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp,
                ),
            )
            Text(
                text = "  ·  ${rangeKm} km",
                style = TextStyle(
                    color = ColorProvider(Color(0xBFFFFFFF)),
                    fontSize = 14.sp,
                ),
            )
        }
        Text(
            text = when (locked) {
                true -> "잠김"
                false -> "열림"
                null -> "—"
            },
            style = TextStyle(
                color = ColorProvider(Color(0xFF64D2FF)),
                fontSize = 12.sp,
            ),
            modifier = GlanceModifier.padding(top = 4.dp),
        )
    }
}

class MyTGlanceWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MyTGlanceWidget()
}
