package com.example.launcher_hyprland

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BackHandler { /* sur l'accueil, retour ne fait rien */ }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Launcher Hyprland", color = Color.White, fontSize = 24.sp)
            }
        }
    }
}
