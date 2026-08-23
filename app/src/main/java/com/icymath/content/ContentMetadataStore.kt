package com.icymath.content

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.contentDownloadDataStore: DataStore<Preferences> by preferencesDataStore(name = "content_download")

data class CompletedContentRecord(
    val id: String,
    val manifestRevision: Long,
    val contentVersion: String,
    val expectedSize: Long,
    val sha256: String,
    val etag: String?
)

class ContentMetadataStore(context: Context, private val gson: Gson) {
    private val store = context.applicationContext.contentDownloadDataStore

    suspend fun lastAcceptedRevision(): Long = store.data.first()[LAST_REVISION] ?: 0L

    suspend fun acceptRevision(revision: Long) {
        store.edit { preferences ->
            val current = preferences[LAST_REVISION] ?: 0L
            if (revision > current) preferences[LAST_REVISION] = revision
        }
    }

    suspend fun completedRecord(id: String): CompletedContentRecord? {
        val json = store.data.first()[recordKey(id)] ?: return null
        return runCatching { gson.fromJson(json, CompletedContentRecord::class.java) }.getOrNull()
    }

    suspend fun markCompleted(record: CompletedContentRecord) {
        store.edit { it[recordKey(record.id)] = gson.toJson(record) }
    }

    suspend fun clearCompleted(id: String) {
        store.edit { it.remove(recordKey(id)) }
    }

    private fun recordKey(id: String) = stringPreferencesKey("completed_$id")

    companion object {
        private val LAST_REVISION = longPreferencesKey("last_accepted_manifest_revision")
    }
}
