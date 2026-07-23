package com.titanshare.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.titanshare.android.ui.components.GlassCard
import com.titanshare.android.ui.theme.*
import com.titanshare.android.viewmodel.AppViewModel
import kotlin.math.roundToInt

@Composable
fun TrackpadScreen(vm: AppViewModel, onBack: () -> Unit) {
    var isRightClick by remember { mutableStateOf(false) }
    var scrollMode   by remember { mutableStateOf(false) }

    // Accumulate small movements and send batched
    var accX by remember { mutableFloatStateOf(0f) }
    var accY by remember { mutableFloatStateOf(0f) }
    val sensitivity = 1.8f

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
                    .padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
                }
                Text(
                    "Trackpad",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(start = 4.dp),
                )
                // Scroll mode toggle
                FilterChip(
                    selected = scrollMode,
                    onClick = { scrollMode = !scrollMode },
                    label = { Text(if (scrollMode) "Scroll" else "Move") },
                    leadingIcon = {
                        Icon(
                            if (scrollMode) Icons.Default.SwapVert else Icons.Default.Mouse,
                            null, modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor     = ElectricBlue.copy(alpha = 0.25f),
                        selectedLabelColor         = ElectricBlue,
                        selectedLeadingIconColor   = ElectricBlue,
                    )
                )
            }

            // ── Trackpad surface ───────────────────────────────────────
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .pointerInput(scrollMode) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            accX += dragAmount.x * sensitivity
                            accY += dragAmount.y * sensitivity

                            val dx = accX.roundToInt()
                            val dy = accY.roundToInt()

                            if (dx != 0 || dy != 0) {
                                if (scrollMode) {
                                    vm.mouseScroll(-dy / 4)
                                } else {
                                    vm.mouseMove(dx, dy)
                                }
                                accX -= dx
                                accY -= dy
                            }
                        }
                    },
                cornerRadius = 24.dp,
                innerPadding = 0.dp,
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Mouse, null,
                            tint = GlassBorderBright,
                            modifier = Modifier.size(48.dp),
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (scrollMode) "Drag to scroll" else "Drag to move cursor",
                            color = TextMuted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            // ── Click buttons ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Left click
                ClickButton(
                    label = "Left Click",
                    modifier = Modifier.weight(1f),
                    color = ElectricBlue,
                ) { vm.mouseClick("left") }

                // Middle click
                ClickButton(
                    label = "Middle",
                    modifier = Modifier.weight(0.6f),
                    color = TextMuted,
                ) { vm.mouseClick("middle") }

                // Right click
                ClickButton(
                    label = "Right Click",
                    modifier = Modifier.weight(1f),
                    color = TextSecondary,
                ) { vm.mouseClick("right") }
            }

            // ── Keyboard shortcut row ──────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf("Esc", "Tab", "Enter", "Backspace", "Delete").forEach { key ->
                    OutlinedButton(
                        onClick = { vm.pressKey(key) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(listOf(GlassBorder, GlassBorder))
                        ),
                    ) {
                        Text(key, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
private fun ClickButton(
    label: String,
    modifier: Modifier,
    color: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.15f),
            contentColor   = color,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
