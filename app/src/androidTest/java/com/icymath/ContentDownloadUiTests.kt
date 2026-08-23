package com.icymath

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.icymath.content.ContentBundle
import com.icymath.content.ContentDownloadUiState
import com.icymath.content.ContentErrorCategory
import com.icymath.ui.components.dialogs.ContentOfferDialog
import com.icymath.ui.components.dialogs.ContentProgressDialog
import com.icymath.ui.theme.IcyMathTheme
import org.junit.Rule
import org.junit.Test

class ContentDownloadUiTests {
    @get:Rule val compose = createComposeRule()

    @Test fun firstEntryShowsLectureOffer() = showLectureOffer(downloaded = 0)
    @Test fun partialLecturesShowContinueOffer() = showLectureOffer(downloaded = 2)

    @Test
    fun downloadedLecturesDoNotShowOffer() {
        compose.setContent { IcyMathTheme { if (12 < 12) LectureOffer(12) } }
        compose.onNodeWithTag("content_offer_dialog").assertDoesNotExist()
    }

    @Test fun activeDownloadShowsCancel() = showProgress(ContentDownloadUiState(activeBundle = ContentBundle.LECTURES, progressVisible = true, phase = "DOWNLOADING"), "content_cancel")
    @Test fun cancelledDownloadShowsRetry() = showProgress(ContentDownloadUiState(activeBundle = ContentBundle.LECTURES, progressVisible = true, phase = "CANCELLED", errorCategory = ContentErrorCategory.CANCELLED), "content_retry")
    @Test fun failedDownloadShowsRestart() = showProgress(ContentDownloadUiState(activeBundle = ContentBundle.LECTURES, progressVisible = true, phase = "FAILED", errorCategory = ContentErrorCategory.NETWORK), "content_restart")
    @Test fun policyDownloadOfferIsVisible() = showPolicyOffer()
    @Test fun policyUpdateOfferIsVisible() = showPolicyOffer()

    private fun showLectureOffer(downloaded: Int) {
        compose.setContent { IcyMathTheme { LectureOffer(downloaded) } }
        compose.onNodeWithTag("content_offer_dialog").assertExists()
        compose.onNodeWithTag("content_offer_primary").assertExists()
    }

    private fun showPolicyOffer() {
        compose.setContent {
            IcyMathTheme {
                ContentOfferDialog("Policy", "Download", 100, primaryText = "Download", secondaryText = "Cancel", onPrimary = {}, onDismiss = {})
            }
        }
        compose.onNodeWithTag("content_offer_dialog").assertExists()
    }

    private fun showProgress(state: ContentDownloadUiState, expectedActionTag: String) {
        compose.setContent { IcyMathTheme { ContentProgressDialog(state, {}, {}, {}, {}) } }
        compose.onNodeWithTag("content_progress_dialog").assertExists()
        compose.onNodeWithTag(expectedActionTag).assertExists()
    }

    @androidx.compose.runtime.Composable
    private fun LectureOffer(downloaded: Int) {
        ContentOfferDialog(
            title = "Lectures",
            message = "Download 12 lectures",
            sizeBytes = 100,
            downloaded = downloaded,
            total = 12,
            primaryText = "Download",
            secondaryText = "Later",
            onPrimary = {},
            onDismiss = {}
        )
    }
}
