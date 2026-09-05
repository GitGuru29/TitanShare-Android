package com.titanshare.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.titanshare.android.data.network.DaemonClient
import com.titanshare.android.viewmodel.AppViewModel
import kotlin.random.Random

@Composable
fun PairingScreen(
    vm: AppViewModel,
    onConnected: () -> Unit,
    onBack: () -> Unit
) {
    val device    by vm.selectedDevice.collectAsStateWithLifecycle()
    val pin       by vm.pinInput.collectAsStateWithLifecycle()
    val connState by vm.connectionState.collectAsStateWithLifecycle()
    val error     by vm.pairingError.collectAsStateWithLifecycle()

    val isConnecting = connState is DaemonClient.State.Connecting

    LaunchedEffect(connState) {
        if (connState is DaemonClient.State.Connected) onConnected()
    }

    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040814))
    ) {
        // ── Background Artwork ───────────────────────────────────────
        PairingBackground(modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Bar ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable { }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Need help?",
                        color = Color(0xFF00A2FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Outlined.HelpOutline,
                        contentDescription = "Help",
                        tint = Color(0xFF00A2FF),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ── Brand Header (Logo + Title + Subtitle) ────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TitanLogo(modifier = Modifier.size(54.dp))

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Titan",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Share",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00A2FF)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "PAIR DEVICE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E9EAF),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter the 6-digit code shown on your Linux screen\nto connect your device.",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── Laptop Pairing Illustration ────────────────────────────
            LaptopPairingIllustration(
                pairingCode = pin.ifEmpty { "271493" }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── PIN Entry Card ─────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x330B172E))
                    .border(1.dp, Color(0x331E385B), RoundedCornerShape(22.dp))
                    .padding(18.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 6 PIN digit boxes
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { focusRequester.requestFocus() }
                    ) {
                        BasicTextField(
                            value = pin,
                            onValueChange = { vm.updatePin(it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier
                                .size(1.dp)
                                .alpha(0f)
                                .focusRequester(focusRequester),
                            singleLine = true
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (i in 0 until 6) {
                                val char = if (i < pin.length) pin[i].toString() else ""
                                val isFocusedBox = i == pin.length || (i == 5 && pin.length == 6)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0x220E2244))
                                        .border(
                                            width = if (isFocusedBox) 1.5.dp else 1.dp,
                                            color = if (isFocusedBox) Color(0xFF00A2FF) else Color(0x331E385B),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (char.isNotEmpty()) {
                                        Text(
                                            text = char,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    } else if (isFocusedBox && pin.length < 6) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(20.dp)
                                                .background(Color(0xFF00A2FF))
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Info box inside card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x220A1A33))
                            .border(1.dp, Color(0x221E385B), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color(0xFF00A2FF),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Make sure TitanShare is running on your Linux PC and both devices are on the same network.",
                                fontSize = 12.sp,
                                color = Color(0xFF8E9EAF),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    // Error text if pairing fails
                    AnimatedVisibility(visible = error != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = error ?: "",
                            color = Color(0xFFFF4D4D),
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Connect Pill Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = if (pin.length == 6 && !isConnecting) {
                                        listOf(Color(0xFF0052FF), Color(0xFF00A2FF))
                                    } else {
                                        listOf(Color(0xFF0052FF), Color(0xFF00A2FF))
                                    }
                                )
                            )
                            .clickable(enabled = pin.length == 6 && !isConnecting) {
                                vm.pair(onConnected)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isConnecting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Connect",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── OR Divider ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(0.9f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = Color(0x22FFFFFF)
                )
                Text(
                    text = "OR",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0x778E9EAF),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 1.dp,
                    color = Color(0x22FFFFFF)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Scan QR Code Capsule Button ──────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .clip(CircleShape)
                    .background(Color(0x1A0E2244))
                    .border(1.dp, Color(0x331E385B), CircleShape)
                    .clickable { }
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = "Scan QR",
                        tint = Color(0xFF00A2FF),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Scan QR Code",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                        tint = Color(0xFF8E9EAF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Bottom Tagline ──────────────────────────────────────────
            Text(
                text = "CONTROL BEYOND BORDERS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0x558E9EAF),
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
private fun LaptopPairingIllustration(
    modifier: Modifier = Modifier,
    pairingCode: String = "271493"
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer aura glow behind laptop
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF00A2FF).copy(alpha = 0.25f),
                        Color(0xFF0052FF).copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = size.width * 0.45f
                ),
                center = Offset(cx, cy),
                radius = size.width * 0.45f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Laptop Lid / Screen Frame
            Box(
                modifier = Modifier
                    .width(230.dp)
                    .height(125.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .background(Color(0xFF0B1424))
                    .border(1.5.dp, Color(0xFF1E385B), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                // Laptop Screen Display Window
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF040A14))
                        .border(1.dp, Color(0x3300A2FF), RoundedCornerShape(8.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top mini header inside laptop screen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start
                        ) {
                            TitanMiniLogo(modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TitanShare",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Center pairing info inside laptop screen
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0x4408162A))
                                .border(1.dp, Color(0x3300A2FF), RoundedCornerShape(6.dp))
                                .padding(vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Link,
                                    contentDescription = null,
                                    tint = Color(0xFF00A2FF),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Pairing Code",
                                    color = Color(0xFF8E9EAF),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = pairingCode,
                                    color = Color(0xFF00F0FF),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Laptop Base / Keyboard Deck
            Box(
                modifier = Modifier
                    .width(260.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF1E385B),
                                Color(0xFF0B1424)
                            )
                        )
                    )
            )

            // Laptop Base shadow line reflection
            Box(
                modifier = Modifier
                    .width(210.dp)
                    .height(2.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF00A2FF).copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun TitanMiniLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.10f, h * 0.05f)
            lineTo(w * 0.95f, h * 0.05f)
            lineTo(w * 0.76f, h * 0.28f)
            lineTo(w * 0.58f, h * 0.28f)
            lineTo(w * 0.42f, h * 0.95f)
            lineTo(w * 0.24f, h * 0.95f)
            lineTo(w * 0.40f, h * 0.28f)
            lineTo(w * 0.22f, h * 0.28f)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF00F0FF), Color(0xFF0066FF)),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )
    }
}

@Composable
private fun PairingBackground(modifier: Modifier = Modifier) {
    val starPoints = remember {
        List(35) {
            Pair(Random.nextFloat(), Random.nextFloat() * 0.45f)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Dark space gradient
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF030712),
                    Color(0xFF050C1C),
                    Color(0xFF030712)
                )
            )
        )

        // Star dots
        starPoints.forEach { (xRatio, yRatio) ->
            val x = width * xRatio
            val y = height * yRatio
            val radius = (0.8f + (xRatio * yRatio * 1.5f)).dp.toPx()
            val alpha = 0.25f + ((xRatio + yRatio) % 0.4f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )
        }

        // Bottom Mountain silhouettes
        val mountainPath = Path().apply {
            moveTo(0f, height * 0.88f)
            lineTo(width * 0.20f, height * 0.82f)
            lineTo(width * 0.40f, height * 0.87f)
            lineTo(width * 0.60f, height * 0.80f)
            lineTo(width * 0.80f, height * 0.86f)
            lineTo(width, height * 0.81f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = mountainPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF09162B).copy(alpha = 0.5f),
                    Color(0xFF02050A)
                ),
                startY = height * 0.80f,
                endY = height
            )
        )
    }
}
