package com.parkassist.pango

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
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
        val appContext = context.applicationContext

        for (event: ActivityTransitionEvent in result.transitionEvents) {
            if (event.activityType != DetectedActivity.IN_VEHICLE) continue

            when (event.transitionType) {
                com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_ENTER -> {
                    pendingExitRunnable?.let { debounceHandler.removeCallbacks(it) }
                    pendingExitRunnable = null

                    Log.d(TAG, "זוהתה תחילת נסיעה (Activity Recognition)")
                    DriveEventHandler.onDriveStarted(appContext, repository)
                }

                com.google.android.gms.location.ActivityTransition.ACTIVITY_TRANSITION_EXIT -> {
                    Log.d(TAG, "זוהה סיום נסיעה אפשרי - ממתין $EXIT_DEBOUNCE_SECONDS שניות לוודא שזו לא עצירה זמנית")

                    pendingExitRunnable?.let { debounceHandler.removeCallbacks(it) }
                    val runnable = Runnable {
                        Log.d(TAG, "אושר סיום נסיעה בפועל - שומר מיקום חניה")
                        repository.setCurrentlyDriving(false)
                        saveCurrentLocationAsParking(appContext, repository) {}
                        pendingExitRunnable = null
                    }
                    pendingExitRunnable = runnable
                    debounceHandler.postDelayed(runnable, EXIT_DEBOUNCE_SECONDS * 1000L)
                }
            }
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
                    TtsHelper.speak(context, "מיקום החניה נשמר")
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
        private const val EXIT_DEBOUNCE_SECONDS = 45L

        private val debounceHandler = Handler(Looper.getMainLooper())
        private var pendingExitRunnable: Runnable? = null
    }
}
