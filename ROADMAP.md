# Feuille de route — Launcher Hyprland

Launcher Android personnel, épuré, avec un écran d'accueil découpé en cases (tiling façon Hyprland).
Appareil de test : Pixel 9a. Rythme : 4-5 h/semaine, petites étapes testables sur le téléphone.

Légende : `[x]` fait · `[ ]` à faire

## 1. Setup + « Hello » sur le Pixel ✅
- [x] Installer Android Studio (Windows)
- [x] Créer le projet Compose vide (minSdk 33)
- [x] Lancer « Hello Android » sur le Pixel 9a
- [x] Créer le dépôt GitHub

## 2. Devenir un launcher
- [ ] Manifeste : ajouter les catégories `HOME` + `DEFAULT` à `MainActivity`
- [ ] `launchMode="singleTask"` : une seule instance de l'écran d'accueil
- [ ] Choisir l'app comme launcher par défaut (Paramètres › Applis › Applis par défaut › Appli d'écran d'accueil)
- [ ] Bouton retour : ne rien faire sur l'écran d'accueil (`BackHandler`)
- [ ] Afficher le fond d'écran du système derrière l'interface (`windowShowWallpaper`)
- [ ] Plan de secours testé : revenir au Pixel Launcher (voir en bas)

## 3. Grille statique de 3 cases
- [ ] 3 zones fixes (une grande à gauche, deux empilées à droite)
- [ ] Un widget factice par case
- [ ] Espacements (« gaps ») façon Hyprland

## 4. Tiling dynamique (BSP)
- [ ] Modèle : arbre binaire (nœud = découpe H/V + ratio, feuille = widget)
- [ ] Rendu Compose récursif de l'arbre
- [ ] Diviser / fermer une case
- [ ] Redimensionner (glisser la séparation → ratio)
- [ ] Échanger deux cases
- [ ] Sauvegarder la disposition (DataStore)

## 5. Premiers vrais widgets maison
- [ ] Horloge, batterie, notes, raccourcis
- [ ] Météo, musique

## 6. Liste et recherche d'apps
- [ ] Lister les apps (`LauncherApps`), rechercher, lancer

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
