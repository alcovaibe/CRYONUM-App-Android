package com.icymath.ui.components

import androidx.compose.ui.tooling.preview.Preview
import com.icymath.ui.theme.IcyMathTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import com.icymath.ui.theme.LocalAppTheme
import com.icymath.managers.ThemeManager
import java.util.Locale
import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icymath.R

@Composable
fun FirstLaunchPolicyDialog(
    onReadClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(28.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.policy_first_launch_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.policy_first_launch_message),
                style = MaterialTheme.typography.bodyMedium,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onReadClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.policy_read_button),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Preview(name = "Light Theme - Russian", locale = "ru", showBackground = true)
@Composable
fun PreviewPolicyDialogLightRu() {
    val configuration = Configuration(LocalConfiguration.current).apply {
        setLocale(Locale("ru"))
    }
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalAppTheme provides ThemeManager.AppTheme.LIGHT
    ) {
        IcyMathTheme {
            FirstLaunchPolicyDialog(onReadClick = {})
        }
    }
}

@Preview(name = "Sandy Theme - German", locale = "de", showBackground = true)
@Composable
fun PreviewPolicyDialogSandyDe() {
    val configuration = Configuration(LocalConfiguration.current).apply {
        setLocale(Locale("de"))
    }
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalAppTheme provides ThemeManager.AppTheme.SANDY_BROWN
    ) {
        IcyMathTheme {
            FirstLaunchPolicyDialog(onReadClick = {})
        }
    }
}

@Preview(name = "Amoled Theme - English", locale = "en", showBackground = true)
@Composable
fun PreviewPolicyDialogAmoledEn() {
    val configuration = Configuration(LocalConfiguration.current).apply {
        setLocale(Locale("en"))
    }
    CompositionLocalProvider(
        LocalConfiguration provides configuration,
        LocalAppTheme provides ThemeManager.AppTheme.AMOLED
    ) {
        IcyMathTheme {
            FirstLaunchPolicyDialog(onReadClick = {})
        }
    }
}
