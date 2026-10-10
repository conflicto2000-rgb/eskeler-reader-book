package com.example.ui.screens.home

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.Book
import com.example.ui.components.WaveVisualizer
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElectricPurple
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File

@Composable
fun HomeScreen(
    books: List<Book>,
    currentBook: Book?,
    isPlaying: Boolean,
    onBookClick: (Book) -> Unit,
    onTogglePlay: () -> Unit,
    onImportPdfClick: () -> Unit,
    onNavigateLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val recentBooks = books.take(6)
    val favoriteCount = books.count { it.isFavorite }
    val finishedCount = books.count { it.progressPercent >= 98f }
    val featuredBook = currentBook ?: books.firstOrNull()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .testTag("home_screen"),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Hero Header Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = "AudioBook Reader Banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Dark gradient overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, DarkBackground.copy(alpha = 0.95f)),
                                startY = 100f
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(ElectricCyan),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Headphones,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AudioBook Reader",
                            color = ElectricCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Convierte tus libros y PDFs en voz natural",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Quick Import PDF Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onImportPdfClick() }
                    .testTag("import_pdf_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = DarkSurface
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(ElectricCyan, ElectricPurple)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Importar nuevo documento PDF",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Extracción de texto, capítulos y OCR inteligente",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Subir",
                        tint = ElectricCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // Continue Listening Card
        if (featuredBook != null) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = "Continuar escuchando",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBookClick(featuredBook) }
                            .testTag("continue_listening_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        Brush.linearGradient(listOf(ElectricPurple, ElectricCyan))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (featuredBook.coverPath != null && File(featuredBook.coverPath).exists()) {
                                    AsyncImage(
                                        model = File(featuredBook.coverPath),
                                        contentDescription = featuredBook.title,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = featuredBook.title,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = featuredBook.author,
                                    color = TextSecondary,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LinearProgressIndicator(
                                    progress = { (featuredBook.progressPercent / 100f).coerceIn(0f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(5.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = ElectricCyan,
                                    trackColor = DarkSurface
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${featuredBook.progressPercent.toInt()}% completado",
                                        color = ElectricCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Pág ${featuredBook.currentPage} de ${featuredBook.totalPages}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Play button
                            IconButton(
                                onClick = {
                                    if (currentBook?.id == featuredBook.id) {
                                        onTogglePlay()
                                    } else {
                                        onBookClick(featuredBook)
                                    }
                                },
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan)
                                    .testTag("home_play_featured_button")
                            ) {
                                Icon(
                                    imageVector = if (isPlaying && currentBook?.id == featuredBook.id) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Reproducir",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Stats Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "En Biblioteca",
                    value = "${books.size}",
                    icon = Icons.Default.AutoStories,
                    color = ElectricCyan
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Favoritos",
                    value = "$favoriteCount",
                    icon = Icons.Default.Favorite,
                    color = ElectricPurple
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Terminados",
                    value = "$finishedCount",
                    icon = Icons.Default.MenuBook,
                    color = Color(0xFF10B981)
                )
            }
        }

        // Recent Books Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Biblioteca Reciente",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Ver todo",
                    color = ElectricCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable { onNavigateLibrary() }
                        .padding(4.dp)
                )
            }
        }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(recentBooks, key = { it.id }) { book ->
                    BookThumbCard(
                        book = book,
                        isCurrentlyPlaying = isPlaying && currentBook?.id == book.id,
                        onClick = { onBookClick(book) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
fun BookThumbCard(
    book: Book,
    isCurrentlyPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick() }
            .testTag("book_thumb_${book.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(ElectricPurple.copy(alpha = 0.8f), ElectricCyan.copy(alpha = 0.8f))
                        )
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
                        modifier = Modifier.size(42.dp)
                    )
                }

                if (isCurrentlyPlaying) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center
                    ) {
                        WaveVisualizer(isPlaying = true, barCount = 10, maxHeight = 22.dp)
                    }
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = book.title,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { (book.progressPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ElectricCyan,
                    trackColor = DarkSurface
                )
            }
        }
    }
}
