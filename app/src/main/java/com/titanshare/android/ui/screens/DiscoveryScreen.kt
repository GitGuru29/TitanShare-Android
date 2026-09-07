package com.titanshare.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material.icons.filled.PhoneIphone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.titanshare.android.data.model.Device
import com.titanshare.android.ui.theme.*
import com.titanshare.android.viewmodel.AppViewModel

@Composable
fun DiscoveryScreen(vm: AppViewModel, onDeviceSelected: () -> Unit) {
    val devices     by vm.discoveredDevices.collectAsStateWithLifecycle()
    val isScanning  by vm.isScanning.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { vm.startDiscovery() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(listOf(NavyDeep, Color(0xFF020509))))
    ) {
        
        // ── Background Ambient Glow ───────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-100).dp)
                .size(300.dp)
                .background(Brush.radialGradient(listOf(ElectricBlue.copy(alpha=0.15f), Color.Transparent)))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(56.dp))

            // ── Header ──────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "TitanShare",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        if (isScanning) "Searching for nearby PCs..." else "Discovery paused",
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricBlue,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Beautiful frosted refresh button
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(GlassWhite.copy(alpha = 0.05f))
                        .border(1.dp, GlassWhite.copy(alpha = 0.1f), CircleShape)
                        .clickable {
                            vm.stopDiscovery()
                            vm.startDiscovery()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }

            Spacer(Modifier.weight(0.2f))

            // ── Premium Radar ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentAlignment = Alignment.Center
            ) {
                RadarAnimation(isScanning = isScanning)
            }
            
            Spacer(Modifier.weight(0.1f))

            // ── Devices Area ─────────────────────────────────────────
            Box(modifier = Modifier.weight(0.6f)) {
                if (devices.isEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Looking for TitanShare",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ensure your Linux PC is connected to the same Wi-Fi\nand the daemon is running.",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "AVAILABLE DEVICES",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary,
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(16.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            items(devices, key = { it.host }) { device ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = slideInVertically(initialOffsetY = { 50 }) + fadeIn()
                                ) {
                                    PremiumDeviceCard(device = device) {
                                        vm.selectDevice(device)
                                        onDeviceSelected()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadarAnimation(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    
    // Rotating sweep
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_rotation"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(300.dp)) {
        
        // Concentric circles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            for (i in 1..3) {
                drawCircle(
                    color = ElectricBlue.copy(alpha = 0.15f - (i * 0.04f)),
                    radius = (size.width / 2) * (i / 3f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
            
            // Sweep beam
            if (isScanning) {
                rotate(degrees = rotation, pivot = center) {
                    drawCircle(
                        brush = Brush.sweepGradient(
                            0f to Color.Transparent,
                            0.7f to Color.Transparent, // Keep the rest of the circle totally invisible
                            0.8f to ElectricBlue.copy(alpha = 0.15f),
                            0.97f to ElectricBlue.copy(alpha = 0.6f),
                            1f to Color(0xFF66B2FF).copy(alpha = 0.9f) // Bright leading edge
                        ),
                        radius = size.width / 2,
                        center = center
                    )
                }
            }
        }

        // Animated expansion rings
        repeat(2) { i ->
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, delayMillis = i * 1250, easing = EaseOutCubic),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ring_scale_$i"
            )
            val alpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2500, delayMillis = i * 1250, easing = EaseOutCubic),
                    repeatMode = RepeatMode.Restart
                ),
                label = "ring_alpha_$i"
            )
            
            if (isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scale)
                        .border(2.dp, ElectricBlue.copy(alpha = alpha * 0.5f), CircleShape)
                )
            }
        }

        // Center Phone Icon
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(NavyCardLight, NavyCard)))
                .border(2.dp, ElectricBlue.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.PhoneIphone, 
                    contentDescription = null, 
                    tint = Color.White, 
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun PremiumDeviceCard(device: Device, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(
                GlassWhite.copy(alpha = 0.08f),
                GlassWhite.copy(alpha = 0.03f)
            )))
            .border(1.dp, GlassWhite.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            
            // Device Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(
                        ElectricBlue.copy(alpha = 0.3f),
                        ElectricBlue.copy(alpha = 0.05f)
                    )))
                    .border(1.dp, ElectricBlue.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Computer, null, tint = ElectricBlue, modifier = Modifier.size(26.dp))
            }

            Spacer(Modifier.width(16.dp))

            // Device Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name.ifEmpty { "Unknown Linux PC" }, 
                    style = MaterialTheme.typography.titleMedium, 
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Ready to connect",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Connect button implicit visually
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(ElectricBlue.copy(alpha = 0.15f))
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    "Connect", 
                    color = ElectricBlue, 
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
