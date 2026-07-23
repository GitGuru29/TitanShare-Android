package com.titanshare.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanshare.android.ui.theme.ElectricBlue
import com.titanshare.android.ui.theme.SuccessGreen

@Composable
fun AirDropOverlay(
    isVisible: Boolean,
    progress: Float,
    isSending: Boolean,
    filename: String,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "progress"
    )

    val isDone = progress >= 1f
    
    // Auto-dismiss after 2 seconds when done
    LaunchedEffect(isDone) {
        if (isDone) {
            kotlinx.coroutines.delay(2000)
            onDismiss()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing rings
                if (!isDone) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = ElectricBlue.copy(alpha = pulseAlpha),
                            radius = (size.minDimension / 4) * pulseScale,
                            center = Offset(size.width / 2, size.height / 2)
                        )
                    }
                }

                // Progress track & ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    drawCircle(
                        color = Color.DarkGray,
                        radius = (size.minDimension / 2) - (strokeWidth / 2),
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = if (isDone) SuccessGreen else ElectricBlue,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center Icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Done",
                            tint = SuccessGreen,
                            modifier = Modifier.size(64.dp)
                        )
                    } else {
                        Icon(
                            if (isSending) Icons.Default.Computer else Icons.Default.PhoneAndroid,
                            contentDescription = "Device",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = if (isDone) "Completed" else if (isSending) "Sending to Linux PC..." else "Receiving from Linux PC...",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = filename,
                color = Color.LightGray,
                fontSize = 14.sp,
                maxLines = 1
            )
        }
    }
}
