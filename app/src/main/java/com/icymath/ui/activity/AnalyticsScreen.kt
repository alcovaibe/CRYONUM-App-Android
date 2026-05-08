package com.icymath.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    initialIsAnalyticsEnabled: Boolean,
    onToggleAnalytics: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    // Добавляем внутреннее состояние для мгновенного переключения
    var isEnabled by remember { mutableStateOf(initialIsAnalyticsEnabled) }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    Text(
                        text = stringResource(R.string.analytics).uppercase(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            textAlign = TextAlign.Center
                        ),
                        color = IcyMathTheme.colors.titleColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 48.dp)
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                // Card with Switch
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = IcyMathTheme.colors.cardBackground
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.analytics_collection),
                            fontSize = 16.sp,
                            color = IcyMathTheme.colors.titleColor,
                            modifier = Modifier.weight(1f)
                        )

                        // Оставляем кликабельным только свитч
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { newValue ->
                                isEnabled = newValue
                                onToggleAnalytics(newValue)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = stringResource(R.string.analytics_info),
                    fontSize = 14.sp,
                    color = IcyMathTheme.colors.titleColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * Bridge function for Java Activity
 */
object AnalyticsScreenBridge {
    @JvmStatic
    fun setAnalyticsContent(
        composeView: ComposeView,
        initialEnabled: Boolean,
        onToggle: (Boolean) -> Unit,
        onBack: () -> Unit
    ) {
        composeView.setContent {
            IcyMathTheme {
                AnalyticsScreen(
                    initialIsAnalyticsEnabled = initialEnabled,
                    onToggleAnalytics = onToggle,
                    onBackClick = onBack
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun AnalyticsScreenPreviewLight() {
    IcyMathTheme {
        Surface(color = Color.White) {
            AnalyticsScreen(
                initialIsAnalyticsEnabled = true,
                onToggleAnalytics = {},
                onBackClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AnalyticsScreenPreviewDark() {
    IcyMathTheme {
        Surface(color = Color.Black) {
            AnalyticsScreen(
                initialIsAnalyticsEnabled = false,
                onToggleAnalytics = {},
                onBackClick = {}
            )
        }
    }
}
