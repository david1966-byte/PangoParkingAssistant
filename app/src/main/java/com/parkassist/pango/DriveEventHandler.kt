package com.parkassist.pango

import android.content.Context

object DriveEventHandler {
    fun onDriveStarted(context: Context, repository: ParkingRepository) {
        if (repository.isCurrentlyDriving()) return

        repository.setCurrentlyDriving(true)
        FastDriveDetector.stop(context)
        NotificationHelper.cancelFastDetectionNotification(context)
        NotificationHelper.showDriveStartedAlert(context)
        TtsHelper.speak(context, "התחלת נסיעה. אל תשכח לסיים את החניה באפליקציית פנגו")
    }
}
