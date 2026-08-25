package com.cryonum.ui.activity

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryonum.R
import com.cryonum.items.HistoryItem
import com.cryonum.ui.menu.AppBottomNavigation
import com.cryonum.ui.theme.CryonumTheme
import com.cryonum.ui.styles.AppStyles
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalLocale
import com.cryonum.ui.components.dialogs.DeleteHistoryDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBackClick: () -> Unit,
    historyItems: List<HistoryItem>,
    onItemClick: (HistoryItem) -> Unit,
    onDeleteSelected: (List<HistoryItem>) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    var selectedItems by remember { mutableStateOf(setOf<HistoryItem>()) }
    var isInSelectionMode by remember { mutableStateOf(false) }
    
    var showFirstDeleteDialog by remember { mutableStateOf(false) }

    val closeSelection = {
        selectedItems = emptySet()
        isInSelectionMode = false
    }

    if (showFirstDeleteDialog) {
        DeleteHistoryDialog(
            count = selectedItems.size,
            onConfirm = {
                onDeleteSelected(selectedItems.toList())
                showFirstDeleteDialog = false
                closeSelection()
            },
            onDismiss = { showFirstDeleteDialog = false }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (isInSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedItems.size,
                    isAllSelected = selectedItems.size == historyItems.size,
                    onCloseClick = closeSelection,
                    onDeleteClick = {
                        showFirstDeleteDialog = true
                    },
                    onSelectAllClick = {
                        if (selectedItems.size < historyItems.size) {
                            selectedItems = historyItems.toSet()
                        } else {
                            selectedItems = emptySet()
                        }
                    }
                )
            } else {
                NormalTopBar(onBackClick)
            }
        },
        bottomBar = bottomBar,
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (historyItems.isEmpty()) {
            EmptyHistoryMessage(padding)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Using itemsIndexed with a unique key combining timestamp and index to avoid duplicate key exceptions.
                // This is necessary because multiple items might have the same lastAccessed timestamp if created quickly.
                itemsIndexed(historyItems, key = { index, item -> "${item.lastAccessed}_$index" }) { _, item ->
                    val isSelected = selectedItems.contains(item)
                    HistoryEntryCard(
                        item = item,
                        isSelected = isSelected,
                        isInSelectionMode = isInSelectionMode,
                        onClick = {
                            if (isInSelectionMode) {
                                selectedItems = if (isSelected) selectedItems - item else selectedItems + item
                                if (selectedItems.isEmpty()) isInSelectionMode = false
                            } else {
                                onItemClick(item)
                            }
                        },
                        onLongClick = {
                            if (!isInSelectionMode) {
                                isInSelectionMode = true
                                selectedItems = setOf(item)
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NormalTopBar(onBackClick: () -> Unit) {
    CenterAlignedTopAppBar(
        modifier = Modifier.statusBarsPadding(),
        title = {
            val fontScale = LocalDensity.current.fontScale
            Text(
                text = stringResource(R.string.history).uppercase(),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    textAlign = TextAlign.Center
                ),
                maxLines = if (fontScale > 1.1f) Int.MAX_VALUE else 1,
                color = CryonumTheme.colors.titleColor
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(32.dp),
                    tint = CryonumTheme.colors.titleColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    selectedCount: Int,
    isAllSelected: Boolean,
    onCloseClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onSelectAllClick: () -> Unit
) {
    TopAppBar(
        modifier = Modifier.statusBarsPadding(),
        title = {
            Text(
                text = androidx.compose.ui.res.pluralStringResource(
                    R.plurals.selected_count_plurals,
                    selectedCount,
                    selectedCount
                ),
                style = MaterialTheme.typography.titleLarge,
                color = CryonumTheme.colors.titleColor
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_back),
                    contentDescription = null,
                    tint = CryonumTheme.colors.titleColor
                )
            }
        },
        actions = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onSelectAllClick() }.padding(end = 8.dp)
            ) {
                Checkbox(
                    checked = isAllSelected,
                    onCheckedChange = null, // Handled by row click
                    colors = CheckboxDefaults.colors(
                        checkedColor = CryonumTheme.colors.confirmButtonBackground,
                        uncheckedColor = CryonumTheme.colors.titleColor.copy(alpha = 0.6f)
                    )
                )
                Text(
                    text = stringResource(R.string.select_all),
                    style = MaterialTheme.typography.bodyLarge,
                    color = CryonumTheme.colors.titleColor
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_bin),
                    contentDescription = stringResource(R.string.delete),
                    tint = CryonumTheme.colors.titleColor
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = if (CryonumTheme.colors.isLight) Color(0xFFE0E0E0) else Color(0xFF333333))
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryEntryCard(
    item: HistoryItem,
    isSelected: Boolean,
    isInSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val textColor = CryonumTheme.colors.titleColor
    val cardBgColor = CryonumTheme.colors.cardBackground

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(AppStyles.CardCornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = AppStyles.CardElevation)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Column: Rows
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (item.type == HistoryItem.HistoryType.SUBSTITUTION) {
                                item.topRow ?: ""
                            } else {
                                item.expression ?: ""
                            },
                            style = AppStyles.CardTitleStyle.copy(
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                        )
                        Text(
                            text = if (item.type == HistoryItem.HistoryType.SUBSTITUTION) {
                                item.bottomRow ?: ""
                            } else {
                                "= ${item.result ?: ""}"
                            },
                            modifier = Modifier.padding(top = 4.dp),
                            style = AppStyles.CardTitleStyle.copy(
                                fontFamily = FontFamily.Monospace,
                                color = textColor
                            )
                        )
                    }

                    // Right Column: Inversions & Parity
                    if (item.type == HistoryItem.HistoryType.SUBSTITUTION) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.inversions, item.inversionCount),
                                style = AppStyles.CardTitleStyle.copy(
                                    color = textColor
                                )
                            )
                            val parityLocalized = when (item.parity) {
                                "even" -> stringResource(R.string.even)
                                "odd" -> stringResource(R.string.odd)
                                else -> item.parity ?: ""
                            }
                            Text(
                                text = stringResource(R.string.history_parity, parityLocalized),
                                modifier = Modifier.padding(top = 4.dp),
                                style = AppStyles.CardTitleStyle.copy(
                                    color = textColor
                                )
                            )
                        }
                    }
                }

                Text(
                    text = SimpleDateFormat("dd.MM.yyyy HH:mm", LocalLocale.current.platformLocale).format(Date(item.lastAccessed)),
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.5f)
                )
            }

            if (isInSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
fun EmptyHistoryMessage(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.history_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = CryonumTheme.colors.titleColor.copy(0.6f),
            modifier = Modifier.padding(32.dp)
        )
    }
}

object HistoryScreenBridge {
    @JvmStatic
    fun setHistoryContent(
        composeView: ComposeView,
        onBack: () -> Unit,
        historyItems: List<HistoryItem>,
        onItemClick: (HistoryItem) -> Unit,
        onDeleteSelected: (List<HistoryItem>) -> Unit,
        onMenuAction: (Int) -> Unit
    ) {
        composeView.setContent {
            CryonumTheme {
                HistoryScreen(
                    onBackClick = onBack,
                    historyItems = historyItems,
                    onItemClick = onItemClick,
                    onDeleteSelected = onDeleteSelected,
                    bottomBar = {
                        AppBottomNavigation(
                            currentRoute = R.id.nav_history,
                            onItemSelected = onMenuAction
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru", name = "Russian", apiLevel = 34)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode", apiLevel = 34)
@Composable
fun HistoryScreenPreview() {
    val mockItems = listOf(
        HistoryItem("1 2 3", "3 2 1", 3, "odd"),
        HistoryItem("2 + 2 * 2", "6")
    )
    CryonumTheme {
        // Wrap the screen in a Surface with a background color for the preview.
        // This is necessary because the HistoryScreen Scaffold uses Color.Transparent
        // by default, which can lead to invisible text in the preview's Dark Mode.
        Surface(
            color = if (isSystemInDarkTheme()) Color(0xFF121212) else Color.White
        ) {
            HistoryScreen(
                onBackClick = {},
                historyItems = mockItems,
                onItemClick = {},
                onDeleteSelected = {},
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
                            Text("Нижняя панель (Превью)")
                        }
                    }
                }
            )
        }
    }
}
