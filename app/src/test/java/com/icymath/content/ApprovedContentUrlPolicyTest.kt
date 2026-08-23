package com.icymath.content

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertThrows
import org.junit.Test

class ApprovedContentUrlPolicyTest {
    @Test fun httpIsRejected() = rejectUrl("http://download.icymath.com/lectures/v1/lecture-01.pdf")
    @Test fun otherHostIsRejected() = rejectUrl("https://evil.example/lectures/v1/lecture-01.pdf")
    @Test fun nonStandardPortIsRejected() = rejectUrl("https://download.icymath.com:8443/lectures/v1/lecture-01.pdf")

    @Test
    fun exactExistingR2LectureKeyIsAccepted() {
        val path = "lectures/v1/lecture-01.pdf"
        ApprovedContentUrlPolicy.validateRelativePath(path, ContentCategory.LECTURE, 1, "lecture-01", "1")
        val url = ApprovedContentUrlPolicy.resolve(lecture(path))
        org.junit.Assert.assertEquals(
            "https://download.icymath.com/lectures/v1/lecture-01.pdf",
            url.toString()
        )
    }

    @Test
    fun exactExistingR2PolicyKeyIsAccepted() {
        val path = "privacy-policy/v4.0/privacy-policy.pdf"
        ApprovedContentUrlPolicy.validateRelativePath(path, ContentCategory.PRIVACY_POLICY, 1, "privacy-policy")
        org.junit.Assert.assertEquals(
            "https://download.icymath.com/privacy-policy/v4.0/privacy-policy.pdf",
            ApprovedContentUrlPolicy.resolve(policy(path)).toString()
        )
    }

    @Test
    fun mismatchedPolicyNamingIsRejected() {
        assertThrows(ContentException::class.java) {
            ApprovedContentUrlPolicy.validateRelativePath(
                "privacy-policy/privacy-policy.4.0.pdf",
                ContentCategory.PRIVACY_POLICY,
                1,
                "privacy-policy"
            )
        }
    }

    @Test
    fun changedCaseOrDifferentLectureNameIsRejected() {
        rejectPath("lectures/lecture-01.pdf")
        rejectPath("lectures/v1/Lecture-01.pdf")
    }

    @Test
    fun lectureVersionDirectoryMustMatchContentVersion() {
        assertThrows(ContentException::class.java) {
            ApprovedContentUrlPolicy.validateRelativePath(
                "lectures/v2/lecture-01.pdf",
                ContentCategory.LECTURE,
                1,
                "lecture-01",
                "1"
            )
        }
    }

    @Test
    fun absoluteUrlInPathIsRejected() {
        rejectPath("https://evil.example/file.pdf")
    }

    @Test
    fun traversalIsRejected() {
        rejectPath("lectures/v1/../privacy-policy.pdf")
    }

    @Test
    fun encodedTraversalIsRejected() {
        rejectPath("lectures/v1/%2e%2e/privacy-policy.pdf")
        rejectPath("lectures/v1/%252e%252e/privacy-policy.pdf")
    }

    private fun rejectUrl(value: String) {
        assertThrows(ContentException::class.java) { ApprovedContentUrlPolicy.validateResolvedUrl(value.toHttpUrl()) }
    }

    private fun rejectPath(value: String) {
        assertThrows(ContentException::class.java) {
            ApprovedContentUrlPolicy.validateRelativePath(value, ContentCategory.LECTURE, 1, "lecture-01")
        }
    }

    private fun lecture(path: String) = ContentManifestFile(
        id = "lecture-01",
        category = ContentCategory.LECTURE,
        order = 1,
        displayName = mapOf("ru" to "Основные алгебраические структуры", "en" to "Basic Algebraic Structures"),
        path = path,
        contentVersion = "1",
        sizeBytes = 1,
        sha256 = "a".repeat(64),
        contentType = "application/pdf"
    )

    private fun policy(path: String) = ContentManifestFile(
        id = "privacy-policy",
        category = ContentCategory.PRIVACY_POLICY,
        order = 1,
        displayName = mapOf("ru" to "Политика конфиденциальности", "en" to "Privacy Policy"),
        path = path,
        contentVersion = "4",
        sizeBytes = 1,
        sha256 = "b".repeat(64),
        contentType = "application/pdf"
    )
}
