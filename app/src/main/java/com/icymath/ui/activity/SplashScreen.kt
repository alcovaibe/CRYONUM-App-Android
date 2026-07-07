package com.icymath.ui.activity

import com.icymath.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.icymath.ui.theme.IcyMathTheme

@Composable
fun SplashScreenContent() {
    val colors = IcyMathTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_logo_splash),
            contentDescription = null,
            modifier = Modifier.size(160.dp)
        )
    }
}

@Preview(showBackground = true, name = "Splash Light", showSystemUi = true)
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, name = "Splash Dark", showSystemUi = true)
@Composable
fun SplashScreenPreview() {
    IcyMathTheme {
        SplashScreenContent()
    }
}

