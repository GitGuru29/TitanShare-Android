package com.titanshare.android.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanshare.android.ui.theme.ElectricBlue
import kotlin.random.Random

@Composable
fun WelcomeScreen(
    onGetStarted: () -> Unit,
    onSkip: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF040814))
    ) {
        // ── Futuristic Background (Space, Planet Arc, Mountains) ────────────
        WelcomeBackground(modifier = Modifier.fillMaxSize())

        // ── Main Content Layout ─────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── Top Header Bar: Skip Button ─────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x334BA2FF), CircleShape)
                        .clickable { onSkip() }
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Skip",
                            color = Color(0xFFD6E6FE),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Skip",
                            tint = Color(0xFFD6E6FE),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Brand Header (Logo + Title + Subtitle) ──────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Titan Logo Mark
                TitanLogo(modifier = Modifier.size(80.dp))

                Spacer(modifier = Modifier.height(16.dp))

                // Title: TitanShare
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Titan",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Share",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue,
                        letterSpacing = (-0.5).sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Subtitle 1: YOUR ANDROID ↔ LINUX ECOSYSTEM
                Text(
                    text = "YOUR ANDROID \u2194 LINUX ECOSYSTEM",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8E9EAF),
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Tagline 2: Control. Transfer. Mirror. Anywhere.
                Text(
                    text = "Control. Transfer. Mirror. Anywhere.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Normal,
                    color = Color(0xFFA0AFBF)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Feature List Cards ──────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FeatureItemCard(
                    icon = Icons.Outlined.Laptop,
                    title = "Remote Control",
                    description = "Trackpad, keyboard and more"
                )
                FeatureItemCard(
                    icon = Icons.Outlined.Folder,
                    title = "File Transfer",
                    description = "Fast. Secure. Offline."
                )
                FeatureItemCard(
                    icon = Icons.Outlined.Monitor,
                    title = "Screen Mirror",
                    description = "See and control your phone"
                )
                FeatureItemCard(
                    icon = Icons.Outlined.Settings,
                    title = "System Monitor",
                    description = "CPU, RAM, Storage and more"
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // ── Bottom Action Button & Footer ───────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Get Started Pill Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF0066FF),
                                    Color(0xFF00AAFF)
                                )
                            )
                        )
                        .clickable { onGetStarted() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Get Started",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer: Built for ArchTitan with flanking lines
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color(0x22FFFFFF)
                    )
                    Text(
                        text = "Built for ArchTitan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0x778E9EAF),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    HorizontalDivider(
                        modifier = Modifier.weight(1f),
                        thickness = 1.dp,
                        color = Color(0x22FFFFFF)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItemCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Glowing Icon Container
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x330E1F3D))
                .border(1.dp, Color(0x333A82F7), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = Color(0xFF00A2FF),
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title and Description
        Column {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 13.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF8E9EAF)
            )
        }
    }
}

@Composable
private fun TitanLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val logoGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF00F0FF),
                Color(0xFF0072FF),
                Color(0xFF0044FF)
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        // Stylized angled "T" path matching TitanShare logo
        val path = Path().apply {
            // Top bar upper left
            moveTo(w * 0.10f, h * 0.05f)
            // Top bar upper right
            lineTo(w * 0.95f, h * 0.05f)
            // Top bar right edge angle
            lineTo(w * 0.76f, h * 0.28f)
            // Inner right cut
            lineTo(w * 0.58f, h * 0.28f)
            // Stem right edge down to bottom point
            lineTo(w * 0.42f, h * 0.95f)
            // Stem bottom edge left
            lineTo(w * 0.24f, h * 0.95f)
            // Stem left edge up to inner left cut
            lineTo(w * 0.40f, h * 0.28f)
            // Inner left cut to left edge angle
            lineTo(w * 0.22f, h * 0.28f)
            close()
        }

        drawPath(path = path, brush = logoGradient)
    }
}

@Composable
private fun WelcomeBackground(modifier: Modifier = Modifier) {
    val starPoints = remember {
        List(45) {
            Pair(Random.nextFloat(), Random.nextFloat() * 0.5f)
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // 1. Dark space backdrop
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF030712),
                    Color(0xFF060D20),
                    Color(0xFF040814)
                )
            )
        )

        // 2. Stars dots
        starPoints.forEach { (xRatio, yRatio) ->
            val x = width * xRatio
            val y = height * yRatio
            val radius = (0.8f + (xRatio * yRatio * 1.5f)).dp.toPx()
            val alpha = 0.25f + ((xRatio + yRatio) % 0.5f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )
        }

        // 3. Planet Arc at upper center
        val planetCenterX = width / 2f
        val planetRadius = width * 1.15f
        val planetCenterY = -planetRadius * 0.62f

        // Outer blue aura glow
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF00A2FF).copy(alpha = 0.35f),
                    Color(0xFF0052FF).copy(alpha = 0.12f),
                    Color.Transparent
                ),
                center = Offset(planetCenterX, planetCenterY + planetRadius * 0.88f),
                radius = planetRadius * 0.65f
            ),
            center = Offset(planetCenterX, planetCenterY + planetRadius * 0.88f),
            radius = planetRadius * 0.65f
        )

        // Planet dark body
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A2B55),
                    Color(0xFF051126),
                    Color(0xFF030712)
                ),
                startY = planetCenterY - planetRadius,
                endY = planetCenterY + planetRadius
            ),
            radius = planetRadius,
            center = Offset(planetCenterX, planetCenterY)
        )

        // Rim highlight stroke
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFF0052FF),
                    Color(0xFF00F0FF),
                    Color(0xFF38BDF8),
                    Color(0xFF0052FF)
                ),
                center = Offset(planetCenterX, planetCenterY)
            ),
            radius = planetRadius,
            center = Offset(planetCenterX, planetCenterY),
            style = Stroke(width = 2.5dp.toPx())
        )

        // 4. Mountain Silhouettes at bottom
        val mountainPath = Path().apply {
            moveTo(0f, height * 0.82f)
            lineTo(width * 0.18f, height * 0.74f)
            lineTo(width * 0.35f, height * 0.79f)
            lineTo(width * 0.52f, height * 0.68f)
            lineTo(width * 0.70f, height * 0.76f)
            lineTo(width * 0.86f, height * 0.71f)
            lineTo(width, height * 0.79f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = mountainPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0A1B36).copy(alpha = 0.6f),
                    Color(0xFF030814)
                ),
                startY = height * 0.68f,
                endY = height
            )
        )

        val detailMountainPath = Path().apply {
            moveTo(0f, height * 0.88f)
            lineTo(width * 0.25f, height * 0.80f)
            lineTo(width * 0.45f, height * 0.86f)
            lineTo(width * 0.65f, height * 0.77f)
            lineTo(width * 0.82f, height * 0.84f)
            lineTo(width, height * 0.78f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        drawPath(
            path = detailMountainPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF071224),
                    Color(0xFF02050A)
                ),
                startY = height * 0.77f,
                endY = height
            )
        )
    }
}
