package com.icymath.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.icymath.ui.components.Keyboard
import com.icymath.ui.theme.IcyMathTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    onBackClick: () -> Unit,
    title: String,
    pin: String,
    isBiometricEnabled: Boolean = false,
    onPinChange: (String) -> Unit,
    onBiometricClick: () -> Unit = {},
    showError: Boolean = false,
    isModeUnlock: Boolean = false
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    val fontScale = LocalDensity.current.fontScale
                    Text(
                        text = stringResource(R.string.security).uppercase(),
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
                    if (!isModeUnlock) {
                        IconButton(onClick = onBackClick) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_back),
                                contentDescription = stringResource(R.string.back),
                                modifier = Modifier.size(32.dp),
                                tint = IcyMathTheme.colors.titleColor
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = IcyMathTheme.colors.titleColor,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Pin Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < pin.length
                        val color = if (showError) Color.Red else if (isFilled) IcyMathTheme.colors.titleColor else IcyMathTheme.colors.cardStroke
                        
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }

            Keyboard(
                onNumberClick = { if (pin.length < 4) onPinChange(pin + it) },
                onBackspaceClick = { if (pin.isNotEmpty()) onPinChange(pin.dropLast(1)) },
                onClearClick = { onPinChange("") },
                isBiometricEnabled = isBiometricEnabled,
                onBiometricClick = onBiometricClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

object SecurityScreenBridge {
    @Composable
    fun SecurityContent(
        title: String,
        pin: String,
        isBiometricEnabled: Boolean,
        onPinChange: (String) -> Unit,
        onBiometricClick: () -> Unit,
        showError: Boolean,
        isModeUnlock: Boolean,
        onBack: () -> Unit
    ) {
        IcyMathTheme {
            SecurityScreen(
                onBackClick = onBack,
                title = title,
                pin = pin,
                isBiometricEnabled = isBiometricEnabled,
                onPinChange = onPinChange,
                onBiometricClick = onBiometricClick,
                showError = showError,
                isModeUnlock = isModeUnlock
            )
        }
    }
}
