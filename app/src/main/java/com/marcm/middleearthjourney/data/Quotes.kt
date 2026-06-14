package com.marcm.middleearthjourney.data

object Quotes {
    val all: List<String> = listOf(
        "No todos los que vagan están perdidos.",
        "Aún hay esperanza mientras la Compañía sea fiel.",
        "Hasta la persona más pequeña puede cambiar el curso del futuro.",
        "Es peligroso, Frodo, cruzar tu puerta. Te metes en el camino, y si no cuidas tus pies, no sabes adónde te pueden llevar.",
        "Todo lo que tenemos que decidir es qué hacer con el tiempo que se nos ha dado.",
        "No puedo llevar el Anillo por ti, señor Frodo, pero puedo llevaros a vos.",
        "Mucho dolor habrás de soportar; pero gran será también tu recompensa.",
        "Que sea para ti una luz en los lugares oscuros, cuando todas las demás luces se apaguen.",
        "Aún queda algo bueno en este mundo, señor Frodo, y vale la pena luchar por ello.",
        "Las raíces profundas no se hielan.",
        "Speak friend, and enter. (Habla, amigo, y entra.)",
        "Un paso más, otro paso. Eso es todo lo que tengo que hacer.",
        "El mundo está cambiando, lo siento en el agua, lo siento en la tierra.",
        "No es la fuerza del cuerpo lo que importa, sino la fuerza del espíritu.",
        "Hoy no, Frodo. Hoy no.",
    )

    fun forDay(epochDay: Long): String = all[(epochDay.mod(all.size.toLong())).toInt()]
}
