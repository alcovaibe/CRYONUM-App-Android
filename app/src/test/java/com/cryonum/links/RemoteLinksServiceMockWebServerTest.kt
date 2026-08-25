package com.cryonum.links

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test

class RemoteLinksServiceMockWebServerTest {
    private lateinit var server: MockWebServer
    private lateinit var service: RemoteLinksService

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val client = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
        service = RemoteLinksService(client, server.url("/data/site.json"))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun fetchesValidConfiguration() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(validJson())
        )

        assertEquals("https://t.me/cryonum", service.fetch().telegramUrl.toString())
    }

    @Test
    fun rejectsRedirect() {
        server.enqueue(
            MockResponse()
                .setResponseCode(302)
                .setHeader("Location", "https://example.com/site.json")
        )

        assertThrows(RemoteLinksException::class.java) {
            runBlocking { service.fetch() }
        }
    }

    @Test
    fun rejectsWrongContentType() {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/html")
                .setBody(validJson())
        )

        assertThrows(RemoteLinksException::class.java) {
            runBlocking { service.fetch() }
        }
    }

    private fun validJson() =
        """{"schemaVersion":1,"revision":1,"socials":{"telegram":"https://t.me/cryonum"}}"""
}
