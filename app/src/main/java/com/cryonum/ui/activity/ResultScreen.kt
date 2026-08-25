package com.cryonum.ui.activity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cryonum.R
import com.cryonum.ui.theme.CryonumTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    inversions: Int,
    parity: String,
    onBackClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    val parityLocalized = when (parity) {
        "even" -> stringResource(R.string.even)
        "odd" -> stringResource(R.string.odd)
        else -> parity
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = { },
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
                actions = {
                    IconButton(onClick = onHistoryClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_history),
                            contentDescription = stringResource(R.string.history),
                            modifier = Modifier.size(32.dp),
                            tint = CryonumTheme.colors.titleColor
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
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.inversions, inversions),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = CryonumTheme.colors.titleColor,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = stringResource(R.string.history_parity, parityLocalized),
                fontSize = 24.sp,
                color = CryonumTheme.colors.titleColor,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }
    }
}

object ResultScreenBridge {
    @JvmStatic
    fun setResultContent(
        composeView: ComposeView,
        inversions: Int,
        parity: String,
        onBack: () -> Unit,
        onHistory: () -> Unit
    ) {
        composeView.setContent {
            CryonumTheme {
                ResultScreen(
                    inversions = inversions,
                    parity = parity,
                    onBackClick = onBack,
                    onHistoryClick = onHistory
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ResultScreenPreview() {
    CryonumTheme {
        ResultScreen(
            inversions = 10,
            parity = "even",
            onBackClick = {},
            onHistoryClick = {}
        )
    }
}
