package com.cryonum.content

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ContentIntegrityVerifierTest {
    @get:Rule val temporaryFolder = TemporaryFolder()
    private val verifier = ContentIntegrityVerifier()

    @Test
    fun htmlMasqueradingAsPdfIsRejected() {
        val file = temporaryFolder.newFile("fake.pdf").apply { writeText("<html>not a pdf</html>") }
        assertFalse(verifier.verify(file, expected(file)))
    }

    @Test
    fun corruptedLocalFileIsRejected() {
        val file = temporaryFolder.newFile("corrupt.pdf").apply { writeBytes("%PDF-corrupt".toByteArray()) }
        val expected = expected(file).copy(sha256 = "0".repeat(64))
        assertFalse(verifier.verify(file, expected))
    }

    @Test
    fun validPdfHeaderSizeAndHashAreAccepted() {
        val file = temporaryFolder.newFile("valid.pdf").apply { writeBytes("%PDF-valid-test".toByteArray()) }
        assertTrue(verifier.verify(file, expected(file)))
    }

    @Test
    fun insufficientSpaceIsDetected() {
        assertFalse(ContentStorage.hasSufficientSpace(availableBytes = 99, requiredBytes = 80, reserveBytes = 20))
        assertTrue(ContentStorage.hasSufficientSpace(availableBytes = 100, requiredBytes = 80, reserveBytes = 20))
    }

    @Test
    fun compatiblePartCanResumeAfterProcessRestart() {
        val expected = expectedForSize(100)
        val metadata = PartialContentMetadata(expected.id, 7, expected.path, expected.sizeBytes, expected.sha256, "\"etag-1\"", 40)
        assertTrue(metadata.matches(7, expected, 40))
    }

    @Test
    fun changedManifestOrPartLengthForcesRestart() {
        val expected = expectedForSize(100)
        val metadata = PartialContentMetadata(expected.id, 7, expected.path, expected.sizeBytes, expected.sha256, "\"etag-1\"", 40)
        assertFalse(metadata.matches(8, expected, 40))
        assertFalse(metadata.matches(7, expected, 39))
    }

    @Test
    fun verifiedTemporaryFileAtomicallyReplacesOldFinal() {
        val source = temporaryFolder.newFile("source.part").apply { writeText("new") }
        val target = temporaryFolder.newFile("final.pdf").apply { writeText("old") }
        AtomicContentPublisher.move(source, target)
        assertFalse(source.exists())
        assertEquals("new", target.readText())
    }

    private fun expected(file: File) = ContentManifestFile(
        id = "privacy-policy",
        category = ContentCategory.PRIVACY_POLICY,
        order = 1,
        displayName = mapOf("ru" to "Политика", "en" to "Policy"),
        path = "privacy-policy/v4.0/privacy-policy.pdf",
        contentVersion = "4",
        sizeBytes = file.length(),
        sha256 = ContentStorage.sha256(file),
        contentType = "application/pdf"
    )

    private fun expectedForSize(size: Long) = ContentManifestFile(
        id = "lecture-01",
        category = ContentCategory.LECTURE,
        order = 1,
        displayName = mapOf("ru" to "Лекция 1", "en" to "Lecture 1"),
        path = "lectures/v1/lecture-01.pdf",
        contentVersion = "1",
        sizeBytes = size,
        sha256 = "a".repeat(64),
        contentType = "application/pdf"
    )
}
