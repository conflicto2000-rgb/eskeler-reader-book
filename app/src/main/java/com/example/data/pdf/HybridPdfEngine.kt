package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.model.BookParagraphEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.regex.Pattern
import java.util.zip.InflaterInputStream

data class HybridExtractionResult(
    val title: String,
    val author: String,
    val coverPath: String?,
    val totalPages: Int,
    val paragraphs: List<BookParagraphEntity>,
    val ocrPageCount: Int,
    val digitalPageCount: Int,
    val hasLowConfidencePages: Boolean
)

object HybridPdfEngine {
    private const val TAG = "HybridPdfEngine"

    suspend fun processPdf(
        context: Context,
        pdfUri: Uri,
        originalFileName: String,
        ocrLanguage: OcrLanguage = OcrLanguage.SPANISH,
        onProgress: (status: String, progress: Float) -> Unit
    ): HybridExtractionResult = withContext(Dispatchers.IO) {
        onProgress("Preparando documento...", 0.05f)

        val booksDir = File(context.filesDir, "books").apply { mkdirs() }
        val coversDir = File(context.filesDir, "covers").apply { mkdirs() }
        val sanitizedName = originalFileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val destinationFile = File(booksDir, "${System.currentTimeMillis()}_$sanitizedName")

        context.contentResolver.openInputStream(pdfUri)?.use { input ->
            FileOutputStream(destinationFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("No se pudo leer el archivo seleccionado")

        onProgress("Analizando páginas y estructura...", 0.12f)

        val pfd = ParcelFileDescriptor.open(destinationFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val totalPages = renderer.pageCount

        if (totalPages == 0) {
            renderer.close()
            pfd.close()
            throw IllegalStateException("El documento PDF no contiene páginas válidas")
        }

        // Generate high-resolution cover thumbnail from page 0
        var coverPath: String? = null
        try {
            val firstPage = renderer.openPage(0)
            val renderWidth = (firstPage.width.coerceAtMost(1080) * 1.5).toInt().coerceAtLeast(400)
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
            Log.e(TAG, "Error generando miniatura de portada: ${e.message}")
        }

        // Extract genuine digital text streams (only streams containing BT ... ET blocks)
        onProgress("Verificando capas de texto digital...", 0.20f)
        val digitalPageTexts = extractDigitalTextPages(destinationFile, totalPages)

        val allParagraphs = mutableListOf<BookParagraphEntity>()
        var currentParagraphIndex = 0
        var ocrPages = 0
        var digitalPages = 0
        var hasLowConfidence = false
        var currentChapter = "Capítulo 1"

        // Process sequentially page by page to keep memory footprint minimal
        for (pageIdx in 0 until totalPages) {
            val pageNumber = pageIdx + 1
            val progressFrac = 0.25f + (0.70f * (pageIdx.toFloat() / totalPages))

            val rawDigital = digitalPageTexts.getOrElse(pageIdx) { "" }
            val digitalReport = TextQualityAnalyzer.evaluate(rawDigital)

            // Strict digital verification:
            // Must have high quality score, no corruption, at least 40 genuine chars,
            // and must NOT contain leaked PDF syntax or unpronounceable streams.
            val isGenuineDigital = !digitalReport.isCorrupted &&
                    digitalReport.score >= 0.75f &&
                    rawDigital.trim().length >= 40

            if (isGenuineDigital) {
                // Method A: Document has genuine, cleanly encoded digital text
                digitalPages++
                onProgress("Página $pageNumber de $totalPages: Texto digital verificado", progressFrac)

                val cleanedParas = TextReconstructor.reconstruct(rawDigital, null, pageNumber)
                for (paraText in cleanedParas) {
                    if (paraText.isNotBlank()) {
                        if (TextReconstructor.isChapterHeading(paraText)) {
                            currentChapter = paraText.take(60)
                        }
                        allParagraphs.add(
                            BookParagraphEntity(
                                bookId = 0,
                                paragraphIndex = currentParagraphIndex++,
                                pageNumber = pageNumber,
                                chapterTitle = currentChapter,
                                text = paraText,
                                originalText = paraText,
                                extractionMethod = "DIGITAL",
                                qualityScore = digitalReport.score,
                                isLowConfidence = false,
                                isUserEdited = false
                            )
                        )
                    }
                }
            } else {
                // Method B & C: Page is scanned, a photo, or digital extraction was corrupt/empty!
                // Execute real on-device ML Kit OCR on the rendered image of this page.
                onProgress("Página $pageNumber de $totalPages: Reconociendo imagen con OCR...", progressFrac)
                ocrPages++

                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(pageIdx)
                    val ocrResult = OcrProcessor.recognizePage(page, pageNumber, ocrLanguage)
                    page.close()
                    page = null

                    val ocrReport = TextQualityAnalyzer.evaluate(ocrResult.fullText)

                    if (ocrResult.paragraphs.isNotEmpty() && !ocrReport.isCorrupted) {
                        // OCR succeeded with clean, readable text!
                        for (paraText in ocrResult.paragraphs) {
                            if (paraText.isNotBlank()) {
                                if (TextReconstructor.isChapterHeading(paraText)) {
                                    currentChapter = paraText.take(60)
                                }
                                allParagraphs.add(
                                    BookParagraphEntity(
                                        bookId = 0,
                                        paragraphIndex = currentParagraphIndex++,
                                        pageNumber = pageNumber,
                                        chapterTitle = currentChapter,
                                        text = paraText,
                                        originalText = paraText,
                                        extractionMethod = "OCR",
                                        qualityScore = ocrReport.score,
                                        isLowConfidence = ocrReport.score < 0.60f,
                                        isUserEdited = false
                                    )
                                )
                            }
                        }
                    } else if (ocrResult.paragraphs.isNotEmpty()) {
                        // OCR produced text, but with lower confidence (e.g. faded scan)
                        hasLowConfidence = true
                        for (paraText in ocrResult.paragraphs) {
                            if (paraText.isNotBlank()) {
                                if (TextReconstructor.isChapterHeading(paraText)) {
                                    currentChapter = paraText.take(60)
                                }
                                allParagraphs.add(
                                    BookParagraphEntity(
                                        bookId = 0,
                                        paragraphIndex = currentParagraphIndex++,
                                        pageNumber = pageNumber,
                                        chapterTitle = currentChapter,
                                        text = paraText,
                                        originalText = paraText,
                                        extractionMethod = "OCR",
                                        qualityScore = ocrReport.score,
                                        isLowConfidence = true,
                                        isUserEdited = false
                                    )
                                )
                            }
                        }
                    } else {
                        // Neither digital nor OCR detected text (blank page, pure illustration, or photo without words)
                        // Do NOT invent or inject binary garbage!
                        allParagraphs.add(
                            BookParagraphEntity(
                                bookId = 0,
                                paragraphIndex = currentParagraphIndex++,
                                pageNumber = pageNumber,
                                chapterTitle = currentChapter,
                                text = "[Página $pageNumber: Imagen sin texto detectable]",
                                originalText = "",
                                extractionMethod = "OCR",
                                qualityScore = 0.5f,
                                isLowConfidence = false,
                                isUserEdited = false
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error durante OCR en página $pageNumber: ${e.message}")
                    page?.close()
                }
            }

            // Memory maintenance for large documents: prompt garbage collection every 20 pages
            if (pageIdx % 20 == 0) {
                System.gc()
            }
        }

        renderer.close()
        pfd.close()

        if (allParagraphs.isEmpty()) {
            allParagraphs.add(
                BookParagraphEntity(
                    bookId = 0,
                    paragraphIndex = 0,
                    pageNumber = 1,
                    chapterTitle = "Página 1",
                    text = "El documento no contiene texto legible ni escaneado reconocido. Puedes revisar o ingresar texto manualmente.",
                    originalText = "",
                    extractionMethod = "OCR",
                    qualityScore = 0.1f,
                    isLowConfidence = true,
                    isUserEdited = false
                )
            )
        }

        onProgress("¡Procesamiento finalizado con éxito!", 1.0f)

        val rawBaseName = originalFileName.removeSuffix(".pdf").removeSuffix(".PDF")
        val (title, author) = deriveTitleAndAuthor(rawBaseName, allParagraphs.firstOrNull()?.text ?: "")

        HybridExtractionResult(
            title = title,
            author = author,
            coverPath = coverPath,
            totalPages = totalPages,
            paragraphs = allParagraphs,
            ocrPageCount = ocrPages,
            digitalPageCount = digitalPages,
            hasLowConfidencePages = hasLowConfidence
        )
    }

    /**
     * Re-runs OCR for a specific page with a designated language.
     */
    suspend fun reprocessSinglePageWithOcr(
        context: Context,
        pdfFilePath: String,
        pageNumber: Int,
        bookId: Long,
        language: OcrLanguage = OcrLanguage.SPANISH,
        forcedPreprocessing: PreprocessingMode? = null
    ): List<BookParagraphEntity> = withContext(Dispatchers.IO) {
        val file = File(pdfFilePath)
        if (!file.exists()) return@withContext emptyList()

        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        val pageIdx = (pageNumber - 1).coerceIn(0, renderer.pageCount - 1)

        val page = renderer.openPage(pageIdx)
        val ocrResult = OcrProcessor.recognizePage(page, pageNumber, language, forcedPreprocessing)
        page.close()
        renderer.close()
        pfd.close()

        val report = TextQualityAnalyzer.evaluate(ocrResult.fullText)
        val result = mutableListOf<BookParagraphEntity>()
        var chapter = "Página $pageNumber"

        if (ocrResult.paragraphs.isNotEmpty()) {
            for ((idx, paraText) in ocrResult.paragraphs.withIndex()) {
                if (TextReconstructor.isChapterHeading(paraText)) {
                    chapter = paraText.take(60)
                }
                result.add(
                    BookParagraphEntity(
                        bookId = bookId,
                        paragraphIndex = idx,
                        pageNumber = pageNumber,
                        chapterTitle = chapter,
                        text = paraText,
                        originalText = paraText,
                        extractionMethod = "OCR",
                        qualityScore = report.score,
                        isLowConfidence = report.score < 0.60f,
                        isUserEdited = false
                    )
                )
            }
        } else {
            result.add(
                BookParagraphEntity(
                    bookId = bookId,
                    paragraphIndex = 0,
                    pageNumber = pageNumber,
                    chapterTitle = chapter,
                    text = "[Página $pageNumber: Sin texto detectado por OCR]",
                    originalText = "",
                    extractionMethod = "OCR",
                    qualityScore = 0.5f,
                    isLowConfidence = false,
                    isUserEdited = false
                )
            )
        }
        result
    }

    /**
     * Extracts ONLY genuine text streams from PDF.
     * Streams without 'BT' and 'ET' text drawing commands are binary images or fonts and are completely ignored.
     */
    private fun extractDigitalTextPages(pdfFile: File, totalPages: Int): List<String> {
        val result = MutableList(totalPages) { "" }
        try {
            val bytes = pdfFile.readBytes()
            val textBlocks = mutableListOf<String>()

            val streamMarker = "stream".toByteArray(Charsets.ISO_8859_1)
            val endStreamMarker = "endstream".toByteArray(Charsets.ISO_8859_1)

            var searchIndex = 0
            while (searchIndex < bytes.size) {
                val start = indexOf(bytes, streamMarker, searchIndex)
                if (start == -1) break

                var contentStart = start + streamMarker.size
                if (contentStart < bytes.size && bytes[contentStart] == '\r'.code.toByte()) contentStart++
                if (contentStart < bytes.size && bytes[contentStart] == '\n'.code.toByte()) contentStart++

                val end = indexOf(bytes, endStreamMarker, contentStart)
                if (end == -1) break

                val streamLength = end - contentStart
                if (streamLength in 1..8_000_000) {
                    val streamBytes = bytes.copyOfRange(contentStart, end)
                    val decompressed = tryDecompressFlate(streamBytes) ?: streamBytes

                    // CRITICAL FILTER: Only process streams containing BT (Begin Text) and ET (End Text).
                    // This prevents binary image or font data from ever being parsed as text!
                    val asIso = String(decompressed, Charsets.ISO_8859_1)
                    if (asIso.contains("BT") && asIso.contains("ET")) {
                        val extracted = extractStringsFromTextStream(asIso)
                        if (extracted.isNotBlank()) {
                            textBlocks.add(extracted)
                        }
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
            Log.e(TAG, "Error extrayendo texto digital: ${e.message}")
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

    /**
     * Extracts text strings specifically from within BT ... ET blocks.
     */
    private fun extractStringsFromTextStream(content: String): String {
        val sb = StringBuilder()

        // Match BT ... ET blocks
        val btPattern = Pattern.compile("BT(.*?)ET", Pattern.DOTALL)
        val btMatcher = btPattern.matcher(content)

        while (btMatcher.find()) {
            val block = btMatcher.group(1) ?: continue

            // 1. Literal strings (text) Tj or ' or "
            val tjPattern = Pattern.compile("\\((.*?)\\)\\s*(?:Tj|'|\")", Pattern.DOTALL)
            val tjMatcher = tjPattern.matcher(block)
            while (tjMatcher.find()) {
                val str = tjMatcher.group(1)?.let { decodePdfLiteral(it) } ?: ""
                if (str.isNotBlank()) {
                    sb.append(str).append(" ")
                }
            }

            // 2. Array TJ operators: [ (str) num (str) ] TJ
            val arrayTjPattern = Pattern.compile("\\[(.*?)\\]\\s*TJ", Pattern.DOTALL)
            val arrayMatcher = arrayTjPattern.matcher(block)
            while (arrayMatcher.find()) {
                val inner = arrayMatcher.group(1) ?: ""
                val innerP = Pattern.compile("\\((.*?)\\)")
                val innerM = innerP.matcher(inner)
                while (innerM.find()) {
                    val s = innerM.group(1)?.let { decodePdfLiteral(it) } ?: ""
                    sb.append(s)
                }
                sb.append(" ")
            }

            // 3. Hex strings: <48656C6C6F> Tj
            val hexPattern = Pattern.compile("<([0-9a-fA-F]+)>\\s*(?:Tj|'|\")")
            val hexMatcher = hexPattern.matcher(block)
            while (hexMatcher.find()) {
                val hex = hexMatcher.group(1) ?: ""
                val decoded = decodePdfHexString(hex)
                if (decoded.isNotBlank()) {
                    sb.append(decoded).append(" ")
                }
            }
        }

        return sb.toString().trim()
    }

    private fun decodePdfLiteral(raw: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            if (c == '\\' && i + 1 < raw.length) {
                val next = raw[i + 1]
                if (next in '0'..'7') {
                    var octalStr = ""
                    var j = i + 1
                    while (j < raw.length && j < i + 4 && raw[j] in '0'..'7') {
                        octalStr += raw[j]
                        j++
                    }
                    val codePoint = octalStr.toIntOrNull(8) ?: 32
                    sb.append(mapPdfCodePointToChar(codePoint))
                    i = j
                    continue
                } else {
                    when (next) {
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'b' -> sb.append('\b')
                        '(' -> sb.append('(')
                        ')' -> sb.append(')')
                        '\\' -> sb.append('\\')
                        else -> sb.append(next)
                    }
                    i += 2
                    continue
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    private fun mapPdfCodePointToChar(code: Int): Char {
        return when (code) {
            225 -> 'á'
            233 -> 'é'
            237 -> 'í'
            243 -> 'ó'
            250 -> 'ú'
            241 -> 'ñ'
            252 -> 'ü'
            193 -> 'Á'
            201 -> 'É'
            205 -> 'Í'
            211 -> 'Ó'
            218 -> 'Ú'
            209 -> 'Ñ'
            220 -> 'Ü'
            191 -> '¿'
            161 -> '¡'
            171 -> '«'
            187 -> '»'
            150 -> '–'
            151 -> '—'
            else -> if (code in 32..255) code.toChar() else ' '
        }
    }

    private fun decodePdfHexString(hex: String): String {
        return try {
            val cleanHex = if (hex.length % 2 != 0) hex + "0" else hex
            val bytes = ByteArray(cleanHex.length / 2)
            for (i in bytes.indices) {
                val index = i * 2
                bytes[i] = cleanHex.substring(index, index + 2).toInt(16).toByte()
            }
            if (bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte()) {
                String(bytes.copyOfRange(2, bytes.size), Charsets.UTF_16BE)
            } else {
                String(bytes, Charsets.ISO_8859_1)
            }
        } catch (e: Exception) {
            ""
        }
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

    private fun deriveTitleAndAuthor(filename: String, firstText: String): Pair<String, String> {
        val cleaned = filename
            .replace("_", " ")
            .replace("-", " ")
            .trim()

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
