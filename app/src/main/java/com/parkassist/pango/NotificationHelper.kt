package com.parkassist.pango

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val SERVICE_CHANNEL_ID = "parking_service_channel"
    const val ALERT_CHANNEL_ID = "parking_alert_channel"

    const val SERVICE_NOTIFICATION_ID = 1001
    const val STATUS_NOTIFICATION_ID = 1002
    const val BT_DEBUG_NOTIFICATION_ID = 1004

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)

        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "מעקב חניה פעיל",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "מציג שהמעקב אחר נסיעה/חניה פעיל ברקע"
        }

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "התראות חניה",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "התראה כשמתחילים נסיעה - תזכורת לסיים חניה בפנגו"
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alertChannel)
    }

    fun buildServiceNotification(context: Context): android.app.Notification {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("עוזר החניה פעיל")
            .setContentText("עוקב אחרי נסיעה כדי להזכיר לך לסיים חניה בפנגו")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    fun showDriveStartedAlert(context: Context) {
        val openPangoIntent = Intent(context, PangoActionReceiver::class.java).apply {
            action = PangoActionReceiver.ACTION_OPEN_PANGO
        }
        val openPangoPendingIntent = PendingIntent.getBroadcast(
            context, 1, openPangoIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("🚗 הנסיעה התחילה")
            .setContentText("סיימת לסגור את החניה באפליקציית פנגו?")
            .setSmallIcon(android.R.drawable.ic_menu_directions)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_send, "פתח את פנגו", openPangoPendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(STATUS_NOTIFICATION_ID, notification)
    }

    fun showParkedNotification(context: Context) {
        val findParkingIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 2, findParkingIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("🅿️ מיקום החניה נשמר")
            .setContentText("תוכל למצוא את הרכב שלך דרך האפליקציה")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(STATUS_NOTIFICATION_ID, notification)
    }

    fun showFastDetectionActiveNotification(context: Context) {
        val notification = NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("🔵 זוהה חיבור לרכב")
            .setContentText("עוקב אחרי מהירות לזיהוי מהיר של תחילת נסיעה")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(BT_DEBUG_NOTIFICATION_ID, notification)
    }
}
