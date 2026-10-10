package com.example

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AudioBookViewModel
import com.example.ui.components.ImportProgressDialog
import com.example.ui.components.MiniPlayer
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.library.LibraryScreen
import com.example.ui.screens.player.PlayerScreen
import com.example.ui.screens.review.TextReviewScreen
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.theme.AudioBookReaderTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class AppScreen {
    HOME, LIBRARY, SETTINGS, PLAYER, REVIEW
}

class MainActivity : ComponentActivity() {

    private val viewModel: AudioBookViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AudioBookReaderTheme {
                MainApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainApp(viewModel: AudioBookViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }

    val allBooks by viewModel.allBooks.collectAsStateWithLifecycle()
    val filteredBooks by viewModel.filteredBooks.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val currentFilter by viewModel.currentFilter.collectAsStateWithLifecycle()
    val currentSort by viewModel.currentSort.collectAsStateWithLifecycle()

    val currentBook by viewModel.currentBook.collectAsStateWithLifecycle()
    val currentParagraphs by viewModel.currentParagraphs.collectAsStateWithLifecycle()
    val currentParagraphIndex by viewModel.currentParagraphIndex.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val speechRate by viewModel.speechRate.collectAsStateWithLifecycle()
    val speechPitch by viewModel.speechPitch.collectAsStateWithLifecycle()
    val availableVoices by viewModel.availableVoices.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val sleepTimerMinutesLeft by viewModel.sleepTimerMinutesLeft.collectAsStateWithLifecycle()
    val fontSize by viewModel.readerFontSize.collectAsStateWithLifecycle()
    val readerTheme by viewModel.readerTheme.collectAsStateWithLifecycle()

    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val importStatusText by viewModel.importStatusText.collectAsStateWithLifecycle()
    val importErrorMessage by viewModel.importErrorMessage.collectAsStateWithLifecycle()
    val isReprocessingPage by viewModel.isReprocessingPage.collectAsStateWithLifecycle()
    val selectedOcrLanguage by viewModel.selectedOcrLanguage.collectAsStateWithLifecycle()
    val selectedPreprocessingMode by viewModel.selectedPreprocessingMode.collectAsStateWithLifecycle()
    val ocrEngineStatus by viewModel.ocrEngineStatus.collectAsStateWithLifecycle()

    // PDF File Picker Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            var fileName = "documento.pdf"
            context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    fileName = cursor.getString(nameIndex) ?: "documento.pdf"
                }
            }
            viewModel.importPdf(it, fileName) { _ ->
                currentScreen = AppScreen.REVIEW
            }
        }
    }

    // Back handling
    BackHandler(enabled = currentScreen == AppScreen.PLAYER || currentScreen == AppScreen.REVIEW) {
        currentScreen = AppScreen.HOME
    }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (currentScreen != AppScreen.PLAYER && currentScreen != AppScreen.REVIEW) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        // Floating MiniPlayer if active book exists
                        AnimatedVisibility(
                            visible = currentBook != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            currentBook?.let { book ->
                                MiniPlayer(
                                    book = book,
                                    isPlaying = isPlaying,
                                    onTogglePlay = { viewModel.togglePlayPause() },
                                    onSkipNext = { viewModel.nextParagraph() },
                                    onClick = { currentScreen = AppScreen.PLAYER }
                                )
                            }
                        }

                        // Bottom Navigation Bar
                        NavigationBar(
                            containerColor = DarkSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bottom_nav_bar")
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.HOME,
                                onClick = { currentScreen = AppScreen.HOME },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
                                label = { Text("Inicio") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_home")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.LIBRARY,
                                onClick = { currentScreen = AppScreen.LIBRARY },
                                icon = { Icon(Icons.Default.LocalLibrary, contentDescription = "Biblioteca") },
                                label = { Text("Biblioteca") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_library")
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.SETTINGS,
                                onClick = { currentScreen = AppScreen.SETTINGS },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                                label = { Text("Ajustes") },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag("nav_settings")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentScreen) {
                    AppScreen.HOME -> {
                        HomeScreen(
                            books = allBooks,
                            currentBook = currentBook,
                            isPlaying = isPlaying,
                            onBookClick = { book ->
                                viewModel.openBook(book, autoPlay = true)
                                currentScreen = AppScreen.PLAYER
                            },
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onImportPdfClick = { pdfPickerLauncher.launch("application/pdf") },
                            onNavigateLibrary = { currentScreen = AppScreen.LIBRARY }
                        )
                    }

                    AppScreen.LIBRARY -> {
                        LibraryScreen(
                            books = filteredBooks,
                            currentBook = currentBook,
                            isPlaying = isPlaying,
                            searchQuery = searchQuery,
                            currentFilter = currentFilter,
                            currentSort = currentSort,
                            onSearchChange = { viewModel.setSearchQuery(it) },
                            onFilterChange = { viewModel.setFilter(it) },
                            onSortChange = { viewModel.setSort(it) },
                            onBookClick = { book ->
                                viewModel.openBook(book, autoPlay = true)
                                currentScreen = AppScreen.PLAYER
                            },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDeleteBook = { viewModel.deleteBook(it) },
                            onImportPdfClick = { pdfPickerLauncher.launch("application/pdf") },
                            onReviewBook = { book ->
                                viewModel.openBookForReview(book)
                                currentScreen = AppScreen.REVIEW
                            }
                        )
                    }

                    AppScreen.SETTINGS -> {
                        SettingsScreen(
                            speechRate = speechRate,
                            speechPitch = speechPitch,
                            availableVoices = availableVoices,
                            selectedVoice = selectedVoice,
                            fontSize = fontSize,
                            readerTheme = readerTheme,
                            onSpeechRateChange = { viewModel.setSpeechRate(it) },
                            onSpeechPitchChange = { viewModel.setSpeechPitch(it) },
                            onSelectVoice = { viewModel.setVoice(it) },
                            onSamplePlay = { viewModel.playSampleVoice(it) },
                            onFontSizeChange = { viewModel.setReaderFontSize(it) },
                            onReaderThemeChange = { viewModel.setReaderTheme(it) }
                        )
                    }

                    AppScreen.PLAYER -> {
                        PlayerScreen(
                            book = currentBook,
                            paragraphs = currentParagraphs,
                            currentParagraphIndex = currentParagraphIndex,
                            isPlaying = isPlaying,
                            speechRate = speechRate,
                            availableVoices = availableVoices,
                            selectedVoice = selectedVoice,
                            sleepTimerMinutesLeft = sleepTimerMinutesLeft,
                            fontSize = fontSize,
                            readerTheme = readerTheme,
                            onBackClick = { currentScreen = AppScreen.HOME },
                            onTogglePlay = { viewModel.togglePlayPause() },
                            onParagraphClick = { viewModel.playParagraph(it) },
                            onNextParagraph = { viewModel.nextParagraph() },
                            onPreviousParagraph = { viewModel.previousParagraph() },
                            onSkipForward15 = { viewModel.skipForward15Sec() },
                            onSkipBackward15 = { viewModel.skipBackward15Sec() },
                            onNextChapter = { viewModel.nextChapter() },
                            onPreviousChapter = { viewModel.previousChapter() },
                            onSelectSpeed = { viewModel.setSpeechRate(it) },
                            onSelectVoice = { viewModel.setVoice(it) },
                            onSamplePlay = { viewModel.playSampleVoice(it) },
                            onStartSleepTimer = { viewModel.startSleepTimer(it) },
                            onCancelSleepTimer = { viewModel.cancelSleepTimer() },
                            onFontSizeChange = { viewModel.setReaderFontSize(it) },
                            onReaderThemeChange = { viewModel.setReaderTheme(it) },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onReviewClick = { currentScreen = AppScreen.REVIEW }
                        )
                    }

                    AppScreen.REVIEW -> {
                        currentBook?.let { book ->
                            TextReviewScreen(
                                book = book,
                                paragraphs = currentParagraphs,
                                isReprocessingPage = isReprocessingPage,
                                selectedOcrLanguage = selectedOcrLanguage,
                                selectedPreprocessingMode = selectedPreprocessingMode,
                                ocrEngineStatus = ocrEngineStatus,
                                onOcrLanguageChange = { viewModel.setOcrLanguage(it) },
                                onPreprocessingModeChange = { viewModel.setPreprocessingMode(it) },
                                onNormalizeArchaicClick = { viewModel.normalizeBookForNarration() },
                                onBackClick = { currentScreen = AppScreen.HOME },
                                onStartListening = { startIdx ->
                                    viewModel.openBook(book, autoPlay = true, startParagraphIndex = startIdx)
                                    currentScreen = AppScreen.PLAYER
                                },
                                onSaveParagraphEdit = { pIdx, newTxt ->
                                    viewModel.saveParagraphEdit(pIdx, newTxt)
                                },
                                onRerunOcrForPage = { pageNum, lang, mode ->
                                    viewModel.reprocessPageWithOcr(pageNum, lang, mode)
                                }
                            )
                        } ?: run {
                            currentScreen = AppScreen.HOME
                        }
                    }
                }
            }
        }

        // Import in-progress dialog
        if (isImporting) {
            ImportProgressDialog(
                statusText = importStatusText,
                progress = importProgress,
                onCancel = { viewModel.cancelImport() }
            )
        }

        // Import Error dialog
        importErrorMessage?.let { errorMsg ->
            AlertDialog(
                onDismissRequest = { viewModel.clearImportError() },
                containerColor = DarkSurface,
                title = {
                    Text("Error al importar PDF", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(errorMsg, color = TextPrimary)
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.clearImportError() },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Text("Aceptar", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
