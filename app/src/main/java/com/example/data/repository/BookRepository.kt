package com.example.data.repository

import android.content.Context
import com.example.data.db.BookDao
import com.example.data.db.ParagraphDao
import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity
import com.example.data.pdf.HybridPdfEngine
import com.example.data.sample.SampleBooks
import kotlinx.coroutines.flow.Flow
import java.io.File

class BookRepository(
    private val bookDao: BookDao,
    private val paragraphDao: ParagraphDao
) {
    val allBooks: Flow<List<Book>> = bookDao.getAllBooks()
    val favoriteBooks: Flow<List<Book>> = bookDao.getFavoriteBooks()

    fun getBookById(id: Long): Flow<Book?> = bookDao.getBookById(id)

    suspend fun getBookByIdSync(id: Long): Book? = bookDao.getBookByIdSync(id)

    fun getParagraphsForBook(bookId: Long): Flow<List<BookParagraphEntity>> =
        paragraphDao.getParagraphsForBook(bookId)

    suspend fun getParagraphsForBookSync(bookId: Long): List<BookParagraphEntity> =
        paragraphDao.getParagraphsForBookSync(bookId)

    suspend fun ensureInitialSampleBooks() {
        val count = bookDao.getBookCount()
        if (count == 0) {
            val samples = SampleBooks.getInitialBooks()
            for ((book, paragraphs) in samples) {
                val newBookId = bookDao.insertBook(book)
                val mappedParas = paragraphs.map { it.copy(bookId = newBookId) }
                paragraphDao.insertParagraphs(mappedParas)
            }
        }
    }

    suspend fun insertImportedBook(
        book: Book,
        paragraphs: List<BookParagraphEntity>
    ): Long {
        val newBookId = bookDao.insertBook(book)
        val mappedParas = paragraphs.map { it.copy(bookId = newBookId) }
        paragraphDao.insertParagraphs(mappedParas)
        return newBookId
    }

    suspend fun updateReadingProgress(
        bookId: Long,
        paragraphIndex: Int,
        page: Int,
        progressPercent: Float
    ) {
        bookDao.updateReadingProgress(
            bookId = bookId,
            paragraphIndex = paragraphIndex,
            page = page,
            progressPercent = progressPercent
        )
    }

    suspend fun updateParagraphText(
        bookId: Long,
        paragraphIndex: Int,
        newText: String
    ) {
        paragraphDao.updateParagraphText(bookId, paragraphIndex, newText)
    }

    suspend fun updateParagraph(paragraph: BookParagraphEntity) {
        paragraphDao.updateParagraph(paragraph)
    }

    suspend fun reprocessPageWithOcr(
        context: Context,
        book: Book,
        pageNumber: Int,
        language: com.example.data.pdf.OcrLanguage = com.example.data.pdf.OcrLanguage.SPANISH,
        forcedPreprocessing: com.example.data.pdf.PreprocessingMode? = null
    ): List<BookParagraphEntity> {
        val newParas = HybridPdfEngine.reprocessSinglePageWithOcr(
            context = context,
            pdfFilePath = book.filePath,
            pageNumber = pageNumber,
            bookId = book.id,
            language = language,
            forcedPreprocessing = forcedPreprocessing
        )

        if (newParas.isNotEmpty()) {
            // Retrieve all current paragraphs for this book
            val allCurrent = paragraphDao.getParagraphsForBookSync(book.id).toMutableList()
            // Replace paragraphs for this page
            allCurrent.removeAll { it.pageNumber == pageNumber }
            allCurrent.addAll(newParas)
            // Re-index all paragraphs to keep sequential indices
            val reIndexed = allCurrent.sortedWith(
                compareBy<BookParagraphEntity> { it.pageNumber }.thenBy { it.paragraphIndex }
            ).mapIndexed { idx, p -> p.copy(paragraphIndex = idx) }

            // Replace in database
            paragraphDao.deleteParagraphsForBook(book.id)
            paragraphDao.insertParagraphs(reIndexed)

            val hasLowConf = reIndexed.any { it.isLowConfidence }
            bookDao.updateBookProcessingStats(
                bookId = book.id,
                totalParas = reIndexed.size,
                hasLowConf = hasLowConf,
                ocrPages = book.ocrPageCount + 1,
                digitalPages = (book.digitalPageCount - 1).coerceAtLeast(0),
                isOcr = true
            )
            return reIndexed
        }
        return paragraphDao.getParagraphsForBookSync(book.id)
    }

    suspend fun toggleFavorite(bookId: Long, isFavorite: Boolean) {
        bookDao.toggleFavorite(bookId, isFavorite)
    }

    suspend fun deleteBook(book: Book) {
        paragraphDao.deleteParagraphsForBook(book.id)
        bookDao.deleteBook(book)
        // Clean up cover image if created
        book.coverPath?.let { path ->
            try {
                val file = File(path)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
        // Clean up PDF copy
        if (!book.filePath.startsWith("sample://")) {
            try {
                val file = File(book.filePath)
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
        }
    }
}
