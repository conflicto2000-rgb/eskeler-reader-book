package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class Book(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val filePath: String,
    val coverPath: String? = null,
    val coverDrawableRes: Int? = null,
    val totalPages: Int = 1,
    val totalParagraphs: Int = 0,
    val currentParagraphIndex: Int = 0,
    val currentPage: Int = 1,
    val progressPercent: Float = 0f,
    val isFavorite: Boolean = false,
    val lastReadTimestamp: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val isOcrProcessed: Boolean = false,
    val hasLowConfidencePages: Boolean = false,
    val ocrPageCount: Int = 0,
    val digitalPageCount: Int = 0
)

@Entity(
    tableName = "paragraphs",
    primaryKeys = ["bookId", "paragraphIndex"]
)
data class BookParagraphEntity(
    val bookId: Long,
    val paragraphIndex: Int,
    val pageNumber: Int,
    val chapterTitle: String,
    val text: String,
    val originalText: String = text,
    val extractionMethod: String = "DIGITAL", // "DIGITAL", "OCR", "HYBRID", "MANUAL"
    val qualityScore: Float = 1.0f,
    val isLowConfidence: Boolean = false,
    val isUserEdited: Boolean = false
)

data class ReadingStats(
    val totalBooks: Int,
    val totalFinished: Int,
    val totalReadingTimeMinutes: Int,
    val favoriteCount: Int
)

enum class ExtractionSource {
    DIGITAL, OCR, HYBRID, MANUAL
}
