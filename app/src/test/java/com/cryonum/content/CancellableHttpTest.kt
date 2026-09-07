package com.cryonum.content

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class CancellableHttpTest {
    @Test fun cancellingWhileWaitingForHeadersClosesCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("ok").setHeadersDelay(10, TimeUnit.SECONDS))
            val call = OkHttpClient().newCall(Request.Builder().url(server.url("/test")).build())
            val work = launch(Dispatchers.IO) {
                try { withCancellableCall(call) { call.execute().use { it.body.string() } } }
                catch (_: java.io.IOException) { ensureActive() }
            }
            withContext(Dispatchers.IO) { assertNotNull(server.takeRequest(2,TimeUnit.SECONDS)) }
            withTimeout(2000) { work.cancelAndJoin() }
            assertTrue(call.isCanceled())
        }
    }
}
