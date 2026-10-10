package com.example.ui.screens.review

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity
import com.example.data.pdf.HistoricalOrthographyNormalizer
import com.example.data.pdf.ImagePreprocessor
import com.example.data.pdf.OcrEngineStatus
import com.example.data.pdf.OcrLanguage
import com.example.data.pdf.PreprocessingMode
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextReviewScreen(
    book: Book,
    paragraphs: List<BookParagraphEntity>,
    isReprocessingPage: Boolean,
    selectedOcrLanguage: OcrLanguage,
    selectedPreprocessingMode: PreprocessingMode = PreprocessingMode.VINTAGE_BOOK,
    ocrEngineStatus: OcrEngineStatus?,
    onOcrLanguageChange: (OcrLanguage) -> Unit,
    onPreprocessingModeChange: (PreprocessingMode) -> Unit = {},
    onNormalizeArchaicClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onStartListening: (startParagraphIndex: Int) -> Unit,
    onSaveParagraphEdit: (paragraphIndex: Int, newText: String) -> Unit,
    onRerunOcrForPage: (pageNumber: Int, language: OcrLanguage, mode: PreprocessingMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPage by remember { mutableIntStateOf(1) }
    var showOnlySuspicious by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    // Dialog editing states
    var editingParagraph by remember { mutableStateOf<BookParagraphEntity?>(null) }
    var editedTextValue by remember { mutableStateOf("") }

    // Compare original dialog
    var comparingParagraph by remember { mutableStateOf<BookParagraphEntity?>(null) }

    // Split-view modal: Original Page Scan vs OCR Text
    var showPageScanComparison by remember { mutableStateOf(false) }
    var renderedOriginalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var renderedProcessedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isRenderingPreview by remember { mutableStateOf(false) }

    val totalPages = book.totalPages.coerceAtLeast(1)
    val pageParagraphs = paragraphs.filter { it.pageNumber == selectedPage }

    // Find pages that have low confidence or are marked as suspicious
    val suspiciousPages = remember(paragraphs) {
        paragraphs.filter { it.isLowConfidence || it.qualityScore < 0.65f }
            .map { it.pageNumber }
            .distinct()
            .toSet()
    }

    // Effect to render original page bitmap when comparing image
    LaunchedEffect(showPageScanComparison, selectedPage, selectedPreprocessingMode) {
        if (showPageScanComparison) {
            isRenderingPreview = true
            withContext(Dispatchers.IO) {
                try {
                    val file = File(book.filePath)
                    if (file.exists()) {
                        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        val pageIdx = (selectedPage - 1).coerceIn(0, renderer.pageCount - 1)
                        val page = renderer.openPage(pageIdx)

                        val width = (page.width * 1.5f).toInt().coerceIn(800, 1400)
                        val height = (page.height * (width.toFloat() / page.width)).toInt()

                        val rawBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        page.render(rawBmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        page.close()
                        renderer.close()
                        pfd.close()

                        val processedBmp = ImagePreprocessor.preprocess(rawBmp, selectedPreprocessingMode)

                        withContext(Dispatchers.Main) {
                            renderedOriginalBitmap = rawBmp
                            renderedProcessedBitmap = processedBmp
                            isRenderingPreview = false
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isRenderingPreview = false
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isRenderingPreview = false
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("text_review_screen"),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Revisión & Restauración OCR",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${book.title} • ${book.totalPages} págs",
                            color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("review_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                    }
                },
                actions = {
                    Button(
                        onClick = { onStartListening(0) },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("start_listening_btn")
                    ) {
                        Icon(Icons.Default.Headphones, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Escuchar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Offline Engine Status & Historical Typography Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val hasIssues = suspiciousPages.isNotEmpty()
                            Icon(
                                imageVector = if (hasIssues) Icons.Default.Warning else Icons.Default.Check,
                                contentDescription = null,
                                tint = if (hasIssues) AmberWarning else SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasIssues) "${suspiciousPages.size} págs con baja confianza detectadas" else "Texto verificado y listo para narrar",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (suspiciousPages.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (showOnlySuspicious) AmberWarning else DarkSurface)
                                    .clickable { showOnlySuspicious = !showOnlySuspicious }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (showOnlySuspicious) "Ver todas" else "Filtrar dudosas",
                                    color = if (showOnlySuspicious) Color.Black else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Motor OCR Adaptativo • Soporte Fraktur, libros antiguos y s-larga (ſ)",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Page Selector Tabs
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val availablePages = if (showOnlySuspicious && suspiciousPages.isNotEmpty()) {
                    suspiciousPages.sorted()
                } else {
                    (1..totalPages).toList()
                }

                items(availablePages) { pageNum ->
                    val isSelected = selectedPage == pageNum
                    val isSuspicious = suspiciousPages.contains(pageNum)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when {
                                    isSelected -> ElectricCyan
                                    isSuspicious -> AmberWarning.copy(alpha = 0.2f)
                                    else -> DarkSurfaceVariant
                                }
                            )
                            .border(
                                width = if (isSuspicious && !isSelected) 1.dp else 0.dp,
                                color = if (isSuspicious && !isSelected) AmberWarning else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedPage = pageNum }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                            .testTag("page_tab_$pageNum"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Pág $pageNum",
                                color = if (isSelected) Color.Black else if (isSuspicious) AmberWarning else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.sp
                            )
                            if (isSuspicious && !isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(AmberWarning)
                                )
                            }
                        }
                    }
                }
            }

            // Advanced Controls Toolbar: Language, Vintage Preprocessing Filter, Compare Scan, Normalize
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Language Dropdown Selector
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { showLanguageMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Language, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(selectedOcrLanguage.displayName.take(12), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    DropdownMenu(
                        expanded = showLanguageMenu,
                        onDismissRequest = { showLanguageMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        OcrLanguage.values().forEach { lang ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            lang.displayName,
                                            color = if (selectedOcrLanguage == lang) ElectricCyan else TextPrimary,
                                            fontWeight = if (selectedOcrLanguage == lang) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            lang.historicalSupport,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                onClick = {
                                    onOcrLanguageChange(lang)
                                    showLanguageMenu = false
                                }
                            )
                        }
                    }
                }

                // Preprocessing Filter Dropdown
                Box {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { showFilterMenu = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FilterAlt, contentDescription = null, tint = AmberWarning, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(selectedPreprocessingMode.displayName.take(14), color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        PreprocessingMode.values().forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(
                                            mode.displayName,
                                            color = if (selectedPreprocessingMode == mode) AmberWarning else TextPrimary,
                                            fontWeight = if (selectedPreprocessingMode == mode) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            mode.description,
                                            color = TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                },
                                onClick = {
                                    onPreprocessingModeChange(mode)
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }

                // View Original Scan Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .clickable { showPageScanComparison = true }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver escaneo", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Re-run OCR button
                IconButton(
                    onClick = { onRerunOcrForPage(selectedPage, selectedOcrLanguage, selectedPreprocessingMode) },
                    enabled = !isReprocessingPage,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                        .testTag("rerun_ocr_btn")
                ) {
                    if (isReprocessingPage) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = ElectricCyan)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Re-escanear", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Normalization Action Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tipografía: ${selectedPreprocessingMode.displayName}",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                TextButton(
                    onClick = onNormalizeArchaicClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Suavizar ortografía clásica para voz", color = ElectricPurple, fontSize = 11.sp)
                }
            }

            // Paragraphs List
            if (pageParagraphs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No hay párrafos reconocidos para la página $selectedPage",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onRerunOcrForPage(selectedPage, selectedOcrLanguage, selectedPreprocessingMode) },
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Reconocer página con OCR", color = Color.Black)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    itemsIndexed(pageParagraphs, key = { _, p -> p.paragraphIndex }) { _, para ->
                        ParagraphReviewCard(
                            paragraph = para,
                            onEditClick = {
                                editingParagraph = para
                                editedTextValue = para.text
                            },
                            onCompareClick = {
                                comparingParagraph = para
                            },
                            onPlayFromHere = {
                                onStartListening(para.paragraphIndex)
                            }
                        )
                    }
                }
            }
        }
    }

    // Modal Edit Dialog
    editingParagraph?.let { para ->
        AlertDialog(
            onDismissRequest = { editingParagraph = null },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Corregir texto del párrafo", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Edita cualquier palabra mal reconocida en la página escaneada. Se guardará de inmediato para el audiolibro.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editedTextValue,
                        onValueChange = { editedTextValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .testTag("edit_paragraph_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveParagraphEdit(para.paragraphIndex, editedTextValue.trim())
                        editingParagraph = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("save_paragraph_btn")
                ) {
                    Text("Guardar corrección", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingParagraph = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Compare Dialog (Original vs Cleaned/Edited)
    comparingParagraph?.let { para ->
        AlertDialog(
            onDismissRequest = { comparingParagraph = null },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CompareArrows, contentDescription = null, tint = ElectricPurple, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Comparar con texto original", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Texto original escaneado / OCR:", color = AmberWarning, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = para.originalText.ifEmpty { "(Sin texto original registrado)" },
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    }

                    Text("Texto actual para la narración:", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(10.dp)
                    ) {
                        Text(
                            text = para.text,
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { comparingParagraph = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Aceptar", color = Color.Black)
                }
            }
        )
    }

    // Modal: Original Scan vs Preprocessed View
    if (showPageScanComparison) {
        AlertDialog(
            onDismissRequest = { showPageScanComparison = false },
            containerColor = DarkSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Compare, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escaneo original vs Filtro", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    IconButton(onClick = { showPageScanComparison = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Página $selectedPage • Comprueba el efecto de ${selectedPreprocessingMode.displayName} sobre la tinta y el fondo amarillento.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (isRenderingPreview) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = ElectricCyan)
                        }
                    } else if (renderedOriginalBitmap != null && renderedProcessedBitmap != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Original", color = AmberWarning, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = renderedOriginalBitmap!!.asImageBitmap(),
                                    contentDescription = "Original scan",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Con Filtro", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Image(
                                    bitmap = renderedProcessedBitmap!!.asImageBitmap(),
                                    contentDescription = "Preprocessed scan",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Black),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No se pudo cargar la imagen del PDF", color = TextSecondary)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPageScanComparison = false
                        onRerunOcrForPage(selectedPage, selectedOcrLanguage, selectedPreprocessingMode)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Re-escanear con este filtro", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPageScanComparison = false }) {
                    Text("Cerrar", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun ParagraphReviewCard(
    paragraph: BookParagraphEntity,
    onEditClick: () -> Unit,
    onCompareClick: () -> Unit,
    onPlayFromHere: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_card_${paragraph.paragraphIndex}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (paragraph.isLowConfidence) Color(0xFF221A15) else DarkSurface
        ),
        border = if (paragraph.isLowConfidence) {
            androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f))
        } else null
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Badges row: Method, Quality score, isEdited
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Method badge
                    val (methodLabel, methodColor) = when (paragraph.extractionMethod) {
                        "OCR" -> "OCR 📷" to ElectricCyan
                        "HYBRID" -> "Híbrido ⚡" to ElectricPurple
                        "MANUAL" -> "Editado ✍️" to AmberWarning
                        else -> "Digital ✓" to SuccessGreen
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(methodColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = methodLabel, color = methodColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Quality Badge
                    val qualityPercent = (paragraph.qualityScore * 100).toInt()
                    val qualColor = if (paragraph.qualityScore >= 0.70f) SuccessGreen else AmberWarning
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(qualColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = "$qualityPercent% calidad", color = qualColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                    }

                    if (paragraph.isLowConfidence) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmberWarning.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "Revisar palabras", color = AmberWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (paragraph.isUserEdited) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmberWarning.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(text = "Modificado", color = AmberWarning, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Play from here button
                IconButton(
                    onClick = onPlayFromHere,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ElectricCyan.copy(alpha = 0.15f))
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Escuchar desde aquí",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Paragraph text with low-confidence / non-dictionary word highlighting
            HighlightedParagraphText(text = paragraph.text, isLowConfidence = paragraph.isLowConfidence)

            Spacer(modifier = Modifier.height(10.dp))

            // Footer buttons: Edit, Compare
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (paragraph.originalText.isNotBlank() && paragraph.originalText != paragraph.text) {
                    TextButton(onClick = onCompareClick) {
                        Icon(Icons.Default.CompareArrows, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ver original", color = TextSecondary, fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Button(
                    onClick = onEditClick,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Editar", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Highlights words that may have low confidence or non-alphabetic noise
 * so the user can easily spot misrecognized characters in antique texts.
 */
@Composable
private fun HighlightedParagraphText(text: String, isLowConfidence: Boolean) {
    if (!isLowConfidence) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 14.sp,
            lineHeight = 21.sp
        )
        return
    }

    val annotated = remember(text) {
        buildAnnotatedString {
            val words = text.split(Regex("(?<=\\s)|(?=\\s)"))
            for (token in words) {
                val clean = token.trim { !it.isLetterOrDigit() }
                // Highlight words with numbers mixed in letters or weird characters
                val isSuspiciousWord = clean.any { it.isDigit() } && clean.any { it.isLetter() } ||
                        clean.count { it == '?' || it == '|' || it == '~' || it == '^' } > 0

                if (isSuspiciousWord && clean.isNotBlank()) {
                    withStyle(SpanStyle(color = AmberWarning, background = AmberWarning.copy(alpha = 0.25f))) {
                        append(token)
                    }
                } else {
                    withStyle(SpanStyle(color = TextPrimary)) {
                        append(token)
                    }
                }
            }
        }
    }

    Text(
        text = annotated,
        fontSize = 14.sp,
        lineHeight = 21.sp
    )
}
