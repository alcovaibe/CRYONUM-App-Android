package com.icymath.ui.activity

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icymath.R
import com.icymath.ui.theme.IcyMathTheme

/**
 * Мостик для вызова из Java кода.
 */
fun setAboutContent(
    composeView: ComposeView,
    appVersion: String,
    onBackClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSourceCodeClick: () -> Unit
) {
    composeView.setContent {
        IcyMathTheme {
            AboutScreen(appVersion, onBackClick, onPrivacyClick, onSourceCodeClick)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    appVersion: String,
    onBackClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onSourceCodeClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(), // Убрали лишний padding сверху
                title = {
                    Text(
                        text = stringResource(R.string.about_app).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = IcyMathTheme.colors.titleColor,
                        modifier = Modifier.fillMaxWidth().padding(end = 48.dp) // Чуть увеличили отступ для центровки
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
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.12f)) // Немного уменьшили bias, чтобы лого был выше

            Image(
                painter = painterResource(id = R.drawable.ic_logo), // Убедимся, что здесь именно логотип
                contentDescription = null,
                modifier = Modifier.size(100.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.policy),
                color = Color(0xFFE53935),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onPrivacyClick() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.source_code),
                color = Color(0xFF2196F3),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable { onSourceCodeClick() }
                    .padding(8.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = appVersion,
                fontSize = 16.sp,
                color = IcyMathTheme.colors.titleColor.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.weight(0.88f))
        }
    }
}

@Preview(showBackground = true, name = "Light Mode", locale = "ru", apiLevel = 34)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Dark Mode", locale = "ru", apiLevel = 34)
@Composable
fun AboutScreenPreview() {
    IcyMathTheme {
        // Оборачиваем в Surface с фоном, так как экран и тема используют Color.Transparent.
        // Это заставит Preview отображать правильный цвет подложки.
        Surface(
            color = if (IcyMathTheme.colors.isLight) Color.White else Color(0xFF121212)
        ) {
            AboutScreen(
                appVersion = "1.0.0 (42)",
                onBackClick = {},
                onPrivacyClick = {},
                onSourceCodeClick = {}
            )
        }
    }
}
