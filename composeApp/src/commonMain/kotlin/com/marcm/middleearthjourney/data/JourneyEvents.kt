package com.marcm.middleearthjourney.data

/** Suceso aleatorio del camino (cada ~5 días, con probabilidad, sin repetir en el viaje). */
data class JourneyEvent(
    val id: String,
    val title: String,
    val body: String,
)

object JourneyEvents {

    val FRODO = listOf(
        JourneyEvent("f_jinete", "Un jinete negro", "Al caer la tarde el aire se enfría de golpe y un caballo cruza el camino a lo lejos, sin rostro bajo la capucha. Te quedas muy quieto tras un seto hasta que el sonido de los cascos se pierde. No te ha visto. Hoy, no."),
        JourneyEvent("f_pan", "Pan de un desconocido", "Un labrador te ve descansar al borde del campo y, sin decir gran cosa, te tiende media hogaza y un trago de su bota. La comparte como si fuera lo más natural del mundo. Sigues con el estómago lleno y algo más ligero por dentro."),
        JourneyEvent("f_niebla", "Niebla en el sendero", "Una bruma espesa borra el camino y durante un buen rato andas sin saber bien hacia dónde. No te detienes; confías en tus pies. Cuando la niebla se abre, descubres que ibas bien encaminado todo el tiempo."),
        JourneyEvent("f_aguila", "Un águila sobre el valle", "Una sombra enorme planea en círculos muy alto. Por un instante temes lo peor, hasta que el sol arranca destellos dorados de unas alas inmensas. Un águila. Dicen que aparecen cuando más falta hace la esperanza."),
        JourneyEvent("f_estrella", "La luz de Eärendil", "La noche es tan negra que parece que la sombra lo ha tragado todo. Entonces, entre dos nubes, brilla una estrella sola y blanca. La miras hasta que el miedo se encoge. Hay una luz que las tinieblas no pueden apagar."),
        JourneyEvent("f_huellas", "Huellas élficas", "En el barro junto al arroyo encuentras pisadas ligerísimas, casi sin peso, que se alejan entre los árboles. Alguien de los Primeros Nacidos pasó por aquí hace poco. No estás tan solo en el camino como creías."),
        JourneyEvent("f_cancion", "Una canción al andar", "Sin darte cuenta empiezas a tararear la vieja tonada de Bilbo: «el Camino sigue y sigue». La melodía marca el ritmo de tus pasos y, durante varios kilómetros, el cansancio no pesa nada."),
        JourneyEvent("f_tormenta", "Tormenta en las colinas", "El cielo se rompe en agua y truenos y te empapa hasta los huesos. Te refugias bajo un saliente de roca, tiritando, viendo caer los rayos. Pasa. Todo pasa. Cuando escampa, el mundo huele a tierra limpia."),
        JourneyEvent("f_athelas", "Hojas de athelas", "Reconoces el aroma fresco de la hierba de los reyes creciendo entre las piedras. Frotas una hoja entre los dedos y respiras hondo: la cabeza se despeja, el ánimo se levanta. Pequeñas curas para un largo camino."),
        JourneyEvent("f_montaraz", "Un montaraz en la sombra", "Sientes que te observan y, al volverte, un hombre alto y callado te saluda apenas con la cabeza desde la linde del bosque. Un Dúnadan. Velan los caminos sin que nadie se lo agradezca. Sigues sabiéndote, en secreto, protegido."),
        JourneyEvent("f_duda", "El peso se hace insoportable", "Hoy cada paso cuesta el doble. Una voz dentro te dice que pares, que no merece la pena, que el destino está demasiado lejos. Te sientas. Respiras. Y recuerdas por qué empezaste. Te levantas y das un paso más. Solo uno. Y luego otro."),
        JourneyEvent("f_sam", "La voz de Sam", "En lo más bajo del ánimo recuerdas las palabras del jardinero: que hay algo bueno en este mundo, y que vale la pena luchar por ello. No estás cargando esto por nada. Hay un después. Y caminas hacia él."),
        JourneyEvent("f_lembas", "Pan del camino", "Compartes un bocado de lembas, el pan de viaje de los elfos: un solo mordisco llena el estómago de un hombre adulto. Hoy tus fuerzas duran más de lo que esperabas. El cuerpo agradece lo que la voluntad le pide."),
        JourneyEvent("f_gollum", "Algo te sigue", "Un chapoteo a tu espalda, un par de ojos pálidos que se apagan en cuanto te giras. Algo te sigue de lejos desde hace días. No es amistoso, pero tampoco ataca. Aprietas el paso y no miras atrás."),
        JourneyEvent("f_lobos", "Aullidos en la noche", "Lejos, en las colinas, unos aullidos largos se responden unos a otros. No son lobos comunes. Avivas el fuego, aprietas la espalda contra la roca y aguantas despierto hasta el alba. Por la mañana, solo huellas en la nieve… que se alejan."),
    )

    val BILBO = listOf(
        JourneyEvent("b_moneda", "Una moneda en el camino", "Brilla algo entre el polvo: una vieja moneda de cobre con la cara gastada. La guardas en el bolsillo por pura suerte. A veces la aventura empieza por reparar en las cosas pequeñas que otros pisan sin ver."),
        JourneyEvent("b_zorro", "Un zorro curioso", "Un zorro se sienta a unos pasos y te mira ladeando la cabeza, sin miedo, como preguntándose qué hace un hobbit tan lejos de casa. Os observáis un rato. Luego se va, y tú también, los dos con vuestros asuntos."),
        JourneyEvent("b_pinos", "Olor a pinos", "El aire se llena de resina y agujas calientes al sol. Respiras hondo y, sin saber por qué, te sientes valiente, como aquella vez que nombraste tu espada. Hueles a bosque y a posibilidad."),
        JourneyEvent("b_cancion", "Canción de caminantes", "Te sorprendes silbando una tonada de los enanos sobre montañas brumosas y oro antiguo. Marcas el paso con ella. Quién te ha visto, Bolsón: tú, que detestabas la música a deshora."),
        JourneyEvent("b_arcoiris", "Un arcoíris doble", "Pasa un chaparrón rápido y, al abrirse el cielo, se tienden dos arcoíris sobre el valle. Te paras a mirarlos como un crío. La Piedra del Arca no brillaría más que esto. Algunos tesoros no se pueden guardar en un bolsillo."),
        JourneyEvent("b_setas", "Un festín de setas", "Encuentras un corro de setas gordas y comestibles junto a un tronco caído. Las asas en un fuego pequeño y cenas como un rey. Para un hobbit, media aventura es una buena comida a tiempo."),
        JourneyEvent("b_aguilas", "Las Águilas pasan", "Muy alto, una bandada de grandes águilas cruza hacia las montañas. Recuerdas que una vez te sacaron del fuego cuando todo estaba perdido. La ayuda llega, a veces, cuando ya no la esperas."),
        JourneyEvent("b_anillo", "Un peso en el bolsillo", "Palpas algo frío y redondo en el bolsillo y por un instante el mundo se vuelve tentador y peligroso a la vez. Cierras la mano, respiras, y sigues andando. Hay cosas que es mejor no usar aunque se puedan."),
        JourneyEvent("b_rio", "Barriles río abajo", "Cruzas un puente sobre un río veloz y te ríes solo recordando aquella huida absurda metido en un barril. Lo más ridículo a veces es lo que te salva. No siempre hay que ir con elegancia: basta con llegar."),
        JourneyEvent("b_estrellas", "Joyas en el cielo", "Acampas y el cielo se llena de estrellas como las gemas que cantaban los enanos. Te tumbas a mirarlas. Saliste de casa para ver justo esto: el mundo grande, ahí fuera, esperándote."),
        JourneyEvent("b_beorn", "Rastro de un gran oso", "Hallas huellas enormes de oso junto al sendero, frescas. Recuerdas a Beorn, el cambiapieles que te dio cobijo. No todo lo que parece fiero quiere hacerte daño. Sigues con respeto, no con miedo."),
        JourneyEvent("b_valor", "Más valiente de lo que creías", "Hoy el camino se hace cuesta arriba y, en vez de quejarte, sonríes. El hobbit asustado que salió de Bolsón Cerrado ya no está. Mira de lo que eres capaz cuando sigues poniendo un pie delante del otro."),
        JourneyEvent("b_mariposas", "Mariposas negras", "Trepas a lo alto de un roble para orientarte y descubres un mar de mariposas negras meciéndose sobre las copas, bajo un sol que abajo no llega. Por un momento el bosque oscuro parece casi hermoso. Bajas con el rumbo claro."),
        JourneyEvent("b_arroyo", "Un arroyo encantado", "Un arroyo de aguas negras cruza el sendero; el aire huele a sueño. Recuerdas el aviso: no beber, no tocar. Saltas con cuidado a la otra orilla. El que se duerme en este bosque tarda mucho en despertar."),
        JourneyEvent("b_humo", "Anillos de humo", "Al anochecer enciendes la pipa y, casi sin querer, sueltas un anillo de humo perfecto que sube y se deshace entre las ramas. Sonríes. Aunque estés lejísimos de casa, un buen humo después de andar siempre sabe a hogar."),
    )

    fun forRoute(id: RouteId): List<JourneyEvent> = when (id) {
        RouteId.FRODO -> FRODO
        RouteId.BILBO -> BILBO
    }

    fun byId(id: RouteId, eventId: String): JourneyEvent? = forRoute(id).firstOrNull { it.id == eventId }
}
