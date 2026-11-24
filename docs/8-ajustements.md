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

## 8.3 Correction des couleurs (selon le diagnostic) : ABANDONNÉE
Décision du 2026-11-24 : le bug venait des réglages du téléphone, il est résolu. Le launcher suit maintenant la palette Material You (`system_*`), plus besoin de la calculer dans le code. Ne pas faire cette étape ; l'ancien contenu est gardé ci-dessous pour mémoire.
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

## 8.7 Widget « thème » : choisir la palette Material You en direct
Contexte : sur un même fond d'écran, Material You propose plusieurs palettes (style + couleur de départ) et la meilleure n'est pas toujours celle choisie par défaut. On veut les changer depuis un widget du launcher, sans passer par les paramètres du téléphone.

Ce qui a été **vérifié par adb** (2026-11-24, Pixel 9a, Android 17) :
- Le choix est stocké dans `Settings.Secure` `theme_customization_overlay_packages`, un JSON : `_applied_timestamp`, `android.theme.customization.color_index`, `.color_both`, `.system_palette` (hex de la couleur de départ), `.color_source` (`home_wallpaper`), `.theme_style` (`TONAL_SPOT`, `VIBRANT`, `EXPRESSIVE`, `SPRITZ`, `RAINBOW`, `FRUIT_SALAD`).
- Réécrire ce JSON avec un autre `theme_style` **et un nouveau `_applied_timestamp`** (millisecondes) fait régénérer les overlays par SystemUI en 1 à 2 s. Sans le nouveau timestamp, rien ne se passe. Le style change `accent2`, `accent3` et les neutres, pas `accent1` (qui dépend de la couleur de départ).
- **Pas vérifié** : l'effet de `color_index` et de `system_palette` sur le système. À tester par adb avant de coder cette partie ; si `color_index` ne suffit pas, passer par `system_palette`.

Prérequis (une fois, à demander à l'utilisateur de le faire ou à lancer avec son accord) :
`adb shell pm grant com.example.launcher_hyprland android.permission.WRITE_SECURE_SETTINGS`
Déclarer aussi la permission dans le manifeste. Sans elle, le widget s'affiche mais n'écrit rien : prévoir un message clair (« permission manquante » + la commande).

À faire, dans l'ordre :
1. `data/ThemeSettings.kt` : lire/écrire le réglage avec `org.json.JSONObject` (jamais de JSON assemblé à la main : un guillemet perdu rend le réglage invalide). Garder les clés inconnues telles quelles. À chaque écriture, mettre `_applied_timestamp` à l'heure actuelle en ms. Sauvegarder la valeur d'origine pour pouvoir « revenir au réglage de départ ».
2. Ajouter un `WidgetType` `THEME` (en fin d'enum, on ajoute, on ne renomme jamais) avec sa branche dans `WidgetContent`, son libellé (`label()`) et son entrée dans `WidgetPicker`. (Seconde exception à l'interdiction de toucher `WidgetType`, comme `PALETTE`.)
3. Widget `ui/widgets/ThemeWidget.kt` : une rangée de pastilles pour les 6 styles (la courante en accent) ; les choix de couleur de départ issus du fond (`WallpaperColors` primaire/secondaire/tertiaire, ou `color_index`) en pastilles de leur couleur ; appui = applique tout de suite. Style des cases du reste de l'app, `MonoText` / `TileLabel`, jetons `LauncherTheme.colors`, aucune couleur en dur hors `LauncherStyle`/`LauncherColors`.
4. Bouton « fond d'écran » : ouvre le sélecteur de photos du système (`ActivityResultContracts.PickVisualMedia`), puis `WallpaperManager.setStream(...)` (permission `SET_WALLPAPER`, normale). Le système recalcule alors la palette tout seul.
5. Si la permission manque ou si l'écriture échoue (JSON absent, exception) : ne rien casser, afficher un message, ne pas planter.

Règles propres à cette étape :
- Ne jamais écrire le réglage par le shell avec `settings put` à la main sans sauvegarde : un guillemet perdu l'a déjà invalidé une fois (restauré depuis la sauvegarde).
- Après le widget, vérifier sur l'appareil branché par adb : changer le style depuis le widget, relire le réglage et les overlays (`adb shell cmd overlay dump com.android.systemui:accent`), puis remettre le style d'origine.

## Résumé attendu en fin d'étape
`docs/8-resume.md` : fichiers touchés avec une ligne de justification, échecs éventuels, et « à vérifier sur le Pixel » : police, palette après changement de fond (avec le widget de diagnostic), taille du texte au redimensionnement, icônes du lanceur, chaque réglage de l'écran de réglages, et le widget « thème » (changement de style, choix de couleur de départ, changement de fond d'écran, message si la permission manque).
