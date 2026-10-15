package local.vn.suppermario

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CactusCycleTest {
    @Test fun waitingOnRimKeepsHiddenPlantSafe() {
        val cycle = CactusCycle(1f)
        repeat(600) { cycle.update(1f / 60f, playerNear = true) }
        assertEquals(0f, cycle.extension, 0.001f)
        repeat(120) { cycle.update(1f / 60f, playerNear = false) }
        assertEquals(0.5f, cycle.extension, 0.01f)
    }

    @Test fun plantFinishesRetreatEvenWhenPlayerApproaches() {
        val cycle = CactusCycle(1f, 3f)
        cycle.update(1.4f, playerNear = true)
        assertEquals(0.5f, cycle.extension, 0.001f)
        cycle.update(0.5f, playerNear = true)
        assertEquals(0f, cycle.extension, 0.001f)
        repeat(300) { cycle.update(1f / 60f, playerNear = true) }
        assertEquals(0f, cycle.extension, 0.001f)
    }

    @Test fun harderTempoEmergesSoonerAndEveryCycleReturnsToCover() {
        val easy = CactusCycle(0.8f)
        val hard = CactusCycle(1.3f)
        easy.update(1.5f, false)
        hard.update(1.5f, false)
        assertEquals(0f, easy.extension, 0.001f)
        assertTrue(hard.extension > 0f)
        val normal = CactusCycle(1f)
        normal.update(5.2f, false)
        assertEquals(0f, normal.extension, 0.001f)
    }
}
