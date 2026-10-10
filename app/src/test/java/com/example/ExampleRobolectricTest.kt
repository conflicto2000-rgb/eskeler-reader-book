package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.pdf.TextQualityAnalyzer
import com.example.data.pdf.TextReconstructor
import com.example.data.sample.SampleBooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    val appName = context.getString(R.string.app_name)
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
  fun `text reconstructor filters standalone page numbers`() {
    val rawWithPageNumber = "12\nEste es el contenido de la página.\n- 12 -"
    val reconstructed = TextReconstructor.reconstruct(rawWithPageNumber, pageNumber = 12)
    val combined = reconstructed.joinToString(" ")
    assertFalse("Should not read standalone 12", combined.startsWith("12"))
    assertFalse("Should not read - 12 -", combined.endsWith("- 12 -"))
    assertTrue("Should keep legitimate text", combined.contains("Este es el contenido"))
  }

  @Test
  fun `text reconstructor detects chapter headings`() {
    assertTrue(TextReconstructor.isChapterHeading("Capítulo I: El Comienzo"))
    assertTrue(TextReconstructor.isChapterHeading("CAPÍTULO 4: LA AVENTURA"))
    assertTrue(TextReconstructor.isChapterHeading("Prólogo del Autor"))
    assertFalse(TextReconstructor.isChapterHeading("Este es un párrafo normal que no es un encabezado."))
  }

  @Test
  fun `ocr engine status check returns ready status`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val status = com.example.data.pdf.OcrProcessor.checkEngineStatus(context)
    // Verify OCR status provides clear diagnostic info
    assertTrue("Engine should report name", status.engineName.isNotBlank())
    assertTrue("Engine should specify Spanish support", status.supportedLanguagesSummary.contains("Español"))
  }

  @Test
  fun `historical orthography normalizer normalizes long-s and classical ligatures`() {
    val archaicText = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme, no ha mucho tiempo que viuía vn hidalgo de los de lança en aſtillero, adarga antigua, roçín flaco y galgo corredor."
    val normalizedGlyphs = com.example.data.pdf.HistoricalOrthographyNormalizer.normalizeGlyphs(archaicText)
    assertTrue("Long-s (aſtillero) should become astillero", normalizedGlyphs.contains("astillero"))

    val narratedText = com.example.data.pdf.HistoricalOrthographyNormalizer.normalizeForNarration(
      text = "El rey dixo a su consejo que dexasen las armas",
      language = com.example.data.pdf.OcrLanguage.SPANISH
    )
    assertTrue("dixo should normalize to dijo for TTS", narratedText.contains("dijo"))
    assertTrue("dexasen should normalize to dejasen for TTS", narratedText.contains("dejasen"))
  }

  @Test
  fun `image preprocessor vintage and binarization filters work on bitmaps`() {
    val bmp = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bmp)
    // Draw yellowish paper background (high R, high G, low B)
    canvas.drawColor(android.graphics.Color.rgb(240, 230, 180))

    val vintageResult = com.example.data.pdf.ImagePreprocessor.preprocess(bmp, com.example.data.pdf.PreprocessingMode.VINTAGE_BOOK)
    org.junit.Assert.assertNotNull(vintageResult)
    org.junit.Assert.assertEquals(100, vintageResult.width)
    org.junit.Assert.assertEquals(100, vintageResult.height)

    val binarizedResult = com.example.data.pdf.ImagePreprocessor.preprocess(bmp, com.example.data.pdf.PreprocessingMode.ADAPTIVE_BINARIZATION)
    org.junit.Assert.assertNotNull(binarizedResult)

    val sharpenResult = com.example.data.pdf.ImagePreprocessor.preprocess(bmp, com.example.data.pdf.PreprocessingMode.SHARPEN_FINE)
    org.junit.Assert.assertNotNull(sharpenResult)

    bmp.recycle()
    vintageResult.recycle()
    binarizedResult.recycle()
    sharpenResult.recycle()
  }
}
