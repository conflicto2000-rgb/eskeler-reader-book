package com.example.data.pdf

object HistoricalOrthographyNormalizer {

    private val GLYPH_REPLACEMENTS = listOf(
        // Long-s variants
        "ſ" to "s",
        "ẛ" to "s",
        "ﬅ" to "st",
        "ﬆ" to "st",

        // Historical Latin / Spanish ligatures
        "æ" to "ae",
        "Æ" to "Ae",
        "œ" to "oe",
        "Œ" to "Oe",
        "ﬀ" to "ff",
        "ﬁ" to "fi",
        "ﬂ" to "fl",
        "ﬃ" to "ffi",
        "ﬄ" to "ffl",

        // Archaic tilde abbreviations (e.g. q̃ -> que)
        "q̃" to "que",
        "Q̃" to "Que",
        "p̃" to "para",
        "d̃" to "de"
    )

    private val SPANISH_ARCHAIC_PATTERNS = listOf(
        Regex("\\b(d)ix([a-zñáéíóú]+)\\b", RegexOption.IGNORE_CASE) to "$1ij$2",
        Regex("\\b(d)ex([a-zñáéíóú]+)\\b", RegexOption.IGNORE_CASE) to "$1ej$2",
        Regex("\\b(t)rax([a-zñáéíóú]+)\\b", RegexOption.IGNORE_CASE) to "$1raj$2",
        Regex("\\b(f)a(z|ç)er\\b", RegexOption.IGNORE_CASE) to "$1acer",
        Regex("\\b(f)a(z|ç)e\\b", RegexOption.IGNORE_CASE) to "$1ace",
        Regex("\\b(f)a(z|ç)en\\b", RegexOption.IGNORE_CASE) to "$1acen",
        Regex("\\b(f)echo\\b", RegexOption.IGNORE_CASE) to "hecho",
        Regex("\\b(f)abl([a-zñáéíóú]+)\\b", RegexOption.IGNORE_CASE) to "habl$2",
        Regex("\\b(f)all([a-zñáéíóú]+)\\b", RegexOption.IGNORE_CASE) to "hall$2",
        Regex("\\b(v)n([a-zñáéíóú]*)\\b", RegexOption.IGNORE_CASE) to "$1n$2",
        Regex("\\b(v)no\\b", RegexOption.IGNORE_CASE) to "uno",
        Regex("\\b(v)na\\b", RegexOption.IGNORE_CASE) to "una",
        Regex("\\b(v)nos\\b", RegexOption.IGNORE_CASE) to "unos",
        Regex("\\b(v)nas\\b", RegexOption.IGNORE_CASE) to "unas",
        Regex("\\b(v)niversidad\\b", RegexOption.IGNORE_CASE) to "universidad",
        Regex("\\b(v)no(s)?\\b", RegexOption.IGNORE_CASE) to "uno$2"
    )

    fun normalizeGlyphs(text: String): String {
        var result = text
        for ((from, to) in GLYPH_REPLACEMENTS) {
            if (result.contains(from)) {
                result = result.replace(from, to)
            }
        }
        return result
    }

    fun normalizeForNarration(text: String, language: OcrLanguage = OcrLanguage.SPANISH): String {
        var processed = normalizeGlyphs(text)
        if (language == OcrLanguage.SPANISH) {
            for ((pattern, replacement) in SPANISH_ARCHAIC_PATTERNS) {
                processed = pattern.replace(processed, replacement)
            }
        }
        return processed
    }
}
