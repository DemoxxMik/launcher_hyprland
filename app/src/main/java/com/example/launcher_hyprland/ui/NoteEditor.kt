package com.example.launcher_hyprland.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import com.example.launcher_hyprland.data.NotesStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/**
 * Éditeur de la note de la case [leafId], en plein écran (à la place de la disposition).
 * Pourquoi plein écran : un clavier qui s'ouvre écraserait toutes les cases dans la moitié haute.
 * Ici, seul cet éditeur reçoit `imePadding()` et se réduit.
 * Sauvegarde : 500 ms après la dernière frappe (debounce, comme la disposition) ET au retour.
 */
@Composable
fun NoteEditor(leafId: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val store = remember { NotesStore(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }

    // null tant que la note n'est pas relue : on n'affiche pas le champ (et surtout on ne sauvegarde pas "").
    var value by remember { mutableStateOf<TextFieldValue?>(null) }

    LaunchedEffect(leafId) {
        val saved = store.load(leafId)
        value = TextFieldValue(saved, selection = TextRange(saved.length)) // curseur à la fin
    }

    // Debounce : chaque frappe change `value` → la tâche précédente est annulée, seule la dernière écrit.
    val text = value?.text
    LaunchedEffect(text) {
        if (text == null) return@LaunchedEffect
        delay(500.milliseconds)
        store.save(leafId, text)
    }

    // Retour : on sauvegarde PUIS on ferme (sinon un texte tapé il y a < 500 ms serait perdu).
    // Déclaré après le BackHandler de HomeScreen → il est prioritaire.
    BackHandler {
        scope.launch {
            text?.let { store.save(leafId, it) }
            onClose()
        }
    }

    val shape = RoundedCornerShape(LauncherStyle.cornerRadius)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(LauncherStyle.gap)
            .clip(shape)
            .background(LauncherStyle.tileBackground, shape)
            .border(LauncherStyle.borderWidth, LauncherStyle.borderActive, shape)
            .padding(LauncherStyle.tilePadding),
    ) {
        val current = value
        if (current != null) {
            BasicTextField(
                value = current,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxSize().focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = LauncherStyle.textPrimary,
                    fontSize = LauncherStyle.textBody,
                    fontFamily = LauncherStyle.font,
                ),
                cursorBrush = SolidColor(LauncherStyle.borderActive),
            )
            // Le champ existe maintenant : on lui donne le focus, ce qui ouvre le clavier.
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        }
    }
}
