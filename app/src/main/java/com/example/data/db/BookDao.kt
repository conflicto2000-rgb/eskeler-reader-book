package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC")
    fun getAllBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookById(id: Long): Flow<Book?>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookByIdSync(id: Long): Book?

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY lastReadTimestamp DESC")
    fun getFavoriteBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books ORDER BY lastReadTimestamp DESC LIMIT :limit")
    fun getRecentBooks(limit: Int): Flow<List<Book>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Delete
    suspend fun deleteBook(book: Book)

    @Query("UPDATE books SET currentParagraphIndex = :paragraphIndex, currentPage = :page, progressPercent = :progressPercent, lastReadTimestamp = :timestamp WHERE id = :bookId")
    suspend fun updateReadingProgress(
        bookId: Long,
        paragraphIndex: Int,
        page: Int,
        progressPercent: Float,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :bookId")
    suspend fun toggleFavorite(bookId: Long, isFavorite: Boolean)

    @Query("UPDATE books SET totalParagraphs = :totalParas, hasLowConfidencePages = :hasLowConf, ocrPageCount = :ocrPages, digitalPageCount = :digitalPages, isOcrProcessed = :isOcr WHERE id = :bookId")
    suspend fun updateBookProcessingStats(
        bookId: Long,
        totalParas: Int,
        hasLowConf: Boolean,
        ocrPages: Int,
        digitalPages: Int,
        isOcr: Boolean
    )

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int
}

@Dao
interface ParagraphDao {
    @Query("SELECT * FROM paragraphs WHERE bookId = :bookId ORDER BY paragraphIndex ASC")
    fun getParagraphsForBook(bookId: Long): Flow<List<BookParagraphEntity>>

    @Query("SELECT * FROM paragraphs WHERE bookId = :bookId ORDER BY paragraphIndex ASC")
    suspend fun getParagraphsForBookSync(bookId: Long): List<BookParagraphEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParagraphs(paragraphs: List<BookParagraphEntity>)

    @Update
    suspend fun updateParagraph(paragraph: BookParagraphEntity)

    @Query("UPDATE paragraphs SET text = :text, isUserEdited = 1 WHERE bookId = :bookId AND paragraphIndex = :paragraphIndex")
    suspend fun updateParagraphText(bookId: Long, paragraphIndex: Int, text: String)

    @Query("SELECT * FROM paragraphs WHERE bookId = :bookId AND pageNumber = :pageNumber ORDER BY paragraphIndex ASC")
    suspend fun getParagraphsForPage(bookId: Long, pageNumber: Int): List<BookParagraphEntity>

    @Query("DELETE FROM paragraphs WHERE bookId = :bookId")
    suspend fun deleteParagraphsForBook(bookId: Long)

    @Query("DELETE FROM paragraphs WHERE bookId = :bookId AND pageNumber = :pageNumber")
    suspend fun deleteParagraphsForPage(bookId: Long, pageNumber: Int)
}
