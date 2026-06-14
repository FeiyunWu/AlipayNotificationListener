package com.zfblistener

import android.content.Context
import android.content.SharedPreferences
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import org.json.JSONArray

object LogHelper {

    private const val PREFS_NAME = "app_logs"
    private const val KEY_LOGS = "logs"
    private const val MAX_LOGS = 50

    private val dateFmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    fun add(context: Context, msg: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_LOGS, "[]") ?: "[]")
        val entry = JSONArray().apply {
            put(dateFmt.format(Date()))
            put(msg)
        }
        arr.put(entry)
        while (arr.length() > MAX_LOGS) {
            arr.remove(0)
        }
        prefs.edit().putString(KEY_LOGS, arr.toString()).apply()
    }

    fun get(context: Context): List<Pair<String, String>> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val arr = JSONArray(prefs.getString(KEY_LOGS, "[]") ?: "[]")
        val result = mutableListOf<Pair<String, String>>()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONArray(i)
            result.add(e.getString(0) to e.getString(1))
        }
        return result.reversed()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LOGS, "[]").apply()
    }
}
