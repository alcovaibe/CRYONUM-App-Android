package com.cryonum.links

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.remoteLinksDataStore: DataStore<Preferences> by preferencesDataStore(name = "remote_links")

data class CachedTelegramLink(val revision: Long, val url: String)

interface RemoteLinksCache {
    suspend fun readTelegram(): CachedTelegramLink?
    suspend fun saveTelegram(config: RemoteLinksConfig)
}

class RemoteLinksStore(context: Context) : RemoteLinksCache {
    private val store = context.applicationContext.remoteLinksDataStore

    override suspend fun readTelegram(): CachedTelegramLink? {
        val preferences = store.data.first()
        val revision = preferences[REVISION] ?: return null
        val url = preferences[TELEGRAM_URL] ?: return null
        if (revision <= 0 || runCatching { TelegramUrlPolicy.parse(url) }.isFailure) return null
        return CachedTelegramLink(revision, url)
    }

    override suspend fun saveTelegram(config: RemoteLinksConfig) {
        store.edit { preferences ->
            preferences[REVISION] = config.revision
            preferences[TELEGRAM_URL] = config.telegramUrl.toString()
        }
    }

    companion object {
        private val REVISION = longPreferencesKey("links_revision")
        private val TELEGRAM_URL = stringPreferencesKey("telegram_url")
    }
}
