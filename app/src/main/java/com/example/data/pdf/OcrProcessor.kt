package com.example.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.abs

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}

enum class OcrLanguage(val code: String, val displayName: String, val compatibilityNote: String) {
    SPANISH("es", "Español", "Alfabeto latino con tildes, ñ y signos ¿?. Totalmente compatible."),
    ENGLISH("en", "Inglés", "Alfabeto latino estándar. Totalmente compatible."),
    LATIN("la", "Latín (Textos Históricos)", "Alfabeto latino clásico y eclesiástico. Totalmente compatible."),
    FRENCH("fr", "Francés", "Alfabeto latino con diacríticos y ligaduras comunes. Totalmente compatible."),
    GERMAN("de", "Alemán (Tipografía Latina)", "Alfabeto latino con diéresis y ß. Nota: ML Kit no incluye modelo entrenado para Fraktur/gótico puro."),
    PORTUGUESE("pt", "Portugués", "Alfabeto latino con cedilla y tildes. Totalmente compatible."),
    ITALIAN("it", "Italiano", "Alfabeto latino con acentos. Totalmente compatible."),
    AUTO("auto", "Detección Automática", "Detección automática en alfabetos latinos soportados por el motor local.")
}

data class OcrEngineStatus(
    val isReady: Boolean,
    val isLocalOffline: Boolean,
    val engineName: String,
    val supportedLanguagesSummary: String,
    val historicalTypographySummary: String,
    val instructions: String?
)

data class OcrPageResult(
    val fullText: String,
    val paragraphs: List<String>,
    val nativeConfidence: Float? = null,
    val qualityScore: Float = 1.0f,
    val isMultiColumn: Boolean = false,
    val blockCount: Int = 0,
    val preprocessingUsed: PreprocessingMode = PreprocessingMode.STANDARD,
    val detectedAngle: Float = 0f
) {
    val averageConfidence: Float
        get() = nativeConfidence ?: qualityScore
}

object OcrProcessor {
    private const val TAG = "OcrProcessor"

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun checkEngineStatus(context: Context): OcrEngineStatus {
        return try {
            val testBmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            val input = InputImage.fromBitmap(testBmp, 0)
            recognizer.process(input).await()
            testBmp.recycle()

            OcrEngineStatus(
                isReady = true,
                isLocalOffline = true,
                engineName = "Google ML Kit Text Recognition (Alfabetos Latinos)",
                supportedLanguagesSummary = "Español, Inglés, Latín, Francés, Alemán (alfabeto latino), Portugués, Italiano",
                historicalTypographySummary = "Filtros adaptativos para papel amarillento, s-larga (ſ), ligaduras históricas y tipografías con serifas. Nota: ML Kit opera sobre alfabetos latinos y no cuenta con modelo entrenado para Fraktur/gótico puro.",
                instructions = null
            )
        } catch (e: Exception) {
            Log.e(TAG, "OCR health check failed: ${e.message}")
            OcrEngineStatus(
                isReady = false,
                isLocalOffline = true,
                engineName = "Google ML Kit Text Recognition",
                supportedLanguagesSummary = "Español y alfabetos latinos",
                historicalTypographySummary = "Filtros de restauración histórica para alfabetos latinos. Nota: ML Kit no incluye modelo específico para tipografía Fraktur/gótica pura.",
                instructions = "El motor OCR se está inicializando. Es 100% local y no requiere conexión a internet ni descargas adicionales."
            )
        }
    }

    suspend fun recognizePage(
        page: PdfRenderer.Page,
        pageNumber: Int,
        language: OcrLanguage = OcrLanguage.SPANISH,
        forcedPreprocessing: PreprocessingMode? = null
    ): OcrPageResult {
        val targetWidth = (page.width * 2.2f).toInt().coerceIn(1400, 2200)
        val targetHeight = (page.height * (targetWidth.toFloat() / page.width)).toInt()

        var rawBitmap: Bitmap? = null
        try {
            rawBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            page.render(rawBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

            val trimmedBitmap = ImagePreprocessor.trimScannerBorders(rawBitmap)

            return recognizeBitmapAdaptive(
                bitmap = trimmedBitmap,
                pageNumber = pageNumber,
                language = language,
                forcedPreprocessing = forcedPreprocessing
            )
        } catch (e: Exception) {
            Log.e(TAG, "OCR error on page $pageNumber: ${e.message}")
            return OcrPageResult(
                fullText = "",
                paragraphs = emptyList(),
                nativeConfidence = null,
                qualityScore = 0.0f,
                isMultiColumn = false,
                blockCount = 0,
                preprocessingUsed = forcedPreprocessing ?: PreprocessingMode.STANDARD
            )
        } finally {
            rawBitmap?.recycle()
        }
    }

    suspend fun recognizeBitmapAdaptive(
        bitmap: Bitmap,
        pageNumber: Int,
        language: OcrLanguage = OcrLanguage.SPANISH,
        forcedPreprocessing: PreprocessingMode? = null
    ): OcrPageResult {
        if (forcedPreprocessing != null) {
            val processedBmp = ImagePreprocessor.preprocess(bitmap, forcedPreprocessing)
            val result = recognizeBitmapDirect(processedBmp, pageNumber, language, forcedPreprocessing)
            if (processedBmp != bitmap) processedBmp.recycle()
            return result
        }

        val standardBmp = ImagePreprocessor.preprocess(bitmap, PreprocessingMode.STANDARD)
        var standardResult = recognizeBitmapDirect(standardBmp, pageNumber, language, PreprocessingMode.STANDARD)

        if (abs(standardResult.detectedAngle) > 2.0f && abs(standardResult.detectedAngle) < 45.0f) {
            val deskewedBmp = ImagePreprocessor.deskew(standardBmp, standardResult.detectedAngle)
            val deskewedResult = recognizeBitmapDirect(deskewedBmp, pageNumber, language, PreprocessingMode.STANDARD)
            if (deskewedResult.qualityScore >= standardResult.qualityScore) {
                standardResult = deskewedResult
            }
            if (deskewedBmp != standardBmp) deskewedBmp.recycle()
        }
        if (standardBmp != bitmap) standardBmp.recycle()

        val standardQuality = TextQualityAnalyzer.evaluate(standardResult.fullText)

        if (standardQuality.score >= 0.75f && standardQuality.totalWords >= 15 && !standardQuality.isCorrupted) {
            return standardResult
        }

        val vintageBmp = ImagePreprocessor.preprocess(bitmap, PreprocessingMode.VINTAGE_BOOK)
        val vintageResult = recognizeBitmapDirect(vintageBmp, pageNumber, language, PreprocessingMode.VINTAGE_BOOK)
        vintageBmp.recycle()
        val vintageQuality = TextQualityAnalyzer.evaluate(vintageResult.fullText)

        val binarizedBmp = ImagePreprocessor.preprocess(bitmap, PreprocessingMode.ADAPTIVE_BINARIZATION)
        val binarizedResult = recognizeBitmapDirect(binarizedBmp, pageNumber, language, PreprocessingMode.ADAPTIVE_BINARIZATION)
        binarizedBmp.recycle()
        val binarizedQuality = TextQualityAnalyzer.evaluate(binarizedResult.fullText)

        val sharpenBmp = ImagePreprocessor.preprocess(bitmap, PreprocessingMode.SHARPEN_FINE)
        val sharpenResult = recognizeBitmapDirect(sharpenBmp, pageNumber, language, PreprocessingMode.SHARPEN_FINE)
        sharpenBmp.recycle()
        val sharpenQuality = TextQualityAnalyzer.evaluate(sharpenResult.fullText)

        val candidates = listOf(
            Triple(standardResult, standardQuality, PreprocessingMode.STANDARD),
            Triple(vintageResult, vintageQuality, PreprocessingMode.VINTAGE_BOOK),
            Triple(binarizedResult, binarizedQuality, PreprocessingMode.ADAPTIVE_BINARIZATION),
            Triple(sharpenResult, sharpenQuality, PreprocessingMode.SHARPEN_FINE)
        )

        val bestCandidate = candidates.maxByOrNull { (_, quality, _) ->
            val wordBonus = (quality.totalWords.coerceAtMost(300) / 300f) * 0.25f
            quality.score + wordBonus + (quality.legitimateWordRatio * 0.15f) - (if (quality.isCorrupted) 0.50f else 0.0f)
        }

        return bestCandidate?.first ?: standardResult
    }

    suspend fun recognizeBitmapDirect(
        bitmap: Bitmap,
        pageNumber: Int,
        language: OcrLanguage = OcrLanguage.SPANISH,
        mode: PreprocessingMode = PreprocessingMode.STANDARD
    ): OcrPageResult {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(inputImage).await()
            processVisionText(visionText, bitmap.width, pageNumber, language, mode)
        } catch (e: Exception) {
            Log.e(TAG, "OCR recognizeBitmapDirect error: ${e.message}")
            OcrPageResult("", emptyList(), null, 0.0f, false, 0, mode)
        }
    }

    suspend fun recognizeBitmap(
        bitmap: Bitmap,
        pageNumber: Int,
        language: OcrLanguage = OcrLanguage.SPANISH
    ): OcrPageResult {
        return recognizeBitmapAdaptive(bitmap, pageNumber, language, null)
    }

    private fun processVisionText(
        visionText: Text,
        pageWidth: Int,
        pageNumber: Int,
        language: OcrLanguage,
        mode: PreprocessingMode
    ): OcrPageResult {
        val blocks = visionText.textBlocks
        if (blocks.isEmpty()) {
            return OcrPageResult("", emptyList(), null, 0.0f, false, 0, mode)
        }

        val midpoint = pageWidth / 2
        var leftCount = 0
        var rightCount = 0
        var spanningCount = 0

        var sumAngle = 0f
        var angleLines = 0

        var sumConfidence = 0.0f
        var elementsWithConfidence = 0

        for (block in blocks) {
            val box = block.boundingBox
            if (box != null) {
                val left = box.left
                val right = box.right
                val width = box.width()

                if (width > pageWidth * 0.70f) {
                    spanningCount++
                } else if (right <= midpoint + (pageWidth * 0.08f)) {
                    leftCount++
                } else if (left >= midpoint - (pageWidth * 0.08f)) {
                    rightCount++
                }
            }

            for (line in block.lines) {
                val angle = try {
                    val m = line.javaClass.getMethod("getAngle")
                    (m.invoke(line) as? Number)?.toFloat() ?: 0f
                } catch (e: Throwable) {
                    0f
                }
                if (angle != 0f) {
                    sumAngle += angle
                    angleLines++
                }

                for (element in line.elements) {
                    val conf = try {
                        val m = element.javaClass.getMethod("getConfidence")
                        (m.invoke(element) as? Number)?.toFloat()
                    } catch (e: Throwable) {
                        null
                    }
                    if (conf != null && !conf.isNaN()) {
                        sumConfidence += conf
                        elementsWithConfidence++
                    }
                }
            }
        }

        val detectedAngle = if (angleLines > 0) sumAngle / angleLines else 0f
        val nativeConfidence = if (elementsWithConfidence > 0) sumConfidence / elementsWithConfidence else null
        val isMultiColumn = leftCount >= 2 && rightCount >= 2 && spanningCount <= 2

        val sortedBlocks = if (isMultiColumn) {
            val leftColumn = blocks.filter { (it.boundingBox?.left ?: 0) < midpoint }
                .sortedBy { it.boundingBox?.top ?: 0 }
            val rightColumn = blocks.filter { (it.boundingBox?.left ?: 0) >= midpoint }
                .sortedBy { it.boundingBox?.top ?: 0 }
            leftColumn + rightColumn
        } else {
            blocks.sortedBy { it.boundingBox?.top ?: 0 }
        }

        val assembledText = StringBuilder()
        for (block in sortedBlocks) {
            val blockClean = block.text.trim()
            if (blockClean.isNotBlank()) {
                assembledText.append(blockClean).append("\n\n")
            }
        }

        val rawStr = assembledText.toString().trim()
        val reconstructed = TextReconstructor.reconstruct(rawStr, null, pageNumber)

        val normalizedParas = reconstructed.map { para ->
            HistoricalOrthographyNormalizer.normalizeGlyphs(para)
        }

        val qualityReport = TextQualityAnalyzer.evaluate(rawStr)

        return OcrPageResult(
            fullText = rawStr,
            paragraphs = normalizedParas,
            nativeConfidence = nativeConfidence,
            qualityScore = qualityReport.score,
            isMultiColumn = isMultiColumn,
            blockCount = blocks.size,
            preprocessingUsed = mode,
            detectedAngle = detectedAngle
        )
    }
}
