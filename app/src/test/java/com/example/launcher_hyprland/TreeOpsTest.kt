package com.example.launcher_hyprland

import com.example.launcher_hyprland.model.Direction
import com.example.launcher_hyprland.model.TileNode
import com.example.launcher_hyprland.model.WidgetType
import com.example.launcher_hyprland.model.closeLeaf
import com.example.launcher_hyprland.model.defaultTree
import com.example.launcher_hyprland.model.firstLeafId
import com.example.launcher_hyprland.model.leafCount
import com.example.launcher_hyprland.model.MAX_RATIO
import com.example.launcher_hyprland.model.MIN_RATIO
import com.example.launcher_hyprland.model.nextId
import com.example.launcher_hyprland.model.resize
import com.example.launcher_hyprland.model.setWidget
import com.example.launcher_hyprland.model.splitLeaf
import com.example.launcher_hyprland.model.swapLeaves
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Test

/** Tests du modèle, sur le PC, sans téléphone (comme pytest). */
class TreeOpsTest {

    @Test
    fun defaultTree_hasThreeLeaves_andNextIdIs6() {
        val tree = defaultTree()
        assertEquals(3, leafCount(tree))
        assertEquals(6, nextId(tree))
        assertEquals(2, firstLeafId(tree)) // l'horloge, en haut à gauche
    }

    @Test
    fun splitLeaf_replacesLeafBySplitWithEmptyCase() {
        val tree = splitLeaf(defaultTree(), leafId = 4, Direction.ROW, splitId = 6, newLeafId = 7)

        assertEquals(4, leafCount(tree))
        val bottom = (tree as TileNode.Split).second as TileNode.Split
        val newSplit = bottom.first as TileNode.Split
        assertEquals(Direction.ROW, newSplit.direction)
        assertEquals(0.5f, newSplit.ratio)
        assertEquals(TileNode.Leaf(4, WidgetType.AGENDA), newSplit.first)
        assertEquals(TileNode.Leaf(7, WidgetType.EMPTY), newSplit.second)
    }

    @Test
    fun splitLeaf_doesNotModifyOriginalTree() {
        val original = defaultTree()
        splitLeaf(original, leafId = 4, Direction.ROW, splitId = 6, newLeafId = 7)
        assertEquals(defaultTree(), original) // immuable : l'original n'a pas bougé
    }

    @Test
    fun splitThenClose_givesBackOriginalTree() {
        val original = defaultTree()
        val split = splitLeaf(original, leafId = 4, Direction.ROW, splitId = 6, newLeafId = 7)
        val closed = closeLeaf(split, leafId = 7)
        assertEquals(original, closed)
    }

    @Test
    fun closeLeaf_siblingTakesThePlace() {
        // fermer l'horloge (2) : la racine est remplacée par sa sœur, la découpe agenda | notes
        val closed = closeLeaf(defaultTree(), leafId = 2)
        val expected = TileNode.Split(
            3, Direction.ROW, 0.5f,
            TileNode.Leaf(4, WidgetType.AGENDA),
            TileNode.Leaf(5, WidgetType.NOTES),
        )
        assertEquals(expected, closed)
    }

    @Test
    fun closeLeaf_lastLeaf_doesNothing() {
        val single = TileNode.Leaf(1, WidgetType.CLOCK)
        assertSame(single, closeLeaf(single, leafId = 1))
    }

    @Test
    fun closeLeaf_unknownId_doesNothing() {
        assertEquals(defaultTree(), closeLeaf(defaultTree(), leafId = 99))
    }

    @Test
    fun resize_changesOnlyTheTargetSplit() {
        val tree = resize(defaultTree(), splitId = 3, delta = 0.1f) as TileNode.Split
        assertEquals(0.33f, tree.ratio, 0.0001f)                              // la racine n'a pas bougé
        assertEquals(0.6f, (tree.second as TileNode.Split).ratio, 0.0001f)    // 0.5 + 0.1
    }

    @Test
    fun swapLeaves_exchangesWholeLeaves() {
        // horloge (2) <-> notes (5)
        val swapped = swapLeaves(defaultTree(), idA = 2, idB = 5)
        val expected = TileNode.Split(
            1, Direction.COLUMN, 0.33f,
            TileNode.Leaf(5, WidgetType.NOTES),
            TileNode.Split(
                3, Direction.ROW, 0.5f,
                TileNode.Leaf(4, WidgetType.AGENDA),
                TileNode.Leaf(2, WidgetType.CLOCK),
            ),
        )
        assertEquals(expected, swapped)
    }

    @Test
    fun swapTwice_givesBackOriginalTree() {
        val twice = swapLeaves(swapLeaves(defaultTree(), 2, 5), 2, 5)
        assertEquals(defaultTree(), twice)
    }

    @Test
    fun swapLeaves_sameOrUnknownId_doesNothing() {
        assertEquals(defaultTree(), swapLeaves(defaultTree(), 4, 4))
        assertEquals(defaultTree(), swapLeaves(defaultTree(), 4, 99))
        assertEquals(defaultTree(), swapLeaves(defaultTree(), 3, 4)) // 3 est une découpe, pas une feuille
    }

    @Test
    fun json_roundTrip_givesBackSameTree() {
        // un arbre un peu modifié : divisé, redimensionné, échangé
        var tree = splitLeaf(defaultTree(), 4, Direction.COLUMN, splitId = 6, newLeafId = 7)
        tree = resize(tree, splitId = 1, delta = 0.2f)
        tree = swapLeaves(tree, 2, 7)

        val text = Json.encodeToString(tree)
        assertTrue(text.contains("\"type\":\"split\"")) // le champ "type" ajouté pour le sealed interface
        assertEquals(tree, Json.decodeFromString<TileNode>(text))
    }

    @Test
    fun setWidget_changesOnlyTheTargetLeaf() {
        val tree = setWidget(defaultTree(), leafId = 4, WidgetType.BATTERY)
        val expected = TileNode.Split(
            1, Direction.COLUMN, 0.33f,
            TileNode.Leaf(2, WidgetType.CLOCK),
            TileNode.Split(
                3, Direction.ROW, 0.5f,
                TileNode.Leaf(4, WidgetType.BATTERY), // même id, nouveau widget
                TileNode.Leaf(5, WidgetType.NOTES),
            ),
        )
        assertEquals(expected, tree)
    }

    @Test
    fun setWidget_unknownOrSplitId_doesNothing() {
        assertEquals(defaultTree(), setWidget(defaultTree(), leafId = 99, WidgetType.EMPTY))
        assertEquals(defaultTree(), setWidget(defaultTree(), leafId = 3, WidgetType.EMPTY))
    }

    @Test
    fun resize_isClampedBetweenMinAndMax() {
        val bigger = resize(defaultTree(), splitId = 1, delta = 5f) as TileNode.Split
        assertEquals(MAX_RATIO, bigger.ratio, 0.0001f)
        val smaller = resize(defaultTree(), splitId = 1, delta = -5f) as TileNode.Split
        assertEquals(MIN_RATIO, smaller.ratio, 0.0001f)
    }
}
