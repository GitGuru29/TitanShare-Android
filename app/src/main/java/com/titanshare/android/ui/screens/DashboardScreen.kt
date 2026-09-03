package com.titanshare.android.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.titanshare.android.data.model.SystemInfo
import com.titanshare.android.ui.components.GlassCard
import com.titanshare.android.ui.navigation.Screen
import com.titanshare.android.ui.theme.*
import com.titanshare.android.viewmodel.AppViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    vm: AppViewModel,
    onNavigate: (String) -> Unit,
    onDisconnect: () -> Unit,
) {
    val device   by vm.selectedDevice.collectAsStateWithLifecycle()
    val sysInfo  by vm.systemInfo.collectAsStateWithLifecycle()
    val toast    by vm.toastMessage.collectAsStateWithLifecycle()
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var showPowerDialog      by remember { mutableStateOf(false) }

    LaunchedEffect(toast) {
        if (toast != null) delay(1500)
        vm.clearToast()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Color(0xFF030810))))
    )  {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top Bar ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 16.dp, top = 52.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        device?.name ?: "Linux PC",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Black,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AnimatedConnectedDot()
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Connected • ${device?.host}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                IconButton(
                    onClick = { showDisconnectDialog = true },
                    modifier = Modifier.clip(CircleShape).background(GlassWhite)
                ) {
                    Icon(Icons.Default.LinkOff, "Disconnect", tint = TextPrimary, modifier = Modifier.size(20.dp))
                }
            }

            // ── Scrollable content ────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(8.dp))

                // ── Bento Row 1: Quick Actions ────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    BentoAction(Icons.Default.Mouse, "Trackpad", ElectricBlue, Modifier.weight(1f).aspectRatio(1f)) {
                        onNavigate(Screen.Trackpad.route)
                    }
                    BentoAction(Icons.Default.Keyboard, "Keyboard", PurpleAccent, Modifier.weight(1f).aspectRatio(1f)) {
                        onNavigate(Screen.Keyboard.route)
                    }
                    BentoAction(Icons.Default.UploadFile, "Send", SuccessGreen, Modifier.weight(1f).aspectRatio(1f)) {
                        onNavigate(Screen.FileTransfer.route)
                    }
                    BentoAction(Icons.Default.Download, "Receive", ElectricBlue, Modifier.weight(1f).aspectRatio(1f)) {
                        onNavigate(Screen.LinuxFiles.route)
                    }
                }

                // ── Screen Mirror feature card ─────────────────────────
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onNavigate(Screen.Mirror.route) },
                    innerPadding = 0.dp,
                    cornerRadius = 24.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(ElectricBlue.copy(alpha = 0.35f), PurpleAccent.copy(alpha = 0.35f)))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ScreenShare, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Screen Mirror",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                "Stream your screen to the PC in real time",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                }

                // ── System Stats ──────────────────────────────────────
                if (sysInfo == null) {
                    GlassCard(modifier = Modifier.fillMaxWidth().height(200.dp), cornerRadius = 32.dp) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = ElectricBlue, modifier = Modifier.size(48.dp), strokeWidth = 4.dp)
                        }
                    }
                } else {
                    sysInfo?.let { info ->
                        // Device Model Card
                        GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 16.dp, cornerRadius = 24.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(ElectricBlue.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Computer, null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text("${info.brand} ${info.model}", color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(info.osVersion, color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // ── CPU Task Manager Card ─────────────────────
                        CpuTaskManagerCard(info = info)

                        // Bento Row 3: RAM + Disk Gauges
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            BentoGaugeCard(
                                label = "RAM", 
                                progress = (info.ramUsage.toFloatOrNull() ?: 0f) / 100f, 
                                valueText = "${info.ramUsage}%", 
                                color = RamColor, 
                                modifier = Modifier.weight(1f).aspectRatio(1f), 
                                subText = "${info.ramUsed}/${info.ramTotal} GB"
                            )
                            BentoGaugeCard(
                                label = "STORAGE", 
                                progress = (info.storageUsage.toFloatOrNull() ?: 0f) / 100f, 
                                valueText = "${info.storageUsage}%", 
                                color = StorageColor, 
                                modifier = Modifier.weight(1f).aspectRatio(1f), 
                                subText = "${info.storageUsed}/${info.storageTotal} GB"
                            )
                        }

                        // Battery
                        if (info.battery > 0) {
                            GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 18.dp, cornerRadius = 24.dp) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val battColor = if (info.battery > 40) SuccessGreen else DangerRed
                                    Icon(
                                        when {
                                            info.battery > 80 -> Icons.Default.BatteryFull
                                            info.battery > 40 -> Icons.Default.Battery4Bar
                                            else -> Icons.Default.BatteryAlert
                                        },
                                        null,
                                        tint = battColor,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Battery", color = TextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                                            Text("${info.battery}%", color = TextPrimary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                            progress = { info.battery / 100f },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                                            color = battColor,
                                            trackColor = GlassWhite,
                                            strokeCap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Volume Control ────────────────────────────────────
                GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 6.dp, cornerRadius = 100.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { vm.volumeDown() }, modifier = Modifier.padding(start = 4.dp).size(48.dp)) {
                            Icon(Icons.Default.VolumeDown, "Vol-", tint = TextPrimary, modifier = Modifier.size(26.dp))
                        }
                        
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            Text("VOLUME", color = TextSecondary, style = MaterialTheme.typography.labelMedium, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                        }

                        IconButton(onClick = { vm.volumeUp() }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.VolumeUp, "Vol+", tint = TextPrimary, modifier = Modifier.size(26.dp))
                        }
                        
                        Box(modifier = Modifier.padding(horizontal = 8.dp).width(1.dp).height(24.dp).background(GlassBorder))
                        
                        IconButton(onClick = { vm.mute() }, modifier = Modifier.padding(end = 4.dp).size(48.dp)) {
                            Icon(Icons.Default.VolumeOff, "Mute", tint = WarningAmber, modifier = Modifier.size(22.dp))
                        }
                    }
                }

                // ── Power Actions ─────────────────────────────────────
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PowerButton(Icons.Default.Lock, "Lock", Modifier.weight(1f)) { vm.lock() }
                    PowerButton(Icons.Default.Bedtime, "Sleep", Modifier.weight(1f)) { vm.sleep() }
                    PowerButton(Icons.Default.PowerSettingsNew, "Power", Modifier.weight(1f), DangerRed) { showPowerDialog = true }
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        // ── Toast snack ───────────────────────────────────────────────
        if (toast != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                Box(
                    modifier = Modifier
                        .padding(bottom = 32.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(NavyCardLight.copy(alpha = 0.95f))
                        .border(1.dp, GlassBorder, RoundedCornerShape(24.dp))
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(toast ?: "", color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────
    if (showDisconnectDialog) {
        AlertDialog(
            onDismissRequest = { showDisconnectDialog = false },
            title = { Text("Disconnect?") },
            text  = { Text("You'll be taken back to device discovery.") },
            confirmButton = {
                TextButton(onClick = { vm.disconnect(); onDisconnect() }) {
                    Text("Disconnect", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDisconnectDialog = false }) { Text("Cancel") }
            },
            containerColor = NavyCard,
        )
    }

    if (showPowerDialog) {
        AlertDialog(
            onDismissRequest = { showPowerDialog = false },
            title = { Text("Power Options") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PowerOption(Icons.Default.Bedtime,  "Sleep",    WarningAmber) { vm.sleep();    showPowerDialog = false }
                    PowerOption(Icons.Default.Refresh,  "Reboot",   ElectricBlue) { vm.reboot();   showPowerDialog = false }
                    PowerOption(Icons.Default.PowerSettingsNew, "Shutdown", DangerRed) { vm.shutdown(); showPowerDialog = false }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showPowerDialog = false }) { Text("Cancel") }
            },
            containerColor = NavyCard,
        )
    }
}

@Composable
private fun CpuTaskManagerCard(info: SystemInfo) {
    // Rolling history — keep last 60 samples (3 min at 3s polling)
    val history = remember { mutableStateListOf<Float>() }
    val cpuNow = info.cpuLoad.toFloatOrNull() ?: 0f
    LaunchedEffect(info.cpuLoad) {
        history.add(cpuNow)
        if (history.size > 60) history.removeAt(0)
    }

    GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 20.dp, cornerRadius = 28.dp) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        "CPU",
                        color = TextMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Text(
                        "${info.cpuLoad}%",
                        color = CpuColor,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (info.cpuFreqGhz != "0.00" && info.cpuFreqGhz.isNotEmpty()) {
                        Text(
                            "${info.cpuFreqGhz} GHz",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (info.cpuCoreCount > 0) {
                        Text(
                            "${info.cpuCoreCount} logical cores",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Text(
                        "${info.cpuTemp}°C",
                        color = TempColor,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Scrolling history graph — Task Manager style
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF050D1A))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            ) {
                val graphColor = CpuColor
                Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    val w = size.width
                    val h = size.height

                    // Draw grid lines at 25%, 50%, 75%
                    val gridColor = Color.White.copy(alpha = 0.06f)
                    listOf(0.25f, 0.50f, 0.75f).forEach { frac ->
                        val y = h * (1f - frac)
                        drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
                    }

                    if (history.size >= 2) {
                        val step = w / (history.size - 1).toFloat()

                        // Filled area under the curve
                        val fillPath = Path().apply {
                            moveTo(0f, h)
                            history.forEachIndexed { i, v ->
                                val x = i * step
                                val y = h * (1f - (v / 100f).coerceIn(0f, 1f))
                                if (i == 0) lineTo(x, y) else lineTo(x, y)
                            }
                            lineTo((history.size - 1) * step, h)
                            close()
                        }
                        drawPath(
                            fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(graphColor.copy(alpha = 0.35f), Color.Transparent)
                            )
                        )

                        // Stroke line
                        val linePath = Path().apply {
                            history.forEachIndexed { i, v ->
                                val x = i * step
                                val y = h * (1f - (v / 100f).coerceIn(0f, 1f))
                                if (i == 0) moveTo(x, y) else lineTo(x, y)
                            }
                        }
                        drawPath(linePath, color = graphColor, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
                    }
                }

                // Corner labels
                Box(modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Text("100%", color = TextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.TopStart))
                    Text("0%",   color = TextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomStart))
                    Text("60s",  color = TextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.align(Alignment.BottomEnd))
                }
            }

            // Per-core grid — like Task Manager's "All cores" view
            if (info.cpuCoresUsage.isNotEmpty()) {
                Text(
                    "CORES",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                CpuCoreGrid(cores = info.cpuCoresUsage)
            }
        }
    }
}

@Composable
private fun CpuCoreGrid(cores: List<Float>) {
    // 4 columns of mini core bars
    val columns = 4
    val rows = (cores.size + columns - 1) / columns

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (col in 0 until columns) {
                    val idx = row * columns + col
                    if (idx < cores.size) {
                        val usage = cores[idx]
                        val color = when {
                            usage > 80f -> DangerRed
                            usage > 50f -> WarningAmber
                            else        -> CpuColor
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Mini bar graph
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF050D1A))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight((usage / 100f).coerceIn(0f, 1f))
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(color, color.copy(alpha = 0.5f))
                                            )
                                        )
                                        .align(Alignment.BottomCenter)
                                )
                            }
                            Text(
                                "${usage.toInt()}%",
                                color = color,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "C${idx}",
                                color = TextMuted,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedConnectedDot() {
    val infiniteTransition = rememberInfiniteTransition()
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(SuccessGreen.copy(alpha = alpha))
    )
}

@Composable
private fun BentoAction(
    icon: ImageVector, label: String, color: Color,
    modifier: Modifier, onClick: () -> Unit,
) {
    GlassCard(
        modifier = modifier.clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick),
        innerPadding = 0.dp,
        cornerRadius = 24.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, label, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(label, color = TextPrimary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun BentoGaugeCard(
    label: String, progress: Float, valueText: String, 
    color: Color, modifier: Modifier, subText: String? = null
) {
    GlassCard(modifier = modifier, innerPadding = 12.dp, cornerRadius = 24.dp) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(4.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = GlassBorder.copy(alpha = 0.1f),
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = color,
                    strokeWidth = 6.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(valueText, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                    if (subText != null) {
                        Text(subText, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 9.sp)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(label, color = TextMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
    }
}

@Composable
private fun PowerButton(icon: ImageVector, label: String, modifier: Modifier, tint: Color = TextSecondary, onClick: () -> Unit) {
    GlassCard(
        modifier = modifier.clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick), 
        innerPadding = 16.dp, 
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(label, color = tint, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PowerOption(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
    }
}
