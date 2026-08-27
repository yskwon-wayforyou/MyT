package com.myt.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.myt.platform.BtConnectionHub

class BtBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        BtConnectionHub.attach(context)
        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) {
                    BtConnectionHub.onAclEvent(false)
                } else if (state == BluetoothAdapter.STATE_ON) {
                    // Re-scan bonded/GATT — do not treat adapter-on alone as vehicle present.
                    BtConnectionHub.emitCurrent()
                }
            }
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val name = deviceName(intent)
                BtConnectionHub.onAclEvent(true, name)
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                BtConnectionHub.onAclEvent(false, deviceName(intent))
            }
        }
    }

    @Suppress("DEPRECATION", "MissingPermission")
    private fun deviceName(intent: Intent): String? {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
        return runCatching { device?.name }.getOrNull()
    }
}
