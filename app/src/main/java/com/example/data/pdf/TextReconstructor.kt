package com.example.data.pdf

import java.util.regex.Pattern

object TextReconstructor {

    // Common ligatures from PDF typesetting
    private val LIGATURE_MAP = mapOf(
        "ﬁ" to "fi",
        "ﬂ" to "fl",
        "ﬀ" to "ff",
        "ﬃ" to "ffi",
        "ﬄ" to "ffl",
        "œ" to "oe",
        "Œ" to "OE",
        "æ" to "ae",
        "Æ" to "AE",
        "‘" to "'",
        "’" to "'",
        "“" to "\"",
        "”" to "\"",
        "–" to "-",
        "—" to "—"
    )

    /**
     * Cleans and reconstructs raw extracted text into fluid readable paragraphs for TTS.
     */
    fun reconstruct(
        rawText: String,
        documentTitle: String? = null,
        pageNumber: Int = 1
    ): List<String> {
        if (rawText.isBlank()) return emptyList()

        // 1. Normalize typography ligatures
        var processed = rawText
        for ((ligature, replacement) in LIGATURE_MAP) {
            processed = processed.replace(ligature, replacement)
        }

        // 2. Un-hyphenate words broken at line ends:
        // Example: "signifi- \n cativo" -> "significativo"
        // Example: "inter- \r\n sección" -> "intersección"
        // Matches lowercase letter, hyphen, whitespace/newline, lowercase letter
        processed = processed.replace(Regex("([a-záéíóúüñA-ZÁÉÍÓÚÜÑ])-\\s*[\r\n]+\\s*([a-záéíóúüñ])"), "$1$2")

        // 3. Remove standalone running page numbers and headers
        val lines = processed.split("\n").map { it.trim() }
        val cleanedLines = mutableListOf<String>()

        val titleClean = documentTitle?.trim()?.lowercase()

        for (line in lines) {
            if (line.isBlank()) {
                cleanedLines.add("")
                continue
            }

            // Detect standalone page numbers at top or bottom (e.g. "12", "- 12 -", "Pág. 12", "[12]")
            if (isStandalonePageNumber(line, pageNumber)) {
                continue
            }

            // Detect repeated running headers that match the book title or author
            if (titleClean != null && titleClean.length > 4) {
                val lowerLine = line.lowercase()
                if (lowerLine == titleClean || (lowerLine.contains(titleClean) && line.length < titleClean.length + 15)) {
                    continue
                }
            }

            cleanedLines.add(line)
        }

        // 4. Group lines into natural paragraphs based on blank lines or indentation
        val rawParagraphs = mutableListOf<StringBuilder>()
        var currentPara = StringBuilder()

        for (line in cleanedLines) {
            if (line.isBlank()) {
                if (currentPara.isNotBlank()) {
                    rawParagraphs.add(currentPara)
                    currentPara = StringBuilder()
                }
            } else {
                if (currentPara.isNotEmpty()) {
                    val lastChar = currentPara.trimEnd().last()
                    // If previous line ends with a period, question, or exclamation mark, and the next starts with capital letter,
                    // it might be a new paragraph, but if it was just a line break within a sentence, join with space.
                    currentPara.append(" ").append(line)
                } else {
                    currentPara.append(line)
                }
            }
        }
        if (currentPara.isNotBlank()) {
            rawParagraphs.add(currentPara)
        }

        // 5. Clean whitespace within paragraphs, eliminate OCR duplicates, and filter noise
        val finalParagraphs = mutableListOf<String>()
        val seenSignatures = mutableSetOf<String>()

        for (sb in rawParagraphs) {
            val text = sb.toString()
                .replace(Regex("\\s+"), " ")
                .replace(Regex("([¿¡])\\s+"), "$1") // Clean spaces after ¿ and ¡
                .replace(Regex("\\s+([.,;:?!»])"), "$1") // Clean spaces before punctuation
                .trim()

            // Skip tiny artifacts (< 4 chars without letters)
            if (text.length < 4 && !text.any { it.isLetter() }) continue

            // Deduplication (prevents double reading if OCR generated overlapping bounding boxes)
            val signature = text.lowercase().take(60)
            if (signature.length >= 20 && seenSignatures.contains(signature)) {
                continue
            }
            if (signature.length >= 20) {
                seenSignatures.add(signature)
            }

            finalParagraphs.add(text)
        }

        return finalParagraphs
    }

    private fun isStandalonePageNumber(line: String, pageNumber: Int): Boolean {
        val trimmed = line.trim()
        // Simple digit: "14", "14.", "- 14 -", "~ 14 ~"
        if (trimmed.matches(Regex("^[-~—]?\\s*\\d{1,4}\\s*[-~—.]?$"))) return true
        // "Pág. 14", "Página 14", "Page 14"
        if (trimmed.matches(Regex("^(pág\\.?|página|page|pag\\.?)\\s*\\d{1,4}$", RegexOption.IGNORE_CASE))) return true
        // If it equals the exact current page number
        if (trimmed == pageNumber.toString()) return true
        return false
    }

    /**
     * Detects if text is a chapter or section heading.
     */
    fun isChapterHeading(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length > 70) return false
        val patterns = listOf(
            Regex("^(capítulo|capitulo|chapter|cap\\.?)\\s+([0-9ivxlcdm]+|uno|dos|tres|cuatro|cinco|primero|segundo)", RegexOption.IGNORE_CASE),
            Regex("^(prólogo|prologo|epílogo|epilogo|introducción|introduccion|prefacio|preface|índice|indice)", RegexOption.IGNORE_CASE),
            Regex("^[IVXLCDM]+\\.\\s+[A-ZÁÉÍÓÚÑ]"),
            Regex("^(PARTE|LIBRO|SECCIÓN|SECCION)\\s+([0-9ivxlcdm]+|uno|dos)", RegexOption.IGNORE_CASE)
        )
        return patterns.any { it.containsMatchIn(trimmed) }
    }
}
