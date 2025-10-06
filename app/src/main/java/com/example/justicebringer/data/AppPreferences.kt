package com.example.justicebringer.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("justicebringer_prefs", Context.MODE_PRIVATE)

    fun saveDriverFile(uri: String) {
        prefs.edit().putString("driver_file", uri).apply()
    }

    fun getDriverFile(): String? {
        return prefs.getString("driver_file", null)
    }

    fun saveLocationFile(uri: String) {
        prefs.edit().putString("location_file", uri).apply()
    }

    fun getLocationFile(): String? {
        return prefs.getString("location_file", null)
    }

    fun saveInspectorFile(uri: String) {
        prefs.edit().putString("inspector_file", uri).apply()
    }

    fun getInspectorFile(): String? {
        return prefs.getString("inspector_file", null)
    }
}
