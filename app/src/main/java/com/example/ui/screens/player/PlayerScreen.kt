package com.example.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity
import com.example.tts.TtsVoiceInfo
import com.example.ui.ReaderThemeMode
import com.example.ui.components.SleepTimerDialog
import com.example.ui.components.SpeedSelectorDialog
import com.example.ui.components.VoiceSelectorDialog
import com.example.ui.components.WaveVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FavoriteRed
import com.example.ui.theme.ReadingHighlight
import com.example.ui.theme.ReadingHighlightBorder
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    book: Book?,
    paragraphs: List<BookParagraphEntity>,
    currentParagraphIndex: Int,
    isPlaying: Boolean,
    speechRate: Float,
    availableVoices: List<TtsVoiceInfo>,
    selectedVoice: TtsVoiceInfo?,
    sleepTimerMinutesLeft: Int?,
    fontSize: Float,
    readerTheme: ReaderThemeMode,
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
    onSelectVoice: (TtsVoiceInfo) -> Unit,
    onSamplePlay: (TtsVoiceInfo) -> Unit,
    onStartSleepTimer: (Int) -> Unit,
    onCancelSleepTimer: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onReaderThemeChange: (ReaderThemeMode) -> Unit,
    onToggleFavorite: (Book) -> Unit,
    onReviewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showReaderSettings by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll to the currently spoken paragraph
    LaunchedEffect(currentParagraphIndex) {
        if (paragraphs.isNotEmpty() && currentParagraphIndex in paragraphs.indices) {
            val targetScroll = (currentParagraphIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

    // Dynamic reader background colors
    val (readerBg, readerTextColor) = when (readerTheme) {
        ReaderThemeMode.OLED_DARK -> Color(0xFF000000) to Color(0xFFF1F5F9)
        ReaderThemeMode.MIDNIGHT_BLUE -> Color(0xFF0A1120) to Color(0xFFE2E8F0)
        ReaderThemeMode.SEPIA -> Color(0xFF231F1C) to Color(0xFFEADCC9)
        ReaderThemeMode.SLATE -> Color(0xFF131823) to Color(0xFFE2E8F0)
    }

    val activeParagraph = paragraphs.getOrNull(currentParagraphIndex)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(readerBg)
            .testTag("player_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book?.title ?: "Reproductor",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = book?.author ?: "",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("player_back_button")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Sleep timer badge
                    IconButton(
                        onClick = { showSleepTimerDialog = true },
                        modifier = Modifier.testTag("sleep_timer_button")
                    ) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Temporizador",
                                tint = if (sleepTimerMinutesLeft != null) ElectricCyan else TextSecondary
                            )
                            if (sleepTimerMinutesLeft != null) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan)
                                        .align(Alignment.TopEnd)
                                )
                            }
                        }
                    }

                    // Reader font/theme settings toggle
                    IconButton(
                        onClick = { showReaderSettings = !showReaderSettings },
                        modifier = Modifier.testTag("reader_settings_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatSize,
                            contentDescription = "Ajustes de texto",
                            tint = if (showReaderSettings) ElectricCyan else TextSecondary
                        )
                    }

                    // Review text button
                    IconButton(
                        onClick = onReviewClick,
                        modifier = Modifier.testTag("player_review_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = "Revisar texto",
                            tint = ElectricCyan
                        )
                    }

                    // Favorite toggle
                    book?.let {
                        IconButton(onClick = { onToggleFavorite(it) }) {
                            Icon(
                                imageVector = if (it.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorito",
                                tint = if (it.isFavorite) FavoriteRed else TextSecondary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = readerBg)
            )

            // Collapsible Reader Settings Panel (Font Size & Theme Mode)
            AnimatedVisibility(visible = showReaderSettings) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tamaño de letra: ${fontSize.toInt()}sp",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurface)
                                        .clickable { onFontSizeChange(fontSize - 2f) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("A-", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(DarkSurface)
                                        .clickable { onFontSizeChange(fontSize + 2f) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("A+", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Theme buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ThemeChip(
                                label = "OLED",
                                color = Color(0xFF000000),
                                isSelected = readerTheme == ReaderThemeMode.OLED_DARK,
                                onClick = { onReaderThemeChange(ReaderThemeMode.OLED_DARK) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeChip(
                                label = "Medianoche",
                                color = Color(0xFF0A1120),
                                isSelected = readerTheme == ReaderThemeMode.MIDNIGHT_BLUE,
                                onClick = { onReaderThemeChange(ReaderThemeMode.MIDNIGHT_BLUE) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeChip(
                                label = "Sepia",
                                color = Color(0xFF231F1C),
                                isSelected = readerTheme == ReaderThemeMode.SEPIA,
                                onClick = { onReaderThemeChange(ReaderThemeMode.SEPIA) },
                                modifier = Modifier.weight(1f)
                            )
                            ThemeChip(
                                label = "Pizarra",
                                color = Color(0xFF131823),
                                isSelected = readerTheme == ReaderThemeMode.SLATE,
                                onClick = { onReaderThemeChange(ReaderThemeMode.SLATE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Book Text Reading Area with Paragraph Highlighting
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (paragraphs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Cargando contenido del libro...",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(paragraphs, key = { idx, _ -> idx }) { index, para ->
                            val isHighlighted = index == currentParagraphIndex

                            // Detect chapter heading change
                            val isFirstOfChapter = index == 0 || paragraphs[index - 1].chapterTitle != para.chapterTitle
                            if (isFirstOfChapter) {
                                ChapterHeaderBadge(title = para.chapterTitle, page = para.pageNumber)
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            // Paragraph Item
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isHighlighted) ReadingHighlight else Color.Transparent)
                                    .then(
                                        if (isHighlighted) {
                                            Modifier.border(1.5.dp, ReadingHighlightBorder, RoundedCornerShape(12.dp))
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable { onParagraphClick(index) }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("paragraph_$index")
                            ) {
                                Row(verticalAlignment = Alignment.Top) {
                                    if (isHighlighted && isPlaying) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 4.dp, end = 8.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(ElectricCyan)
                                        )
                                    }
                                    Text(
                                        text = para.text,
                                        color = if (isHighlighted) ElectricCyan else readerTextColor,
                                        fontSize = fontSize.sp,
                                        lineHeight = (fontSize * 1.55f).sp,
                                        fontWeight = if (isHighlighted) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Audio Player Controls Sheet (Spotify/Audible Style)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = DarkSurface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    // Chapter info and soundwave
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeParagraph?.chapterTitle ?: "Lectura continua",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Párrafo ${currentParagraphIndex + 1} de ${paragraphs.size.coerceAtLeast(1)} • Pág ${activeParagraph?.pageNumber ?: 1}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }

                        WaveVisualizer(
                            isPlaying = isPlaying,
                            barCount = 12,
                            maxHeight = 20.dp,
                            barWidth = 2.5.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Progress Slider
                    val progressFraction = if (paragraphs.isNotEmpty()) {
                        (currentParagraphIndex.toFloat() / (paragraphs.size - 1).coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Slider(
                        value = progressFraction,
                        onValueChange = { frac ->
                            if (paragraphs.isNotEmpty()) {
                                val targetIdx = (frac * (paragraphs.size - 1)).toInt()
                                onParagraphClick(targetIdx)
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan,
                            inactiveTrackColor = DarkSurfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(24.dp)
                            .testTag("progress_slider")
                    )

                    // Percentage & Time labels
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${(progressFraction * 100).toInt()}%",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${paragraphs.size - (currentParagraphIndex + 1)} párrafos restantes",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Media Control Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous chapter
                        IconButton(
                            onClick = onPreviousChapter,
                            modifier = Modifier.testTag("prev_chapter_button")
                        ) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                contentDescription = "Capítulo anterior",
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Rewind 15 seconds
                        IconButton(
                            onClick = onSkipBackward15,
                            modifier = Modifier.testTag("rewind_15_button")
                        ) {
                            Icon(
                                Icons.Default.Replay10,
                                contentDescription = "Retroceder",
                                tint = TextPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Big Play / Pause button
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(ElectricCyan, ElectricPurple)
                                    )
                                )
                                .clickable { onTogglePlay() }
                                .testTag("player_play_pause_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        // Forward 15 seconds
                        IconButton(
                            onClick = onSkipForward15,
                            modifier = Modifier.testTag("forward_15_button")
                        ) {
                            Icon(
                                Icons.Default.Forward10,
                                contentDescription = "Avanzar",
                                tint = TextPrimary,
                                modifier = Modifier.size(30.dp)
                            )
                        }

                        // Next chapter
                        IconButton(
                            onClick = onNextChapter,
                            modifier = Modifier.testTag("next_chapter_button")
                        ) {
                            Icon(
                                Icons.Default.SkipNext,
                                contentDescription = "Siguiente capítulo",
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Tools: Speed, Voice, Sleep Timer
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Speed button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .clickable { showSpeedDialog = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("speed_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Speed, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("${speechRate}x", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Voice Selector Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .clickable { showVoiceDialog = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("voice_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                val shortVoiceName = selectedVoice?.displayName?.take(10) ?: "Voz"
                                Text(shortVoiceName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Sleep Timer Button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sleepTimerMinutesLeft != null) ElectricCyan.copy(alpha = 0.2f) else DarkSurfaceVariant)
                                .clickable { showSleepTimerDialog = true }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                                .testTag("timer_chip_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Bedtime,
                                    contentDescription = null,
                                    tint = if (sleepTimerMinutesLeft != null) ElectricCyan else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (sleepTimerMinutesLeft != null) "${sleepTimerMinutesLeft}m" else "Timer",
                                    color = if (sleepTimerMinutesLeft != null) ElectricCyan else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dialogs
        if (showSpeedDialog) {
            SpeedSelectorDialog(
                currentSpeed = speechRate,
                onSelectSpeed = onSelectSpeed,
                onDismiss = { showSpeedDialog = false }
            )
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

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                currentMinutesLeft = sleepTimerMinutesLeft,
                onSelectMinutes = onStartSleepTimer,
                onCancelTimer = onCancelSleepTimer,
                onDismiss = { showSleepTimerDialog = false }
            )
        }
    }
}

@Composable
private fun ChapterHeaderBadge(title: String, page: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(ElectricPurple.copy(alpha = 0.2f))
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Bookmark,
                    contentDescription = null,
                    tint = ElectricPurple,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    color = ElectricPurple,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Página $page",
            color = TextSecondary,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun ThemeChip(
    label: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) ElectricCyan else Color(0xFF334155),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) ElectricCyan else TextPrimary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
