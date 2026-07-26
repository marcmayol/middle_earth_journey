package com.marcm.middleearthjourney.data

/**
 * Hito de una ruta.
 * @param distanceKm distancia acumulada desde el inicio de la ruta (en sentido de ida).
 * @param mapX/mapY posición normalizada en el lienzo del mapa (0..1).
 * @param lore curiosidad/contexto en español.
 */
data class Waypoint(
    val name: String,
    val distanceKm: Double,
    val mapX: Float,
    val mapY: Float,
    val lore: String,
    val region: String,
)

/** Rutas disponibles en la app. */
enum class RouteId { FRODO, BILBO }

/** Sentido del viaje: ida (a la meta) o vuelta a casa (mismos lugares en orden inverso). */
enum class Direction { FORWARD, RETURN }

/**
 * Definición de una ruta: sus hitos de ida, su meta y sus logros.
 * Los waypoints y logros se almacenan siempre en sentido de IDA; [orientedWaypoints] y
 * [orientedAchievements] los reorientan para el viaje de vuelta.
 */
data class RouteDef(
    val id: RouteId,
    val title: String,
    val goalName: String,
    val homeName: String,
    val totalKm: Double,
    val waypoints: List<Waypoint>,
    val achievements: List<Achievement>,
) {
    fun orientedWaypoints(dir: Direction): List<Waypoint> =
        if (dir == Direction.FORWARD) {
            waypoints
        } else {
            waypoints.reversed().map { wp ->
                wp.copy(distanceKm = totalKm - wp.distanceKm)
            }
        }

    fun orientedAchievements(dir: Direction): List<Achievement> =
        if (dir == Direction.FORWARD) {
            achievements
        } else {
            achievements
                .map { it.copy(unlockKm = (totalKm - it.unlockKm).coerceAtLeast(0.0)) }
                .sortedBy { it.unlockKm }
        }

    /** Nombre del destino según el sentido: la meta en ida, el hogar en vuelta. */
    fun destinationName(dir: Direction): String =
        if (dir == Direction.FORWARD) goalName else homeName
}

object Routes {

    val FRODO: RouteDef = RouteDef(
        id = RouteId.FRODO,
        title = "La marcha de Frodo",
        goalName = "Monte del Destino",
        homeName = "Bolsón Cerrado",
        totalKm = 2860.0,
        waypoints = listOf(
            Waypoint(
                name = "Bolsón Cerrado",
                distanceKm = 0.0,
                mapX = 0.18f, mapY = 0.32f,
                region = "La Comarca",
                lore = "Aquí empieza todo. Bolsón Cerrado, en Hobbiton, es el agujero-hobbit donde Frodo recibe el Anillo Único de manos de Bilbo. Pequeño, acogedor, con la chimenea siempre encendida. La Comarca representa todo lo que merece ser defendido: un mundo en paz que ignora la sombra que se cierne sobre Mordor.",
            ),
            Waypoint(
                name = "Bree",
                distanceKm = 190.0,
                mapX = 0.24f, mapY = 0.36f,
                region = "Tierras Solitarias",
                lore = "Encrucijada de caminos donde hombres y hobbits conviven. En la posada del Poney Pisador, los hobbits encuentran a Trancos, un montaraz harapiento que resultará ser Aragorn, heredero de Isildur. Aquí, Frodo desaparece accidentalmente al ponerse el Anillo por primera vez en público.",
            ),
            Waypoint(
                name = "Cima de los Vientos",
                distanceKm = 365.0,
                mapX = 0.31f, mapY = 0.36f,
                region = "Eriador",
                lore = "Ruinas de la antigua torre de vigilancia de Amon Sûl. En esta colina pelada, los Nazgûl atacan a la compañía bajo la luz de las estrellas. Frodo es herido por la daga de Morgul del Rey Brujo de Angmar: una herida que nunca cicatrizará del todo y que solo Elrond puede curar.",
            ),
            Waypoint(
                name = "Rivendel",
                distanceKm = 730.0,
                mapX = 0.42f, mapY = 0.34f,
                region = "Imladris",
                lore = "La Última Morada del Hogar. Refugio élfico fundado por Elrond Medioelfo en una garganta oculta del río Bruinen. Aquí se celebra el Concilio donde se decide destruir el Anillo en el Monte del Destino y se forma la Comunidad: Frodo, Sam, Merry, Pippin, Aragorn, Boromir, Legolas, Gimli y Gandalf.",
            ),
            Waypoint(
                name = "Hollin (Puertas de Moria)",
                distanceKm = 970.0,
                mapX = 0.46f, mapY = 0.46f,
                region = "Eregion",
                lore = "Tierra antigua de los Noldor, hoy desolada. Las puertas occidentales de Khazad-dûm están grabadas con runas élficas que dicen: 'Habla, amigo, y entra'. Gandalf tarda en darse cuenta de que la solución es decir 'mellon' (amigo en sindarin). Mientras, el Vigilante del Agua asoma sus tentáculos del lago oscuro.",
            ),
            Waypoint(
                name = "Khazad-dûm (Moria)",
                distanceKm = 1020.0,
                mapX = 0.50f, mapY = 0.48f,
                region = "Minas de Moria",
                lore = "El gran reino enano bajo las Montañas Nubladas. Cavaron demasiado profundo y despertaron al Balrog, demonio de la antigüedad. La Compañía cruza las salas en oscuridad. En la cámara de Mazarbul leen el diario que cuenta el fin de los enanos: 'no podemos salir, no podemos salir... vienen'. Gandalf cae con el Balrog en el Puente de Khazad-dûm: 'No podéis pasar'.",
            ),
            Waypoint(
                name = "Dimrill Dale",
                distanceKm = 1080.0,
                mapX = 0.53f, mapY = 0.50f,
                region = "Azanulbizar",
                lore = "El Valle del Arroyo Sombrío, salida oriental de Moria. Aquí Aragorn toma el mando y la Compañía llora a Gandalf bajo el cielo abierto, junto al Espejo de Kheled-zâram, un lago donde de día se ven las estrellas reflejadas. La piedra de Durin marca el lugar.",
            ),
            Waypoint(
                name = "Lothlórien",
                distanceKm = 1180.0,
                mapX = 0.55f, mapY = 0.54f,
                region = "El Bosque Dorado",
                lore = "El reino de Galadriel y Celeborn, donde el tiempo parece detenerse. En Caras Galadhon, ciudad construida en lo alto de los mallorn, la Dama del Bosque ofrece a Frodo el Espejo de Galadriel. Le regala una redoma con la luz de Eärendil: 'que sea para ti una luz en los lugares oscuros, cuando todas las demás luces se apaguen'.",
            ),
            Waypoint(
                name = "Parth Galen y Amon Hen",
                distanceKm = 1700.0,
                mapX = 0.58f, mapY = 0.66f,
                region = "Nen Hithoel",
                lore = "Tras descender el Anduin en las barcas élficas, llegan a las colinas a los pies del Rauros. En Amon Hen, la Colina del Ojo, Boromir intenta arrebatar el Anillo a Frodo, sucumbiendo a su poder. Frodo decide ir solo a Mordor; Sam lo descubre y se niega a abandonarlo: 'Voy contigo, señor Frodo'. Boromir muere defendiendo a Merry y Pippin de los uruk-hai.",
            ),
            Waypoint(
                name = "Emyn Muil",
                distanceKm = 1830.0,
                mapX = 0.62f, mapY = 0.68f,
                region = "Colinas pedregosas",
                lore = "Laberinto de riscos cortantes al este del Anduin. Frodo y Sam se pierden días enteros. Aquí capturan a Gollum, que los venía siguiendo desde Moria. Frodo le perdona la vida y le hace jurar por el Tesoro: Gollum se convierte en guía. Sméagol y Gollum empiezan a discutir dentro de su propia cabeza.",
            ),
            Waypoint(
                name = "Pantanos de los Muertos",
                distanceKm = 1990.0,
                mapX = 0.66f, mapY = 0.69f,
                region = "Dagorlad",
                lore = "Antiguo campo de batalla de la Última Alianza, ahora cubierto por una ciénaga. Bajo el agua, los rostros pálidos de los caídos —elfos, hombres, orcos— se conservan intactos, llamando a los caminantes a unirse a ellos. Gollum advierte: 'no mirar las luces'. Frodo casi se hunde, atraído por un rostro.",
            ),
            Waypoint(
                name = "Morannon (Puerta Negra)",
                distanceKm = 2150.0,
                mapX = 0.72f, mapY = 0.66f,
                region = "Cirith Gorgor",
                lore = "La puerta principal de Mordor: dos torres de hierro y una muralla impasable, custodiada por miles de orcos. Frodo y Sam descubren que entrar por aquí es imposible. Gollum les habla entonces de un 'otro camino', secreto, por las montañas del sur. Una decisión que cambiará todo.",
            ),
            Waypoint(
                name = "Henneth Annûn (Ithilien)",
                distanceKm = 2330.0,
                mapX = 0.70f, mapY = 0.74f,
                region = "Ithilien",
                lore = "Tras los caminos secretos por Ithilien, los montaraces de Faramir, hermano de Boromir, los capturan. En la cueva tras la Ventana del Anochecer, Faramir resiste la tentación del Anillo: 'no levantaría esa cosa, ni aunque la encontrara junto al camino'. Sam llora de alivio. Aquí prueban el conejo guisado con hierbas frescas.",
            ),
            Waypoint(
                name = "Cirith Ungol",
                distanceKm = 2540.0,
                mapX = 0.77f, mapY = 0.74f,
                region = "Ephel Dúath",
                lore = "El Paso de la Araña. Gollum los traiciona y los conduce al túnel de Ella-Laraña, criatura antiquísima descendiente de Ungoliant. Frodo cae paralizado por su aguijón. Sam, creyéndolo muerto, toma el Anillo y se enfrenta a la araña con Dardo y la redoma de Galadriel. Después, al oír a los orcos llevarse el cuerpo de Frodo, comprende que vive y se lanza a rescatarlo solo, dentro de la torre.",
            ),
            Waypoint(
                name = "Plateau de Gorgoroth",
                distanceKm = 2730.0,
                mapX = 0.82f, mapY = 0.72f,
                region = "Mordor",
                lore = "La meseta árida y ceniza al pie del Monte del Destino. Frodo está agotado, el Anillo pesa como plomo y le quema el cuello. Sam carga con él gran parte del camino: 'no puedo llevar el Anillo por ti, señor Frodo, pero puedo llevaros a vos'. Aragorn y los ejércitos del Oeste atacan la Puerta Negra para distraer al Ojo de Sauron.",
            ),
            Waypoint(
                name = "Monte del Destino (Sammath Naur)",
                distanceKm = 2860.0,
                mapX = 0.85f, mapY = 0.70f,
                region = "Orodruin",
                lore = "El volcán donde se forjó el Anillo y el único lugar donde puede destruirse. En las Grietas del Destino, Frodo finalmente sucumbe y se pone el Anillo: 'es mío'. Gollum lucha con él, le arranca el dedo de un mordisco y, exultante, cae al fuego con su Tesoro. El Anillo se destruye, las torres de Barad-dûr se derrumban, y la Sombra se rinde. Lo lograste.",
            ),
        ),
        achievements = listOf(
            Achievement("primer_paso", "El primer paso", "Has dejado Bolsón Cerrado. 'Es peligroso, Frodo, cruzar tu puerta...'", 0.1),
            Achievement("bree", "Encuentro con Trancos", "Has llegado a Bree y conocido al montaraz.", 190.0),
            Achievement("cima_vientos", "Sobreviviste a los Nazgûl", "Resististe el ataque en la Cima de los Vientos.", 365.0),
            Achievement("rivendel", "Concilio de Elrond", "Llegaste a Rivendel. La Compañía se forma.", 730.0),
            Achievement("moria_entrada", "Mellon", "Resolviste el acertijo de las puertas de Moria.", 970.0),
            Achievement("moria_cruzado", "No podéis pasar", "Cruzaste Moria. Gandalf cayó. Vosotros seguís.", 1020.0),
            Achievement("lothlorien", "Bendición de Galadriel", "Recibiste los regalos del Bosque Dorado.", 1180.0),
            Achievement("amon_hen", "La Comunidad se rompe", "En Parth Galen decidiste cargar tú con el Anillo.", 1700.0),
            Achievement("emyn_muil", "Gollum atado", "Capturaste y domesticaste a Sméagol.", 1830.0),
            Achievement("pantanos", "No mirar las luces", "Cruzaste los Pantanos de los Muertos.", 1990.0),
            Achievement("morannon", "Ante la Puerta Negra", "Comprendiste que no se puede entrar por delante.", 2150.0),
            Achievement("ithilien", "Faramir resiste", "El hermano de Boromir te dejó marchar libre.", 2330.0),
            Achievement("cirith_ungol", "Ella-Laraña vencida", "Sobreviviste al Paso de la Araña.", 2540.0),
            Achievement("gorgoroth", "A hombros de Sam", "Atravesaste la ceniza de Gorgoroth.", 2730.0),
            Achievement("destruido", "El Anillo es destruido", "Llegaste a las Grietas del Destino. La Sombra cae.", 2860.0),
        ),
    )

    val BILBO: RouteDef = RouteDef(
        id = RouteId.BILBO,
        title = "Ida y vuelta de Bilbo",
        goalName = "Erebor",
        homeName = "Bolsón Cerrado",
        totalKm = 1500.0,
        waypoints = listOf(
            Waypoint(
                name = "Bolsón Cerrado",
                distanceKm = 0.0,
                mapX = 0.18f, mapY = 0.32f,
                region = "La Comarca",
                lore = "Una mañana tranquila, Bilbo Bolsón fuma su pipa cuando Gandalf y trece enanos liderados por Thorin Escudo de Roble irrumpen en su agujero. Quieren un saqueador para recuperar el tesoro de Erebor, custodiado por el dragón Smaug. Bilbo sale corriendo sin pañuelo ni sombrero: comienza la aventura más improbable de la Comarca.",
            ),
            Waypoint(
                name = "Quebradas de los Trolls",
                distanceKm = 280.0,
                mapX = 0.30f, mapY = 0.33f,
                region = "Las Tierras Ásperas",
                lore = "Una noche fría, la compañía topa con tres trolls —Tom, Berto y Guillermo— asando carnero. Bilbo intenta robarles la bolsa y casi acaba en el caldero. Gandalf los entretiene discutiendo hasta el amanecer, y la luz del sol los convierte en piedra. En su cueva encuentran las espadas élficas Orcrist, Glamdring y un puñal que Bilbo llamará Dardo.",
            ),
            Waypoint(
                name = "Rivendel",
                distanceKm = 460.0,
                mapX = 0.42f, mapY = 0.30f,
                region = "Imladris",
                lore = "La Última Morada acogedora de Elrond. Aquí descansan y se reabastecen. Elrond examina el mapa de Thrór y descubre las letras lunares: solo visibles a la luz de la luna de la misma estación en que fueron escritas, revelan cómo encontrar la puerta secreta de Erebor el Día de Durin.",
            ),
            Waypoint(
                name = "El Paso Alto (Trasgos)",
                distanceKm = 620.0,
                mapX = 0.47f, mapY = 0.27f,
                region = "Montañas Nubladas",
                lore = "Una tormenta los obliga a refugiarse en una cueva que resulta ser una entrada a la ciudad de los Trasgos. Capturados y arrastrados a las profundidades, Bilbo se pierde en la oscuridad y encuentra un anillo de oro. En la orilla de un lago subterráneo juega a los acertijos con Gollum: '¿Qué tengo en el bolsillo?'. Escapa invisible gracias al Anillo.",
            ),
            Waypoint(
                name = "La Carroca (casa de Beorn)",
                distanceKm = 740.0,
                mapX = 0.53f, mapY = 0.25f,
                region = "El Vado del Anduin",
                lore = "Acorralados por huargos y trasgos en lo alto de unos pinos en llamas —'fuera de la sartén, dentro del fuego'—, las Águilas los rescatan. Llegan a la Carroca y a la casa de Beorn, el cambiapieles que de día es hombre y de noche, gran oso negro. Beorn los hospeda y les presta poneys para cruzar hasta el Bosque Negro.",
            ),
            Waypoint(
                name = "Bosque Negro",
                distanceKm = 1020.0,
                mapX = 0.63f, mapY = 0.24f,
                region = "Mirkwood",
                lore = "El bosque más oscuro y enfermo de la Tierra Media. Sin Gandalf, que los abandona aquí, la compañía sufre hambre, ilusiones y telarañas. Bilbo mata arañas gigantes con Dardo y, por primera vez, se siente valiente y nombra a su espada. Los Elfos del Bosque apresan a los enanos; Bilbo, invisible, urde la huida dentro de barriles vacíos río abajo.",
            ),
            Waypoint(
                name = "Esgaroth (Ciudad del Lago)",
                distanceKm = 1320.0,
                mapX = 0.78f, mapY = 0.22f,
                region = "Lago Largo",
                lore = "Ciudad de los Hombres construida sobre pilares en el Lago Largo, a la sombra de la Montaña Solitaria. Los enanos, empapados pero libres, son recibidos como héroes: la profecía decía que el Rey bajo la Montaña regresaría. Thorin proclama su linaje y la ciudad celebra. Desde aquí parten hacia Erebor.",
            ),
            Waypoint(
                name = "Erebor (La Montaña Solitaria)",
                distanceKm = 1500.0,
                mapX = 0.88f, mapY = 0.18f,
                region = "El Reino bajo la Montaña",
                lore = "Con las letras lunares, hallan la puerta secreta el Día de Durin. Bilbo desciende solo al corazón de la montaña y conversa con Smaug, robándole una copa y descubriendo el hueco en su coraza. El dragón, furioso, ataca Esgaroth y cae bajo la flecha negra de Bardo. Sigue la Batalla de los Cinco Ejércitos. Bilbo, con la Piedra del Arca, ayuda a sellar la paz y emprende el largo camino de vuelta a casa.",
            ),
        ),
        achievements = listOf(
            Achievement("b_primer_paso", "Una aventura inesperada", "Saliste de Bolsón Cerrado sin pañuelo ni sombrero.", 0.1),
            Achievement("b_trolls", "Trolls de piedra", "Sobreviviste a Tom, Berto y Guillermo. Y encontraste a Dardo.", 280.0),
            Achievement("b_rivendel", "Letras lunares", "En Rivendel, Elrond reveló el secreto del mapa de Thrór.", 460.0),
            Achievement("b_acertijos", "Acertijos en las tinieblas", "Ganaste a Gollum y encontraste el Anillo.", 620.0),
            Achievement("b_beorn", "Fuera de la sartén", "Las Águilas te salvaron y Beorn te dio cobijo.", 740.0),
            Achievement("b_aranas", "El nombre de la espada", "Venciste a las arañas del Bosque Negro: nombraste a Dardo.", 1020.0),
            Achievement("b_barriles", "Huida en barriles", "Llegaste a Esgaroth río abajo, dentro de un barril.", 1320.0),
            Achievement("b_erebor", "La Montaña Solitaria", "Llegaste a Erebor y hablaste con Smaug. Eres todo un saqueador.", 1500.0),
        ),
    )

    fun byId(id: RouteId): RouteDef = when (id) {
        RouteId.FRODO -> FRODO
        RouteId.BILBO -> BILBO
    }

    /** Índice del waypoint actual dentro de una lista orientada, según el km recorrido. */
    fun currentWaypointIndex(waypoints: List<Waypoint>, distanceKm: Double): Int {
        if (distanceKm <= 0) return 0
        for (i in waypoints.indices.reversed()) {
            if (distanceKm >= waypoints[i].distanceKm) return i
        }
        return 0
    }
}
