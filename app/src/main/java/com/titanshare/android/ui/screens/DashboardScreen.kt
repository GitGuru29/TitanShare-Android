package com.titanshare.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.titanshare.android.data.model.SystemInfo
import com.titanshare.android.ui.navigation.Screen
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

    LaunchedEffect(toast) {
        if (toast != null) delay(1500)
        vm.clearToast()
    }

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
            // ── Top Header Bar ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TitanShare",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(
                    onClick = { showDisconnectDialog = true },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x1AFFFFFF))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // ── Scrollable Content Area ─────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Connected Laptop Header Card
                ConnectedDeviceHeaderCard(
                    deviceName = device?.name ?: "msfvenom",
                    ipAddress = device?.host ?: "10.72.76.6",
                    model = sysInfo?.let { "${it.brand} ${it.model}".trim() }?.takeIf { it.isNotBlank() } ?: "LENOVO 82KB",
                    kernel = sysInfo?.osVersion?.takeIf { it.isNotBlank() } ?: "Linux 6.18.47-1-lts",
                    onCardClick = { onNavigate(Screen.SystemDetails.route) }
                )

                // 2. Bento Quick Action Grid (4 Buttons)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BentoQuickAction(
                        icon = Icons.Outlined.Mouse,
                        label = "Trackpad",
                        iconColor = Color(0xFF00A2FF),
                        bgGlow = Color(0x220052FF),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Trackpad.route) }
                    )
                    BentoQuickAction(
                        icon = Icons.Outlined.Keyboard,
                        label = "Keyboard",
                        iconColor = Color(0xFFA855F7),
                        bgGlow = Color(0x22A855F7),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.Keyboard.route) }
                    )
                    BentoQuickAction(
                        icon = Icons.Outlined.ArrowUpward,
                        label = "Send",
                        iconColor = Color(0xFF00E676),
                        bgGlow = Color(0x2200E676),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.FileTransfer.route) }
                    )
                    BentoQuickAction(
                        icon = Icons.Outlined.ArrowDownward,
                        label = "Receive",
                        iconColor = Color(0xFF00A2FF),
                        bgGlow = Color(0x2200A2FF),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(Screen.LinuxFiles.route) }
                    )
                }

                // 3. Screen Mirror Card
                ScreenMirrorBannerCard(
                    onClick = { onNavigate(Screen.Mirror.route) }
                )

                // 4. System Overview Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onNavigate(Screen.SystemDetails.route) }
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "System Overview",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF8E9EAF),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // 5. 2x2 Metric Cards Grid
                SystemMetricGrid(sysInfo = sysInfo)

                Spacer(modifier = Modifier.height(72.dp)) // Clearance for bottom nav bar
            }
        }

        // ── Docked Bottom Navigation Bar ──────────────────────────────
        DashboardBottomNavBar(
            activeTab = "Home",
            onTabSelected = { tab ->
                when (tab) {
                    "Files"  -> onNavigate(Screen.LinuxFiles.route)
                    "Mirror" -> onNavigate(Screen.Mirror.route)
                    "System" -> onNavigate(Screen.SystemDetails.route)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // ── Disconnect Dialog ──────────────────────────────────────────
        if (showDisconnectDialog) {
            AlertDialog(
                onDismissRequest = { showDisconnectDialog = false },
                title = { Text("Disconnect Device?") },
                text  = { Text("Are you sure you want to disconnect from ${device?.name ?: "Linux PC"}?") },
                confirmButton = {
                    TextButton(onClick = { vm.disconnect(); onDisconnect() }) {
                        Text("Disconnect", color = Color(0xFFFF4D4D))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDisconnectDialog = false }) { Text("Cancel") }
                },
                containerColor = Color(0xFF0D1C33)
            )
        }
    }
}

@Composable
private fun ConnectedDeviceHeaderCard(
    deviceName: String,
    ipAddress: String,
    model: String,
    kernel: String,
    onCardClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x330B172E))
            .border(1.dp, Color(0x331E385B), RoundedCornerShape(20.dp))
            .clickable { onCardClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            LaptopConnectedGraphic()

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = deviceName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Wifi",
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(18.dp)
                    )
                }

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
                        text = "Connected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00E676)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = model,
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = ipAddress,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = kernel,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
private fun LaptopConnectedGraphic(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(70.dp)
            .height(54.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(40.dp)
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
                    .width(70.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFF1E385B))
            )
        }
    }
}

@Composable
private fun BentoQuickAction(
    icon: ImageVector,
    label: String,
    iconColor: Color,
    bgGlow: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .aspectRatio(0.95f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x330B172E))
            .border(1.dp, Color(0x331E385B), RoundedCornerShape(18.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgGlow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ScreenMirrorBannerCard(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x330B172E))
            .border(1.dp, Color(0x331E385B), RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.4f),
                                Color(0xFFA855F7).copy(alpha = 0.4f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Monitor,
                    contentDescription = "Screen Mirror",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Screen Mirror",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Stream your screen to the PC\nin real time",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = Color(0xFF8E9EAF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SystemMetricGrid(sysInfo: SystemInfo?) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Row 1: CPU & RAM
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                icon = Icons.Outlined.Memory,
                iconColor = Color(0xFF00A2FF),
                title = "CPU",
                value = "${sysInfo?.cpuLoad ?: "6.3"}%",
                subtext = "${sysInfo?.cpuFreqGhz ?: "0.40"} GHz  ${sysInfo?.cpuTemp ?: "43"}°C",
                progress = (sysInfo?.cpuLoad?.toFloatOrNull() ?: 6.3f) / 100f,
                progressColor = Color(0xFF00E676),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                icon = Icons.Outlined.Memory,
                iconColor = Color(0xFF00A2FF),
                title = "RAM",
                value = "${sysInfo?.ramUsage ?: "43.0"}%",
                subtext = "${sysInfo?.ramUsed ?: "6"} / ${sysInfo?.ramTotal ?: "15"} GB",
                progress = (sysInfo?.ramUsage?.toFloatOrNull() ?: 43.0f) / 100f,
                progressColor = Color(0xFF00A2FF),
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Storage & Battery
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MetricCard(
                icon = Icons.Outlined.DirectionsBus,
                iconColor = Color(0xFF00E676),
                title = "Storage",
                value = "${sysInfo?.storageUsage ?: "42.7"}%",
                subtext = "${sysInfo?.storageUsed ?: "199"} / ${sysInfo?.storageTotal ?: "467"} GB",
                progress = (sysInfo?.storageUsage?.toFloatOrNull() ?: 42.7f) / 100f,
                progressColor = Color(0xFF00E676),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                icon = Icons.Outlined.BatteryStd,
                iconColor = Color(0xFF00E676),
                title = "Battery",
                value = "${sysInfo?.battery ?: 99}%",
                subtext = "Charging",
                progress = (sysInfo?.battery ?: 99) / 100f,
                progressColor = Color(0xFF00E676),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    subtext: String,
    progress: Float,
    progressColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x330B172E))
            .border(1.dp, Color(0x331E385B), RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtext,
                fontSize = 11.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color(0x22FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(progressColor)
                )
            }
        }
    }
}

@Composable
private fun DashboardBottomNavBar(
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
            NavTabItem(
                icon = Icons.Filled.Home,
                label = "Home",
                isSelected = activeTab == "Home",
                onClick = { onTabSelected("Home") }
            )
            NavTabItem(
                icon = Icons.Outlined.Folder,
                label = "Files",
                isSelected = activeTab == "Files",
                onClick = { onTabSelected("Files") }
            )
            NavTabItem(
                icon = Icons.Outlined.Monitor,
                label = "Mirror",
                isSelected = activeTab == "Mirror",
                onClick = { onTabSelected("Mirror") }
            )
            NavTabItem(
                icon = Icons.Outlined.Settings,
                label = "System",
                isSelected = activeTab == "System",
                onClick = { onTabSelected("System") }
            )
        }
    }
}

@Composable
private fun NavTabItem(
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
