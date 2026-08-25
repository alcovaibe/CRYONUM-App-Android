package com.cryonum.ui.activity

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
import com.cryonum.R
import com.cryonum.items.LanguageItem
import com.cryonum.ui.theme.CryonumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onBackClick: () -> Unit,
    languages: List<LanguageItem>,
    onLanguageSelected: (LanguageItem) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    val fontScale = LocalDensity.current.fontScale
                    Text(
                        text = stringResource(R.string.language).uppercase(),
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
            items(languages) { language ->
                LanguageCard(language, onLanguageSelected)
            }
        }
    }
}

@Composable
fun LanguageCard(
    language: LanguageItem,
    onLanguageSelected: (LanguageItem) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onLanguageSelected(language) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = CryonumTheme.colors.cardBackground
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
            // Флаг
            Text(
                text = language.flagEmoji,
                fontSize = 24.sp,
                modifier = Modifier.padding(end = 12.dp)
            )

            // Название языка
            Text(
                text = language.displayName,
                fontSize = 18.sp,
                color = CryonumTheme.colors.titleColor,
                modifier = Modifier.weight(1f)
            )

            // Галочка выбора
            if (language.isSelected) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = CryonumTheme.colors.titleColor
                )
            }
        }
    }
}

object LanguageScreenBridge {
    @JvmStatic
    fun setLanguageContent(
        composeView: ComposeView,
        onBack: () -> Unit,
        languages: List<LanguageItem>,
        onLanguageSelected: (LanguageItem) -> Unit
    ) {
        composeView.setContent {
            CryonumTheme {
                LanguageScreen(
                    onBackClick = onBack,
                    languages = languages,
                    onLanguageSelected = onLanguageSelected
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun LanguageScreenPreviewLight() {
    val mockLanguages = listOf(
        LanguageItem("ru", "Русский", "🇷🇺", true),
        LanguageItem("en", "English", "🇺🇸", false),
        LanguageItem("de", "Deutsch", "🇩🇪", false)
    )
    CryonumTheme {
        Surface(color = Color.White) {
            LanguageScreen(
                onBackClick = {},
                languages = mockLanguages,
                onLanguageSelected = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun LanguageScreenPreviewDark() {
    val mockLanguages = listOf(
        LanguageItem("ru", "Русский", "🇷🇺", true),
        LanguageItem("en", "English", "🇺🇸", false),
        LanguageItem("de", "Deutsch", "🇩🇪", false)
    )
    CryonumTheme {
        Surface(color = Color.Black) {
            LanguageScreen(
                onBackClick = {},
                languages = mockLanguages,
                onLanguageSelected = {}
            )
        }
    }
}
