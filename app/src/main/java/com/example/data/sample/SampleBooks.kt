package com.example.data.sample

import com.example.data.model.Book
import com.example.data.model.BookParagraphEntity

object SampleBooks {

    fun getInitialBooks(): List<Pair<Book, List<BookParagraphEntity>>> {
        val now = System.currentTimeMillis()

        // 1. El Principito
        val principito = Book(
            id = 1,
            title = "El Principito",
            author = "Antoine de Saint-Exupéry",
            filePath = "sample://el_principito",
            coverDrawableRes = null,
            totalPages = 12,
            totalParagraphs = 18,
            currentParagraphIndex = 0,
            currentPage = 1,
            progressPercent = 0f,
            isFavorite = true,
            lastReadTimestamp = now,
            createdAt = now,
            fileSizeBytes = 450_000L
        )

        val principitoParagraphs = listOf(
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 0,
                pageNumber = 1,
                chapterTitle = "Capítulo 1: El Sombrero y la Boa",
                text = "Pido perdón a los niños por haber dedicado este libro a una persona grande. Tengo una seria razón para ello: esta persona grande es el mejor amigo que tengo en el mundo."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 1,
                pageNumber = 1,
                chapterTitle = "Capítulo 1: El Sombrero y la Boa",
                text = "Cuando yo tenía seis años vi en un libro sobre la selva virgen que se titulaba 'Historias vividas', una magnífica lámina. Representaba una serpiente boa que se tragaba a una fiera."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 2,
                pageNumber = 1,
                chapterTitle = "Capítulo 1: El Sombrero y la Boa",
                text = "El libro decía: 'Las serpientes boas tragan sus presas enteras, sin masticarlas. Luego no pueden moverse y duermen durante los seis meses que dura su digestión'. Reflexioné mucho entonces sobre las aventuras de la selva y logré trazar con un lápiz de color mi primer dibujo."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 3,
                pageNumber = 2,
                chapterTitle = "Capítulo 1: El Sombrero y la Boa",
                text = "Enseñé mi obra de arte a las personas mayores y les pregunté si mi dibujo les daba miedo. Me contestaron: '¿Por qué habría de dar miedo un sombrero?' Mi dibujo no representaba un sombrero. Representaba una serpiente boa que digería un elefante."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 4,
                pageNumber = 3,
                chapterTitle = "Capítulo 2: El Encuentro en el Desierto",
                text = "Viví así, solo, sin nadie con quien hablar verdaderamente, hasta que tuve una avería en el desierto de Sahara, hace seis años. Algo se había roto en mi motor. Y como no llevaba conmigo ni mecánico ni pasajeros, me dispuse a realizar, solo, una difícil reparación. Era para mí una cuestión de vida o muerte. Apenas tenía agua para beber durante ocho días."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 5,
                pageNumber = 3,
                chapterTitle = "Capítulo 2: El Encuentro en el Desierto",
                text = "La primera noche me dormí sobre la arena, a mil millas de toda tierra habitada. Estaba más aislado que un náufrago en una balsa en medio del océano. Imaginaos, pues, mi sorpresa cuando, al romper el día, me despertó una extraña vocecita que decía: 'Por favor... ¡dibújame un cordero!'."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 6,
                pageNumber = 4,
                chapterTitle = "Capítulo 2: El Encuentro en el Desierto",
                text = "Me puse en pie de un salto, como herido por el rayo. Me froté los ojos. Miré bien. Y vi un hombrecito extraordinario que me examinaba gravemente. No parecía ni perdido, ni muerto de fatiga, ni muerto de hambre, ni muerto de sed, ni muerto de miedo."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 7,
                pageNumber = 5,
                chapterTitle = "Capítulo 21: El Zorro y el Secreto",
                text = "Fue entonces cuando apareció el zorro. 'Buenos días', dijo el zorro. 'Buenos días', respondió cortésmente el principito, que se volvió pero no vio nada. 'Estoy aquí', dijo la voz, 'bajo el manzano...' '¿Quién eres?', preguntó el principito. 'Eres muy bonito...' 'Soy un zorro', dijo el zorro."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 8,
                pageNumber = 5,
                chapterTitle = "Capítulo 21: El Zorro y el Secreto",
                text = "'Ven a jugar conmigo', le propuso el principito. 'Estoy tan triste...' 'No puedo jugar contigo', dijo el zorro. 'No estoy domesticado'. '¡Ah! Perdón', dijo el principito. Pero, después de reflexionar, agregó: '¿Qué significa domesticar?'."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 9,
                pageNumber = 6,
                chapterTitle = "Capítulo 21: El Zorro y el Secreto",
                text = "'Es una cosa demasiado olvidada', dijo el zorro. 'Significa crear lazos... Para mí no eres todavía más que un muchachito semejante a cien mil muchachitos. Y no te necesito. Y tú tampoco me necesitas. No soy para ti más que un zorro semejante a cien mil zorros. Pero, si me domesticas, tendremos necesidad el uno del otro. Serás para mí único en el mundo. Seré para ti único en el mundo...'"
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 10,
                pageNumber = 7,
                chapterTitle = "Capítulo 21: El Zorro y el Secreto",
                text = "'Empiezo a comprender', dijo el principito. 'Hay una flor... creo que me ha domesticado...' 'Es posible', dijo el zorro. '¡En la Tierra se ve toda clase de cosas!'."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 11,
                pageNumber = 8,
                chapterTitle = "Capítulo 21: Lo Esencial es Invisible",
                text = "Y volvió hacia el zorro: 'Adiós', le dijo... 'Adiós', dijo el zorro. 'He aquí mi secreto. Es muy simple: solo con el corazón se puede ver bien; lo esencial es invisible a los ojos'."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 12,
                pageNumber = 8,
                chapterTitle = "Capítulo 21: Lo Esencial es Invisible",
                text = "'Lo esencial es invisible a los ojos', repitió el principito a fin de acordarse. 'El tiempo que perdiste por tu rosa hace que tu rosa sea tan importante'."
            ),
            BookParagraphEntity(
                bookId = 1,
                paragraphIndex = 13,
                pageNumber = 9,
                chapterTitle = "Capítulo 21: Lo Esencial es Invisible",
                text = "'Los hombres han olvidado esta verdad', dijo el zorro. 'Pero tú no debes olvidarla. Eres responsable para siempre de lo que has domesticado. Eres responsable de tu rosa...' 'Soy responsable de mi rosa...', repitió el principito a fin de acordarse."
            )
        )

        // 2. Don Quijote de la Mancha
        val quijote = Book(
            id = 2,
            title = "Don Quijote de la Mancha",
            author = "Miguel de Cervantes",
            filePath = "sample://don_quijote",
            coverDrawableRes = null,
            totalPages = 24,
            totalParagraphs = 12,
            currentParagraphIndex = 0,
            currentPage = 1,
            progressPercent = 0f,
            isFavorite = false,
            lastReadTimestamp = now - 3600_000L,
            createdAt = now - 3600_000L,
            fileSizeBytes = 820_000L
        )

        val quijoteParagraphs = listOf(
            BookParagraphEntity(
                bookId = 2,
                paragraphIndex = 0,
                pageNumber = 1,
                chapterTitle = "Capítulo I: De la condición del famoso hidalgo",
                text = "En un lugar de la Mancha, de cuyo nombre no quiero acordarme, no ha mucho tiempo que vivía un hidalgo de los de lanza en astillero, adarga antigua, rocín flaco y galgo corredor."
            ),
            BookParagraphEntity(
                bookId = 2,
                paragraphIndex = 1,
                pageNumber = 1,
                chapterTitle = "Capítulo I: De la condición del famoso hidalgo",
                text = "Una olla de algo más vaca que carnero, salpicón las más noches, duelos y quebrantos los sábados, lantejas los viernes, algún palomino de añadidura los domingos, consumían las tres partes de su hacienda."
            ),
            BookParagraphEntity(
                bookId = 2,
                paragraphIndex = 2,
                pageNumber = 2,
                chapterTitle = "Capítulo I: De la condición del famoso hidalgo",
                text = "Frisaba la edad de nuestro hidalgo con los cincuenta años; era de complexión recia, seco de carnes, enjuto de rostro, gran madrugador y amigo de la caza. Quieren decir que tenía el sobrenombre de Quijada, o Quesada, que en esto hay alguna diferencia en los autores que deste caso escriben."
            ),
            BookParagraphEntity(
                bookId = 2,
                paragraphIndex = 3,
                pageNumber = 3,
                chapterTitle = "Capítulo I: De la condición del famoso hidalgo",
                text = "Es, pues, de saber que este sobredicho hidalgo, los ratos que estaba ocioso —que eran los más del año—, se daba a leer libros de caballerías, con tanta afición y gusto, que olvidó casi de todo punto el ejercicio de la caza, y aun la administración de su hacienda."
            ),
            BookParagraphEntity(
                bookId = 2,
                paragraphIndex = 4,
                pageNumber = 4,
                chapterTitle = "Capítulo VIII: Los Molinos de Viento",
                text = "En esto, descubrieron treinta o cuarenta molinos de viento que hay en aquel campo, y así como don Quijote los vio, dijo a su escudero: 'La ventura va guiando nuestras cosas mejor de lo que acertáramos a desear; porque ves allí, amigo Sancho Panza, donde se descubren treinta o pocos más desaforados gigantes, con quien pienso hacer batalla'."
            ),
            BookParagraphEntity(
                bookId = 2,
                paragraphIndex = 5,
                pageNumber = 4,
                chapterTitle = "Capítulo VIII: Los Molinos de Viento",
                text = "'¿Qué gigantes?', dijo Sancho Panza. 'Aquellos que allí ves', respondió su amo, 'de los brazos largos, que los suelen tener algunos de casi dos leguas'. 'Mire vuestra merced', respondió Sancho, 'que aquellos que allí se parecen no son gigantes, sino molinos de viento, y lo que en ellos parecen brazos son las aspas, que, volteadas del viento, hacen andar la piedra del molino'."
            ),
            BookParagraphEntity(
                bookId = 2,
                paragraphIndex = 6,
                pageNumber = 5,
                chapterTitle = "Capítulo VIII: Los Molinos de Viento",
                text = "'Bien parece', respondió don Quijote, 'que no estás cursado en esto de las aventuras: ellos son gigantes; y si tienes miedo, quítate de ahí, y ponte en oración en el espacio que yo voy a entrar con ellos en fiera y desigual batalla'."
            )
        )

        // 3. La Metamorfosis
        val metamorfosis = Book(
            id = 3,
            title = "La Metamorfosis",
            author = "Franz Kafka",
            filePath = "sample://la_metamorfosis",
            coverDrawableRes = null,
            totalPages = 16,
            totalParagraphs = 8,
            currentParagraphIndex = 0,
            currentPage = 1,
            progressPercent = 0f,
            isFavorite = true,
            lastReadTimestamp = now - 7200_000L,
            createdAt = now - 7200_000L,
            fileSizeBytes = 390_000L
        )

        val metamorfosisParagraphs = listOf(
            BookParagraphEntity(
                bookId = 3,
                paragraphIndex = 0,
                pageNumber = 1,
                chapterTitle = "Capítulo I: El Despertar",
                text = "Una mañana, tras un sueño intranquilo, Gregorio Samsa se despertó convertido en un monstruoso insecto. Estaba echado sobre el quitinoso caparazón de su espalda, y al levantar un poco la cabeza, vio su vientre oscuro, surcado por curvadas callosidades."
            ),
            BookParagraphEntity(
                bookId = 3,
                paragraphIndex = 1,
                pageNumber = 1,
                chapterTitle = "Capítulo I: El Despertar",
                text = "Innumerables patas, lamentablemente escuálidas en comparación con el grosor ordinario de sus piernas, se agitaban sin concierto ante sus ojos. '¿Qué me ha ocurrido?', pensó. No era un sueño. Su habitación, una auténtica habitación humana, si bien algo pequeña, permanecía tranquila entre las cuatro paredes bien conocidas."
            ),
            BookParagraphEntity(
                bookId = 3,
                paragraphIndex = 2,
                pageNumber = 2,
                chapterTitle = "Capítulo I: El Despertar",
                text = "Sobre la mesa reposaba esparcido un muestrario de paños: Samsa era viajante de comercio. La mirada de Gregorio se dirigió después hacia la ventana, y el tiempo lluvioso —se oían caer las gotas sobre la chapa de cinc del alféizar— le infundió una profunda melancolía."
            ),
            BookParagraphEntity(
                bookId = 3,
                paragraphIndex = 3,
                pageNumber = 3,
                chapterTitle = "Capítulo I: El Despertar",
                text = "'¿Qué pasaría si siguiese durmiendo un rato y me olvidase de todas estas locuras?', pensó. Pero era absolutamente irrealizable, porque estaba acostumbrado a dormir sobre el lado derecho, y su actual estado no le permitía adoptar semejante postura."
            )
        )

        return listOf(
            principito to principitoParagraphs,
            quijote to quijoteParagraphs,
            metamorfosis to metamorfosisParagraphs
        )
    }
}
