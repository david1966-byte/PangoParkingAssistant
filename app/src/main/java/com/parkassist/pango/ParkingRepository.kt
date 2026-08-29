package com.parkassist.pango

import android.content.Context
import android.content.SharedPreferences

class ParkingRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isMonitoringEnabled(): Boolean = prefs.getBoolean(KEY_MONITORING_ENABLED, false)

    fun setMonitoringEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_MONITORING_ENABLED, enabled).apply()
    }

    fun isCurrentlyDriving(): Boolean = prefs.getBoolean(KEY_IS_DRIVING, false)

    fun setCurrentlyDriving(driving: Boolean) {
        prefs.edit().putBoolean(KEY_IS_DRIVING, driving).apply()
    }

    fun saveParkingLocation(lat: Double, lng: Double) {
        prefs.edit()
            .putFloat(KEY_LAT, lat.toFloat())
            .putFloat(KEY_LNG, lng.toFloat())
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .putBoolean(KEY_HAS_LOCATION, true)
            .apply()
    }

    fun hasParkingLocation(): Boolean = prefs.getBoolean(KEY_HAS_LOCATION, false)

    fun getParkingLocation(): Pair<Double, Double>? {
        if (!hasParkingLocation()) return null
        val lat = prefs.getFloat(KEY_LAT, 0f).toDouble()
        val lng = prefs.getFloat(KEY_LNG, 0f).toDouble()
        return Pair(lat, lng)
    }

    fun getParkingTimestamp(): Long = prefs.getLong(KEY_TIMESTAMP, 0L)

    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun saveCarBluetoothDevice(address: String, name: String) {
        prefs.edit()
            .putString(KEY_CAR_BT_ADDRESS, address)
            .putString(KEY_CAR_BT_NAME, name)
            .apply()
    }

    fun getCarBluetoothDeviceAddress(): String? = prefs.getString(KEY_CAR_BT_ADDRESS, null)

    fun getCarBluetoothDeviceName(): String? = prefs.getString(KEY_CAR_BT_NAME, null)

    fun clearCarBluetoothDevice() {
        prefs.edit().remove(KEY_CAR_BT_ADDRESS).remove(KEY_CAR_BT_NAME).apply()
    }

    companion object {
        private const val PREFS_NAME = "pango_parking_prefs"
        private const val KEY_MONITORING_ENABLED = "monitoring_enabled"
        private const val KEY_IS_DRIVING = "is_driving"
        private const val KEY_LAT = "parking_lat"
        private const val KEY_LNG = "parking_lng"
        private const val KEY_TIMESTAMP = "parking_timestamp"
        private const val KEY_HAS_LOCATION = "has_location"
        private const val KEY_CAR_BT_ADDRESS = "car_bt_address"
        private const val KEY_CAR_BT_NAME = "car_bt_name"
    }
}
