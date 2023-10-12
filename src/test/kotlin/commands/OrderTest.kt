package commands

import Mod
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderTest {

    @Test
    fun modsAreNotNegativeOrdered() {
        val mods = fakeMods(2)
        setModOrder(mods, 0, -1)
        assertEquals(0, mods[0].loadOrder)

        val expected = listOf(100, 101)
        val actual = mods.map { it.id }
        assertEquals(expected, actual)
    }

    @Test
    fun nonExistentModDoesNotError() {
        val mods = fakeMods(2)
        setModOrder(mods, 100, 1)

        val expected = listOf(100, 101)
        val actual = mods.map { it.id }
        assertEquals(expected, actual)
    }

    @Test
    fun modsAreShiftedDown() {
        val mods = fakeMods(5)
        setModOrder(mods, 4, 0)
        assertEquals(0, mods[4].loadOrder)

        val sortedMods = mods.sortedBy { it.loadOrder }
        val expected = listOf(104, 100, 101, 102, 103)
        val actual = sortedMods.map { it.id }
        assertEquals(expected, actual)
    }

    @Test
    fun modsAreShiftedUp() {
        val mods = fakeMods(5)
        setModOrder(mods, 1, 10)
        assertEquals(10, mods[1].loadOrder)

        val sortedMods = mods.sortedBy { it.loadOrder }
        val expected = listOf(100, 102, 103, 104, 101)
        val actual = sortedMods.map { it.id }
        assertEquals(expected, actual)
    }

    @Test
    fun modsAreShiftedBothWays() {
        val mods = fakeMods(5)
        setModOrder(mods, 3, 1)
        assertEquals(1, mods[3].loadOrder)

        val sortedMods = mods.sortedBy { it.loadOrder }
        val expected = listOf(100, 103, 101, 102, 104)
        val actual = sortedMods.map { it.id }
        assertEquals(expected, actual)
    }


    private fun fakeMods(number: Int): List<Mod> {
        return (0..<number).map { i ->
            Mod("Mod-$i", "mods/mod-$i", i, 100 + i)
        }
    }
}