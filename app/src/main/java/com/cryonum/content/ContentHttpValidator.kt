package com.cryonum.content

import okhttp3.Response
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

enum class ResumeDisposition { USE_RESPONSE, RESTART, VERIFY_COMPLETE_PART, ERROR }

object ContentHttpValidator {
    fun resumeDisposition(statusCode: Int, partSize: Long, expectedSize: Long): ResumeDisposition = when (statusCode) {
        206 -> ResumeDisposition.USE_RESPONSE
        200 -> ResumeDisposition.RESTART
        416 -> if (partSize == expectedSize) ResumeDisposition.VERIFY_COMPLETE_PART else ResumeDisposition.RESTART
        else -> ResumeDisposition.ERROR
    }

    fun validateStatus(response: Response, resume: Boolean) {
        val expected = if (resume) 206 else 200
        if (response.code == expected) return
        val retryable = response.code == 408 || response.code == 429 || response.code in 500..599
        val category = when (response.code) {
            404 -> ContentErrorCategory.FILE_UNAVAILABLE
            in 300..399 -> ContentErrorCategory.SECURITY
            else -> ContentErrorCategory.NETWORK
        }
        throw ContentException(category, "Content HTTP ${response.code}", retryable, parseRetryAfter(response))
    }

    fun validateHeaders(response: Response, file: ContentManifestFile, existing: Long, resume: Boolean) {
        val encoding = response.header("Content-Encoding")
        if (encoding != null && !encoding.equals("identity", ignoreCase = true)) {
            throw ContentException(ContentErrorCategory.SECURITY, "Unexpected Content-Encoding")
        }
        val mediaType = response.header("Content-Type")?.substringBefore(';')?.trim()?.lowercase()
        if (mediaType != file.contentType) throw ContentException(ContentErrorCategory.SECURITY, "Unexpected PDF MIME type")
        val contentLength = response.header("Content-Length")?.toLongOrNull()
        val expectedLength = file.sizeBytes - if (resume) existing else 0L
        if (contentLength != null && contentLength != expectedLength) {
            throw ContentException(ContentErrorCategory.SECURITY, "Content-Length does not match signed size")
        }
    }

    fun validateContentRange(response: Response, start: Long, total: Long) {
        val value = response.header("Content-Range") ?: throw ContentException(ContentErrorCategory.SECURITY, "Missing Content-Range")
        val match = Regex("bytes ([0-9]+)-([0-9]+)/([0-9]+)").matchEntire(value)
            ?: throw ContentException(ContentErrorCategory.SECURITY, "Invalid Content-Range")
        val actualStart = match.groupValues[1].toLongOrNull()
        val end = match.groupValues[2].toLongOrNull()
        val actualTotal = match.groupValues[3].toLongOrNull()
        if (actualStart != start || actualTotal != total || end != total - 1 || end < start) {
            throw ContentException(ContentErrorCategory.SECURITY, "Unsafe Content-Range")
        }
    }

    fun parseRetryAfter(response: Response): Long? {
        if (response.code != 429) return null
        val value = response.header("Retry-After")?.trim() ?: return null
        val millis = value.toLongOrNull()?.let { it * 1000L } ?: runCatching {
            ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - Instant.now().toEpochMilli()
        }.getOrNull()
        return millis?.coerceIn(0L, MAX_RETRY_AFTER_MILLIS)
    }

    fun isStrongEtag(value: String): Boolean = value.matches(Regex("\"[^\"\\r\\n]+\"")) && !value.startsWith("W/")

    private const val MAX_RETRY_AFTER_MILLIS = 5L * 60L * 1000L
}
