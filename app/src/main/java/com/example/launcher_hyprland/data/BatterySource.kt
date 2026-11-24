package com.example.launcher_hyprland.data

// Kotlin sans Compose : transforme un message système (callback) en Flow.

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/** Ce que le widget affiche : le pourcentage, et l'état du câble. */
data class BatteryState(
    val percent: Int,      // 0..100
    val plugged: Boolean,  // câble (ou chargeur sans fil) branché
    val charging: Boolean, // en train de charger (branché ET pas encore plein)
)

/** La batterie du téléphone, émise à chaque changement de pourcentage ou de câble. */
class BatterySource(private val context: Context) {

    /**
     * callbackFlow ≈ ton `battery_updates` Python avec sa asyncio.Queue :
     * - le receiver pousse chaque message dans la file (trySend) ;
     * - awaitClose ≈ le `finally` : il s'exécute quand plus personne ne collecte
     *   (accueil caché → collectAsStateWithLifecycle annule) → on se DÉSABONNE.
     */
    fun updates(): Flow<BatteryState> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                readState(intent)?.let { trySend(it) }
            }
        }
        Log.d("Launcher", "battery register")
        // RECEIVER_NOT_EXPORTED : obligatoire depuis Android 14 ; les messages du système arrivent quand même.
        // Le message est « collant » (sticky) : la valeur actuelle est renvoyée tout de suite.
        val sticky = ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        sticky?.let { intent -> readState(intent)?.let { trySend(it) } }

        awaitClose {
            Log.d("Launcher", "battery unregister")
            context.unregisterReceiver(receiver)
        }
    }
        // Pendant la charge, le système renvoie le message souvent (tension, température…) :
        // on ne laisse passer que si le pourcentage ou l'état du câble a vraiment changé.
        .distinctUntilChanged()

    /** Lit le message système. null s'il est incomplet. */
    private fun readState(intent: Intent): BatteryState? {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        return BatteryState(
            percent = level * 100 / scale,
            plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0,
            charging = status == BatteryManager.BATTERY_STATUS_CHARGING,
        )
    }
}
