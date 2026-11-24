package com.example.launcher_hyprland

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.launcher_hyprland.data.SettingsStore
import com.example.launcher_hyprland.ui.HomeScreen
import kotlinx.coroutines.launch
import com.example.launcher_hyprland.ui.LauncherTheme

class MainActivity : ComponentActivity() {
    // Incrémenté quand le fond d'écran change : force LauncherTheme à relire les couleurs système.
    private var paletteVersion by mutableIntStateOf(0)

    private val wallpaperListener = WallpaperManager.OnColorsChangedListener { _: WallpaperColors?, _: Int ->
        paletteVersion++
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WallpaperManager.getInstance(this).addOnColorsChangedListener(wallpaperListener, Handler(Looper.getMainLooper()))
        val settingsStore = SettingsStore(applicationContext)
        setContent {
            BackHandler { /* sur l'accueil, retour ne fait rien */ }
            // null tant que les réglages ne sont pas relus (quelques ms) : on n'affiche rien plutôt que les défauts
            val settings by settingsStore.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = settings
            if (current != null) {
                LauncherTheme(
                    refreshKey = paletteVersion,
                    settings = current,
                    // l'écriture réémet les réglages par le Flow : c'est ce qui redessine l'app
                    onSettingsChange = { new -> lifecycleScope.launch { settingsStore.save(new) } },
                ) {
                    HomeScreen()
                }
            }
        }
    }

    override fun onDestroy() {
        WallpaperManager.getInstance(this).removeOnColorsChangedListener(wallpaperListener)
        super.onDestroy()
    }
}
