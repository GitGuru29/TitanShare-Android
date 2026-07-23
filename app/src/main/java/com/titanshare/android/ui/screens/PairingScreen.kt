package com.titanshare.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.titanshare.android.data.network.DaemonClient
import com.titanshare.android.ui.components.GlassCard
import com.titanshare.android.ui.theme.*
import com.titanshare.android.viewmodel.AppViewModel

@Composable
fun PairingScreen(vm: AppViewModel, onConnected: () -> Unit, onBack: () -> Unit) {
    val device    by vm.selectedDevice.collectAsStateWithLifecycle()
    val pin       by vm.pinInput.collectAsStateWithLifecycle()
    val connState by vm.connectionState.collectAsStateWithLifecycle()
    val error     by vm.pairingError.collectAsStateWithLifecycle()

    val isConnecting = connState is DaemonClient.State.Connecting

    // Navigate once connected
    LaunchedEffect(connState) {
        if (connState is DaemonClient.State.Connected) onConnected()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Color(0xFF030810))))
    ) {
        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.padding(start = 16.dp, top = 48.dp)
        ) {
            Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {

            // ── Device icon ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(ElectricBlue.copy(alpha = 0.12f))
                    .border(1.5.dp, ElectricBlue.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.LaptopMac, null, tint = ElectricBlue, modifier = Modifier.size(38.dp))
            }

            Spacer(Modifier.height(20.dp))

            Text(
                device?.name ?: "Linux PC",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                device?.host ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )

            Spacer(Modifier.height(36.dp))

            // ── PIN card ────────────────────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth(), cornerRadius = 22.dp, innerPadding = 28.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Lock, null, tint = ElectricBlue, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Enter Pairing Code",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Enter the 6-digit code shown on your Linux screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(28.dp))

                    // PIN digit blocks
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        for (i in 0 until 6) {
                            val char      = if (i < pin.length) pin[i].toString() else ""
                            val isCurrent = i == pin.length && pin.length < 6
                            Box(
                                modifier = Modifier
                                    .size(width = 42.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricBlue.copy(alpha = if (isCurrent) 0.2f else 0.1f))
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isCurrent) ElectricBlue else GlassBorder,
                                        shape = RoundedCornerShape(10.dp),
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    char,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextPrimary,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Number input field
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { vm.updatePin(it) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        placeholder = { Text("Tap to enter PIN", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = ElectricBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = ElectricBlue,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(16.dp))

                    AnimatedVisibility(visible = error != null) {
                        Text(
                            error ?: "",
                            color = DangerRed,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 10.dp),
                        )
                    }

                    Button(
                        onClick = { vm.pair(onConnected) },
                        enabled = pin.length == 6 && !isConnecting,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("Connect", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
