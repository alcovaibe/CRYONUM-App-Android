package com.cryonum.ui.components.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cryonum.R
import com.cryonum.content.ContentBundle
import com.cryonum.content.ContentDownloadUiState
import com.cryonum.content.ContentErrorCategory
import com.cryonum.ui.theme.IcyMathTheme
import java.text.DecimalFormat

@Composable
fun ContentOfferDialog(
    title: String,
    message: String,
    sizeBytes: Long?,
    downloaded: Int? = null,
    total: Int? = null,
    primaryText: String,
    secondaryText: String,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("content_offer_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = IcyMathTheme.colors.dialogBackground,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = IcyMathTheme.colors.titleColor, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                Text(message, fontSize = 16.sp, lineHeight = 22.sp, color = IcyMathTheme.colors.titleColor.copy(alpha = 0.82f), textAlign = TextAlign.Center)
                if (total != null && downloaded != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(stringResource(R.string.content_downloaded_count, downloaded, total), color = IcyMathTheme.colors.titleColor)
                    Text(stringResource(R.string.content_remaining_count, (total - downloaded).coerceAtLeast(0)), color = IcyMathTheme.colors.titleColor)
                }
                if (sizeBytes != null && sizeBytes > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.content_total_size, formatBytes(sizeBytes)), color = IcyMathTheme.colors.titleColor.copy(alpha = 0.72f))
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onPrimary,
                    modifier = Modifier.fillMaxWidth().testTag("content_offer_primary"),
                    colors = ButtonDefaults.buttonColors(containerColor = IcyMathTheme.colors.confirmButtonBackground),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(12.dp)
                ) { Text(primaryText, fontWeight = FontWeight.SemiBold) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().testTag("content_offer_secondary")) {
                    Text(secondaryText, color = IcyMathTheme.colors.titleColor.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun ContentProgressDialog(
    state: ContentDownloadUiState,
    onHide: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRestart: () -> Unit
) {
    Dialog(
        onDismissRequest = onHide,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("content_progress_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = IcyMathTheme.colors.dialogBackground,
            tonalElevation = 6.dp
        ) {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text(
                    text = if (state.activeBundle == ContentBundle.PRIVACY_POLICY) stringResource(R.string.content_policy_download_title) else stringResource(R.string.content_lectures_download_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = IcyMathTheme.colors.titleColor
                )
                Spacer(Modifier.height(12.dp))
                Text(phaseText(state), color = IcyMathTheme.colors.titleColor, fontWeight = FontWeight.SemiBold)
                if (state.currentFileCount > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (state.activeBundle == ContentBundle.LECTURES) stringResource(R.string.content_current_lecture, state.currentFileIndex, state.currentFileCount)
                        else stringResource(R.string.content_privacy_policy),
                        color = IcyMathTheme.colors.titleColor.copy(alpha = 0.82f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                ProgressBlock(
                    label = stringResource(R.string.content_current_file_progress),
                    current = state.currentFileBytes,
                    total = state.currentFileTotalBytes
                )
                Spacer(Modifier.height(14.dp))
                ProgressBlock(
                    label = stringResource(R.string.content_overall_progress),
                    current = state.overallBytes,
                    total = state.overallTotalBytes
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    stringResource(R.string.content_bytes_progress, formatBytes(state.overallBytes), formatBytes(state.overallTotalBytes)),
                    color = IcyMathTheme.colors.titleColor.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onHide, modifier = Modifier.testTag("content_hide")) { Text(stringResource(R.string.content_hide)) }
                    if (state.phase in setOf("FAILED", "CANCELLED")) {
                        TextButton(onClick = onRestart, modifier = Modifier.testTag("content_restart")) { Text(stringResource(R.string.content_start_over)) }
                        Button(onClick = onRetry, modifier = Modifier.testTag("content_retry"), shape = RoundedCornerShape(12.dp)) { Text(stringResource(R.string.content_retry)) }
                    } else if (state.phase != "COMPLETED") {
                        TextButton(onClick = onCancel, modifier = Modifier.testTag("content_cancel")) { Text(stringResource(R.string.cancel)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressBlock(label: String, current: Long, total: Long) {
    val progress = if (total > 0) (current.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    Text(label, color = IcyMathTheme.colors.titleColor.copy(alpha = 0.8f), fontSize = 14.sp)
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier.fillMaxWidth().semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }
    )
}

@Composable
private fun phaseText(state: ContentDownloadUiState): String = when {
    state.errorCategory != null -> errorText(state.errorCategory)
    state.phase == "PREPARING" -> stringResource(R.string.content_state_preparing)
    state.phase == "DOWNLOADING" -> stringResource(R.string.content_state_downloading)
    state.phase == "VERIFYING" -> stringResource(R.string.content_state_verifying)
    state.phase == "COMPLETED" -> stringResource(R.string.content_state_completed)
    state.phase == "ENQUEUED" -> stringResource(R.string.content_state_waiting_network)
    state.phase == "CANCELLED" -> stringResource(R.string.content_state_cancelled)
    else -> stringResource(R.string.content_state_preparing)
}

@Composable
private fun errorText(category: ContentErrorCategory): String = stringResource(
    when (category) {
        ContentErrorCategory.NETWORK -> R.string.content_error_network
        ContentErrorCategory.FILE_UNAVAILABLE -> R.string.content_error_unavailable
        ContentErrorCategory.SECURITY -> R.string.content_error_security
        ContentErrorCategory.CORRUPT_FILE -> R.string.content_error_corrupt
        ContentErrorCategory.INSUFFICIENT_SPACE -> R.string.content_error_space
        ContentErrorCategory.FILE_SYSTEM -> R.string.content_error_file_system
        ContentErrorCategory.CANCELLED -> R.string.content_state_cancelled
        ContentErrorCategory.UNKNOWN -> R.string.content_error_unknown
    }
)

private fun formatBytes(value: Long): String {
    if (value <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    var amount = value.toDouble()
    var index = 0
    while (amount >= 1024 && index < units.lastIndex) {
        amount /= 1024
        index++
    }
    return "${DecimalFormat("0.#").format(amount)} ${units[index]}"
}
