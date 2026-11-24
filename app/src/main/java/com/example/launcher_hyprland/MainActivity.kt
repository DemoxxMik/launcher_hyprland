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
import com.example.launcher_hyprland.ui.HomeScreen
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
        setContent {
            BackHandler { /* sur l'accueil, retour ne fait rien */ }
            LauncherTheme(refreshKey = paletteVersion) {
                HomeScreen()
            }
        }
    }

    override fun onDestroy() {
        WallpaperManager.getInstance(this).removeOnColorsChangedListener(wallpaperListener)
        super.onDestroy()
    }
}
