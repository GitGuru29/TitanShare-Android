package com.titanshare.android.ui.screens

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.*
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
import com.titanshare.android.ui.navigation.Screen
import com.titanshare.android.viewmodel.AppViewModel

@Composable
fun SystemDetailsScreen(
    vm: AppViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val device  by vm.selectedDevice.collectAsStateWithLifecycle()
    val sysInfo by vm.systemInfo.collectAsStateWithLifecycle()
    var activeSubTab by remember { mutableStateOf("Performance") }

    val currentSysInfo = sysInfo ?: SystemInfo()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040814))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // ── Top Bar ───────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "System Details",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // ── Scrollable Body ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Hardware Specs Header Card
                SystemHeaderCard(
                    deviceName = device?.name ?: currentSysInfo.let { "${it.brand} ${it.model}" },
                    ipAddress = device?.host ?: "10.72.76.6",
                    sysInfo = currentSysInfo
                )

                // 2. Sub-Tab Navigation Bar (Performance / Hardware / Network)
                SubTabBar(
                    activeTab = activeSubTab,
                    onTabSelected = { activeSubTab = it }
                )

                // 3. Tab Content View
                when (activeSubTab) {
                    "Performance" -> PerformanceTabView(info = currentSysInfo)
                    "Hardware"    -> HardwareTabView(info = currentSysInfo)
                    "Network"     -> NetworkTabView(info = currentSysInfo, ip = device?.host ?: "10.72.76.6")
                }

                Spacer(modifier = Modifier.height(72.dp)) // Clearance for bottom nav bar
            }
        }

        // ── Docked Bottom Navigation Bar ──────────────────────────────
        SystemBottomNavBar(
            activeTab = "System",
            onTabSelected = { tab ->
                when (tab) {
                    "Home"   -> onBack()
                    "Files"  -> onNavigate(Screen.LinuxFiles.route)
                    "Mirror" -> onNavigate(Screen.Mirror.route)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
private fun SystemHeaderCard(
    deviceName: String,
    ipAddress: String,
    sysInfo: SystemInfo
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x330B172E))
            .border(1.dp, Color(0x331E385B), RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            LaptopGraphicMini()

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = deviceName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(2.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Online",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00E676)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Detail Spec Rows
                SpecDetailRow(icon = Icons.Outlined.Terminal, text = sysInfo.osVersion)
                Spacer(modifier = Modifier.height(3.dp))
                SpecDetailRow(icon = Icons.Outlined.Memory, text = sysInfo.cpuModel)
                Spacer(modifier = Modifier.height(3.dp))
                SpecDetailRow(icon = Icons.Outlined.Tv, text = sysInfo.gpuModel)
                Spacer(modifier = Modifier.height(3.dp))
                SpecDetailRow(icon = Icons.Outlined.Public, text = ipAddress)
            }
        }
    }
}

@Composable
private fun SpecDetailRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF8E9EAF),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun SubTabBar(
    activeTab: String,
    onTabSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        listOf("Performance", "Hardware", "Network").forEach { tab ->
            val isSelected = activeTab == tab
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    text = tab,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF00A2FF) else Color(0xFF8E9EAF)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(2.5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFF00A2FF) else Color.Transparent)
                )
            }
        }
    }
}

@Composable
private fun PerformanceTabView(info: SystemInfo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // CPU Usage Card with Graph
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x330B172E))
                .border(1.dp, Color(0x331E385B), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "CPU Usage",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${info.cpuLoad}%",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00A2FF)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${info.cpuFreqGhz} GHz",
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${info.cpuTemp}°C",
                            fontSize = 14.sp,
                            color = Color(0xFFFF9900),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Task Manager Graph
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF040A14))
                        .border(1.dp, Color(0x221E385B), RoundedCornerShape(12.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                        val w = size.width
                        val h = size.height

                        // Grid lines
                        val gridColor = Color.White.copy(alpha = 0.05f)
                        drawLine(gridColor, Offset(0f, h * 0.25f), Offset(w, h * 0.25f), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), strokeWidth = 1f)
                        drawLine(gridColor, Offset(0f, h * 0.75f), Offset(w, h * 0.75f), strokeWidth = 1f)

                        // Sample line waveform
                        val points = listOf(
                            0.2f, 0.22f, 0.21f, 0.25f, 0.23f, 0.20f, 0.22f, 0.40f,
                            0.32f, 0.48f, 0.35f, 0.42f, 0.25f, 0.28f, 0.30f, 0.22f, 0.24f
                        )
                        val step = w / (points.size - 1)

                        val linePath = Path()
                        points.forEachIndexed { i, p ->
                            val x = i * step
                            val y = h * (1f - p)
                            if (i == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                        }

                        drawPath(
                            path = linePath,
                            color = Color(0xFF00A2FF),
                            style = Stroke(width = 2f, cap = StrokeCap.Round)
                        )
                    }

                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("100%", color = Color(0x668E9EAF), fontSize = 9.sp, modifier = Modifier.align(Alignment.TopStart))
                        Text("0%", color = Color(0x668E9EAF), fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomStart))
                        Text("60s", color = Color(0x668E9EAF), fontSize = 9.sp, modifier = Modifier.align(Alignment.BottomEnd))
                    }
                }
            }
        }

        // CPU Cores Grid Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0x330B172E))
                .border(1.dp, Color(0x331E385B), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "CPU Cores",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                val cores = info.cpuCoresUsage.ifEmpty { listOf(4f, 11f, 5f, 3f, 8f, 4f, 5f, 7f) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Column 1: C0 .. C3
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 0..3) {
                            val usage = if (i < cores.size) cores[i] else 4f
                            CoreUsageRow(coreLabel = "C$i", usage = usage)
                        }
                    }

                    // Column 2: C4 .. C7
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (i in 4..7) {
                            val usage = if (i < cores.size) cores[i] else 5f
                            CoreUsageRow(coreLabel = "C$i", usage = usage)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CoreUsageRow(coreLabel: String, usage: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = coreLabel,
            fontSize = 12.sp,
            color = Color(0xFF94A3B8),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(22.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(Color(0x22FFFFFF))
                .padding(horizontal = 1.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((usage / 100f).coerceIn(0.08f, 1f))
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(Color(0xFF00A2FF))
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "${usage.toInt()}%",
            fontSize = 11.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(28.dp)
        )
    }
}

@Composable
private fun HardwareTabView(info: SystemInfo) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HardwareItemCard(
            icon = Icons.Outlined.Memory,
            title = "RAM Memory",
            subtitle = "${info.ramUsed} / ${info.ramTotal} GB Used (${info.ramUsage}%)",
            details = "DDR4 3200MHz Dual Channel"
        )
        HardwareItemCard(
            icon = Icons.Outlined.Storage,
            title = "NVMe SSD Storage",
            subtitle = "${info.storageUsed} / ${info.storageTotal} GB Used (${info.storageUsage}%)",
            details = "SAMSUNG MZALQ512HALU 512GB"
        )
        HardwareItemCard(
            icon = Icons.Outlined.BatteryStd,
            title = "Battery Health",
            subtitle = "${info.battery}% - Charging",
            details = "Design Capacity 45Wh (98% Health)"
        )
        HardwareItemCard(
            icon = Icons.Outlined.Tv,
            title = "GPU Acceleration",
            subtitle = info.gpuModel,
            details = "2GB GDDR5 VRAM"
        )
    }
}

@Composable
private fun NetworkTabView(info: SystemInfo, ip: String) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        HardwareItemCard(
            icon = Icons.Outlined.Public,
            title = "Wi-Fi Network Interface",
            subtitle = "IP: $ip",
            details = "Interface: wlan0 (802.11ax Wi-Fi 6)"
        )
        HardwareItemCard(
            icon = Icons.Outlined.Speed,
            title = "Link Speed",
            subtitle = info.netSpeed,
            details = "Latency: 4ms local ping"
        )
    }
}

@Composable
private fun HardwareItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    details: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x330B172E))
            .border(1.dp, Color(0x331E385B), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x2200A2FF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF00A2FF),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF00E676)
                )
                Text(
                    text = details,
                    fontSize = 11.sp,
                    color = Color(0xFF8E9EAF)
                )
            }
        }
    }
}

@Composable
private fun LaptopGraphicMini(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(72.dp)
            .height(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(42.dp)
                    .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                    .background(Color(0xFF0B1424))
                    .border(1.dp, Color(0xFF1E385B), RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF020712),
                                    Color(0xFF0052FF),
                                    Color(0xFF00D4FF)
                                ),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 80f)
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFF1E385B))
            )
        }
    }
}

@Composable
private fun SystemBottomNavBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xF0081224))
            .border(width = 0.5.dp, color = Color(0x331E385B), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItemSystem(
                icon = Icons.Filled.Home,
                label = "Home",
                isSelected = activeTab == "Home",
                onClick = { onTabSelected("Home") }
            )
            NavTabItemSystem(
                icon = Icons.Outlined.Folder,
                label = "Files",
                isSelected = activeTab == "Files",
                onClick = { onTabSelected("Files") }
            )
            NavTabItemSystem(
                icon = Icons.Outlined.Monitor,
                label = "Mirror",
                isSelected = activeTab == "Mirror",
                onClick = { onTabSelected("Mirror") }
            )
            NavTabItemSystem(
                icon = Icons.Outlined.Settings,
                label = "System",
                isSelected = activeTab == "System",
                onClick = { onTabSelected("System") }
            )
        }
    }
}

@Composable
private fun NavTabItemSystem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF00A2FF) else Color(0xFF64748B),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF00A2FF) else Color(0xFF64748B)
        )
    }
}
