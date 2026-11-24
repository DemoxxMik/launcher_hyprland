package com.example.launcher_hyprland.data

// Kotlin sans Compose : produit des valeurs, ne sait pas qui les affiche.

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** Un événement. Pour « toute la journée » : start = 00:00 du premier jour, end = 00:00 du lendemain du dernier (heure locale). */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val location: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    val allDay: Boolean,
)

/** Lit les événements du téléphone (calendriers visibles). Sans permission : toujours une liste vide. */
class CalendarSource(context: Context) {
    private val context = context.applicationContext

    /**
     * Flow « froid » : ré-émet la liste à chaque changement du calendrier (≈ un watcher de fichier
     * qui relance la requête). La requête tourne sur Dispatchers.IO, jamais sur le thread principal.
     */
    fun events(from: LocalDateTime, to: LocalDateTime): Flow<List<CalendarEvent>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        try {
            context.contentResolver.registerContentObserver(CalendarContract.CONTENT_URI, true, observer)
        } catch (_: SecurityException) {
            // pas de permission : on émettra juste une liste vide
        }
        trySend(Unit) // première lecture immédiate
        awaitClose { context.contentResolver.unregisterContentObserver(observer) }
    }
        .conflate()
        .map { query(from, to) }
        .flowOn(Dispatchers.IO)

    private fun query(from: LocalDateTime, to: LocalDateTime): List<CalendarEvent> {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) return emptyList()

        val zone = ZoneId.systemDefault()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, from.atZone(zone).toInstant().toEpochMilli())
            ContentUris.appendId(it, to.atZone(zone).toInstant().toEpochMilli())
        }.build()

        val result = mutableListOf<CalendarEvent>()
        try {
            context.contentResolver.query(
                uri,
                PROJECTION,
                "${CalendarContract.Instances.VISIBLE} = 1", // seulement les calendriers cochés
                null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val allDay = c.getInt(5) != 0
                    val beginMs = c.getLong(3)
                    val endMs = c.getLong(4)
                    // « Toute la journée » est stocké en UTC (minuit UTC) : le lire en heure locale
                    // décalerait l'événement d'un jour. On lit la DATE en UTC, puis on la pose en local.
                    val start: LocalDateTime
                    val end: LocalDateTime
                    if (allDay) {
                        start = Instant.ofEpochMilli(beginMs).atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay()
                        end = Instant.ofEpochMilli(endMs).atOffset(ZoneOffset.UTC).toLocalDate().atStartOfDay()
                    } else {
                        start = Instant.ofEpochMilli(beginMs).atZone(zone).toLocalDateTime()
                        end = Instant.ofEpochMilli(endMs).atZone(zone).toLocalDateTime()
                    }
                    // La requête rend ce qui chevauche l'intervalle en millisecondes ; après conversion
                    // des « toute la journée » il faut re-filtrer (sinon celui d'hier peut s'inviter).
                    if (end <= from && start < from) continue
                    if (start >= to) continue
                    result += CalendarEvent(
                        id = c.getLong(0),
                        title = c.getString(1).orEmpty(),
                        location = c.getString(2).orEmpty(),
                        start = start,
                        end = end,
                        allDay = allDay,
                    )
                }
            }
        } catch (_: SecurityException) {
            return emptyList() // permission retirée pendant la requête
        }
        // « toute la journée » d'abord, puis par heure de début, puis par titre
        return result.sortedWith(compareBy<CalendarEvent>({ !it.allDay }, { it.start }, { it.title }))
    }

    private companion object {
        val PROJECTION = arrayOf(
            CalendarContract.Instances.EVENT_ID,       // 0
            CalendarContract.Instances.TITLE,          // 1
            CalendarContract.Instances.EVENT_LOCATION, // 2
            CalendarContract.Instances.BEGIN,          // 3
            CalendarContract.Instances.END,            // 4
            CalendarContract.Instances.ALL_DAY,        // 5
        )
    }
}
