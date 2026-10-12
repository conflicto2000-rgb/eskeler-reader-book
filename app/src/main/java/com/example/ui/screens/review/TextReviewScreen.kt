package com.example.ui.screens.review

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity
import com.example.data.pdf.OcrEngineStatus
import com.example.data.pdf.OcrLanguage
import com.example.data.pdf.PreprocessingMode
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextReviewScreen(
    book: Book,
    paragraphs: List<BookParagraphEntity>,
    isReprocessingPage: Boolean,
    selectedOcrLanguage: OcrLanguage,
    selectedPreprocessingMode: PreprocessingMode,
    ocrEngineStatus: OcrEngineStatus?,
    onOcrLanguageChange: (OcrLanguage) -> Unit,
    onPreprocessingModeChange: (PreprocessingMode) -> Unit,
    onNormalizeArchaicClick: () -> Unit,
    onBackClick: () -> Unit,
    onStartListening: (Int) -> Unit,
    onSaveParagraphEdit: (paragraphIndex: Int, newText: String) -> Unit,
    onRerunOcrForPage: (pageNumber: Int, language: OcrLanguage, mode: PreprocessingMode) -> Unit
) {
    val context = LocalContext.current
    var editingParagraph by remember { mutableStateOf<BookParagraphEntity?>(null) }
    var editTextValue by remember { mutableStateOf("") }
    var previewImagePage by remember { mutableStateOf<Int?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var langMenuExpanded by remember { mutableStateOf(false) }
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val pages = remember(paragraphs) {
        paragraphs.map { it.pageNumber }.distinct().sorted()
    }
    var selectedPage by remember { mutableStateOf(pages.firstOrNull() ?: 1) }

    val pageParagraphs = remember(paragraphs, selectedPage) {
        paragraphs.filter { it.pageNumber == selectedPage }
    }

    LaunchedEffect(previewImagePage) {
        val pageNum = previewImagePage
        if (pageNum != null) {
            previewBitmap = renderPdfPage(context, book.filePath, pageNum)
        } else {
            previewBitmap = null
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Revisión OCR: ${book.title}",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Verifica y edita el texto antes de narrar",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = TextPrimary)
                }
            },
            actions = {
                IconButton(
                    onClick = onNormalizeArchaicClick,
                    modifier = Modifier.testTag("normalize_archaic_button")
                ) {
                    Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = "Normalizar ortografía clásica", tint = ElectricCyan)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        // Banner controls
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Página actual: $selectedPage", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)

                    Row {
                        OutlinedButton(
                            onClick = { previewImagePage = selectedPage },
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(14.dp), tint = ElectricCyan)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ver imagen", fontSize = 12.sp, color = ElectricCyan)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onRerunOcrForPage(selectedPage, selectedOcrLanguage, selectedPreprocessingMode) },
                            enabled = !isReprocessingPage,
                            modifier = Modifier.height(34.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                        ) {
                            if (isReprocessingPage) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.Black, strokeWidth = 2.dp)
                            } else {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Black)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Re-OCR", fontSize = 12.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box {
                        Text(
                            text = "Idioma: ${selectedOcrLanguage.displayName}",
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { langMenuExpanded = true }
                        )
                        DropdownMenu(expanded = langMenuExpanded, onDismissRequest = { langMenuExpanded = false }) {
                            OcrLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = { Text(lang.displayName) },
                                    onClick = {
                                        onOcrLanguageChange(lang)
                                        langMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Box {
                        Text(
                            text = "Filtro: ${selectedPreprocessingMode.displayName}",
                            color = ElectricCyan,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { filterMenuExpanded = true }
                        )
                        DropdownMenu(expanded = filterMenuExpanded, onDismissRequest = { filterMenuExpanded = false }) {
                            PreprocessingMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.displayName) },
                                    onClick = {
                                        onPreprocessingModeChange(mode)
                                        filterMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Paragraphs List
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        ) {
            itemsIndexed(pageParagraphs) { _, para ->
                ParagraphReviewCard(
                    paragraph = para,
                    onEditClick = {
                        editingParagraph = para
                        editTextValue = para.text
                    },
                    onListenFromHere = { onStartListening(para.paragraphIndex) }
                )
            }
        }

        // Bottom CTA: Start TTS Narration
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan)
        ) {
            Button(
                onClick = { onStartListening(0) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
            ) {
                Icon(imageVector = Icons.Default.Headphones, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Escuchar libro completo en el reproductor", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }

    // Edit paragraph modal dialog
    editingParagraph?.let { targetPara ->
        AlertDialog(
            onDismissRequest = { editingParagraph = null },
            title = { Text("Editar texto reconocido", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editTextValue,
                    onValueChange = { editTextValue = it },
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = DarkBorder
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveParagraphEdit(targetPara.paragraphIndex, editTextValue)
                        editingParagraph = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingParagraph = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }

    // Image preview modal dialog
    if (previewImagePage != null) {
        AlertDialog(
            onDismissRequest = { previewImagePage = null },
            title = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Página original $previewImagePage", color = TextPrimary, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { previewImagePage = null }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = TextSecondary)
                    }
                }
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth().height(360.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    previewBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Página original",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } ?: CircularProgressIndicator(color = ElectricCyan)
                }
            },
            confirmButton = {
                Button(
                    onClick = { previewImagePage = null },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Cerrar", color = Color.Black)
                }
            }
        )
    }
}

@Composable
fun ParagraphReviewCard(
    paragraph: BookParagraphEntity,
    onEditClick: () -> Unit,
    onListenFromHere: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (paragraph.isLowConfidence) Color(0xFFF59E0B) else DarkBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Párrafo #${paragraph.paragraphIndex + 1}",
                        color = ElectricCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (paragraph.isLowConfidence) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.Warning, contentDescription = "Baja confianza", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    }
                    if (paragraph.isUserEdited) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("(editado)", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                Row {
                    IconButton(onClick = onListenFromHere, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Headphones, contentDescription = "Escuchar", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onEditClick, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Highlight words with potential recognition artifacts
            val annotatedText = buildAnnotatedString {
                val words = paragraph.text.split(" ")
                words.forEachIndexed { idx, word ->
                    val clean = word.trim { !it.isLetterOrDigit() }
                    val suspicious = clean.length > 2 && !clean.any { it in "aeiouáéíóúAEIOUÁÉÍÓÚ" } && clean.all { it.isLetter() }
                    if (suspicious) {
                        withStyle(SpanStyle(color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)) {
                            append(word)
                        }
                    } else {
                        append(word)
                    }
                    if (idx < words.size - 1) append(" ")
                }
            }

            Text(
                text = annotatedText,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
        }
    }
}

private suspend fun renderPdfPage(context: Context, filePath: String, pageNumber: Int): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val file = File(filePath)
        if (!file.exists()) return@withContext null
        val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(fd)
        val pageIdx = (pageNumber - 1).coerceIn(0, renderer.pageCount - 1)
        val page = renderer.openPage(pageIdx)
        val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        fd.close()
        bmp
    } catch (_: Exception) {
        null
    }
}
