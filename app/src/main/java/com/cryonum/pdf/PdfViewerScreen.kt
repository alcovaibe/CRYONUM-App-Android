package com.cryonum.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.graphics.createBitmap
import com.cryonum.R
import com.cryonum.managers.PolicyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun PdfViewerScreen(
    filePath: String,
    shouldShowAcceptDialogOnScrollEnd: Boolean,
    onBack: () -> Unit,
    onShowAcceptDialog: () -> Unit,
    onPdfError: () -> Unit,
    isFirstLaunchMode: Boolean = false,
    fromDialogViewAction: Boolean = false,
    policyVersionToAccept: Int? = null,
    onLaunchViewer: (Boolean) -> Unit = {},
    onExitApp: () -> Unit = {}
) {
    val context = LocalContext.current

    PolicyManager.PolicyDialogHandler(
        onLaunchViewer = onLaunchViewer,
        onExitApp = onExitApp
    )

    var pageCount by remember(filePath) { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showAcceptButton by remember { mutableStateOf(isFirstLaunchMode || fromDialogViewAction) }

    val listState = rememberLazyListState()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    var isZoomControlVisible by remember { mutableStateOf(false) }
    var isBackButtonVisible by remember { mutableStateOf(true) }

    LaunchedEffect(filePath) {
        isLoading = true
        try {
            pageCount = withContext(Dispatchers.IO) {
                ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        require(renderer.pageCount in 1..2000)
                        renderer.pageCount
                    }
                }
            }
            isLoading = false
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { onPdfError() }
    }

    // Hide/show back button on scroll without reading mutable state inside the Flow transform.
    LaunchedEffect(listState) {
        var previousIndex = listState.firstVisibleItemIndex
        var previousOffset = listState.firstVisibleItemScrollOffset
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .distinctUntilChanged()
            .collect { (index, scrollOffset) ->
                val isScrollingDown = index > previousIndex ||
                    (index == previousIndex && scrollOffset > previousOffset)
                val hasScrolled = index > 0 || scrollOffset > 50
                isBackButtonVisible = !(isScrollingDown && hasScrolled)
                previousIndex = index
                previousOffset = scrollOffset
            }
    }

    // Show accept dialog at end of scroll.
    if (shouldShowAcceptDialogOnScrollEnd && !isFirstLaunchMode) {
        LaunchedEffect(listState) {
            snapshotFlow {
                val totalItems = listState.layoutInfo.totalItemsCount
                val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                totalItems > 0 && lastVisibleIndex >= totalItems - 1
            }
                .distinctUntilChanged()
                .collect { isAtEnd ->
                    if (isAtEnd) onShowAcceptDialog()
                }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color(0xFFD3D3D3))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            // Main Zoomable Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 3f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = androidx.compose.ui.geometry.Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures {
                            isZoomControlVisible = !isZoomControlVisible
                        }
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(pageCount, key = { "$filePath:$it" }) { index ->
                        PdfPage(filePath, index, onPdfError)
                    }

                    if (showAcceptButton && policyVersionToAccept != null) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    PolicyManager.acceptPolicy(context, policyVersionToAccept)
                                    showAcceptButton = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(bottom = 40.dp)
                                    .height(56.dp),
                                shape = RoundedCornerShape(28.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = androidx.compose.ui.graphics.Color(0xFF4CAF50), // Green
                                    contentColor = androidx.compose.ui.graphics.Color.White
                                )
                            ) {
                                Text(
                                    text = stringResource(id = R.string.accept),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    if (shouldShowAcceptDialogOnScrollEnd && !isFirstLaunchMode) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onShowAcceptDialog,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(bottom = 32.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.view_policy_options_at_end),
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

            }
        }

        // Back Button Overlay
        AnimatedVisibility(
            visible = isBackButtonVisible,
            enter = fadeIn() + slideInHorizontally { -it },
            exit = fadeOut() + slideOutHorizontally { -it },
            modifier = Modifier
                .padding(12.dp)
                .zIndex(1f)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .shadow(4.dp, CircleShape)
                    .background(androidx.compose.ui.graphics.Color.White, CircleShape)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = androidx.compose.ui.graphics.Color.Black,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        // Zoom Control Overlay
        AnimatedVisibility(
            visible = isZoomControlVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(24.dp),
                tonalElevation = 8.dp,
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Slider(
                        value = scale,
                        onValueChange = { scale = it },
                        valueRange = 1f..3f,
                        modifier = Modifier.width(200.dp)
                    )
                }
            }

            // Auto-hide zoom control after 4 seconds
            LaunchedEffect(isZoomControlVisible) {
                if (isZoomControlVisible) {
                    delay(4000.milliseconds)
                    isZoomControlVisible = false
                }
            }
        }
    }
}

/** Only composed pages own bitmaps. Never recycle a bitmap still referenced by a draw command. */
@Composable
private fun PdfPage(filePath: String, index: Int, onError: () -> Unit) {
    var bitmap by remember(filePath, index) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(filePath, index) {
        var pending: Bitmap? = null
        try {
            withContext(Dispatchers.IO) {
                ParcelFileDescriptor.open(File(filePath), ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        renderer.openPage(index).use { page ->
                            val dimensions = PdfRenderBudget.dimensions(page.width, page.height)
                            val image = createBitmap(dimensions.first, dimensions.second, Bitmap.Config.ARGB_8888)
                            pending = image
                            image.eraseColor(Color.WHITE)
                            val matrix = Matrix().apply {
                                setScale(dimensions.first.toFloat() / page.width, dimensions.second.toFloat() / page.height)
                            }
                            page.render(image, null, matrix, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
            }
            bitmap = pending
            pending = null
        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (_: Exception) { onError() }
        finally { pending?.recycle() }
    }
    val image = bitmap
    if (image == null) {
        Box(Modifier.fillMaxWidth().height(400.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        Image(image.asImageBitmap(), contentDescription = "PDF ${index + 1}",
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), contentScale = ContentScale.FillWidth)
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode")
@Composable
fun PdfViewerScreenPreview() {
    MaterialTheme {
        PdfViewerScreen(
            filePath = "",
            shouldShowAcceptDialogOnScrollEnd = false,
            onBack = {},
            onShowAcceptDialog = {},
            onPdfError = {}
        )
    }
}
