package com.icymath.content

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

object ApprovedContentUrlPolicy {
    val BASE_URL: HttpUrl = "https://download.icymath.com/".toHttpUrl()
    val MANIFEST_URL: HttpUrl = BASE_URL.newBuilder()
        .addPathSegment("manifests")
        .addPathSegment("content-v1.signed.json")
        .build()

    private val lecturePath = Regex("lectures/v[1-9][0-9]{0,8}/lecture-(0[1-9]|1[0-2])\\.pdf")
    private val policyPath = Regex(
        "privacy-policy/v[1-9][0-9]{0,8}(?:\\.[0-9]{1,3}){1,2}/privacy-policy\\.pdf"
    )

    fun validateRelativePath(
        path: String,
        category: ContentCategory,
        order: Int,
        id: String,
        contentVersion: String? = null
    ) {
        reject(path.isEmpty(), "Empty content path")
        reject(path.any { it.code < 0x20 || it.code > 0x7e }, "Non-ASCII or control character in path")
        reject('\\' in path || '%' in path || '?' in path || '#' in path, "Ambiguous content path")
        reject(path.startsWith('/') || "://" in path, "Absolute content URL is forbidden")
        val segments = path.split('/')
        reject(segments.any { it.isEmpty() || it == "." || it == ".." }, "Invalid content path segment")

        when (category) {
            ContentCategory.LECTURE -> {
                reject(!id.matches(Regex("lecture-(0[1-9]|1[0-2])")), "Invalid lecture id")
                reject(id != "lecture-${order.toString().padStart(2, '0')}", "Lecture id/order mismatch")
                reject(!lecturePath.matches(path), "Unapproved lecture path")
                if (contentVersion != null) {
                    reject(path != "lectures/v$contentVersion/$id.pdf", "Lecture path/version mismatch")
                }
            }
            ContentCategory.PRIVACY_POLICY -> {
                reject(id != "privacy-policy" || order != 1, "Invalid privacy policy identity")
                reject(!policyPath.matches(path), "Unapproved privacy policy path")
            }
        }
    }

    fun resolve(file: ContentManifestFile): HttpUrl {
        validateRelativePath(file.path, file.category, file.order, file.id, file.contentVersion)
        return BASE_URL.newBuilder().addPathSegments(file.path).build().also(::validateResolvedUrl)
    }

    fun validateResolvedUrl(url: HttpUrl) {
        reject(url.scheme != "https", "Only HTTPS is allowed")
        reject(url.host != "download.icymath.com", "Unapproved content host")
        reject(url.port != 443, "Non-standard port is forbidden")
        reject(url.username.isNotEmpty() || url.password.isNotEmpty(), "URL userinfo is forbidden")
        reject(url.query != null || url.fragment != null, "Query and fragment are forbidden")
        reject(url.host.matches(Regex("(?:\\d{1,3}\\.){3}\\d{1,3}")), "IP literals are forbidden")
    }

    private fun reject(condition: Boolean, message: String) {
        if (condition) throw ContentException(ContentErrorCategory.SECURITY, message)
    }
}
