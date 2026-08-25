package com.cryonum.links

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets

data class RemoteLinksConfig(
    val revision: Long,
    val telegramUrl: HttpUrl
)

interface RemoteLinksSource {
    suspend fun fetch(): RemoteLinksConfig
}

class RemoteLinksException(message: String, cause: Throwable? = null) : Exception(message, cause)

object TelegramUrlPolicy {
    private val usernamePattern = Regex("[A-Za-z][A-Za-z0-9_]{4,31}")

    fun parse(value: String): HttpUrl {
        val url = value.toHttpUrlOrNull() ?: fail("Invalid Telegram URL")
        check(url.scheme == "https", "Only HTTPS Telegram links are allowed")
        check(url.host == "t.me", "Unapproved Telegram host")
        check(url.port == 443, "Non-standard Telegram port is forbidden")
        check(url.username.isEmpty() && url.password.isEmpty(), "Telegram URL userinfo is forbidden")
        check(url.query == null && url.fragment == null, "Telegram URL query and fragment are forbidden")
        check(url.pathSegments.size == 1, "Telegram URL must contain one channel username")
        check(usernamePattern.matches(url.pathSegments.single()), "Invalid Telegram channel username")
        return url
    }

    fun username(url: HttpUrl): String = parse(url.toString()).pathSegments.single()

    private fun check(condition: Boolean, message: String) {
        if (!condition) fail(message)
    }

    private fun fail(message: String): Nothing = throw RemoteLinksException(message)
}

object RemoteLinksConfigParser {
    private const val SUPPORTED_SCHEMA_VERSION = 1L
    const val MAX_BYTES = 64 * 1024

    fun parse(bytes: ByteArray): RemoteLinksConfig {
        requireSecure(bytes.isNotEmpty() && bytes.size <= MAX_BYTES, "Remote links config is empty or too large")
        val root = parseObject(decodeUtf8(bytes), "Invalid remote links JSON")
        requireSecure(requiredLong(root, "schemaVersion") == SUPPORTED_SCHEMA_VERSION, "Unsupported links schemaVersion")
        val revision = requiredLong(root, "revision")
        requireSecure(revision > 0, "Invalid links revision")

        val socialsElement = root.get("socials")
        requireSecure(socialsElement != null && socialsElement.isJsonObject, "Missing socials config")
        val telegram = requiredString(socialsElement!!.asJsonObject, "telegram")

        return RemoteLinksConfig(revision, TelegramUrlPolicy.parse(telegram))
    }

    private fun parseObject(json: String, message: String): JsonObject = try {
        JsonParser.parseString(json).also { requireSecure(it.isJsonObject, message) }.asJsonObject
    } catch (e: RemoteLinksException) {
        throw e
    } catch (e: Exception) {
        throw RemoteLinksException(message, e)
    }

    private fun requiredString(value: JsonObject, name: String): String {
        val element = value.get(name)
        requireSecure(element != null && element.isJsonPrimitive && element.asJsonPrimitive.isString, "Invalid $name")
        return element!!.asString.also { requireSecure(it.isNotBlank(), "Blank $name") }
    }

    private fun requiredLong(value: JsonObject, name: String): Long = try {
        val element = value.get(name)
        requireSecure(element != null && element.isJsonPrimitive && element.asJsonPrimitive.isNumber, "Invalid $name")
        val primitive = element!!.asJsonPrimitive
        requireSecure(primitive.asString.matches(Regex("[0-9]+")), "Invalid integer $name")
        primitive.asLong
    } catch (e: RemoteLinksException) {
        throw e
    } catch (e: Exception) {
        throw RemoteLinksException("Invalid $name", e)
    }

    private fun decodeUtf8(bytes: ByteArray): String = try {
        StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    } catch (e: Exception) {
        throw RemoteLinksException("Remote links config is not valid UTF-8", e)
    }

    private fun requireSecure(condition: Boolean, message: String) {
        if (!condition) throw RemoteLinksException(message)
    }
}
