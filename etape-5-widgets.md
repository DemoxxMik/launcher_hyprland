# Étape 5 : premiers vrais widgets

## Le but en une phrase
Remplacer les widgets factices par des **widgets vivants** (horloge, batterie, notes, puis agenda), et pouvoir
**choisir quel widget** affiche chaque case, sans vider la batterie du téléphone.

## L'étape est finie quand…
1. En mode édition, un bouton **`widget`** ouvre une petite liste ; toucher un nom change le widget de la case active.
   Le choix est **gardé** après redémarrage (il fait partie de l'arbre).
2. L'**horloge** affiche la vraie heure et la date, et change **pile à la minute**. Dans Logcat, la ligne
   `clock tick` n'apparaît **que quand l'écran d'accueil est visible** (rien quand une autre app est ouverte ou
   l'écran éteint).
3. La **batterie** affiche le pourcentage et `charge` / `secteur` ; elle change **tout de suite** quand tu branches
   ou débranches le câble.
4. Les **notes** : toucher une case notes **déjà active** ouvre un éditeur plein écran avec le clavier ; retour →
   le texte apparaît dans la case, il survit au redémarrage et **suit la case** quand tu l'échanges.
5. (5d, optionnel cette semaine) L'**agenda** demande l'accès au calendrier une fois, puis montre les prochains
   événements ; si tu refuses, la case l'explique au lieu de planter.

4 sous-étapes (5a à 5d) d'environ 1 h chacune, testables sur le Pixel. Un **commit après chaque sous-étape**.

> **Avant de commencer : ton dépôt GitHub est incomplet.** Le dernier push (« Etape 4 fonctionel ») ne contient que
> `ui/HomeScreen.kt`, `ui/Tile.kt`, `ui/LauncherStyle.kt` et `MainActivity.kt`. Les dossiers `model/` et `data/`,
> et `ui/TilingLayout.kt`, `ui/Widgets.kt`, `ui/EditBar.kt` sont restés sur ton PC : ils n'ont jamais été **ajoutés
> à Git** (Android Studio les affiche en **rouge** dans l'arborescence). Pour les envoyer : **Git › Commit…**, dans
> la liste coche le groupe **Unversioned Files** (ou clic droit dessus › Add to VCS), message « Étape 4 : fichiers
> oubliés », **Commit and Push**. Coche aussi les étapes 2, 3 et 4 dans `ROADMAP.md`. Sans ça, ton code n'est
> sauvegardé nulle part ailleurs que sur ton PC.

---

## Ce qu'on fait et ce qu'on reporte

| Widget                 | Données                              | Permission                         | Quand         |
|------------------------|--------------------------------------|------------------------------------|---------------|
| Horloge + date         | l'heure du téléphone                 | aucune                             | 5a            |
| Batterie               | message système « batterie changée » | aucune                             | 5b            |
| Notes                  | ton texte, sauvegardé sur le Pixel   | aucune                             | 5c            |
| Agenda                 | le calendrier du téléphone           | `READ_CALENDAR` (on la demande)    | 5d            |
| Apps favorites         | la liste des apps                    | —                                  | **étape 6** (il faut d'abord savoir lister et lancer les apps) |
| Météo                  | internet + localisation              | réseau + position                  | plus tard     |
| Musique                | la session média de l'app qui joue   | accès aux notifications (lourde)   | plus tard     |

Météo et musique demandent chacune une étape à elles seules (clé d'API, permissions sensibles, service en arrière-plan
pour la musique). On les garde pour après l'étape 6.

**Le design n'est pas figé ici.** Les couleurs pastel tirées du fond d'écran (Material You) se décident dans le fil
« Définir le design du launcher ». Règle pour toute l'étape : **un widget ne contient aucune couleur ni taille en
dur**, il lit tout dans `LauncherStyle`. Le jour où `LauncherStyle` devient dynamique, tous les widgets suivent sans
être retouchés. Pour l'instant : texte d'abord, pas d'icônes (elles viendront avec la police d'icônes).

---

## L'idée centrale : un widget = une source de données + un affichage

Jusqu'ici, un widget était un texte figé. Un vrai widget a deux moitiés :

1. **La source** (package `data/`) : elle sait **d'où viennent les données** et prévient quand elles changent.
   Du Kotlin sans Compose, comme le package `model`.
2. **L'affichage** (package `ui/widgets/`) : un composable qui **s'abonne** à la source et dessine la dernière valeur.

En Kotlin, une source qui « prévient quand ça change » s'appelle un **`Flow`**. C'est l'équivalent d'un **générateur
asynchrone** Python (`async def ... yield`) :

```python
# data/clock_source.py : la SOURCE
async def minute_ticks():
    while True:
        now = datetime.now()
        print("clock tick")                     # Log.d dans le vrai code : sert au test n°2
        yield now
        await asyncio.sleep(60 - now.second)    # dors jusqu'à la minute suivante, pas 60 s pile

# ui/widgets/clock_widget.py : l'AFFICHAGE
def clock_widget():
    now = collect_while_visible(minute_ticks(), initial=datetime.now())
    return Column([MonoText(now.strftime("%H:%M"), size=Style.text_huge),
                   MonoText(now.strftime("%a %d %b"), color=Style.text_muted)])
```

Le cœur de l'étape, c'est ce `collect_while_visible`. En Compose il s'appelle
**`collectAsStateWithLifecycle()`** :

```python
def collect_while_visible(flow, initial):
    state = State(initial)            # comme mutableStateOf : chaque nouvelle valeur → redessin
    async def run():
        while True:
            await lifecycle.wait_until_started()        # l'écran d'accueil est visible
            task = asyncio.create_task(copy_into(flow, state))
            await lifecycle.wait_until_stopped()        # une autre app passe devant, ou écran éteint
            task.cancel()                               # → la source s'ARRÊTE vraiment
    launch(run())
    return state.value
```

### Pourquoi c'est LE piège batterie de cette étape
Un launcher est une app bizarre : il n'est **jamais fermé**. Quand tu ouvres une autre app, l'écran d'accueil est
juste caché (l'activité est « stopped »), mais tous ses composables existent toujours. Donc :

- un `LaunchedEffect { while (true) { …; delay(1000) } }` dans l'horloge **continuerait à tourner** derrière
  YouTube, toute la journée ;
- un récepteur batterie enregistré au démarrage **resterait abonné** pour toujours.

`collectAsStateWithLifecycle()` coupe l'abonnement quand l'écran n'est plus visible et le relance quand tu reviens
(et la première valeur arrive tout de suite, donc l'heure est juste dès le retour). C'est pour ça que le critère n°2
vérifie les lignes `clock tick` dans Logcat.

Autres règles :
- L'horloge s'aligne **sur la minute** (`60 - secondes`), elle ne fait pas `delay(60_000)` qui dériverait. Pas de
  secondes affichées : 1 réveil par minute au lieu de 60.
- Rien ne se rafraîchit en arrière-plan. Pas de service, pas de `WorkManager` à cette étape.

---

## L'architecture

### Les fichiers (vue **Android**)

```
app › kotlin+java › com.example.launcher_hyprland
├── MainActivity.kt              (inchangé)
├── model/
│   ├── TileNode.kt              (modifié 5a) + BATTERY dans WidgetType (puis rien d'autre)
│   └── TreeOps.kt               (modifié 5a) + setWidget(tree, leafId, type)
├── data/
│   ├── LayoutStore.kt           (inchangé)
│   ├── ClockSource.kt           5a  NOUVEAU : Flow qui émet l'heure à chaque minute
│   ├── BatterySource.kt         5b  NOUVEAU : Flow qui émet (niveau, en charge ?) quand la batterie change
│   ├── NotesStore.kt            5c  NOUVEAU : texte des notes, rangé par id de case (DataStore « notes »)
│   └── CalendarSource.kt        5d  NOUVEAU : lit les prochains événements du calendrier
└── ui/
    ├── LauncherStyle.kt         (modifié 5a) + tailles de texte (textHuge, textBody, textSmall)
    ├── Tile.kt                  (inchangé)
    ├── TilingLayout.kt          (modifié 5a) passe leafId et active à WidgetContent
    ├── EditBar.kt               (modifié 5a) + bouton « widget »
    ├── WidgetPicker.kt          5a  NOUVEAU : la liste des widgets à choisir
    ├── NoteEditor.kt            5c  NOUVEAU : éditeur plein écran
    ├── HomeScreen.kt            (modifié 5a, 5c, 5d) + pickerOpen, openNoteId, « 2e toucher = action »
    └── widgets/                 NOUVEAU package : un fichier par widget
        ├── WidgetContent.kt     5a  le `when` qui choisit le widget + MonoText (sortis de Widgets.kt)
        ├── EmptyWidget.kt       5a  « vide · appui long › widget »
        ├── ClockWidget.kt       5a
        ├── BatteryWidget.kt     5b
        ├── NotesWidget.kt       5c
        └── AgendaWidget.kt      5d
```

`ui/Widgets.kt` disparaît : son contenu part dans `ui/widgets/`. Un fichier par widget, c'est comme un dossier
`widgets/` en Python avec un module par widget : ajouter un widget = ajouter un fichier + une ligne dans le `when`.

### Comment une case sait quel widget afficher (rappel + ce qui change)
C'est déjà dans l'arbre depuis l'étape 4 : `Leaf(id, widget: WidgetType)`. Changer le widget d'une case, c'est donc
**une opération sur l'arbre** comme les autres, et elle est sauvegardée gratuitement par `LayoutStore` :

```python
def set_widget(node, leaf_id, widget):
    if isinstance(node, Leaf):
        return replace(node, widget=widget) if node.id == leaf_id else node
    return replace(node, first=set_widget(node.first, leaf_id, widget),
                         second=set_widget(node.second, leaf_id, widget))
```

`WidgetContent` reçoit maintenant aussi l'**id de la case** et si elle est **active**, parce que certains widgets en
ont besoin (les notes : « quelle note est la mienne ? ») :

```python
def widget_content(widget, leaf_id, active):
    match widget:
        case Widget.CLOCK:   return clock_widget()
        case Widget.BATTERY: return battery_widget()
        case Widget.NOTES:   return notes_widget(leaf_id)
        case Widget.AGENDA:  return agenda_widget()
        case Widget.EMPTY:   return empty_widget()
```

**Piège de sauvegarde** : ajouter `BATTERY` à l'enum ne casse pas l'ancien JSON. En revanche **renommer ou supprimer**
une valeur (ex. `CLOCK` → `TIME`) rend la disposition sauvegardée illisible : `load()` renvoie `null` et tu repars
de l'arbre par défaut. Donc on **ajoute**, on ne renomme pas.

### Qui contient qui

```
HomeScreen                                  ← possède tout l'état
├── si openNoteId != null → NoteEditor(leafId)              (plein écran, à la place du reste)
└── sinon Column
    ├── TilingLayout(tree)
    │   └── NodeView → Tile → WidgetContent(widget, leafId, active)
    │                            ├── ClockWidget   ← s'abonne à ClockSource
    │                            ├── BatteryWidget ← s'abonne à BatterySource
    │                            ├── NotesWidget   ← s'abonne à NotesStore.observe(leafId)
    │                            ├── AgendaWidget  ← CalendarSource (si permission)
    │                            └── EmptyWidget
    └── si editMode :
        ├── si pickerOpen → WidgetPicker(onPick, onCancel)
        └── sinon        → EditBar(… + onWidget)
```

Chaque widget **s'abonne lui-même** à sa source (avec `remember` pour créer la source une seule fois, et
`LocalContext.current` quand la source a besoin du système). `HomeScreen` ne connaît pas l'heure ni la batterie :
il ne gère que **la disposition et la navigation**. C'est l'équivalent d'un widget Qt/Tk qui gère sa propre
minuterie au lieu de tout faire passer par la fenêtre principale.

### L'état nouveau dans HomeScreen

| Variable       | Type      | Rôle                                                                   |
|----------------|-----------|------------------------------------------------------------------------|
| `pickerOpen`   | `Boolean` | la liste des widgets remplace la barre d'édition                       |
| `openNoteId`   | `Int?`    | id de la case dont on édite la note, `null` sinon                      |

Et une règle de toucher, hors mode édition : **1er toucher = sélectionner, 2e toucher sur la case déjà active =
action du widget** (façon Hyprland : on met le focus, puis on interagit). On ne met **pas** de zone cliquable à
l'intérieur des widgets : elle volerait le toucher et l'appui long de la `Tile`.

```python
def on_tile_click(self, leaf_id):
    if self.swap_from_id is not None:  ...                      # inchangé (étape 4)
    elif leaf_id == self.active_id and not self.edit_mode:
        self.on_widget_action(find_leaf(self.tree, leaf_id))    # NOUVEAU
    else:
        self.active_id = leaf_id

def on_widget_action(self, leaf):
    match leaf.widget:
        case Widget.NOTES:  self.open_note_id = leaf.id
        case Widget.AGENDA: request_calendar_permission_or_open_calendar()   # 5d
        case Widget.CLOCK:  open_clock_app()                                 # bonus
        case _: pass
```

### Ce qui déclenche le redessin, widget par widget

| Widget   | Déclencheur                                              | Combien de fois                         |
|----------|----------------------------------------------------------|-----------------------------------------|
| Horloge  | `ClockSource` émet à chaque changement de minute         | 1 fois/min, **seulement si visible**    |
| Batterie | Android diffuse `ACTION_BATTERY_CHANGED`                 | à chaque changement de niveau/câble     |
| Notes    | `NotesStore` émet quand le texte sauvegardé change       | quand tu sors de l'éditeur              |
| Agenda   | relecture à chaque retour sur l'accueil + quand le calendrier change | rare                        |

### La batterie : transformer un « callback » en Flow
Android ne propose pas de `get_battery()` qu'on appellerait en boucle (ce serait du gaspillage). Il **diffuse un
message** (`ACTION_BATTERY_CHANGED`) à qui s'est abonné avec un `BroadcastReceiver`. On l'emballe dans un Flow avec
`callbackFlow`, qui garantit qu'on se **désabonne** quand plus personne n'écoute :

```python
async def battery_updates(context):
    queue = asyncio.Queue()
    receiver = lambda intent: queue.put_nowait(read_level_and_plugged(intent))
    context.register_receiver(receiver, "ACTION_BATTERY_CHANGED")   # Log.d("battery register")
    try:
        while True:
            yield await queue.get()
    finally:
        context.unregister_receiver(receiver)                      # Log.d("battery unregister")
```

Le message est « collant » (*sticky*) : dès l'abonnement, on reçoit la valeur actuelle, pas besoin d'attendre.
Le système envoie ce message souvent pendant la charge (la tension change), alors on ajoute
`.distinctUntilChanged()` : on ne redessine que si le **pourcentage** ou l'état **branché** change.

Affichage suggéré, très monospace : `62%` en gros, une barre texte `▮▮▮▮▮▮▯▯▯▯` et `charge` / `secteur` en atténué.

### Les notes : où ranger le texte
Le texte **n'est pas dans l'arbre**. L'arbre décrit la disposition ; si on y mettait le texte, chaque lettre tapée
recréerait et sauvegarderait toute la disposition. On le range à côté, dans un second DataStore (`notes`), **indexé
par l'id de la case** :

```python
notes = {}          # DataStore "notes" : {"note_5": "acheter du pain", "note_9": "..."}

def observe(leaf_id):   ...   # Flow : émet le texte de note_<id> à chaque changement
async def save(leaf_id, text): notes[f"note_{leaf_id}"] = text
```

Conséquences (voulues) :
- tu **échanges** deux cases → la feuille garde son id → la note suit ;
- tu changes une case notes en horloge puis de nouveau en notes → la note est **toujours là** ;
- tu **fermes** la case → la note reste orpheline dans le stockage (quelques octets ; on nettoiera à l'étape 8).

L'éditeur (`NoteEditor`) garde le texte en état local pendant la frappe et sauvegarde avec le même **debounce
de 500 ms** que la disposition, plus une sauvegarde au moment où tu sors (retour). Le widget affiche les premières
lignes, coupées avec `…`.

**Pourquoi un éditeur plein écran plutôt que taper dans la case** : quand le clavier s'ouvre, Android réduit la
fenêtre (`adjustResize` dans ton manifeste). Toute la disposition en cases serait écrasée dans la moitié haute.
En plein écran, on contrôle ça avec `Modifier.imePadding()` sur l'éditeur seul.

### L'agenda : une permission « dangereuse » (5d)
Lire le calendrier est une donnée personnelle : il faut la **déclarer** et la **demander**.

1. **Déclarer** dans `AndroidManifest.xml` : `<uses-permission android:name="android.permission.READ_CALENDAR" />`.
   Sans ça, la demande ne s'affiche même pas.
2. **Vérifier** à chaque retour sur l'accueil (`ContextCompat.checkSelfPermission`) : tu as pu l'accorder ou la
   retirer dans les Paramètres entre-temps.
3. **Demander** au 2e toucher sur la case agenda, avec `rememberLauncherForActivityResult(RequestPermission())`.
   C'est un callback : Android affiche sa boîte de dialogue, puis rappelle ton code avec `true` / `false`.
4. **Lire** avec `CalendarContract.Instances` (les occurrences réelles, y compris les événements répétés) pour les
   prochaines 24 h ou 7 jours, sur `Dispatchers.IO` (c'est une requête de base de données : jamais sur le fil
   d'affichage).

```python
def agenda_widget():
    granted = check_permission_on_every_resume("READ_CALENDAR")
    if not granted:
        return MonoText("agenda · touche 2× pour autoriser", color=Style.text_muted)
    events = collect_while_visible(calendar_source.upcoming(days=1), initial=[])
    return Column([MonoText(f"{e.start:%H:%M}  {e.title}") for e in events[:4]]
                  or [MonoText("rien aujourd'hui", color=Style.text_muted)])
```

Piège : si tu refuses **deux fois**, Android n'affiche plus jamais la boîte de dialogue (le `launch` renvoie
`false` immédiatement). Dans ce cas, le 2e toucher ouvre la page de l'app dans les Paramètres
(`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`) pour l'autoriser à la main.

---

## Les sous-étapes

### 5a. Choisir le widget d'une case + la vraie horloge
- Dépendance : `androidx.lifecycle:lifecycle-runtime-compose` (pour `collectAsStateWithLifecycle`), même version
  que `lifecycleRuntimeKtx` dans `gradle/libs.versions.toml`. Ta version (2.6.1) est vieille : la monter à la
  dernière stable des deux côtés est une bonne idée. Puis **Sync Now**.
- `model` : `BATTERY` dans `WidgetType`, `setWidget` dans `TreeOps` (+ un test dans `TreeOpsTest` si tu l'as).
- `LauncherStyle` : tailles de texte `textHuge` (horloge), `textBody`, `textSmall`.
- `ui/widgets/` : déplacer `MonoText`, `WidgetContent`, `EmptyWidget` et les factices (agenda, notes, en attendant
  5c/5d) ; `WidgetContent(type, leafId, active)`.
- `data/ClockSource.kt` : Flow aligné sur la minute, `Log.d("Launcher", "clock tick")` à chaque émission.
- `ClockWidget` : `HH:mm` en `textHuge`, date en français (`lun. 24 nov.`) en atténué, `collectAsStateWithLifecycle`.
  Dans l'aperçu d'Android Studio (`LocalInspectionMode`), afficher une heure fixe.
- `WidgetPicker` : une colonne de boutons texte (`horloge`, `batterie`, `notes`, `agenda`, `vide`, `annuler`), fond
  de case, callbacks seulement. `EditBar` : bouton `widget`. `HomeScreen` : `pickerOpen`, `tree = setWidget(…)`.
- **Test sur le Pixel** : la minute change pile ; Logcat (filtre `clock tick`) : rien quand tu ouvres une autre app
  ou éteins l'écran, une ligne tout de suite au retour. Divise une case, mets-y une 2e horloge, redémarre l'app :
  elle est toujours là.

### 5b. Batterie
- `data/BatterySource.kt` : `callbackFlow` + `ContextCompat.registerReceiver(…, RECEIVER_NOT_EXPORTED)`,
  `awaitClose { unregisterReceiver }`, logs `battery register` / `battery unregister`, `distinctUntilChanged()`.
- `BatteryWidget` : pourcentage, barre texte, `charge` / `secteur`.
- **Test** : branche / débranche le câble, la case change en moins d'une seconde. Logcat : `battery unregister`
  quand tu ouvres une autre app.

### 5c. Notes
- `data/NotesStore.kt` : DataStore Preferences **dans un fichier à part** (`notes`), `observe(leafId): Flow<String>`
  et `suspend fun save(leafId, text)`.
- `NotesWidget(leafId)` : premières lignes du texte (`maxLines` + `…`), ou `notes · touche 2× pour écrire` si vide.
- `NoteEditor(leafId, onClose)` : `BasicTextField` plein écran, fond de case, monospace, `imePadding()`, clavier
  ouvert automatiquement (`FocusRequester`), debounce 500 ms, `BackHandler` qui sauvegarde puis ferme.
- `HomeScreen` : `openNoteId` ; règle « 2e toucher = action ».
- **Test** : écris une note, retour, la case l'affiche ; échange la case avec l'horloge, la note suit ; redémarre
  le Pixel, elle est là.

### 5d. Agenda (optionnel cette semaine)
- Manifeste : `READ_CALENDAR`.
- `data/CalendarSource.kt` : `upcoming(days)` via `CalendarContract.Instances` sur `Dispatchers.IO` ; relu à chaque
  retour sur l'accueil (le `collectAsStateWithLifecycle` relance le Flow) et, bonus, quand le calendrier change
  (`ContentObserver` dans un `callbackFlow`).
- `AgendaWidget` : 3-4 prochains événements `HH:mm  titre`, ou le message d'autorisation.
- `HomeScreen` : `rememberLauncherForActivityResult(RequestPermission())` ; 2e toucher → demande, ou Paramètres si
  refusé définitivement, ou ouverture de l'app Agenda si déjà autorisé.
- **Test** : refuse une fois (message dans la case, pas de crash), accepte au 2e essai (les événements
  apparaissent), ajoute un événement dans Google Agenda, reviens : il est là.

---

## Ce que tu demandes au Claude d'Android Studio

Une **nouvelle conversation** pour l'étape 5. Colle ce guide en premier message avec :
> « Voici le guide de l'étape 5 de mon launcher. Lis d'abord le code existant (packages model, data, ui). Ne fais
> rien tant que je ne te donne pas une sous-étape. Pour chaque sous-étape : implémente un fichier à la fois, dans
> l'ordre du guide ; après chaque fichier, explique en 3-4 phrases son rôle et comment il s'emboîte avec les autres,
> en le comparant à du Python. Règles : aucune couleur ni taille en dur dans les widgets, tout passe par
> LauncherStyle ; pas de MaterialTheme ; les packages model et data n'importent rien de Compose ; chaque widget
> s'abonne à sa source avec collectAsStateWithLifecycle (jamais de boucle while/delay dans un LaunchedEffect) ;
> pas de zone cliquable à l'intérieur d'un widget, les touchers passent par la Tile ; on ajoute des valeurs à
> WidgetType sans jamais en renommer. »

Puis, une à la fois, en testant sur le Pixel entre chaque :
> « Fais la sous-étape 5a. Commence par la dépendance Gradle et dis-moi quand faire Sync Now. »

> « Fais la sous-étape 5b. »

> « Fais la sous-étape 5c. »

> « Fais la sous-étape 5d. »

## Vérifie que tu as compris (sans l'aide de Claude)
1. Tu ouvres YouTube pendant 1 h. Qu'est-ce qui tourne encore côté launcher pendant ce temps, et qu'est-ce qui se
   passe dans `ClockWidget` à la seconde où tu reviens sur l'accueil ?
2. Tu échanges une case notes avec l'horloge. Pourquoi la note suit-elle la case, alors que son texte n'est pas
   dans l'arbre ?
3. Pourquoi changer le widget d'une case passe par une fonction de `TreeOps`, alors que changer le texte d'une note
   n'y passe pas ?

Envoie-moi tes réponses si tu veux une correction.

## Pièges possibles
- **`collectAsStateWithLifecycle` introuvable** : la dépendance `lifecycle-runtime-compose` manque, ou pas de
  Sync Now.
- **Deux DataStore sur le même fichier** → crash « There are multiple DataStores active for the same file ».
  `NotesStore` doit utiliser un **autre nom de fichier** que `LayoutStore`, et chaque DataStore doit être créé
  **une seule fois** (délégué `preferencesDataStore` en haut du fichier, pas dans une fonction).
- **L'heure est fausse d'une minute au retour** : la source n'émet pas tout de suite à l'abonnement (elle attend
  d'abord). Il faut `emit(maintenant)` **avant** le premier `delay`.
- **La date en anglais** : formatter avec `Locale.FRENCH` (ou la langue du téléphone).
- **`registerReceiver` souligné / crash sur Android 14+** : il faut préciser `RECEIVER_NOT_EXPORTED` ;
  `ContextCompat.registerReceiver` le fait proprement.
- **L'appui long ne marche plus sur un widget** : un `clickable` ou un `TextField` s'est glissé dans le widget.
  Tout toucher passe par la `Tile`.
- **Le clavier écrase les cases** : l'édition se fait dans `NoteEditor` plein écran, pas dans la case.
- **La note tapée est perdue** : on a quitté l'éditeur avant la fin du debounce. Le `BackHandler` doit
  **sauvegarder puis fermer**.
- **La demande de permission ne s'affiche pas** : `READ_CALENDAR` manque dans le manifeste, ou tu as déjà refusé
  deux fois (voir 5d). Pour remettre à zéro : Paramètres › Applis › launcher_hyprland › Autorisations.
- **Logcat trop bavard** : filtre `tag:Launcher`.

## 🛟 Si ça plante (à lire AVANT de tester)
- Volet de notifications › roue dentée › Applis › Applis par défaut › Appli d'écran d'accueil › **Pixel Launcher**.
- Ou, téléphone branché, dans l'onglet Terminal d'Android Studio :
  `adb uninstall com.example.launcher_hyprland` → le Pixel revient tout seul à son launcher.
- Crash en boucle au démarrage (disposition ou notes illisibles) : Paramètres › Applis › launcher_hyprland ›
  Stockage › **Effacer les données** (tu perds disposition et notes).
- Écran noir ou crash : onglet **Logcat**, filtre `AndroidRuntime`, envoie-moi la ligne rouge.
- Revenir à la sous-étape précédente : **Git › Rollback…** (annule ce qui n'est pas encore commité).

## À la fin
1. **Après chaque sous-étape** : Git › Commit… (« 5a : choix du widget + horloge », etc.) puis Git › Push….
   **Vérifie que les nouveaux fichiers (en rouge dans l'arborescence) sont bien cochés** avant de commiter.
2. Dans `ROADMAP.md` (à la racine du dépôt), coche les lignes de l'étape 5 au fur et à mesure.
3. Dis-moi si tu veux que je relise ton code sur GitHub, par exemple après 5a (la façon dont un widget s'abonne à
   sa source servira pour tous les suivants).
