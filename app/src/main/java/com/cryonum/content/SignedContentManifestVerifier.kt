package com.cryonum.content

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.security.Signature
import java.time.Instant
import java.util.Base64

class SignedContentManifestVerifier(
    private val trustedKeys: Map<String, PublicKey>,
    private val maxManifestBytes: Int = 256 * 1024,
    private val maxFileBytes: Long = 100L * 1024L * 1024L,
    private val maxTotalBytes: Long = 512L * 1024L * 1024L
) {
    fun verify(envelopeBytes: ByteArray, lastAcceptedRevision: Long): ContentManifest {
        security(envelopeBytes.isNotEmpty() && envelopeBytes.size <= maxManifestBytes, "Manifest envelope is empty or too large")
        val envelope = parseObject(decodeUtf8(envelopeBytes), "Invalid manifest envelope")
        val schemaVersion = requiredInt(envelope, "schemaVersion")
        security(schemaVersion == SUPPORTED_SCHEMA_VERSION, "Unsupported schemaVersion")

        val keyId = requiredString(envelope, "keyId")
        val publicKey = trustedKeys[keyId]
            ?: throw ContentException(ContentErrorCategory.SECURITY, "Unknown manifest keyId")
        val payload = decodeBase64Url(requiredString(envelope, "payload"), "payload")
        val signatureBytes = decodeBase64Url(requiredString(envelope, "signature"), "signature")
        security(payload.isNotEmpty() && payload.size <= maxManifestBytes, "Manifest payload is empty or too large")

        val verified = try {
            Signature.getInstance("SHA256withECDSA").run {
                initVerify(publicKey)
                update(payload)
                verify(signatureBytes)
            }
        } catch (e: Exception) {
            throw ContentException(ContentErrorCategory.SECURITY, "Invalid manifest signature", cause = e)
        }
        security(verified, "Invalid manifest signature")

        val root = parseObject(decodeUtf8(payload), "Invalid signed payload JSON")
        val revision = requiredLong(root, "revision")
        security(revision > 0, "Manifest revision must be positive")
        security(revision >= lastAcceptedRevision, "Manifest rollback rejected")
        val generatedAt = requiredString(root, "generatedAt")
        try {
            Instant.parse(generatedAt)
        } catch (e: Exception) {
            throw ContentException(ContentErrorCategory.SECURITY, "Invalid generatedAt", cause = e)
        }

        val filesElement = root.get("files")
        security(filesElement != null && filesElement.isJsonArray, "Missing files array")
        val files = filesElement!!.asJsonArray.map { element ->
            security(element.isJsonObject, "Invalid file entry")
            parseFile(element.asJsonObject)
        }
        validateCollection(files)
        return ContentManifest(revision, generatedAt, files)
    }

    private fun parseFile(value: JsonObject): ContentManifestFile {
        val id = requiredString(value, "id")
        val category = ContentCategory.fromWireName(requiredString(value, "category"))
            ?: throw ContentException(ContentErrorCategory.SECURITY, "Unsupported content category")
        val order = requiredInt(value, "order")
        val path = requiredString(value, "path")
        val version = requiredString(value, "contentVersion")
        val size = requiredLong(value, "sizeBytes")
        val sha256 = requiredString(value, "sha256")
        val contentType = requiredString(value, "contentType")
        val displayElement = value.get("displayName")
        security(displayElement != null && displayElement.isJsonObject, "Missing displayName")
        val display = displayElement!!.asJsonObject.entrySet().associate { (key, element) ->
            security(key.matches(Regex("[a-z]{2,3}(-[A-Z]{2})?")), "Invalid displayName locale")
            security(element.isJsonPrimitive && element.asJsonPrimitive.isString, "Invalid displayName")
            key to element.asString.also { security(it.isNotBlank(), "Blank displayName") }
        }

        security(display["ru"].isNullOrBlank().not() && display["en"].isNullOrBlank().not(), "displayName must contain ru and en")
        security(version.matches(Regex("[1-9][0-9]{0,8}")), "Invalid contentVersion")
        security(size in 1..maxFileBytes, "Invalid or excessive file size")
        security(sha256.matches(Regex("[0-9a-f]{64}")), "Invalid SHA-256")
        security(contentType == PDF_CONTENT_TYPE, "Unsupported content type")
        ApprovedContentUrlPolicy.validateRelativePath(path, category, order, id, version)

        return ContentManifestFile(id, category, order, display, path, version, size, sha256, contentType)
    }

    private fun validateCollection(files: List<ContentManifestFile>) {
        security(files.map { it.id }.toSet().size == files.size, "Duplicate content id")
        security(files.map { it.path }.toSet().size == files.size, "Duplicate content path")
        val lectures = files.filter { it.category == ContentCategory.LECTURE }
        val policies = files.filter { it.category == ContentCategory.PRIVACY_POLICY }
        security(lectures.size == 12, "Manifest must contain exactly 12 lectures")
        security(lectures.map { it.order }.sorted() == (1..12).toList(), "Lecture order must be 1 through 12")
        security(policies.size == 1, "Manifest must contain exactly one privacy policy")
        security(files.sumOf { it.sizeBytes } <= maxTotalBytes, "Manifest total size is excessive")
    }

    private fun parseObject(json: String, message: String): JsonObject = try {
        JsonParser.parseString(json).also { security(it.isJsonObject, message) }.asJsonObject
    } catch (e: ContentException) {
        throw e
    } catch (e: Exception) {
        throw ContentException(ContentErrorCategory.SECURITY, message, cause = e)
    }

    private fun requiredString(value: JsonObject, name: String): String {
        val element = value.get(name)
        security(element != null && element.isJsonPrimitive && element.asJsonPrimitive.isString, "Missing or invalid $name")
        return element!!.asString.also { security(it.isNotBlank(), "Blank $name") }
    }

    private fun requiredInt(value: JsonObject, name: String): Int = try {
        val element = value.get(name)
        security(element != null && element.isJsonPrimitive && element.asJsonPrimitive.isNumber, "Missing or invalid $name")
        val primitive = element!!.asJsonPrimitive
        security(primitive.asString.matches(Regex("-?[0-9]+")), "Invalid integer $name")
        primitive.asInt
    } catch (e: ContentException) {
        throw e
    } catch (e: Exception) {
        throw ContentException(ContentErrorCategory.SECURITY, "Invalid $name", cause = e)
    }

    private fun requiredLong(value: JsonObject, name: String): Long = try {
        val element = value.get(name)
        security(element != null && element.isJsonPrimitive && element.asJsonPrimitive.isNumber, "Missing or invalid $name")
        val primitive = element!!.asJsonPrimitive
        security(primitive.asString.matches(Regex("-?[0-9]+")), "Invalid integer $name")
        primitive.asLong
    } catch (e: ContentException) {
        throw e
    } catch (e: Exception) {
        throw ContentException(ContentErrorCategory.SECURITY, "Invalid $name", cause = e)
    }

    private fun decodeBase64Url(value: String, field: String): ByteArray {
        security(value.matches(Regex("[A-Za-z0-9_-]+")), "Invalid base64url $field")
        return try {
            Base64.getUrlDecoder().decode(value)
        } catch (e: IllegalArgumentException) {
            throw ContentException(ContentErrorCategory.SECURITY, "Invalid base64url $field", cause = e)
        }
    }

    private fun decodeUtf8(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: Exception) {
        throw ContentException(ContentErrorCategory.SECURITY, "Manifest is not valid UTF-8", cause = e)
    }

    private fun security(condition: Boolean, message: String) {
        if (!condition) throw ContentException(ContentErrorCategory.SECURITY, message)
    }

    companion object {
        const val SUPPORTED_SCHEMA_VERSION = 1
        const val PDF_CONTENT_TYPE = "application/pdf"
    }
}
