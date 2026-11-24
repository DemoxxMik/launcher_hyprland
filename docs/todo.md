# À faire (petits ajustements)

Liste des détails à reprendre plus tard. Une ligne par point ; cocher quand c'est fait, et noter la date.

## Lanceur d'apps
- [ ] Icônes : le rendu n'est pas encore satisfaisant après 8.5 (silhouette teintée / icône en couleurs quand l'aplat est plein). Ajuster :
  - le seuil `SOLID_RATIO` (85 %) dans `data/AppsSource.kt` ;
  - la taille apparente des silhouettes issues de la couche avant des icônes adaptatives (marges de 108 dp, elles paraissent plus petites) ;
  - décider si les icônes en couleurs restent une exception ou si elles passent dans le futur réglage « icônes monochromes / silhouette / sans » (8.6).

## Widget « thème » (8.7)
- [ ] Revoir l'interface du choix de palette (`ui/widgets/ThemeWidget.kt`) : disposition et rendu des 6 pastilles de style.
- [ ] Revoir le bouton « départ » (retour au réglage d'origine, `ThemeSettings.restoreOriginal`) : son libellé, quand il apparaît, et son fonctionnement (la sauvegarde est effacée après le retour).

## Divers
- [ ] Commit à faire pour 8.4 et 8.5 (refusé pendant la session, rien n'est committé).
- [ ] Vérifier sur le Pixel l'auto-ajustement du texte (horloge, batterie) au redimensionnement d'une case.
