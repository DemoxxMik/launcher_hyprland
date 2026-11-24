package com.example.launcher_hyprland.model

// Fonctions PURES sur l'arbre : elles ne modifient jamais l'arbre reçu, elles en renvoient un nouveau.
// Toutes sont récursives : on descend jusqu'au nœud visé, et en remontant on recrée les parents avec `copy`.

/** Le plus grand id de l'arbre + 1 : un id libre pour un nouveau nœud. */
fun nextId(node: TileNode): Int = maxId(node) + 1

private fun maxId(node: TileNode): Int = when (node) {
    is TileNode.Leaf -> node.id
    is TileNode.Split -> maxOf(node.id, maxId(node.first), maxId(node.second))
}

/** L'id de la feuille la plus en haut à gauche (on descend toujours par `first`). */
fun firstLeafId(node: TileNode): Int = when (node) {
    is TileNode.Leaf -> node.id
    is TileNode.Split -> firstLeafId(node.first)
}

/** Nombre de cases (feuilles) dans l'arbre. */
fun leafCount(node: TileNode): Int = when (node) {
    is TileNode.Leaf -> 1
    is TileNode.Split -> leafCount(node.first) + leafCount(node.second)
}

/**
 * Remplace la feuille [leafId] par une découpe [splitId] en deux moitiés :
 * l'ancienne feuille, puis une nouvelle case vide [newLeafId].
 */
fun splitLeaf(
    node: TileNode,
    leafId: Int,
    direction: Direction,
    splitId: Int,
    newLeafId: Int,
): TileNode = when (node) {
    is TileNode.Leaf ->
        if (node.id == leafId) {
            TileNode.Split(
                id = splitId,
                direction = direction,
                ratio = 0.5f,
                first = node,
                second = TileNode.Leaf(newLeafId, WidgetType.EMPTY),
            )
        } else {
            node
        }

    is TileNode.Split -> node.copy(
        first = splitLeaf(node.first, leafId, direction, splitId, newLeafId),
        second = splitLeaf(node.second, leafId, direction, splitId, newLeafId),
    )
}

/**
 * Ferme la feuille [leafId] : sa découpe parente est remplacée par l'autre enfant (la sœur).
 * S'il ne reste qu'une seule case, on ne ferme rien.
 */
fun closeLeaf(root: TileNode, leafId: Int): TileNode =
    if (leafCount(root) <= 1) root else closeIn(root, leafId)

private fun closeIn(node: TileNode, leafId: Int): TileNode = when (node) {
    is TileNode.Leaf -> node
    is TileNode.Split -> when {
        node.first is TileNode.Leaf && node.first.id == leafId -> node.second
        node.second is TileNode.Leaf && node.second.id == leafId -> node.first
        else -> node.copy(
            first = closeIn(node.first, leafId),
            second = closeIn(node.second, leafId),
        )
    }
}

/** La sœur de la feuille [leafId] (le nœud qui prendrait sa place si on la fermait), ou null. */
fun siblingOf(node: TileNode, leafId: Int): TileNode? = when (node) {
    is TileNode.Leaf -> null
    is TileNode.Split -> when {
        node.first is TileNode.Leaf && node.first.id == leafId -> node.second
        node.second is TileNode.Leaf && node.second.id == leafId -> node.first
        else -> siblingOf(node.first, leafId) ?: siblingOf(node.second, leafId)
    }
}

/** La feuille d'id [leafId], ou null si elle n'est pas dans l'arbre. */
fun findLeaf(node: TileNode, leafId: Int): TileNode.Leaf? = when (node) {
    is TileNode.Leaf -> if (node.id == leafId) node else null
    is TileNode.Split -> findLeaf(node.first, leafId) ?: findLeaf(node.second, leafId)
}

/**
 * Échange de place les feuilles [idA] et [idB] (la feuille entière, avec son id, pas juste le widget :
 * plus tard, l'état d'un widget suivra sa case). Ne fait rien si l'une manque ou si idA == idB.
 */
fun swapLeaves(root: TileNode, idA: Int, idB: Int): TileNode {
    if (idA == idB) return root
    val leafA = findLeaf(root, idA) ?: return root
    val leafB = findLeaf(root, idB) ?: return root
    return replaceLeaves(root, leafA, leafB)
}

private fun replaceLeaves(node: TileNode, a: TileNode.Leaf, b: TileNode.Leaf): TileNode = when (node) {
    is TileNode.Leaf -> when (node.id) {
        a.id -> b   // là où était A, on met B
        b.id -> a   // et inversement
        else -> node
    }
    is TileNode.Split -> node.copy(
        first = replaceLeaves(node.first, a, b),
        second = replaceLeaves(node.second, a, b),
    )
}

/** Change le widget affiché par la feuille [leafId]. Les autres nœuds (et l'id de la feuille) ne bougent pas. */
fun setWidget(node: TileNode, leafId: Int, widget: WidgetType): TileNode = when (node) {
    is TileNode.Leaf -> if (node.id == leafId) node.copy(widget = widget) else node
    is TileNode.Split -> node.copy(
        first = setWidget(node.first, leafId, widget),
        second = setWidget(node.second, leafId, widget),
    )
}

/** Bornes du ratio d'une découpe : aucune case ne peut descendre sous 15 % de sa zone. */
const val MIN_RATIO = 0.15f
const val MAX_RATIO = 0.85f

/**
 * Change le partage de la découpe [splitId] de [delta] (une fraction, ex. +0.02 = 2 % de plus
 * pour le premier enfant). Le résultat est borné entre [MIN_RATIO] et [MAX_RATIO].
 */
fun resize(node: TileNode, splitId: Int, delta: Float): TileNode = when (node) {
    is TileNode.Leaf -> node
    is TileNode.Split ->
        if (node.id == splitId) {
            node.copy(ratio = (node.ratio + delta).coerceIn(MIN_RATIO, MAX_RATIO))
        } else {
            node.copy(
                first = resize(node.first, splitId, delta),
                second = resize(node.second, splitId, delta),
            )
        }
}
