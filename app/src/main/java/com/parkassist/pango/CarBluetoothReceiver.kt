package com.parkassist.pango

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat

class CarBluetoothReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = ParkingRepository(context)
        val configuredAddress = repository.getCarBluetoothDeviceAddress()

        if (configuredAddress != null) {
            val device = getDeviceFromIntent(intent)
            val connectedAddress = getDeviceAddressSafely(context, device)
            if (connectedAddress == null || connectedAddress != configuredAddress) {
                return
            }
        }

        when (intent.action) {
            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                if (!repository.isCurrentlyDriving()) {
                    Log.d(TAG, "התחברות בלוטות' רלוונטית זוהתה בזמן חניה - מפעיל בדיקת מהירות מהירה")
                    NotificationHelper.showFastDetectionActiveNotification(context)
                    FastDriveDetector.start(context, repository)
                }
            }
            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                Log.d(TAG, "בלוטות' התנתק - עוצר בדיקת מהירות מהירה")
                FastDriveDetector.stop(context)
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun getDeviceFromIntent(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun getDeviceAddressSafely(context: Context, device: BluetoothDevice?): String? {
        if (device == null) return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return null
        }
        return try {
            device.address
        } catch (e: SecurityException) {
            null
        }
    }

    companion object {
        private const val TAG = "CarBluetoothReceiver"
    }
}
