package com.parkassist.pango

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * חשוב לדעת: אין ל-Pango API ציבורי שמאפשר לאפליקציה חיצונית לסיים חניה באופן
 * אוטומטי בתוך פנגו. מה שאפשר לעשות זה לפתוח את אפליקציית פנגו כדי שהמשתמש
 * יסיים את החניה בעצמו בלחיצה אחת.
 */
object PangoLauncher {

    private val KNOWN_PANGO_PACKAGES = listOf(
        "com.unicell.pangoandroid",
        "com.pango.customerapp",
        "il.co.pango.pango"
    )

    private const val PLAY_STORE_SEARCH_URL =
        "https://play.google.com/store/search?q=Pango&c=apps"

    fun openPango(context: Context) {
        val packageManager = context.packageManager

        val installedPackage = KNOWN_PANGO_PACKAGES.firstOrNull { pkg ->
            try {
                packageManager.getPackageInfo(pkg, 0)
                true
            } catch (e: Exception) {
                false
            }
        }

        if (installedPackage != null) {
            val launchIntent = packageManager.getLaunchIntentForPackage(installedPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return
            }
        }

        // פנגו לא נמצאה במכשיר / שם החבילה לא תואם - נפתח את חנות Google Play לחיפוש
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(PLAY_STORE_SEARCH_URL))
        browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(browserIntent)
    }
}
