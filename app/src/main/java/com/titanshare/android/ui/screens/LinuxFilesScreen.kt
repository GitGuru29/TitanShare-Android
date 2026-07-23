package com.titanshare.android.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.titanshare.android.ui.components.AirDropOverlay
import com.titanshare.android.ui.components.GlassCard
import com.titanshare.android.ui.theme.*
import com.titanshare.android.viewmodel.AppViewModel

@Composable
fun LinuxFilesScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context         = LocalContext.current
    val files           by vm.linuxFiles.collectAsStateWithLifecycle()
    val receiveProgress by vm.receiveProgress.collectAsStateWithLifecycle()
    val receiveStatus   by vm.receiveStatus.collectAsStateWithLifecycle()
    val receiveSpeed    by vm.receiveSpeed.collectAsStateWithLifecycle()

    var downloadingFile by remember { mutableStateOf<String?>(null) }

    // Auto-refresh file list when screen opens
    LaunchedEffect(Unit) { vm.refreshLinuxFiles() }

    val animProgress by animateFloatAsState(
        targetValue = receiveProgress,
        animationSpec = tween(200),
        label = "rcv_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Color(0xFF030810))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        ) {
            // ── Header ─────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
                    }
                    Column {
                        Text(
                            "Linux Files",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "Drop files in /var/lib/titanshare/send_to_android/",
                            color = TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                        )
                    }
                }
                IconButton(onClick = { vm.refreshLinuxFiles() }) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = ElectricBlue)
                }
            }

            // ── Download Progress Card ──────────────────────────────────────
            if (receiveStatus != null || receiveProgress > 0f) {
                GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 18.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                receiveStatus ?: "Downloading…",
                                color = when {
                                    receiveStatus?.startsWith("✅") == true -> SuccessGreen
                                    receiveStatus?.startsWith("❌") == true -> DangerRed
                                    else -> TextPrimary
                                },
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 2, overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "${(animProgress * 100).toInt()}%",
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }

                        // Speed row
                        if (receiveSpeed > 0.0 && receiveProgress < 1f) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Speed, null,
                                    tint = ElectricBlue.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                val speedText = if (receiveSpeed >= 1.0)
                                    String.format("%.1f Mbps", receiveSpeed)
                                else
                                    String.format("%.0f Kbps", receiveSpeed * 1000)
                                Text(speedText, color = ElectricBlue,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }

                        LinearProgressIndicator(
                            progress = { animProgress },
                            modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                            color = ElectricBlue,
                            trackColor = GlassWhite,
                        )

                        if (receiveStatus?.startsWith("✅") == true || receiveStatus?.startsWith("❌") == true) {
                            TextButton(
                                onClick = { vm.clearReceiveStatus(); downloadingFile = null },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Dismiss", color = ElectricBlue,
                                    style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // ── File List ──────────────────────────────────────────────────
            if (files.isEmpty()) {
                Spacer(Modifier.height(48.dp))
                GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 32.dp) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.FolderOpen, null,
                                tint = ElectricBlue, modifier = Modifier.size(30.dp))
                        }
                        Text("No files available", color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                        Text(
                            "On your Linux PC, copy files into:\n/var/lib/titanshare/send_to_android/\n\nThen tap Refresh ↻",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                Text(
                    "${files.size} file${if (files.size != 1) "s" else ""} available",
                    color = TextMuted,
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(files, key = { it.name }) { file ->
                        FileRow(
                            file = file,
                            isDownloading = downloadingFile == file.name && receiveProgress < 1f,
                            onDownload = {
                                if (downloadingFile == null || receiveStatus?.startsWith("✅") == true || receiveStatus?.startsWith("❌") == true) {
                                    downloadingFile = file.name
                                    vm.clearReceiveStatus()
                                    vm.receiveFile(context, file.name, file.size)
                                }
                            }
                        )
                    }
                }
            }
        }

        AirDropOverlay(
            isVisible = downloadingFile != null && (receiveProgress > 0f || receiveStatus?.startsWith("✅") == true),
            progress = receiveProgress,
            isSending = false,
            filename = downloadingFile ?: "",
            onDismiss = { downloadingFile = null; vm.clearReceiveStatus() }
        )
    }
}

@Composable
private fun FileRow(
    file: DaemonClient.LinuxFile,
    isDownloading: Boolean,
    onDownload: () -> Unit,
) {
    GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 16.dp, cornerRadius = 18.dp) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // File type icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ElectricBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    fileIcon(file.name), null,
                    tint = ElectricBlue, modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    file.name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatSize(file.size),
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Download button
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = ElectricBlue,
                    strokeWidth = 2.dp
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(ElectricBlue.copy(alpha = 0.15f))
                        .clickable(onClick = onDownload),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Download, "Download",
                        tint = ElectricBlue, modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun fileIcon(name: String) = when {
    name.endsWith(".pdf", true)                          -> Icons.Default.PictureAsPdf
    name.endsWith(".jpg", true) || name.endsWith(".jpeg", true) ||
    name.endsWith(".png", true) || name.endsWith(".gif", true)   -> Icons.Default.Image
    name.endsWith(".mp4", true) || name.endsWith(".mkv", true) ||
    name.endsWith(".avi", true)                                   -> Icons.Default.VideoFile
    name.endsWith(".mp3", true) || name.endsWith(".flac", true) ||
    name.endsWith(".ogg", true)                                   -> Icons.Default.AudioFile
    name.endsWith(".zip", true) || name.endsWith(".tar", true) ||
    name.endsWith(".gz", true)                                    -> Icons.Default.FolderZip
    name.endsWith(".apk", true)                                   -> Icons.Default.Android
    else                                                          -> Icons.Default.InsertDriveFile
}

private fun formatSize(bytes: Long): String = when {
    bytes >= 1_073_741_824L -> String.format("%.1f GB", bytes / 1_073_741_824.0)
    bytes >= 1_048_576L     -> String.format("%.1f MB", bytes / 1_048_576.0)
    bytes >= 1_024L         -> String.format("%.1f KB", bytes / 1_024.0)
    else                    -> "$bytes B"
}
