package com.myt.service

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BtBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            BluetoothAdapter.ACTION_STATE_CHANGED,
            android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED,
            android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED,
            -> {
                // TODO: Notify PresenceService / BluetoothRepository
            }
        }
    }
}
