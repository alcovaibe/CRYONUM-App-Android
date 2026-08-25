package com.cryonum.links

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RemoteLinksConfigParserTest {
    @Test
    fun parsesTelegramLinkAndIgnoresUnrelatedSiteFields() {
        val config = RemoteLinksConfigParser.parse(
            json("https://t.me/cryonum", extra = ", \"site\": {\"name\": \"Icy Math\"}")
        )

        assertEquals(7L, config.revision)
        assertEquals("https://t.me/cryonum", config.telegramUrl.toString())
        assertEquals("cryonum", TelegramUrlPolicy.username(config.telegramUrl))
    }

    @Test
    fun rejectsUnknownSchemaVersion() {
        assertRejected("""{"schemaVersion":2,"revision":1,"socials":{"telegram":"https://t.me/cryonum"}}""")
    }

    @Test
    fun rejectsHttp() = assertRejected(jsonString("http://t.me/invalid_channel"))

    @Test
    fun rejectsForeignHost() = assertRejected(jsonString("https://example.com/invalid_channel"))

    @Test
    fun rejectsQuery() = assertRejected(jsonString("https://t.me/cryonum?start=unsafe"))

    @Test
    fun rejectsNestedPath() = assertRejected(jsonString("https://t.me/s/cryonum"))

    @Test
    fun rejectsMissingTelegramLink() {
        assertRejected("""{"schemaVersion":1,"revision":1,"socials":{}}""")
    }

    private fun json(url: String, extra: String = ""): ByteArray = jsonString(url, extra).toByteArray()

    private fun jsonString(url: String, extra: String = ""): String =
        """{"schemaVersion":1,"revision":7,"socials":{"telegram":"$url"}$extra}"""

    private fun assertRejected(json: String) {
        assertThrows(RemoteLinksException::class.java) {
            RemoteLinksConfigParser.parse(json.toByteArray())
        }
    }
}
