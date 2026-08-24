package com.cryonum.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PolicyConfigParserTest {
    @Test
    fun validConfigUsesConfirmedR2ObjectKey() {
        val config = PolicyConfigParser.parse(validConfig())
        assertEquals(4, config.versionCode)
        assertEquals("4.0", config.versionName)
        assertEquals("privacy-policy/v4.0/privacy-policy.pdf", config.objectPath)
    }

    @Test
    fun unknownSchemaIsRejected() {
        assertSecurity { PolicyConfigParser.parse(validConfig().decodeToString().replace("\"schemaVersion\":1", "\"schemaVersion\":2").encodeToByteArray()) }
    }

    @Test
    fun mismatchedVersionAndPathAreRejected() {
        assertSecurity {
            PolicyConfigParser.parse(
                validConfig().decodeToString()
                    .replace("v4.0/privacy-policy.pdf", "v5.0/privacy-policy.pdf")
                    .encodeToByteArray()
            )
        }
    }

    @Test
    fun updateIsNotifiedOnlyAfterAnEarlierAcceptedVersion() {
        val config = PolicyConfigParser.parse(validConfig())
        val manifest = manifest()
        assertFalse(PolicyUpdateEvaluator.shouldNotify(0, 0, 0, config, manifest))
        assertTrue(PolicyUpdateEvaluator.shouldNotify(3, 0, 0, config, manifest))
        assertFalse(PolicyUpdateEvaluator.shouldNotify(4, 4, 0, config, manifest))
        assertFalse(PolicyUpdateEvaluator.shouldNotify(3, 4, 4, config, manifest))
    }

    @Test
    fun configRollbackIsRejected() {
        val config = PolicyConfigParser.parse(validConfig())
        assertSecurity { PolicyUpdateEvaluator.shouldNotify(3, 5, 0, config, manifest()) }
    }

    private fun validConfig() = """
        {"schemaVersion":1,"policy":{"versionCode":4,"versionName":"4.0","contentManifestRevision":1,"contentId":"privacy-policy","objectPath":"privacy-policy/v4.0/privacy-policy.pdf","notificationEnabled":true,"requiresReaccept":true}}
    """.trimIndent().encodeToByteArray()

    private fun manifest() = ContentManifest(
        revision = 1,
        generatedAt = "2026-08-23T12:00:00Z",
        files = listOf(
            ContentManifestFile(
                id = "privacy-policy",
                category = ContentCategory.PRIVACY_POLICY,
                order = 1,
                displayName = mapOf("ru" to "Политика", "en" to "Policy"),
                path = "privacy-policy/v4.0/privacy-policy.pdf",
                contentVersion = "4",
                sizeBytes = 100,
                sha256 = "a".repeat(64),
                contentType = "application/pdf"
            )
        )
    )

    private fun assertSecurity(block: () -> Unit) {
        val error = assertThrows(ContentException::class.java) { block() }
        assertEquals(ContentErrorCategory.SECURITY, error.category)
    }
}
