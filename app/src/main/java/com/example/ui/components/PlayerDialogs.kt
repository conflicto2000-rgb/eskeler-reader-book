package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tts.TtsVoiceInfo
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SleepTimerDialog(
    currentMinutesLeft: Int?,
    onSelectMinutes: (Int) -> Unit,
    onCancelTimer: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 10, 15, 30, 45, 60)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("sleep_timer_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = null,
                                tint = ElectricPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Temporizador de apagado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }

                if (currentMinutesLeft != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Activo: se detendrá en $currentMinutesLeft min",
                        color = ElectricCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                options.chunked(3).forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowOptions.forEach { mins ->
                            val isSelected = currentMinutesLeft == mins
                            Button(
                                onClick = {
                                    onSelectMinutes(mins)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("timer_opt_$mins"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ElectricCyan else DarkSurfaceVariant,
                                    contentColor = if (isSelected) Color.Black else TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("$mins m", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                if (currentMinutesLeft != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Desactivar temporizador", color = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}

@Composable
fun SpeedSelectorDialog(
    currentSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("speed_selector_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Velocidad de lectura",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                speeds.chunked(3).forEach { rowSpeeds ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowSpeeds.forEach { spd ->
                            val isSelected = (currentSpeed - spd).let { kotlin.math.abs(it) < 0.05f }
                            Button(
                                onClick = {
                                    onSelectSpeed(spd)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("speed_opt_$spd"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) ElectricCyan else DarkSurfaceVariant,
                                    contentColor = if (isSelected) Color.Black else TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("${spd}x", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun VoiceSelectorDialog(
    voices: List<TtsVoiceInfo>,
    selectedVoice: TtsVoiceInfo?,
    onSelectVoice: (TtsVoiceInfo) -> Unit,
    onSamplePlay: (TtsVoiceInfo) -> Unit,
    onDismiss: () -> Unit
) {
    var showOnlySpanish by remember { mutableStateOf(true) }

    val displayedVoices = remember(voices, showOnlySpanish) {
        if (showOnlySpanish) {
            voices.filter { it.isSpanish }
        } else {
            voices
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp)
                .testTag("voice_selector_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricPurple.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.RecordVoiceOver,
                                contentDescription = null,
                                tint = ElectricPurple,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Voces de lectura",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Language toggle filter
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (showOnlySpanish) ElectricPurple else Color.Transparent)
                            .clickable { showOnlySpanish = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Solo Español",
                            color = if (showOnlySpanish) Color.White else TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (!showOnlySpanish) ElectricPurple else Color.Transparent)
                            .clickable { showOnlySpanish = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Todos los idiomas",
                            color = if (!showOnlySpanish) Color.White else TextSecondary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (displayedVoices.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron voces disponibles en este filtro.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedVoices, key = { it.id }) { voice ->
                            val isSelected = selectedVoice?.id == voice.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) DarkSurfaceVariant else Color.Transparent)
                                    .clickable { onSelectVoice(voice) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { onSelectVoice(voice) },
                                    colors = RadioButtonDefaults.colors(selectedColor = ElectricCyan)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = voice.displayName,
                                        color = if (isSelected) ElectricCyan else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "${voice.genderGuess} • ${if (voice.isNetworkConnectionRequired) "Calidad Alta (Red)" else "Offline"}",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                IconButton(
                                    onClick = { onSamplePlay(voice) },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan.copy(alpha = 0.15f))
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Escuchar muestra",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Aceptar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
