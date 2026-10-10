package com.example.data.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import kotlin.math.max
import kotlin.math.min

/**
 * Modes of image pre-processing specialized for difficult typographies,
 * historical prints, aged yellowed paper, faded ink, and high-serif text.
 */
enum class PreprocessingMode(val id: String, val displayName: String, val description: String) {
    STANDARD("standard", "Estándar", "Escala de grises y contraste balanceado para libros contemporáneos"),
    VINTAGE_BOOK("vintage", "Libro Antiguo / Papel Amarillo", "Elimina fondo amarillento, realza tinta tenue y compensa iluminación desigual"),
    ADAPTIVE_BINARIZATION("binarized", "Binarización Adaptativa", "Alto contraste blanco y negro para fuentes góticas, Fraktur y serifas marcadas"),
    SHARPEN_FINE("sharpen", "Enfoque de Trazos Finos", "Realza serifas pronunciadas, ligaduras y tipografías pequeñas o ligeramente borrosas")
}

data class SkewCorrectionResult(
    val bitmap: Bitmap,
    val angleDegrees: Float
)

/**
 * Image preprocessor designed for difficult book scans:
 * - Aged / yellowed pages with uneven illumination
 * - Faded / irregular ink, broken strokes
 * - Heavy serif, Gothic / Fraktur fonts
 * - Margin cropping of dark scanner borders
 * - Skew / tilt detection and rotation
 */
object ImagePreprocessor {

    /**
     * Preprocesses a rendered page bitmap using the designated strategy.
     * Always returns a newly allocated Bitmap; the caller is responsible for recycling.
     */
    fun preprocess(source: Bitmap, mode: PreprocessingMode): Bitmap {
        return when (mode) {
            PreprocessingMode.STANDARD -> applyStandardEnhancement(source)
            PreprocessingMode.VINTAGE_BOOK -> applyVintageBookCorrection(source)
            PreprocessingMode.ADAPTIVE_BINARIZATION -> applyAdaptiveBinarization(source)
            PreprocessingMode.SHARPEN_FINE -> applyFineStrokeEnhancement(source)
        }
    }

    /**
     * Standard enhancement: mild grayscale conversion with moderate contrast boost.
     */
    private fun applyStandardEnhancement(src: Bitmap): Bitmap {
        val dest = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(dest)
        val paint = Paint()

        val cm = ColorMatrix()
        cm.setSaturation(0.0f)
        val contrast = 1.30f
        val translate = (-0.5f * contrast + 0.5f) * 255f
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)
        paint.colorFilter = ColorMatrixColorFilter(cm)
        canvas.drawBitmap(src, 0f, 0f, paint)
        return dest
    }

    /**
     * Vintage Book / Aged Paper Correction:
     * - Whitens yellowish/brownish background paper tone.
     * - Elevates the blue channel weight (yellow is high R+G, low B; boosting B response flattens yellow background).
     * - Darkens faded ink without eroding thin historical character serifs.
     */
    fun applyVintageBookCorrection(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Sample background luminance estimation (sampling corners and edges)
        var bgSum = 0L
        var bgSamples = 0
        val sampleStep = max(1, min(width, height) / 30)
        for (y in 0 until min(height, 80) step sampleStep) {
            for (x in 0 until width step sampleStep) {
                val p = pixels[y * width + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val lum = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
                bgSum += lum
                bgSamples++
            }
        }
        val estimatedBg = if (bgSamples > 0) (bgSum / bgSamples).toInt().coerceIn(160, 245) else 215

        // Process pixels: Stretch paper tone to clean white (255) and clamp dark ink
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF

            // Perceptual gray favoring ink contrast over yellow paper
            // Paper yellow has low blue, so weighting blue reduces yellow dominance
            val gray = (0.2126 * r + 0.7152 * g + 0.0722 * b).toInt()

            // Non-linear gamma stretch:
            // If near background level, brighten to white
            // If significantly darker, preserve ink density
            val normalized = (gray.toFloat() / estimatedBg.toFloat()).coerceIn(0f, 1.25f)
            val enhanced = if (normalized > 0.85f) {
                255
            } else {
                // S-curve contrast boost on ink strokes
                val ink = (normalized / 0.85f)
                (ink * ink * 240).toInt().coerceIn(0, 255)
            }

            pixels[i] = (0xFF shl 24) or (enhanced shl 16) or (enhanced shl 8) or enhanced
        }

        dest.setPixels(pixels, 0, width, 0, 0, width, height)
        return dest
    }

    /**
     * Adaptive Binarization (Otsu-inspired local thresholding):
     * Ideal for Gothic / Fraktur fonts and heavy serif typography where sharp character
     * boundaries dramatically improve OCR token classification.
     */
    fun applyAdaptiveBinarization(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Compute local grid thresholds (8x8 window grid to accommodate uneven scanner lighting)
        val gridCols = 8
        val gridRows = 8
        val colWidth = width / gridCols
        val rowHeight = height / gridRows
        val thresholds = Array(gridRows) { IntArray(gridCols) }

        for (gr in 0 until gridRows) {
            for (gc in 0 until gridCols) {
                var sum = 0L
                var count = 0
                val startX = gc * colWidth
                val endX = if (gc == gridCols - 1) width else (gc + 1) * colWidth
                val startY = gr * rowHeight
                val endY = if (gr == gridRows - 1) height else (gr + 1) * rowHeight

                for (y in startY until endY step 4) {
                    for (x in startX until endX step 4) {
                        val p = pixels[y * width + x]
                        val r = (p shr 16) and 0xFF
                        val g = (p shr 8) and 0xFF
                        val b = p and 0xFF
                        sum += (r + g + b) / 3
                        count++
                    }
                }
                val avg = if (count > 0) (sum / count).toInt() else 128
                // Set threshold slightly below average to prevent stroke erosion
                thresholds[gr][gc] = (avg * 0.82f).toInt().coerceIn(70, 200)
            }
        }

        for (y in 0 until height) {
            val gr = (y / max(1, rowHeight)).coerceIn(0, gridRows - 1)
            for (x in 0 until width) {
                val gc = (x / max(1, colWidth)).coerceIn(0, gridCols - 1)
                val p = pixels[y * width + x]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val gray = (r * 299 + g * 587 + b * 114) / 1000
                val thresh = thresholds[gr][gc]

                val binColor = if (gray < thresh) 0x00 else 0xFF
                pixels[y * width + x] = (0xFF shl 24) or (binColor shl 16) or (binColor shl 8) or binColor
            }
        }

        dest.setPixels(pixels, 0, width, 0, 0, width, height)
        return dest
    }

    /**
     * Fine Stroke / High-Frequency Detail Enhancement:
     * Unsharp-masking style kernel that brings out small antique fonts, serifs, and italics.
     */
    fun applyFineStrokeEnhancement(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        val dest = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        val outPixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        // Convert to grayscale first
        for (i in pixels.indices) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val gray = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            pixels[i] = gray
        }

        // 3x3 Sharpening filter: [0, -1, 0; -1, 5, -1; 0, -1, 0]
        for (y in 1 until height - 1) {
            val rowOffset = y * width
            for (x in 1 until width - 1) {
                val center = pixels[rowOffset + x]
                val top = pixels[rowOffset - width + x]
                val bottom = pixels[rowOffset + width + x]
                val left = pixels[rowOffset + x - 1]
                val right = pixels[rowOffset + x + 1]

                // Sharpen response
                val sharpened = 5 * center - top - bottom - left - right
                // Soft contrast clamp
                val clamped = sharpened.coerceIn(0, 255)
                outPixels[rowOffset + x] = (0xFF shl 24) or (clamped shl 16) or (clamped shl 8) or clamped
            }
        }

        // Fill borders
        for (x in 0 until width) {
            val pTop = pixels[x].coerceIn(0, 255)
            val pBottom = pixels[(height - 1) * width + x].coerceIn(0, 255)
            outPixels[x] = (0xFF shl 24) or (pTop shl 16) or (pTop shl 8) or pTop
            outPixels[(height - 1) * width + x] = (0xFF shl 24) or (pBottom shl 16) or (pBottom shl 8) or pBottom
        }
        for (y in 0 until height) {
            val pLeft = pixels[y * width].coerceIn(0, 255)
            val pRight = pixels[y * width + width - 1].coerceIn(0, 255)
            outPixels[y * width] = (0xFF shl 24) or (pLeft shl 16) or (pLeft shl 8) or pLeft
            outPixels[y * width + width - 1] = (0xFF shl 24) or (pRight shl 16) or (pRight shl 8) or pRight
        }

        dest.setPixels(outPixels, 0, width, 0, 0, width, height)
        return dest
    }

    /**
     * Trims black scanning borders / scanner frame shadows that often fool OCR into
     * producing lines of garbage symbols ("||||", "=====", ".....").
     */
    fun trimScannerBorders(src: Bitmap): Bitmap {
        val width = src.width
        val height = src.height
        if (width < 200 || height < 200) return src

        val pixels = IntArray(width * height)
        src.getPixels(pixels, 0, width, 0, 0, width, height)

        var topCut = 0
        var bottomCut = 0
        var leftCut = 0
        var rightCut = 0

        val maxCropY = (height * 0.08f).toInt()
        val maxCropX = (width * 0.08f).toInt()

        // Check top black edge
        for (y in 0 until maxCropY) {
            var darkCount = 0
            for (x in 0 until width step 8) {
                val p = pixels[y * width + x]
                val lum = ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF)) / 3
                if (lum < 40) darkCount++
            }
            if (darkCount > (width / 8) * 0.70f) {
                topCut = y + 1
            } else break
        }

        // Check bottom black edge
        for (y in height - 1 downTo height - maxCropY) {
            var darkCount = 0
            for (x in 0 until width step 8) {
                val p = pixels[y * width + x]
                val lum = ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF)) / 3
                if (lum < 40) darkCount++
            }
            if (darkCount > (width / 8) * 0.70f) {
                bottomCut = height - y
            } else break
        }

        // Check left black edge
        for (x in 0 until maxCropX) {
            var darkCount = 0
            for (y in 0 until height step 8) {
                val p = pixels[y * width + x]
                val lum = ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF)) / 3
                if (lum < 40) darkCount++
            }
            if (darkCount > (height / 8) * 0.70f) {
                leftCut = x + 1
            } else break
        }

        // Check right black edge
        for (x in width - 1 downTo width - maxCropX) {
            var darkCount = 0
            for (y in 0 until height step 8) {
                val p = pixels[y * width + x]
                val lum = ((p shr 16 and 0xFF) + (p shr 8 and 0xFF) + (p and 0xFF)) / 3
                if (lum < 40) darkCount++
            }
            if (darkCount > (height / 8) * 0.70f) {
                rightCut = width - x
            } else break
        }

        val cropWidth = width - leftCut - rightCut
        val cropHeight = height - topCut - bottomCut

        return if (cropWidth > (width * 0.85f) && cropHeight > (height * 0.85f) && (topCut > 0 || bottomCut > 0 || leftCut > 0 || rightCut > 0)) {
            Bitmap.createBitmap(src, leftCut, topCut, cropWidth, cropHeight)
        } else {
            src
        }
    }

    /**
     * Rotates a bitmap by a specified angle in degrees (e.g., 90, 180, 270, or small skew angles).
     * Returns a new Bitmap instance.
     */
    fun rotateBitmap(src: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return src
        val matrix = android.graphics.Matrix().apply {
            postRotate(degrees)
        }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }

    /**
     * Deskews a bitmap if the measured angle exceeds threshold (|angle| > 0.8 degrees).
     */
    fun deskew(src: Bitmap, angleDegrees: Float): Bitmap {
        if (kotlin.math.abs(angleDegrees) < 0.8f || kotlin.math.abs(angleDegrees) > 45.0f) {
            return src
        }
        return rotateBitmap(src, -angleDegrees)
    }
}
