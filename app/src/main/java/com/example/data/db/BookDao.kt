package com.example.data.db

import androidx.room.Dao
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

    @Query("SELECT * FROM books WHERE isFavorite = 1 ORDER BY lastReadTimestamp DESC")
    fun getFavoriteBooks(): Flow<List<Book>>

    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: Long): Flow<Book?>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookByIdSync(id: Long): Book?

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book): Long

    @Update
    suspend fun updateBook(book: Book)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBook(id: Long)

    @Query("UPDATE books SET currentParagraphIndex = :paragraphIndex, currentPage = :page, progressPercent = :progressPercent, lastReadTimestamp = :timestamp WHERE id = :bookId")
    suspend fun updateReadingProgress(bookId: Long, paragraphIndex: Int, page: Int, progressPercent: Float, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE books SET isFavorite = NOT isFavorite WHERE id = :id")
    suspend fun toggleFavorite(id: Long)
}

@Dao
interface ParagraphDao {
    @Query("SELECT * FROM paragraphs WHERE bookId = :bookId ORDER BY paragraphIndex ASC")
    fun getParagraphsForBook(bookId: Long): Flow<List<BookParagraphEntity>>

    @Query("SELECT * FROM paragraphs WHERE bookId = :bookId ORDER BY paragraphIndex ASC")
    suspend fun getParagraphsForBookSync(bookId: Long): List<BookParagraphEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParagraphs(paragraphs: List<BookParagraphEntity>)

    @Query("UPDATE paragraphs SET text = :newText, isUserEdited = 1 WHERE bookId = :bookId AND paragraphIndex = :paragraphIndex")
    suspend fun updateParagraphText(bookId: Long, paragraphIndex: Int, newText: String)

    @Query("DELETE FROM paragraphs WHERE bookId = :bookId")
    suspend fun deleteParagraphsForBook(bookId: Long)
}
