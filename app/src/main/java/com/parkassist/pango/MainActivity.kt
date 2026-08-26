package com.parkassist.pango

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.parkassist.pango.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: ParkingRepository

    private val permissionsLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        if (allGranted) {
            startMonitoring()
        } else {
            binding.statusText.text = "לא ניתן להפעיל מעקב בלי אישור ההרשאות. אפשר לאשר אותן בהגדרות האפליקציה."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = ParkingRepository(this)
        NotificationHelper.createChannels(this)

        binding.toggleMonitoringButton.setOnClickListener {
            if (repository.isMonitoringEnabled()) {
                stopMonitoring()
            } else {
                requestPermissionsAndStart()
            }
        }

        binding.findParkingButton.setOnClickListener {
            openNavigationToParkingSpot()
        }

        binding.openPangoButton.setOnClickListener {
            PangoLauncher.openPango(this)
        }

        updateUi()
    }

    override fun onResume() {
        super.onResume()
        updateUi()
    }

    private fun requiredPermissions(): Array<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        // הרשאת מיקום ברקע נדרשת בנפרד בגרסאות חדשות - מטופלת אחרי שאר ההרשאות מאושרות
        return permissions.toTypedArray()
    }

    private fun hasAllPermissions(): Boolean {
        return requiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissionsAndStart() {
        if (hasAllPermissions()) {
            requestBackgroundLocationIfNeeded()
        } else {
            permissionsLauncher.launch(requiredPermissions())
        }
    }

    private fun requestBackgroundLocationIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val hasBackground = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasBackground) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                    BACKGROUND_LOCATION_REQUEST_CODE
                )
                return
            }
        }
        startMonitoring()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == BACKGROUND_LOCATION_REQUEST_CODE) {
            // גם אם המשתמש דחה מיקום-ברקע, נפעיל בכל זאת עם הרשאות המיקום הרגילות
            // (המעקב עלול להיות פחות אמין כשהאפליקציה סגורה לגמרי)
            startMonitoring()
        }
    }

    private fun startMonitoring() {
        repository.setMonitoringEnabled(true)
        val serviceIntent = Intent(this, ParkingMonitorService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
        updateUi()
    }

    private fun stopMonitoring() {
        repository.setMonitoringEnabled(false)
        stopService(Intent(this, ParkingMonitorService::class.java))
        updateUi()
    }

    private fun openNavigationToParkingSpot() {
        val location = repository.getParkingLocation()
        if (location == null) {
            binding.statusText.text = "עדיין אין מיקום חניה שמור. הוא יישמר אוטומטית בפעם הבאה שתחנה."
            return
        }
        val (lat, lng) = location
        val uri = Uri.parse("google.navigation:q=$lat,$lng&mode=w")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        } else {
            // Google Maps לא מותקן - ניפול חזרה לקישור מפה כללי בדפדפן
            val webUri = Uri.parse("https://maps.google.com/?q=$lat,$lng")
            startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    private fun updateUi() {
        val monitoring = repository.isMonitoringEnabled()
        binding.toggleMonitoringButton.text =
            if (monitoring) "כבה מעקב נסיעה/חניה" else "הפעל מעקב נסיעה/חניה"

        binding.statusText.text = if (monitoring) {
            if (repository.isCurrentlyDriving()) "מעקב פעיל · הרכב כרגע בנסיעה"
            else "מעקב פעיל · הרכב במצב חניה"
        } else {
            "המעקב כבוי"
        }

        val location = repository.getParkingLocation()
        if (location != null) {
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("iw", "IL"))
            val time = sdf.format(Date(repository.getParkingTimestamp()))
            binding.parkingInfoText.text =
                "מקום חניה אחרון נשמר: ${"%.5f".format(location.first)}, ${"%.5f".format(location.second)}\nבתאריך: $time"
        } else {
            binding.parkingInfoText.text = "טרם נשמר מיקום חניה"
        }
    }

    companion object {
        private const val BACKGROUND_LOCATION_REQUEST_CODE = 501
    }
}
