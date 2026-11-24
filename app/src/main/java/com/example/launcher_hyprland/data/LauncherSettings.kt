package com.example.launcher_hyprland.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.IOException

/** Icônes du lanceur d'apps. */
enum class IconMode {
    MONOCHROME, // seulement la couche monochrome des icônes (Android 13+) ; sinon l'initiale
    SILHOUETTE, // monochrome, sinon silhouette de la couche avant / de l'icône (le défaut)
    NONE,       // pas d'icône : texte seul
}

/** Écart de luminosité entre le texte (et les contours) et le fond des cases. */
enum class Contrast { SOFT, NORMAL, HIGH }

/**
 * Les réglages du launcher (écran « réglages »). Valeurs par défaut = le design de DESIGN.md.
 * Sauvegardés en JSON dans le DataStore commun : on ajoute des champs (avec une valeur par défaut),
 * on n'en renomme jamais, sinon l'ancienne sauvegarde serait perdue.
 */
@Serializable
data class LauncherSettings(
    val lightTiles: Boolean = false,              // cases claires (false = sombres)
    val cornerRadius: Int = 8,                    // dp : 4, 8 ou 14
    val fontScale: Float = 1f,                    // multiplie toutes les tailles de texte
    val shadow: Boolean = false,                  // ombre pleine décalée de 3 dp derrière les cases
    val icons: IconMode = IconMode.SILHOUETTE,
    val contrast: Contrast = Contrast.NORMAL,
    val tint: Int = 0,                            // intensité de la teinte des fonds : 0 (neutre) à 3
)

/** Lit (en continu) et écrit les réglages, sous la clé « settings » du DataStore partagé avec [LayoutStore]. */
class SettingsStore(context: Context) {

    private val dataStore = context.launcherDataStore
    private val key = stringPreferencesKey("settings")
    // ignoreUnknownKeys : champ d'une version future ; coerceInputValues : valeur d'enum inconnue → défaut
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    /** Émet les réglages au démarrage puis à chaque écriture (c'est ce qui redessine l'app en direct). */
    val settings: Flow<LauncherSettings> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> decode(prefs[key]) }

    private fun decode(text: String?): LauncherSettings {
        if (text == null) return LauncherSettings()
        return try {
            json.decodeFromString<LauncherSettings>(text)
        } catch (_: SerializationException) {
            LauncherSettings()
        } catch (_: IllegalArgumentException) {
            LauncherSettings()
        }
    }

    suspend fun save(settings: LauncherSettings) {
        val text = json.encodeToString(settings)
        dataStore.edit { prefs -> prefs[key] = text }
    }
}
