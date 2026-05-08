package com.icymath.ui.activity

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.icymath.R
import com.icymath.items.ItemType
import com.icymath.items.ReferenceItem
import com.icymath.ui.menu.AppBottomNavigation
import com.icymath.ui.theme.IcyMathTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReferenceMaterialScreen(
    title: String,
    items: List<ReferenceItem>,
    onBackClick: () -> Unit,
    onItemClick: (ReferenceItem) -> Unit,
    bottomBar: @Composable () -> Unit
) {
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
                items(items) { item ->
                    ReferenceCard(item, onItemClick)
                }
            }
        }
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
        onMenuAction: (Int) -> Unit
    ) {
        composeView.setContent {
            IcyMathTheme {
                ReferenceMaterialScreen(
                    title = title,
                    items = items,
                    onBackClick = onBack,
                    onItemClick = onItemClick,
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
