package com.example.launcher_hyprland.model

// Kotlin pur : aucun import Compose ici. C'est la "logique" du tiling, testable sans téléphone.
// kotlinx.serialization n'est pas de l'affichage : il a le droit d'être ici.

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Ce qu'affiche une case. Sauvegardé par NOM dans le JSON de la disposition :
 * on peut AJOUTER des valeurs, jamais en renommer ni en supprimer (l'ancien JSON deviendrait illisible).
 */
enum class WidgetType { CLOCK, AGENDA, NOTES, EMPTY, BATTERY, CALENDAR }

/** Sens de la découpe d'un [TileNode.Split]. */
enum class Direction {
    ROW,     // enfants côte à côte (gauche | droite) → Compose Row
    COLUMN,  // enfants empilés     (haut / bas)      → Compose Column
}

/**
 * Un nœud de l'arbre BSP : soit une case ([Leaf]), soit une découpe en deux ([Split]).
 * "sealed" = la liste des cas est fermée : un `when` qui oublie un cas ne compile pas.
 * Tout est immuable (val + data class) : pour changer l'arbre, on en recrée un avec `copy`.
 * @Serializable : le plugin génère la conversion vers/depuis JSON. Pour un sealed interface,
 * il ajoute un champ "type" ("leaf" ou "split", fixés par @SerialName) pour savoir quoi recréer.
 */
@Serializable
sealed interface TileNode {
    val id: Int

    /** Une case de l'écran, qui affiche un widget. */
    @Serializable
    @SerialName("leaf")
    data class Leaf(
        override val id: Int,
        val widget: WidgetType,
    ) : TileNode

    /** Une zone coupée en deux : [first] prend [ratio] de la place, [second] le reste. */
    @Serializable
    @SerialName("split")
    data class Split(
        override val id: Int,
        val direction: Direction,
        val ratio: Float,
        val first: TileNode,
        val second: TileNode,
    ) : TileNode
}

/** La disposition de l'étape 3 : horloge en haut (1/3), agenda | notes en bas. */
fun defaultTree(): TileNode =
    TileNode.Split(
        id = 1,
        direction = Direction.COLUMN,
        ratio = 0.33f,
        first = TileNode.Leaf(2, WidgetType.CLOCK),
        second = TileNode.Split(
            id = 3,
            direction = Direction.ROW,
            ratio = 0.5f,
            first = TileNode.Leaf(4, WidgetType.AGENDA),
            second = TileNode.Leaf(5, WidgetType.NOTES),
        ),
    )
