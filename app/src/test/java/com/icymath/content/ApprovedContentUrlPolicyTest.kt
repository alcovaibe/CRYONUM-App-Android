package com.icymath.content

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertThrows
import org.junit.Test

class ApprovedContentUrlPolicyTest {
    @Test fun httpIsRejected() = rejectUrl("http://download.icymath.com/lectures/v1/lecture-01.pdf")
    @Test fun otherHostIsRejected() = rejectUrl("https://evil.example/lectures/v1/lecture-01.pdf")
    @Test fun nonStandardPortIsRejected() = rejectUrl("https://download.icymath.com:8443/lectures/v1/lecture-01.pdf")

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
}
