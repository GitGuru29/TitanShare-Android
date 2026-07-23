package com.titanshare.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.titanshare.android.ui.components.GlassCard
import com.titanshare.android.ui.theme.*
import com.titanshare.android.viewmodel.AppViewModel

private val SPECIAL_KEYS = listOf(
    "Esc", "F1", "F2", "F3", "F4", "F5",
    "F6", "F7", "F8", "F9", "F10", "F11",
    "Tab", "Enter", "Backspace", "Delete",
    "Home", "End", "PgUp", "PgDn",
    "↑", "↓", "←", "→",
    "Ctrl+C", "Ctrl+V", "Ctrl+Z", "Ctrl+A",
    "Ctrl+X", "Ctrl+S", "Alt+F4", "Super",
)

@Composable
fun KeyboardScreen(vm: AppViewModel, onBack: () -> Unit) {
    var textInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(NavyDeep, Color(0xFF030810))))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp)) {

            // ── Header ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back", tint = TextSecondary)
                }
                Text(
                    "Remote Keyboard",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }

            // ── Text send ──────────────────────────────────────────────
            GlassCard(modifier = Modifier.fillMaxWidth(), innerPadding = 16.dp) {
                Column {
                    Text("TYPE TEXT", style = MaterialTheme.typography.labelSmall,
                        color = ElectricBlue, letterSpacing = 2.sp)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = { Text("Enter text to type on PC…", color = TextMuted) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = ElectricBlue,
                            unfocusedBorderColor = GlassBorder,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                        ),
                        singleLine = false,
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (textInput.isNotEmpty()) {
                                vm.typeText(textInput)
                                textInput = ""
                            }
                        }),
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            if (textInput.isNotEmpty()) {
                                vm.typeText(textInput)
                                textInput = ""
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(10.dp),
                        enabled = textInput.isNotEmpty(),
                    ) {
                        Icon(Icons.Default.Keyboard, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Send")
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("SPECIAL KEYS", style = MaterialTheme.typography.labelSmall,
                color = ElectricBlue, letterSpacing = 2.sp)
            Spacer(Modifier.height(10.dp))

            // ── Key grid ────────────────────────────────────────────────
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(SPECIAL_KEYS) { key ->
                    val isCombo = key.contains('+')
                    OutlinedButton(
                        onClick = { vm.pressKey(key) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isCombo) ElectricBlue.copy(alpha = 0.1f) else Color.Transparent,
                            contentColor   = if (isCombo) ElectricBlue else TextSecondary,
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = Brush.linearGradient(
                                listOf(
                                    if (isCombo) ElectricBlue.copy(0.4f) else GlassBorder,
                                    if (isCombo) ElectricBlue.copy(0.2f) else GlassBorder,
                                )
                            )
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            key,
                            fontSize = 11.sp,
                            maxLines = 1,
                            fontWeight = if (isCombo) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}
