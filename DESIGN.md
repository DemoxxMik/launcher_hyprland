# Design du launcher (référence, décidé le 2026-11-24)

Maquette interactive : `docs/maquette.html` (HTML/CSS lisibles : mesures, ordre, états). Ses couleurs sont simulées en JS ; dans l'app, elles viennent des palettes système ci-dessous. En cas de doute, ce fichier prime sur la maquette.

## 1. Ambiance
Calme, douce, reposante. Pastel : sable, beige, sauge, vert d'eau, bleu ardoise. Aplats, contours fins.
Interdit : néon, lueur (glow), accent saturé, dégradé violet/bleu, icônes colorées.
Texte d'abord ; petites icônes monochromes au trait en appui.

## 2. Couleurs = fond d'écran (priorité n°1)
Aucune couleur en dur hors secours. Source : `android.R.color.system_*` (Material You). Pas de MaterialTheme : un objet palette transmis par CompositionLocal. Secours (aperçu Android Studio) = anciennes valeurs de LauncherStyle.

| rôle | sombre (défaut) | clair (option) |
|---|---|---|
| fond de case | neutral1_900 × 90 % | neutral1_50 × 92 % |
| contour | neutral2_700 | neutral2_200 |
| texte | neutral1_100 | neutral1_900 |
| texte atténué | neutral2_300 | neutral2_600 |
| accent : case active, pastilles, heure actuelle | accent1_200 | accent1_600 |
| accent secondaire : points calendrier, barres | accent3_200 | accent3_600 |
| champ / sélection | neutral1_800 | neutral1_100 |
| texte sur accent | neutral1_900 | neutral1_50 |

Changer de fond d'écran doit réaccorder le launcher tout seul.

## 3. Cases
Écart 10 dp (entre cases et avec les bords), coins 8 dp, contour 1 dp, marge intérieure 12 dp.
Fond opaque 90 % : le fond d'écran se devine entre les cases. Flou : plus tard (étape 7). Option future : ombre pleine décalée 3 dp, couleur foncée de la palette.
Case active : contour accent + son étiquette devient une pastille remplie d'accent (texte sur accent). Autres étiquettes : atténuées, petites, minuscules.

## 4. Typographie
Monospace partout, cible JetBrains Mono embarquée (sinon monospace système).
Horloge ~44 sp Light ; texte courant ~13 sp ; étiquettes ~11 sp minuscules.

## 5. Espaces de travail
4 pages (1 2 3 4). Bascule : toucher la barre du bas ou balayer horizontalement.
Barre du bas : case fine pleine largeur, pastilles ; active = remplie d'accent et plus large, autres atténuées.
Espace 2 = accueil (défaut), 3 = calendrier plein écran, 1 et 4 libres. Chaque espace a son propre arbre de cases (ne pas changer la structure de l'arbre).

## 6. Accueil (espace 2)
Horloge pleine largeur en haut : heure, date en toutes lettres, météo à droite (picto + « 9° couvert »).
Aujourd'hui : frise verticale, ligne « maintenant » en accent, prochain événement marqué « ensuite ».
Notes ; apps favorites (icône monochrome accent + nom) ; musique (titre, artiste, commandes en icônes, barre en accent secondaire).

## 7. Lanceur d'apps (façon rofi)
Balayage vers le haut : grand panneau par-dessus l'accueil, même style de case.
Champ en haut : « apps » en accent + texte tapé + curseur. Liste filtrée : icône monochrome en accent, lettres trouvées en accent, ligne sélectionnée sur fond « champ ».

## 8. Plus tard
Flou réel, shaders AGSL, animations de bascule, effets au toucher. Données réelles météo/musique.
