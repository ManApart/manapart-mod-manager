package commands

import GameConfig
import Mod
import commands.deploy.setModOrder
import gameConfig
import org.junit.jupiter.api.BeforeAll
import testingMode
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderTest {
    companion object {
        @JvmStatic
        @BeforeAll
        fun setup() {
            testingMode = true
            gameConfig = GameConfig()
        }
    }

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
    fun modsAreShiftedByOrderNotIndex() {
        val mods = listOf(5,4,3,2,1).mapIndexed { i, order ->
            Mod("Mod-$i", "mods/mod-$i", order, i)
        }
        setModOrder(mods, 0, 0)
        assertEquals(0, mods[0].loadOrder)

        val sortedMods = mods.sortedBy { it.loadOrder }
        val expected = listOf(0,4,3,2,1)
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
