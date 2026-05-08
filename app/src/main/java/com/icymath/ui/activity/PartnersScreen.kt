package com.icymath.ui.activity

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.icymath.ui.theme.IcyMathTheme
import com.icymath.managers.ContentConfig
import com.icymath.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnersScreen(
    onBackClick: () -> Unit,
    onNafuLinkClick: () -> Unit,
    onKometLinkClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val showKomet = remember { ContentConfig.isExtraContentEnabled(context) }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = {
                    val fontScale = LocalDensity.current.fontScale
                    Text(
                        text = stringResource(R.string.partners).uppercase(),
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
                    .padding(horizontal = 16.dp)
            ) {
                PartnerCard(
                    logoResId = R.drawable.ic_narfu_logo,
                    nameResId = R.string.name_narfu,
                    descriptionResId = R.string.Copyright_text,
                    linkTextResId = R.string.narfu_go_to_website,
                    onLinkClick = onNafuLinkClick
                )
                if (showKomet) {
                    PartnerCard(
                        logoResId = R.drawable.ic_partner_metko,
                        nameResId = R.string.name_komet,
                        descriptionResId = R.string.description_komet,
                        linkTextResId = R.string.komet_go_to_telegram,
                        onLinkClick = onKometLinkClick
                    )
                }
            }
        }
    }
}

@Composable
fun PartnerCard(
    logoResId: Int,
    nameResId: Int,
    descriptionResId: Int,
    linkTextResId: Int,
    onLinkClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 270f else 90f, label = "arrowRotation")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = IcyMathTheme.colors.cardBackground
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = logoResId),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )

                Text(
                    text = stringResource(nameResId),
                    fontSize = 18.sp,
                    color = IcyMathTheme.colors.titleColor,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.ic_arrow_right),
                    contentDescription = stringResource(R.string.card_expand_arrow),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(IcyMathTheme.colors.titleColor)
                )
            }

            // Expandable content
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = stringResource(descriptionResId),
                        fontSize = 16.sp,
                        color = IcyMathTheme.colors.titleColor.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(linkTextResId),
                        color = IcyMathTheme.colors.confirmButtonBackground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { onLinkClick() }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

object PartnersScreenBridge {
    @JvmStatic
    fun setPartnersContent(
        composeView: ComposeView,
        onBack: () -> Unit,
        onNafuLink: () -> Unit,
        onKometLink: () -> Unit
    ) {
        composeView.setContent {
            IcyMathTheme {
                PartnersScreen(
                    onBackClick = onBack,
                    onNafuLinkClick = onNafuLink,
                    onKometLinkClick = onKometLink
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "ru")
@Composable
fun PartnersScreenPreviewLight() {
    IcyMathTheme {
        Surface(color = Color.White) {
            PartnersScreen(
                onBackClick = {},
                onNafuLinkClick = {},
                onKometLinkClick = {}
            )
        }
    }
}

@Preview(showBackground = true, locale = "ru", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PartnersScreenPreviewDark() {
    IcyMathTheme {
        Surface(color = Color.Black) {
            PartnersScreen(
                onBackClick = {},
                onNafuLinkClick = {},
                onKometLinkClick = {}
            )
        }
    }
}
