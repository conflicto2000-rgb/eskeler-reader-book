package com.example.data.sample

import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity

object SampleBooks {
    fun getInitialBooks(): List<Pair<Book, List<BookParagraphEntity>>> {
        val principito = Book(
            id = 1L,
            title = "El Principito",
            author = "Antoine de Saint-Exupéry",
            filePath = "sample://el_principito",
            totalPages = 3,
            totalParagraphs = 5,
            progressPercent = 0.20f,
            isFavorite = true,
            isOcrProcessed = false,
            ocrPageCount = 0,
            digitalPageCount = 3
        )
        val principitoParas = listOf(
            BookParagraphEntity(
                bookId = 1L,
                paragraphIndex = 0,
                pageNumber = 1,
                chapterTitle = "Capítulo I",
                text = "Pido perdón a los niños por haber dedicado este libro a una persona grande. Tengo una seria razón para ello: esta persona grande es el mejor amigo que tengo en el mundo.",
                extractionMethod = "DIGITAL",
                qualityScore = 1.0f
            ),
            BookParagraphEntity(
                bookId = 1L,
                paragraphIndex = 1,
                pageNumber = 1,
                chapterTitle = "Capítulo I",
                text = "Tengo otra razón: esta persona grande puede comprenderlo todo, incluso los libros para niños. Tengo una tercera razón: esta persona grande vive en Francia, donde tiene hambre y frío.",
                extractionMethod = "DIGITAL",
                qualityScore = 1.0f
            ),
            BookParagraphEntity(
                bookId = 1L,
                paragraphIndex = 2,
                pageNumber = 2,
                chapterTitle = "Capítulo II",
                text = "Viví así, solo, sin nadie con quien hablar verdaderamente, hasta que tuve una avería en el desierto de Sahara, hace seis años. Algo se había roto en mi motor.",
                extractionMethod = "DIGITAL",
                qualityScore = 1.0f
            ),
            BookParagraphEntity(
                bookId = 1L,
                paragraphIndex = 3,
                pageNumber = 2,
                chapterTitle = "Capítulo II",
                text = "La primera noche me dormí sobre la arena, a mil millas de toda tierra habitada. Estaba más aislado que un náufrago en una balsa en medio del océano.",
                extractionMethod = "DIGITAL",
                qualityScore = 1.0f
            ),
            BookParagraphEntity(
                bookId = 1L,
                paragraphIndex = 4,
                pageNumber = 3,
                chapterTitle = "Capítulo II",
                text = "Imaginen pues mi sorpresa cuando, al romper el día, me despertó una extraña vocecita que decía: —Por favor... ¡dibújame un cordero! —¡Eh! —Dibújame un cordero...",
                extractionMethod = "DIGITAL",
                qualityScore = 1.0f
            )
        )

        val quijote = Book(
            id = 2L,
            title = "Don Quijote de la Mancha",
            author = "Miguel de Cervantes Saavedra",
            filePath = "sample://don_quijote",
            totalPages = 2,
            totalParagraphs = 3,
            progressPercent = 0.0f,
            isFavorite = true,
            isOcrProcessed = true,
            ocrPageCount = 2,
            digitalPageCount = 0
        )
        val quijoteParas = listOf(
            BookParagraphEntity(
                bookId = 2L,
                paragraphIndex = 0,
                pageNumber = 1,
                chapterTitle = "Capítulo I",
                text = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme, no ha mucho tiempo que vivía un hidalgo de los de lanza en astillero, adarga antigua, rocín flaco y galgo corredor.",
                extractionMethod = "OCR",
                qualityScore = 0.98f
            ),
            BookParagraphEntity(
                bookId = 2L,
                paragraphIndex = 1,
                pageNumber = 1,
                chapterTitle = "Capítulo I",
                text = "Una olla de algo más vaca que carnero, salpicón las más noches, duelos y quebrantos los sábados, lantejas los viernes, algún palomino de añadidura los domingos, consumían las tres partes de su hacienda.",
                extractionMethod = "OCR",
                qualityScore = 0.96f
            ),
            BookParagraphEntity(
                bookId = 2L,
                paragraphIndex = 2,
                pageNumber = 2,
                chapterTitle = "Capítulo I",
                text = "Es, pues, de saber que este sobredicho hidalgo, los ratos que estaba ocioso —que eran los más del año—, se daba a leer libros de caballerías con tanta afición y gusto, que olvidó casi de todo punto el ejercicio de la caza.",
                extractionMethod = "OCR",
                qualityScore = 0.97f
            )
        )

        val odisea = Book(
            id = 3L,
            title = "La Odisea",
            author = "Homero",
            filePath = "sample://la_odisea",
            totalPages = 1,
            totalParagraphs = 2,
            progressPercent = 0.10f,
            isFavorite = false,
            isOcrProcessed = false,
            ocrPageCount = 0,
            digitalPageCount = 1
        )
        val odiseaParas = listOf(
            BookParagraphEntity(
                bookId = 3L,
                paragraphIndex = 0,
                pageNumber = 1,
                chapterTitle = "Canto I",
                text = "Cuéntame, Musa, la historia del hombre de muchos senderos, que anduvo errante muy mucho después de destruir el alcázar sagrado de Troya.",
                extractionMethod = "DIGITAL",
                qualityScore = 1.0f
            ),
            BookParagraphEntity(
                bookId = 3L,
                paragraphIndex = 1,
                pageNumber = 1,
                chapterTitle = "Canto I",
                text = "Muchas ciudades de hombres vio y conoció sus costumbres, y muchos dolores padeció en su corazón por el mar para conservar su vida y el retorno de sus compañeros.",
                extractionMethod = "DIGITAL",
                qualityScore = 1.0f
            )
        )

        return listOf(
            Pair(principito, principitoParas),
            Pair(quijote, quijoteParas),
            Pair(odisea, odiseaParas)
        )
    }
}
