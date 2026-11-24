# Feuille de route — Launcher Hyprland

Launcher Android personnel, épuré, avec un écran d'accueil découpé en cases (tiling façon Hyprland).
Appareil de test : Pixel 9a. Rythme : 4-5 h/semaine, petites étapes testables sur le téléphone.

Légende : `[x]` fait · `[ ]` à faire

## 1. Setup + « Hello » sur le Pixel ✅
- [x] Installer Android Studio (Windows)
- [x] Créer le projet Compose vide (minSdk 33)
- [x] Lancer « Hello Android » sur le Pixel 9a
- [x] Créer le dépôt GitHub

## 2. Devenir un launcher ✅
- [x] Manifeste : ajouter les catégories `HOME` + `DEFAULT` à `MainActivity`
- [x] `launchMode="singleTask"` : une seule instance de l'écran d'accueil
- [x] Choisir l'app comme launcher par défaut (Paramètres › Applis › Applis par défaut › Appli d'écran d'accueil)
- [x] Bouton retour : ne rien faire sur l'écran d'accueil (`BackHandler`)
- [x] Afficher le fond d'écran du système derrière l'interface (`windowShowWallpaper`)
- [x] Plan de secours testé : revenir au Pixel Launcher (voir en bas)

## 3. Grille statique de 3 cases ✅
- [x] 3 zones fixes : horloge en haut, agenda + notes côte à côte dessous
- [x] Un widget factice par case (monospace)
- [x] Espacements (« gaps ») façon Hyprland, cases sombres arrondies
- [x] Case active au toucher (contour couleur d'accent)

## 4. Tiling dynamique (BSP)
- [ ] Modèle : arbre binaire (nœud = découpe H/V + ratio, feuille = widget)
- [ ] Rendu Compose récursif de l'arbre
- [ ] Diviser / fermer une case
- [ ] Redimensionner (glisser la séparation → ratio)
- [ ] Échanger deux cases
- [ ] Sauvegarder la disposition (DataStore)

## 5. Premiers vrais widgets maison
- [ ] 5a : choisir le widget d'une case + vraie horloge/date (s'arrête quand l'accueil est caché)
- [ ] 5b : batterie (niveau + charge)
- [ ] 5c : notes (éditeur plein écran, sauvegardées par case)
- [ ] 5d : agenda (permission calendrier)
- Reporté : apps favorites (étape 6), météo et musique (étape 6b, via les widgets Android, ou maison plus tard)

## 6. Liste et recherche d'apps
- [ ] Lister les apps (`LauncherApps`), rechercher, lancer

## 6b. Widgets Android dans les cases (AppWidgetHost)
- [ ] Une case peut afficher un widget d'une autre app (météo, Google Agenda, Spotify…)
- [ ] Choisir le widget, autorisation système, configuration, garder son id dans l'arbre

## 7. Gestes, animations, thème
- [ ] Gestes, animations, thème, effets AGSL (shaders)

## 8. Finitions
- [ ] Stabilité, batterie, cycle de vie

---

## 🛟 Plan de secours : revenir au Pixel Launcher

1. **Paramètres** : volet de notifications › roue dentée › Applis › Applis par défaut ›
   Appli d'écran d'accueil › **Pixel Launcher**.
2. **Android Studio** : relancer une version corrigée (▶ Run) ou désinstaller l'app.
3. **ADB** (onglet Terminal d'Android Studio, téléphone branché) :
   ```
   adb uninstall com.example.launcher_hyprland
   ```
   Le téléphone revient tout seul au Pixel Launcher.
