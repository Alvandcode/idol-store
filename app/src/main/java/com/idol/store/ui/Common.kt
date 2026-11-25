package com.idol.store.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.idol.store.R
import com.idol.store.R as R2

/** گرادیان برند اپ — از فیروزه‌ای نئونی لوگو به بنفش نیلی */
val BrandGradient: Brush = Brush.linearGradient(listOf(Color(0xFF22D3EE), Color(0xFF6366F1)))

/** گرادیان سرمه‌ای عمیق برای هدرها — هماهنگ با پس‌زمینه لوگو */
val HeaderGradient: Brush = Brush.verticalGradient(
    listOf(Color(0xFF0B1626), Color(0xFF070A12)),
)

/** گلو نئونی فیروزه‌ای برای پشت آیکون‌ها و هدرها */
val GlowGradient: Brush = Brush.radialGradient(
    listOf(Color(0xFF22D3EE).copy(alpha = 0.22f), Color(0xFF22D3EE).copy(alpha = 0f)),
)

/** آیکون اپ با fallback به لوگوی برند وقتی آیکون remote نیست یا لود نمی‌شود */
@Composable
fun AppIcon(
    model: Any?,
    contentDescription: String?,
    size: Dp,
    corner: Dp = size * 0.26f,
    modifier: Modifier = Modifier,
) {
    val brand: Painter = painterResource(R.drawable.ic_logo)
    AsyncImage(
        model = model,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        placeholder = brand,
        error = brand,
        fallback = brand,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(corner)),
    )
}

/** هدر سکشن با نوار گرادیان برند + لینک «همه» */
@Composable
fun SectionHeader(
    title: String,
    showViewAll: Boolean = false,
    onViewAll: () -> Unit = {},
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 18.dp)
                .clip(CircleShape)
                .background(BrandGradient),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
        )
        if (showViewAll) {
            Text(
                stringResource(R.string.view_all),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickableNoRipple(onViewAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

/** کلیک بدون افکت ripple (برای لینک‌های متنی کوچک) */
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier = composed {
    clickable(
        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
        indication = null,
        onClick = onClick,
    )
}

/** چیپ آمار کوچک (مثلاً «۱۲ اپ») */
@Composable
fun StatChip(text: String) {
    Box(
        Modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** کارت تنظیمات: آیکون رنگی + عنوان + زیرعنوان */
@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                if (subtitle != null) {
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
