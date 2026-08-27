package com.myt.ui.gauge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TireRepair
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myt.domain.model.ConnectionStatus
import com.myt.domain.model.TelemetrySource

@Composable
fun StatusIconLabel(
    icon: ImageVector,
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp,
    fontSize: TextUnit = 12.sp,
    fontWeight: FontWeight = FontWeight.Bold,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(iconSize),
        )
        Text(
            label,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
        )
    }
}

object ClusterIcons {
    val battery = Icons.Filled.BatteryFull
    val batteryCharging = Icons.Filled.BatteryChargingFull
    val range = Icons.Filled.Route
    val voice = Icons.Filled.Mic
    val history = Icons.Filled.History
    val more = Icons.Filled.MoreHoriz
    val bluetoothOn = Icons.Filled.Bluetooth
    val bluetoothOff = Icons.Filled.BluetoothDisabled
    val locked = Icons.Filled.Lock
    val unlocked = Icons.Filled.LockOpen
    val climate = Icons.Filled.AcUnit
    val charging = Icons.Filled.EvStation
    val idle = Icons.Filled.ElectricCar
    val map = Icons.Filled.Map
    val place = Icons.Filled.Place
    val navigation = Icons.Filled.Navigation
    val speedCam = Icons.Filled.Speed
    val warning = Icons.Filled.Warning
    val tire = Icons.Filled.TireRepair
    val gMeter = Icons.Filled.SwapVert
    val gps = Icons.Filled.SatelliteAlt
    val fleet = Icons.Filled.Wifi
    val offline = Icons.Filled.WifiOff
    val sleep = Icons.Filled.CloudOff
    val schedule = Icons.Filled.Schedule
    val simulation = Icons.Filled.Science
    val compass = Icons.Filled.Explore
    val info = Icons.Filled.Info
    val list = Icons.AutoMirrored.Filled.List
    val turnLeft = Icons.AutoMirrored.Filled.ArrowBack
    val turnRight = Icons.AutoMirrored.Filled.ArrowForward
}

fun bluetoothIcon(on: Boolean) = if (on) ClusterIcons.bluetoothOn else ClusterIcons.bluetoothOff

fun lockIcon(locked: Boolean?) = when (locked) {
    true -> ClusterIcons.locked
    false -> ClusterIcons.unlocked
    null -> ClusterIcons.locked
}

fun connectionIcon(status: ConnectionStatus) = when (status) {
    ConnectionStatus.FleetConnected -> ClusterIcons.fleet
    ConnectionStatus.BluetoothOnly -> ClusterIcons.bluetoothOn
    ConnectionStatus.Sleeping -> ClusterIcons.sleep
    ConnectionStatus.QuotaHold -> ClusterIcons.warning
    ConnectionStatus.Error, ConnectionStatus.Disconnected -> ClusterIcons.offline
}

fun telemetryIcon(source: TelemetrySource) = when (source) {
    TelemetrySource.Device, TelemetrySource.Degraded -> ClusterIcons.gps
    TelemetrySource.Fleet -> ClusterIcons.fleet
    TelemetrySource.Cache -> ClusterIcons.schedule
    TelemetrySource.None -> ClusterIcons.offline
}
