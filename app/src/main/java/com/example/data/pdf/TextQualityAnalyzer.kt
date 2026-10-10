package com.example.data.pdf

data class QualityReport(
    val score: Float,
    val isCorrupted: Boolean,
    val issues: List<String>,
    val legitimateWordRatio: Float,
    val totalWords: Int,
    val totalChars: Int
)

object TextQualityAnalyzer {

    // Common vowels in Spanish, English, and Latin languages (accented included)
    private val VOWELS = setOf('a', 'e', 'i', 'o', 'u', 'á', 'é', 'í', 'ó', 'ú', 'ü', 'y',
                               'A', 'E', 'I', 'O', 'U', 'Á', 'É', 'Í', 'Ó', 'Ú', 'Ü', 'Y')

    // Common Spanish / English abbreviations and symbols that are legitimate
    private val LEGITIMATE_SHORT_TOKENS = setOf(
        "pág", "pag", "vol", "cap", "art", "dr", "dra", "sr", "sra", "etc", "vs", "km", "kg", "cm", "mm",
        "no", "nº", "núm", "num", "ed", "op", "cit", "ibíd", "ibid", "fig", "ej", "apdo", "ee.uu", "eeuu"
    )

    fun evaluate(text: String): QualityReport {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return QualityReport(
                score = 0.0f,
                isCorrupted = true,
                issues = listOf("Texto vacío"),
                legitimateWordRatio = 0.0f,
                totalWords = 0,
                totalChars = 0
            )
        }

        val issues = mutableListOf<String>()
        var scoreDeduction = 0.0f

        val totalChars = trimmed.length
        val words = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        val totalWords = words.size

        if (totalWords < 3 && totalChars < 25) {
            return QualityReport(
                score = 0.35f,
                isCorrupted = false,
                issues = listOf("Texto muy corto (posible pie de página o título aislado)"),
                legitimateWordRatio = 0.5f,
                totalWords = totalWords,
                totalChars = totalChars
            )
        }

        // 1. Check for Unicode replacement character and non-printable control characters
        val replacementCharCount = trimmed.count { it == '\uFFFD' || it == '\u0000' || (it.code in 1..8) || (it.code in 14..31) }
        val replacementRatio = replacementCharCount.toFloat() / totalChars.toFloat()
        if (replacementRatio > 0.02f) {
            issues.add("Presencia de caracteres de reemplazo o bytes no imprimibles (${(replacementRatio * 100).toInt()}%)")
            scoreDeduction += (replacementRatio * 10).coerceAtMost(0.5f)
        }

        // Check for leaked PDF internal dictionary syntax tokens
        val pdfSyntaxTokens = listOf("/type", "/font", "/xobject", "/procset", "endobj", "endstream", "/mediabox", "/length", "/filter", "/flatedecode", "/cff", "/cidfont")
        val lowerText = trimmed.lowercase()
        val leakedTokenCount = pdfSyntaxTokens.count { lowerText.contains(it) }
        if (leakedTokenCount >= 2) {
            issues.add("Fragmentos de sintaxis interna o metadatos de PDF detectados")
            scoreDeduction += 0.60f
        }

        // 2. Analyze words for legitimate pronounceability (vowel/consonant balance)
        var legitimateWords = 0
        var unpronounceableWords = 0
        var mixedAlphaNumWords = 0
        var overlyLongWords = 0

        for (word in words) {
            val cleanWord = word.trim { !it.isLetterOrDigit() }
            if (cleanWord.isEmpty()) continue

            // Allow valid numbers, dates, Roman numerals, abbreviations
            if (isLegitimateNumericOrSymbolic(cleanWord)) {
                legitimateWords++
                continue
            }

            // Detect mixed letters and digits (font encoding corruption, e.g. "c0rr1d0", "f0nt4")
            if (hasMixedLettersAndDigits(cleanWord)) {
                mixedAlphaNumWords++
                continue
            }

            // Word length check (strings longer than 35 without hyphens are typically corrupt streams)
            if (cleanWord.length > 35) {
                overlyLongWords++
                continue
            }

            // Pronounceability check (must have at least one vowel if word is > 1 char, except legitimate acronyms)
            val hasVowel = cleanWord.any { it in VOWELS }
            val onlyConsonants = cleanWord.length >= 3 && !hasVowel

            // Consecutive consonant cluster (> 5 consecutive consonants is abnormal in Spanish/English prose)
            val hasExtremeConsonantCluster = hasTooManyConsecutiveConsonants(cleanWord)

            if (onlyConsonants || hasExtremeConsonantCluster) {
                unpronounceableWords++
            } else {
                legitimateWords++
            }
        }

        val legitimateRatio = if (totalWords > 0) legitimateWords.toFloat() / totalWords.toFloat() else 0.0f

        if (mixedAlphaNumWords > totalWords * 0.10f) {
            issues.add("Palabras con números intercalados por fuente defectuosa ($mixedAlphaNumWords palabras)")
            scoreDeduction += 0.35f
        }

        if (unpronounceableWords > totalWords * 0.15f) {
            issues.add("Secuencias de caracteres no pronunciables ($unpronounceableWords palabras sin vocales o cadenas incoherentes)")
            scoreDeduction += 0.40f
        }

        if (overlyLongWords > 2) {
            issues.add("Palabras anormalmente largas sin espacios ($overlyLongWords palabras)")
            scoreDeduction += 0.20f
        }

        // 3. Check symbol vs letter ratio
        val letterCount = trimmed.count { it.isLetter() }
        val letterRatio = letterCount.toFloat() / totalChars.toFloat()
        if (letterRatio < 0.45f && totalChars > 40) {
            issues.add("Baja proporción de letras respecto a símbolos (${(letterRatio * 100).toInt()}%)")
            scoreDeduction += 0.30f
        }

        // 4. Repeated consecutive token sequences (OCR duplicate loops or stream stutter)
        val hasStutter = hasRepeatedNgrams(words)
        if (hasStutter) {
            issues.add("Repetición anormal de frases o palabras")
            scoreDeduction += 0.20f
        }

        val finalScore = (1.0f - scoreDeduction).coerceIn(0.0f, 1.0f)
        val isCorrupted = finalScore < 0.65f || legitimateRatio < 0.60f

        return QualityReport(
            score = finalScore,
            isCorrupted = isCorrupted,
            issues = issues,
            legitimateWordRatio = legitimateRatio,
            totalWords = totalWords,
            totalChars = totalChars
        )
    }

    private fun isLegitimateNumericOrSymbolic(token: String): Boolean {
        val lower = token.lowercase()
        if (lower in LEGITIMATE_SHORT_TOKENS) return true

        // Pure numbers, currencies, percentages, dates, Roman numerals (e.g. 1998, $45.00, 25%, 12/05/2020, XIV)
        if (token.all { it.isDigit() || it in ".,/%$-+:°" }) return true
        if (token.matches(Regex("^[IVXLCDMivxlcdm]+$"))) return true // Roman numerals
        return false
    }

    private fun hasMixedLettersAndDigits(token: String): Boolean {
        val hasLetter = token.any { it.isLetter() }
        val hasDigit = token.any { it.isDigit() }
        if (!hasLetter || !hasDigit) return false

        // Exception for legitimate terms: e.g. 3D, 4K, CO2, H2O, MP3, COVID-19, 1ª, 2º
        val lower = token.lowercase()
        if (lower.matches(Regex("^[0-9]+[ªº]$"))) return false
        if (lower.matches(Regex("^(3d|4k|mp3|mp4|co2|h2o|covid-19|v[0-9]+)$"))) return false

        return true
    }

    private fun hasTooManyConsecutiveConsonants(word: String): Boolean {
        var consecutiveConsonants = 0
        for (char in word) {
            if (char.isLetter()) {
                if (char in VOWELS) {
                    consecutiveConsonants = 0
                } else {
                    consecutiveConsonants++
                    if (consecutiveConsonants >= 5) return true
                }
            } else {
                consecutiveConsonants = 0
            }
        }
        return false
    }

    private fun hasRepeatedNgrams(words: List<String>): Boolean {
        if (words.size < 6) return false
        for (i in 0 until words.size - 4) {
            val tri = words[i] + " " + words[i + 1] + " " + words[i + 2]
            val nextTri = words[i + 3] + " " + words[i + 4] + " " + (words.getOrNull(i + 5) ?: "")
            if (tri.equals(nextTri, ignoreCase = true) && tri.length > 8) {
                return true
            }
        }
        return false
    }
}
