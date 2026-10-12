package com.example.ui.screens.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity
import com.example.data.pdf.OcrLanguage
import com.example.data.pdf.PreprocessingMode
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkBorderSubtle
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricCyanSubtle
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

enum class ReaderDisplayMode {
    ORIGINAL_PAGE, OCR_TEXT
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    book: Book,
    paragraphs: List<BookParagraphEntity>,
    bookmarkedPages: Set<Int>,
    isPlaying: Boolean,
    selectedOcrLanguage: OcrLanguage,
    selectedPreprocessingMode: PreprocessingMode,
    isReprocessingPage: Boolean,
    onBackClick: () -> Unit,
    onPageChanged: (Int) -> Unit,
    onToggleBookmark: (Int) -> Unit,
    onPlayPageText: (Int) -> Unit,
    onTogglePlay: () -> Unit,
    onOpenPlayer: () -> Unit,
    onReviewClick: () -> Unit,
    onOcrLanguageChange: (OcrLanguage) -> Unit,
    onPreprocessingModeChange: (PreprocessingMode) -> Unit,
    onReprocessPageWithOcr: (Int, OcrLanguage, PreprocessingMode) -> Unit
) {
    val context = LocalContext.current
    val totalPages = (book.totalPages).coerceAtLeast(1)
    var currentPage by remember { mutableIntStateOf(book.currentPage.coerceIn(1, totalPages)) }

    var displayMode by remember { mutableStateOf(ReaderDisplayMode.ORIGINAL_PAGE) }
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showOcrDialog by remember { mutableStateOf(false) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }

    // Page bitmap cache
    val pageBitmaps = remember { mutableStateMapOf<Int, Bitmap?>() }
    var isRenderingPage by remember { mutableStateOf(false) }

    // Notify page changes and save current page
    fun setPage(newPage: Int) {
        val clamped = newPage.coerceIn(1, totalPages)
        if (clamped != currentPage) {
            currentPage = clamped
            zoomScale = 1f
            offsetX = 0f
            offsetY = 0f
            onPageChanged(clamped)
        }
    }

    // Render original PDF page bitmap via PdfRenderer
    LaunchedEffect(book.filePath, currentPage) {
        if (!pageBitmaps.containsKey(currentPage)) {
            isRenderingPage = true
            val bmp = renderPdfPage(context, book.filePath, currentPage)
            pageBitmaps[currentPage] = bmp
            isRenderingPage = false
        }
    }

    val currentPageParas = remember(paragraphs, currentPage) {
        paragraphs.filter { it.pageNumber == currentPage }
    }

    val isCurrentPageBookmarked = bookmarkedPages.contains(currentPage)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Minimalist Futuristic Top Bar
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pág. $currentPage de $totalPages",
                                color = ElectricCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (book.isOcrProcessed) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(ElectricCyanSubtle)
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "OCR ACTIVO",
                                        color = ElectricCyan,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("pdf_reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Search toggle
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Buscar texto",
                            tint = if (showSearch) ElectricCyan else TextSecondary
                        )
                    }

                    // Bookmark toggle
                    IconButton(
                        onClick = { onToggleBookmark(currentPage) },
                        modifier = Modifier.testTag("pdf_toggle_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (isCurrentPageBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Marcador",
                            tint = if (isCurrentPageBookmarked) ElectricCyan else TextSecondary
                        )
                    }

                    // View Bookmarks
                    IconButton(onClick = { showBookmarksSheet = true }) {
                        Box(contentAlignment = Alignment.TopEnd) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Ver marcadores",
                                tint = TextSecondary
                            )
                            if (bookmarkedPages.isNotEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(ElectricCyan)
                                )
                            }
                        }
                    }

                    // Advanced OCR Dialog
                    IconButton(
                        onClick = { showOcrDialog = true },
                        modifier = Modifier.testTag("pdf_advanced_ocr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "OCR Avanzado",
                            tint = ElectricCyan
                        )
                    }

                    // Review OCR Text
                    IconButton(onClick = onReviewClick) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Revisar texto",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )

            // Search Bar (Discreet Expandable)
            AnimatedVisibility(visible = showSearch) {
                Surface(
                    color = DarkSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Buscar en este documento...", color = TextTertiary, fontSize = 13.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = ElectricCyan
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = {
                            searchQuery = ""
                            showSearch = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                        }
                    }
                }
            }

            // Search Results Dropdown/Sheet
            if (showSearch && searchQuery.isNotBlank()) {
                val matches = remember(searchQuery, paragraphs) {
                    paragraphs.filter { it.text.contains(searchQuery, ignoreCase = true) }
                }

                Surface(
                    color = DarkSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .border(1.dp, DarkBorder)
                ) {
                    if (matches.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No se encontraron coincidencias.", color = TextSecondary, fontSize = 13.sp)
                        }
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            item {
                                Text(
                                    text = "${matches.size} resultados encontrados:",
                                    color = ElectricCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(12.dp, 8.dp)
                                )
                            }
                            items(matches) { match ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            setPage(match.pageNumber)
                                            showSearch = false
                                        }
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(ElectricCyanSubtle)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("Pág. ${match.pageNumber}", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = match.text,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Mode Selector & Discreet Zoom Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurfaceVariant)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // View Mode Toggle (Original vs OCR Text)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurface)
                        .border(1.dp, DarkBorderSubtle, RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (displayMode == ReaderDisplayMode.ORIGINAL_PAGE) ElectricCyan else Color.Transparent)
                            .clickable { displayMode = ReaderDisplayMode.ORIGINAL_PAGE }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Página Original",
                            color = if (displayMode == ReaderDisplayMode.ORIGINAL_PAGE) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (displayMode == ReaderDisplayMode.OCR_TEXT) ElectricCyan else Color.Transparent)
                            .clickable { displayMode = ReaderDisplayMode.OCR_TEXT }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Texto OCR",
                            color = if (displayMode == ReaderDisplayMode.OCR_TEXT) Color.Black else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Discreet Zoom Controls (active in Original Page mode)
                if (displayMode == ReaderDisplayMode.ORIGINAL_PAGE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                zoomScale = (zoomScale - 0.25f).coerceAtLeast(0.75f)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomOut, contentDescription = "Alejar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }

                        Text(
                            text = "${(zoomScale * 100).roundToInt()}%",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    zoomScale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                                .padding(horizontal = 4.dp)
                        )

                        IconButton(
                            onClick = {
                                zoomScale = (zoomScale + 0.25f).coerceAtMost(3.0f)
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.ZoomIn, contentDescription = "Acercar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }

                        if (zoomScale != 1f || offsetX != 0f || offsetY != 0f) {
                            IconButton(
                                onClick = {
                                    zoomScale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.RestartAlt, contentDescription = "Reset zoom", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Main Reader Content Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                when (displayMode) {
                    ReaderDisplayMode.ORIGINAL_PAGE -> {
                        val currentBmp = pageBitmaps[currentPage]
                        if (isRenderingPage && currentBmp == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("Cargando página original...", color = TextSecondary, fontSize = 12.sp)
                            }
                        } else if (currentBmp != null) {
                            // Interactive Zoom & Pan Container
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTransformGestures { _, pan, zoom, _ ->
                                            zoomScale = (zoomScale * zoom).coerceIn(0.75f, 3.5f)
                                            if (zoomScale > 1f) {
                                                offsetX += pan.x
                                                offsetY += pan.y
                                            } else {
                                                offsetX = 0f
                                                offsetY = 0f
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = currentBmp.asImageBitmap(),
                                    contentDescription = "Página $currentPage de ${book.title}",
                                    modifier = Modifier
                                        .graphicsLayer(
                                            scaleX = zoomScale,
                                            scaleY = zoomScale,
                                            translationX = offsetX,
                                            translationY = offsetY
                                        )
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(1.dp, DarkBorderSubtle, RoundedCornerShape(8.dp))
                                )
                            }
                        } else {
                            // Digital Book Fallback Layout for sample books or non-PDF files
                            DigitalPageFallbackView(
                                book = book,
                                currentPage = currentPage,
                                totalPages = totalPages,
                                paragraphs = currentPageParas
                            )
                        }
                    }

                    ReaderDisplayMode.OCR_TEXT -> {
                        // Transcribed OCR Text with clean readability
                        if (currentPageParas.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    text = "Sin texto reconocido para la página $currentPage",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        onReprocessPageWithOcr(currentPage, selectedOcrLanguage, selectedPreprocessingMode)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                                ) {
                                    Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color.Black)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ejecutar OCR ahora", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Transcripción OCR • Página $currentPage",
                                            color = ElectricCyan,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${currentPageParas.size} párrafos",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                items(currentPageParas) { p ->
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = DarkSurface,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorderSubtle),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = p.chapterTitle,
                                                    color = TextTertiary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                if (p.isLowConfidence) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(3.dp))
                                                            .background(Color(0xFF3B1D0E))
                                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                                    ) {
                                                        Text("Revisión sugerida", color = Color(0xFFF59E0B), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = p.text,
                                                color = TextPrimary,
                                                fontSize = 15.sp,
                                                lineHeight = 22.sp,
                                                fontFamily = FontFamily.SansSerif
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Overlay Loading Indicator during OCR reprocessing
                if (isReprocessingPage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = DarkSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Re-ejecutando OCR adaptativo en Pág. $currentPage...",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Filtro: ${selectedPreprocessingMode.displayName} • ${selectedOcrLanguage.displayName}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation & TTS Control Bar (Futurista Minimalista)
            Surface(
                color = DarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .border(androidx.compose.foundation.BorderStroke(0.5.dp, DarkBorderSubtle))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Page Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$currentPage",
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showJumpToPageDialog = true }
                                .padding(end = 8.dp)
                        )

                        Slider(
                            value = currentPage.toFloat(),
                            onValueChange = { setPage(it.roundToInt()) },
                            valueRange = 1f..totalPages.toFloat(),
                            steps = (totalPages - 2).coerceAtLeast(0),
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan,
                                inactiveTrackColor = DarkSurfaceVariant
                            )
                        )

                        Text(
                            text = "$totalPages",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Navigation Controls & TTS Action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Page Button
                        IconButton(
                            onClick = { setPage(currentPage - 1) },
                            enabled = currentPage > 1,
                            modifier = Modifier.testTag("pdf_prev_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Página anterior",
                                tint = if (currentPage > 1) TextPrimary else TextTertiary
                            )
                        }

                        // Page Jump Button
                        TextButton(onClick = { showJumpToPageDialog = true }) {
                            Text(
                                text = "Página $currentPage de $totalPages",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Next Page Button
                        IconButton(
                            onClick = { setPage(currentPage + 1) },
                            enabled = currentPage < totalPages,
                            modifier = Modifier.testTag("pdf_next_page_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Página siguiente",
                                tint = if (currentPage < totalPages) TextPrimary else TextTertiary
                            )
                        }

                        // Listen Page with TTS (Luminous Cyan Action)
                        Button(
                            onClick = {
                                if (isPlaying) {
                                    onTogglePlay()
                                } else {
                                    onPlayPageText(currentPage)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan
                            ),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("pdf_listen_page_button")
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Escuchar página",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isPlaying) "Pausar" else "Escuchar",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Jump to Page Dialog
        if (showJumpToPageDialog) {
            var jumpPageText by remember { mutableStateOf("$currentPage") }
            AlertDialog(
                onDismissRequest = { showJumpToPageDialog = false },
                containerColor = DarkSurface,
                title = { Text("Ir a la página", color = TextPrimary, fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("Introduce el número de página (1 - $totalPages):", color = TextSecondary, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = jumpPageText,
                            onValueChange = { jumpPageText = it.filter { char -> char.isDigit() } },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = DarkBorder,
                                focusedTextColor = TextPrimary,
                                cursorColor = ElectricCyan
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val target = jumpPageText.toIntOrNull()
                            if (target != null) {
                                setPage(target)
                            }
                            showJumpToPageDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Text("Ir", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpToPageDialog = false }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            )
        }

        // Bookmarks Drawer/Modal Sheet
        if (showBookmarksSheet) {
            AlertDialog(
                onDismissRequest = { showBookmarksSheet = false },
                containerColor = DarkSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Bookmark, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Marcadores guardados", color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    if (bookmarkedPages.isEmpty()) {
                        Text("No tienes marcadores en este documento. Pulsa el icono de marcador para guardar páginas clave.", color = TextSecondary)
                    } else {
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            val sorted = bookmarkedPages.sorted()
                            items(sorted) { pNum ->
                                val samplePara = paragraphs.firstOrNull { it.pageNumber == pNum }?.text ?: "Página $pNum"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            setPage(pNum)
                                            showBookmarksSheet = false
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(ElectricCyanSubtle)
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("Pág. $pNum", color = ElectricCyan, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = samplePara,
                                        color = TextPrimary,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { onToggleBookmark(pNum) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Eliminar", tint = TextTertiary, modifier = Modifier.size(16.dp))
                                    }
                                }
                                HorizontalDivider(color = DarkBorderSubtle, thickness = 0.5.dp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showBookmarksSheet = false }) {
                        Text("Cerrar", color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // Advanced OCR Options Dialog
        if (showOcrDialog) {
            AlertDialog(
                onDismissRequest = { showOcrDialog = false },
                containerColor = DarkSurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = ElectricCyan)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("OCR Avanzado para Pág. $currentPage", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Ajusta el filtro óptico y el idioma para reconocer tipografías difíciles o libros antiguos:",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Language selector
                        Text("Idioma OCR:", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(OcrLanguage.SPANISH, OcrLanguage.LATIN, OcrLanguage.GERMAN, OcrLanguage.ENGLISH).forEach { lang ->
                                val selected = lang == selectedOcrLanguage
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) ElectricCyan else DarkSurfaceVariant)
                                        .border(1.dp, if (selected) ElectricCyan else DarkBorder, RoundedCornerShape(6.dp))
                                        .clickable { onOcrLanguageChange(lang) }
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = lang.displayName.take(7),
                                        color = if (selected) Color.Black else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Filter selector
                        Text("Preprocesamiento Óptico:", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            PreprocessingMode.entries.forEach { mode ->
                                val selected = mode == selectedPreprocessingMode
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) ElectricCyanSubtle else DarkSurfaceVariant)
                                        .border(1.dp, if (selected) ElectricCyan else DarkBorderSubtle, RoundedCornerShape(6.dp))
                                        .clickable { onPreprocessingModeChange(mode) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = mode.displayName,
                                        color = if (selected) ElectricCyan else TextPrimary,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showOcrDialog = false
                            onReprocessPageWithOcr(currentPage, selectedOcrLanguage, selectedPreprocessingMode)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Text("Ejecutar en Pág. $currentPage", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showOcrDialog = false }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            )
        }
    }
}

/**
 * Editorial fallback layout for sample books or non-PDF items
 */
@Composable
private fun DigitalPageFallbackView(
    book: Book,
    currentPage: Int,
    totalPages: Int,
    paragraphs: List<BookParagraphEntity>
) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = book.title,
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Página $currentPage / $totalPages",
                    color = TextTertiary,
                    fontSize = 11.sp
                )
            }
            HorizontalDivider(color = DarkBorderSubtle, modifier = Modifier.padding(vertical = 12.dp))

            if (paragraphs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Capítulo o página sin texto adicional.",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                paragraphs.forEach { para ->
                    Text(
                        text = para.chapterTitle,
                        color = ElectricCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = para.text,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(bottom = 14.dp)
                    )
                }
            }
        }
    }
}

/**
 * Safe helper to render a high-resolution Bitmap from a PDF file using PdfRenderer
 */
private suspend fun renderPdfPage(
    context: Context,
    filePath: String,
    pageNumber: Int
): Bitmap? = withContext(Dispatchers.IO) {
    if (filePath.isBlank() || filePath.startsWith("sample://")) {
        return@withContext null
    }

    try {
        val fileDescriptor: ParcelFileDescriptor? = if (filePath.startsWith("content://")) {
            val uri = Uri.parse(filePath)
            context.contentResolver.openFileDescriptor(uri, "r")
        } else {
            val file = File(filePath)
            if (file.exists() && file.canRead()) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                null
            }
        }

        if (fileDescriptor == null) return@withContext null

        val pdfRenderer = PdfRenderer(fileDescriptor)
        try {
            val pageIndex = (pageNumber - 1).coerceIn(0, pdfRenderer.pageCount - 1)
            val page = pdfRenderer.openPage(pageIndex)
            try {
                // High resolution scaling (width 1200 - 1600px)
                val targetWidth = 1400
                val scale = targetWidth.toFloat() / page.width.toFloat()
                val targetHeight = (page.height * scale).roundToInt().coerceAtLeast(100)

                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                // Fill white background for PDF page rendering
                bitmap.eraseColor(android.graphics.Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            } finally {
                page.close()
            }
        } finally {
            pdfRenderer.close()
            fileDescriptor.close()
        }
    } catch (e: Exception) {
        null
    }
}
