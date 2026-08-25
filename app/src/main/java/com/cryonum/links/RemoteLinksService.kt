package com.cryonum.links

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException

class RemoteLinksService(
    private val client: OkHttpClient,
    private val configUrl: HttpUrl = CONFIG_URL
) : RemoteLinksSource {
    override suspend fun fetch(): RemoteLinksConfig = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(configUrl)
            .header("Accept", "application/json")
            .header("Accept-Encoding", "identity")
            .header("Cache-Control", "no-cache")
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isRedirect) throw RemoteLinksException("Remote links redirect rejected")
                if (!response.isSuccessful) throw RemoteLinksException("Remote links HTTP ${response.code}")

                val contentType = response.header("Content-Type").orEmpty().substringBefore(';').trim()
                if (contentType != "application/json") throw RemoteLinksException("Unexpected remote links Content-Type")
                val contentEncoding = response.header("Content-Encoding")
                if (contentEncoding != null && !contentEncoding.equals("identity", ignoreCase = true)) {
                    throw RemoteLinksException("Unexpected remote links Content-Encoding")
                }

                val output = ByteArrayOutputStream()
                response.body.byteStream().use { input ->
                    val buffer = ByteArray(8 * 1024)
                    var total = 0
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        total += read
                        if (total > RemoteLinksConfigParser.MAX_BYTES) {
                            throw RemoteLinksException("Remote links response is too large")
                        }
                        output.write(buffer, 0, read)
                    }
                }
                RemoteLinksConfigParser.parse(output.toByteArray())
            }
        } catch (e: RemoteLinksException) {
            throw e
        } catch (e: IOException) {
            throw RemoteLinksException("Unable to fetch remote links", e)
        }
    }

    companion object {
        val CONFIG_URL: HttpUrl = "https://cryonum.com/data/site.json".toHttpUrl()
    }
}
