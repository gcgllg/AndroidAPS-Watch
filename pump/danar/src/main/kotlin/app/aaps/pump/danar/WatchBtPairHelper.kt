package app.aaps.pump.danar

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter

/**
 * WATCH PATCH.
 *
 * ColorOS Watch never shows the Bluetooth pairing PIN dialog, so a Dana R
 * (classic SPP, fixed PIN 0000) cannot be paired through system settings.
 * This helper runs discovery, calls createBond() and answers the pairing
 * request programmatically. BLUETOOTH_ADMIN is granted, so setPin() works.
 */
object WatchBtPairHelper {

    @Suppress("MissingPermission", "DEPRECATION")
    fun scan(context: Context, timeoutMs: Long = 15000): List<BluetoothDevice> {
        val bta = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
        val found = LinkedHashMap<String, BluetoothDevice>()
        val receiver = object : BroadcastReceiver() {

            override fun onReceive(c: Context?, i: Intent?) {
                if (i == null) return
                if (i.action != BluetoothDevice.ACTION_FOUND) return
                val d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
                val name = d?.name
                if (d != null && name != null) found[name] = d
            }
        }
        try {
            context.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
        } catch (e: Exception) {
            return emptyList()
        }
        try {
            if (bta.isDiscovering) bta.cancelDiscovery()
            bta.startDiscovery()
            val end = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < end) Thread.sleep(500)
        } finally {
            try {
                bta.cancelDiscovery()
            } catch (ignore: Exception) {
            }
            try {
                context.unregisterReceiver(receiver)
            } catch (ignore: Exception) {
            }
        }
        return found.values.toList()
    }

    @Suppress("MissingPermission", "DEPRECATION")
    fun bond(context: Context, device: BluetoothDevice, pin: String = "0000"): Boolean {
        if (device.bondState == BluetoothDevice.BOND_BONDED) return true
        var done = false
        var ok = false
        val receiver = object : BroadcastReceiver() {

            override fun onReceive(c: Context?, i: Intent?) {
                if (i == null) return
                val d = i.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE) as? BluetoothDevice
                if (d == null || d.address != device.address) return
                if (i.action == BluetoothDevice.ACTION_PAIRING_REQUEST) {
                    try {
                        device.setPin(pin.toByteArray())
                        abortBroadcast()
                    } catch (ignore: Exception) {
                    }
                } else if (i.action == BluetoothDevice.ACTION_BOND_STATE_CHANGED) {
                    val state = i.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    if (state == BluetoothDevice.BOND_BONDED) {
                        ok = true
                        done = true
                    } else if (state == BluetoothDevice.BOND_NONE) {
                        done = true
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_PAIRING_REQUEST)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            priority = IntentFilter.SYSTEM_HIGH_PRIORITY
        }
        try {
            context.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            return false
        }
        try {
            device.createBond()
            val end = System.currentTimeMillis() + 30000
            while (!done && System.currentTimeMillis() < end) Thread.sleep(300)
        } catch (e: Exception) {
            return false
        } finally {
            try {
                context.unregisterReceiver(receiver)
            } catch (ignore: Exception) {
            }
        }
        return ok || device.bondState == BluetoothDevice.BOND_BONDED
    }
}
