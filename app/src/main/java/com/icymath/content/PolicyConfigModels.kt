package com.icymath.content

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class PolicyRemoteConfig(
    val versionCode: Long,
    val versionName: String,
    val contentManifestRevision: Long,
    val contentId: String,
    val objectPath: String,
    val notificationEnabled: Boolean,
    val requiresReaccept: Boolean
)

object PolicyConfigParser {
    private const val SUPPORTED_SCHEMA_VERSION = 1
    private val rootFields = setOf("schemaVersion", "policy")
    private val policyFields = setOf(
        "versionCode",
        "versionName",
        "contentManifestRevision",
        "contentId",
        "objectPath",
        "notificationEnabled",
        "requiresReaccept"
    )
    private val versionNamePattern = Regex("[1-9][0-9]{0,8}(?:\\.[0-9]{1,3}){1,2}")

    fun parse(bytes: ByteArray): PolicyRemoteConfig {
        security(bytes.isNotEmpty() && bytes.size <= MAX_BYTES, "Policy config is empty or too large")
        val root = parseObject(decodeUtf8(bytes), "Invalid policy config JSON")
        security(root.keySet() == rootFields, "Unexpected policy config fields")
        security(requiredLong(root, "schemaVersion") == SUPPORTED_SCHEMA_VERSION.toLong(), "Unsupported policy config schemaVersion")

        val policyElement = root.get("policy")
        security(policyElement != null && policyElement.isJsonObject, "Missing policy config")
        val policy = policyElement!!.asJsonObject
        security(policy.keySet() == policyFields, "Unexpected policy fields")

        val versionCode = requiredLong(policy, "versionCode")
        val versionName = requiredString(policy, "versionName")
        val manifestRevision = requiredLong(policy, "contentManifestRevision")
        val contentId = requiredString(policy, "contentId")
        val objectPath = requiredString(policy, "objectPath")
        val notificationEnabled = requiredBoolean(policy, "notificationEnabled")
        val requiresReaccept = requiredBoolean(policy, "requiresReaccept")

        security(versionCode > 0, "Invalid policy versionCode")
        security(versionCode <= Int.MAX_VALUE, "Policy versionCode is too large")
        security(versionNamePattern.matches(versionName), "Invalid policy versionName")
        security(manifestRevision > 0, "Invalid content manifest revision")
        security(contentId == "privacy-policy", "Invalid policy contentId")
        security(objectPath == "privacy-policy/v$versionName/privacy-policy.pdf", "Policy versionName/path mismatch")
        ApprovedContentUrlPolicy.validateRelativePath(objectPath, ContentCategory.PRIVACY_POLICY, 1, contentId)

        return PolicyRemoteConfig(
            versionCode,
            versionName,
            manifestRevision,
            contentId,
            objectPath,
            notificationEnabled,
            requiresReaccept
        )
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
        security(element != null && element.isJsonPrimitive && element.asJsonPrimitive.isString, "Invalid $name")
        return element!!.asString.also { security(it.isNotBlank(), "Blank $name") }
    }

    private fun requiredLong(value: JsonObject, name: String): Long = try {
        val element = value.get(name)
        security(element != null && element.isJsonPrimitive && element.asJsonPrimitive.isNumber, "Invalid $name")
        val primitive = element!!.asJsonPrimitive
        security(primitive.asString.matches(Regex("[0-9]+")), "Invalid integer $name")
        primitive.asLong
    } catch (e: ContentException) {
        throw e
    } catch (e: Exception) {
        throw ContentException(ContentErrorCategory.SECURITY, "Invalid $name", cause = e)
    }

    private fun requiredBoolean(value: JsonObject, name: String): Boolean {
        val element = value.get(name)
        security(element != null && element.isJsonPrimitive && element.asJsonPrimitive.isBoolean, "Invalid $name")
        return element!!.asBoolean
    }

    private fun decodeUtf8(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: Exception) {
        throw ContentException(ContentErrorCategory.SECURITY, "Policy config is not valid UTF-8", cause = e)
    }

    private fun security(condition: Boolean, message: String) {
        if (!condition) throw ContentException(ContentErrorCategory.SECURITY, message)
    }

    const val MAX_BYTES = 64 * 1024
}

object PolicyUpdateEvaluator {
    fun shouldNotify(
        acceptedVersion: Long,
        lastKnownVersion: Long,
        lastNotifiedVersion: Long,
        config: PolicyRemoteConfig,
        manifest: ContentManifest
    ): Boolean {
        security(config.versionCode >= lastKnownVersion, "Policy config rollback rejected")
        security(manifest.revision >= config.contentManifestRevision, "Policy config references a newer manifest")
        val policy = manifest.privacyPolicy
        security(policy.id == config.contentId, "Policy content id mismatch")
        security(policy.path == config.objectPath, "Policy object path mismatch")
        security(policy.contentVersion == config.versionCode.toString(), "Policy content version mismatch")

        return acceptedVersion > 0 &&
            config.notificationEnabled &&
            config.versionCode > acceptedVersion &&
            config.versionCode > lastNotifiedVersion
    }

    private fun security(condition: Boolean, message: String) {
        if (!condition) throw ContentException(ContentErrorCategory.SECURITY, message)
    }
}
