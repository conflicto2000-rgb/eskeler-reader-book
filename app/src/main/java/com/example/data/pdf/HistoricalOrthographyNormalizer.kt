package com.example.data.pdf

/**
 * Normalizer for archaic spelling, historical orthography, ligatures,
 * long-s (ſ), and archaic abbreviations commonly found in 16th-19th century printed books.
 *
 * IMPORTANT DESIGN GOAL:
 * - Keeps original extracted text fully intact in [BookParagraphEntity.originalText].
 * - Provides non-destructive normalized text for TTS narration so speech engines
 *   (e.g. Spanish & English TTS) pronounce archaic characters naturally instead of stumbling.
 */
object HistoricalOrthographyNormalizer {

    // Common historical print ligatures and archaic typography symbols
    private val HISTORICAL_GLYPH_REPLACEMENTS = listOf(
        // Long 's' (ſ) common in books printed before the 19th century
        // e.g. "preſente" -> "presente", "eſtado" -> "estado", "cauſa" -> "causa"
        "ſ" to "s",
        "ẛ" to "s",

        // Historical ligatures
        "ﬆ" to "st",
        "ﬅ" to "ft",
        "ﬃ" to "ffi",
        "ﬄ" to "ffl",
        "ﬀ" to "ff",
        "ﬁ" to "fi",
        "ﬂ" to "fl",
        "ꝛ" to "r",  // r rotunda
        "æ" to "ae",
        "Æ" to "AE",
        "œ" to "oe",
        "Œ" to "OE",
        "&c." to "etc.",
        "&c" to "etc.",

        // Archaic tilde abbreviations on vowels (e.g. q̃ -> que, d̃ -> de)
        "q̃" to "que",
        "Q̃" to "Que",
        "q̄" to "que",
        "d̃" to "de",
        "m̃" to "men",
        "ñ" to "nn"
    )

    // Spanish Golden Age & 18th-century common orthography adaptations for TTS fluidity:
    // (e.g. "dexar" -> "dejar", "dixo" -> "dijo", "fazer" -> "hacer", "fabló" -> "habló")
    // When enabled as an optional normalization, it assists TTS in pronouncing archaic texts smoothly.
    private val SPANISH_ARCHAIC_PATTERNS = listOf(
        // 'x' for modern 'j' in classic Spanish verbal forms and nouns
        Regex("\\b(d)ix([a-zñáéíóú]+)\\b", RegexOption.IGNORE_CASE) to "$1ij$2",
        Regex("\\b(d)ex([a-zñáéíóú]+)\\b", RegexOption.IGNORE_CASE) to "$1ej$2",
        Regex("\\b(t)rax([a-zñáéíóú]+)\\b", RegexOption.IGNORE_CASE) to "$1raj$2",
        Regex("\\b(c)on[sſ]exo\\b", RegexOption.IGNORE_CASE) to "$1onsejo",
        Regex("\\b(e)xemplo\\b", RegexOption.IGNORE_CASE) to "$1jemplo",
        Regex("\\b(l)exos\\b", RegexOption.IGNORE_CASE) to "$1ejos",
        Regex("\\b(m)uger\\b", RegexOption.IGNORE_CASE) to "$1ujer",
        Regex("\\b(q)ual\\b", RegexOption.IGNORE_CASE) to "$1ual",
        Regex("\\b(q)uando\\b", RegexOption.IGNORE_CASE) to "$1uando",
        Regex("\\b(q)uanto\\b", RegexOption.IGNORE_CASE) to "$1uanto",
        Regex("\\b(y)glesia\\b", RegexOption.IGNORE_CASE) to "$1glesia",
        Regex("\\b(c)ibdad\\b", RegexOption.IGNORE_CASE) to "$1iudad"
    )

    /**
     * Replaces historical glyphs (long-s, ligatures, typography abbreviations)
     * without altering original words. Safe for all documents.
     */
    fun normalizeGlyphs(text: String): String {
        if (text.isBlank()) return text
        var result = text
        for ((glyph, replacement) in HISTORICAL_GLYPH_REPLACEMENTS) {
            result = result.replace(glyph, replacement)
        }
        return result
    }

    /**
     * Normalizes archaic spelling specifically for TTS narration fluency,
     * allowing modern speech synthesizers to pronounce classical texts naturally.
     */
    fun normalizeForNarration(text: String, language: OcrLanguage): String {
        var normalized = normalizeGlyphs(text)

        if (language == OcrLanguage.SPANISH || language == OcrLanguage.AUTO) {
            for ((pattern, replacement) in SPANISH_ARCHAIC_PATTERNS) {
                normalized = pattern.replace(normalized, replacement)
            }
        }

        // Clean typography dashes and double hyphens
        normalized = normalized.replace("—", " — ")
            .replace("--", "—")
            .replace(Regex("\\s+"), " ")
            .trim()

        return normalized
    }
}
