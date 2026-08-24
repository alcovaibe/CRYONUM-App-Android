package com.cryonum.content

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.policyUpdateDataStore: DataStore<Preferences> by preferencesDataStore(name = "policy_update")

class PolicyUpdateStore(context: Context) {
    private val store = context.applicationContext.policyUpdateDataStore

    suspend fun lastKnownVersion(): Long = store.data.first()[LAST_KNOWN_VERSION] ?: 0L
    suspend fun lastNotifiedVersion(): Long = store.data.first()[LAST_NOTIFIED_VERSION] ?: 0L

    suspend fun recordKnownVersion(version: Long) {
        store.edit { preferences ->
            val current = preferences[LAST_KNOWN_VERSION] ?: 0L
            if (version > current) preferences[LAST_KNOWN_VERSION] = version
        }
    }

    suspend fun recordNotifiedVersion(version: Long) {
        store.edit { preferences ->
            val current = preferences[LAST_NOTIFIED_VERSION] ?: 0L
            if (version > current) preferences[LAST_NOTIFIED_VERSION] = version
        }
    }

    companion object {
        private val LAST_KNOWN_VERSION = longPreferencesKey("last_known_policy_version")
        private val LAST_NOTIFIED_VERSION = longPreferencesKey("last_notified_policy_version")
    }
}
