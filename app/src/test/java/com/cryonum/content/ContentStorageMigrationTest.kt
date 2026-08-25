package com.cryonum.content

import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentStorageMigrationTest {
    @Test
    fun `legacy content is moved to CRYONUM directory`() {
        val filesDir = Files.createTempDirectory("cryonum-content-test").toFile()
        try {
            val legacyFile = filesDir.resolve("icy_content/privacy/privacy-policy.pdf")
            assertTrue(legacyFile.parentFile.mkdirs())
            legacyFile.writeText("legacy")

            val result = ContentStorage.migrateLegacyRoot(filesDir)

            assertEquals(filesDir.resolve("cryonum_content"), result)
            assertEquals("legacy", result.resolve("privacy/privacy-policy.pdf").readText())
            assertFalse(filesDir.resolve("icy_content").exists())
        } finally {
            filesDir.deleteRecursively()
        }
    }

    @Test
    fun `migration preserves files already stored in CRYONUM directory`() {
        val filesDir = Files.createTempDirectory("cryonum-content-test").toFile()
        try {
            val currentFile = filesDir.resolve("cryonum_content/privacy/privacy-policy.pdf")
            assertTrue(currentFile.parentFile.mkdirs())
            currentFile.writeText("current")

            val legacyFile = filesDir.resolve("icy_content/privacy/privacy-policy.pdf")
            assertTrue(legacyFile.parentFile.mkdirs())
            legacyFile.writeText("legacy")

            val result = ContentStorage.migrateLegacyRoot(filesDir)

            assertEquals("current", result.resolve("privacy/privacy-policy.pdf").readText())
        } finally {
            filesDir.deleteRecursively()
        }
    }
}
