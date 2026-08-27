package com.myt.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.myt.domain.simulation.DriveSimController
import com.myt.domain.simulation.DrivingSimulationId
import org.koin.core.context.GlobalContext

/**
 * adb shell am broadcast -a com.myt.action.DRIVE_SIM --es scenario charging_parked
 * adb shell am broadcast -a com.myt.action.DRIVE_SIM --es scenario stop
 */
class DriveSimBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION) return
        val controller = runCatching { GlobalContext.get().get<DriveSimController>() }.getOrNull()
            ?: return
        when (intent.getStringExtra(EXTRA_SCENARIO)?.lowercase()) {
            "stop", "none", "" -> controller.stop()
            "charging_parked", "charging" ->
                controller.start(DrivingSimulationId.ChargingParkedSuwon)
            "speed_cam_l3", "approach_speed_cam" ->
                controller.start(DrivingSimulationId.ApproachSpeedCamL3)
            "highway_nav", "nav" ->
                controller.start(DrivingSimulationId.HighwayWithNav)
        }
    }

    companion object {
        const val ACTION = "com.myt.action.DRIVE_SIM"
        const val EXTRA_SCENARIO = "scenario"
    }
}
