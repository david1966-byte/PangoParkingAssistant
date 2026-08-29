package com.parkassist.pango

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

object FastDriveDetector {
    private const val TAG = "FastDriveDetector"
    private const val POLL_INTERVAL_MS = 15_000L
    private const val SPEED_THRESHOLD_KMH = 15f

    @Volatile
    private var isActive = false
    private var locationCallback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start(context: Context, repository: ParkingRepository) {
        if (isActive) return
        isActive = true

        val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        val request = LocationRequest.Builder(POLL_INTERVAL_MS)
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMinUpdateIntervalMillis(POLL_INTERVAL_MS)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                if (!location.hasSpeed()) return

                val speedKmh = location.speed * 3.6f
                Log.d(TAG, "מהירות נמדדת: ${"%.1f".format(speedKmh)} קמ\"ש")

                if (speedKmh >= SPEED_THRESHOLD_KMH) {
                    Log.d(TAG, "זוהתה תנועה מהירה - מפעיל אירוע תחילת נסיעה")
                    DriveEventHandler.onDriveStarted(context, repository)
                }
            }
        }
        locationCallback = callback

        try {
            client.requestLocationUpdates(request, callback, Looper.getMainLooper())
            Log.d(TAG, "בדיקת מהירות מהירה הופעלה")
        } catch (e: SecurityException) {
            Log.e(TAG, "אין הרשאת מיקום - לא ניתן להפעיל בדיקת מהירות מהירה: ${e.message}")
            isActive = false
        }
    }

    fun stop(context: Context) {
        val callback = locationCallback ?: run { isActive = false; return }
        val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
        client.removeLocationUpdates(callback)
        locationCallback = null
        isActive = false
        Log.d(TAG, "בדיקת מהירות מהירה הופסקה")
    }
}
