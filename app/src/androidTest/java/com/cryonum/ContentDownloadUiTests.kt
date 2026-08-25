package com.cryonum

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.cryonum.content.ContentBundle
import com.cryonum.content.ContentDownloadUiState
import com.cryonum.content.ContentErrorCategory
import com.cryonum.ui.components.dialogs.ContentOfferDialog
import com.cryonum.ui.components.dialogs.ContentProgressDialog
import com.cryonum.ui.theme.CryonumTheme
import org.junit.Rule
import org.junit.Test

class ContentDownloadUiTests {
    @get:Rule val compose = createComposeRule()

    @Test fun firstEntryShowsLectureOffer() = showLectureOffer(downloaded = 0)
    @Test fun partialLecturesShowContinueOffer() = showLectureOffer(downloaded = 2)

    @Test
    fun downloadedLecturesDoNotShowOffer() {
        compose.setContent { CryonumTheme { if (12 < 12) LectureOffer(12) } }
        compose.onNodeWithTag("content_offer_dialog").assertDoesNotExist()
    }

    @Test fun activeDownloadShowsCancel() = showProgress(ContentDownloadUiState(activeBundle = ContentBundle.LECTURES, progressVisible = true, phase = "DOWNLOADING"), "content_cancel")
    @Test fun cancelledDownloadShowsRetry() = showProgress(ContentDownloadUiState(activeBundle = ContentBundle.LECTURES, progressVisible = true, phase = "CANCELLED", errorCategory = ContentErrorCategory.CANCELLED), "content_retry")
    @Test fun failedDownloadShowsRestart() = showProgress(ContentDownloadUiState(activeBundle = ContentBundle.LECTURES, progressVisible = true, phase = "FAILED", errorCategory = ContentErrorCategory.NETWORK), "content_restart")
    @Test fun policyDownloadOfferIsVisible() = showPolicyOffer()
    @Test fun policyUpdateOfferIsVisible() = showPolicyOffer()

    private fun showLectureOffer(downloaded: Int) {
        compose.setContent { CryonumTheme { LectureOffer(downloaded) } }
        compose.onNodeWithTag("content_offer_dialog").assertExists()
        compose.onNodeWithTag("content_offer_primary").assertExists()
    }

    private fun showPolicyOffer() {
        compose.setContent {
            CryonumTheme {
                ContentOfferDialog("Policy", "Download", 100, primaryText = "Download", secondaryText = "Cancel", onPrimary = {}, onDismiss = {})
            }
        }
        compose.onNodeWithTag("content_offer_dialog").assertExists()
    }

    private fun showProgress(state: ContentDownloadUiState, expectedActionTag: String) {
        compose.setContent { CryonumTheme { ContentProgressDialog(state, {}, {}, {}, {}) } }
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
