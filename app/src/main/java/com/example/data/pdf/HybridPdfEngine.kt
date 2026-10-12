package com.example.data.pdf

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.model.BookParagraphEntity
import com.example.data.model.ExtractionSource
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class PdfImportResult(
    val title: String,
    val author: String,
    val totalPages: Int,
    val paragraphs: List<BookParagraphEntity>,
    val ocrPageCount: Int,
    val digitalPageCount: Int,
    val hasLowConfidencePages: Boolean,
    val localFilePath: String = ""
)

object HybridPdfEngine {
    private const val TAG = "HybridPdfEngine"

    suspend fun processPdf(
        context: Context,
        pdfUri: Uri,
        originalFileName: String,
        selectedLanguage: OcrLanguage = OcrLanguage.SPANISH,
        onProgress: (page: Int, totalPages: Int, statusText: String) -> Unit
    ): PdfImportResult = withContext(Dispatchers.IO) {
        PDFBoxResourceLoader.init(context)

        val localPdfFile = copyUriToInternalStorage(context, pdfUri, originalFileName)
        val fileDescriptor = ParcelFileDescriptor.open(localPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(fileDescriptor)
        val totalPages = pdfRenderer.pageCount

        val pdDocument = try {
            PDDocument.load(localPdfFile)
        } catch (e: Exception) {
            Log.w(TAG, "PDFBox load failed: ${e.message}, falling back strictly to renderer+OCR")
            null
        }

        val allParagraphs = mutableListOf<BookParagraphEntity>()
        var ocrPageCount = 0
        var digitalPageCount = 0
        var hasLowConfidencePages = false
        var currentGlobalParagraphIndex = 0

        try {
            for (pageIndex in 0 until totalPages) {
                val pageNumber = pageIndex + 1
                onProgress(pageNumber, totalPages, "Analizando página $pageNumber de $totalPages...")

                var digitalText = ""
                if (pdDocument != null) {
                    try {
                        val stripper = PDFTextStripper()
                        stripper.startPage = pageNumber
                        stripper.endPage = pageNumber
                        digitalText = stripper.getText(pdDocument).trim()
                    } catch (e: Exception) {
                        Log.e(TAG, "Digital extraction error on page $pageNumber: ${e.message}")
                    }
                }

                val digitalQuality = TextQualityAnalyzer.evaluate(digitalText)
                val needsOcr = digitalText.isBlank() || digitalQuality.isCorrupted || digitalQuality.totalWords < 5

                val (pageParagraphs, method, pageQuality) = if (needsOcr) {
                    onProgress(pageNumber, totalPages, "Aplicando OCR inteligente a página $pageNumber (escaneada/antigua)...")
                    val page = pdfRenderer.openPage(pageIndex)
                    val ocrResult = try {
                        OcrProcessor.recognizePage(page, pageNumber, selectedLanguage)
                    } finally {
                        page.close()
                    }

                    ocrPageCount++
                    val qScore = ocrResult.qualityScore
                    val isLowConf = qScore < 0.65f || ocrResult.paragraphs.isEmpty()
                    if (isLowConf) hasLowConfidencePages = true

                    Triple(ocrResult.paragraphs, ExtractionSource.OCR, qScore)
                } else {
                    digitalPageCount++
                    val reconstructed = TextReconstructor.reconstruct(digitalText, null, pageNumber)
                    Triple(reconstructed, ExtractionSource.DIGITAL, digitalQuality.score)
                }

                var currentChapter = "Capítulo $pageNumber"
                for (pText in pageParagraphs) {
                    if (TextReconstructor.isChapterHeading(pText)) {
                        currentChapter = pText
                    }

                    allParagraphs.add(
                        BookParagraphEntity(
                            bookId = 0L,
                            paragraphIndex = currentGlobalParagraphIndex++,
                            pageNumber = pageNumber,
                            chapterTitle = currentChapter,
                            text = pText,
                            originalText = pText,
                            extractionMethod = method.name,
                            qualityScore = pageQuality,
                            isLowConfidence = pageQuality < 0.65f
                        )
                    )
                }
            }
        } finally {
            pdDocument?.close()
            pdfRenderer.close()
            fileDescriptor.close()
        }

        val bookTitle = originalFileName.removeSuffix(".pdf").replace("_", " ").trim()
        PdfImportResult(
            title = bookTitle,
            author = "Autor Desconocido",
            totalPages = totalPages,
            paragraphs = allParagraphs,
            ocrPageCount = ocrPageCount,
            digitalPageCount = digitalPageCount,
            hasLowConfidencePages = hasLowConfidencePages,
            localFilePath = localPdfFile.absolutePath
        )
    }

    suspend fun reprocessSinglePageWithOcr(
        context: Context,
        pdfFilePath: String,
        pageNumber: Int,
        bookId: Long,
        language: OcrLanguage = OcrLanguage.SPANISH,
        forcedPreprocessing: PreprocessingMode? = null
    ): List<BookParagraphEntity> = withContext(Dispatchers.IO) {
        val file = File(pdfFilePath)
        if (!file.exists()) {
            return@withContext emptyList()
        }

        val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(fileDescriptor)
        try {
            val pageIndex = (pageNumber - 1).coerceIn(0, pdfRenderer.pageCount - 1)
            val page = pdfRenderer.openPage(pageIndex)
            val ocrResult = try {
                OcrProcessor.recognizePage(page, pageNumber, language, forcedPreprocessing)
            } finally {
                page.close()
            }

            var currentChapter = "Página $pageNumber"
            ocrResult.paragraphs.mapIndexed { idx, pText ->
                if (TextReconstructor.isChapterHeading(pText)) {
                    currentChapter = pText
                }
                BookParagraphEntity(
                    bookId = bookId,
                    paragraphIndex = idx,
                    pageNumber = pageNumber,
                    chapterTitle = currentChapter,
                    text = pText,
                    originalText = pText,
                    extractionMethod = ExtractionSource.OCR.name,
                    qualityScore = ocrResult.qualityScore,
                    isLowConfidence = ocrResult.qualityScore < 0.65f,
                    isUserEdited = false
                )
            }
        } finally {
            pdfRenderer.close()
            fileDescriptor.close()
        }
    }

    private fun copyUriToInternalStorage(context: Context, uri: Uri, fileName: String): File {
        val pdfDir = File(context.filesDir, "imported_pdfs")
        if (!pdfDir.exists()) pdfDir.mkdirs()

        val safeFileName = "${System.currentTimeMillis()}_${fileName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")}"
        val destFile = File(pdfDir, safeFileName)

        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        return destFile
    }
}
