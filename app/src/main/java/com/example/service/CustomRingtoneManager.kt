package com.example.service

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

data class CustomRingtone(
    val name: String,
    val frequency: Float,
    val lfoSpeed: Float,
    val waveform: String, // "SINE", "SQUARE", "TRIANGLE", "CHIRP"
    val isCustom: Boolean = true
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("frequency", frequency.toDouble())
            put("lfoSpeed", lfoSpeed.toDouble())
            put("waveform", waveform)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): CustomRingtone {
            return CustomRingtone(
                name = obj.getString("name"),
                frequency = obj.getDouble("frequency").toFloat(),
                lfoSpeed = obj.getDouble("lfoSpeed").toFloat(),
                waveform = obj.optString("waveform", "SINE")
            )
        }
    }
}

object CustomRingtoneManager {
    private const val PREFS_NAME = "custom_ringtone_prefs"
    private const val KEY_RINGTONES = "custom_ringtones_json"

    private val defaultRingtones = listOf(
        "GLYPH RAPID",
        "VOX UNISON",
        "TEENAGE AMBIENT",
        "SILENT STATE",
        "RETRO BEATS",
        "DIGITAL CHIRP"
    )

    private val customRingtonesList = mutableListOf<CustomRingtone>()

    fun init(context: Context) {
        customRingtonesList.clear()
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_RINGTONES, null)
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    customRingtonesList.add(CustomRingtone.fromJsonObject(arr.getJSONObject(i)))
                }
            } catch (e: Exception) {
                Log.e("CustomRingtoneManager", "Failed to load custom ringtones", e)
            }
        }
    }

    fun getAllRingtones(): List<String> {
        val list = mutableListOf<String>()
        list.addAll(defaultRingtones)
        list.addAll(customRingtonesList.map { it.name })
        return list
    }

    fun getCustomRingtone(name: String): CustomRingtone? {
        return customRingtonesList.find { it.name.uppercase() == name.uppercase() }
    }

    fun addCustomRingtone(context: Context, ringtone: CustomRingtone) {
        if (customRingtonesList.any { it.name.uppercase() == ringtone.name.uppercase() }) {
            customRingtonesList.removeAll { it.name.uppercase() == ringtone.name.uppercase() }
        }
        customRingtonesList.add(ringtone)
        save(context)
    }

    fun removeCustomRingtone(context: Context, name: String) {
        customRingtonesList.removeAll { it.name.uppercase() == name.uppercase() }
        save(context)
    }

    private fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray()
        for (item in customRingtonesList) {
            arr.put(item.toJsonObject())
        }
        prefs.edit().putString(KEY_RINGTONES, arr.toString()).apply()
    }
}
