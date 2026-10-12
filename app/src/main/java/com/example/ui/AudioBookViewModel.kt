package com.example.ui

import android.app.Application
import android.net.Uri
import android.speech.tts.Voice
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity
import com.example.data.pdf.HistoricalOrthographyNormalizer
import com.example.data.pdf.HybridPdfEngine
import com.example.data.pdf.OcrEngineStatus
import com.example.data.pdf.OcrLanguage
import com.example.data.pdf.OcrProcessor
import com.example.data.pdf.PreprocessingMode
import com.example.data.pdf.TextQualityAnalyzer
import com.example.data.repository.BookRepository
import com.example.tts.TextToSpeechManager
import android.content.Context
import com.example.data.model.ReadingStats
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BookFilter {
    ALL, FAVORITES, OCR_SCANNED, FINISHED
}

enum class BookSort {
    RECENT, TITLE, AUTHOR, PROGRESS
}

class AudioBookViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = BookRepository(db.bookDao(), db.paragraphDao())
    val ttsManager = TextToSpeechManager(application)

    val allBooks: StateFlow<List<Book>> = repository.allBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow(BookFilter.ALL)
    val currentFilter: StateFlow<BookFilter> = _currentFilter.asStateFlow()

    private val _currentSort = MutableStateFlow(BookSort.RECENT)
    val currentSort: StateFlow<BookSort> = _currentSort.asStateFlow()

    val filteredBooks: StateFlow<List<Book>> = combine(
        allBooks, _searchQuery, _currentFilter, _currentSort
    ) { books, query, filter, sort ->
        var list = books

        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true)
            }
        }

        list = when (filter) {
            BookFilter.ALL -> list
            BookFilter.FAVORITES -> list.filter { it.isFavorite }
            BookFilter.OCR_SCANNED -> list.filter { it.isOcrProcessed }
            BookFilter.FINISHED -> list.filter { it.progressPercent >= 0.99f }
        }

        list = when (sort) {
            BookSort.RECENT -> list.sortedByDescending { it.lastReadTimestamp }
            BookSort.TITLE -> list.sortedBy { it.title.lowercase() }
            BookSort.AUTHOR -> list.sortedBy { it.author.lowercase() }
            BookSort.PROGRESS -> list.sortedByDescending { it.progressPercent }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingStats: StateFlow<ReadingStats> = allBooks.map { books ->
        val total = books.size
        val finished = books.count { it.progressPercent >= 0.95f || (it.totalPages > 1 && it.currentPage >= it.totalPages) }
        val favorites = books.count { it.isFavorite }
        val totalMinutes = books.sumOf { (it.currentParagraphIndex * 15) / 60 + (it.currentPage * 2) }
        ReadingStats(
            totalBooks = total,
            totalFinished = finished,
            totalReadingTimeMinutes = totalMinutes,
            favoriteCount = favorites
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingStats(0, 0, 0, 0))

    // Bookmarks for active book
    private val _bookmarkedPages = MutableStateFlow<Set<Int>>(emptySet())
    val bookmarkedPages: StateFlow<Set<Int>> = _bookmarkedPages.asStateFlow()

    // Active playback state
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    private val _currentParagraphs = MutableStateFlow<List<BookParagraphEntity>>(emptyList())
    val currentParagraphs: StateFlow<List<BookParagraphEntity>> = _currentParagraphs.asStateFlow()

    private val _currentParagraphIndex = MutableStateFlow(0)
    val currentParagraphIndex: StateFlow<Int> = _currentParagraphIndex.asStateFlow()

    val isPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val speechRate: StateFlow<Float> = ttsManager.speechRate
    val speechPitch: StateFlow<Float> = ttsManager.speechPitch
    val availableVoices: StateFlow<List<Voice>> = ttsManager.availableVoices
    val selectedVoice: StateFlow<Voice?> = ttsManager.selectedVoice

    private val _sleepTimerMinutesLeft = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesLeft: StateFlow<Int?> = _sleepTimerMinutesLeft.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Preferences
    private val _readerFontSize = MutableStateFlow(18)
    val readerFontSize: StateFlow<Int> = _readerFontSize.asStateFlow()

    private val _readerTheme = MutableStateFlow("dark")
    val readerTheme: StateFlow<String> = _readerTheme.asStateFlow()

    // Import progress state
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow(0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    private val _importStatusText = MutableStateFlow("")
    val importStatusText: StateFlow<String> = _importStatusText.asStateFlow()

    private val _importErrorMessage = MutableStateFlow<String?>(null)
    val importErrorMessage: StateFlow<String?> = _importErrorMessage.asStateFlow()

    private var importJob: Job? = null

    // Single-page OCR reprocessing state
    private val _isReprocessingPage = MutableStateFlow(false)
    val isReprocessingPage: StateFlow<Boolean> = _isReprocessingPage.asStateFlow()

    private val _selectedOcrLanguage = MutableStateFlow(OcrLanguage.SPANISH)
    val selectedOcrLanguage: StateFlow<OcrLanguage> = _selectedOcrLanguage.asStateFlow()

    private val _selectedPreprocessingMode = MutableStateFlow(PreprocessingMode.VINTAGE_BOOK)
    val selectedPreprocessingMode: StateFlow<PreprocessingMode> = _selectedPreprocessingMode.asStateFlow()

    private val _ocrEngineStatus = MutableStateFlow<OcrEngineStatus?>(null)
    val ocrEngineStatus: StateFlow<OcrEngineStatus?> = _ocrEngineStatus.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialSampleBooks()
            _ocrEngineStatus.value = OcrProcessor.checkEngineStatus(application)
        }

        ttsManager.onParagraphCompleted = {
            advanceToNextParagraph()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: BookFilter) {
        _currentFilter.value = filter
    }

    fun setSort(sort: BookSort) {
        _currentSort.value = sort
    }

    fun openBook(book: Book, autoPlay: Boolean = false, startParagraphIndex: Int = -1) {
        viewModelScope.launch {
            _currentBook.value = book
            val paras = repository.getParagraphsForBookSync(book.id)
            _currentParagraphs.value = paras

            val startIdx = if (startParagraphIndex >= 0) {
                startParagraphIndex.coerceIn(0, (paras.size - 1).coerceAtLeast(0))
            } else {
                book.currentParagraphIndex.coerceIn(0, (paras.size - 1).coerceAtLeast(0))
            }
            _currentParagraphIndex.value = startIdx

            if (autoPlay && paras.isNotEmpty()) {
                playCurrentParagraph()
            }
        }
    }

    fun openBookForReview(book: Book) {
        viewModelScope.launch {
            _currentBook.value = book
            val paras = repository.getParagraphsForBookSync(book.id)
            _currentParagraphs.value = paras
        }
    }

    fun openBookForPdf(book: Book) {
        viewModelScope.launch {
            _currentBook.value = book
            val paras = repository.getParagraphsForBookSync(book.id)
            _currentParagraphs.value = paras
            loadBookmarksForBook(book.id)
        }
    }

    private fun loadBookmarksForBook(bookId: Long) {
        val prefs = getApplication<Application>().getSharedPreferences("pdf_bookmarks", Context.MODE_PRIVATE)
        val saved = prefs.getStringSet("book_$bookId", emptySet()) ?: emptySet()
        _bookmarkedPages.value = saved.mapNotNull { it.toIntOrNull() }.toSet()
    }

    fun toggleBookmark(pageNumber: Int) {
        val book = _currentBook.value ?: return
        val current = _bookmarkedPages.value.toMutableSet()
        if (current.contains(pageNumber)) {
            current.remove(pageNumber)
        } else {
            current.add(pageNumber)
        }
        _bookmarkedPages.value = current
        val prefs = getApplication<Application>().getSharedPreferences("pdf_bookmarks", Context.MODE_PRIVATE)
        prefs.edit().putStringSet("book_${book.id}", current.map { it.toString() }.toSet()).apply()
    }

    fun saveCurrentPage(pageNumber: Int) {
        val book = _currentBook.value ?: return
        val totalPages = book.totalPages.coerceAtLeast(1)
        val safePage = pageNumber.coerceIn(1, totalPages)
        val progress = safePage.toFloat() / totalPages.toFloat()

        val paras = _currentParagraphs.value
        val matchedIdx = paras.indexOfFirst { it.pageNumber == safePage }.takeIf { it != -1 } ?: book.currentParagraphIndex

        viewModelScope.launch {
            repository.updateReadingProgress(book.id, matchedIdx, safePage, progress)
            _currentBook.value = _currentBook.value?.copy(
                currentPage = safePage,
                currentParagraphIndex = matchedIdx,
                progressPercent = progress
            )
        }
    }

    fun playPageTextWithTts(pageNumber: Int) {
        val paras = _currentParagraphs.value
        val pageParas = paras.filter { it.pageNumber == pageNumber }
        if (pageParas.isNotEmpty()) {
            val firstIdx = paras.indexOfFirst { it.pageNumber == pageNumber }
            if (firstIdx != -1) {
                playParagraph(firstIdx)
            }
        } else {
            ttsManager.speak("Página $pageNumber. No hay texto reconocido para reproducir.")
        }
    }

    fun saveParagraphEdit(paragraphIndex: Int, newText: String) {
        val book = _currentBook.value ?: return
        viewModelScope.launch {
            repository.updateParagraphText(book.id, paragraphIndex, newText)
            val updated = _currentParagraphs.value.map { p ->
                if (p.paragraphIndex == paragraphIndex) {
                    p.copy(text = newText, isUserEdited = true)
                } else p
            }
            _currentParagraphs.value = updated
        }
    }

    fun normalizeBookForNarration() {
        val current = _currentParagraphs.value
        if (current.isEmpty()) return
        val updated = current.map { para ->
            val modern = HistoricalOrthographyNormalizer.normalizeForNarration(para.text, _selectedOcrLanguage.value)
            para.copy(text = modern, isUserEdited = true)
        }
        _currentParagraphs.value = updated
    }

    fun setOcrLanguage(language: OcrLanguage) {
        _selectedOcrLanguage.value = language
    }

    fun setPreprocessingMode(mode: PreprocessingMode) {
        _selectedPreprocessingMode.value = mode
    }

    fun reprocessPageWithOcr(
        pageNumber: Int,
        language: OcrLanguage = _selectedOcrLanguage.value,
        mode: PreprocessingMode = _selectedPreprocessingMode.value
    ) {
        val book = _currentBook.value ?: return
        viewModelScope.launch {
            _isReprocessingPage.value = true
            try {
                val updatedParas = repository.reprocessPageWithOcr(
                    context = getApplication(),
                    book = book,
                    pageNumber = pageNumber,
                    language = language,
                    forcedPreprocessing = mode
                )
                _currentParagraphs.value = updatedParas
            } catch (_: Exception) {
            } finally {
                _isReprocessingPage.value = false
            }
        }
    }

    fun saveLensReaderDocument(
        title: String,
        paragraphs: List<String>,
        imagePath: String?,
        onSuccess: (Book) -> Unit
    ) {
        viewModelScope.launch {
            val cleanTitle = title.ifBlank { "Lectura Lens" }
            val book = Book(
                title = cleanTitle,
                author = "Lens Reader (OCR)",
                filePath = imagePath ?: "",
                coverPath = imagePath,
                totalPages = 1,
                totalParagraphs = paragraphs.size,
                currentParagraphIndex = 0,
                currentPage = 1,
                progressPercent = 0f,
                isFavorite = false,
                isOcrProcessed = true,
                hasLowConfidencePages = false,
                ocrPageCount = 1,
                digitalPageCount = 0
            )

            val paragraphEntities = paragraphs.mapIndexed { idx, pText ->
                val qReport = TextQualityAnalyzer.evaluate(pText)
                BookParagraphEntity(
                    bookId = 0L,
                    paragraphIndex = idx,
                    pageNumber = 1,
                    chapterTitle = "Página Reconocida",
                    text = pText,
                    originalText = pText,
                    extractionMethod = "LENS_OCR",
                    qualityScore = qReport.score,
                    isLowConfidence = qReport.isCorrupted,
                    isUserEdited = false
                )
            }

            val newBookId = repository.insertImportedBook(book, paragraphEntities)
            val insertedBook = repository.getBookByIdSync(newBookId) ?: book.copy(id = newBookId)
            _currentBook.value = insertedBook
            _currentParagraphs.value = paragraphEntities.map { it.copy(bookId = newBookId) }
            _currentParagraphIndex.value = 0

            onSuccess(insertedBook)
        }
    }

    fun importPdf(uri: Uri, fileName: String, onSuccess: (Book) -> Unit) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _isImporting.value = true
            _importProgress.value = 0f
            _importStatusText.value = "Iniciando análisis del PDF..."
            _importErrorMessage.value = null

            try {
                val result = HybridPdfEngine.processPdf(
                    context = getApplication(),
                    pdfUri = uri,
                    originalFileName = fileName,
                    selectedLanguage = _selectedOcrLanguage.value
                ) { page, total, statusText ->
                    _importProgress.value = if (total > 0) page.toFloat() / total.toFloat() else 0f
                    _importStatusText.value = statusText
                }

                val book = Book(
                    title = result.title,
                    author = result.author,
                    filePath = result.localFilePath.ifBlank { uri.toString() },
                    totalPages = result.totalPages,
                    totalParagraphs = result.paragraphs.size,
                    currentParagraphIndex = 0,
                    currentPage = 1,
                    progressPercent = 0f,
                    isOcrProcessed = result.ocrPageCount > 0,
                    hasLowConfidencePages = result.hasLowConfidencePages,
                    ocrPageCount = result.ocrPageCount,
                    digitalPageCount = result.digitalPageCount
                )

                val newBookId = repository.insertImportedBook(book, result.paragraphs)
                val insertedBook = repository.getBookByIdSync(newBookId) ?: book.copy(id = newBookId)

                _currentBook.value = insertedBook
                _currentParagraphs.value = result.paragraphs.map { it.copy(bookId = newBookId) }
                _currentParagraphIndex.value = 0

                _isImporting.value = false
                onSuccess(insertedBook)
            } catch (e: Exception) {
                _isImporting.value = false
                _importErrorMessage.value = "No se pudo procesar el archivo: ${e.message ?: "Formato no compatible"}"
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        _isImporting.value = false
        _importProgress.value = 0f
        _importStatusText.value = ""
    }

    fun clearImportError() {
        _importErrorMessage.value = null
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            ttsManager.stop()
        } else {
            playCurrentParagraph()
        }
    }

    fun playParagraph(index: Int) {
        val paras = _currentParagraphs.value
        if (index in paras.indices) {
            _currentParagraphIndex.value = index
            updateProgressInDb(index)
            playCurrentParagraph()
        }
    }

    private fun playCurrentParagraph() {
        val paras = _currentParagraphs.value
        val idx = _currentParagraphIndex.value
        if (idx in paras.indices) {
            val textToSpeak = paras[idx].text
            ttsManager.speak(textToSpeak)
            updateProgressInDb(idx)
        }
    }

    private fun advanceToNextParagraph() {
        val paras = _currentParagraphs.value
        val nextIdx = _currentParagraphIndex.value + 1
        if (nextIdx < paras.size) {
            _currentParagraphIndex.value = nextIdx
            updateProgressInDb(nextIdx)
            playCurrentParagraph()
        } else {
            ttsManager.stop()
            _currentParagraphIndex.value = paras.size - 1
            updateProgressInDb(paras.size - 1)
        }
    }

    fun nextParagraph() {
        advanceToNextParagraph()
    }

    fun previousParagraph() {
        val prevIdx = (_currentParagraphIndex.value - 1).coerceAtLeast(0)
        _currentParagraphIndex.value = prevIdx
        updateProgressInDb(prevIdx)
        if (isPlaying.value) {
            playCurrentParagraph()
        }
    }

    fun skipForward15Sec() {
        nextParagraph()
    }

    fun skipBackward15Sec() {
        previousParagraph()
    }

    fun nextChapter() {
        val paras = _currentParagraphs.value
        val currentIdx = _currentParagraphIndex.value
        val currentChapter = paras.getOrNull(currentIdx)?.chapterTitle ?: ""
        val nextChapterIdx = paras.indexOfFirst { it.paragraphIndex > currentIdx && it.chapterTitle != currentChapter }
        if (nextChapterIdx != -1) {
            playParagraph(nextChapterIdx)
        }
    }

    fun previousChapter() {
        val paras = _currentParagraphs.value
        val currentIdx = _currentParagraphIndex.value
        val currentChapter = paras.getOrNull(currentIdx)?.chapterTitle ?: ""
        val prevChapterIdx = paras.indexOfLast { it.paragraphIndex < currentIdx && it.chapterTitle != currentChapter }
        if (prevChapterIdx != -1) {
            playParagraph(prevChapterIdx)
        }
    }

    private fun updateProgressInDb(paragraphIndex: Int) {
        val book = _currentBook.value ?: return
        val total = _currentParagraphs.value.size
        val progress = if (total > 0) paragraphIndex.toFloat() / total.toFloat() else 0f
        val page = _currentParagraphs.value.getOrNull(paragraphIndex)?.pageNumber ?: 1

        viewModelScope.launch {
            repository.updateReadingProgress(book.id, paragraphIndex, page, progress)
            _currentBook.value = _currentBook.value?.copy(
                currentParagraphIndex = paragraphIndex,
                currentPage = page,
                progressPercent = progress
            )
        }
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            repository.toggleFavorite(book.id)
            if (_currentBook.value?.id == book.id) {
                _currentBook.value = _currentBook.value?.copy(isFavorite = !book.isFavorite)
            }
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            if (_currentBook.value?.id == book.id) {
                ttsManager.stop()
                _currentBook.value = null
                _currentParagraphs.value = emptyList()
            }
            repository.deleteBook(book.id)
        }
    }

    fun setSpeechRate(rate: Float) {
        ttsManager.setSpeechRate(rate)
    }

    fun setSpeechPitch(pitch: Float) {
        ttsManager.setSpeechPitch(pitch)
    }

    fun setVoice(voice: Voice) {
        ttsManager.setVoice(voice)
    }

    fun playSampleVoice(voice: Voice) {
        ttsManager.setVoice(voice)
        ttsManager.speak("Esta es una muestra de voz para AudioBook Reader.")
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutesLeft.value = minutes

        sleepTimerJob = viewModelScope.launch {
            var mins = minutes
            while (mins > 0) {
                delay(60_000)
                mins--
                _sleepTimerMinutesLeft.value = mins
            }
            ttsManager.stop()
            _sleepTimerMinutesLeft.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutesLeft.value = null
    }

    fun setReaderFontSize(size: Int) {
        _readerFontSize.value = size.coerceIn(12, 32)
    }

    fun setReaderTheme(theme: String) {
        _readerTheme.value = theme
    }

    override fun onCleared() {
        super.onCleared()
        ttsManager.release()
    }
}
