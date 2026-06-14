package com.marcm.middleearthjourney.ui.cinematic

import androidx.compose.ui.graphics.Color
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ───────── Paleta tinta/pergamino (de las cinemáticas del handoff) ─────────
private val INK = Color(0xFF4A3618)
private val INK2 = Color(0xFF5A4326)
private val GREEN = Color(0xFF6F7A44)
private val GREEN_DARK = Color(0xFF566036)
private val GREEN_TREE = Color(0xFF4F5A30)
private val SPIDER_TREE = Color(0xFF3F4A2A)
private val RIVER = Color(0xFF8AA6BD)
private val ROUTE_GOLD = Color(0xFFC08A3A)
private val GLOW_GOLD = Color(0xFFCAA251)
private val STAR_GOLD = Color(0xFFE0B052)
private val DARK_FILL = Color(0xFF3A2C16)
private val EMBER = Color(0xFFD0492F)
private val POS_RED = Color(0xFFB5392B)
private val LIGHT = Color(0xFFF3EAD0)
private val CAVE_EYE = Color(0xFFCFE3A0)
private val SNOW = Color(0xFFCDD6DF)
private val STORM = Color(0xFF9BB0C0)
private val FIRE = Color(0xFFC0763A)
private val COIN = Color(0xFFE9CF82)
private val DARKZ = Color(0xFF1C2228)
private val POOL = Color(0xFF2A3640)
private val REDX = Color(0xFF7E271B)
private val QMARK = Color(0xFF7E6A2F)
private val TETHER = Color(0xFF9A7A36)
private val STONE = Color(0xFF9A8A66)
private val MOON = Color(0xFFE7D9A8)
private val TROLL_EYE = Color(0xFFE3CF9E)
private val SPARK = Color(0xFFB07C2C)
private val SPARK2 = Color(0xFFC98A2E)
private val WHITE = Color(0xFFFFFFFF)

/** Pulso de brillo infinito (mejGlowP: 0.35 ↔ 0.85). */
class Pulse(val period: Float, val phase: Float)

sealed interface Prim
/** Trazo que se "dibuja" progresivamente (mejDraw). */
data class StrokePrim(val d: String, val width: Float, val color: Color, val delay: Float, val dur: Float, val opacity: Float) : Prim
/** Forma rellena que aparece por fundido (mejFadeIn), opcionalmente con pulso. */
data class FillPrim(val d: String, val color: Color, val delay: Float, val dur: Float, val opacity: Float, val pulse: Pulse?) : Prim
/** Texto que aparece por fundido. */
data class TextPrim(val x: Float, val y: Float, val text: String, val color: Color, val size: Float, val delay: Float, val dur: Float) : Prim

private fun n(v: Float): String {
    val r = (v * 100f).toLong() / 100.0
    return if (r == r.toLong().toDouble()) r.toLong().toString() else r.toString()
}
private fun circlePath(cx: Float, cy: Float, r: Float): String =
    "M${n(cx - r)} ${n(cy)} a${n(r)} ${n(r)} 0 1 0 ${n(2 * r)} 0 a${n(r)} ${n(r)} 0 1 0 ${n(-2 * r)} 0 Z"
private fun ellipsePath(cx: Float, cy: Float, rx: Float, ry: Float): String =
    "M${n(cx - rx)} ${n(cy)} a${n(rx)} ${n(ry)} 0 1 0 ${n(2 * rx)} 0 a${n(rx)} ${n(ry)} 0 1 0 ${n(-2 * rx)} 0 Z"

/** DSL que reproduce los helpers _p / _fade / _txt / _hob / _star del prototipo. */
class SceneScope {
    val prims = ArrayList<Prim>()
    fun p(d: String, w: Float = 2.2f, s: Color = INK, o: Float = 1f, dur: Float = 1.5f, d0: Float = 0f) {
        prims.add(StrokePrim(d, w, s, d0, dur, o))
    }
    fun fadePath(d: String, color: Color, d0: Float = 0f, dur: Float = 1f, o: Float = 1f, pulse: Pulse? = null) {
        prims.add(FillPrim(d, color, d0, dur, o, pulse))
    }
    fun fadeCircle(cx: Float, cy: Float, r: Float, color: Color, d0: Float = 0f, dur: Float = 1f, o: Float = 1f, pulse: Pulse? = null) =
        fadePath(circlePath(cx, cy, r), color, d0, dur, o, pulse)
    fun fadeEllipse(cx: Float, cy: Float, rx: Float, ry: Float, color: Color, d0: Float = 0f, dur: Float = 1f, o: Float = 1f) =
        fadePath(ellipsePath(cx, cy, rx, ry), color, d0, dur, o)
    fun txt(x: Float, y: Float, t: String, color: Color = INK2, size: Float = 16f, d0: Float = 0f, dur: Float = 0.8f) {
        prims.add(TextPrim(x, y, t, color, size, d0, dur))
    }

    fun hob(cx: Float, base: Float, d: Float = 0f, scared: Boolean = false, wave: Boolean = false, armsUp: Boolean = false) {
        p("M${n(cx-24)} ${n(base)} Q${n(cx-28)} ${n(base-58)} ${n(cx)} ${n(base-72)} Q${n(cx+28)} ${n(base-58)} ${n(cx+24)} ${n(base)}", w = 2.4f, dur = 1.1f, d0 = d)
        p("M${n(cx-20)} ${n(base-54)} Q${n(cx)} ${n(base-86)} ${n(cx+20)} ${n(base-54)}", w = 2.1f, dur = .8f, d0 = d+.4f)
        p("M${n(cx)} ${n(base-60)} m-14 0 a14 14 0 1 0 28 0 a14 14 0 1 0 -28 0", w = 2.1f, dur = .9f, d0 = d+.5f)
        p("M${n(cx-12)} ${n(base-68)} q5 -7 12 -5 q7 -2 12 5", w = 1.5f, dur = .6f, d0 = d+.8f, o = .7f)
        if (scared) {
            p("M${n(cx-9)} ${n(base-63)} l6 6 m0 -6 l-6 6", w = 1.4f, dur = .3f, d0 = d+1f)
            p("M${n(cx+3)} ${n(base-63)} l6 6 m0 -6 l-6 6", w = 1.4f, dur = .3f, d0 = d+1.05f)
        } else {
            fadeCircle(cx-5, base-61, 1.8f, INK, d+1f)
            fadeCircle(cx+5, base-61, 1.8f, INK, d+1.05f)
        }
        if (scared) p("M${n(cx-5)} ${n(base-50)} a5 5 0 0 0 10 0", w = 1.5f, dur = .3f, d0 = d+1.1f)
        else p("M${n(cx-6)} ${n(base-51)} q6 5 12 0", w = 1.5f, dur = .4f, d0 = d+1.1f)
        fadeEllipse(cx-8, base+4, 10f, 5.5f, INK2, d+.9f)
        fadeEllipse(cx+8, base+4, 10f, 5.5f, INK2, d+.95f)
        if (wave) p("M${n(cx+18)} ${n(base-36)} Q${n(cx+38)} ${n(base-52)} ${n(cx+30)} ${n(base-74)}", w = 2.1f, dur = .6f, d0 = d+1.2f)
        else if (armsUp) {
            p("M${n(cx-14)} ${n(base-30)} L${n(cx-28)} ${n(base-52)}", w = 2.1f, dur = .5f, d0 = d+1.2f)
            p("M${n(cx+14)} ${n(base-30)} L${n(cx+28)} ${n(base-52)}", w = 2.1f, dur = .5f, d0 = d+1.25f)
        }
    }

    fun star(cx: Float, cy: Float, d: Float = 0f) {
        fadeCircle(cx, cy, 27f, SPARK2, d, 1f, 1f, Pulse(2.4f, d+.3f))
        fadePath(
            "M${n(cx)} ${n(cy-19)} L${n(cx+4)} ${n(cy-5)} L${n(cx+18)} ${n(cy-3)} L${n(cx+6)} ${n(cy+6)} L${n(cx+10)} ${n(cy+20)} L${n(cx)} ${n(cy+11)} L${n(cx-10)} ${n(cy+20)} L${n(cx-6)} ${n(cy+6)} L${n(cx-18)} ${n(cy-3)} L${n(cx-4)} ${n(cy-5)} Z",
            STAR_GOLD, d+.2f,
        )
    }
}

private fun scene(block: SceneScope.() -> Unit): List<Prim> = SceneScope().apply(block).prims

// ───────────────────────── Escenas (portadas del prototipo) ─────────────────────────

private fun emblem() = scene {
    p("M40 56 H326 Q350 56 350 80 V780 Q350 804 326 804 H40 Q16 804 16 780 V80 Q16 56 40 56 Z", w = 2.4f, dur = 2f, s = INK2)
    p("M52 70 H314 Q336 70 336 92 V768 Q336 790 314 790 H52 Q30 790 30 768 V92 Q30 70 52 70 Z", w = 1.2f, dur = 1.8f, o = .7f, d0 = .3f)
    p("M183 300 m-86 0 a86 86 0 1 0 172 0 a86 86 0 1 0 -172 0", w = 2.4f, dur = 2f, d0 = .5f)
    p("M183 300 m-66 0 a66 66 0 1 0 132 0 a66 66 0 1 0 -132 0", w = 1.2f, dur = 1.6f, o = .7f, d0 = .8f)
    p("M138 336 L168 286 L186 312 L206 270 L230 336 Z", w = 2.2f, dur = 1.6f, d0 = 1.1f)
    p("M126 336 H242", w = 1.4f, dur = 1f, o = .6f, d0 = 1.5f)
    val cc = listOf("M40 110 q40 -50 78 -14", "M326 110 q-40 -50 -78 -14", "M40 726 q40 50 78 14", "M326 726 q-40 50 -78 14")
    cc.forEachIndexed { i, c -> p(c, w = 1.4f, dur = 1f, o = .65f, d0 = 1.2f + i * .12f) }
}

private fun wizard() = scene {
    p("M150 250 L188 150 L226 250", w = 2.6f, dur = 1.3f)
    p("M188 150 Q178 134 196 124", w = 2.2f, dur = .7f, d0 = 1f)
    p("M134 250 Q188 276 242 250", w = 2.6f, dur = 1f, d0 = .6f)
    p("M150 244 Q188 262 226 244", w = 1.4f, dur = .8f, d0 = 1.2f, o = .7f)
    fadeCircle(206f, 124f, 2.4f, INK, 1.4f)
    p("M168 262 q8 -4 14 0", w = 1.8f, dur = .5f, d0 = 1.5f)
    p("M196 262 q8 -4 14 0", w = 1.8f, dur = .5f, d0 = 1.6f)
    p("M164 270 Q150 360 188 414 Q226 360 212 270", w = 2.4f, dur = 1.6f, d0 = 1.4f)
    p("M176 290 Q172 350 188 392", w = 1.2f, dur = 1f, d0 = 1.9f, o = .6f)
    p("M200 290 Q204 350 188 392", w = 1.2f, dur = 1f, d0 = 2f, o = .6f)
    p("M150 270 Q108 440 150 500 L226 500 Q268 440 226 270", w = 2.2f, dur = 1.8f, d0 = 1.8f)
    p("M188 330 L188 500 M170 360 Q188 380 206 360", w = 1.2f, dur = 1.2f, d0 = 2.4f, o = .55f)
    p("M268 230 L286 506", w = 2.6f, dur = 1.4f, d0 = .4f)
    p("M277 222 m-12 0 a12 12 0 1 0 24 0 a12 12 0 1 0 -24 0", w = 2.2f, dur = .9f, d0 = 1.6f)
    fadeCircle(277f, 222f, 24f, SPARK2, 2f, 1f, 1f, Pulse(2.4f, 2f))
    listOf(262f to 196f, 298f to 206f, 252f to 236f).forEachIndexed { i, s ->
        val x = s.first; val y = s.second
        fadePath("M${n(x)} ${n(y-5)} L${n(x+1.5f)} ${n(y-1.5f)} L${n(x+5)} ${n(y)} L${n(x+1.5f)} ${n(y+1.5f)} L${n(x)} ${n(y+5)} L${n(x-1.5f)} ${n(y+1.5f)} L${n(x-5)} ${n(y)} L${n(x-1.5f)} ${n(y-1.5f)} Z", SPARK, 2.1f + i * .2f)
    }
}

private fun finale() = scene {
    val cx = 188f; val cy = 300f
    fadeCircle(cx, cy, 60f, GLOW_GOLD, .4f, 1f, 1f, Pulse(3f, .4f))
    p("M${n(cx)} ${n(cy)} m-80 0 a80 80 0 1 0 160 0 a80 80 0 1 0 -160 0", w = 2.2f, dur = 1.6f)
    for (i in 0 until 12) {
        val a = i / 12f * PI.toFloat() * 2f
        val x1 = cx + cos(a) * 54f; val y1 = cy + sin(a) * 54f
        val x2 = cx + cos(a) * 70f; val y2 = cy + sin(a) * 70f
        fadePath("M${n(x1)} ${n(y1)} L${n(x2)} ${n(y2)}", Color(0xFF6E521F), .8f + i * .06f, .5f)
    }
    p("M150 326 L180 280 L198 306 L218 268 L240 326 Z", w = 2.2f, dur = 1.4f, d0 = .6f)
}

private fun hobbits() = scene {
    hob(150f, 360f, d = 0f)
    hob(232f, 360f, d = .3f, wave = true)
    p("M104 366 H278", w = 1.6f, dur = 1.2f, d0 = .3f, o = .5f)
    txt(150f, 406f, "Berto", size = 15f, d0 = 1.8f)
    txt(232f, 406f, "Pim", size = 15f, d0 = 2f)
}

private fun mapFrodo() = scene {
    p("M60 90 C84 160 70 240 80 320 C88 392 74 470 86 540", w = 1.6f, dur = 1.6f, o = .55f)
    listOf(112f to 150f, 126f to 158f, 120f to 170f, 136f to 150f).forEachIndexed { i, s -> fadeCircle(s.first, s.second, 6f, GREEN, .2f + i * .12f, 1f, .7f) }
    p("M150 300 l22 -34 l20 28 l26 -40 l24 40 l22 -30 l22 36", w = 2f, dur = 1.6f, d0 = .6f)
    p("M180 338 l20 -28 l18 24 l22 -32 l20 36", w = 1.6f, dur = 1.4f, d0 = 1f, o = .7f)
    listOf(250f to 250f, 264f to 256f, 258f to 268f, 274f to 252f, 268f to 240f).forEachIndexed { i, s -> fadeCircle(s.first, s.second, 7f, GREEN_DARK, 1.2f + i * .1f, 1f, .6f) }
    fadeCircle(298f, 462f, 26f, GLOW_GOLD, 2.2f, 1f, 1f, Pulse(2.6f, 2.4f))
    p("M282 470 v-14 l11 -11 l11 11 v14 Z", w = 2f, dur = .9f, d0 = 2f)
    p("M302 470 v-11 l8 -8 l8 8 v11 Z", w = 1.8f, dur = .9f, d0 = 2.3f)
    p("M120 168 C150 210 150 270 192 292 C238 316 250 380 264 430 C276 456 288 470 298 462", w = 3.2f, dur = 2.6f, d0 = .8f, s = ROUTE_GOLD)
    listOf(192f to 292f, 264f to 430f).forEachIndexed { i, m -> fadeCircle(m.first, m.second, 4f, WHITE, 2.4f + i * .3f) }
    fadeCircle(120f, 168f, 7f, POS_RED, .6f)
}

private fun rescue() = scene {
    val cx = 150f; val base = 432f
    p("M58 432 H168", w = 2.2f, dur = 1f, o = .7f)
    p("M214 432 H318", w = 2.2f, dur = 1f, d0 = .2f, o = .7f)
    p("M168 432 Q191 488 214 432", w = 2.2f, dur = .9f, d0 = .6f)
    p("M${n(cx-22)} ${n(base)} Q${n(cx-26)} ${n(base-54)} ${n(cx)} ${n(base-66)} Q${n(cx+26)} ${n(base-54)} ${n(cx+22)} ${n(base)}", w = 2.4f, dur = 1.1f, d0 = .8f)
    p("M${n(cx)} ${n(base-66)} m-13 0 a13 13 0 1 0 26 0 a13 13 0 1 0 -26 0", w = 2.2f, dur = .9f, d0 = 1.2f)
    fadeCircle(cx-5, base-67, 1.8f, INK, 1.5f)
    fadeCircle(cx+5, base-67, 1.8f, INK, 1.5f)
    p("M${n(cx-5)} ${n(base-58)} a5 5 0 0 0 10 0", w = 1.6f, dur = .4f, d0 = 1.6f)
    p("M${n(cx-16)} ${n(base-34)} L${n(cx-32)} ${n(base-58)}", w = 2.2f, dur = .6f, d0 = 1.5f)
    p("M${n(cx+16)} ${n(base-34)} L${n(cx+32)} ${n(base-58)}", w = 2.2f, dur = .6f, d0 = 1.6f)
    p("M${n(cx-12)} ${n(base)} q12 8 24 0", w = 2f, dur = .5f, d0 = 1.3f)
    txt(cx+40, base-62, "!", color = REDX, size = 22f, d0 = 1.9f, dur = .5f)
    p("M250 432 Q246 392 268 384 Q290 392 286 432", w = 2.2f, dur = 1f, d0 = 1f)
    p("M268 380 m-12 0 a12 12 0 1 0 24 0 a12 12 0 1 0 -24 0", w = 2f, dur = .8f, d0 = 1.4f)
    p("M252 404 Q230 410 220 426", w = 2.2f, dur = .7f, d0 = 1.8f)
    star(236f, 150f, .5f)
    p("M236 178 Q205 280 ${n(cx)} ${n(base-66)}", w = 1.8f, dur = 1.4f, d0 = 2f, s = TETHER, o = .85f)
}

private fun bridge() = scene {
    val y = 384f
    p("M40 ${n(y)} H150", w = 2.2f, dur = 1f, o = .75f)
    p("M236 ${n(y)} H336", w = 2.2f, dur = 1f, d0 = .2f, o = .75f)
    p("M150 ${n(y+16)} q22 10 44 0 q22 -10 44 0", w = 2f, dur = 1.2f, d0 = .4f, s = RIVER)
    p("M150 ${n(y+26)} q22 10 44 0 q22 -10 44 0", w = 1.6f, dur = 1.2f, d0 = .6f, s = RIVER, o = .7f)
    p("M150 ${n(y-4)} l26 0 l0 -7 l-26 0", w = 2f, dur = .8f, d0 = .8f)
    p("M236 ${n(y-4)} l-26 0 l0 -7 l26 0", w = 2f, dur = .8f, d0 = 1f)
    fadeEllipse(193f, y+8, 15f, 7f, STONE, 2.2f)
    hob(110f, y, d = .6f)
    hob(193f, y-34, d = 1.2f, armsUp = true)
    star(274f, 168f, .4f)
}

private fun forest() = scene {
    listOf(
        Triple(90f, 304f, 90f), Triple(150f, 262f, 112f), Triple(252f, 282f, 100f),
        Triple(300f, 322f, 80f), Triple(200f, 332f, 120f), Triple(128f, 360f, 70f), Triple(280f, 206f, 90f),
    ).forEachIndexed { i, t ->
        val x = t.first; val yb = t.second; val h = t.third
        p("M${n(x)} ${n(yb)} L${n(x-h*0.5f)} ${n(yb)} L${n(x)} ${n(yb-h)} L${n(x+h*0.5f)} ${n(yb)} Z", w = 2f, dur = 1f, d0 = .1f + i * .12f, o = .85f, s = GREEN_TREE)
        p("M${n(x)} ${n(yb)} l0 14", w = 2f, dur = .4f, d0 = .4f + i * .12f, o = .7f)
    }
    p("M150 452 C118 420 184 398 204 430 C224 462 158 472 150 452", w = 1.8f, dur = 2f, d0 = .9f, s = Color(0xFF6E521F), o = .6f)
    hob(176f, 452f, d = 1.2f)
    hob(230f, 452f, d = 1.4f)
    txt(176f, 372f, "?", color = QMARK, size = 20f, d0 = 2f, dur = .6f)
    txt(232f, 368f, "?", color = QMARK, size = 15f, d0 = 2.2f, dur = .6f)
    star(290f, 158f, .4f)
    p("M290 182 L250 320", w = 1.6f, dur = 1f, d0 = 2.4f, s = TETHER, o = .7f)
}

private fun cave() = scene {
    p("M64 470 L64 304 Q188 198 312 304 L312 470", w = 2.4f, dur = 1.8f, o = .85f)
    fadePath("M118 470 Q150 360 212 372 Q282 384 272 470 Z", DARK_FILL, .9f, 1.2f)
    p("M118 470 Q150 360 212 372 Q282 384 272 470", w = 2f, dur = 1.6f, d0 = 1.1f, o = .55f)
    p("M232 392 q10 -6 0 -12", w = 1.6f, dur = .6f, d0 = 1.8f, o = .6f)
    txt(244f, 346f, "Z z z", color = INK2, size = 21f, d0 = 1.9f, dur = 1f)
    hob(132f, 470f, d = 1.2f, scared = true)
    hob(166f, 470f, d = 1.45f, scared = true)
    star(300f, 418f, 1.7f)
}

private fun storm() = scene {
    p("M36 366 l60 -120 l40 70 l68 -110 l60 130 l60 -72 l40 72", w = 2.2f, dur = 2f, o = .8f)
    listOf(262f, 300f, 338f, 376f).forEachIndexed { i, y ->
        p("M58 ${n(y)} q62 -14 124 0 q60 14 142 0", w = 1.4f, dur = 1f, d0 = .6f + i * .15f, s = STORM, o = .55f)
    }
    for (i in 0 until 13) {
        val x = (70 + (i * 53) % 236).toFloat(); val y = (250 + (i * 71) % 170).toFloat()
        fadeCircle(x, y, 2f, SNOW, 1f + i * .08f)
    }
    hob(176f, 446f, d = 1.2f, scared = true)
    hob(214f, 446f, d = 1.4f, scared = true)
    star(108f, 300f, .4f)
    p("M128 322 L165 408", w = 1.6f, dur = 1f, d0 = 2f, s = TETHER, o = .7f)
}

private fun home() = scene {
    p("M36 462 Q188 356 340 462", w = 2.2f, dur = 1.6f, o = .7f)
    fadeCircle(236f, 418f, 34f, STAR_GOLD, 1f, 1f, 1f, Pulse(2.8f, .8f))
    p("M236 452 a26 26 0 1 1 0 -52 a26 26 0 1 1 0 52 Z", w = 2.4f, dur = 1.4f, d0 = .6f)
    fadeCircle(250f, 426f, 2.2f, INK2, 1.8f)
    p("M120 462 Q172 452 209 454", w = 1.6f, dur = 1f, d0 = 1f, o = .5f)
    hob(110f, 462f, d = 1.2f, wave = true)
    hob(150f, 462f, d = 1.4f)
    fadePath("M128 336 c-4 -6 -12 -2 -12 4 c0 6 12 12 12 12 c0 0 12 -6 12 -12 c0 -6 -8 -10 -12 -4 Z", FIRE, 2f, .8f)
    fadePath("M162 316 c-4 -6 -12 -2 -12 4 c0 6 12 12 12 12 c0 0 12 -6 12 -12 c0 -6 -8 -10 -12 -4 Z", FIRE, 2.3f, .8f)
}

private fun roadback() = scene {
    fadeCircle(236f, 202f, 30f, STAR_GOLD, .4f, 1f, 1f, Pulse(3f, .4f))
    p("M236 202 m-34 0 a34 34 0 1 0 68 0 a34 34 0 1 0 -68 0", w = 1.6f, dur = 1.2f, d0 = .6f, o = .6f)
    p("M150 478 C150 426 232 426 222 362 C214 314 240 304 240 256", w = 3f, dur = 2.2f, d0 = .8f, s = TETHER)
    p("M150 478 C150 426 232 426 222 362 C214 314 240 304 240 256", w = 1f, dur = 2.2f, d0 = 1f, s = Color(0xFF6E521F), o = .5f)
    hob(206f, 362f, d = 1.4f)
    hob(233f, 362f, d = 1.6f)
    p("M68 472 Q58 410 90 400 Q122 410 112 472", w = 2.2f, dur = 1.2f, d0 = .6f)
    p("M70 400 L90 348 L110 400", w = 2.2f, dur = .9f, d0 = 1f)
    p("M80 404 Q90 442 100 404", w = 1.6f, dur = .7f, d0 = 1.4f, o = .7f)
    p("M106 430 Q132 420 130 398", w = 2f, dur = .6f, d0 = 1.7f)
}

// ── Bilbo ──
private fun hobOdo() = scene {
    hob(188f, 400f, d = 0f)
    p("M120 406 H256", w = 1.6f, dur = 1.2f, d0 = .3f, o = .5f)
    txt(188f, 446f, "Odo", size = 16f, d0 = 1.8f)
}

private fun mapMtn() = scene {
    p("M60 90 C84 160 70 240 80 320 C88 392 74 470 86 540", w = 1.6f, dur = 1.6f, o = .55f)
    listOf(112f to 150f, 126f to 158f, 120f to 170f, 136f to 150f).forEachIndexed { i, s -> fadeCircle(s.first, s.second, 6f, GREEN, .2f + i * .12f, 1f, .7f) }
    p("M140 320 l22 -30 l20 24 l24 -34 l24 40", w = 2f, dur = 1.5f, d0 = .6f)
    listOf(206f to 250f, 220f to 256f, 214f to 268f, 230f to 252f).forEachIndexed { i, s -> fadeCircle(s.first, s.second, 7f, GREEN_DARK, 1.2f + i * .1f, 1f, .6f) }
    fadeCircle(300f, 412f, 26f, EMBER, 2.2f, 1f, 1f, Pulse(2.6f, 2.4f))
    p("M250 470 L300 312 L350 470 Z", w = 2.4f, dur = 1.4f, d0 = 1.8f)
    p("M300 470 v-18 a8 8 0 0 0 -16 0 v18", w = 1.8f, dur = .8f, d0 = 2.4f)
    p("M120 168 C150 222 162 282 200 302 C246 328 250 392 270 432 C282 452 292 460 300 456", w = 3.2f, dur = 2.6f, d0 = .8f, s = ROUTE_GOLD)
    listOf(200f to 302f, 270f to 432f).forEachIndexed { i, m -> fadeCircle(m.first, m.second, 4f, WHITE, 2.4f + i * .3f) }
    fadeCircle(120f, 168f, 7f, POS_RED, .6f)
}

private fun trolls() = scene {
    val gy = 472f
    p("M48 ${n(gy)} H328", w = 2f, dur = 1.2f, o = .6f)
    fun troll(cx: Float, w: Float, h: Float, d: Float) {
        fadePath("M${n(cx-w)} ${n(gy)} Q${n(cx-w)} ${n(gy-h)} ${n(cx)} ${n(gy-h)} Q${n(cx+w)} ${n(gy-h)} ${n(cx+w)} ${n(gy)} Z", DARK_FILL, d, 1f)
        fadeCircle(cx-w*0.3f, gy-h*0.72f, 2.6f, TROLL_EYE, d+.6f)
        fadeCircle(cx+w*0.3f, gy-h*0.72f, 2.6f, TROLL_EYE, d+.65f)
    }
    troll(96f, 38f, 86f, .4f)
    troll(180f, 46f, 112f, .7f)
    troll(280f, 38f, 80f, 1f)
    p("M150 466 q18 14 36 0 l-5 -22 h-26 Z", w = 2f, dur = .9f, d0 = 1.4f)
    p("M160 472 q4 -10 8 -4 q5 -8 9 2", w = 1.6f, dur = .7f, d0 = 1.8f, s = FIRE)
    hob(132f, 472f, scared = true, d = 1.5f)
    star(300f, 150f, .4f)
    for (i in 0 until 5) {
        val a = i / 5f * PI.toFloat() - 0.2f
        p("M${n(300 + cos(a) * 30f)} ${n(150 + sin(a) * 30f)} l${n(cos(a) * 10f)} ${n(sin(a) * 10f)}", w = 1.4f, dur = .4f, d0 = 1f + i * .1f, s = SPARK2, o = .7f)
    }
}

private fun caveRiddle() = scene {
    p("M64 470 L64 304 Q188 198 312 304 L312 470", w = 2.4f, dur = 1.8f, o = .85f)
    fadePath("M196 460 Q244 412 292 460 Z", DARKZ, 1.1f, 1f)
    fadeEllipse(244f, 460f, 58f, 12f, POOL, 1f, 1f, 1f)
    fadeCircle(238f, 440f, 3f, CAVE_EYE, 1.6f, 1f, 1f, Pulse(2f, 1.6f))
    fadeCircle(252f, 440f, 3f, CAVE_EYE, 1.7f, 1f, 1f, Pulse(2f, 1.7f))
    hob(128f, 470f, scared = true, d = 1.2f)
    txt(150f, 330f, "?", color = QMARK, size = 22f, d0 = 2f, dur = .6f)
    star(150f, 182f, .4f)
}

private fun bear() = scene {
    fadeCircle(306f, 172f, 22f, MOON, .4f, 1f, 1f, Pulse(3f, .4f))
    p("M64 470 v-66 h86 v66", w = 2.2f, dur = 1.4f, d0 = .6f)
    p("M54 404 l63 -42 l63 42", w = 2.2f, dur = 1f, d0 = 1f)
    p("M94 470 v-42 h28 v42", w = 1.8f, dur = .8f, d0 = 1.4f)
    fadePath("M226 470 Q216 384 256 376 Q272 356 292 376 Q332 384 318 470 Z", DARK_FILL, 1.2f, 1.2f)
    fadeCircle(274f, 364f, 17f, DARK_FILL, 1.4f, 1f)
    fadeCircle(262f, 350f, 6f, DARK_FILL, 1.6f)
    fadeCircle(286f, 350f, 6f, DARK_FILL, 1.65f)
    hob(158f, 470f, d = 1.5f)
    star(306f, 260f, 1.6f)
}

private fun spiders() = scene {
    val cx = 190f; val cy = 300f
    listOf(Triple(78f, 310f, 120f), Triple(306f, 300f, 120f)).forEachIndexed { i, t ->
        p("M${n(t.first)} ${n(t.second)} L${n(t.first-t.third*0.4f)} ${n(t.second)} L${n(t.first)} ${n(t.second-t.third)} L${n(t.first+t.third*0.4f)} ${n(t.second)} Z", w = 2f, dur = 1f, d0 = .2f + i * .2f, o = .7f, s = SPIDER_TREE)
    }
    for (i in 0 until 8) {
        val a = i / 8f * PI.toFloat() * 2f
        p("M${n(cx)} ${n(cy)} L${n(cx + cos(a) * 92f)} ${n(cy + sin(a) * 92f)}", w = 1f, dur = .8f, d0 = .6f + i * .05f, o = .5f)
    }
    p("M${n(cx)} ${n(cy-32)} q32 0 32 32 q0 32 -32 32 q-32 0 -32 -32 q0 -32 32 -32", w = 1f, dur = 1.4f, d0 = 1.2f, o = .5f)
    fadeCircle(252f, 248f, 9f, DARK_FILL, 1.6f)
    for (i in 0 until 3) {
        p("M252 248 l${n(12 + i * 4f)} ${n(-8 + i * 8f)}", w = 1.2f, dur = .4f, d0 = 1.8f)
        p("M252 248 l${n(-(12 + i * 4f))} ${n(-8 + i * 8f)}", w = 1.2f, dur = .4f, d0 = 1.85f)
    }
    p("M168 384 q20 -42 40 0 q-20 52 -40 0 Z", w = 2f, dur = 1f, d0 = 1.4f)
    fadeCircle(182f, 374f, 1.8f, INK, 2f)
    fadeCircle(194f, 374f, 1.8f, INK, 2.05f)
    star(150f, 172f, .4f)
    p("M150 194 L178 360", w = 1.6f, dur = 1f, d0 = 2.2f, s = Color(0xFF9A7A36), o = .8f)
}

private fun dragon() = scene {
    p("M40 472 L150 250 L210 340 L300 232 L360 472", w = 2.2f, dur = 2f, o = .6f)
    fadePath("M88 472 Q190 422 304 472 Z", GLOW_GOLD, 1.4f, 1.2f, 1f, Pulse(3f, 1f))
    for (i in 0 until 5) {
        val x = (120 + i * 40).toFloat()
        fadeCircle(x, 466f, 3f, COIN, 1.6f + i * .08f)
    }
    p("M108 462 C88 422 150 402 192 422 C242 446 252 392 222 372 C200 358 208 332 234 336", w = 3f, dur = 2.4f, d0 = .8f, s = INK2)
    p("M192 422 q30 -52 72 -30", w = 1.6f, dur = 1.2f, d0 = 1.8f, o = .6f)
    p("M234 336 q24 -6 32 10 q-10 9 -23 5 q5 9 -6 9 q-9 0 -3 -11 Z", w = 2f, dur = 1f, d0 = 2.2f)
    fadeCircle(252f, 340f, 2.6f, EMBER, 2.4f, 1f, 1f, Pulse(1.8f, 2.4f))
    p("M150 412 l-8 -10 M170 400 l-7 -11 M196 392 l-6 -11", w = 1.4f, dur = .8f, d0 = 1.6f, o = .7f)
    hob(92f, 472f, scared = true, d = 1.4f)
}

/** Construye la lista de primitivas de una escena por su nombre. */
fun buildScene(name: String): List<Prim> = when (name) {
    "emblem" -> emblem()
    "wizard" -> wizard()
    "finale" -> finale()
    "hobbits" -> hobbits()
    "map" -> mapFrodo()
    "rescue" -> rescue()
    "bridge" -> bridge()
    "forest" -> forest()
    "cave" -> cave()
    "storm" -> storm()
    "home" -> home()
    "roadback" -> roadback()
    "hob" -> hobOdo()
    "mapMtn" -> mapMtn()
    "trolls" -> trolls()
    "caveR" -> caveRiddle()
    "bear" -> bear()
    "spiders" -> spiders()
    "dragon" -> dragon()
    else -> emblem()
}
