package com.icymath.ui.activity

import androidx.compose.foundation.Image
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
import com.icymath.ui.theme.IcyMathTheme

data class SettingItemCompose(
    val nameRes: Int,
    val descriptionRes: Int,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    settings: List<SettingItemCompose>
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    val fontScale = LocalDensity.current.fontScale
                    Text(
                        text = stringResource(R.string.settings).uppercase(),
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
            items(settings) { item ->
                SettingCard(item)
            }
        }
    }
}

@Composable
fun SettingCard(item: SettingItemCompose) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { item.onClick() },
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(item.nameRes),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Normal,
                    color = IcyMathTheme.colors.titleColor
                )
                if (item.descriptionRes != 0 && item.descriptionRes != item.nameRes) {
                    Text(
                        text = stringResource(item.descriptionRes),
                        fontSize = 14.sp,
                        color = IcyMathTheme.colors.titleColor.copy(alpha = 0.7f)
                    )
                }
            }

            Image(
                painter = painterResource(id = R.drawable.ic_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(IcyMathTheme.colors.titleColor)
            )
        }
    }
}

object SettingsScreenBridge {
    @JvmStatic
    fun setSettingsContent(
        composeView: ComposeView,
        onBack: () -> Unit,
        settings: List<SettingItemCompose>
    ) {
        composeView.setContent {
            IcyMathTheme {
                SettingsScreen(
                    onBackClick = onBack,
                    settings = settings
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "Light Mode", locale = "ru")
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode", locale = "ru")
@Composable
fun SettingsScreenPreview() {
    val mockSettings = listOf(
        SettingItemCompose(R.string.Themes, R.string.Themes, {}),
        SettingItemCompose(R.string.language, R.string.language, {}),
        SettingItemCompose(R.string.analytics, R.string.analytics, {}),
        SettingItemCompose(R.string.security, R.string.security, {})
    )
    IcyMathTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(
                onBackClick = {},
                settings = mockSettings
            )
        }
    }
}
