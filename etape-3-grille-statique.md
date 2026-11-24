# Étape 3 : grille statique de 3 cases

## Le but en une phrase
Remplacer le texte « Launcher Hyprland » par un vrai écran d'accueil en 3 cases sombres (horloge en haut, agenda et notes
dessous), avec le design validé, pour poser les briques visuelles que le tiling dynamique de l'étape 4 réutilisera.

## L'étape est finie quand…
1. En appuyant sur Accueil, tu vois **3 cases sombres semi-transparentes** sur ton fond d'écran : l'horloge en haut
   (environ 1/3 de la hauteur), l'agenda et les notes côte à côte dessous.
2. Les **écarts sont égaux** partout (entre les cases et avec les bords de l'écran), et rien ne passe sous la barre
   d'état ni sous la barre de gestes.
3. **Toucher une case** met son contour en couleur d'accent ; la case précédente redevient neutre.
4. Tout le texte est en **monospace**, et un aperçu (`@Preview`) de l'écran s'affiche dans Android Studio.

Les contenus sont **factices** (heure figée, faux rendez-vous, fausses notes) : les vrais widgets, c'est l'étape 5.

Ce guide te dit **quoi** coder et **pourquoi**. Le Claude d'Android Studio écrit le code ; toi tu dois comprendre
l'architecture. Voir « Ce que tu demandes au Claude d'Android Studio » plus bas.

---

## Les concepts à connaître (version Godot / Python)

**Composable = une fonction qui décrit ce qu'on voit.** Une fonction marquée `@Composable` décrit l'écran à partir de
l'état actuel. Quand l'état change, Compose la rappelle et met l'écran à jour tout seul. Imagine un `_draw()` relancé
automatiquement à chaque changement de variable.

**`Column` / `Row` = `VBoxContainer` / `HBoxContainer`.** Le paramètre `verticalArrangement` (ou
`horizontalArrangement`) avec `Arrangement.spacedBy(...)` joue le rôle de `separation`. `Modifier.weight(x)` est
l'équivalent de `size_flags_stretch_ratio` avec « Expand » coché : la place est partagée au prorata des poids.

**`Box` = un `Control` simple** qui empile ses enfants. C'est la base d'une case.

**`Modifier` = une pile d'effets appliqués dans l'ordre**, de gauche à droite, comme des transformations empilées.
L'ordre compte : `clip` puis `background` peint le fond dans la forme arrondie ; l'inverse peint un rectangle.
`padding` placé avant `background` fait une marge extérieure, placé après il fait une marge intérieure.

**`remember { mutableIntStateOf(...) }` = une variable observée.** Comme une variable GDScript dont le setter appelle
`queue_redraw()`. `remember` garde la valeur entre deux rappels de la fonction. Pour l'écrire avec `by`, il faut
importer `getValue` et `setValue`.

**Une fonction qui reçoit du contenu.** Un composable peut prendre en paramètre un autre bloc composable (un lambda
`@Composable`), comme un nœud parent qui accepte n'importe quel enfant. C'est ce qui permettra d'avoir une seule
fonction « case » pour tous les widgets.

**`dp` et pas `px`.** Le design dit 10 px, mais sur Android on utilise des `dp`. Le Pixel 9a a environ 2,6 pixels par
dp : `10.dp` donne visuellement l'écart Hyprland voulu. Si c'est trop large, tu baisses la valeur à un seul endroit.

**`systemBarsPadding()`.** Depuis l'étape 2 l'app dessine sous la barre d'état et la barre de gestes
(edge-to-edge). Ce modificateur ajoute juste la marge qu'il faut pour ne pas passer dessous.

---

## Ce qu'il faut coder, dans l'ordre

Organisation visée : un nouveau package `ui` à côté de `ui/theme` (qui existe déjà et ne bouge pas).

```
com.example.launcher_hyprland/
├── MainActivity.kt      (modifié)
└── ui/
    ├── LauncherStyle.kt (nouveau) les valeurs du design
    ├── Tile.kt          (nouveau) une case réutilisable
    └── HomeScreen.kt    (nouveau) la grille + les widgets factices
```

Créer un package / fichier : panneau de gauche, vue **Android**, clic droit sur `com.example.launcher_hyprland` ›
**New › Package**. Puis clic droit sur le package › **New › Kotlin Class/File** › **File** (ou **Object**).

### 1. `LauncherStyle.kt` : toutes les valeurs du design au même endroit
Un `object` Kotlin (l'équivalent d'un autoload Godot) qui contient des constantes :
- l'**écart** entre les cases (10 dp), le **rayon des coins** (autour de 12 dp), l'**épaisseur du contour** (2 dp),
  la **marge intérieure** d'une case (autour de 16 dp) ;
- les **couleurs** : fond de case sombre semi-transparent, contour discret pour une case inactive, couleur d'accent
  pour la case active, texte clair, texte atténué pour les étiquettes ;
- la **police** : `FontFamily.Monospace`.

Pourquoi : quand tu voudras régler le design, tu ne toucheras qu'à ce fichier.

Point utile : une couleur s'écrit `Color(0xAARRGGBB)`. Les deux premiers chiffres hexadécimaux sont l'opacité
(`FF` opaque, `CC` ≈ 80 %, `99` ≈ 60 %). Pour l'accent, un bleu clair type `89B4FA` rappelle les thèmes Hyprland,
mais choisis ce qui te plaît.

### 2. `Tile.kt` : une fonction composable « case »
Une fonction `Tile` qui prend :
- `active` (booléen) : est-ce la case sélectionnée ?
- `onClick` (une fonction sans argument) : quoi faire quand on la touche ;
- `modifier` (avec une valeur par défaut `Modifier`) : pour que l'appelant décide de la taille ;
- `content` : le contenu de la case, un lambda `@Composable` (type `@Composable BoxScope.() -> Unit`).

Dedans, un seul `Box` dont le modifier enchaîne, dans cet ordre : `clip` (forme `RoundedCornerShape`), `background`,
`border` (couleur accent si `active`, sinon contour discret ; même forme), `clickable`, `padding` intérieur.
Le `Box` affiche `content`.

Pourquoi : c'est la brique que le tiling BSP de l'étape 4 placera dans chaque feuille de l'arbre.

Piège : commence le chaînage par le `modifier` reçu en paramètre (pas par `Modifier`), sinon la taille donnée par
l'appelant (`weight`, `fillMaxWidth`) est ignorée.

### 3. `HomeScreen.kt` : la grille
Une fonction `HomeScreen` qui :
1. déclare l'état **case active** avec `remember { mutableIntStateOf(0) }` (0 = horloge, 1 = agenda, 2 = notes ;
   des constantes nommées rendent ça plus lisible) ;
2. pose une `Column` plein écran avec, dans le modifier : `fillMaxSize`, `systemBarsPadding`, puis un `padding` égal
   à l'écart (pour l'écart avec les bords). Espacement vertical entre enfants = l'écart ;
3. dans la colonne :
   - une `Tile` pour l'**horloge**, largeur pleine, `weight(1f)` ;
   - une `Row`, largeur pleine, `weight(2f)`, espacement horizontal = l'écart, qui contient deux `Tile`
     (**agenda** et **notes**), chacune `weight(1f)` et `fillMaxHeight` ;
4. chaque `Tile` reçoit `active = (caseActive == son numéro)` et `onClick = { caseActive = son numéro }`.

Ensuite, dans le même fichier, trois petites fonctions **privées** pour les widgets factices :
- **horloge** : une grande heure figée (vers 64 sp) et la date en dessous en texte atténué, centrées verticalement ;
- **agenda** : une étiquette « agenda » en petit texte atténué, puis 3 lignes « 09:00 cours » etc. ;
- **notes** : une étiquette « notes », puis 3 lignes « - acheter café » etc.

Astuce : écris une petite fonction de texte maison (texte + couleur + taille, police monospace imposée) et utilise-la
partout. Ça évite d'oublier la police quelque part.

Enfin, un **aperçu** : une fonction privée annotée `@Preview` qui appelle `HomeScreen()`. Donne-lui une taille de
téléphone (`widthDp = 412, heightDp = 915`), `showBackground = true` et une `backgroundColor` moyenne (gris-bleu)
pour simuler le fond d'écran, sinon les cases sombres sont invisibles sur fond noir.

### 4. `MainActivity.kt`
Dans `setContent`, garde le `BackHandler` et remplace le `Box` + texte par un appel à `HomeScreen()`. Supprime les
imports devenus inutiles (Android Studio les grise).

---

## Ce que tu demandes au Claude d'Android Studio

Nouvelle conversation, colle ce guide, puis :
> « Implémente l'étape 3 décrite dans ce guide, un fichier à la fois, dans l'ordre : LauncherStyle, Tile, HomeScreen,
> MainActivity. Après chaque fichier, explique-moi en 3-4 phrases son rôle et comment il s'emboîte avec les autres,
> en le comparant à du Python. Ne touche pas aux fichiers ui/theme. »

Pour vérifier que tu as compris l'architecture, avant de tester :
> « Résume l'arbre des composables de HomeScreen et dis-moi ce qui se passe, dans l'ordre, quand je touche la case
> notes. »

Si tu sais répondre à ces deux questions sans lui : *qui contient qui ?* et *qu'est-ce qui déclenche le redessin ?*,
l'étape est comprise.

## Tester

1. **Aperçu d'abord** : ouvre `HomeScreen.kt`, mode **Split** en haut à droite de l'éditeur, puis **Build & Refresh**
   si demandé. Tu peux régler le design ici sans brancher le téléphone.
2. **Sur le Pixel** : ▶ Run, puis Accueil.
3. Vérifie les 4 points de « L'étape est finie quand… ».
4. Joue avec `LauncherStyle` (opacité, écart, rayon, accent) jusqu'à ce que ça te plaise.

## Pièges possibles
- **Import en rouge** (`weight`, `getValue`, `dp`…) : curseur dessus, **Alt+Entrée › Import**.
- **`weight` introuvable** : il n'existe qu'à l'intérieur d'une `Column` ou d'une `Row` (il vient de leur « scope »).
  Il faut donc l'écrire dans le bloc entre les accolades de la `Column`/`Row`, pas ailleurs.
- **Coins pas arrondis ou contour carré** : vérifie l'ordre `clip` → `background` → `border`, et que `border`
  reçoit la même forme.
- **L'heure ne bouge pas** : normal, elle est factice.
- **La case active revient à l'horloge** après un redémarrage de l'app : normal, `remember` ne garde la valeur que
  tant que l'écran vit. On sauvegardera la disposition à l'étape 4.
- **Le reflet gris au toucher** (ripple) est l'effet standard de `clickable`. On le remplacera à l'étape 7.

## 🛟 Si ça plante (à lire AVANT de tester)
- Volet de notifications › roue dentée › Applis › Applis par défaut › Appli d'écran d'accueil › **Pixel Launcher**.
- Ou, téléphone branché, dans l'onglet Terminal d'Android Studio :
  `adb uninstall com.example.launcher_hyprland` → le Pixel revient tout seul à son launcher.
- Écran noir ou crash au lancement : onglet **Logcat** en bas d'Android Studio, filtre `AndroidRuntime`, et
  envoie-moi la ligne rouge.

## À la fin
1. Ajoute `ROADMAP.md` à la racine du projet si ce n'est pas encore fait, et coche l'étape 3.
2. **Commit + push** : menu **Git › Commit…** (par exemple « Étape 3 : grille statique de 3 cases »), puis
   **Git › Push…**.
3. Dis-moi si tu veux que je relise ton code sur GitHub.
