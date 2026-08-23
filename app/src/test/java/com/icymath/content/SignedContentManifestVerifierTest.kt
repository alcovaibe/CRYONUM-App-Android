package com.icymath.content

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class SignedContentManifestVerifierTest {
    private lateinit var keyPair: KeyPair
    private lateinit var verifier: SignedContentManifestVerifier
    private val gson = Gson()

    @Before
    fun setup() {
        keyPair = KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()
        verifier = SignedContentManifestVerifier(mapOf(KEY_ID to keyPair.public))
    }

    @Test
    fun validSignatureIsAccepted() {
        val result = verifier.verify(envelope(payload()), 0)
        assertEquals(1, result.revision)
        assertEquals(12, result.lectures.size)
    }

    @Test
    fun invalidSignatureIsRejected() {
        val envelope = JsonParser.parseString(String(envelope(payload()))).asJsonObject
        envelope.addProperty("signature", b64(ByteArray(70) { 1 }))
        assertSecurity { verifier.verify(gson.toJson(envelope).toByteArray(), 0) }
    }

    @Test
    fun unknownKeyIdIsRejected() {
        assertSecurity { verifier.verify(envelope(payload(), keyId = "unknown"), 0) }
    }

    @Test
    fun unknownSchemaVersionIsRejected() {
        assertSecurity { verifier.verify(envelope(payload(), schemaVersion = 2), 0) }
    }

    @Test
    fun rollbackIsRejected() {
        assertSecurity { verifier.verify(envelope(payload(revision = 4)), 5) }
    }

    @Test
    fun missingRequiredFieldIsRejected() {
        val root = payloadObject()
        root.remove("generatedAt")
        assertSecurity { verifier.verify(envelope(gson.toJson(root).toByteArray()), 0) }
    }

    @Test
    fun duplicateIdIsRejected() {
        val root = payloadObject()
        root.getAsJsonArray("files")[1].asJsonObject.addProperty("id", "lecture-01")
        assertSecurity { verifier.verify(envelope(gson.toJson(root).toByteArray()), 0) }
    }

    @Test
    fun duplicatePathIsRejected() {
        val root = payloadObject()
        root.getAsJsonArray("files")[1].asJsonObject.addProperty("path", "lectures/Basic Algebraic Structures.pdf")
        assertSecurity { verifier.verify(envelope(gson.toJson(root).toByteArray()), 0) }
    }

    @Test
    fun wrongLectureCountIsRejected() {
        val root = payloadObject()
        root.getAsJsonArray("files").remove(0)
        assertSecurity { verifier.verify(envelope(gson.toJson(root).toByteArray()), 0) }
    }

    @Test
    fun malformedSha256IsRejected() {
        val root = payloadObject()
        root.getAsJsonArray("files")[0].asJsonObject.addProperty("sha256", "ABC")
        assertSecurity { verifier.verify(envelope(gson.toJson(root).toByteArray()), 0) }
    }

    @Test
    fun wrongMimeIsRejected() {
        val root = payloadObject()
        root.getAsJsonArray("files")[0].asJsonObject.addProperty("contentType", "text/html")
        assertSecurity { verifier.verify(envelope(gson.toJson(root).toByteArray()), 0) }
    }

    @Test
    fun excessiveFileSizeIsRejected() {
        val root = payloadObject()
        root.getAsJsonArray("files")[0].asJsonObject.addProperty("sizeBytes", 101L * 1024L * 1024L)
        assertSecurity { verifier.verify(envelope(gson.toJson(root).toByteArray()), 0) }
    }

    private fun payload(revision: Long = 1): ByteArray = gson.toJson(payloadObject(revision)).toByteArray()

    private fun payloadObject(revision: Long = 1): JsonObject {
        val lectureNames = listOf(
            "Basic Algebraic Structures.pdf",
            "Divisibility in the Ring of Integers.pdf",
            "GCD and LCM. Coprime Integers.pdf",
            "Prime Numbers.pdf",
            "Numerical Congruences.pdf",
            "Solving Congruences.pdf",
            "Complex Numbers. Part 1.pdf",
            "Complex Numbers. Part 2.pdf",
            "Systems of Linear Equations. Gauss Method.pdf",
            "Matrices.pdf",
            "Determinants.pdf",
            "Permutations.pdf"
        )
        val files = (1..12).map { order ->
            mapOf(
                "id" to "lecture-${order.toString().padStart(2, '0')}",
                "category" to "lecture",
                "order" to order,
                "displayName" to mapOf("ru" to "Лекция $order", "en" to "Lecture $order"),
                "path" to "lectures/${lectureNames[order - 1]}",
                "contentVersion" to "1",
                "sizeBytes" to 1000,
                "sha256" to "a".repeat(64),
                "contentType" to "application/pdf"
            )
        } + mapOf(
            "id" to "privacy-policy",
            "category" to "privacy_policy",
            "order" to 1,
            "displayName" to mapOf("ru" to "Политика конфиденциальности", "en" to "Privacy Policy"),
            "path" to "privacy-policy/v1/privacy-policy.pdf",
            "contentVersion" to "1",
            "sizeBytes" to 1000,
            "sha256" to "b".repeat(64),
            "contentType" to "application/pdf"
        )
        return JsonParser.parseString(gson.toJson(mapOf("revision" to revision, "generatedAt" to "2026-08-23T12:00:00Z", "files" to files))).asJsonObject
    }

    private fun envelope(payload: ByteArray, keyId: String = KEY_ID, schemaVersion: Int = 1): ByteArray {
        val signature = Signature.getInstance("SHA256withECDSA").run {
            initSign(keyPair.private)
            update(payload)
            sign()
        }
        return gson.toJson(mapOf("schemaVersion" to schemaVersion, "keyId" to keyId, "payload" to b64(payload), "signature" to b64(signature))).toByteArray()
    }

    private fun b64(value: ByteArray): String = Base64.getUrlEncoder().withoutPadding().encodeToString(value)

    private fun assertSecurity(block: () -> Unit) {
        val error = assertThrows(ContentException::class.java) { block() }
        assertEquals(ContentErrorCategory.SECURITY, error.category)
    }

    companion object {
        private const val KEY_ID = "content-2026-01"
    }
}
