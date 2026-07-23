package com.titanshare.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.titanshare.android.ui.theme.GlassBorder
import com.titanshare.android.ui.theme.NavyCard
import com.titanshare.android.ui.theme.NavyCardLight

/**
 * Glassmorphism card — deep navy with subtle top-edge highlight and border.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    innerPadding: Dp = 20.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                // Deep navy gradient fill
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(NavyCardLight, NavyCard),
                        startY = 0f,
                        endY = size.height,
                    )
                )
                // Subtle top highlight line (glass edge)
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(cornerRadius.toPx(), 0f),
                    end = Offset(size.width - cornerRadius.toPx(), 0f),
                    strokeWidth = 1f,
                )
            }
            .border(width = 1.dp, color = GlassBorder, shape = shape)
            .padding(innerPadding),
        content = content,
    )
}
