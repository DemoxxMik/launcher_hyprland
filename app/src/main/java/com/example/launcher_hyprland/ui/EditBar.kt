package com.example.launcher_hyprland.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * La barre de commandes du mode édition. Elle ne sait pas ce que font les boutons :
 * elle appelle juste les callbacks reçus ("les événements remontent").
 * [swapPending] = un échange attend sa 2e case : on affiche une consigne et `swap` en couleur.
 */
@Composable
fun EditBar(
    canClose: Boolean,
    canSwap: Boolean,
    swapPending: Boolean,
    onSplitRow: () -> Unit,
    onSplitColumn: () -> Unit,
    onClose: () -> Unit,
    onSwap: () -> Unit,
    onOk: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(LauncherStyle.cornerRadius)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(LauncherStyle.tileBackground, shape)
            .border(LauncherStyle.borderWidth, LauncherStyle.borderInactive, shape)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (swapPending) {
            Box(Modifier.padding(top = 8.dp)) {
                MonoText("swap : touche une case", color = LauncherStyle.borderSwap, size = 13.sp)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarButton("split |", onClick = onSplitRow)
            BarButton("split —", onClick = onSplitColumn)
            BarButton("close", enabled = canClose, onClick = onClose)
            BarButton("swap", enabled = canSwap, highlighted = swapPending, onClick = onSwap)
            BarButton("ok", onClick = onOk)
        }
    }
}

/** Un bouton texte monospace. Grisé et inactif si [enabled] est faux, en couleur si [highlighted]. */
@Composable
private fun BarButton(
    label: String,
    enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        MonoText(
            text = label,
            color = when {
                !enabled -> LauncherStyle.textMuted
                highlighted -> LauncherStyle.borderSwap
                else -> LauncherStyle.textPrimary
            },
        )
    }
}
