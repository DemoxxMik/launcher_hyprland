package com.example.launcher_hyprland.data

// Kotlin sans Compose : cette source ne sait pas qui l'affiche, elle produit juste des valeurs.

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.milliseconds

/** L'heure du téléphone, émise à chaque changement de minute. */
object ClockSource {

    /**
     * Flow "froid" (≈ générateur async Python) : rien ne tourne tant que personne ne le collecte,
     * et la boucle s'arrête dès que le collecteur est annulé (le `delay` est interrompu).
     * 1) émet TOUT DE SUITE l'heure actuelle (sinon l'heure serait fausse au retour sur l'accueil) ;
     * 2) dort jusqu'au début de la minute suivante (pas 60 s pile : ça dériverait).
     */
    fun minuteTicks(): Flow<LocalDateTime> = flow {
        while (true) {
            Log.d("Launcher", "clock tick")
            emit(LocalDateTime.now())
            val msIntoMinute = System.currentTimeMillis() % 60_000
            delay((60_000 - msIntoMinute).milliseconds)
        }
    }
}
