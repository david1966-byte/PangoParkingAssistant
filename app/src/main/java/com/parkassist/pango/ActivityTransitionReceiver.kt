package com.parkassist.pango

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.ActivityTransitionEvent
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationServices

class ActivityTransitionReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val repository = ParkingRepository(context)

        val pendingResult = goAsync()
        var pendingOps = 0

        fun opDone() {
            pendingOps--
            if (pendingOps <= 0) {
                pendingResult.finish()
            }
        }

        for (event: ActivityTransitionEvent in result.transitionEvents) {
            if (event.activityType != DetectedActivity.IN_VEHICLE) continue

            when (event.transitionType) {
                com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                    Log.d(TAG, "זוהתה תחילת נסיעה (Activity Recognition)")
                    DriveEventHandler.onDriveStarted(context, repository)
                }

                com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                    Log.d(TAG, "זוהה סיום נסיעה - שומר מיקום חניה")
                    repository.setCurrentlyDriving(false)

                    pendingOps++
                    saveCurrentLocationAsParking(context, repository) { opDone() }
                }
            }
        }

        if (pendingOps == 0) {
            pendingResult.finish()
        }
    }

    @SuppressLint("MissingPermission")
    private fun saveCurrentLocationAsParking(
        context: Context,
        repository: ParkingRepository,
        onDone: () -> Unit
    ) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation) {
            Log.w(TAG, "אין הרשאת מיקום - לא ניתן לשמור את נקודת החניה")
            onDone()
            return
        }

        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    repository.saveParkingLocation(location.latitude, location.longitude)
                    NotificationHelper.showParkedNotification(context)
                } else {
                    Log.w(TAG, "lastLocation חזר null - אין נקודת מיקום אחרונה זמינה")
                }
                onDone()
            }
            .addOnFailureListener {
                Log.e(TAG, "שליפת מיקום נכשלה: ${it.message}")
                onDone()
            }
    }

    companion object {
        private const val TAG = "ActivityTransitionRcvr"
    }
}
