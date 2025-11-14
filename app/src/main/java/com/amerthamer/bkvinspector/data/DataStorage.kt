package com.amerthamer.bkvinspector.data

import android.content.Context
import android.content.SharedPreferences
import com.amerthamer.bkvinspector.Driver
import com.amerthamer.bkvinspector.Route
import com.amerthamer.bkvinspector.Inspector
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class DataStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("justicebringer_data", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveDrivers(list: List<Driver>) {
        val json = gson.toJson(list)
        prefs.edit().putString("drivers", json).apply()
    }

    fun loadDrivers(): List<Driver> {
        val json = prefs.getString("drivers", "[]")
        val type = object : TypeToken<List<Driver>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveRoutes(list: List<Route>) {
        val json = gson.toJson(list)
        prefs.edit().putString("routes", json).apply()
    }

    fun loadRoutes(): List<Route> {
        val json = prefs.getString("routes", "[]")
        val type = object : TypeToken<List<Route>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveInspectors(list: List<Inspector>) {
        val json = gson.toJson(list)
        prefs.edit().putString("inspectors", json).apply()
    }

    fun loadInspectors(): List<Inspector> {
        val json = prefs.getString("inspectors", "[]")
        val type = object : TypeToken<List<Inspector>>() {}.type
        return gson.fromJson(json, type)
    }
}
