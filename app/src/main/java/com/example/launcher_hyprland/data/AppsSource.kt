package com.example.launcher_hyprland.data

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap

/**
 * Une app lançable. [glyph] = son icône monochrome (un masque : on la teinte à l'affichage),
 * ou null si l'app n'en fournit pas (on affiche alors l'initiale du nom).
 */
data class AppEntry(
    val label: String,
    val component: ComponentName,
    val glyph: ImageBitmap?,
)

private const val GLYPH_PX = 96

/** Liste et lance les apps du téléphone (celles qui ont une icône dans le tiroir d'apps). */
class AppsSource(private val context: Context) {

    /** Bloquant (lit les icônes) : à appeler hors du thread principal. Trié par nom. */
    fun load(): List<AppEntry> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(0))
            .map { info ->
                val activity = info.activityInfo
                AppEntry(
                    label = info.loadLabel(pm).toString(),
                    component = ComponentName(activity.packageName, activity.name),
                    glyph = monochromeGlyph(info.loadIcon(pm)),
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /** Seules les icônes adaptatives ont une couche monochrome (Android 13+) ; sinon null. */
    private fun monochromeGlyph(icon: android.graphics.drawable.Drawable?): ImageBitmap? {
        val mono = (icon as? AdaptiveIconDrawable)?.monochrome ?: return null
        return try {
            mono.mutate().toBitmap(GLYPH_PX, GLYPH_PX).asImageBitmap()
        } catch (_: RuntimeException) {
            null
        }
    }

    fun launch(app: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // app désinstallée entre-temps : rien à lancer
        } catch (_: SecurityException) {
            // activité non exportée : idem
        }
    }
}
