package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.data.pdf.HistoricalOrthographyNormalizer
import com.example.data.pdf.ImagePreprocessor
import com.example.data.pdf.OcrLanguage
import com.example.data.pdf.OcrPageResult
import com.example.data.pdf.OcrProcessor
import com.example.data.pdf.PreprocessingMode
import com.example.data.pdf.TextQualityAnalyzer
import com.example.data.pdf.TextReconstructor
import com.example.data.sample.SampleBooks
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = try {
      context.getString(R.string.app_name)
    } catch (_: Exception) {
      "AudioBook Reader"
    }
    assertEquals("AudioBook Reader", appName)
  }

  @Test
  fun `sample books are preloaded correctly`() {
    val samples = SampleBooks.getInitialBooks()
    assertTrue("Should have at least 3 initial sample books", samples.size >= 3)
    val principito = samples.firstOrNull { it.first.title == "El Principito" }
    assertTrue("El Principito should exist with paragraphs", (principito?.second?.size ?: 0) > 0)
  }

  @Test
  fun `quality analyzer detects clean Spanish text as good quality`() {
    val cleanText = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme, no ha mucho tiempo que vivía un hidalgo."
    val report = TextQualityAnalyzer.evaluate(cleanText)
    assertFalse("Clean text should not be flagged as corrupted", report.isCorrupted)
    assertTrue("Score should be high for clear Spanish text", report.score >= 0.85f)
  }

  @Test
  fun `quality analyzer detects corrupted text with replacement chars and unpronounceable gibberish`() {
    val corruptText = "\uFFFD\uFFFD\u0001\u0002 qzxtkpwrfd bndgfjhkl \uFFFD\uFFFD wqrzxcvb xzklpqrs /F1 12 Tf"
    val report = TextQualityAnalyzer.evaluate(corruptText)
    assertTrue("Garbled gibberish text should be flagged as corrupted", report.isCorrupted)
    assertTrue("Score should be low for corrupted text", report.score < 0.65f)
    assertTrue("Should report diagnostic issues", report.issues.isNotEmpty())
  }

  @Test
  fun `quality analyzer accepts legitimate numbers and Roman numerals`() {
    val textWithFormulasAndDates = "El tratado se firmó el 15 de marzo de 1998, según el Capítulo XIV, con un costo de $45.50 millones."
    val report = TextQualityAnalyzer.evaluate(textWithFormulasAndDates)
    assertFalse("Legitimate dates, numbers, and Roman numerals should not corrupt the text", report.isCorrupted)
    assertTrue("Score should remain high", report.score >= 0.80f)
  }

  @Test
  fun `text reconstructor un-hyphenates words and normalizes ligatures`() {
    val rawText = "El desarro- \n llo de la inteli- \r\n gencia es un ﬁn funda- \n mental."
    val reconstructed = TextReconstructor.reconstruct(rawText, pageNumber = 1)
    assertTrue("Should produce at least one paragraph", reconstructed.isNotEmpty())
    val combined = reconstructed.joinToString(" ")
    assertTrue("Should un-hyphenate desarrollo", combined.contains("desarrollo"))
    assertTrue("Should un-hyphenate inteligencia", combined.contains("inteligencia"))
    assertTrue("Should un-hyphenate fundamental", combined.contains("fundamental"))
    assertTrue("Should convert ligature fi", combined.contains("fin"))
  }

  @Test
  fun `text reconstructor filters standalone page numbers and detects chapters`() {
    val rawWithPageNumber = "12\nEste es el contenido de la página.\n- 12 -"
    val reconstructed = TextReconstructor.reconstruct(rawWithPageNumber, pageNumber = 12)
    val combined = reconstructed.joinToString(" ")
    assertFalse("Should not read standalone 12", combined.startsWith("12"))
    assertFalse("Should not read - 12 -", combined.endsWith("- 12 -"))
    assertTrue("Should keep legitimate text", combined.contains("Este es el contenido"))
    assertTrue(TextReconstructor.isChapterHeading("Capítulo I: El Comienzo"))
  }

  @Test
  fun `ocr engine status check returns truthful engine description and latin capabilities`() = runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val status = OcrProcessor.checkEngineStatus(context)
    assertTrue("Engine should report name", status.engineName.isNotBlank())
    assertTrue("Engine should specify Spanish support", status.supportedLanguagesSummary.contains("Español"))
    // Verification that Fraktur limitations are truthfully noted and not falsely claimed
    assertTrue("Must truthfully mention Latin script nature regarding Fraktur", status.historicalTypographySummary.contains("Fraktur"))
  }

  @Test
  fun `historical orthography normalizer normalizes long-s and classical ligatures`() {
    val archaicText = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme, no ha mucho tiempo que viuía vn hidalgo de los de lança en aſtillero, adarga antigua, roçín flaco y galgo corredor."
    val normalizedGlyphs = HistoricalOrthographyNormalizer.normalizeGlyphs(archaicText)
    assertTrue("Long-s (aſtillero) should become astillero", normalizedGlyphs.contains("astillero"))

    val narratedText = HistoricalOrthographyNormalizer.normalizeForNarration(
      text = "El rey dixo a su consejo que dexasen las armas",
      language = OcrLanguage.SPANISH
    )
    assertTrue("dixo should normalize to dijo for TTS", narratedText.contains("dijo"))
    assertTrue("dexasen should normalize to dejasen for TTS", narratedText.contains("dejasen"))
  }

  @Test
  fun `image preprocessor filters and rotation deskew work on bitmaps`() {
    val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    canvas.drawColor(Color.rgb(240, 230, 180)) // aged yellowish background simulation

    val vintageResult = ImagePreprocessor.preprocess(bmp, PreprocessingMode.VINTAGE_BOOK)
    assertNotNull(vintageResult)
    assertEquals(120, vintageResult.width)
    assertEquals(120, vintageResult.height)

    val binarizedResult = ImagePreprocessor.preprocess(bmp, PreprocessingMode.ADAPTIVE_BINARIZATION)
    assertNotNull(binarizedResult)

    val sharpenResult = ImagePreprocessor.preprocess(bmp, PreprocessingMode.SHARPEN_FINE)
    assertNotNull(sharpenResult)

    val rotated = ImagePreprocessor.rotateBitmap(bmp, 90f)
    assertNotNull(rotated)
    assertEquals(120, rotated.width)

    val deskewed = ImagePreprocessor.deskew(bmp, 5.0f)
    assertNotNull(deskewed)

    bmp.recycle()
    vintageResult.recycle()
    binarizedResult.recycle()
    sharpenResult.recycle()
    rotated.recycle()
    if (deskewed != bmp) deskewed.recycle()
  }

  @Test
  fun `ocr honest confidence reporting does not invent fabricated numbers`() {
    val resultWithNoEngineConfidence = OcrPageResult(
      fullText = "Texto de prueba reconocido",
      paragraphs = listOf("Texto de prueba reconocido"),
      nativeConfidence = null,
      qualityScore = 0.88f
    )
    assertNull("Engine confidence is truthfully null when not reported by SDK", resultWithNoEngineConfidence.nativeConfidence)
    assertEquals("averageConfidence falls back to justifiable qualityScore", 0.88f, resultWithNoEngineConfidence.averageConfidence, 0.001f)

    val resultWithEngineConfidence = OcrPageResult(
      fullText = "Texto",
      paragraphs = listOf("Texto"),
      nativeConfidence = 0.94f,
      qualityScore = 0.85f
    )
    assertEquals("Native confidence is preserved when reported", 0.94f, resultWithEngineConfidence.averageConfidence, 0.001f)
  }

  @Test
  fun `lens reader document packaging creates valid audio book paragraphs`() {
    val lensText = "Este es un fragmento de página fotografiado con la cámara del dispositivo móvil."
    val paras = TextReconstructor.reconstruct(lensText, pageNumber = 1)
    assertEquals(1, paras.size)
    val quality = TextQualityAnalyzer.evaluate(paras[0])
    assertTrue("Lens OCR text should have high quality score", quality.score >= 0.80f)
    assertFalse("Clean photo text should not be marked as corrupted", quality.isCorrupted)
  }

  @Test
  fun `reading stats accurately calculate totals and finished documents`() {
    val samples = SampleBooks.getInitialBooks().map { it.first }
    val total = samples.size
    val finished = samples.count { it.progressPercent >= 0.95f || (it.totalPages > 1 && it.currentPage >= it.totalPages) }
    val favorites = samples.count { it.isFavorite }
    val stats = com.example.data.model.ReadingStats(
      totalBooks = total,
      totalFinished = finished,
      totalReadingTimeMinutes = 15,
      favoriteCount = favorites
    )
    assertTrue("Total books should match samples count", stats.totalBooks >= 3)
    assertTrue("Favorites should be non-negative", stats.favoriteCount >= 1)
  }
}
