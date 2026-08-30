package com.parkassist.pango

import android.content.Context
import android.os.Handler
import android.os.Looper

object DriveEventHandler {
    private val handler = Handler(Looper.getMainLooper())
    private const val VOICE_DELAY_MS = 2_500L

    fun onDriveStarted(context: Context, repository: ParkingRepository) {
        if (repository.isCurrentlyDriving()) return

        repository.setCurrentlyDriving(true)
        FastDriveDetector.stop(context)
        NotificationHelper.cancelFastDetectionNotification(context)
        NotificationHelper.showDriveStartedAlert(context)

        handler.postDelayed({
            TtsHelper.speak(context, "התחלת נסיעה. אל תשכח לסיים את החניה באפליקציית פנגו")
        }, VOICE_DELAY_MS)
    }
}
