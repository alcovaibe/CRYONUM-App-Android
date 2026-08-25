package com.cryonum.links

import kotlinx.coroutines.CancellationException
import okhttp3.HttpUrl

class RemoteLinksRepository(
    private val service: RemoteLinksSource,
    private val store: RemoteLinksCache
) {
    suspend fun telegramUrl(): HttpUrl {
        val cached = store.readTelegram()

        return try {
            val remote = service.fetch()
            if (cached != null) {
                if (remote.revision < cached.revision) throw RemoteLinksException("Remote links rollback rejected")
                if (remote.revision == cached.revision && remote.telegramUrl.toString() != cached.url) {
                    throw RemoteLinksException("Remote links changed without revision increment")
                }
            }
            store.saveTelegram(remote)
            remote.telegramUrl
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            cached?.let { TelegramUrlPolicy.parse(it.url) } ?: throw e
        }
    }
}
