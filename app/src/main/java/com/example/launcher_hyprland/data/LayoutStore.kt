package com.example.launcher_hyprland.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.launcher_hyprland.model.TileNode
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException

// Un seul DataStore pour toute l'app (fichier "launcher_prefs" dans le stockage privé de l'app).
// Le `by preferencesDataStore` garantit qu'il n'est créé qu'une fois, comme un singleton.
private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

/**
 * Lit et écrit la disposition (l'arbre) sur le téléphone, sous forme de JSON dans une seule clé.
 * Les deux fonctions sont `suspend` (≈ `async def`) : lire/écrire un fichier ne bloque pas l'affichage.
 */
class LayoutStore(context: Context) {

    private val dataStore = context.launcherDataStore
    private val layoutKey = stringPreferencesKey("layout")

    /** L'arbre sauvegardé, ou null si rien n'est sauvegardé ou si le JSON est illisible. */
    suspend fun load(): TileNode? {
        return try {
            val text = dataStore.data.first()[layoutKey] ?: return null
            Json.decodeFromString<TileNode>(text)
        } catch (_: IllegalArgumentException) {
            // JSON d'une ancienne version du modèle, corrompu, enum inconnue… (SerializationException
            // en est un cas particulier) → on repartira de l'arbre par défaut
            null
        } catch (_: IOException) {
            null // fichier illisible
        }
    }

    /** Écrit l'arbre (remplace l'ancienne sauvegarde). */
    suspend fun save(tree: TileNode) {
        val text = Json.encodeToString(tree)
        dataStore.edit { prefs -> prefs[layoutKey] = text }
    }
}
