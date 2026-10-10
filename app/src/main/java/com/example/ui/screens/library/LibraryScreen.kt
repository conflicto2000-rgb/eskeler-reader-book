package com.example.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Book
import com.example.ui.BookFilter
import com.example.ui.BookSort
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.DarkSurfaceContainerHigh
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.FavoriteRed
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun LibraryScreen(
    books: List<Book>,
    currentBook: Book?,
    isPlaying: Boolean,
    searchQuery: String,
    currentFilter: BookFilter,
    currentSort: BookSort,
    onSearchChange: (String) -> Unit,
    onFilterChange: (BookFilter) -> Unit,
    onSortChange: (BookSort) -> Unit,
    onBookClick: (Book) -> Unit,
    onToggleFavorite: (Book) -> Unit,
    onDeleteBook: (Book) -> Unit,
    onImportPdfClick: () -> Unit,
    onReviewBook: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    var bookToDelete by remember { mutableStateOf<Book?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("library_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mi Biblioteca",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${books.size} libros disponibles",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }

                // Sort Button
                Box {
                    IconButton(
                        onClick = { showSortMenu = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("sort_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Ordenar",
                            tint = ElectricCyan
                        )
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(DarkSurface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Más recientes", color = if (currentSort == BookSort.RECIENTES) ElectricCyan else TextPrimary) },
                            onClick = {
                                onSortChange(BookSort.RECIENTES)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Título (A - Z)", color = if (currentSort == BookSort.TITULO) ElectricCyan else TextPrimary) },
                            onClick = {
                                onSortChange(BookSort.TITULO)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Mayor progreso", color = if (currentSort == BookSort.PROGRESO) ElectricCyan else TextPrimary) },
                            onClick = {
                                onSortChange(BookSort.PROGRESO)
                                showSortMenu = false
                            }
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("search_field"),
                placeholder = { Text("Buscar por título o autor...", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary)
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color(0xFF242C3D),
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(
                    BookFilter.TODOS to "Todos",
                    BookFilter.FAVORITOS to "Favoritos",
                    BookFilter.EN_PROGRESO to "En progreso",
                    BookFilter.TERMINADOS to "Terminados"
                )
                items(filters) { (filter, label) ->
                    val isSelected = currentFilter == filter
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ElectricCyan else DarkSurfaceVariant)
                            .clickable { onFilterChange(filter) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("filter_${filter.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.Black else TextSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Books List
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No hay resultados para tu búsqueda" else "No hay libros en esta sección",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Importa un archivo PDF para comenzar a escuchar",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onImportPdfClick,
                            colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Importar PDF", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(books, key = { it.id }) { book ->
                        LibraryBookCard(
                            book = book,
                            isCurrentlyActive = currentBook?.id == book.id,
                            isPlaying = isPlaying && currentBook?.id == book.id,
                            onClick = { onBookClick(book) },
                            onToggleFavorite = { onToggleFavorite(book) },
                            onDelete = { bookToDelete = book },
                            onReview = { onReviewBook(book) }
                        )
                    }
                }
            }
        }

        // Floating Action Button to Import PDF
        FloatingActionButton(
            onClick = onImportPdfClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 90.dp)
                .testTag("fab_import_pdf"),
            containerColor = ElectricCyan,
            contentColor = Color.Black,
            shape = CircleShape
        ) {
            Icon(Icons.Default.Add, contentDescription = "Importar PDF", modifier = Modifier.size(28.dp))
        }

        // Delete Confirmation Dialog
        bookToDelete?.let { book ->
            AlertDialog(
                onDismissRequest = { bookToDelete = null },
                containerColor = DarkSurface,
                title = {
                    Text("Eliminar audiolibro", color = TextPrimary, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "¿Estás seguro de que deseas eliminar '${book.title}' de tu biblioteca? Esta acción no se puede deshacer.",
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteBook(book)
                            bookToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                    ) {
                        Text("Eliminar", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { bookToDelete = null }) {
                        Text("Cancelar", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun LibraryBookCard(
    book: Book,
    isCurrentlyActive: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onReview: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("library_card_${book.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyActive) DarkSurfaceContainerHigh else DarkSurface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover
            Box(
                modifier = Modifier
                    .size(width = 64.dp, height = 82.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(listOf(ElectricPurple, ElectricCyan))
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (book.coverPath != null && File(book.coverPath).exists()) {
                    AsyncImage(
                        model = File(book.coverPath),
                        contentDescription = book.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                if (book.isOcrProcessed) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = "OCR",
                            tint = ElectricCyan,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    color = if (isCurrentlyActive) ElectricCyan else TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${book.totalPages} págs • ${book.totalParagraphs} párrafos",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    if (book.hasLowConfidencePages) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "⚠️ Revisar",
                            color = AmberWarning,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = if (book.progressPercent >= 98f) Color(0xFF10B981) else ElectricCyan,
                    trackColor = DarkSurfaceVariant
                )

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = if (book.progressPercent >= 98f) "Terminado" else "${book.progressPercent.toInt()}% leído",
                    color = if (book.progressPercent >= 98f) Color(0xFF10B981) else ElectricCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (book.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (book.isFavorite) FavoriteRed else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onReview,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.TextFields,
                        contentDescription = "Revisar texto",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
