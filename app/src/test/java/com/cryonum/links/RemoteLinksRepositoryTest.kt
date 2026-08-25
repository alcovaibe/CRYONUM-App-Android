package com.cryonum.links

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteLinksRepositoryTest {
    @Test
    fun storesFreshRemoteLink() = runBlocking {
        val source = FakeSource(config(2, "https://t.me/cryonum"))
        val cache = FakeCache()

        val url = RemoteLinksRepository(source, cache).telegramUrl()

        assertEquals("https://t.me/cryonum", url.toString())
        assertEquals(2L, cache.value?.revision)
    }

    @Test
    fun usesCachedLinkWhenNetworkFails() = runBlocking {
        val source = FakeSource(error = RemoteLinksException("offline"))
        val cache = FakeCache(CachedTelegramLink(3, "https://t.me/cryonum"))

        val url = RemoteLinksRepository(source, cache).telegramUrl()

        assertEquals("https://t.me/cryonum", url.toString())
    }

    @Test
    fun rejectsRollbackAndKeepsNewerCachedLink() = runBlocking {
        val source = FakeSource(config(2, "https://t.me/oldername"))
        val cache = FakeCache(CachedTelegramLink(3, "https://t.me/cryonum"))

        val url = RemoteLinksRepository(source, cache).telegramUrl()

        assertEquals("https://t.me/cryonum", url.toString())
        assertEquals(3L, cache.value?.revision)
    }

    private fun config(revision: Long, url: String) =
        RemoteLinksConfig(revision, TelegramUrlPolicy.parse(url))

    private class FakeSource(
        private val config: RemoteLinksConfig? = null,
        private val error: Exception? = null
    ) : RemoteLinksSource {
        override suspend fun fetch(): RemoteLinksConfig {
            error?.let { throw it }
            return requireNotNull(config)
        }
    }

    private class FakeCache(initial: CachedTelegramLink? = null) : RemoteLinksCache {
        var value: CachedTelegramLink? = initial

        override suspend fun readTelegram(): CachedTelegramLink? = value

        override suspend fun saveTelegram(config: RemoteLinksConfig) {
            value = CachedTelegramLink(config.revision, config.telegramUrl.toString())
        }
    }
}
