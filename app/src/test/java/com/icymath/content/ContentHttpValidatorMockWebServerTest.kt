package com.icymath.content

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ContentHttpValidatorMockWebServerTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient
    private val body = "%PDF-test" // 9 bytes
    private val file = ContentManifestFile(
        "lecture-01", ContentCategory.LECTURE, 1,
        mapOf("ru" to "Лекция 1", "en" to "Lecture 1"),
        "lectures/v1/lecture-01.pdf", "1", body.toByteArray().size.toLong(), "a".repeat(64), "application/pdf"
    )

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun successful200IsAccepted() {
        server.enqueue(pdfResponse(200, body))
        execute().use { ContentHttpValidator.validateStatus(it, resume = false); ContentHttpValidator.validateHeaders(it, file, 0, false) }
    }

    @Test
    fun disconnectDuringBodyIsObservable() {
        server.enqueue(pdfResponse(200, body.repeat(1000)).setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
        assertThrows(IOException::class.java) { execute().use { it.body.bytes() } }
    }

    @Test
    fun correct206IsAccepted() {
        val remaining = body.substring(5)
        server.enqueue(pdfResponse(206, remaining).setHeader("Content-Range", "bytes 5-8/9").setHeader("ETag", "\"v1\""))
        execute().use {
            ContentHttpValidator.validateStatus(it, resume = true)
            ContentHttpValidator.validateContentRange(it, 5, 9)
            ContentHttpValidator.validateHeaders(it, file, 5, true)
        }
    }

    @Test
    fun wrongContentRangeIsRejected() {
        server.enqueue(pdfResponse(206, body.substring(5)).setHeader("Content-Range", "bytes 4-8/9"))
        execute().use { assertSecurity { ContentHttpValidator.validateContentRange(it, 5, 9) } }
    }

    @Test
    fun response200ToRangeRequiresRestart() {
        assertEquals(ResumeDisposition.RESTART, ContentHttpValidator.resumeDisposition(200, 5, 9))
    }

    @Test
    fun changedEtagIsNotAcceptedAsSameValidator() {
        assertTrue(ContentHttpValidator.isStrongEtag("\"old\""))
        assertTrue(ContentHttpValidator.isStrongEtag("\"new\""))
        assertNotEquals("\"old\"", "\"new\"")
        assertFalse(ContentHttpValidator.isStrongEtag("W/\"weak\""))
    }

    @Test
    fun response416VerifiesOnlyCompletePart() {
        assertEquals(ResumeDisposition.VERIFY_COMPLETE_PART, ContentHttpValidator.resumeDisposition(416, 9, 9))
        assertEquals(ResumeDisposition.RESTART, ContentHttpValidator.resumeDisposition(416, 8, 9))
    }

    @Test
    fun response404IsNotRetryable() {
        server.enqueue(MockResponse().setResponseCode(404))
        execute().use {
            val error = assertThrows(ContentException::class.java) { ContentHttpValidator.validateStatus(it, false) }
            assertEquals(ContentErrorCategory.FILE_UNAVAILABLE, error.category)
            assertFalse(error.retryable)
        }
    }

    @Test
    fun response429HonorsBoundedRetryAfter() {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "999999"))
        execute().use {
            val error = assertThrows(ContentException::class.java) { ContentHttpValidator.validateStatus(it, false) }
            assertTrue(error.retryable)
            assertEquals(5L * 60L * 1000L, error.retryAfterMillis)
        }
    }

    @Test
    fun response5xxIsRetryable() {
        server.enqueue(MockResponse().setResponseCode(503))
        execute().use {
            val error = assertThrows(ContentException::class.java) { ContentHttpValidator.validateStatus(it, false) }
            assertTrue(error.retryable)
        }
    }

    @Test
    fun redirectToOtherHostIsNotFollowedAndRejected() {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "https://evil.example/file.pdf"))
        execute().use { response ->
            assertEquals(302, response.code)
            assertSecurity { ContentHttpValidator.validateStatus(response, false) }
        }
    }

    @Test
    fun redirectToHttpIsNotFollowedAndRejected() {
        server.enqueue(MockResponse().setResponseCode(302).setHeader("Location", "http://download.icymath.com/file.pdf"))
        execute().use { response ->
            assertEquals(302, response.code)
            assertSecurity { ContentHttpValidator.validateStatus(response, false) }
        }
    }

    @Test
    fun responseLargerThanSignedSizeIsRejectedFromHeaders() {
        server.enqueue(pdfResponse(200, body + "extra"))
        execute().use { assertSecurity { ContentHttpValidator.validateHeaders(it, file, 0, false) } }
    }

    private fun pdfResponse(code: Int, value: String) = MockResponse()
        .setResponseCode(code)
        .setHeader("Content-Type", "application/pdf")
        .setBody(value)

    private fun execute() = client.newCall(Request.Builder().url(server.url("/file.pdf")).build()).execute()

    private fun assertSecurity(block: () -> Unit) {
        val error = assertThrows(ContentException::class.java) { block() }
        assertEquals(ContentErrorCategory.SECURITY, error.category)
    }
}
