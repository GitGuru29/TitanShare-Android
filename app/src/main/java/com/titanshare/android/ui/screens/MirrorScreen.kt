package com.titanshare.android.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.titanshare.android.ui.components.GlassCard
import com.titanshare.android.ui.theme.*
import com.titanshare.android.viewmodel.AppViewModel

@Composable
fun MirrorScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val isActive by vm.mirrorActive.collectAsStateWithLifecycle()
    val error by vm.mirrorError.collectAsStateWithLifecycle()
    val stats by vm.mirrorStats.collectAsStateWithLifecycle()

    val mpm = remember(context) {
        context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    // Request screen-capture permission once the daemon is ready.
    val projectionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            vm.setMirrorPermissionResultCode(result.resultCode)
            vm.startMirror(result.data)
        } else {
            vm.clearMirrorError()
        }
    }

    // Abort any in-progress mirror when leaving the screen.
    DisposableEffect(Unit) {
        onDispose { if (vm.mirrorActive.value) vm.stopMirror() }
    }

    LaunchedEffect(error) {
        if (error != null) {
            kotlinx.coroutines.delay(3000)
            vm.clearMirrorError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Color(0xFF030810))))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 20.dp, top = 48.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
                }
                Text(
                    "Screen Mirror",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                if (isActive) {
                    LiveBadge()
                }
            }

            if (isActive) {
                // ── Active view ────────────────────────────────────────
                ActiveMirrorView(vm = vm, stats = stats)
            } else {
                // ── Idle / setup view ──────────────────────────────────
                IdleMirrorView(
                    onStart = {
                        vm.prepareAndStartMirror {
                            projectionLauncher.launch(mpm.createScreenCaptureIntent())
                        }
                    },
                    error = error,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.IdleMirrorView(onStart: () -> Unit, error: String?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp)
            .weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 32.dp, cornerRadius = 32.dp) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.ScreenShare, null,
                        tint = ElectricBlue, modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Mirror your screen to the Linux PC",
                    color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your phone screen will appear in a window on the paired PC.\n"
                        + "Use it live — touch, scroll and type remotely.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp,
                )
                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = onStart,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    Icon(Icons.Default.Videocam, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Start Mirroring", style = MaterialTheme.typography.labelLarge)
                }

                if (error != null) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        error,
                        color = DangerRed,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.ActiveMirrorView(
    vm: AppViewModel,
    stats: AppViewModel.MirrorStats,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        // ── Live monitor panel ────────────────────────────────────────
        GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 20.dp, cornerRadius = 24.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "LIVE FEED",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 2.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Streaming", color = SuccessGreen, style = MaterialTheme.typography.labelSmall)
                    }
                }

                // Telemetry grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatTile("FPS", "${stats.fps}", ElectricBlue, Modifier.weight(1f))
                    StatTile("QUALITY", "${stats.quality}", PurpleAccent, Modifier.weight(1f))
                    StatTile("REZ", "${stats.width}×${stats.height}", TextSecondary, Modifier.weight(1f))
                }

                Spacer(Modifier.height(4.dp))

                // Dropped frames indicator
                val dropText = if (stats.framesDropped > 0) {
                    "${stats.framesDropped} frames dropped"
                } else {
                    "No dropped frames"
                }
                Text(
                    dropText,
                    color = if (stats.framesDropped > 0) WarningAmber else TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Remote touch surface (mirror preview + input) ─────────────
        Text(
            "TOUCH TO CONTROL",
            color = TextMuted,
            style = MaterialTheme.typography.labelSmall,
            letterSpacing = 2.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(12.dp))

        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 12.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { vm.mouseClick("left") },
                        onLongPress = { vm.mouseClick("right") },
                    )
                },
            cornerRadius = 28.dp,
            innerPadding = 0.dp,
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Gesture, null,
                        tint = GlassBorderBright,
                        modifier = Modifier.size(56.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Your screen is live on the PC.\nTap to click · long-press to right-click.",
                        color = TextMuted,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 20.sp,
                    )
                }
            }
        }

        // ── Stop ───────────────────────────────────────────────────────
        Button(
            onClick = { vm.stopMirror() },
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .height(54.dp),
        ) {
            Icon(Icons.Default.Stop, null, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Stop Mirroring", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            value,
            color = color,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(2.dp))
        Text(label, color = TextMuted, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
    }
}

@Composable
private fun LiveBadge() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(SuccessGreen.copy(alpha = 0.15f))
            .border(1.dp, SuccessGreen.copy(alpha = alpha), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(SuccessGreen.copy(alpha = alpha))
        )
        Spacer(Modifier.width(6.dp))
        Text("LIVE", color = SuccessGreen, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
    }
}
