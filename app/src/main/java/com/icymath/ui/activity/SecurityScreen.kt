package com.icymath.ui.activity

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icymath.R
import com.icymath.managers.ThemeManager
import com.icymath.ui.components.keyboard.Keyboard
import com.icymath.ui.theme.IcyMathTheme
import com.icymath.ui.theme.LocalAppTheme

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecuritySettingsScreen(
    isAppLockEnabled: Boolean,
    onAppLockToggle: (Boolean) -> Unit,
    isBiometricEnabled: Boolean,
    onBiometricToggle: (Boolean) -> Unit,
    onBackClick: () -> Unit
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
                // Pin Lock Card
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
                            text = stringResource(R.string.app_lock_label),
                            fontSize = 16.sp,
                            color = IcyMathTheme.colors.titleColor,
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = isAppLockEnabled,
                            onCheckedChange = onAppLockToggle
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Description
                Text(
                    text = stringResource(R.string.app_lock_description),
                    fontSize = 14.sp,
                    color = IcyMathTheme.colors.titleColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 4.dp),
                    lineHeight = 20.sp
                )

                if (isAppLockEnabled) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Biometric Card
                    Card(
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
                                .height(56.dp)
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.biometric_unlock),
                                fontSize = 16.sp,
                                color = IcyMathTheme.colors.titleColor,
                                modifier = Modifier.weight(1f)
                            )

                            Switch(
                                checked = isBiometricEnabled,
                                onCheckedChange = onBiometricToggle
                            )
                        }
                    }
                }
            }
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

    @Composable
    fun SecuritySettingsContent(
        isAppLockEnabled: Boolean,
        onAppLockToggle: (Boolean) -> Unit,
        isBiometricEnabled: Boolean,
        onBiometricToggle: (Boolean) -> Unit,
        onBack: () -> Unit
    ) {
        IcyMathTheme {
            SecuritySettingsScreen(
                isAppLockEnabled = isAppLockEnabled,
                onAppLockToggle = onAppLockToggle,
                isBiometricEnabled = isBiometricEnabled,
                onBiometricToggle = onBiometricToggle,
                onBackClick = onBack
            )
        }
    }
}

@Preview(showBackground = true, name = "Light English", locale = "en")
@Composable
fun SecurityScreenPreviewLight() {
    CompositionLocalProvider(LocalAppTheme provides ThemeManager.AppTheme.LIGHT) {
        IcyMathTheme {
            Surface(color = Color.White) {
                SecurityScreen(
                    onBackClick = {},
                    title = "Enter PIN",
                    pin = "12",
                    isBiometricEnabled = true,
                    onPinChange = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "AMOLED Russian", locale = "ru", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun SecurityScreenPreviewAmoled() {
    CompositionLocalProvider(LocalAppTheme provides ThemeManager.AppTheme.AMOLED) {
        IcyMathTheme {
            Surface(color = Color.Black) {
                SecurityScreen(
                    onBackClick = {},
                    title = "Введите PIN-код",
                    pin = "123",
                    isBiometricEnabled = true,
                    onPinChange = {}
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "SandyBrown German", locale = "de")
@Composable
fun SecurityScreenPreviewSandy() {
    CompositionLocalProvider(LocalAppTheme provides ThemeManager.AppTheme.SANDY_BROWN) {
        IcyMathTheme {
            Surface(color = Color(0xFFF4A460)) {
                SecurityScreen(
                    onBackClick = {},
                    title = "PIN eingeben",
                    pin = "1",
                    isBiometricEnabled = true,
                    onPinChange = {}
                )
            }
        }
    }
}
