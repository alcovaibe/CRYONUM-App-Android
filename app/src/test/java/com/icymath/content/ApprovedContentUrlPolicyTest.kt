package com.icymath.content

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertThrows
import org.junit.Test

class ApprovedContentUrlPolicyTest {
    @Test fun httpIsRejected() = rejectUrl("http://download.icymath.com/lectures/Basic%20Algebraic%20Structures.pdf")
    @Test fun otherHostIsRejected() = rejectUrl("https://evil.example/lectures/Basic%20Algebraic%20Structures.pdf")
    @Test fun nonStandardPortIsRejected() = rejectUrl("https://download.icymath.com:8443/lectures/Basic%20Algebraic%20Structures.pdf")

    @Test
    fun exactExistingR2LectureKeyIsAccepted() {
        val path = "lectures/Basic Algebraic Structures.pdf"
        ApprovedContentUrlPolicy.validateRelativePath(path, ContentCategory.LECTURE, 1, "lecture-01")
        val url = ApprovedContentUrlPolicy.resolve(lecture(path))
        org.junit.Assert.assertEquals(
            "https://download.icymath.com/lectures/Basic%20Algebraic%20Structures.pdf",
            url.toString()
        )
    }

    @Test
    fun changedCaseOrDifferentLectureNameIsRejected() {
        rejectPath("lectures/basic algebraic structures.pdf")
        rejectPath("lectures/Lecture 01.pdf")
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
}
