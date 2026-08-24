package com.cryonum.managers

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.cryonum.items.HistoryItem
import java.util.Objects

object HistoryManager {

    private const val PREFS_NAME = "history_prefs"
    private const val KEY_HISTORY_LIST = "history_list"
    private const val NINETY_DAYS_MILLIS = 90L * 24L * 60L * 60L * 1000L
    private const val MAX_HISTORY_SIZE = 50

    private var cachedHistory: MutableList<HistoryItem>? = null

    @JvmStatic
    fun saveHistory(context: Context?, history: List<HistoryItem>) {
        if (context == null) return

        val trimmedHistory = if (history.size > MAX_HISTORY_SIZE) {
            history.take(MAX_HISTORY_SIZE)
        } else {
            history
        }

        cachedHistory = trimmedHistory.toMutableList()
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = Gson().toJson(trimmedHistory)
        prefs.edit { putString(KEY_HISTORY_LIST, json) }
    }

    @JvmStatic
    fun loadHistory(context: Context?): MutableList<HistoryItem> {
        if (context == null) return mutableListOf()
        
        cachedHistory?.let { return it }

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY_LIST, null)
        val now = System.currentTimeMillis()

        if (json == null) {
            return mutableListOf()
        }

        val type = object : TypeToken<ArrayList<HistoryItem>>() {}.type
        val history: MutableList<HistoryItem>? = Gson().fromJson(json, type)
        val resultHistory = history ?: mutableListOf()

        val filtered = resultHistory.filter { 
            (now - it.lastAccessed) <= NINETY_DAYS_MILLIS 
        }.toMutableList()

        if (filtered.size != resultHistory.size) {
            saveHistory(context, filtered)
        }

        return filtered
    }

    @JvmStatic
    fun addHistoryEntry(context: Context?, entry: HistoryItem?) {
        if (context == null || entry == null) return

        val history = loadHistory(context)
        val updated = entry.copyWithLastAccessed(System.currentTimeMillis())

        history.add(0, updated)
        saveHistory(context, history)
    }

    @JvmStatic
    fun deleteHistoryEntry(context: Context?, target: HistoryItem?) {
        if (context == null || target == null) return

        val history = loadHistory(context)
        val updated = history.filter { !sameRows(it, target) }

        saveHistory(context, updated)
    }

    private fun sameRows(a: HistoryItem, b: HistoryItem): Boolean {
        if (a.type != b.type) return false
        return if (a.type == HistoryItem.HistoryType.SUBSTITUTION) {
            Objects.equals(a.topRow, b.topRow) && Objects.equals(a.bottomRow, b.bottomRow)
        } else {
            Objects.equals(a.expression, b.expression) && Objects.equals(a.result, b.result)
        }
    }
}
