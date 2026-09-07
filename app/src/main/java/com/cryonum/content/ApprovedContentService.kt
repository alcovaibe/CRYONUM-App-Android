package com.cryonum.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.ByteArrayOutputStream
import java.io.IOException

class ApprovedContentService(
    private val client: OkHttpClient,
    private val verifier: SignedContentManifestVerifier,
    private val metadataStore: ContentMetadataStore,
    private val storage: ContentStorage
) {
    private val manifestLock = Mutex()
    suspend fun getVerifiedManifest(): ContentManifest = manifestLock.withLock { withContext(Dispatchers.IO) {
        val lastRevision = metadataStore.lastAcceptedRevision()
        try {
            val envelope = fetchEnvelope()
            val manifest = verifier.verify(envelope, lastRevision)
            val cached = loadCached(lastRevision)
            if (cached != null && cached.revision == manifest.revision && cached.files != manifest.files) {
                throw ContentException(ContentErrorCategory.SECURITY, "Content changed without revision increment")
            }
            storage.writeCachedManifest(envelope)
            metadataStore.acceptRevision(manifest.revision)
            manifest
        } catch (e: ContentException) {
            if (e.category == ContentErrorCategory.SECURITY) throw e
            loadCached(lastRevision) ?: throw e
        } catch (e: IOException) {
            coroutineContext.ensureActive()
            loadCached(lastRevision) ?: throw ContentException(
                ContentErrorCategory.NETWORK,
                "Unable to fetch content manifest",
                retryable = true,
                cause = e
            )
        }
    } }

    suspend fun cached(): ContentManifest? = manifestLock.withLock { withContext(Dispatchers.IO) { loadCached(metadataStore.lastAcceptedRevision()) } }

    private suspend fun fetchEnvelope(): ByteArray {
        val request = Request.Builder()
            .url(ApprovedContentUrlPolicy.MANIFEST_URL)
            .header("Accept", "application/json")
            .header("Accept-Encoding", "identity")
            .get()
            .build()
        val call = client.newCall(request)
        return withCancellableCall(call) { call.execute().use { response ->
            if (response.isRedirect) throw ContentException(ContentErrorCategory.SECURITY, "Manifest redirect rejected")
            if (!response.isSuccessful) {
                val retryable = response.code == 408 || response.code == 429 || response.code in 500..599
                throw ContentException(
                    if (response.code == 404) ContentErrorCategory.FILE_UNAVAILABLE else ContentErrorCategory.NETWORK,
                    "Manifest HTTP ${response.code}",
                    retryable = retryable
                )
            }
            val encoding = response.header("Content-Encoding")
            if (encoding != null && !encoding.equals("identity", ignoreCase = true)) {
                throw ContentException(ContentErrorCategory.SECURITY, "Unexpected manifest Content-Encoding")
            }
            val body = response.body
            val output = ByteArrayOutputStream()
            body.byteStream().use { input ->
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > MAX_MANIFEST_BYTES) {
                        throw ContentException(ContentErrorCategory.SECURITY, "Manifest response is too large")
                    }
                    output.write(buffer, 0, read)
                }
            }
            output.toByteArray()
        } }
    }

    private fun loadCached(lastRevision: Long): ContentManifest? {
        val cache = storage.cachedManifest
        if (!cache.isFile || cache.length() !in 1..MAX_MANIFEST_BYTES.toLong()) return null
        return runCatching { verifier.verify(cache.readBytes(), lastRevision) }.getOrNull()
    }

    companion object {
        private const val MAX_MANIFEST_BYTES = 256 * 1024
    }
}
