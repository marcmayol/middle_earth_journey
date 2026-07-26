package com.marcm.middleearthjourney.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.marcm.middleearthjourney.MainActivity
import com.marcm.middleearthjourney.MiddleEarthApp
import com.marcm.middleearthjourney.R
import com.marcm.middleearthjourney.data.Direction
import com.marcm.middleearthjourney.data.JourneyEvent
import com.marcm.middleearthjourney.data.JourneyEvents
import com.marcm.middleearthjourney.data.RouteId
import com.marcm.middleearthjourney.data.Routes
import com.marcm.middleearthjourney.data.StepRepository
import com.marcm.middleearthjourney.data.stepsToKm
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.random.Random

class StepTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var eventObserveJob: Job? = null
    private var refreshJob: Job? = null
    private var eventResolvingUpTo = -1

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val repo = (application as MiddleEarthApp).stepRepository

        // Notificación inicial con los datos ACTUALES (no un placeholder), para que ya
        // muestre pasos/km al arrancar y no se quede en "Contando tus pasos…".
        val (t0, x0) = notifContent(
            repo.journeySteps.value, repo.todaySteps.value,
            repo.activeRoute.value, repo.activeDirection.value, repo.heightCm.value,
        )
        val initial = buildNotification(t0, x0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
        } else {
            startForeground(NOTIF_ID, initial)
        }

        scope.launch { repo.startListening() }

        // En modo Health Connect releemos periódicamente para recoger los pasos que el
        // smartwatch va sincronizando. repo.refresh() es no-op si la fuente es el sensor.
        if (refreshJob == null) {
            refreshJob = scope.launch {
                while (true) {
                    delay(HC_REFRESH_INTERVAL_MS)
                    repo.refresh()
                }
            }
        }

        if (observeJob == null) {
            observeJob = scope.launch {
                combine(
                    repo.journeySteps,
                    repo.todaySteps,
                    repo.activeRoute,
                    repo.activeDirection,
                    repo.heightCm,
                ) { steps, today, route, direction, height ->
                    notifContent(steps, today, route, direction, height)
                }.collectLatest { (title, text) ->
                    val nm = getSystemService(NotificationManager::class.java)
                    nm.notify(NOTIF_ID, buildNotification(title, text))
                }
            }
        }

        // Resolución de sucesos aleatorios (cada 5 días, solo tras 3 km hoy) + notificación.
        if (eventObserveJob == null) {
            eventObserveJob = scope.launch {
                combine(
                    repo.journeyStart,
                    repo.todaySteps,
                    repo.activeRoute,
                    repo.activeDirection,
                    repo.eventCheckpoint,
                ) { js, today, route, direction, cp -> EventCtx(js, today, route, direction, cp) }
                    .collectLatest { ctx -> resolveEvents(repo, ctx) }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        observeJob?.cancel()
        observeJob = null
        eventObserveJob?.cancel()
        eventObserveJob = null
        refreshJob?.cancel()
        refreshJob = null
        super.onDestroy()
    }

    private fun resolveEvents(repo: StepRepository, ctx: EventCtx) {
        val js = ctx.journeyStart ?: return
        val daysElapsed = (LocalDate.now().toEpochDay() - js).coerceAtLeast(0L).toInt() + 1
        val cp = daysElapsed / 5
        if (cp <= ctx.checkpoint || cp <= eventResolvingUpTo) return
        if (stepsToKm(ctx.today) < MIN_KM_FOR_EVENT) return // solo si ya has andado 3 km hoy
        eventResolvingUpTo = cp
        val pool = JourneyEvents.forRoute(ctx.route)
        val seen = repo.eventSeen.value.toMutableList()
        var pending: JourneyEvent? = null
        for (c in (ctx.checkpoint + 1)..cp) {
            val seed = js * 1_000_003L xor
                (ctx.route.ordinal.toLong() shl 16) xor (ctx.direction.ordinal.toLong() shl 8) xor c.toLong()
            val rng = Random(seed)
            if (rng.nextFloat() < EVENT_PROBABILITY) {
                val unseen = pool.filter { it.id !in seen }
                if (unseen.isNotEmpty()) {
                    val pick = unseen[rng.nextInt(unseen.size)]
                    seen.add(pick.id)
                    if (c == cp) pending = pick // solo el más reciente salta como popup/notificación
                }
            }
        }
        scope.launch { repo.applyEventResolution(cp, seen, pending?.id) }
        pending?.let { notifyEvent(it) }
    }

    private fun ensureEventChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(EVENT_CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            EVENT_CHANNEL_ID,
            "Sucesos del camino",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Te avisa cuando ocurre algo en tu viaje por la Tierra Media."
            setShowBadge(true)
        }
        nm.createNotificationChannel(channel)
    }

    private fun notifyEvent(event: JourneyEvent) {
        ensureEventChannel()
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notif = NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(event.title)
            .setContentText(event.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(event.body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setContentIntent(pi)
            .build()
        runCatching { getSystemService(NotificationManager::class.java).notify(EVENT_NOTIF_ID, notif) }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Progreso a Mordor",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Muestra los pasos y kilómetros recorridos de Frodo."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        nm.createNotificationChannel(channel)
    }

    /** Título («Hoy: N pasos · K km») y texto (tramo actual → siguiente) de la notificación. */
    private fun notifContent(steps: Long, today: Long, route: RouteId, direction: Direction, heightCm: Int): Pair<String, String> {
        val routeDef = Routes.byId(route)
        val waypoints = routeDef.orientedWaypoints(direction)
        val journeyKm = stepsToKm(steps, heightCm)
        val idx = Routes.currentWaypointIndex(waypoints, journeyKm)
        val current = waypoints[idx]
        val next = waypoints.getOrNull(idx + 1)
        val todayKmText = String.format("%.2f", stepsToKm(today, heightCm))
        val title = "Hoy: $today pasos · $todayKmText km"
        val text = if (next != null) {
            val remaining = (next.distanceKm - journeyKm).coerceAtLeast(0.0)
            "${current.name} → ${next.name} (${String.format("%.1f", remaining)} km)"
        } else {
            "¡Llegaste a ${routeDef.destinationName(direction)}!"
        }
        return title to text
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setContentIntent(pi)
            .build()
    }

    private data class EventCtx(
        val journeyStart: Long?,
        val today: Long,
        val route: RouteId,
        val direction: Direction,
        val checkpoint: Int,
    )

    companion object {
        private const val CHANNEL_ID = "step_tracking"
        private const val NOTIF_ID = 42
        private const val EVENT_CHANNEL_ID = "journey_events"
        private const val EVENT_NOTIF_ID = 43
        private const val MIN_KM_FOR_EVENT = 3.0 // solo salta tras andar 3 km ese día
        private const val EVENT_PROBABILITY = 0.6f
        private const val HC_REFRESH_INTERVAL_MS = 15L * 60L * 1000L // refresco Health Connect cada 15 min

        fun start(context: Context) {
            val intent = Intent(context, StepTrackingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
