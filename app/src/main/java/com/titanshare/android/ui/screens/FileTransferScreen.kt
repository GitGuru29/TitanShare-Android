package com.titanshare.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.titanshare.android.data.network.DaemonClient
import com.titanshare.android.ui.components.AirDropOverlay
import com.titanshare.android.ui.navigation.Screen
import com.titanshare.android.ui.theme.*
import com.titanshare.android.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TransferFileType {
    IMAGE, DOCUMENT, ARCHIVE, VIDEO, AUDIO, OTHER
}

data class TransferHistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val sizeFormatted: String,
    val status: String = "Completed",
    val timestamp: String,
    val isSuccess: Boolean = true,
    val fileType: TransferFileType = TransferFileType.OTHER,
    val isIncoming: Boolean = false,
    val isSampleAvatar: Boolean = false
)

@Composable
fun FileTransferScreen(
    vm: AppViewModel,
    onNavigate: (String) -> Unit = {},
    onBack: () -> Unit
) {
    val context         = LocalContext.current
    val device          by vm.selectedDevice.collectAsStateWithLifecycle()
    val progress        by vm.fileProgress.collectAsStateWithLifecycle()
    val status          by vm.fileStatus.collectAsStateWithLifecycle()
    val speed           by vm.transferSpeed.collectAsStateWithLifecycle()

    val linuxFiles      by vm.linuxFiles.collectAsStateWithLifecycle()
    val receiveProgress by vm.receiveProgress.collectAsStateWithLifecycle()
    val receiveStatus   by vm.receiveStatus.collectAsStateWithLifecycle()
    val receiveSpeed    by vm.receiveSpeed.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Send to PC, 1: Receive from PC, 2: History

    var selectedUri  by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var fileSize     by remember { mutableLongStateOf(0L) }

    // Sample default transfers matching mockup + dynamic ones
    var recentTransfers by remember {
        mutableStateOf(
            listOf(
                TransferHistoryItem(
                    name = "IMG_20260904_1205.jpg",
                    sizeFormatted = "2.4 MB",
                    status = "Completed",
                    timestamp = "12:05",
                    fileType = TransferFileType.IMAGE,
                    isSampleAvatar = true
                ),
                TransferHistoryItem(
                    name = "ArchTitan-ISO.iso",
                    sizeFormatted = "2.7 GB",
                    status = "Completed",
                    timestamp = "11:42",
                    fileType = TransferFileType.DOCUMENT
                ),
                TransferHistoryItem(
                    name = "project.zip",
                    sizeFormatted = "156 MB",
                    status = "Completed",
                    timestamp = "10:18",
                    fileType = TransferFileType.ARCHIVE
                )
            )
        )
    }

    val animatedSendProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "send_progress"
    )

    val animatedReceiveProgress by animateFloatAsState(
        targetValue = receiveProgress,
        animationSpec = tween(300),
        label = "rcv_progress"
    )

    // Helper for formatting file size
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f GB", bytes / (1024.0 * 1024 * 1024))
            bytes >= 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024))
            bytes >= 1024 -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }

    // Helper for determining file type
    fun getFileType(filename: String): TransferFileType {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp") || lower.endsWith(".gif") -> TransferFileType.IMAGE
            lower.endsWith(".zip") || lower.endsWith(".tar") || lower.endsWith(".gz") || lower.endsWith(".7z") || lower.endsWith(".rar") -> TransferFileType.ARCHIVE
            lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".mov") || lower.endsWith(".avi") -> TransferFileType.VIDEO
            lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".flac") || lower.endsWith(".aac") -> TransferFileType.AUDIO
            lower.endsWith(".pdf") || lower.endsWith(".iso") || lower.endsWith(".txt") || lower.endsWith(".doc") || lower.endsWith(".docx") -> TransferFileType.DOCUMENT
            else -> TransferFileType.OTHER
        }
    }

    // Listen for completion to append to recent transfers
    LaunchedEffect(status) {
        if (status?.startsWith("✅") == true && selectedName.isNotEmpty()) {
            val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val newItem = TransferHistoryItem(
                name = selectedName,
                sizeFormatted = formatSize(fileSize),
                status = "Completed",
                timestamp = time,
                isSuccess = true,
                fileType = getFileType(selectedName),
                isIncoming = false,
                isSampleAvatar = false
            )
            recentTransfers = listOf(newItem) + recentTransfers
        }
    }

    // File picker launcher
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        selectedUri = uri

        var name = "file"
        var size = 0L
        context.contentResolver.query(
            uri,
            arrayOf(
                android.provider.OpenableColumns.DISPLAY_NAME,
                android.provider.OpenableColumns.SIZE
            ),
            null, null, null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (nameIdx != -1) name = cursor.getString(nameIdx) ?: "file"
                if (sizeIdx != -1 && !cursor.isNull(sizeIdx)) size = cursor.getLong(sizeIdx)
            }
        }
        if (size <= 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { size = it.available().toLong() }
            } catch (_: Exception) {}
        }
        selectedName = name
        fileSize = size
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
        ) {
            // ── Top Header Bar ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 20.dp, top = 8.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "File Transfer",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // ── Main Scrollable/Padded Content Area ────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Connected Device Status Card
                ConnectedDeviceTopCard(
                    deviceName = device?.name ?: "msfvenom",
                    ipAddress = device?.host ?: "10.72.76.6"
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 2. Custom Tabs: "Send to PC", "Receive from PC", "History"
                TransferTabBar(
                    tabs = listOf("Send to PC", "Receive from PC", "History"),
                    selectedTabIndex = selectedTab,
                    onTabSelected = { selectedTab = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Tab Contents
                when (selectedTab) {
                    0 -> {
                        // ── Send to PC Tab ─────────────────────────────
                        SendToPcContent(
                            selectedUri = selectedUri,
                            selectedName = selectedName,
                            fileSize = fileSize,
                            progress = animatedSendProgress,
                            status = status,
                            speed = speed,
                            recentTransfers = recentTransfers,
                            onPickFile = { filePicker.launch("*/*") },
                            onSendFile = {
                                val uri = selectedUri
                                if (uri != null && selectedName.isNotEmpty()) {
                                    val inputStream = context.contentResolver.openInputStream(uri)
                                    if (inputStream != null) {
                                        vm.sendFile(selectedName, fileSize, inputStream)
                                    }
                                }
                            },
                            onClearSelected = {
                                vm.clearFileStatus()
                                selectedUri = null
                                selectedName = ""
                                fileSize = 0L
                            },
                            onClearHistory = {
                                recentTransfers = emptyList()
                            }
                        )
                    }
                    1 -> {
                        // ── Receive from PC Tab ─────────────────────────
                        ReceiveFromPcContent(
                            linuxFiles = linuxFiles,
                            receiveProgress = animatedReceiveProgress,
                            receiveStatus = receiveStatus,
                            receiveSpeed = receiveSpeed,
                            onRefresh = { vm.refreshLinuxFiles() },
                            onDownloadFile = { file ->
                                vm.receiveFile(context, file.name, file.size)
                            }
                        )
                    }
                    2 -> {
                        // ── History Tab ────────────────────────────────
                        HistoryContent(
                            transfers = recentTransfers,
                            onClearHistory = { recentTransfers = emptyList() }
                        )
                    }
                }
            }

            // ── Bottom Navigation Bar with Home Bar Indicator ──────────
            FileTransferBottomNavBar(
                activeTab = "Files",
                onTabSelected = { tab ->
                    when (tab) {
                        "Home" -> onNavigate(Screen.Dashboard.route)
                        "Files" -> { /* Already here */ }
                        "Mirror" -> onNavigate(Screen.Mirror.route)
                        "System" -> onNavigate(Screen.SystemDetails.route)
                    }
                }
            )
        }

        AirDropOverlay(
            isVisible = progress > 0f || status?.startsWith("✅") == true,
            progress = progress,
            isSending = true,
            filename = selectedName,
            onDismiss = {
                vm.clearFileStatus()
                selectedUri = null
                selectedName = ""
                fileSize = 0L
            }
        )
    }
}

@Composable
private fun ConnectedDeviceTopCard(
    deviceName: String,
    ipAddress: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0A1224))
            .border(1.dp, Color(0x331E385B), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Laptop Icon Graphic Container
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x1F1E385B)),
                contentAlignment = Alignment.Center
            ) {
                MiniLaptopGraphic()
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = deviceName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Connected - $ipAddress",
                        fontSize = 12.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniLaptopGraphic() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Screen Lid
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(29.dp)
                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                .background(Color(0xFF0B1322))
                .border(0.8.dp, Color(0xFF1E385B), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                .padding(1.8.dp),
            contentAlignment = Alignment.Center
        ) {
            // Wallpaper display with glowing beam
            Canvas(modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp))) {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF020713),
                            Color(0xFF041944),
                            Color(0xFF0052FF)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                )
                // Glowing curved arc wave
                val wavePath = Path().apply {
                    moveTo(0f, size.height * 0.9f)
                    cubicTo(
                        size.width * 0.4f, size.height * 0.8f,
                        size.width * 0.6f, size.height * 0.2f,
                        size.width, size.height * 0.1f
                    )
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.linearGradient(
                        listOf(Color(0x3300D4FF), Color(0x990088FF), Color(0xFF00D4FF))
                    )
                )
            }
        }
        // Base / Keyboard Deck
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(3.5.dp)
                .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF334A6E), Color(0xFF1E2E48))
                    )
                )
        )
    }
}

@Composable
private fun TransferTabBar(
    tabs: List<String>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTabIndex == index
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 4.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) Color(0xFF00A2FF) else Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(2.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00A2FF))
                    )
                } else {
                    Spacer(modifier = Modifier.height(2.5.dp))
                }
            }
        }
    }
}

@Composable
private fun SendToPcContent(
    selectedUri: Uri?,
    selectedName: String,
    fileSize: Long,
    progress: Float,
    status: String?,
    speed: Double,
    recentTransfers: List<TransferHistoryItem>,
    onPickFile: () -> Unit,
    onSendFile: () -> Unit,
    onClearSelected: () -> Unit,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // ── Dashed Dropzone Card ───────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(195.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable { onPickFile() }
        ) {
            // Custom Dashed Outline
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = Stroke(
                    width = 1.6.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f), 0f)
                )
                drawRoundRect(
                    color = Color(0x5500A2FF),
                    size = size,
                    cornerRadius = CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                    style = stroke
                )
            }

            // Interior Glass Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x0F0052FF))
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Folder,
                        contentDescription = "Folder",
                        tint = Color.White,
                        modifier = Modifier.size(46.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (selectedUri != null) selectedName else "Select files to send",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (selectedUri != null) "${fileSize / 1024} KB • Tap to change" else "Or tap to browse",
                        fontSize = 13.5.sp,
                        color = Color(0xFF94A3B8)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "You can also share files from other apps\nusing the share menu.",
                        fontSize = 11.5.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // ── In-Flight Sending Progress & Send Action Button ─────────────
        if (selectedUri != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0A1224))
                    .border(1.dp, Color(0x331E385B), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = status ?: if (progress > 0f) "Uploading..." else "Ready to Send",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            status?.startsWith("✅") == true -> Color(0xFF00E676)
                            status?.startsWith("❌") == true -> Color(0xFFFF5252)
                            else -> Color(0xFF00A2FF)
                        }
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (speed > 0.0 && progress < 1f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (speed >= 1.0) String.format("%.1f Mbps", speed) else String.format("%.0f Kbps", speed * 1000),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color(0xFF00A2FF),
                    trackColor = Color(0x33FFFFFF)
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (progress == 0f && status == null) {
                        Button(
                            onClick = onSendFile,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A2FF)),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Send to PC", fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = onClearSelected,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text(if (status != null) "Done" else "Cancel", color = Color(0xFF94A3B8))
                    }
                }
            }
        }

        // ── Recent Transfers Card ──────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A1224))
                .border(1.dp, Color(0x331E385B), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Transfers",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (recentTransfers.isNotEmpty()) {
                    Text(
                        text = "Clear all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00A2FF),
                        modifier = Modifier
                            .clickable { onClearHistory() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (recentTransfers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent transfers",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    recentTransfers.forEach { item ->
                        TransferItemRow(item = item)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun TransferItemRow(item: TransferHistoryItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail / File Icon
        FileIconBadge(
            fileType = item.fileType,
            filename = item.name,
            isSampleAvatar = item.isSampleAvatar
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "${item.sizeFormatted} • ${item.status}",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = Color(0xFF00E676),
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.timestamp,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
    }
}

@Composable
private fun FileIconBadge(
    fileType: TransferFileType,
    filename: String,
    isSampleAvatar: Boolean = false
) {
    when {
        isSampleAvatar || (fileType == TransferFileType.IMAGE && filename.contains("IMG_20260904")) -> {
            // High fidelity illustrated portrait photo matching the mockup
            PortraitAvatarBadge()
        }
        fileType == TransferFileType.IMAGE -> {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF23354E), Color(0xFF152233))
                        )
                    )
                    .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = "Image",
                    tint = Color(0xFF60A5FA),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        fileType == TransferFileType.DOCUMENT -> {
            // Clean document sheet with folded corner icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF162032))
                    .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = "Document",
                    tint = Color(0xFFF1F5F9),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        fileType == TransferFileType.ARCHIVE -> {
            // Amber/orange archive badge matching the mockup screenshot
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF162032))
                    .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFF59E0B)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
        fileType == TransferFileType.VIDEO -> {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF162032))
                    .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Videocam,
                    contentDescription = "Video",
                    tint = Color(0xFFA855F7),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        fileType == TransferFileType.AUDIO -> {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF162032))
                    .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Audiotrack,
                    contentDescription = "Audio",
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF162032))
                    .border(0.8.dp, Color(0x22FFFFFF), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.InsertDriveFile,
                    contentDescription = "File",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Realistic portrait avatar badge designed to match the mockup's person photo thumbnail.
 */
@Composable
private fun PortraitAvatarBadge() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF4A3E38), Color(0xFF1F2430))
                )
            )
            .border(0.8.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Warm studio background glow
            drawCircle(
                color = Color(0x44D97706),
                radius = w * 0.45f,
                center = Offset(w * 0.5f, h * 0.45f)
            )

            // Shoulders / Dark Jacket
            drawArc(
                color = Color(0xFF1E293B),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.1f, h * 0.55f),
                size = Size(w * 0.8f, h * 0.8f)
            )

            // White shirt collar V
            val collarPath = Path().apply {
                moveTo(w * 0.42f, h * 0.62f)
                lineTo(w * 0.5f, h * 0.76f)
                lineTo(w * 0.58f, h * 0.62f)
                close()
            }
            drawPath(collarPath, Color(0xFFE2E8F0))

            // Neck
            drawRect(
                color = Color(0xFFD4A373),
                topLeft = Offset(w * 0.42f, h * 0.52f),
                size = Size(w * 0.16f, h * 0.14f)
            )

            // Face
            drawCircle(
                color = Color(0xFFE7C198),
                radius = w * 0.22f,
                center = Offset(w * 0.5f, h * 0.38f)
            )

            // Glasses rims
            drawCircle(
                color = Color(0xFF262626),
                radius = w * 0.075f,
                center = Offset(w * 0.41f, h * 0.36f),
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF262626),
                radius = w * 0.075f,
                center = Offset(w * 0.59f, h * 0.36f),
                style = Stroke(width = 1.2.dp.toPx())
            )
            // Glasses bridge
            drawLine(
                color = Color(0xFF262626),
                start = Offset(w * 0.485f, h * 0.36f),
                end = Offset(w * 0.515f, h * 0.36f),
                strokeWidth = 1.2.dp.toPx()
            )

            // Smile
            drawArc(
                color = Color(0xFF8B4513),
                startAngle = 10f,
                sweepAngle = 160f,
                useCenter = false,
                topLeft = Offset(w * 0.44f, h * 0.42f),
                size = Size(w * 0.12f, h * 0.08f),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}

@Composable
private fun ReceiveFromPcContent(
    linuxFiles: List<DaemonClient.LinuxFile>,
    receiveProgress: Float,
    receiveStatus: String?,
    receiveSpeed: Double,
    onRefresh: () -> Unit,
    onDownloadFile: (DaemonClient.LinuxFile) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status & Refresh card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF0A1224))
                .border(1.dp, Color(0x331E385B), RoundedCornerShape(18.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PC Shared Directory",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "/var/lib/titanshare/send_to_android/",
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8)
                )
            }

            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x2200A2FF))
            ) {
                Icon(Icons.Default.Refresh, "Refresh", tint = Color(0xFF00A2FF), modifier = Modifier.size(18.dp))
            }
        }

        // Active download progress
        if (receiveStatus != null || receiveProgress > 0f) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0A1224))
                    .border(1.dp, Color(0x331E385B), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = receiveStatus ?: "Receiving...",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00A2FF)
                    )
                    Text(
                        text = "${(receiveProgress * 100).toInt()}%",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (receiveSpeed > 0.0 && receiveProgress < 1f) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (receiveSpeed >= 1.0) String.format("%.1f Mbps", receiveSpeed) else String.format("%.0f Kbps", receiveSpeed * 1000),
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LinearProgressIndicator(
                    progress = { receiveProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape),
                    color = Color(0xFF00A2FF),
                    trackColor = Color(0x33FFFFFF)
                )
            }
        }

        // Files List
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A1224))
                .border(1.dp, Color(0x331E385B), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Text(
                text = "Available Files (${linuxFiles.size})",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (linuxFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No files in shared directory on Linux PC",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    linuxFiles.forEach { file ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF162032)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Outlined.InsertDriveFile,
                                    contentDescription = null,
                                    tint = Color(0xFF00A2FF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = file.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        file.size >= 1024 * 1024 -> String.format("%.1f MB", file.size / (1024.0 * 1024))
                                        file.size >= 1024 -> String.format("%.1f KB", file.size / 1024.0)
                                        else -> "${file.size} B"
                                    },
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            IconButton(
                                onClick = { onDownloadFile(file) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00A2FF))
                            ) {
                                Icon(
                                    Icons.Default.Download,
                                    contentDescription = "Download",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HistoryContent(
    transfers: List<TransferHistoryItem>,
    onClearHistory: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0A1224))
                .border(1.dp, Color(0x331E385B), RoundedCornerShape(20.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transfer Log (${transfers.size})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                if (transfers.isNotEmpty()) {
                    Text(
                        text = "Clear all",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF00A2FF),
                        modifier = Modifier
                            .clickable { onClearHistory() }
                            .padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (transfers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transfer history found",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    transfers.forEach { item ->
                        TransferItemRow(item = item)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun FileTransferBottomNavBar(
    activeTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF081224))
            .border(width = 0.5.dp, color = Color(0x331E385B), shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                icon = Icons.Outlined.Home,
                label = "Home",
                isSelected = activeTab == "Home",
                onClick = { onTabSelected("Home") }
            )
            NavTabItem(
                icon = Icons.Filled.Folder,
                label = "Files",
                isSelected = activeTab == "Files",
                onClick = { onTabSelected("Files") }
            )
            NavTabItem(
                icon = Icons.Outlined.Tv,
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

        // Bottom Home bar indicator
        Box(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .width(120.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0x99FFFFFF))
        )
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
            .padding(horizontal = 14.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) Color(0xFF00A2FF) else Color(0xFF64748B),
            modifier = Modifier.size(23.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF00A2FF) else Color(0xFF64748B)
        )
    }
}
