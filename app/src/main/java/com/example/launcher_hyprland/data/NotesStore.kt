package com.example.launcher_hyprland.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Un DataStore à part, dans son PROPRE fichier ("notes"), différent de "launcher_prefs" (LayoutStore).
// Deux DataStore sur le même fichier = crash. Défini ici, au niveau du fichier : créé une seule fois.
private val Context.notesDataStore: DataStore<Preferences> by preferencesDataStore(name = "notes")

/**
 * Le texte des notes, rangé PAR ID DE CASE : {"note_5": "acheter du pain", "note_9": "…"}.
 * Le texte n'est pas dans l'arbre : chaque lettre tapée ne doit pas recréer et sauvegarder toute la disposition.
 * Comme l'échange de cases garde l'id de la feuille, la note suit la case.
 */
class NotesStore(context: Context) {

    private val dataStore = context.notesDataStore

    private fun key(leafId: Int) = stringPreferencesKey("note_$leafId")

    /** Émet le texte de la note à l'abonnement, puis à chaque changement ("" si elle n'existe pas). */
    fun observe(leafId: Int): Flow<String> =
        dataStore.data.map { prefs -> prefs[key(leafId)] ?: "" }.distinctUntilChanged()

    /** Lecture unique (pour remplir l'éditeur à l'ouverture). */
    suspend fun load(leafId: Int): String = observe(leafId).first()

    /** Écrit le texte de la note (remplace l'ancien). */
    suspend fun save(leafId: Int, text: String) {
        dataStore.edit { prefs -> prefs[key(leafId)] = text }
    }
}
