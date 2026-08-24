package com.cryonum.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException

class PolicyConfigService(private val client: OkHttpClient) {
    suspend fun fetch(): PolicyRemoteConfig = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(CONFIG_URL)
            .header("Accept", "application/json")
            .header("Accept-Encoding", "identity")
            .header("Cache-Control", "no-cache")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isRedirect) throw ContentException(ContentErrorCategory.SECURITY, "Policy config redirect rejected")
                if (!response.isSuccessful) {
                    val retryable = response.code == 408 || response.code == 429 || response.code in 500..599
                    throw ContentException(
                        if (response.code == 404) ContentErrorCategory.FILE_UNAVAILABLE else ContentErrorCategory.NETWORK,
                        "Policy config HTTP ${response.code}",
                        retryable = retryable
                    )
                }
                val contentType = response.header("Content-Type").orEmpty().substringBefore(';').trim()
                if (contentType != "application/json") {
                    throw ContentException(ContentErrorCategory.SECURITY, "Unexpected policy config Content-Type")
                }
                val encoding = response.header("Content-Encoding")
                if (encoding != null && !encoding.equals("identity", ignoreCase = true)) {
                    throw ContentException(ContentErrorCategory.SECURITY, "Unexpected policy config Content-Encoding")
                }

                val output = ByteArrayOutputStream()
                response.body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > PolicyConfigParser.MAX_BYTES) {
                            throw ContentException(ContentErrorCategory.SECURITY, "Policy config response is too large")
                        }
                        output.write(buffer, 0, read)
                    }
                }
                PolicyConfigParser.parse(output.toByteArray())
            }
        } catch (e: ContentException) {
            throw@withContext e
        } catch (e: IOException) {
            throw@withContext ContentException(
                ContentErrorCategory.NETWORK,
                "Unable to fetch policy config",
                retryable = true,
                cause = e
            )
        }
    }

    companion object {
        val CONFIG_URL: HttpUrl = "https://icymath.com/data/privacy-policy.json".toHttpUrl()
    }
}
