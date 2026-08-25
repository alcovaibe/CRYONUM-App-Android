package com.cryonum.content

import android.content.Context
import android.os.StatFs
import com.google.gson.Gson
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

data class PartialContentMetadata(
    val id: String,
    val manifestRevision: Long,
    val path: String,
    val expectedSize: Long,
    val sha256: String,
    val etag: String?,
    val savedBytes: Long
) {
    fun matches(manifestRevision: Long, file: ContentManifestFile, actualBytes: Long): Boolean =
        id == file.id &&
            this.manifestRevision == manifestRevision &&
            path == file.path &&
            expectedSize == file.sizeBytes &&
            sha256 == file.sha256 &&
            savedBytes == actualBytes &&
            actualBytes in 0..file.sizeBytes &&
            (etag == null || ContentHttpValidator.isStrongEtag(etag))
}

class ContentStorage(context: Context, private val gson: Gson) {
    val root = migrateLegacyRoot(context.filesDir)
    private val lectures = File(root, "lectures")
    private val privacy = File(root, "privacy")
    private val partial = File(root, "partial")
    val cachedManifest = File(root, "content-v1.signed.json")

    init {
        listOf(root, lectures, privacy, partial).forEach { directory ->
            if (!directory.exists() && !directory.mkdirs()) {
                throw IllegalStateException("Cannot create content directory")
            }
        }
    }

    fun finalFile(file: ContentManifestFile): File = when (file.category) {
        ContentCategory.LECTURE -> File(lectures, "${file.id}.pdf")
        ContentCategory.PRIVACY_POLICY -> File(privacy, "privacy-policy.pdf")
    }

    fun partialFile(file: ContentManifestFile): File = File(partial, "${file.id}.pdf.part")
    fun partialMetadataFile(file: ContentManifestFile): File = File(partial, "${file.id}.part.json")

    fun readPartialMetadata(file: ContentManifestFile): PartialContentMetadata? {
        val metadataFile = partialMetadataFile(file)
        if (!metadataFile.isFile || metadataFile.length() > 16 * 1024) return null
        return runCatching { gson.fromJson(metadataFile.readText(Charsets.UTF_8), PartialContentMetadata::class.java) }.getOrNull()
    }

    fun writePartialMetadata(file: ContentManifestFile, metadata: PartialContentMetadata) {
        val target = partialMetadataFile(file)
        val temporary = File(target.parentFile, "${target.name}.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(gson.toJson(metadata).toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        AtomicContentPublisher.move(temporary, target)
    }

    fun discardPartial(file: ContentManifestFile) {
        partialFile(file).delete()
        partialMetadataFile(file).delete()
    }

    fun discardBundlePartials(bundle: ContentBundle) {
        val prefix = if (bundle == ContentBundle.LECTURES) "lecture-" else "privacy-policy"
        partial.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach(File::delete)
    }

    fun hasSpaceFor(requiredBytes: Long): Boolean {
        val reserve = maxOf(16L * 1024L * 1024L, requiredBytes / 10L)
        val available = StatFs(root.absolutePath).availableBytes
        return hasSufficientSpace(available, requiredBytes, reserve)
    }

    fun isVerified(file: ContentManifestFile): Boolean {
        val local = finalFile(file)
        if (!local.isFile || local.length() != file.sizeBytes) return false
        return hasPdfMagic(local) && sha256(local) == file.sha256
    }

    fun verifyPart(file: ContentManifestFile): Boolean {
        val local = partialFile(file)
        return local.isFile && local.length() == file.sizeBytes && hasPdfMagic(local) && sha256(local) == file.sha256
    }

    fun deleteCorruptFinal(file: ContentManifestFile) {
        finalFile(file).delete()
    }

    fun publish(file: ContentManifestFile) {
        val source = partialFile(file)
        val target = finalFile(file)
        if (!source.isFile) throw ContentException(ContentErrorCategory.FILE_SYSTEM, "Verified temporary file disappeared")
        AtomicContentPublisher.move(source, target)
        partialMetadataFile(file).delete()
    }

    fun writeCachedManifest(bytes: ByteArray) {
        val temporary = File(root, "${cachedManifest.name}.tmp")
        FileOutputStream(temporary).use { output ->
            output.write(bytes)
            output.fd.sync()
        }
        AtomicContentPublisher.move(temporary, cachedManifest)
    }

    companion object {
        private const val CONTENT_DIRECTORY = "cryonum_content"
        private const val LEGACY_CONTENT_DIRECTORY = "icy_content"

        internal fun migrateLegacyRoot(filesDir: File): File {
            val target = File(filesDir, CONTENT_DIRECTORY)
            val legacy = File(filesDir, LEGACY_CONTENT_DIRECTORY)
            if (!legacy.isDirectory) return target

            if (!target.exists()) {
                val moved = runCatching {
                    java.nio.file.Files.move(
                        legacy.toPath(),
                        target.toPath(),
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE
                    )
                }.isSuccess
                if (moved) return target
            }

            runCatching {
                legacy.walkTopDown().forEach { source ->
                    val relativePath = source.relativeTo(legacy).path
                    val destination = if (relativePath.isEmpty()) target else File(target, relativePath)
                    if (source.isDirectory) {
                        check(destination.isDirectory || destination.mkdirs()) {
                            "Cannot create content migration directory"
                        }
                    } else if (!destination.exists()) {
                        source.copyTo(destination, overwrite = false)
                    }
                }
            }.onSuccess {
                legacy.deleteRecursively()
            }
            return target
        }

        fun hasSufficientSpace(availableBytes: Long, requiredBytes: Long, reserveBytes: Long): Boolean =
            availableBytes >= 0 && requiredBytes >= 0 && reserveBytes >= 0 &&
                availableBytes >= requiredBytes && availableBytes - requiredBytes >= reserveBytes

        fun hasPdfMagic(file: File): Boolean {
            if (!file.isFile || file.length() < 5) return false
            val magic = ByteArray(5)
            return FileInputStream(file).use { it.read(magic) == magic.size } && magic.contentEquals("%PDF-".toByteArray(Charsets.US_ASCII))
        }

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { input ->
                val buffer = ByteArray(32 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    digest.update(buffer, 0, count)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

object AtomicContentPublisher {
    fun move(source: File, target: File) {
        try {
            java.nio.file.Files.move(
                source.toPath(),
                target.toPath(),
                java.nio.file.StandardCopyOption.ATOMIC_MOVE,
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            )
        } catch (e: java.nio.file.AtomicMoveNotSupportedException) {
            throw ContentException(ContentErrorCategory.FILE_SYSTEM, "Atomic file publication is not supported", cause = e)
        } catch (e: Exception) {
            throw ContentException(ContentErrorCategory.FILE_SYSTEM, "Unable to publish content file", cause = e)
        }
    }
}
