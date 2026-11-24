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
 * Une app lançable. [glyph] = sa silhouette (un masque : on la teinte à l'affichage),
 * ou null si aucune icône n'a pu être lue (on affiche alors l'initiale du nom).
 */
data class AppEntry(
    val label: String,
    val component: ComponentName,
    val glyph: ImageBitmap?,
    /** Faux : [glyph] est l'icône en couleurs d'origine (aucune silhouette exploitable), à afficher sans teinte. */
    val tinted: Boolean = true,
)

private const val GLYPH_PX = 96

/** Au-delà de cette part de pixels opaques, une silhouette n'est qu'un aplat : la teinter donnerait un carré plein. */
private const val SOLID_RATIO = 0.85f

/** Liste et lance les apps du téléphone (celles qui ont une icône dans le tiroir d'apps). */
class AppsSource(private val context: Context) {

    /** Bloquant (lit les icônes) : à appeler hors du thread principal. Trié par nom. */
    fun load(): List<AppEntry> {
        val pm = context.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(query, PackageManager.ResolveInfoFlags.of(0))
            .map { info ->
                val activity = info.activityInfo
                val (glyph, tinted) = glyphOf(info.loadIcon(pm))
                AppEntry(
                    label = info.loadLabel(pm).toString(),
                    component = ComponentName(activity.packageName, activity.name),
                    glyph = glyph,
                    tinted = tinted,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Silhouette de l'icône, teintée ensuite à l'affichage (seul le canal alpha compte). Par ordre de préférence :
     * couche monochrome (Android 13+), couche avant de l'icône adaptative, icône entière ; on prend la première
     * qui n'est pas un aplat. Si toutes en sont un : l'icône entière en couleurs, non teintée (tinted = false).
     * Retourne (null, true) en dernier recours (l'initiale sera affichée).
     */
    private fun glyphOf(icon: android.graphics.drawable.Drawable?): Pair<ImageBitmap?, Boolean> {
        if (icon == null) return null to true
        val adaptive = icon as? AdaptiveIconDrawable
        val candidates = listOfNotNull(adaptive?.monochrome, adaptive?.foreground, icon)
        var fallback: ImageBitmap? = null
        for (drawable in candidates) {
            try {
                val bitmap = drawable.mutate().toBitmap(GLYPH_PX, GLYPH_PX)
                if (opaqueRatio(bitmap) <= SOLID_RATIO) return bitmap.asImageBitmap() to true
                if (fallback == null) fallback = bitmap.asImageBitmap()
            } catch (_: RuntimeException) {
                // essayer la couche suivante
            }
        }
        // tout est plein : l'icône entière en couleurs (plutôt qu'un carré teinté)
        val colored = try {
            icon.mutate().toBitmap(GLYPH_PX, GLYPH_PX).asImageBitmap()
        } catch (_: RuntimeException) {
            null
        }
        return if (colored != null) colored to false else fallback to true
    }

    /** Part (0..1) des pixels non transparents. */
    private fun opaqueRatio(bitmap: android.graphics.Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val opaque = pixels.count { (it ushr 24) > 32 }
        return opaque.toFloat() / pixels.size
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
