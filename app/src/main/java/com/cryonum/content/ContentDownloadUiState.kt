package com.cryonum.content

data class ContentDownloadUiState(
    val checkingManifest: Boolean = false,
    val manifestReady: Boolean = false,
    val manifestRevision: Long = 0,
    val lectureCount: Int = 12,
    val lecturesDownloaded: Int = 0,
    val lecturesTotalBytes: Long = 0,
    val policySizeBytes: Long? = null,
    val showLecturePrompt: Boolean = false,
    val showPolicyPrompt: Boolean = false,
    val policyIsUpdate: Boolean = false,
    val progressVisible: Boolean = false,
    val activeBundle: ContentBundle? = null,
    val phase: String = "IDLE",
    val currentFileId: String? = null,
    val currentFileIndex: Int = 0,
    val currentFileCount: Int = 0,
    val currentFileBytes: Long = 0,
    val currentFileTotalBytes: Long = 0,
    val overallBytes: Long = 0,
    val overallTotalBytes: Long = 0,
    val completedFiles: Int = 0,
    val errorCategory: ContentErrorCategory? = null,
    val openPdfPath: String? = null,
    val openPdfContentVersion: Int? = null
) {
    val lecturesRemaining: Int get() = (lectureCount - lecturesDownloaded).coerceAtLeast(0)
    val workActive: Boolean get() = activeBundle != null && phase in setOf("PREPARING", "DOWNLOADING", "VERIFYING", "ENQUEUED")
}
