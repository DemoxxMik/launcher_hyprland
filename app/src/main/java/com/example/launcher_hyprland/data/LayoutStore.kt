package com.example.launcher_hyprland.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.launcher_hyprland.model.TileNode
import com.example.launcher_hyprland.model.WidgetType
import com.example.launcher_hyprland.model.defaultTree
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.io.IOException

// Un seul DataStore pour toute l'app (fichier "launcher_prefs" dans le stockage privé de l'app).
// Le `by preferencesDataStore` garantit qu'il n'est créé qu'une fois, comme un singleton.
private val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")

/** Nombre d'espaces de travail. */
const val WORKSPACE_COUNT = 4

/** L'espace affiché au départ (le 2e, indice 1) : l'accueil. */
const val HOME_WORKSPACE = 1

/**
 * Les 4 arbres par défaut. Les ids de cases sont disjoints d'un espace à l'autre (1000, 3000, 4000 ;
 * l'espace 2 garde les ids historiques) car les notes sont rangées par id de case.
 */
fun defaultWorkspaces(): List<TileNode> = listOf(
    TileNode.Leaf(1001, WidgetType.EMPTY),
    defaultTree(),
    TileNode.Leaf(3001, WidgetType.CALENDAR),
    TileNode.Leaf(4001, WidgetType.EMPTY),
)

/**
 * Lit et écrit les dispositions (un arbre par espace de travail) sur le téléphone, en JSON dans une seule clé.
 * Les deux fonctions sont `suspend` (≈ `async def`) : lire/écrire un fichier ne bloque pas l'affichage.
 *
 * Migration : avant les espaces de travail, un seul arbre était sauvegardé sous la clé « layout ».
 * S'il n'y a pas encore de clé « layouts », cet ancien arbre devient l'espace 2. L'ancienne clé n'est jamais effacée.
 */
class LayoutStore(context: Context) {

    private val dataStore = context.launcherDataStore
    private val layoutsKey = stringPreferencesKey("layouts")
    private val legacyKey = stringPreferencesKey("layout")

    /** Les [WORKSPACE_COUNT] arbres sauvegardés, ou null si rien n'est sauvegardé ou si le JSON est illisible. */
    suspend fun load(): List<TileNode>? {
        return try {
            val prefs = dataStore.data.first()
            decodeLayouts(prefs[layoutsKey]) ?: migrateLegacy(prefs[legacyKey])
        } catch (_: IOException) {
            null // fichier illisible
        }
    }

    private fun decodeLayouts(text: String?): List<TileNode>? {
        if (text == null) return null
        return try {
            Json.decodeFromString<List<TileNode>>(text).takeIf { it.size == WORKSPACE_COUNT }
        } catch (_: IllegalArgumentException) {
            null // JSON corrompu ou d'une ancienne version du modèle
        }
    }

    /** L'ancien arbre unique devient l'espace 2 ; les autres espaces prennent leur valeur par défaut. */
    private fun migrateLegacy(text: String?): List<TileNode>? {
        if (text == null) return null
        val old = try {
            Json.decodeFromString<TileNode>(text)
        } catch (_: IllegalArgumentException) {
            return null
        }
        return defaultWorkspaces().toMutableList().also { it[HOME_WORKSPACE] = old }
    }

    /** Écrit les arbres (remplace la sauvegarde des espaces ; l'ancienne clé reste en place). */
    suspend fun save(trees: List<TileNode>) {
        val text = Json.encodeToString(trees)
        dataStore.edit { prefs -> prefs[layoutsKey] = text }
    }
}
