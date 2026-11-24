# Étape 4 : tiling dynamique (arbre BSP)

## Le but en une phrase
Remplacer la grille figée de l'étape 3 par un **arbre de découpage** (comme Hyprland) : l'écran est une case qu'on
peut **diviser**, **fermer**, **redimensionner** et **échanger**, et la disposition est **gardée** quand le téléphone
redémarre.

## L'étape est finie quand…
1. Au démarrage, tu retrouves la même disposition qu'à l'étape 3 (horloge en haut, agenda + notes dessous), mais
   **dessinée à partir d'un arbre**.
2. Un **appui long** sur une case ouvre le **mode édition** : les écarts s'élargissent en poignées et une barre de
   commandes apparaît en bas (`split |`, `split —`, `close`, `swap`, `ok`). Le bouton retour ou `ok` en sort.
3. En mode édition, tu peux **diviser** la case active (côte à côte ou dessus/dessous), la **fermer** (sa voisine
   prend la place), **glisser une poignée** pour changer le partage, et **échanger** deux cases.
4. Tu fermes l'app (ou tu redémarres le Pixel) : la disposition **revient telle quelle**.

C'est la plus grosse étape jusqu'ici. Elle est découpée en **5 sous-étapes (4a à 4e)**, chacune testable sur le Pixel
et d'environ 1 h. Compte une à deux semaines à ton rythme, et fais un **commit après chaque sous-étape** : si une
sous-étape casse tout, tu reviens à la précédente.

Comme à l'étape 3 : le Claude d'Android Studio écrit le code, toi tu dois comprendre l'architecture.

---

## L'idée centrale : un arbre binaire (BSP)

BSP = *Binary Space Partitioning*. On part d'un rectangle (l'écran). Soit c'est **une case** (une feuille), soit on le
**coupe en deux** (un nœud), et chaque moitié est à nouveau soit une case, soit coupée en deux, etc.

Ta disposition de l'étape 3, vue comme un arbre :

```
Split(COLUMN, ratio=0.33)            ← l'écran, coupé haut/bas, 1/3 pour le haut
├── Leaf(CLOCK)                      ← en haut : l'horloge
└── Split(ROW, ratio=0.5)            ← en bas, coupé gauche/droite, moitié-moitié
    ├── Leaf(AGENDA)
    └── Leaf(NOTES)
```

En Python, avec des dataclasses, ça s'écrirait :

```python
from dataclasses import dataclass, replace
from enum import Enum

class Widget(Enum): CLOCK = 1; AGENDA = 2; NOTES = 3; EMPTY = 4
class Direction(Enum):
    ROW = 1      # enfants côte à côte  (gauche | droite)  → Compose Row
    COLUMN = 2   # enfants empilés      (haut / bas)       → Compose Column

@dataclass(frozen=True)          # frozen = immuable, on ne modifie jamais, on recrée
class Leaf:
    id: int
    widget: Widget

@dataclass(frozen=True)
class Split:
    id: int
    direction: Direction
    ratio: float                 # part du 1er enfant, entre 0.15 et 0.85
    first: "Node"
    second: "Node"

Node = Leaf | Split              # un nœud est soit une feuille, soit une découpe

def default_tree() -> Node:
    return Split(1, Direction.COLUMN, 0.33,
                 Leaf(2, Widget.CLOCK),
                 Split(3, Direction.ROW, 0.5, Leaf(4, Widget.AGENDA), Leaf(5, Widget.NOTES)))
```

En Kotlin, c'est presque pareil :
- `Node = Leaf | Split` devient une **`sealed interface TileNode`** avec deux `data class` qui l'implémentent.
  « sealed » veut dire « la liste des cas est fermée » : le compilateur sait qu'il n'y a que `Leaf` et `Split`, donc
  un `when` (le `match` de Python) qui oublie un cas ne compile pas.
- `@dataclass(frozen=True)` devient `data class` avec des `val` (lecture seule).
- `dataclasses.replace(leaf, widget=...)` devient `leaf.copy(widget = ...)`.

Chaque nœud a un **`id` unique** (un entier). Il sert à dire « divise la case 4 » ou « change le ratio de la
découpe 3 ». Pour en créer un nouveau : le plus grand id de l'arbre + 1.

### Pourquoi un arbre immuable ? (le point le plus important de l'étape)
Compose redessine quand **la variable d'état reçoit un nouvel objet**. Si tu modifies l'arbre « en place »
(`node.ratio = 0.4`), la variable pointe toujours vers le même objet : Compose ne voit rien et l'écran ne bouge pas.

Donc toutes les opérations sont des **fonctions pures** qui prennent un arbre et **renvoient un nouvel arbre** :

```python
def split_leaf(tree, leaf_id, direction, new_id) -> Node: ...
def close_leaf(tree, leaf_id) -> Node: ...
def resize(tree, split_id, delta) -> Node: ...
def swap_leaves(tree, id_a, id_b) -> Node: ...

tree = split_leaf(tree, 4, Direction.ROW, new_id=6)   # nouvel objet → Compose redessine
```

Elles sont toutes **récursives** : on descend dans l'arbre jusqu'au nœud visé, on le remplace, et en remontant on
recrée les parents avec `copy`. Exemple pour fermer une case :

```python
def close_leaf(node, leaf_id):
    if isinstance(node, Leaf):
        return node                               # rien à faire ici
    # si un de mes enfants directs est la feuille à fermer, je suis remplacé par l'AUTRE enfant
    if isinstance(node.first, Leaf) and node.first.id == leaf_id:
        return node.second
    if isinstance(node.second, Leaf) and node.second.id == leaf_id:
        return node.first
    # sinon je cherche plus bas, et je me recrée avec les enfants éventuellement changés
    return replace(node, first=close_leaf(node.first, leaf_id),
                         second=close_leaf(node.second, leaf_id))
```

Règles du jeu :
- **Diviser** une feuille : elle est remplacée par `Split(direction, 0.5, ancienne_feuille, Leaf(nouvel_id, EMPTY))`.
  La nouvelle case devient la case active.
- **Fermer** : le parent est remplacé par la sœur (voir ci-dessus). Si l'arbre n'est plus qu'**une seule feuille**,
  on ne ferme rien.
- **Redimensionner** : `ratio = clamp(ratio + delta, 0.15, 0.85)`. Le bornage est fait **dans le modèle**, comme ça
  aucune interface ne peut produire une case de taille zéro.
- **Échanger** : les deux feuilles (avec leur id) changent de place. On déplace la feuille entière, pas juste son
  widget : à l'étape 5, quand un widget aura un état (texte de note…), il suivra sa case.

---

## L'architecture

### Les fichiers (vue **Android** du panneau de gauche)

Dans le panneau de gauche, en haut, vérifie que la liste déroulante affiche **Android** (pas « Project »). Ton code est
dans **app › kotlin+java › com.example.launcher_hyprland** (celui **sans** mention entre parenthèses). Sur le disque,
c'est le dossier `app/src/main/java/com/example/launcher_hyprland/`.

```
app › kotlin+java › com.example.launcher_hyprland
├── MainActivity.kt          (inchangé)
├── model/                   NOUVEAU package : du Kotlin pur, aucun import Compose
│   ├── TileNode.kt          4a  les types : WidgetType, Direction, TileNode (Leaf, Split), defaultTree()
│   └── TreeOps.kt           4b+ les fonctions pures : split, close, resize, swap, nextId…
├── data/                    NOUVEAU package
│   └── LayoutStore.kt       4e  lire / écrire l'arbre sur le téléphone (DataStore)
└── ui/
    ├── LauncherStyle.kt     (modifié) + écart en mode édition, couleur des poignées, ratio min/max
    ├── Tile.kt              (modifié 4b) + appui long (onLongClick)
    ├── Widgets.kt           4a  NOUVEAU : MonoText + les widgets factices sortis de HomeScreen + « case vide »
    ├── TilingLayout.kt      4a  NOUVEAU : dessine l'arbre, récursivement
    ├── EditBar.kt           4b  NOUVEAU : la barre de commandes du mode édition
    └── HomeScreen.kt        (réécrit) possède l'état et branche tout ensemble

app › kotlin+java › com.example.launcher_hyprland (test)      ← le paquet marqué « (test) », en vert
└── TreeOpsTest.kt           4b  bonus : tests du modèle, comme pytest, sans téléphone
```

Créer un package : clic droit sur `com.example.launcher_hyprland` › **New › Package**, tape `model` (Android Studio
ajoute le préfixe tout seul). Créer un fichier : clic droit sur le package › **New › Kotlin Class/File** › **File**.
Si le Claude d'Android Studio crée les fichiers lui-même, vérifie juste qu'ils atterrissent au bon endroit.

Pourquoi un package `model` séparé : c'est la **logique** (l'arbre et ses règles), sans rien d'affichage. En Python,
ce serait un module `model.py` qu'on peut tester avec pytest sans ouvrir de fenêtre. `ui` ne fait que **dessiner**
le modèle et **transmettre** les gestes.

### Qui contient qui (arbre des composables)

```
HomeScreen                          ← possède TOUT l'état
└── Column (plein écran, marges systèmes)
    ├── TilingLayout(tree)  weight(1f)
    │   └── NodeView(node)                         ← fonction récursive
    │       ├── si Leaf  → Tile(active, onClick, onLongClick) { WidgetContent(widget) }
    │       └── si Split → Row ou Column (selon direction)
    │                       ├── NodeView(first)    weight(ratio)
    │                       ├── Handle             (l'écart, glissable en mode édition)
    │                       └── NodeView(second)   weight(1 - ratio)
    └── EditBar  (seulement si editMode)
```

Le rendu récursif, en pseudo-Python :

```python
def node_view(node):
    match node:
        case Leaf():
            return Tile(active=(node.id == active_id),
                        on_click=lambda: on_tile_click(node.id),
                        content=widget_content(node.widget))
        case Split(direction=Direction.ROW):
            return Row([node_view(node.first).weight(node.ratio),
                        Handle(node.id),
                        node_view(node.second).weight(1 - node.ratio)])
        case Split(direction=Direction.COLUMN):
            return Column([...pareil...])
```

Les écarts ne viennent plus de `Arrangement.spacedBy` : c'est la **poignée** (`Handle`) entre les deux enfants qui
fait l'écart. Elle mesure 10 dp en temps normal (invisible, juste l'espace) et environ 24 dp en mode édition, avec
un petit trait au milieu pour qu'on voie où attraper. Un doigt a besoin d'au moins ~24 dp pour viser ; c'est pour ça
qu'on ne redimensionne qu'en mode édition. En usage quotidien, rien ne bouge par erreur.

### L'état et ce qui déclenche le redessin

Tout l'état vit dans `HomeScreen`, dans 4 variables `remember { mutableStateOf(...) }` :

| Variable       | Type          | Rôle                                                        |
|----------------|---------------|-------------------------------------------------------------|
| `tree`         | `TileNode`    | la disposition                                              |
| `activeId`     | `Int`         | la feuille sélectionnée (contour accent)                    |
| `editMode`     | `Boolean`     | mode édition ouvert ou non                                  |
| `swapFromId`   | `Int?`        | en attente d'échange : la case de départ (`null` sinon)     |

Le principe Compose s'appelle **« l'état descend, les événements remontent »**. Les composables du dessous ne
modifient rien eux-mêmes : ils reçoivent des valeurs, et des **callbacks** (des `lambda`) qu'ils appellent quand on
les touche. En Python :

```python
class HomeScreen:
    def __init__(self):
        self.tree = default_tree()     # chaque affectation → redessin automatique
        self.active_id = 4
        self.edit_mode = False
        self.swap_from_id = None

    # --- callbacks passés vers le bas ---
    def on_tile_click(self, leaf_id):
        if self.swap_from_id is not None and leaf_id != self.swap_from_id:
            self.tree = swap_leaves(self.tree, self.swap_from_id, leaf_id)
            self.swap_from_id = None
        else:
            self.active_id = leaf_id

    def on_tile_long_click(self, leaf_id):
        self.active_id = leaf_id
        self.edit_mode = True

    def on_resize(self, split_id, delta_fraction):
        self.tree = resize(self.tree, split_id, delta_fraction)

    def on_split(self, direction):
        new_id = next_id(self.tree)
        self.tree = split_leaf(self.tree, self.active_id, direction, new_id)
        self.active_id = new_id
    # on_close, on_swap, on_ok : même idée
```

Ce qui se passe quand tu touches **`split |`** :
1. `EditBar` appelle `onSplit(ROW)` (il ne sait pas ce que ça fait, il transmet).
2. `HomeScreen` calcule un **nouvel** arbre et l'affecte à `tree`, puis change `activeId`.
3. Compose voit que `tree` et `activeId` ont changé → il rappelle `HomeScreen`, qui rappelle `TilingLayout`, qui
   redescend l'arbre. Les branches dont les paramètres n'ont pas changé sont **sautées** (Compose compare les
   `data class` avec `==`).

### Redimensionner : du glissé au ratio
La poignée reçoit `Modifier.draggable` (orientation horizontale dans un `Row`, verticale dans une `Column`).
Le glissé donne un déplacement **en pixels** ; le modèle veut une **fraction**. Il faut donc connaître la taille du
`Row`/`Column` parent en pixels (avec `Modifier.onSizeChanged`, gardée dans une petite variable `remember`) :
`delta_fraction = delta_px / taille_parent_px`, puis on appelle `onResize(split.id, delta_fraction)`.

Piège classique : ne fais **pas** calculer le nouveau ratio dans la poignée (`ratio + delta`). Le lambda de glissé est
créé une fois et garderait un **vieux** ratio en mémoire (comme une closure Python qui capture une valeur périmée) :
la case « saute ». On envoie juste le **delta** vers le haut, et `HomeScreen` l'applique à l'arbre **actuel**.

### Sauvegarder (4e)
- **kotlinx.serialization** transforme l'arbre en texte JSON et inversement. C'est l'équivalent de
  `json.dumps(asdict(tree))` / un chargement pydantic : on ajoute `@Serializable` sur les types du modèle, et ça
  gère tout seul le `sealed interface` (il ajoute un champ `"type"` pour savoir si c'est une `Leaf` ou un `Split`).
- **DataStore (Preferences)** est un petit stockage clé → valeur sur le téléphone, comme un fichier `shelve` ou un
  `config.json`. On y range une seule clé : `"layout"` = le JSON de l'arbre.
- `LayoutStore` a deux fonctions `suspend` (l'équivalent de `async def`, parce que lire/écrire un fichier ne doit pas
  bloquer l'affichage) : `load(): TileNode?` (null si rien de sauvegardé ou JSON illisible) et `save(tree)`.
- Dans `HomeScreen` :
  - **au démarrage** : `LaunchedEffect(Unit) { store.load()?.let { tree = it } }`. `LaunchedEffect` lance une
    coroutine (une tâche `asyncio`) liée à la vie de l'écran.
  - **à chaque changement** : `LaunchedEffect(tree) { delay(500); store.save(tree) }`. Quand `tree` change,
    Compose **annule** la tâche en cours et en relance une. Pendant un glissé (60 changements par seconde), seule la
    dernière va au bout des 500 ms : c'est un **debounce** gratuit, on n'écrit pas 60 fois par seconde.
  - Attention à ne pas sauvegarder l'arbre par défaut **avant** d'avoir chargé : un booléen `loaded` règle ça.

Pourquoi c'est nécessaire même sans redémarrer : Android peut détruire et recréer l'activité d'un launcher
(changement de thème, de langue, mémoire basse). `remember` repart alors de zéro, DataStore non.

---

## Les sous-étapes

### 4a. Le modèle et le rendu récursif (aucune interaction nouvelle)
- `model/TileNode.kt` : `WidgetType` (CLOCK, AGENDA, NOTES, EMPTY), `Direction` (ROW, COLUMN), `TileNode` scellé
  avec `Leaf` et `Split`, et `defaultTree()` qui reproduit la disposition de l'étape 3.
- `ui/Widgets.kt` : déplacer `MonoText`, `ClockWidget`, `AgendaWidget`, `NotesWidget` hors de `HomeScreen`, ajouter
  `EmptyWidget` (« case vide », texte atténué, centré) et `WidgetContent(type)` qui choisit avec un `when`.
- `ui/TilingLayout.kt` : `TilingLayout` + `NodeView` récursif. Écart fixe 10 dp entre enfants (pas encore de poignée
  glissable). Utiliser `key(leaf.id)` autour de chaque feuille pour que Compose suive les cases par identité.
- `ui/HomeScreen.kt` : `tree` + `activeId` en état ; appelle `TilingLayout`.
- **Test sur le Pixel** : l'écran est identique à l'étape 3, le toucher sélectionne toujours une case. Puis modifie
  `defaultTree()` à la main (par exemple divise les notes en deux) : la disposition suit sans toucher à l'UI.

### 4b. Mode édition, diviser, fermer
- `model/TreeOps.kt` : `nextId`, `firstLeafId`, `leafCount`, `splitLeaf`, `closeLeaf`.
- `ui/Tile.kt` : paramètre `onLongClick` ; remplacer `clickable` par `combinedClickable` (qui gère clic + appui long).
- `ui/EditBar.kt` : une rangée de boutons texte monospace (`split |`, `split —`, `close`, `swap`, `ok`), fond de case,
  callbacks seulement. `close` grisé s'il ne reste qu'une case.
- `HomeScreen` : `editMode`, `BackHandler(enabled = editMode)` pour sortir du mode avec le bouton retour.
- Bonus : `TreeOpsTest.kt` dans le paquet `(test)` (diviser puis fermer redonne l'arbre de départ, fermer la dernière
  case ne fait rien). Clic droit sur le fichier › **Run** : ça tourne sur le PC, sans téléphone.
- **Test** : appui long → barre ; `split |` sur l'agenda → une case vide apparaît à sa droite ; `close` sur la case
  vide → l'agenda reprend sa place ; retour → sortie du mode édition.

### 4c. Redimensionner
- `LauncherStyle` : `editGap` (~24 dp), couleur de poignée, `minRatio` 0.15 / `maxRatio` 0.85.
- `TreeOps.resize`.
- `TilingLayout` : la poignée (`Handle`) entre les enfants, glissable seulement en mode édition, taille du parent
  mesurée avec `onSizeChanged`.
- **Test** : en mode édition, glisse la poignée entre horloge et agenda : l'horloge grandit/rétrécit en suivant le
  doigt, sans sauter, et s'arrête aux bornes.

### 4d. Échanger
- `TreeOps.swapLeaves`.
- `HomeScreen` : `swapFromId`. Bouton `swap` → la case active devient « départ » (contour accent clignotant ou en
  pointillé si facile, sinon juste un libellé `swap: touche une case` dans la barre) ; toucher une autre case échange ;
  retoucher `swap` ou la même case annule.
- **Test** : échange horloge et notes ; la case active reste cohérente.

### 4e. Sauvegarder la disposition
- Dépendances : plugin `org.jetbrains.kotlin.plugin.serialization` (même version que Kotlin, 2.2.10 dans
  `gradle/libs.versions.toml`), bibliothèques `kotlinx-serialization-json` et `androidx.datastore:datastore-preferences`
  (versions stables récentes), puis **Sync Now**.
- `@Serializable` sur `TileNode`, `Leaf`, `Split` (les enums sont gérés tout seuls).
- `data/LayoutStore.kt` : `load()` / `save()`, JSON illisible → `null` (on retombe sur l'arbre par défaut).
- `HomeScreen` : chargement au démarrage, sauvegarde avec debounce, booléen `loaded`.
- **Test** : modifie la disposition, puis fais glisser l'app hors des récents, ou redémarre le Pixel : elle revient.

---

## Ce que tu demandes au Claude d'Android Studio

Une **nouvelle conversation** pour l'étape 4. Colle ce guide en premier message avec :
> « Voici le guide de l'étape 4 de mon launcher. Lis d'abord le code existant (package ui). Ne fais rien tant que je
> ne te donne pas une sous-étape. Pour chaque sous-étape : implémente un fichier à la fois, dans l'ordre du guide ;
> après chaque fichier, explique en 3-4 phrases son rôle et comment il s'emboîte avec les autres, en le comparant à
> du Python. Règles : pas de MaterialTheme ni de package ui/theme, tout le style passe par LauncherStyle, le package
> model ne doit importer aucune classe Compose, l'arbre est immuable (data class + copy). »

Puis, une à la fois, en testant sur le Pixel entre chaque :
> « Fais la sous-étape 4a. »

> « Fais la sous-étape 4b, avec le fichier de test bonus. »

> « Fais la sous-étape 4c. »

> « Fais la sous-étape 4d. »

> « Fais la sous-étape 4e. Commence par les modifications Gradle et dis-moi quand faire Sync Now. »

Si la conversation devient très longue ou qu'il commence à se tromper, ouvre-en une nouvelle, recolle le guide et
dis-lui où tu en es.

## Vérifie que tu as compris (sans l'aide de Claude)
1. Dessine l'arbre avant et après un `split |` sur la case agenda de la disposition par défaut. Quels nœuds ont été
   **recréés**, lesquels sont **les mêmes objets** qu'avant ?
2. Pourquoi l'écran ne bougerait pas si `resize` faisait `split.ratio = 0.4` sur l'arbre existant, au lieu de renvoyer
   un nouvel arbre ?
3. (4e) Pendant que tu glisses une poignée, combien de fois la disposition est-elle écrite sur le téléphone, et
   pourquoi ?

Envoie-moi tes réponses si tu veux une correction.

## Pièges possibles
- **Le paquet `(test)`** : il y a trois paquets `com.example.launcher_hyprland` dans la vue Android. Le code va dans
  celui **sans** parenthèses ; le test unitaire dans celui marqué **(test)**, pas **(androidTest)**.
- **`when` qui ne compile pas** (« must be exhaustive ») : il manque un cas (`EMPTY` par exemple). C'est le compilateur
  qui te protège, ajoute le cas.
- **Rien ne bouge après une action** : quelque part on modifie au lieu de recréer, ou on oublie d'affecter le résultat
  (`splitLeaf(tree, …)` sans `tree = `).
- **La case saute pendant le glissé** : le lambda de glissé utilise un vieux ratio (voir « Redimensionner »).
- **`combinedClickable` souligné « experimental »** : il demande `@OptIn(ExperimentalFoundationApi::class)` ;
  Alt+Entrée le propose.
- **Retour quitte le mode édition mais aussi… rien d'autre** : c'est voulu. Le `BackHandler` de `MainActivity` reste
  là pour l'écran normal ; celui de `HomeScreen` (actif seulement en mode édition) passe devant quand il est activé.
- **4e : erreur « Serializer for class … not found »** : le plugin de sérialisation n'est pas appliqué dans
  `app/build.gradle.kts` (il faut à la fois le déclarer et l'appliquer), ou Sync Now n'a pas été fait.
- **4e : après un changement du modèle** (renommer un widget…), l'ancien JSON sauvegardé devient illisible. `load()`
  renvoie `null` et on repart de l'arbre par défaut : c'est voulu. Pour forcer une remise à zéro : Paramètres › Applis
  › launcher_hyprland › Stockage › **Effacer les données**.
- **Import en rouge** : curseur dessus, **Alt+Entrée › Import**.

## 🛟 Si ça plante (à lire AVANT de tester)
- Volet de notifications › roue dentée › Applis › Applis par défaut › Appli d'écran d'accueil › **Pixel Launcher**.
- Ou, téléphone branché, dans l'onglet Terminal d'Android Studio :
  `adb uninstall com.example.launcher_hyprland` → le Pixel revient tout seul à son launcher.
- Crash en boucle au démarrage après 4e (une sauvegarde corrompue ?) : **Effacer les données** de l'app (voir plus
  haut) ou `adb uninstall`.
- Écran noir ou crash : onglet **Logcat**, filtre `AndroidRuntime`, envoie-moi la ligne rouge.
- Revenir à la sous-étape précédente : **Git › Rollback…** (annule ce qui n'est pas encore commité).

## À la fin
1. **Après chaque sous-étape** : Git › Commit… (« 4a : arbre BSP + rendu récursif », etc.) puis Git › Push….
2. Dans `ROADMAP.md` (à la racine du dépôt), coche les lignes de l'étape 4 au fur et à mesure.
3. Dis-moi si tu veux que je relise ton code sur GitHub, par exemple après 4a (c'est la base de tout le reste).
