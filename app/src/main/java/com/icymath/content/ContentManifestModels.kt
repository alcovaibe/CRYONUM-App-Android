package com.icymath.content

data class ContentManifest(
    val revision: Long,
    val generatedAt: String,
    val files: List<ContentManifestFile>
) {
    val lectures: List<ContentManifestFile>
        get() = files.filter { it.category == ContentCategory.LECTURE }.sortedBy { it.order }

    val privacyPolicy: ContentManifestFile
        get() = files.single { it.category == ContentCategory.PRIVACY_POLICY }
}

data class ContentManifestFile(
    val id: String,
    val category: ContentCategory,
    val order: Int,
    val displayName: Map<String, String>,
    val path: String,
    val contentVersion: String,
    val sizeBytes: Long,
    val sha256: String,
    val contentType: String
)

enum class ContentCategory(val wireName: String) {
    LECTURE("lecture"),
    PRIVACY_POLICY("privacy_policy");

    companion object {
        fun fromWireName(value: String): ContentCategory? = entries.firstOrNull { it.wireName == value }
    }
}

enum class ContentBundle(val workName: String) {
    LECTURES("icy-content-lectures"),
    PRIVACY_POLICY("icy-content-privacy-policy")
}

enum class ContentErrorCategory {
    NETWORK,
    FILE_UNAVAILABLE,
    SECURITY,
    CORRUPT_FILE,
    INSUFFICIENT_SPACE,
    FILE_SYSTEM,
    CANCELLED,
    UNKNOWN
}

class ContentException(
    val category: ContentErrorCategory,
    message: String,
    val retryable: Boolean = false,
    val retryAfterMillis: Long? = null,
    cause: Throwable? = null
) : Exception(message, cause)

data class LocalContentSummary(
    val verifiedCount: Int,
    val totalCount: Int,
    val verifiedBytes: Long,
    val totalBytes: Long,
    val current: Boolean
)
