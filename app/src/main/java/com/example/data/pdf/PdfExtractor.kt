package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.model.BookParagraphEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import java.util.zip.InflaterInputStream

data class ExtractedBookResult(
    val title: String,
    val author: String,
    val coverPath: String?,
    val totalPages: Int,
    val paragraphs: List<BookParagraphEntity>,
    val isOcrProcessed: Boolean
)

object PdfExtractor {
    private const val TAG = "PdfExtractor"

    suspend fun processPdf(
        context: Context,
        pdfUri: Uri,
        originalFileName: String,
        onProgress: (status: String, progress: Float) -> Unit
    ): ExtractedBookResult = withContext(Dispatchers.IO) {
        onProgress("Preparando documento...", 0.05f)

        // 1. Copy uri to local app internal storage file
        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val sanitizedName = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val destinationFile = File(booksDir, "${System.currentTimeMillis()}_$sanitizedName")

        context.contentResolver.openInputStream(pdfUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("No se pudo leer el archivo seleccionado")

        onProgress("Analizando páginas del PDF...", 0.15f)

        // 2. Open via PdfRenderer
        val pfd = ParcelFileDescriptor.open(destinationFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val totalPages = renderer.pageCount

        if (totalPages == 0) {
            renderer.close()
            pfd.close()
            throw IllegalStateException("El documento PDF no contiene páginas")
        }

        // 3. Extract cover thumbnail from page 0
        var coverPath: String? = null
        try {
            val firstPage = renderer.openPage(0)
            val renderWidth = (firstPage.width.coerceAtMost(1080) * 1.2).toInt().coerceAtLeast(300)
            val renderHeight = (firstPage.height * (renderWidth.toFloat() / firstPage.width)).toInt()
            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            firstPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            firstPage.close()

            val coverFile = File(coversDir, "cover_${System.currentTimeMillis()}.png")
            FileOutputStream(coverFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
            }
            bitmap.recycle()
            coverPath = coverFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error generating cover thumbnail: ${e.message}")
        }

        // 4. Extract text from PDF content streams
        onProgress("Extrayendo texto...", 0.3f)
        val rawPageTexts = extractDigitalTextByPages(destinationFile, totalPages)
        val totalDigitalChars = rawPageTexts.sumOf { it.length }

        val paragraphs = mutableListOf<BookParagraphEntity>()
        var usedOcr = false

        val avgCharsPerPage = if (totalPages > 0) totalDigitalChars / totalPages else 0
        if (avgCharsPerPage < 40) {
            // Document appears scanned or image-based, apply OCR via ML Kit
            usedOcr = true
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            var paragraphIndex = 0

            for (pageIdx in 0 until totalPages) {
                val progressVal = 0.3f + (0.6f * (pageIdx.toFloat() / totalPages))
                onProgress("Escaneando con OCR página ${pageIdx + 1} de $totalPages...", progressVal)

                try {
                    val page = renderer.openPage(pageIdx)
                    val ocrWidth = (page.width * 1.5f).toInt().coerceIn(600, 1600)
                    val ocrHeight = (page.height * (ocrWidth.toFloat() / page.width)).toInt()
                    val pageBitmap = Bitmap.createBitmap(ocrWidth, ocrHeight, Bitmap.Config.ARGB_8888)
                    page.render(pageBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val image = InputImage.fromBitmap(pageBitmap, 0)
                    val visionText = recognizer.process(image).await()
                    pageBitmap.recycle()

                    val pageText = visionText.text
                    val splitParas = splitTextIntoParagraphs(pageText)
                    for (para in splitParas) {
                        if (para.isNotBlank()) {
                            paragraphs.add(
                                BookParagraphEntity(
                                    bookId = 0,
                                    paragraphIndex = paragraphIndex++,
                                    pageNumber = pageIdx + 1,
                                    chapterTitle = detectChapter(para, pageIdx + 1),
                                    text = cleanText(para)
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "OCR failed on page $pageIdx: ${e.message}")
                }
            }
        } else {
            // Digital text is available!
            onProgress("Estructurando párrafos y capítulos...", 0.85f)
            var paragraphIndex = 0
            for ((pageIdx, pageText) in rawPageTexts.withIndex()) {
                val splitParas = splitTextIntoParagraphs(pageText)
                for (para in splitParas) {
                    if (para.isNotBlank()) {
                        paragraphs.add(
                            BookParagraphEntity(
                                bookId = 0,
                                paragraphIndex = paragraphIndex++,
                                pageNumber = pageIdx + 1,
                                chapterTitle = detectChapter(para, pageIdx + 1),
                                text = cleanText(para)
                            )
                        )
                    }
                }
            }
        }

        renderer.close()
        pfd.close()

        // Fallback if no text could be extracted
        if (paragraphs.isEmpty()) {
            paragraphs.add(
                BookParagraphEntity(
                    bookId = 0,
                    paragraphIndex = 0,
                    pageNumber = 1,
                    chapterTitle = "Página 1",
                    text = "No se pudo extraer texto legible de este archivo. Asegúrese de que el documento no esté protegido con contraseña."
                )
            )
        }

        onProgress("¡Libro listo para escuchar!", 1.0f)

        // Parse title and author from file name
        val rawBaseName = originalFileName.removeSuffix(".pdf").removeSuffix(".PDF")
        val (title, author) = deriveTitleAndAuthor(rawBaseName, paragraphs.firstOrNull()?.text ?: "")

        ExtractedBookResult(
            title = title,
            author = author,
            coverPath = coverPath,
            totalPages = totalPages,
            paragraphs = paragraphs,
            isOcrProcessed = usedOcr
        )
    }

    private fun extractDigitalTextByPages(pdfFile: File, totalPages: Int): List<String> {
        val result = MutableList(totalPages) { "" }
        try {
            val bytes = pdfFile.readBytes()
            val textBlocks = mutableListOf<String>()

            // Find all stream / endstream segments
            val streamMarker = "stream".toByteArray(Charsets.ISO_8859_1)
            val endStreamMarker = "endstream".toByteArray(Charsets.ISO_8859_1)

            var searchIndex = 0
            while (searchIndex < bytes.size) {
                val start = indexOf(bytes, streamMarker, searchIndex)
                if (start == -1) break

                // Find newline after "stream"
                var contentStart = start + streamMarker.size
                if (contentStart < bytes.size && bytes[contentStart] == '\r'.code.toByte()) contentStart++
                if (contentStart < bytes.size && bytes[contentStart] == '\n'.code.toByte()) contentStart++

                val end = indexOf(bytes, endStreamMarker, contentStart)
                if (end == -1) break

                val streamLength = end - contentStart
                if (streamLength > 0 && streamLength < 10_000_000) {
                    val streamBytes = bytes.copyOfRange(contentStart, end)
                    val decompressed = tryDecompressFlate(streamBytes) ?: streamBytes
                    val extracted = extractStringsFromPdfStream(decompressed)
                    if (extracted.isNotBlank()) {
                        textBlocks.add(extracted)
                    }
                }

                searchIndex = end + endStreamMarker.size
            }

            if (textBlocks.isNotEmpty()) {
                val blocksPerPage = (textBlocks.size / totalPages.coerceAtLeast(1)).coerceAtLeast(1)
                for (i in 0 until totalPages) {
                    val from = i * blocksPerPage
                    val to = ((i + 1) * blocksPerPage).coerceAtMost(textBlocks.size)
                    if (from < textBlocks.size) {
                        result[i] = textBlocks.subList(from, to).joinToString("\n\n")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in extractDigitalTextByPages: ${e.message}")
        }
        return result
    }

    private fun tryDecompressFlate(data: ByteArray): ByteArray? {
        return try {
            val inflater = InflaterInputStream(ByteArrayInputStream(data))
            val bos = ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            var read: Int
            while (inflater.read(buffer).also { read = it } != -1) {
                bos.write(buffer, 0, read)
            }
            bos.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun extractStringsFromPdfStream(streamBytes: ByteArray): String {
        val content = String(streamBytes, Charsets.ISO_8859_1)
        val sb = StringBuilder()

        // Match BT ... ET blocks or string literals ( ... ) Tj / TJ
        val tjPattern = Pattern.compile("\\((.*?)\\)\\s*(?:Tj|'|\")", Pattern.DOTALL)
        val tjMatcher = tjPattern.matcher(content)
        while (tjMatcher.find()) {
            val str = tjMatcher.group(1)?.let { unescapePdfString(it) } ?: ""
            if (str.isNotBlank()) {
                sb.append(str).append(" ")
            }
        }

        // Match array TJ operators: [ (str) num (str) ] TJ
        val arrayTjPattern = Pattern.compile("\\[(.*?)\\]\\s*TJ", Pattern.DOTALL)
        val arrayMatcher = arrayTjPattern.matcher(content)
        while (arrayMatcher.find()) {
            val inner = arrayMatcher.group(1) ?: ""
            val innerP = Pattern.compile("\\((.*?)\\)")
            val innerM = innerP.matcher(inner)
            while (innerM.find()) {
                val s = innerM.group(1)?.let { unescapePdfString(it) } ?: ""
                sb.append(s)
            }
            sb.append(" ")
        }

        return sb.toString().trim()
    }

    private fun unescapePdfString(raw: String): String {
        return raw
            .replace("\\(", "(")
            .replace("\\)", ")")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (fromIndex >= source.size || target.isEmpty()) return -1
        outer@ for (i in fromIndex until (source.size - target.size + 1)) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) {
                    continue@outer
                }
            }
            return i
        }
        return -1
    }

    private fun splitTextIntoParagraphs(text: String): List<String> {
        return text
            .split(Regex("(\r?\n){2,}"))
            .flatMap { it.split("\n") }
            .map { it.trim() }
            .filter { it.length > 5 }
    }

    private fun cleanText(text: String): String {
        return text
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun detectChapter(paragraphText: String, page: Int): String {
        val trimmed = paragraphText.trim()
        val chapterPrefixes = listOf("Capítulo", "Capitulo", "Chapter", "CAPÍTULO", "CAPITULO", "Prólogo", "Epílogo", "Introducción")
        for (prefix in chapterPrefixes) {
            if (trimmed.startsWith(prefix, ignoreCase = true)) {
                return trimmed.take(50)
            }
        }
        return "Página $page"
    }

    private fun deriveTitleAndAuthor(filename: String, firstText: String): Pair<String, String> {
        val cleaned = filename
            .replace("_", " ")
            .replace("-", " ")
            .trim()

        // Check if pattern "Author - Title" or "Title by Author"
        if (cleaned.contains(" by ", ignoreCase = true)) {
            val parts = cleaned.split(Regex(" by ", RegexOption.IGNORE_CASE))
            return parts[0].trim() to parts.getOrNull(1)?.trim().orEmpty().ifEmpty { "Desconocido" }
        }

        val title = cleaned.split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        val author = if (firstText.contains("por ", ignoreCase = true) || firstText.contains("de ", ignoreCase = true)) {
            "Autor del PDF"
        } else {
            "Documento PDF"
        }
        return title to author
    }
}
