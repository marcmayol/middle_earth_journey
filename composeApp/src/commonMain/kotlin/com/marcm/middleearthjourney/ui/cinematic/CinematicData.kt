package com.marcm.middleearthjourney.ui.cinematic

import com.marcm.middleearthjourney.data.Direction
import com.marcm.middleearthjourney.data.RouteId

data class CineScene(val builder: String, val dur: Float, val caption: String)
/** @param unlockKm km de viaje a partir del cual el capítulo queda desbloqueado. */
data class CineChapter(val title: String, val sub: String, val unlockKm: Double, val scenes: List<CineScene>)
data class CineCollection(
    val eyebrow: String,
    val title: String,
    val tagline: String,
    val chapters: List<CineChapter>,
)

private fun sc(b: String, dur: Float, t: String) = CineScene(b, dur, t)

private val FRODO_IDA = CineCollection(
    eyebrow = "La marcha de Frodo · ida",
    title = "Crónicas de la marcha",
    tagline = "El mago te confía a Berto y Pim, dos hobbits patosos.",
    chapters = listOf(
        CineChapter("El encargo", "El encargo del mago · 0 km", 0.0, listOf(
            sc("emblem", 6f, "Ah, por fin apareces, caminante. Acércate. Tengo un encargo para ti… y muy poca paciencia."),
            sc("wizard", 8f, "Yo soy demasiado viejo para los caminos. Pero tú no. Necesito que cuides de dos hobbits con más coraje que sentido común."),
            sc("hobbits", 8f, "Se llaman Berto y Pim. Valientes, leales… y un completo desastre. Serían capaces de perderse en su propio jardín."),
            sc("map", 8f, "Su hogar está a dos mil ochocientos sesenta kilómetros. Cada paso que tú des, en tu mundo, los empujará un paso en el suyo."),
            sc("rescue", 8f, "Se meterán en líos: pozos, ríos y cosas peores. Y tú, cada día, les salvarás el pellejo sin que ellos se enteren jamás."),
            sc("finale", 6f, "¿Listo? Ellos cuentan contigo, aunque nunca lo sabrán. Anda… que comience la marcha."),
        )),
        CineChapter("El Puente Roto", "Río Cascabel · 280 km", 280.0, listOf(
            sc("bridge", 7f, "Berto y Pim llegaron al río Cascabel, donde el puente llevaba roto desde tiempos de su bisabuelo."),
            sc("bridge", 8f, "Pim, cómo no, quiso cruzarlo de un salto. Tú deslizaste la piedra justa bajo su pie. Chapuzón evitado."),
        )),
        CineChapter("El Bosque de los Susurros", "Bosque de los Susurros · 730 km", 730.0, listOf(
            sc("forest", 8f, "El Bosque de los Susurros se los tragó enteros. Tres veces pasaron junto al mismo árbol torcido sin notarlo."),
            sc("forest", 8f, "Antes de que cayera la noche, torciste con suavidad su brújula interior hacia el sendero correcto."),
        )),
        CineChapter("La Cueva del Trasgo", "Cueva del Trasgo · 1.080 km", 1080.0, listOf(
            sc("cave", 8f, "Huyendo de la lluvia, se metieron en la cueva equivocada. Algo enorme roncaba en la oscuridad."),
            sc("cave", 8f, "Conteniendo el aliento, los guiaste de puntillas hasta la salida. El trasgo jamás llegó a despertar."),
        )),
        CineChapter("El Paso Helado", "El Paso Helado · 1.620 km", 1620.0, listOf(
            sc("storm", 7f, "En lo alto del paso, la ventisca borró el camino… y por poco también a los dos hobbits."),
            sc("storm", 8f, "Paso a paso, los empujaste contra el viento hasta el otro lado de la montaña."),
        )),
        CineChapter("El Hogar", "¡Llegada! · 2.860 km", 2860.0, listOf(
            sc("home", 8f, "Y una mañana, tras dos mil ochocientos sesenta kilómetros, brilló ante ellos la luz de su hogar."),
            sc("home", 8f, "Berto y Pim cruzaron la puerta, sanos y salvos, sin saber jamás quién velaba cada uno de sus pasos."),
            sc("wizard", 6f, "Buen trabajo, caminante. Yo siempre lo supe."),
        )),
    ),
)

private val FRODO_VUELTA = CineCollection(
    eyebrow = "La marcha de Frodo · regreso",
    title = "La vuelta a casa",
    tagline = "Se dejaron la mejor cuchara en la otra punta del mundo.",
    chapters = listOf(
        CineChapter("El regreso", "El regreso · 2.860 km a casa", 0.0, listOf(
            sc("roadback", 8f, "Berto y Pim ya estaban en casa, los pies en alto… cuando recordaron que se habían dejado la mejor cuchara en la otra punta del mundo."),
            sc("wizard", 8f, "Ridículo, lo sé. Pero un hobbit y su cuchara no se separan jamás. Otra vez al camino, caminante: tráemelos de vuelta."),
            sc("finale", 6f, "El mismo camino, los mismos líos… y la misma sombra dorada velando sus pasos. Que comience el regreso."),
        )),
        CineChapter("El Paso Helado", "El Paso Helado · 1.620 km a casa", 1240.0, listOf(
            sc("storm", 7f, "La montaña no los había echado de menos. La ventisca volvía a borrar el sendero ante ellos."),
            sc("storm", 8f, "Pero esta vez conocían el truco… y tú, el atajo. Cruzaron al otro lado sin un solo rasguño."),
        )),
        CineChapter("La Cueva del Trasgo", "Cueva del Trasgo · 1.080 km a casa", 1780.0, listOf(
            sc("cave", 7f, "En la cueva, el viejo trasgo seguía roncando en su rincón, ajeno a todo."),
            sc("cave", 8f, "Pasaron de puntillas, le birlaron una manzana de la despensa y salieron aguantando la risa."),
        )),
        CineChapter("El Bosque de los Susurros", "Bosque de los Susurros · 730 km a casa", 2130.0, listOf(
            sc("forest", 7f, "El bosque lo intentó una vez más, girando sus caminos en círculos para enredarlos."),
            sc("forest", 8f, "Tú enderezaste el rumbo, y los susurros se quedaron con las ganas."),
        )),
        CineChapter("El Puente Roto", "Río Cascabel · 280 km a casa", 2580.0, listOf(
            sc("bridge", 7f, "El puente del río Cascabel seguía tan roto como el primer día."),
            sc("bridge", 8f, "Pim saltó de nuevo, cómo no. Y de nuevo, la piedra justa apareció bajo su pie."),
        )),
        CineChapter("En casa, por fin", "En casa · ¡llegada!", 2860.0, listOf(
            sc("home", 8f, "Y al caer una tarde dorada, su agujero los recibió con la chimenea ya encendida."),
            sc("home", 8f, "Berto colgó el abrigo, Pim recuperó su cuchara, y ninguno supo jamás quién los trajo de vuelta."),
            sc("wizard", 6f, "Bien hecho, caminante. Ahora sí, descansa. …Hasta la próxima."),
        )),
    ),
)

private val BILBO_IDA = CineCollection(
    eyebrow = "Ida y vuelta de Bilbo · ida",
    title = "Crónicas del dragón",
    tagline = "Odo va a comprobar si el dragón está de verdad muerto.",
    chapters = listOf(
        CineChapter("El encargo", "El asunto del dragón · 0 km", 0.0, listOf(
            sc("emblem", 6f, "Una última cosa, caminante. Verás… hay un asunto pendiente. Un asunto con escamas."),
            sc("wizard", 8f, "Hace años, otro hobbit fue a la Montaña Solitaria a tratar con cierto dragón. Volvió rico… pero algo distraído."),
            sc("hob", 9f, "Nunca llegó a comprobar si el dragón estaba del todo muerto. Por eso envío a Odo: el hobbit más prudente que conozco. Y el más asustadizo."),
            sc("mapMtn", 8f, "La Montaña aguarda al este, tras bosques y montañas. Cada paso tuyo llevará a Odo un paso más cerca de… ejem… asegurarse."),
            sc("finale", 6f, "Cuídamelo, ¿quieres? Tiene buen corazón y una espantosa puntería. Que comience la marcha."),
        )),
        CineChapter("Los Trolls de Piedra", "El claro del bosque · 240 km", 240.0, listOf(
            sc("trolls", 7f, "En el primer claro, tres trolls enormes discutían a gritos cómo cocinar a un hobbit tan pequeño."),
            sc("trolls", 8f, "Estiraste la discusión un poco más de lo normal… y el sol los volvió de piedra antes del primer bocado."),
        )),
        CineChapter("La Criatura de la Cueva", "Túneles de las Montañas · 620 km", 620.0, listOf(
            sc("caveR", 8f, "Bajo las montañas, Odo se perdió en túneles húmedos donde algo viscoso acechaba en la penumbra."),
            sc("caveR", 8f, "Le soplaste la respuesta a un acertijo imposible, y Odo salió a la luz justo a tiempo."),
        )),
        CineChapter("El Hombre-Oso", "La casa del cambiapieles · 980 km", 980.0, listOf(
            sc("bear", 7f, "Acorralados, llamaron a la puerta del cambiapieles: hombre de día, enorme oso al caer la noche."),
            sc("bear", 8f, "Con un par de buenos modales que tú le soplaste, Odo se ganó cama, cena y un poni prestado."),
        )),
        CineChapter("Las Arañas del Bosque", "El Bosque Oscuro · 1.380 km", 1380.0, listOf(
            sc("spiders", 7f, "En el Bosque Oscuro, las arañas envolvieron a Odo en seda antes de que pudiera gritar."),
            sc("spiders", 8f, "Guiaste su manita hasta el cuchillo. Un corte, dos… y se soltó casi con elegancia."),
        )),
        CineChapter("La Montaña Solitaria", "¡Llegada! · 1.500 km", 1500.0, listOf(
            sc("dragon", 8f, "Y al fin, la Montaña Solitaria. Dentro, sobre un mar de oro, dormía el dragón. Respirando."),
            sc("dragon", 8f, "Odo tragó saliva, se acercó de puntillas… y comprobó que, en efecto, seguía muy vivo."),
            sc("wizard", 7f, "¿Vivo? …Vaya. Bueno. Eso, caminante, ya es historia para la próxima ruta. Buen trabajo."),
        )),
    ),
)

private val BILBO_VUELTA = CineCollection(
    eyebrow = "Ida y vuelta de Bilbo · regreso",
    title = "Crónicas de la huida",
    tagline = "Odo huye del dragón… que estaba muy despierto.",
    chapters = listOf(
        CineChapter("La huida", "La huida · Montaña Solitaria", 0.0, listOf(
            sc("dragon", 8f, "El dragón abrió un ojo. Luego el otro. Y Odo comprendió que confirmar su buena salud había sido… una pésima idea."),
            sc("wizard", 7f, "¡Corre, Odo, corre! Caminante, esto se ha torcido. Sácalo de la Montaña de una sola pieza."),
            sc("finale", 6f, "Mil quinientos kilómetros de vuelta a casa, y un dragón muy despierto detrás. Que empiece la huida."),
        )),
        CineChapter("Las Arañas del Bosque", "El Bosque Oscuro · 1.380 km a casa", 120.0, listOf(
            sc("spiders", 7f, "En el Bosque Oscuro, las arañas recordaban a Odo. Y guardaban rencor."),
            sc("spiders", 8f, "Pero un hobbit perseguido por un dragón corre como ninguno. Cortaste la tela y salió disparado."),
        )),
        CineChapter("El Hombre-Oso", "La casa del cambiapieles · 980 km a casa", 520.0, listOf(
            sc("bear", 7f, "El cambiapieles abrió la puerta, vio el cielo en llamas a lo lejos y suspiró con paciencia."),
            sc("bear", 8f, "Le dio a Odo el poni más veloz y un consejo: «no mires atrás». Tú te aseguraste de que lo cumpliera."),
        )),
        CineChapter("La Cueva de los Acertijos", "Túneles de las Montañas · 620 km a casa", 880.0, listOf(
            sc("caveR", 7f, "De vuelta en los túneles, la criatura viscosa esperaba su revancha en la penumbra."),
            sc("caveR", 8f, "Le soplaste un acertijo aún más retorcido, y Odo escapó mientras ella seguía pensando."),
        )),
        CineChapter("Los Trolls de Piedra", "El claro del bosque · 240 km a casa", 1260.0, listOf(
            sc("trolls", 7f, "En el claro, los tres trolls seguían petrificados, con cara de tontos para toda la eternidad."),
            sc("trolls", 8f, "Odo les hizo una reverencia burlona y, esta vez, pasó de largo sin un solo rasguño."),
        )),
        CineChapter("En casa", "En casa · ¡llegada!", 1500.0, listOf(
            sc("hob", 8f, "Y al fin, su puerta redonda. Odo entró, echó tres cerrojos y se escondió bajo la cama."),
            sc("wizard", 7f, "¿El dragón? Bah. Se cansó en la frontera y dio media vuelta. Misión cumplida, caminante… más o menos."),
            sc("finale", 6f, "Y así, paso a paso, también la huida encontró su final. Buen trabajo."),
        )),
    ),
)

fun collectionFor(routeId: RouteId, direction: Direction): CineCollection = when {
    routeId == RouteId.FRODO && direction == Direction.FORWARD -> FRODO_IDA
    routeId == RouteId.FRODO -> FRODO_VUELTA
    direction == Direction.FORWARD -> BILBO_IDA
    else -> BILBO_VUELTA
}
