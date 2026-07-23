package com.titanshare.android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.titanshare.android.ui.components.AirDropOverlay
import com.titanshare.android.ui.components.GlassCard
import com.titanshare.android.ui.theme.*
import com.titanshare.android.viewmodel.AppViewModel

@Composable
fun FileTransferScreen(vm: AppViewModel, onBack: () -> Unit) {
    val context     = LocalContext.current
    val progress    by vm.fileProgress.collectAsStateWithLifecycle()
    val status      by vm.fileStatus.collectAsStateWithLifecycle()
    val speed       by vm.transferSpeed.collectAsStateWithLifecycle()

    var selectedUri  by remember { mutableStateOf<Uri?>(null) }
    var selectedName by remember { mutableStateOf("") }
    var fileSize     by remember { mutableStateOf(0L) }

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(300),
        label = "progress"
    )

    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        selectedUri = uri

        // Extract display name and size from content resolver
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
        // Fallback: try to open and count bytes if size is still 0
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
            .background(Brush.verticalGradient(listOf(NavyDeep, Color(0xFF030810))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ── Header ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
                }
                Text(
                    "Send Files",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Drop Zone ───────────────────────────────────────────────
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                cornerRadius = 24.dp,
                innerPadding = 0.dp,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            2.dp,
                            if (selectedUri != null) ElectricBlue.copy(alpha = 0.5f) else GlassBorder,
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (selectedUri != null) Icons.Default.InsertDriveFile else Icons.Default.UploadFile,
                                null, tint = ElectricBlue, modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.height(14.dp))

                        if (selectedUri != null) {
                            Text(selectedName, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${fileSize / 1024} KB",
                                color = TextSecondary, style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text("Tap to pick a file", color = TextSecondary,
                                style = MaterialTheme.typography.bodyMedium)
                            Text("Any file type supported", color = TextMuted,
                                style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { filePicker.launch("*/*") },
                            shape = RoundedCornerShape(10.dp),
                        ) {
                            Text(if (selectedUri != null) "Change File" else "Browse",
                                color = ElectricBlue)
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Progress Card ────────────────────────────────────────────
            if (status != null || progress > 0f) {
                GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 20.dp) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                status ?: "Sending…",
                                color = when {
                                    status?.startsWith("✅") == true -> SuccessGreen
                                    status?.startsWith("❌") == true -> DangerRed
                                    else -> TextPrimary
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${(animatedProgress * 100).toInt()}%",
                                color = ElectricBlue,
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        // Transfer speed
                        if (speed > 0.0 && progress < 1f) {
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val speedText = if (speed >= 1.0) {
                                    String.format("%.1f Mbps", speed)
                                } else {
                                    String.format("%.0f Kbps", speed * 1000)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Speed,
                                        null,
                                        tint = ElectricBlue.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        speedText,
                                        color = ElectricBlue,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                // ETA
                                if (speed > 0.0 && progress > 0f && progress < 1f) {
                                    val bytesPerSec = (speed * 1_000_000.0) / 8.0
                                    val bytesRemaining = fileSize * (1.0 - progress)
                                    val etaSec = (bytesRemaining / bytesPerSec).toInt()
                                    val etaText = when {
                                        etaSec < 60  -> "${etaSec}s left"
                                        etaSec < 3600 -> "${etaSec / 60}m ${etaSec % 60}s left"
                                        else -> "${etaSec / 3600}h ${(etaSec % 3600) / 60}m left"
                                    }
                                    Text(
                                        etaText,
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = ElectricBlue,
                            trackColor = GlassWhite,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // ── Send Button ─────────────────────────────────────────────
            Button(
                onClick = {
                    val uri = selectedUri
                    if (uri != null && selectedName.isNotEmpty()) {
                        val inputStream = context.contentResolver.openInputStream(uri)
                        if (inputStream != null) {
                            vm.sendFile(selectedName, fileSize, inputStream)
                        }
                    }
                },
                enabled = selectedUri != null && fileSize > 0L && progress == 0f,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricBlue,
                    disabledContainerColor = NavyCard,
                ),
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Send to Linux PC", fontWeight = FontWeight.SemiBold)
            }

            if (status?.startsWith("✅") == true || status?.startsWith("❌") == true) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    vm.clearFileStatus()
                    selectedUri  = null
                    selectedName = ""
                    fileSize     = 0L
                }) {
                    Text("Send another file", color = ElectricBlue)
                }
            }
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
