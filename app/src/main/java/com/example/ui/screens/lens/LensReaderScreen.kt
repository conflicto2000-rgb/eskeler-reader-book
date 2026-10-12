package com.example.ui.screens.lens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.pdf.HistoricalOrthographyNormalizer
import com.example.data.pdf.ImagePreprocessor
import com.example.data.pdf.OcrLanguage
import com.example.data.pdf.OcrPageResult
import com.example.data.pdf.OcrProcessor
import com.example.data.pdf.PreprocessingMode
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensReaderScreen(
    selectedLanguage: OcrLanguage,
    selectedMode: PreprocessingMode,
    onLanguageChange: (OcrLanguage) -> Unit,
    onModeChange: (PreprocessingMode) -> Unit,
    onBackClick: () -> Unit,
    onSendToPlayer: (title: String, paragraphs: List<String>, imagePath: String?) -> Unit,
    onSamplePlayText: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentImagePath by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var ocrResult by remember { mutableStateOf<OcrPageResult?>(null) }
    var recognizedParagraphs by remember { mutableStateOf<List<String>>(emptyList()) }
    var documentTitle by remember { mutableStateOf("Captura Lens") }

    var editingIndex by remember { mutableStateOf<Int?>(null) }
    var editParagraphText by remember { mutableStateOf("") }
    var isArchaicNormalized by remember { mutableStateOf(false) }

    var langMenuExpanded by remember { mutableStateOf(false) }

    // Gallery Picker Launcher (Zero-permission Android Photo Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { pickedUri ->
            coroutineScope.launch {
                isProcessing = true
                val loadedBitmap = loadAndScaleBitmapFromUri(context, pickedUri)
                if (loadedBitmap != null) {
                    val savedFile = saveBitmapLocally(context, loadedBitmap)
                    currentImagePath = savedFile.absolutePath
                    currentBitmap = loadedBitmap
                    // Run OCR
                    val result = OcrProcessor.recognizeBitmapAdaptive(
                        bitmap = loadedBitmap,
                        pageNumber = 1,
                        language = selectedLanguage,
                        forcedPreprocessing = selectedMode
                    )
                    ocrResult = result
                    recognizedParagraphs = result.paragraphs
                    documentTitle = result.paragraphs.firstOrNull()?.take(30) ?: "Captura Lens"
                }
                isProcessing = false
            }
        }
    }

    // Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { photo: Bitmap? ->
        photo?.let { takenBitmap ->
            coroutineScope.launch {
                isProcessing = true
                val savedFile = saveBitmapLocally(context, takenBitmap)
                currentImagePath = savedFile.absolutePath
                currentBitmap = takenBitmap
                val result = OcrProcessor.recognizeBitmapAdaptive(
                    bitmap = takenBitmap,
                    pageNumber = 1,
                    language = selectedLanguage,
                    forcedPreprocessing = selectedMode
                )
                ocrResult = result
                recognizedParagraphs = result.paragraphs
                documentTitle = result.paragraphs.firstOrNull()?.take(30) ?: "Captura de Cámara"
                isProcessing = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(DarkBackground)
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = "Lens Reader",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Escaneo OCR de fotos, páginas y libros",
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
                Box {
                    TextButton(onClick = { langMenuExpanded = true }) {
                        Text(selectedLanguage.displayName, color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    DropdownMenu(expanded = langMenuExpanded, onDismissRequest = { langMenuExpanded = false }) {
                        OcrLanguage.values().forEach { lang ->
                            DropdownMenuItem(
                                text = { Text(lang.displayName) },
                                onClick = {
                                    onLanguageChange(lang)
                                    langMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
        )

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
        ) {
            // Source Picker buttons
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Seleccionar imagen para OCR",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("lens_take_photo_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                            ) {
                                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tomar Foto", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    galleryLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("lens_pick_gallery_button"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ElectricCyan)
                            ) {
                                Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Galería", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Image Preview & Filter Bar
            currentBitmap?.let { bmp ->
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = bmp.asImageBitmap(),
                                    contentDescription = "Página capturada",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )

                                IconButton(
                                    onClick = {
                                        val rotated = ImagePreprocessor.rotateBitmap(bmp, 90f)
                                        currentBitmap = rotated
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.6f))
                                ) {
                                    Icon(imageVector = Icons.Default.RotateRight, contentDescription = "Girar 90°", tint = ElectricCyan)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text("Filtro de preprocesamiento:", color = TextSecondary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                PreprocessingMode.values().forEach { mode ->
                                    FilterChip(
                                        selected = selectedMode == mode,
                                        onClick = { onModeChange(mode) },
                                        label = { Text(mode.displayName, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = ElectricCyan,
                                            selectedLabelColor = Color.Black,
                                            containerColor = Color(0xFF1E293B),
                                            labelColor = TextSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        isProcessing = true
                                        val result = OcrProcessor.recognizeBitmapAdaptive(
                                            bitmap = bmp,
                                            pageNumber = 1,
                                            language = selectedLanguage,
                                            forcedPreprocessing = selectedMode
                                        )
                                        ocrResult = result
                                        recognizedParagraphs = result.paragraphs
                                        isProcessing = false
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp).testTag("lens_reprocess_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Re-procesar imagen con este filtro", color = ElectricCyan, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Processing indicator
            if (isProcessing) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = ElectricCyan)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Reconociendo texto con OCR...", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Results Header & Diagnostic Quality
            if (!isProcessing && recognizedParagraphs.isNotEmpty()) {
                item {
                    val qScore = ocrResult?.qualityScore ?: 1.0f
                    val qualityPercent = (qScore * 100).toInt()

                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (qualityPercent < 65) Color(0xFFF59E0B) else Color(0xFF10B981))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Calidad lingüística: $qualityPercent%",
                                    color = if (qualityPercent < 65) Color(0xFFF59E0B) else Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${recognizedParagraphs.size} párrafos detectados • Filtro: ${ocrResult?.preprocessingUsed?.displayName ?: "Estándar"}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    isArchaicNormalized = !isArchaicNormalized
                                    recognizedParagraphs = recognizedParagraphs.map { p ->
                                        if (isArchaicNormalized) {
                                            HistoricalOrthographyNormalizer.normalizeForNarration(p, selectedLanguage)
                                        } else {
                                            p
                                        }
                                    }
                                },
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(imageVector = Icons.Default.AutoFixHigh, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArchaicNormalized) "Ortografía normalizada" else "Normalizar clásica", fontSize = 11.sp, color = ElectricCyan)
                            }
                        }
                    }
                }
            }

            // Paragraph Cards
            if (!isProcessing) {
                itemsIndexed(recognizedParagraphs) { index, paraText ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Párrafo #${index + 1}",
                                    color = ElectricCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row {
                                    IconButton(
                                        onClick = { onSamplePlayText(paraText) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Headphones, contentDescription = "Escuchar párrafo", tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = {
                                            editingIndex = index
                                            editParagraphText = paraText
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar párrafo", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Highlight suspicious or low-confidence words
                            val annotated = buildAnnotatedString {
                                val tokens = paraText.split(" ")
                                tokens.forEachIndexed { tIdx, token ->
                                    val clean = token.trim { !it.isLetterOrDigit() }
                                    val isSuspicious = clean.length > 2 && !clean.any { it in "aeiouáéíóúAEIOUÁÉÍÓÚ" } && clean.all { it.isLetter() }
                                    if (isSuspicious) {
                                        withStyle(SpanStyle(color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)) {
                                            append(token)
                                        }
                                    } else {
                                        append(token)
                                    }
                                    if (tIdx < tokens.size - 1) append(" ")
                                }
                            }

                            Text(text = annotated, color = TextPrimary, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
        }

        // Send to TTS Player Action Bar
        if (recognizedParagraphs.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan)
            ) {
                Button(
                    onClick = {
                        onSendToPlayer(documentTitle, recognizedParagraphs, currentImagePath)
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("lens_send_to_player_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Icon(imageVector = Icons.Default.Headphones, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar al Reproductor de Audiolibro", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }

    // Edit Paragraph Dialog
    editingIndex?.let { editIdx ->
        AlertDialog(
            onDismissRequest = { editingIndex = null },
            title = { Text("Editar texto reconocido", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editParagraphText,
                    onValueChange = { editParagraphText = it },
                    modifier = Modifier.fillMaxWidth().height(160.dp),
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
                        val updated = recognizedParagraphs.toMutableList()
                        if (editIdx in updated.indices) {
                            updated[editIdx] = editParagraphText
                            recognizedParagraphs = updated
                        }
                        editingIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                ) {
                    Text("Guardar", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingIndex = null }) {
                    Text("Cancelar", color = TextSecondary)
                }
            }
        )
    }
}

private suspend fun loadAndScaleBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
    try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)

            val maxDimension = maxOf(options.outWidth, options.outHeight)
            var sampleSize = 1
            if (maxDimension > 2200) {
                sampleSize = maxDimension / 2000
            }

            context.contentResolver.openInputStream(uri)?.use { stream2 ->
                val realOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
                BitmapFactory.decodeStream(stream2, null, realOptions)
            }
        }
    } catch (_: Exception) {
        null
    }
}

private suspend fun saveBitmapLocally(context: android.content.Context, bitmap: Bitmap): File = withContext(Dispatchers.IO) {
    val dir = File(context.filesDir, "lens_captures")
    if (!dir.exists()) dir.mkdirs()
    val file = File(dir, "lens_${System.currentTimeMillis()}.jpg")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    file
}
