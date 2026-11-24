package com.example.launcher_hyprland.data

import android.Manifest
import android.app.WallpaperManager
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/** Ce que dit le réglage Material You du téléphone (null : clé absente du JSON). */
data class ThemeState(
    val style: String?, // « TONAL_SPOT », « VIBRANT »…
    val seed: Int?,     // couleur de départ (system_palette), opaque (0xFFRRGGBB)
)

/** Issue d'une écriture : rien ne lève d'exception vers l'interface. */
sealed interface ThemeResult {
    data object Ok : ThemeResult
    data object NoPermission : ThemeResult
    data class Failed(val reason: String) : ThemeResult
}

/**
 * Lit et écrit le choix de palette Material You (`Settings.Secure` `theme_customization_overlay_packages`),
 * comme le fait l'app « Fond d'écran et style ». Vérifié par adb (Pixel 9a, Android 17) : SystemUI
 * régénère les overlays dès que le JSON change ET que `_applied_timestamp` est nouveau ; `theme_style`
 * change accent2/accent3/neutres, `system_palette` (hex sans #) change la couleur de départ (accent1).
 *
 * Règles : le JSON est toujours manipulé avec [JSONObject] (jamais assemblé à la main), les clés
 * inconnues sont gardées, et la valeur d'origine est sauvegardée (DataStore) avant la première écriture.
 * Écrire demande `WRITE_SECURE_SETTINGS`, accordée une fois par adb (voir [grantCommand]).
 */
class ThemeSettings(private val context: Context) {

    private val resolver = context.contentResolver
    private val dataStore = context.launcherDataStore
    private val originalKey = stringPreferencesKey("theme_original")

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** La commande à lancer depuis l'ordinateur pour accorder la permission. */
    val grantCommand: String
        get() = "adb shell pm grant ${context.packageName} ${Manifest.permission.WRITE_SECURE_SETTINGS}"

    /** L'état actuel ; null si le réglage est absent ou illisible. */
    fun read(): ThemeState? {
        val json = readJson() ?: return null
        return ThemeState(
            style = json.optString(KEY_STYLE).ifEmpty { null },
            seed = json.optString(KEY_PALETTE).ifEmpty { null }?.let(::parseHex),
        )
    }

    /** L'état au démarrage puis à chaque modification du réglage (par nous, ou par les paramètres). */
    val changes: Flow<ThemeState?> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(read())
            }
        }
        resolver.registerContentObserver(Settings.Secure.getUriFor(SETTING), false, observer)
        trySend(read())
        awaitClose { resolver.unregisterContentObserver(observer) }
    }

    /** Vrai si une valeur d'origine est sauvegardée (« revenir au départ » possible). */
    val hasOriginal: Flow<Boolean> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { prefs -> prefs[originalKey] != null }

    suspend fun setStyle(style: String): ThemeResult = write { it.put(KEY_STYLE, style) }

    /** Réécrit la valeur sauvegardée avant la première modification (avec un nouveau timestamp), puis l'oublie. */
    suspend fun restoreOriginal(): ThemeResult = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext ThemeResult.NoPermission
        val saved = savedOriginal() ?: return@withContext ThemeResult.Failed("aucune sauvegarde")
        val json = try {
            JSONObject(saved)
        } catch (_: JSONException) {
            return@withContext ThemeResult.Failed("sauvegarde illisible")
        }
        json.put(KEY_TIMESTAMP, System.currentTimeMillis())
        val result = put(json.toString())
        if (result == ThemeResult.Ok) {
            try {
                dataStore.edit { it.remove(originalKey) }
            } catch (_: IOException) {
                // la sauvegarde reste : sans conséquence
            }
        }
        result
    }

    /** Pose l'image comme fond d'écran (accueil et verrouillage) ; le système recalcule la palette seul. */
    suspend fun setWallpaper(uri: Uri): ThemeResult = withContext(Dispatchers.IO) {
        try {
            val stream = resolver.openInputStream(uri) ?: return@withContext ThemeResult.Failed("image illisible")
            stream.use { WallpaperManager.getInstance(context).setStream(it) }
            ThemeResult.Ok
        } catch (e: IOException) {
            ThemeResult.Failed("fond non appliqué : ${e.message ?: "erreur"}")
        } catch (e: SecurityException) {
            ThemeResult.Failed("fond refusé : ${e.message ?: "permission"}")
        }
    }

    private suspend fun write(change: (JSONObject) -> Unit): ThemeResult = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext ThemeResult.NoPermission
        val raw = Settings.Secure.getString(resolver, SETTING)
        if (raw.isNullOrBlank()) {
            return@withContext ThemeResult.Failed("réglage absent : choisir une fois un style dans les paramètres")
        }
        val json = try {
            JSONObject(raw)
        } catch (_: JSONException) {
            return@withContext ThemeResult.Failed("réglage illisible, rien n'est écrit")
        }
        if (savedOriginal() == null) {
            try {
                dataStore.edit { it[originalKey] = raw }
            } catch (_: IOException) {
                return@withContext ThemeResult.Failed("sauvegarde impossible, rien n'est écrit")
            }
        }
        change(json)
        json.put(KEY_TIMESTAMP, System.currentTimeMillis()) // sans nouveau timestamp, SystemUI ignore le changement
        put(json.toString())
    }

    private fun put(text: String): ThemeResult = try {
        if (Settings.Secure.putString(resolver, SETTING, text)) ThemeResult.Ok
        else ThemeResult.Failed("écriture refusée")
    } catch (_: SecurityException) {
        ThemeResult.NoPermission
    } catch (e: IllegalArgumentException) {
        ThemeResult.Failed("écriture impossible : ${e.message ?: "erreur"}")
    }

    private suspend fun savedOriginal(): String? = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .first()[originalKey]

    private fun readJson(): JSONObject? {
        val raw = Settings.Secure.getString(resolver, SETTING)
        if (raw.isNullOrBlank()) return null
        return try {
            JSONObject(raw)
        } catch (_: JSONException) {
            null
        }
    }

    private fun parseHex(text: String): Int? =
        text.removePrefix("#").toLongOrNull(16)?.let { (it.toInt() and 0xFFFFFF) or 0xFF000000.toInt() }

    companion object {
        const val SETTING = "theme_customization_overlay_packages"
        private const val PREFIX = "android.theme.customization."
        private const val KEY_STYLE = PREFIX + "theme_style"
        private const val KEY_PALETTE = PREFIX + "system_palette"
        private const val KEY_TIMESTAMP = "_applied_timestamp"

        /** Les styles proposés par Material You, dans l'ordre de l'app des paramètres. */
        val STYLES = listOf("TONAL_SPOT", "VIBRANT", "EXPRESSIVE", "SPRITZ", "RAINBOW", "FRUIT_SALAD")
    }
}
