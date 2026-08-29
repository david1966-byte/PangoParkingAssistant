package com.parkassist.pango

import android.app.PendingIntent
import android.app.Service
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.util.Log
import com.google.android.gms.location.ActivityRecognition
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionRequest
import com.google.android.gms.location.DetectedActivity

class ParkingMonitorService : Service() {

    private val carBluetoothReceiver = CarBluetoothReceiver()

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, NotificationHelper.buildServiceNotification(this))
        registerActivityTransitions()
        TtsHelper.init(this)

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(carBluetoothReceiver, filter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        unregisterActivityTransitions()
        TtsHelper.shutdown()
        FastDriveDetector.stop(this)
        try {
            unregisterReceiver(carBluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            // כבר לא רשום - אין בעיה
        }
    }

    private fun getTransitionPendingIntent(): PendingIntent {
        val intent = Intent(this, ActivityTransitionReceiver::class.java)
        return PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun registerActivityTransitions() {
        val transitions = listOf(
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build(),
            ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build()
        )

        val request = ActivityTransitionRequest(transitions)
        val pendingIntent = getTransitionPendingIntent()

        val task = ActivityRecognition.getClient(this)
            .requestActivityTransitionUpdates(request, pendingIntent)

        task.addOnSuccessListener {
            Log.d(TAG, "נרשמנו בהצלחה למעקב אחר מעברי נסיעה/חניה")
        }
        task.addOnFailureListener { e ->
            Log.e(TAG, "נכשלה הרשמה למעקב פעילות: ${e.message}")
        }
    }

    private fun unregisterActivityTransitions() {
        val pendingIntent = getTransitionPendingIntent()
        ActivityRecognition.getClient(this).removeActivityTransitionUpdates(pendingIntent)
    }

    companion object {
        private const val TAG = "ParkingMonitorService"
    }
}
