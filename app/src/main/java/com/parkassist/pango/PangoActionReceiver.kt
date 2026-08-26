package com.parkassist.pango

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** מטפל בלחיצה על כפתור "פתח את פנגו" בתוך ההתראה */
class PangoActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_OPEN_PANGO) {
            PangoLauncher.openPango(context)
        }
    }

    companion object {
        const val ACTION_OPEN_PANGO = "com.parkassist.pango.ACTION_OPEN_PANGO"
    }
}
