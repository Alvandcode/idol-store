package com.idol.store.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.idol.store.BuildConfig
import com.idol.store.R

/** صفحه حریم خصوصی و امنیت */
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    LegalScreen(
        title = stringResource(R.string.legal_privacy_title),
        bodyItems = stringArrayResource(R.array.privacy_items).toList(),
        icon = Icons.Filled.Shield,
        onBack = onBack,
    )
}

/** صفحه شرایط استفاده */
@Composable
fun TermsScreen(onBack: () -> Unit) {
    LegalScreen(
        title = stringResource(R.string.legal_terms_title),
        bodyItems = stringArrayResource(R.array.terms_items).toList(),
        icon = Icons.Filled.Description,
        onBack = onBack,
    )
}

/** اسکلت مشترک صفحات حقوقی: هدر گرادیانی + متن رسمی با سکشن‌ها */
@Composable
private fun LegalScreen(
    title: String,
    bodyItems: List<String>,
    icon: ImageVector,
    onBack: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // هدر گرادیانی
        Box(
            Modifier
                .fillMaxWidth()
                .background(HeaderGradient),
        ) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(140.dp)
                    .background(GlowGradient),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                }
                Box(
                    Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(BrandGradient),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontSize = 18.sp, color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.ic_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Idol Store",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.65f),
                        )
                    }
                }
            }
        }

        // متن رسمی — آیتم‌های زوج: عنوان سکشن، فرد: متن
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            bodyItems.forEachIndexed { i, item ->
                if (i % 2 == 0) {
                    Text(
                        item,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = if (i == 0) 0.dp else 16.dp, bottom = 6.dp),
                    )
                } else {
                    Text(
                        item,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 24.sp,
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(Modifier.height(10.dp))
            Text(
                "Idol Store • v${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** کارت کوچک دسترسی سریع صفحات حقوقی و درباره در تب «شما» */
@Composable
fun LegalMiniCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
