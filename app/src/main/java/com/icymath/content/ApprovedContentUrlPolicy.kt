package com.icymath.content

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

object ApprovedContentUrlPolicy {
    val BASE_URL: HttpUrl = "https://download.icymath.com/".toHttpUrl()
    val MANIFEST_URL: HttpUrl = BASE_URL.newBuilder()
        .addPathSegment("manifests")
        .addPathSegment("content-v1.signed.json")
        .build()

    /**
     * Stable IDs stay machine-friendly, while paths match the existing case-sensitive R2 keys.
     * Keep this order aligned with LectureId and the signed manifest's order field.
     */
    internal val lectureFileNames = listOf(
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
    private val policyPath = Regex("privacy-policy/v[1-9][0-9]{0,8}/privacy-policy\\.pdf")

    fun validateRelativePath(path: String, category: ContentCategory, order: Int, id: String) {
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
                val expectedName = lectureFileNames.getOrNull(order - 1)
                reject(expectedName == null || path != "lectures/$expectedName", "Unapproved lecture path")
            }
            ContentCategory.PRIVACY_POLICY -> {
                reject(id != "privacy-policy" || order != 1, "Invalid privacy policy identity")
                reject(!policyPath.matches(path), "Unapproved privacy policy path")
            }
        }
    }

    fun resolve(file: ContentManifestFile): HttpUrl {
        validateRelativePath(file.path, file.category, file.order, file.id)
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
