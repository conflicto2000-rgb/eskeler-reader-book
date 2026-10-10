package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.example.tts.TtsVoiceInfo
import com.example.ui.ReaderThemeMode
import com.example.ui.components.VoiceSelectorDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    speechRate: Float,
    speechPitch: Float,
    availableVoices: List<TtsVoiceInfo>,
    selectedVoice: TtsVoiceInfo?,
    fontSize: Float,
    readerTheme: ReaderThemeMode,
    onSpeechRateChange: (Float) -> Unit,
    onSpeechPitchChange: (Float) -> Unit,
    onSelectVoice: (TtsVoiceInfo) -> Unit,
    onSamplePlay: (TtsVoiceInfo) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onReaderThemeChange: (ReaderThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var showVoiceDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ajustes y Voces",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = "Personaliza la lectura en voz alta y la interfaz",
                color = TextSecondary,
                fontSize = 13.sp
            )
        }

        // Section: Síntesis de Voz
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Voz de lectura", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selected voice card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { showVoiceDialog = true }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = selectedVoice?.displayName ?: "Voz predeterminada",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "${selectedVoice?.genderGuess ?: "Español"} • Pulsa para cambiar voz",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        selectedVoice?.let { voice ->
                            IconButton(
                                onClick = { onSamplePlay(voice) },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Muestra", tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Velocidad de lectura
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Velocidad de reproducción", color = TextPrimary, fontSize = 14.sp)
                        Text("${String.format("%.2f", speechRate)}x", color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechRate,
                        onValueChange = onSpeechRateChange,
                        valueRange = 0.5f..3.0f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("speech_rate_slider")
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tono de voz
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tono de voz (Pitch)", color = TextPrimary, fontSize = 14.sp)
                        Text("${String.format("%.2f", speechPitch)}x", color = ElectricPurple, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = speechPitch,
                        onValueChange = onSpeechPitchChange,
                        valueRange = 0.6f..1.5f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricPurple,
                            activeTrackColor = ElectricPurple,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier.testTag("speech_pitch_slider")
                    )
                }
            }
        }

        // Section: Visualización de lectura
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(ElectricPurple.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Modo Lectura y Tipografía", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tamaño del texto", color = TextPrimary, fontSize = 14.sp)
                        Text("${fontSize.toInt()} sp", color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = fontSize,
                        onValueChange = onFontSizeChange,
                        valueRange = 14f..28f,
                        steps = 6,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Tema visual del lector", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val themes = listOf(
                            Triple("OLED", Color(0xFF000000), ReaderThemeMode.OLED_DARK),
                            Triple("Azul", Color(0xFF0A1120), ReaderThemeMode.MIDNIGHT_BLUE),
                            Triple("Sepia", Color(0xFF231F1C), ReaderThemeMode.SEPIA),
                            Triple("Pizarra", Color(0xFF131823), ReaderThemeMode.SLATE)
                        )
                        themes.forEach { (name, color, mode) ->
                            val isSelected = readerTheme == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(color)
                                    .clickable { onReaderThemeChange(mode) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    color = if (isSelected) ElectricCyan else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section: Privacidad y Offline
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Privacidad y Modo Offline", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• Procesamiento 100% local: los documentos PDF se leen y extraen en tu dispositivo sin enviarse a servidores externos.\n• Base de datos Room offline para recordar tu progreso, capítulos y favoritos.\n• Las voces del motor Android TTS funcionan sin conexión a internet.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // Section: Acerca de la app
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "AudioBook Reader v1.0",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Desarrollado con Jetpack Compose, Material 3, Android TextToSpeech y Room Database.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showVoiceDialog) {
        VoiceSelectorDialog(
            voices = availableVoices,
            selectedVoice = selectedVoice,
            onSelectVoice = onSelectVoice,
            onSamplePlay = onSamplePlay,
            onDismiss = { showVoiceDialog = false }
        )
    }
}
