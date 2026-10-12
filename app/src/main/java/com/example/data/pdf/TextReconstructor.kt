package com.example.data.pdf

object TextReconstructor {

    private val LIGATURE_MAP = mapOf(
        "ﬁ" to "fi",
        "ﬂ" to "fl",
        "ﬀ" to "ff",
        "ﬃ" to "ffi",
        "ﬄ" to "ffl",
        "ﬅ" to "st",
        "ﬆ" to "st",
        "œ" to "oe",
        "æ" to "ae"
    )

    fun reconstruct(
        rawText: String,
        previousPageTrailingHyphen: String? = null,
        pageNumber: Int = 1
    ): List<String> {
        if (rawText.isBlank()) return emptyList()

        var text = rawText
        for ((lig, rep) in LIGATURE_MAP) {
            if (text.contains(lig)) {
                text = text.replace(lig, rep)
            }
        }

        val lines = text.lines()
        val cleanedLines = mutableListOf<String>()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                cleanedLines.add("")
                continue
            }

            if (isStandalonePageNumber(trimmed, pageNumber)) {
                continue
            }

            cleanedLines.add(trimmed)
        }

        val paragraphs = mutableListOf<String>()
        var currentPara = StringBuilder()

        if (previousPageTrailingHyphen != null && previousPageTrailingHyphen.isNotBlank()) {
            currentPara.append(previousPageTrailingHyphen)
        }

        for (i in cleanedLines.indices) {
            val line = cleanedLines[i]

            if (line.isEmpty()) {
                if (currentPara.isNotBlank()) {
                    paragraphs.add(currentPara.toString().trim())
                    currentPara = StringBuilder()
                }
                continue
            }

            if (isChapterHeading(line)) {
                if (currentPara.isNotBlank()) {
                    paragraphs.add(currentPara.toString().trim())
                    currentPara = StringBuilder()
                }
                paragraphs.add(line)
                continue
            }

            if (currentPara.isEmpty()) {
                currentPara.append(line)
            } else {
                val lastStr = currentPara.toString()
                if (lastStr.endsWith("-")) {
                    currentPara.setLength(currentPara.length - 1)
                    currentPara.append(line)
                } else {
                    currentPara.append(" ").append(line)
                }
            }
        }

        if (currentPara.isNotBlank()) {
            paragraphs.add(currentPara.toString().trim())
        }

        return paragraphs.filter { it.isNotBlank() }
    }

    fun isChapterHeading(line: String): Boolean {
        val trimmed = line.trim()
        val upper = trimmed.uppercase()
        if (upper.startsWith("CAPÍTULO") || upper.startsWith("CAPITULO") || upper.startsWith("CHAPTER")) {
            return true
        }
        if (upper.startsWith("CANTO ") || upper.startsWith("PARTE ") || upper.startsWith("PRÓLOGO") || upper.startsWith("PROLOGO")) {
            return true
        }
        if (trimmed.matches(Regex("^[IVXLCDM]+\\.\\s*.*$")) && trimmed.length < 50) {
            return true
        }
        return false
    }

    private fun isStandalonePageNumber(line: String, pageNumber: Int): Boolean {
        val trimmed = line.trim()
        if (trimmed == pageNumber.toString()) return true
        if (trimmed.matches(Regex("^-\\s*\\d+\\s*-$"))) return true
        if (trimmed.matches(Regex("^\\[\\s*\\d+\\s*\\]$"))) return true
        if (trimmed.matches(Regex("^pág\\.?\\s*\\d+$", RegexOption.IGNORE_CASE))) return true
        return false
    }
}
