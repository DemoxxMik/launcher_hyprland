# Résumé de la nuit (étapes 7a : thème, espaces de travail, lanceur d'apps)

Branche `nuit-theme`, rien de poussé. Tout compile (`:app:compileDebugKotlin`), rien n'a été testé sur appareil.

## Fichiers touchés

**Phase A : palette et cases** (commits « nuit 1 » à « nuit 3 »)
- `ui/LauncherColors.kt` (nouveau) : `LauncherColors`, `LocalLauncherColors`, `LauncherTheme` (objet + composable). Lit `android.R.color.system_*` ; sombre par défaut, clair si `LauncherStyle.lightTiles`.
- `MainActivity.kt` : enveloppe `HomeScreen` dans `LauncherTheme` ; `WallpaperManager.OnColorsChangedListener` (retiré dans `onDestroy`) incrémente `refreshKey` pour forcer la relecture des couleurs.
- `ui/LauncherStyle.kt` : gap 10, coins 8, contour 1, marge 12 dp ; textes 44 sp Light / 13 / 11 sp ; anciennes couleurs retirées sauf les `fallback*` (aperçu Android Studio) ; ajout de `lightTiles` et `weightHuge`.
- `ui/Tile.kt` : couleurs par jetons ; contour accent si active, accent2 si départ d'échange ; fournit `LocalTileActive`.
- `ui/widgets/WidgetContent.kt` : `MonoText` prend le jeton `text` ; nouveau `TileLabel` (pastille accent/onAccent si case active, sinon `muted`, minuscules).
- `ClockWidget` (étiquette « horloge », date en toutes lettres, heure Light), `BatteryWidget` (accent2 en charge), `EmptyWidget`, `NotesWidget`, `AgendaWidget` : jetons + `TileLabel`.
- `ui/widgets/agenda/*` : accent = « maintenant », prochain événement, aujourd'hui, pilule active, jour choisi en contour `border` ; accent2 = points des jours ; passé = `muted`/`border`, hors mois = `border` ; pilules inactives sur `field`.
- `ui/EditBar.kt`, `ui/WidgetPicker.kt`, `ui/NoteEditor.kt`, `ui/TilingLayout.kt` (couleur de poignée seulement) : jetons.

**Phase B : espaces de travail** (commit « nuit 5-6 »)
- `data/LayoutStore.kt` : 4 arbres sous la clé `layouts`. Migration : si seule l'ancienne clé `layout` existe, son arbre devient l'espace 2 (l'ancienne clé n'est jamais effacée). Défauts : espace 1 et 4 = feuille vide, espace 3 = feuille calendrier. Ids de cases disjoints (1001, 3001, 4001) car les notes sont rangées par id de case.
- `ui/HomeScreen.kt` : `HorizontalPager` de 4 pages (page 2 par défaut), une case active par espace, balayage coupé en mode édition ; les commandes d'édition agissent sur la page affichée.
- `ui/WorkspaceBar.kt` (nouveau) : barre fine pleine largeur, pastilles 1 2 3 4 (active = accent, plus large). Inactive en mode édition.

**Phase C : lanceur d'apps** (commit « nuit 7-8 »)
- `data/AppsSource.kt` (nouveau) : apps via `PackageManager` (MAIN/LAUNCHER), icône monochrome d'`AdaptiveIconDrawable` (sinon initiale), lancement par intent explicite.
- `AndroidManifest.xml` : bloc `<queries>` MAIN/LAUNCHER.
- `ui/AppLauncher.kt` (nouveau) : panneau en style de case, champ « apps » + curseur, liste filtrée (lettres trouvées en accent, icône teintée accent, première ligne sélectionnée sur `field`), toucher = lancer et fermer, action « Go » du clavier = lancer la première ; retour ou balayage bas = fermer.
- `ui/HomeScreen.kt` : balayage haut sur le pager (hors édition) ouvre le panneau.

## Ce qui a échoué / écarts
- Rien n'a échoué de façon durable (une erreur de compilation sur `addOnColorsChangedListener(…, null)` corrigée aussitôt).
- `DESIGN.md` et `maquette.html` sont à la racine du dépôt, pas dans `docs/` : je les ai lus là (ils sont aussi dans le commit « nuit 1 », car non suivis).
- Pas de JetBrains Mono dans `res/font/` : police monospace système (rien téléchargé).
- `MainActivity` : son `BackHandler` vide n'a pas eu besoin d'être modifié : ceux de `HomeScreen` / `AppLauncher` sont composés plus tard, donc prioritaires.
- Je n'ai pas ajouté de météo (pas de donnée), ni de musique / apps favorites (hors périmètre).
- La relecture des couleurs après changement de fond dépend de ce qu'Android rafraîchit dans les ressources sans recréation d'activité : le listener force la recomposition, mais pas la mise à jour des ressources système.
- Pas de tests unitaires relancés (aucun changement dans `TileNode` / `TreeOps`).

## À vérifier sur le Pixel
- Réaccord des couleurs après un changement de fond d'écran (sans relancer l'app).
- Lisibilité (texte atténué, jours hors mois, événements passés) en sombre.
- Case active : contour accent + étiquette en pastille ; autres étiquettes atténuées.
- Mode édition : poignées visibles (couleur du texte), swap en accent2, balayage entre espaces bloqué, barre des espaces inactive.
- Notes : ouvrir/écrire/fermer, et que chaque espace garde ses notes.
- Agenda et calendrier : « maintenant », « ensuite », pilules semaine/mois, point des jours.
- Disposition d'avant conservée dans l'espace 2 après mise à jour (horloge / agenda / notes et leurs notes).
- Bascule d'espaces : balayage et toucher de la barre ; l'espace 3 montre le calendrier.
- Lanceur d'apps : balayage haut ouvre le panneau, clavier ouvert, filtre, lancement, retour / balayage bas ferment ; icônes monochromes ou initiale ; conflit éventuel du balayage bas avec le défilement de la liste.
