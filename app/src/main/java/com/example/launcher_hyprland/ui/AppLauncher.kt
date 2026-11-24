package com.example.launcher_hyprland.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.example.launcher_hyprland.data.AppEntry
import com.example.launcher_hyprland.data.AppsSource
import com.example.launcher_hyprland.ui.widgets.MonoText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lanceur d'apps façon rofi : un grand panneau (même style de case) par-dessus l'accueil.
 * Champ en haut (« apps » en accent + texte + curseur), liste filtrée dessous. Toucher une ligne = lancer et fermer.
 * Retour ou balayage vers le bas = [onClose].
 */
@Composable
fun AppLauncher(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LauncherTheme.colors
    val context = LocalContext.current
    val preview = LocalInspectionMode.current
    val source = remember { AppsSource(context.applicationContext) }
    var apps by remember { mutableStateOf<List<AppEntry>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        if (!preview) apps = withContext(Dispatchers.IO) { source.load() }
    }
    // Le champ prend le focus dès l'ouverture : le clavier s'ouvre.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    DisposableEffect(Unit) { onDispose { keyboard?.hide() } }

    BackHandler(onBack = onClose)

    val shown = remember(apps, query) { filterApps(apps, query) }
    fun launch(app: AppEntry) {
        source.launch(app)
        onClose()
    }

    val shape = RoundedCornerShape(LauncherStyle.cornerRadius)
    val fieldShape = RoundedCornerShape(LauncherStyle.cornerRadius * 0.7f)
    val closeThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .clip(shape)
            .background(colors.tile, shape)
            .border(LauncherStyle.borderWidth, colors.border, shape)
            // Balayage vers le bas = fermer (le défilement de la liste, lui, garde la priorité)
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = { if (total > closeThresholdPx) onClose() },
                    onVerticalDrag = { _, delta -> total += delta },
                )
            }
            .padding(LauncherStyle.tilePadding),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(LauncherStyle.lineSpacing),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(fieldShape)
                .background(colors.field, fieldShape)
                .padding(horizontal = 9.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MonoText("apps", color = colors.accent, size = LauncherStyle.textBody, maxLines = 1)
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                textStyle = TextStyle(
                    color = colors.text,
                    fontSize = LauncherStyle.textBody,
                    fontFamily = LauncherStyle.font,
                ),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { shown.firstOrNull()?.let { launch(it.app) } }),
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(shown, key = { it.app.component.flattenToString() }) { match ->
                AppRow(match, selected = match === shown.first(), onClick = { launch(match.app) })
            }
        }
    }
}

@Composable
private fun AppRow(match: AppMatch, selected: Boolean, onClick: () -> Unit) {
    val colors = LauncherTheme.colors
    val rowShape = RoundedCornerShape(LauncherStyle.cornerRadius * 0.7f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .then(if (selected) Modifier.background(colors.field, rowShape) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val glyph = match.app.glyph
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (glyph != null) {
                Image(glyph, contentDescription = null, colorFilter = ColorFilter.tint(colors.accent), modifier = Modifier.size(20.dp))
            } else {
                // pas d'icône monochrome : l'initiale, dans la même couleur
                MonoText(
                    match.app.label.take(1).lowercase(),
                    color = colors.accent,
                    size = LauncherStyle.textSmall,
                    maxLines = 1,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        androidx.compose.material3.Text(
            text = highlighted(match.app.label, match.hits, colors.accent),
            color = if (selected) colors.text else colors.muted,
            fontSize = LauncherStyle.textBody,
            fontFamily = LauncherStyle.font,
            maxLines = 1,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        )
    }
}

/** Une app trouvée et les positions (dans son nom) des lettres de la recherche. */
private class AppMatch(val app: AppEntry, val hits: Set<Int>)

private fun highlighted(label: String, hits: Set<Int>, accent: androidx.compose.ui.graphics.Color): AnnotatedString =
    buildAnnotatedString {
        label.forEachIndexed { i, c ->
            if (i in hits) withStyle(SpanStyle(color = accent)) { append(c) } else append(c)
        }
    }

/**
 * Filtre : les lettres de la recherche doivent apparaître dans l'ordre dans le nom (sans tenir compte de la casse).
 * Ordre : nom qui commence par la recherche, puis recherche d'un seul tenant, puis le reste ; alphabétique à égalité.
 */
private fun filterApps(apps: List<AppEntry>, query: String): List<AppMatch> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return apps.map { AppMatch(it, emptySet()) }
    return apps.mapNotNull { app ->
        val name = app.label.lowercase()
        val at = name.indexOf(q)
        if (at >= 0) {
            AppMatch(app, (at until at + q.length).toSet()) to (if (at == 0) 0 else 1)
        } else {
            val hits = mutableSetOf<Int>()
            var from = 0
            for (ch in q) {
                val i = name.indexOf(ch, from)
                if (i < 0) return@mapNotNull null
                hits += i
                from = i + 1
            }
            AppMatch(app, hits) to 2
        }
    }.sortedWith(compareBy({ it.second }, { it.first.app.label.lowercase() })).map { it.first }
}
