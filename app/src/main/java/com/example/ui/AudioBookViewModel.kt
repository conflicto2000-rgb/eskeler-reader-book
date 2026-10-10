package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity
import com.example.data.model.ReadingStats
import com.example.data.pdf.HybridPdfEngine
import com.example.data.repository.BookRepository
import com.example.tts.TtsManager
import com.example.tts.TtsVoiceInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class BookFilter {
    TODOS, FAVORITOS, EN_PROGRESO, TERMINADOS
}

enum class BookSort {
    RECIENTES, TITULO, PROGRESO
}

enum class ReaderThemeMode {
    OLED_DARK, MIDNIGHT_BLUE, SEPIA, SLATE
}

class AudioBookViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val repository = BookRepository(db.bookDao(), db.paragraphDao())
    val ttsManager = TtsManager(application)

    val allBooks = repository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _currentFilter = MutableStateFlow(BookFilter.TODOS)
    val currentFilter: StateFlow<BookFilter> = _currentFilter.asStateFlow()

    private val _currentSort = MutableStateFlow(BookSort.RECIENTES)
    val currentSort: StateFlow<BookSort> = _currentSort.asStateFlow()

    val filteredBooks: StateFlow<List<Book>> = combine(
        allBooks,
        _searchQuery,
        _currentFilter,
        _currentSort
    ) { books, query, filter, sort ->
        var list = books.filter {
            it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true)
        }

        list = when (filter) {
            BookFilter.TODOS -> list
            BookFilter.FAVORITOS -> list.filter { it.isFavorite }
            BookFilter.EN_PROGRESO -> list.filter { it.progressPercent > 0f && it.progressPercent < 98f }
            BookFilter.TERMINADOS -> list.filter { it.progressPercent >= 98f }
        }

        when (sort) {
            BookSort.RECIENTES -> list.sortedByDescending { it.lastReadTimestamp }
            BookSort.TITULO -> list.sortedBy { it.title.lowercase() }
            BookSort.PROGRESO -> list.sortedByDescending { it.progressPercent }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Player states
    private val _currentBook = MutableStateFlow<Book?>(null)
    val currentBook: StateFlow<Book?> = _currentBook.asStateFlow()

    private val _currentParagraphs = MutableStateFlow<List<BookParagraphEntity>>(emptyList())
    val currentParagraphs: StateFlow<List<BookParagraphEntity>> = _currentParagraphs.asStateFlow()

    private val _currentParagraphIndex = MutableStateFlow(0)
    val currentParagraphIndex: StateFlow<Int> = _currentParagraphIndex.asStateFlow()

    val isPlaying: StateFlow<Boolean> = ttsManager.isPlaying
    val speechRate: StateFlow<Float> = ttsManager.speechRate
    val speechPitch: StateFlow<Float> = ttsManager.speechPitch
    val availableVoices: StateFlow<List<TtsVoiceInfo>> = ttsManager.availableVoices
    val selectedVoice: StateFlow<TtsVoiceInfo?> = ttsManager.selectedVoice

    // Reader UI appearance
    private val _readerFontSize = MutableStateFlow(18f)
    val readerFontSize: StateFlow<Float> = _readerFontSize.asStateFlow()

    private val _readerTheme = MutableStateFlow(ReaderThemeMode.OLED_DARK)
    val readerTheme: StateFlow<ReaderThemeMode> = _readerTheme.asStateFlow()

    // Sleep timer
    private val _sleepTimerMinutesLeft = MutableStateFlow<Int?>(null)
    val sleepTimerMinutesLeft: StateFlow<Int?> = _sleepTimerMinutesLeft.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Import states
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

    private val _selectedOcrLanguage = MutableStateFlow(com.example.data.pdf.OcrLanguage.SPANISH)
    val selectedOcrLanguage: StateFlow<com.example.data.pdf.OcrLanguage> = _selectedOcrLanguage.asStateFlow()

    private val _ocrEngineStatus = MutableStateFlow<com.example.data.pdf.OcrEngineStatus?>(null)
    val ocrEngineStatus: StateFlow<com.example.data.pdf.OcrEngineStatus?> = _ocrEngineStatus.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureInitialSampleBooks()
            _ocrEngineStatus.value = com.example.data.pdf.OcrProcessor.checkEngineStatus(application)
        }

        // Setup TTS callback for continuous reading of all paragraphs!
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

    fun saveParagraphEdit(paragraphIndex: Int, newText: String) {
        val book = _currentBook.value ?: return
        viewModelScope.launch {
            repository.updateParagraphText(book.id, paragraphIndex, newText)
            // Update in-memory list
            val updated = _currentParagraphs.value.map { p ->
                if (p.paragraphIndex == paragraphIndex) {
                    p.copy(text = newText, isUserEdited = true, extractionMethod = "MANUAL", isLowConfidence = false)
                } else p
            }
            _currentParagraphs.value = updated
        }
    }

    /**
     * Normalizes archaic spelling (e.g. "dixo" -> "dijo", "dexar" -> "dejar", long-s)
     * across the current book's paragraphs for optimal TTS pronunciation without altering
     * the underlying original scanned text.
     */
    fun normalizeBookForNarration() {
        val book = _currentBook.value ?: return
        viewModelScope.launch {
            val paras = _currentParagraphs.value
            val normalized = paras.map { p ->
                val smoothed = com.example.data.pdf.HistoricalOrthographyNormalizer.normalizeForNarration(
                    text = p.text,
                    language = _selectedOcrLanguage.value
                )
                p.copy(text = smoothed, isUserEdited = true)
            }
            _currentParagraphs.value = normalized
            // Persist smoothed paragraphs to database
            for (p in normalized) {
                repository.updateParagraph(p)
            }
        }
    }

    fun setOcrLanguage(lang: com.example.data.pdf.OcrLanguage) {
        _selectedOcrLanguage.value = lang
    }

    private val _selectedPreprocessingMode = MutableStateFlow<com.example.data.pdf.PreprocessingMode>(com.example.data.pdf.PreprocessingMode.VINTAGE_BOOK)
    val selectedPreprocessingMode: StateFlow<com.example.data.pdf.PreprocessingMode> = _selectedPreprocessingMode.asStateFlow()

    fun setPreprocessingMode(mode: com.example.data.pdf.PreprocessingMode) {
        _selectedPreprocessingMode.value = mode
    }

    fun reprocessPageWithOcr(
        pageNumber: Int,
        language: com.example.data.pdf.OcrLanguage = _selectedOcrLanguage.value,
        mode: com.example.data.pdf.PreprocessingMode? = _selectedPreprocessingMode.value
    ) {
        val book = _currentBook.value ?: return
        viewModelScope.launch {
            _isReprocessingPage.value = true
            try {
                val newParas = repository.reprocessPageWithOcr(
                    context = getApplication(),
                    book = book,
                    pageNumber = pageNumber,
                    language = language,
                    forcedPreprocessing = mode
                )
                _currentParagraphs.value = newParas
                val updatedBook = repository.getBookByIdSync(book.id)
                if (updatedBook != null) {
                    _currentBook.value = updatedBook
                }
            } catch (_: Exception) {
            } finally {
                _isReprocessingPage.value = false
            }
        }
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
        if (paras.isEmpty()) return
        val clamped = index.coerceIn(0, paras.size - 1)
        _currentParagraphIndex.value = clamped
        saveProgress(clamped)
        playCurrentParagraph()
    }

    private fun playCurrentParagraph() {
        val paras = _currentParagraphs.value
        val idx = _currentParagraphIndex.value
        if (idx in paras.indices) {
            val textToSpeak = paras[idx].text
            ttsManager.speakParagraph(textToSpeak, "para_$idx")
            saveProgress(idx)
        }
    }

    private fun advanceToNextParagraph() {
        viewModelScope.launch {
            val paras = _currentParagraphs.value
            val currentIdx = _currentParagraphIndex.value
            if (currentIdx + 1 < paras.size) {
                val nextIdx = currentIdx + 1
                _currentParagraphIndex.value = nextIdx
                saveProgress(nextIdx)
                delay(120)
                playCurrentParagraph()
            } else {
                // Book completed!
                ttsManager.stop()
                _currentBook.value?.let { b ->
                    repository.updateReadingProgress(b.id, currentIdx, b.totalPages, 100f)
                }
            }
        }
    }

    fun nextParagraph() {
        val paras = _currentParagraphs.value
        if (paras.isEmpty()) return
        val nextIdx = (_currentParagraphIndex.value + 1).coerceAtMost(paras.size - 1)
        playParagraph(nextIdx)
    }

    fun previousParagraph() {
        val paras = _currentParagraphs.value
        if (paras.isEmpty()) return
        val prevIdx = (_currentParagraphIndex.value - 1).coerceAtLeast(0)
        playParagraph(prevIdx)
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
        if (currentIdx !in paras.indices) return
        val currentChapter = paras[currentIdx].chapterTitle
        val nextChapterIdx = paras.indexOfFirst { it.paragraphIndex > currentIdx && it.chapterTitle != currentChapter }
        if (nextChapterIdx != -1) {
            playParagraph(nextChapterIdx)
        } else {
            nextParagraph()
        }
    }

    fun previousChapter() {
        val paras = _currentParagraphs.value
        val currentIdx = _currentParagraphIndex.value
        if (currentIdx !in paras.indices) return
        val currentChapter = paras[currentIdx].chapterTitle
        val prevChapterIdx = paras.indexOfLast { it.paragraphIndex < currentIdx && it.chapterTitle != currentChapter }
        if (prevChapterIdx != -1) {
            val targetChapter = paras[prevChapterIdx].chapterTitle
            val startOfChapter = paras.indexOfFirst { it.chapterTitle == targetChapter }
            playParagraph(if (startOfChapter != -1) startOfChapter else prevChapterIdx)
        } else {
            previousParagraph()
        }
    }

    private fun saveProgress(index: Int) {
        val book = _currentBook.value ?: return
        val paras = _currentParagraphs.value
        if (paras.isEmpty()) return

        val percent = ((index + 1).toFloat() / paras.size.toFloat() * 100f).coerceIn(0f, 100f)
        val currentPage = paras.getOrNull(index)?.pageNumber ?: 1

        viewModelScope.launch {
            repository.updateReadingProgress(
                bookId = book.id,
                paragraphIndex = index,
                page = currentPage,
                progressPercent = percent
            )
            _currentBook.value = book.copy(
                currentParagraphIndex = index,
                currentPage = currentPage,
                progressPercent = percent
            )
        }
    }

    fun setSpeechRate(rate: Float) {
        ttsManager.setSpeechRate(rate)
    }

    fun setSpeechPitch(pitch: Float) {
        ttsManager.setPitch(pitch)
    }

    fun setVoice(voice: TtsVoiceInfo) {
        ttsManager.setVoice(voice)
    }

    fun playSampleVoice(voice: TtsVoiceInfo) {
        ttsManager.playSampleVoice(voice)
    }

    fun setReaderFontSize(size: Float) {
        _readerFontSize.value = size.coerceIn(12f, 32f)
    }

    fun setReaderTheme(theme: ReaderThemeMode) {
        _readerTheme.value = theme
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerMinutesLeft.value = minutes
        sleepTimerJob = viewModelScope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60_000L)
                remaining--
                _sleepTimerMinutesLeft.value = remaining
            }
            ttsManager.stop()
            _sleepTimerMinutesLeft.value = null
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerMinutesLeft.value = null
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            val newFav = !book.isFavorite
            repository.toggleFavorite(book.id, newFav)
            if (_currentBook.value?.id == book.id) {
                _currentBook.value = _currentBook.value?.copy(isFavorite = newFav)
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
            repository.deleteBook(book)
        }
    }

    fun importPdf(uri: Uri, fileName: String, onImportFinished: (Book) -> Unit) {
        importJob?.cancel()
        importJob = viewModelScope.launch {
            _isImporting.value = true
            _importErrorMessage.value = null
            _importProgress.value = 0.05f
            _importStatusText.value = "Iniciando análisis inteligente..."

            try {
                val result = HybridPdfEngine.processPdf(
                    context = getApplication(),
                    pdfUri = uri,
                    originalFileName = fileName,
                    ocrLanguage = _selectedOcrLanguage.value
                ) { status, progress ->
                    _importStatusText.value = status
                    _importProgress.value = progress
                }

                val newBook = Book(
                    title = result.title,
                    author = result.author,
                    filePath = uri.toString(),
                    coverPath = result.coverPath,
                    totalPages = result.totalPages,
                    totalParagraphs = result.paragraphs.size,
                    currentParagraphIndex = 0,
                    currentPage = 1,
                    progressPercent = 0f,
                    isFavorite = false,
                    isOcrProcessed = result.ocrPageCount > 0,
                    hasLowConfidencePages = result.hasLowConfidencePages,
                    ocrPageCount = result.ocrPageCount,
                    digitalPageCount = result.digitalPageCount
                )

                val newId = repository.insertImportedBook(newBook, result.paragraphs)
                val insertedBook = newBook.copy(id = newId)

                _currentBook.value = insertedBook
                _currentParagraphs.value = result.paragraphs
                _currentParagraphIndex.value = 0
                _isImporting.value = false

                onImportFinished(insertedBook)
            } catch (e: Exception) {
                _isImporting.value = false
                _importErrorMessage.value = "Error al procesar el PDF: ${e.localizedMessage ?: "Formato no válido o protegido"}"
            }
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        _isImporting.value = false
        _importStatusText.value = "Importación cancelada"
    }

    fun clearImportError() {
        _importErrorMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        importJob?.cancel()
        sleepTimerJob?.cancel()
        ttsManager.shutdown()
    }
}
