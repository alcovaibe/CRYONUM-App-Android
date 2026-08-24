package com.cryonum.content

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.coroutineContext

data class DownloadProgress(
    val phase: String,
    val fileId: String?,
    val fileIndex: Int,
    val fileCount: Int,
    val fileBytes: Long,
    val fileTotalBytes: Long,
    val overallBytes: Long,
    val overallTotalBytes: Long,
    val completedFiles: Int
)

class ContentDownloadRepository(
    private val client: OkHttpClient,
    private val service: ApprovedContentService,
    private val storage: ContentStorage,
    private val metadataStore: ContentMetadataStore,
    private val integrityVerifier: ContentIntegrityVerifier
) {
    suspend fun manifest(): ContentManifest = service.getVerifiedManifest()

    suspend fun localSummary(manifest: ContentManifest, bundle: ContentBundle): LocalContentSummary {
        val files = filesFor(manifest, bundle)
        var verifiedCount = 0
        var verifiedBytes = 0L
        for (file in files) {
            val final = storage.finalFile(file)
            if (integrityVerifier.verify(final, file)) {
                verifiedCount++
                verifiedBytes += file.sizeBytes
                metadataStore.markCompleted(
                    CompletedContentRecord(file.id, manifest.revision, file.contentVersion, file.sizeBytes, file.sha256, metadataStore.completedRecord(file.id)?.etag)
                )
            } else {
                val record = metadataStore.completedRecord(file.id)
                if (record != null && record.contentVersion == file.contentVersion) {
                    storage.deleteCorruptFinal(file)
                    metadataStore.clearCompleted(file.id)
                }
            }
        }
        return LocalContentSummary(
            verifiedCount = verifiedCount,
            totalCount = files.size,
            verifiedBytes = verifiedBytes,
            totalBytes = files.sumOf { it.sizeBytes },
            current = verifiedCount == files.size
        )
    }

    suspend fun verifiedLocalFile(manifest: ContentManifest, file: ContentManifestFile) = withContext(Dispatchers.IO) {
        val local = storage.finalFile(file)
        if (integrityVerifier.verify(local, file)) return@withContext local
        val record = metadataStore.completedRecord(file.id)
        val belongsToCurrentManifest = record == null || (record.contentVersion == file.contentVersion && record.sha256 == file.sha256)
        if (local.isFile && belongsToCurrentManifest) {
            storage.deleteCorruptFinal(file)
            metadataStore.clearCompleted(file.id)
        }
        null
    }

    fun hasStoredFile(file: ContentManifestFile): Boolean = storage.finalFile(file).isFile

    suspend fun downloadBundle(
        bundle: ContentBundle,
        onCallChanged: (Call?) -> Unit,
        onProgress: suspend (DownloadProgress) -> Unit
    ): ContentManifest = withContext(Dispatchers.IO) {
        val manifest = service.getVerifiedManifest()
        val files = filesFor(manifest, bundle)
        val totalBytes = files.sumOf { it.sizeBytes }
        var completedBytes = 0L
        var completedFiles = 0

        files.forEachIndexed { index, file ->
            coroutineContext.ensureActive()
            if (integrityVerifier.verify(storage.finalFile(file), file)) {
                completedBytes += file.sizeBytes
                completedFiles++
                onProgress(DownloadProgress("DOWNLOADING", file.id, index + 1, files.size, file.sizeBytes, file.sizeBytes, completedBytes, totalBytes, completedFiles))
                return@forEachIndexed
            }

            val part = storage.partialFile(file)
            val partialMetadata = validPartialMetadata(manifest, file)
            if (partialMetadata == null && part.exists()) storage.discardPartial(file)
            val existing = if (partialMetadata != null) part.length() else 0L
            if (!storage.hasSpaceFor(file.sizeBytes - existing)) {
                throw ContentException(ContentErrorCategory.INSUFFICIENT_SPACE, "Insufficient storage space")
            }
            onProgress(DownloadProgress("PREPARING", file.id, index + 1, files.size, existing, file.sizeBytes, completedBytes + existing, totalBytes, completedFiles))
            downloadOne(manifest, file, partialMetadata, onCallChanged) { saved ->
                onProgress(DownloadProgress("DOWNLOADING", file.id, index + 1, files.size, saved, file.sizeBytes, completedBytes + saved, totalBytes, completedFiles))
            }
            onProgress(DownloadProgress("VERIFYING", file.id, index + 1, files.size, file.sizeBytes, file.sizeBytes, completedBytes + file.sizeBytes, totalBytes, completedFiles))
            if (!integrityVerifier.verify(part, file)) {
                storage.discardPartial(file)
                throw ContentException(ContentErrorCategory.CORRUPT_FILE, "Downloaded file failed integrity verification")
            }
            val etag = storage.readPartialMetadata(file)?.etag
            storage.publish(file)
            metadataStore.markCompleted(CompletedContentRecord(file.id, manifest.revision, file.contentVersion, file.sizeBytes, file.sha256, etag))
            completedBytes += file.sizeBytes
            completedFiles++
        }
        onProgress(DownloadProgress("COMPLETED", null, files.size, files.size, 0, 0, totalBytes, totalBytes, files.size))
        manifest
    }

    fun clearPartials(bundle: ContentBundle) = storage.discardBundlePartials(bundle)

    private fun filesFor(manifest: ContentManifest, bundle: ContentBundle): List<ContentManifestFile> = when (bundle) {
        ContentBundle.LECTURES -> manifest.lectures
        ContentBundle.PRIVACY_POLICY -> listOf(manifest.privacyPolicy)
    }

    private fun validPartialMetadata(manifest: ContentManifest, file: ContentManifestFile): PartialContentMetadata? {
        val part = storage.partialFile(file)
        val metadata = storage.readPartialMetadata(file) ?: return null
        val valid = part.isFile && metadata.matches(manifest.revision, file, part.length())
        return metadata.takeIf { valid }
    }

    private suspend fun downloadOne(
        manifest: ContentManifest,
        file: ContentManifestFile,
        initialMetadata: PartialContentMetadata?,
        onCallChanged: (Call?) -> Unit,
        onBytes: suspend (Long) -> Unit
    ) {
        var metadata = initialMetadata
        repeat(2) { attempt ->
            val part = storage.partialFile(file)
            val existing = if (metadata != null) part.length() else 0L
            val canResume = existing > 0 && metadata?.etag?.let(ContentHttpValidator::isStrongEtag) == true
            if (existing > 0 && !canResume) {
                storage.discardPartial(file)
                metadata = null
            }

            val requestBuilder = Request.Builder()
                .url(ApprovedContentUrlPolicy.resolve(file))
                .header("Accept", SignedContentManifestVerifier.PDF_CONTENT_TYPE)
                .header("Accept-Encoding", "identity")
                .get()
            if (canResume) {
                requestBuilder.header("Range", "bytes=$existing-")
                requestBuilder.header("If-Range", metadata!!.etag!!)
            }

            val call = client.newCall(requestBuilder.build())
            onCallChanged(call)
            try {
                call.execute().use { response ->
                    if (response.isRedirect) throw ContentException(ContentErrorCategory.SECURITY, "Content redirect rejected")
                    if (canResume) {
                        when (ContentHttpValidator.resumeDisposition(response.code, part.length(), file.sizeBytes)) {
                            ResumeDisposition.VERIFY_COMPLETE_PART -> {
                                if (integrityVerifier.verify(part, file)) return
                                storage.discardPartial(file)
                                metadata = null
                                return@use
                            }
                            ResumeDisposition.RESTART -> {
                                storage.discardPartial(file)
                                metadata = null
                                return@use
                            }
                            ResumeDisposition.USE_RESPONSE -> Unit
                            ResumeDisposition.ERROR -> ContentHttpValidator.validateStatus(response, resume = true)
                        }
                    }
                    ContentHttpValidator.validateStatus(response, canResume)
                    val responseEtag = response.header("ETag")?.takeIf(ContentHttpValidator::isStrongEtag)
                    if (canResume && responseEtag != metadata!!.etag) {
                        storage.discardPartial(file)
                        metadata = null
                        return@use
                    }
                    if (canResume) ContentHttpValidator.validateContentRange(response, existing, file.sizeBytes)
                    ContentHttpValidator.validateHeaders(response, file, existing, canResume)

                    val effectiveEtag = if (canResume) metadata!!.etag else responseEtag
                    val start = if (canResume) existing else 0L
                    if (!canResume && part.exists() && !part.delete()) {
                        throw ContentException(ContentErrorCategory.FILE_SYSTEM, "Unable to reset temporary file")
                    }
                    storage.writePartialMetadata(file, PartialContentMetadata(file.id, manifest.revision, file.path, file.sizeBytes, file.sha256, effectiveEtag, start))
                    streamResponse(response, file, start, canResume, effectiveEtag, manifest.revision, onBytes)
                    return
                }
            } catch (e: ContentException) {
                throw e
            } catch (e: IOException) {
                val saved = storage.partialFile(file).length().coerceAtMost(file.sizeBytes)
                val currentEtag = storage.readPartialMetadata(file)?.etag
                if (saved > 0) {
                    storage.writePartialMetadata(file, PartialContentMetadata(file.id, manifest.revision, file.path, file.sizeBytes, file.sha256, currentEtag, saved))
                }
                throw ContentException(ContentErrorCategory.NETWORK, "Content download interrupted", retryable = true, cause = e)
            } finally {
                onCallChanged(null)
            }
            if (attempt == 1) throw ContentException(ContentErrorCategory.SECURITY, "Unable to establish a safe resumable response")
        }
    }

    private suspend fun streamResponse(
        response: Response,
        file: ContentManifestFile,
        start: Long,
        append: Boolean,
        etag: String?,
        revision: Long,
        onBytes: suspend (Long) -> Unit
    ) {
        val target = storage.partialFile(file)
        var total = start
        var lastMetadataWrite = start
        FileOutputStream(target, append).use { output ->
            response.body.byteStream().use { input ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    if (total + read > file.sizeBytes) {
                        throw ContentException(ContentErrorCategory.SECURITY, "Response exceeds signed size")
                    }
                    output.write(buffer, 0, read)
                    total += read
                    if (total - lastMetadataWrite >= 512 * 1024) {
                        output.flush()
                        storage.writePartialMetadata(file, PartialContentMetadata(file.id, revision, file.path, file.sizeBytes, file.sha256, etag, total))
                        lastMetadataWrite = total
                    }
                    onBytes(total)
                }
            }
            output.flush()
            output.fd.sync()
        }
        storage.writePartialMetadata(file, PartialContentMetadata(file.id, revision, file.path, file.sizeBytes, file.sha256, etag, total))
        if (total != file.sizeBytes) {
            throw ContentException(ContentErrorCategory.NETWORK, "Incomplete content response", retryable = true)
        }
    }

}
