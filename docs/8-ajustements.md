# Étape 8 : ajustements après les premiers tests sur le Pixel

Document destiné à un assistant (Claude Code dans Android Studio) qui travaille sur ce dépôt. Les étapes sont à faire **dans l'ordre**, une sous-étape à la fois.

## Contexte
- Projet : `launcher_hyprland`, lanceur Android en Jetpack Compose (cases en arbre BSP, 4 espaces de travail, lanceur d'apps façon rofi). Branche : `nuit-theme` (ne pas en changer, ne pas pousser).
- À lire d'abord, en entier : `DESIGN.md` (il prime), `maquette.html` (mesures, ordre, états ; ses couleurs sont simulées), `docs/7a-resume.md` (ce qui existe déjà), puis tout `app/src/main` utile.
- Palette actuelle : `ui/LauncherColors.kt` (`LauncherTheme.colors`), lue dans `android.R.color.system_*`. Style : `ui/LauncherStyle.kt`. Texte : `MonoText` et `TileLabel` dans `ui/widgets/WidgetContent.kt`.

## Règles
- Interdit de modifier : `TileNode`, `TreeOps`, la logique de `TilingLayout`, `WidgetType`, `NotesStore`.
- Pas de couleur en dur (`Color(0x…)`) hors `LauncherStyle.kt` et `LauncherColors.kt`.
- Édition de fichiers avec les outils de l'IDE uniquement (jamais `sed`/redirection shell).
- Après chaque sous-étape : `gradlew :app:compileDebugKotlin` jusqu'au vert, puis `git add -A` et `git commit -m "étape 8.N : …"`. Pas de test sur appareil : l'utilisateur teste.
- Si une sous-étape ne compile pas après plusieurs essais : `git restore .`, la noter, passer à la suivante.
- Fin : `docs/8-resume.md` (fichiers touchés + pourquoi, ce qui a échoué, « à vérifier sur le Pixel »), committé.

## 8.1 Police JetBrains Mono
- Télécharger JetBrains Mono (dépôt officiel JetBrains, licence OFL) : graisses Light (300), Regular (400), Bold (700), format `.ttf`. Télécharger dans un dossier temporaire vide, ne pas exécuter de script depuis ce dossier.
- Les copier dans `app/src/main/res/font/` avec des noms en minuscules/underscore (`jetbrains_mono_light.ttf`, …). Ajouter le fichier de licence (`OFL.txt`) dans `docs/`.
- `LauncherStyle.font` devient une `FontFamily` avec ces trois graisses (`Font(R.font.…, FontWeight.…)`). Aucun widget à modifier : ils passent par `LauncherStyle.font`.

## 8.2 Diagnostic de la palette (couleurs qui ne suivent pas le fond d'écran)
Symptôme constaté : après un changement de fond d'écran, cases noires légèrement transparentes et contours blancs. Trois causes possibles : (1) palette système trop neutre (`neutral1_900` presque noir, `accent1_200` pastel très clair) ; (2) réglage du téléphone qui n'utilise pas les couleurs du fond ; (3) ressources système non rafraîchies sans recréation de l'activité.
- Ajouter un `WidgetType` `PALETTE` (en fin d'enum : on ajoute, on ne renomme jamais), avec sa branche dans `WidgetContent`, son libellé (`label()`) et son entrée dans `WidgetPicker`. (Seule exception à l'interdiction de toucher `WidgetType`.)
- Ce widget affiche, pour chacun des 8 rôles de `LauncherColors` : une pastille de la couleur + son code hex ; et, via `WallpaperManager.getWallpaperColors(FLAG_SYSTEM)`, les couleurs primaire/secondaire/tertiaire du fond (hex).
- Livrer, puis **demander à l'utilisateur** de poser ce widget sur le Pixel et de communiquer ce qu'il affiche avant de passer à 8.3.

## 8.3 Correction des couleurs (selon le diagnostic)
- Cause 1 (palette trop neutre) : calculer la palette dans le code à partir de `WallpaperColors` (couleur dominante), avec la bibliothèque officielle de couleurs Material (`com.google.android.material:material-color-utilities` ou équivalent à jour), en gardant les mêmes 8 rôles et les mêmes tons que `DESIGN.md` (tile ton 10 ≈ ×0,90 d'opacité, border, text, muted, accent ton 80, accent2 tertiaire ton 80, field, onAccent). Paramètre d'intensité de la teinte des fonds. Les `system_*` restent le secours.
- Cause 3 : la recomposition via `refreshKey` existe déjà ; relire les couleurs depuis `WallpaperColors` fournit alors la mise à jour immédiate.
- Cause 2 : documenter, ne rien coder.
- Vérifier la lisibilité : contraste texte/fond d'au moins 4,5:1 pour `text`, ~3:1 pour `muted`. Prévoir 2 ou 3 variantes (doux / normal / contrasté) sélectionnables plus tard (voir 8.6).

## 8.4 Le texte s'adapte à la taille de la case
- Problème : en redimensionnant une case, la taille du texte ne change pas ; horloge et batterie laissent beaucoup de vide.
- Ajouter dans `MonoText` une option d'ajustement automatique (auto-size de Compose, avec taille min et max) et l'appliquer à l'heure, à la date, au pourcentage et à la barre de la batterie. Étiquettes et listes : taille fixe.
- Aucune modification de `TilingLayout`.

## 8.5 Lanceur d'apps : icônes
- Agrandir : icône 20 → ~28 dp, lignes plus hautes (`ui/AppLauncher.kt`).
- Une icône pour **toutes** les apps : si pas de couche monochrome, utiliser la couche avant de l'icône adaptative (ou l'icône entière) comme silhouette teintée en accent (masque alpha), l'initiale seulement en dernier recours (`data/AppsSource.kt`). Rester cohérent avec `DESIGN.md` : pas d'icônes colorées par défaut.

## 8.6 Réglages du launcher
Décision : écran de réglages **dans l'app** (pas d'application tierce : deux apps, communication et permissions pour aucun avantage).
1. Base : un objet `LauncherSettings` (data class) sauvegardé dans le DataStore existant, exposé par un CompositionLocal et modifiable en direct. Il remplace les constantes réglables de `LauncherStyle` (coins, échelle de police, etc.) et `lightTiles`. Sans ceci, changer un réglage ne redessine rien.
2. Écran « réglages » dans le style des cases, ouvert par un bouton dans la barre d'édition (ou appui long sur la barre des espaces). Se référer au panneau « Réglages » de la maquette.
3. Réglages à fournir, dans cet ordre : cases sombres/claires ; coins 4/8/14 dp ; échelle de taille de police ; ombre pleine décalée de 3 dp (rectangle derrière la case, couleur foncée de la palette) ; icônes monochromes/silhouette/sans ; contraste (doux/normal/contrasté) et intensité de la teinte.
4. **Flou : à ne pas faire dans cette étape.** Android ne permet pas de lire le fond d'écran derrière une case sans contournement ; le flou de toute la fenêtre (`setBackgroundBlurRadius`) floute tout l'écran. À évaluer plus tard, séparément.

## Résumé attendu en fin d'étape
`docs/8-resume.md` : fichiers touchés avec une ligne de justification, échecs éventuels, et « à vérifier sur le Pixel » : police, palette après changement de fond (avec le widget de diagnostic), taille du texte au redimensionnement, icônes du lanceur, chaque réglage de l'écran de réglages.
