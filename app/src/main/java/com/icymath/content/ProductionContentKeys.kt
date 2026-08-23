package com.icymath.content

import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

object ProductionContentKeys {
    /*
     * Production placeholder. Add the real X.509 SubjectPublicKeyInfo base64 value only after
     * the offline private key has been created. An empty map deliberately makes every manifest
     * fail closed with "unknown keyId"; there is no unsigned release fallback.
     */
    private val TRUSTED_X509_KEYS: Map<String, String> = emptyMap()

    fun trustedKeys(): Map<String, PublicKey> = TRUSTED_X509_KEYS.mapNotNull { (keyId, encoded) ->
        runCatching {
            val bytes = Base64.getDecoder().decode(encoded)
            keyId to KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(bytes))
        }.getOrNull()
    }.toMap()
}
