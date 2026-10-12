package com.example.ui.screens.player

import android.speech.tts.Voice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanSubtle
import com.example.ui.theme.ElectricCyanTrack
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    book: Book?,
    paragraphs: List<BookParagraphEntity>,
    currentParagraphIndex: Int,
    isPlaying: Boolean,
    speechRate: Float,
    availableVoices: List<Voice>,
    selectedVoice: Voice?,
    sleepTimerMinutesLeft: Int?,
    fontSize: Int,
    readerTheme: String,
    onBackClick: () -> Unit,
    onTogglePlay: () -> Unit,
    onParagraphClick: (Int) -> Unit,
    onNextParagraph: () -> Unit,
    onPreviousParagraph: () -> Unit,
    onSkipForward15: () -> Unit,
    onSkipBackward15: () -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onSelectSpeed: (Float) -> Unit,
    onSelectVoice: (Voice) -> Unit,
    onSamplePlay: (Voice) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onFontSizeChange: (Int) -> Unit,
    onReaderThemeChange: (String) -> Unit,
    onToggleFavorite: (Book) -> Unit,
    onReviewClick: () -> Unit,
    onOpenPdfReader: (() -> Unit)? = null
) {
    if (book == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            Text("No hay libro seleccionado", color = TextSecondary)
        }
        return
    }

    var showChaptersDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showVoicesDialog by remember { mutableStateOf(false) }
    var showTimerDialog by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll transcript to active paragraph
    LaunchedEffect(currentParagraphIndex) {
        if (paragraphs.isNotEmpty() && currentParagraphIndex in paragraphs.indices) {
            listState.animateScrollToItem((currentParagraphIndex - 1).coerceAtLeast(0))
        }
    }

    val currentPara = paragraphs.getOrNull(currentParagraphIndex)
    val currentChapter = currentPara?.chapterTitle ?: "Página ${book.currentPage}"
    val totalParas = paragraphs.size.coerceAtLeast(1)
    val progress = (currentParagraphIndex.toFloat() / totalParas.toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book.title,
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book.author,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("player_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Open in PDF Reader
                    if (onOpenPdfReader != null) {
                        IconButton(onClick = onOpenPdfReader) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = "Ver documento PDF",
                                tint = ElectricCyan
                            )
                        }
                    }

                    // OCR Review
                    IconButton(onClick = onReviewClick) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "Revisar texto OCR",
                            tint = TextSecondary
                        )
                    }

                    // Favorite
                    IconButton(onClick = { onToggleFavorite(book) }) {
                        Icon(
                            imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorito",
                            tint = if (book.isFavorite) Color(0xFFEF4444) else TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )

            // Top Area: Portada Destacada y Controles Modernos
            Surface(
                color = DarkSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorderSubtle))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Portada Destacada Minimalista
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.5.dp, ElectricCyan, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Pág. ${currentPara?.pageNumber ?: book.currentPage}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Capítulo actual
                    Text(
                        text = currentChapter,
                        color = ElectricCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Barra de progreso interactiva
                    Slider(
                        value = progress,
                        onValueChange = { newProg ->
                            val targetIdx = (newProg * totalParas).roundToInt().coerceIn(0, totalParas - 1)
                            onParagraphClick(targetIdx)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .testTag("player_progress_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = ElectricCyanTrack
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Párrafo ${currentParagraphIndex + 1} de $totalParas",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "${(progress * 100).roundToInt()}%",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Botones modernos de reproducción: Pause/Play, Avanzar, Retroceder, Capítulos
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip Backward 15s
                        IconButton(
                            onClick = onSkipBackward15,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("player_skip_back_15")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "Retroceder 15s",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Previous Paragraph
                        IconButton(
                            onClick = onPreviousParagraph,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("player_prev_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Párrafo anterior",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Main Play / Pause Button in Luminous Cyan
                        IconButton(
                            onClick = onTogglePlay,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan)
                                .testTag("player_play_pause_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        // Next Paragraph
                        IconButton(
                            onClick = onNextParagraph,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("player_next_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Párrafo siguiente",
                                tint = TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Skip Forward 15s
                        IconButton(
                            onClick = onSkipForward15,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("player_skip_fwd_15")
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "Avanzar 15s",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Acceso a Capítulos, Velocidad, Voces y Temporizador
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        // Capítulos
                        ActionPill(
                            icon = Icons.Default.List,
                            label = "Capítulos",
                            onClick = { showChaptersDialog = true }
                        )

                        // Velocidad
                        ActionPill(
                            icon = Icons.Default.Speed,
                            label = "${speechRate}x",
                            onClick = { showSpeedDialog = true }
                        )

                        // Voces
                        ActionPill(
                            icon = Icons.Default.RecordVoiceOver,
                            label = "Voz",
                            onClick = { showVoicesDialog = true }
                        )

                        // Temporizador
                        ActionPill(
                            icon = Icons.Default.Timer,
                            label = if (sleepTimerMinutesLeft != null) "${sleepTimerMinutesLeft}m" else "Timer",
                            isActive = sleepTimerMinutesLeft != null,
                            onClick = { showTimerDialog = true }
                        )
                    }
                }
            }

            // Transcripción sincronizada debajo del reproductor
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(paragraphs, key = { idx, _ -> idx }) { idx, para ->
                        val isCurrent = idx == currentParagraphIndex
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isCurrent) DarkSurfaceVariant else DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isCurrent) 1.5.dp else 0.5.dp,
                                color = if (isCurrent) ElectricCyan else DarkBorderSubtle
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onParagraphClick(idx) }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                if (isCurrent) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = null,
                                        tint = ElectricCyan,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${para.chapterTitle} • Pág. ${para.pageNumber}",
                                            color = if (isCurrent) ElectricCyan else TextTertiary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (para.isUserEdited) {
                                            Text("Editado", color = ElectricCyan, fontSize = 9.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = para.text,
                                        color = if (isCurrent) TextPrimary else TextSecondary,
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * 1.45).sp,
                                        fontWeight = if (isCurrent) FontWeight.Medium else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Diálogo de Capítulos
        if (showChaptersDialog) {
            val chapters = remember(paragraphs) {
                paragraphs.groupBy { it.chapterTitle }.map { (title, list) ->
                    Pair(title, list.first().paragraphIndex)
                }
            }
            AlertDialog(
                onDismissRequest = { showChaptersDialog = false },
                containerColor = DarkSurface,
                title = { Text("Capítulos y Secciones", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(chapters.size) { i ->
                            val (chTitle, chIdx) = chapters[i]
                            val isSelected = currentPara?.chapterTitle == chTitle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onParagraphClick(chIdx)
                                        showChaptersDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = chTitle,
                                    color = if (isSelected) ElectricCyan else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 14.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                }
                            }
                            HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showChaptersDialog = false }) {
                        Text("Cerrar", color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Diálogo de Velocidad
        if (showSpeedDialog) {
            AlertDialog(
                onDismissRequest = { showSpeedDialog = false },
                containerColor = DarkSurface,
                title = { Text("Velocidad de reproducción", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { spd ->
                            val isSel = (speechRate - spd).let { kotlin.math.abs(it) < 0.05f }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) ElectricCyan else DarkSurfaceVariant)
                                    .clickable {
                                        onSelectSpeed(spd)
                                        showSpeedDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = "${spd}x",
                                    color = if (isSel) Color.Black else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showSpeedDialog = false }) {
                        Text("Aceptar", color = ElectricCyan)
                    }
                }
            )
        }

        // Diálogo de Voces
        if (showVoicesDialog) {
            AlertDialog(
                onDismissRequest = { showVoicesDialog = false },
                containerColor = DarkSurface,
                title = { Text("Voces TTS del Sistema", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    if (availableVoices.isEmpty()) {
                        Text("Cargando voces instaladas...", color = TextSecondary)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                            items(availableVoices.size) { i ->
                                val v = availableVoices[i]
                                val isSel = v.name == selectedVoice?.name
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectVoice(v)
                                            showVoicesDialog = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = v.locale.displayName,
                                            color = if (isSel) ElectricCyan else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Text(text = v.name.takeLast(24), color = TextTertiary, fontSize = 10.sp)
                                    }
                                    IconButton(onClick = { onSamplePlay(v) }, modifier = Modifier.size(32.dp)) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Probar", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                    }
                                }
                                HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVoicesDialog = false }) {
                        Text("Cerrar", color = ElectricCyan)
                    }
                }
            )
        }

        // Diálogo de Temporizador
        if (showTimerDialog) {
            AlertDialog(
                onDismissRequest = { showTimerDialog = false },
                containerColor = DarkSurface,
                title = { Text("Temporizador de apagado", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            text = if (sleepTimerMinutesLeft != null) "Tiempo restante: $sleepTimerMinutesLeft minutos" else "Detiene la lectura automáticamente:",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            listOf(15, 30, 45, 60).forEach { mins ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurfaceVariant)
                                        .clickable {
                                            onStartSleepTimer(mins)
                                            showTimerDialog = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Text("${mins}m", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        if (sleepTimerMinutesLeft != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = {
                                    onCancelSleepTimer()
                                    showTimerDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancelar temporizador activo", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showTimerDialog = false }) {
                        Text("Cerrar", color = ElectricCyan)
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) ElectricCyanSubtle else DarkSurfaceVariant)
            .border(1.dp, if (isActive) ElectricCyan else DarkBorderSubtle, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) ElectricCyan else TextSecondary,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = label,
            color = if (isActive) ElectricCyan else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
