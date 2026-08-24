package com.cryonum.ui.activity

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryonum.R
import com.cryonum.content.ContentDownloadUiState
import com.cryonum.content.ContentDownloadViewModel
import com.cryonum.items.ItemType
import com.cryonum.items.ReferenceItem
import com.cryonum.ui.menu.AppBottomNavigation
import com.cryonum.ui.components.dialogs.ContentOfferDialog
import com.cryonum.ui.components.dialogs.ContentProgressDialog
import com.cryonum.ui.theme.IcyMathTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceMaterialScreen(
    title: String,
    items: List<ReferenceItem>,
    onBackClick: () -> Unit,
    onItemClick: (ReferenceItem) -> Unit,
    bottomBar: @Composable () -> Unit,
    downloadState: ContentDownloadUiState = ContentDownloadUiState(),
    onStartDownload: () -> Unit = {},
    onDismissOffer: () -> Unit = {},
    onShowProgress: () -> Unit = {},
    onHideProgress: () -> Unit = {},
    onCancelDownload: () -> Unit = {},
    onRetryDownload: () -> Unit = {},
    onRestartDownload: () -> Unit = {},
    onOpenPdf: (String) -> Unit = {},
    onPdfOpened: () -> Unit = {}
) {
    LaunchedEffect(downloadState.openPdfPath) {
        downloadState.openPdfPath?.let {
            onOpenPdf(it)
            onPdfOpened()
        }
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    val fontScale = LocalDensity.current.fontScale
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center
                        ),
                        maxLines = if (fontScale > 1.1f) Int.MAX_VALUE else 1,
                        color = IcyMathTheme.colors.titleColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_back),
                            contentDescription = stringResource(R.string.back),
                            modifier = Modifier.size(32.dp),
                            tint = IcyMathTheme.colors.titleColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (items.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = "No items", color = IcyMathTheme.colors.titleColor)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (downloadState.workActive || downloadState.errorCategory != null) {
                    item {
                        Card(
                            onClick = onShowProgress,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = IcyMathTheme.colors.cardBackground)
                        ) {
                            Text(
                                text = when {
                                    downloadState.errorCategory != null -> stringResource(R.string.content_retry)
                                    downloadState.phase == "VERIFYING" -> stringResource(R.string.content_state_verifying)
                                    downloadState.phase == "ENQUEUED" -> stringResource(R.string.content_state_waiting_network)
                                    else -> stringResource(R.string.content_state_downloading)
                                },
                                modifier = Modifier.padding(16.dp),
                                color = IcyMathTheme.colors.titleColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                items(items) { item ->
                    ReferenceCard(item, onItemClick)
                }
            }
        }
    }

    if (downloadState.showLecturePrompt) {
        ContentOfferDialog(
            title = stringResource(R.string.content_lectures_offer_title),
            message = stringResource(R.string.content_lectures_offer_message) + "\n\n" + stringResource(R.string.content_storage_note),
            sizeBytes = downloadState.lecturesTotalBytes.takeIf { it > 0 },
            downloaded = downloadState.lecturesDownloaded,
            total = downloadState.lectureCount,
            primaryText = stringResource(if (downloadState.lecturesDownloaded > 0) R.string.content_continue_download else R.string.content_download),
            secondaryText = stringResource(R.string.content_later),
            onPrimary = onStartDownload,
            onDismiss = onDismissOffer
        )
    }
    if (downloadState.progressVisible) {
        ContentProgressDialog(downloadState, onHideProgress, onCancelDownload, onRetryDownload, onRestartDownload)
    }
}

@Composable
fun ReferenceCard(item: ReferenceItem, onClick: (ReferenceItem) -> Unit) {
    Card(
        onClick = { onClick(item) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = IcyMathTheme.colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titleResId = item.titleResId
            Text(
                text = if (titleResId != null) stringResource(titleResId) else "",
                fontSize = 17.sp,
                color = IcyMathTheme.colors.titleColor,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = IcyMathTheme.colors.titleColor.copy(0.5f)
            )
        }
    }
}

object ReferenceMaterialScreenBridge {
    @JvmStatic
    fun setReferenceContent(
        composeView: ComposeView,
        title: String,
        items: List<ReferenceItem>,
        onBack: () -> Unit,
        onItemClick: (ReferenceItem) -> Unit,
        onMenuAction: (Int) -> Unit,
        downloadViewModel: ContentDownloadViewModel,
        onOpenPdf: (String) -> Unit
    ) {
        composeView.setContent {
            val downloadState by downloadViewModel.uiState.collectAsState()
            IcyMathTheme {
                ReferenceMaterialScreen(
                    title = title,
                    items = items,
                    onBackClick = onBack,
                    onItemClick = onItemClick,
                    downloadState = downloadState,
                    onStartDownload = downloadViewModel::startLectures,
                    onDismissOffer = downloadViewModel::dismissLecturePrompt,
                    onShowProgress = downloadViewModel::showProgress,
                    onHideProgress = downloadViewModel::hideProgress,
                    onCancelDownload = downloadViewModel::cancel,
                    onRetryDownload = downloadViewModel::retry,
                    onRestartDownload = downloadViewModel::restart,
                    onOpenPdf = onOpenPdf,
                    onPdfOpened = downloadViewModel::consumeOpenPdf,
                    bottomBar = {
                        AppBottomNavigation(
                            currentRoute = R.id.nav_reference,
                            onItemSelected = onMenuAction
                        )
                    }
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Light Mode", apiLevel = 34)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode", apiLevel = 34)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, name = "Russian", locale = "ru", apiLevel = 34)
@Composable
fun ReferenceMaterialScreenPreview() {
    val mockItems = listOf(
        ReferenceItem(ItemType.SUBJECT, null, R.string.algebra_and_number_theory),
        ReferenceItem(ItemType.SUBJECT, null, R.string.permutations)
    )
    IcyMathTheme {
        Surface(
            color = if (isSystemInDarkTheme()) Color(0xFF121212) else Color.White
        ) {
            ReferenceMaterialScreen(
                title = stringResource(R.string.reference_material),
                items = mockItems,
                onBackClick = {},
                onItemClick = {},
                bottomBar = {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        tonalElevation = 3.dp,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Bottom Bar (Preview)")
                        }
                    }
                }
            )
        }
    }
}
