package com.icymath.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icymath.R
import com.icymath.items.ThemeItem
import com.icymath.managers.ThemeManager
import com.icymath.ui.theme.IcyMathTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSelectionScreen(
    onBackClick: () -> Unit,
    themes: List<ThemeItem>,
    currentTheme: ThemeManager.AppTheme,
    onThemeSelected: (ThemeManager.AppTheme) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    val fontScale = LocalDensity.current.fontScale
                    Text(
                        text = stringResource(R.string.Themes).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
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
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themes) { item ->
                ThemeCard(
                    item = item,
                    isSelected = item.theme == currentTheme,
                    onThemeSelected = onThemeSelected
                )
            }
        }
    }
}

@Composable
fun ThemeCard(
    item: ThemeItem,
    isSelected: Boolean,
    onThemeSelected: (ThemeManager.AppTheme) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onThemeSelected(item.theme) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = IcyMathTheme.colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(item.nameResId),
                fontSize = 18.sp,
                fontWeight = FontWeight.Normal,
                color = IcyMathTheme.colors.titleColor,
                modifier = Modifier.weight(1f)
            )

            if (isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = IcyMathTheme.colors.titleColor
                )
            }
        }
    }
}

object ThemeSelectionScreenBridge {
    @JvmStatic
    fun setThemeSelectionContent(
        composeView: ComposeView,
        onBack: () -> Unit,
        themes: List<ThemeItem>,
        currentTheme: ThemeManager.AppTheme,
        onThemeSelected: (ThemeManager.AppTheme) -> Unit
    ) {
        composeView.setContent {
            IcyMathTheme {
                ThemeSelectionScreen(
                    onBackClick = onBack,
                    themes = themes,
                    currentTheme = currentTheme,
                    onThemeSelected = onThemeSelected
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun ThemeSelectionScreenPreviewLight() {
    val mockThemes = listOf(
        ThemeItem(R.string.ClassicWhite, R.string.desc_theme_classic_light, ThemeManager.AppTheme.LIGHT),
        ThemeItem(R.string.DarkAMOLED, R.string.desc_theme_amoled, ThemeManager.AppTheme.AMOLED),
        ThemeItem(R.string.SandyBrown, R.string.desc_theme_sandybrown, ThemeManager.AppTheme.SANDY_BROWN),
        ThemeItem(R.string.SystemTheme, R.string.desc_theme_system, ThemeManager.AppTheme.SYSTEM)
    )
    IcyMathTheme {
        Surface(color = Color.White) {
            ThemeSelectionScreen(
                onBackClick = {},
                themes = mockThemes,
                currentTheme = ThemeManager.AppTheme.LIGHT,
                onThemeSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ThemeSelectionScreenPreviewDark() {
    val mockThemes = listOf(
        ThemeItem(R.string.ClassicWhite, R.string.desc_theme_classic_light, ThemeManager.AppTheme.LIGHT),
        ThemeItem(R.string.DarkAMOLED, R.string.desc_theme_amoled, ThemeManager.AppTheme.AMOLED),
        ThemeItem(R.string.SandyBrown, R.string.desc_theme_sandybrown, ThemeManager.AppTheme.SANDY_BROWN),
        ThemeItem(R.string.SystemTheme, R.string.desc_theme_system, ThemeManager.AppTheme.SYSTEM)
    )
    IcyMathTheme {
        Surface(color = Color.Black) {
            ThemeSelectionScreen(
                onBackClick = {},
                themes = mockThemes,
                currentTheme = ThemeManager.AppTheme.AMOLED,
                onThemeSelected = {}
            )
        }
    }
}