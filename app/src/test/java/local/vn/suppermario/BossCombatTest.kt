package local.vn.suppermario

import org.junit.Assert.*
import org.junit.Test

class BossCombatTest {
    @Test fun bossCannotChargeShotsBeforeArenaEntry() {
        val boss = BossCombat(14, 1.4f)
        boss.update(30f, active = false)
        assertFalse(boss.consumeShot())
        boss.update(1.3f, active = true)
        assertFalse(boss.consumeShot())
        boss.update(0.2f, active = true)
        assertTrue(boss.consumeShot())
        assertFalse(boss.consumeShot())
    }

    @Test fun overlappingAttacksDoNotDrainHealthInOneFrame() {
        val boss = BossCombat(14, 1.4f)
        assertTrue(boss.takeDamage(1))
        repeat(100) { assertFalse(boss.takeDamage(1)) }
        assertEquals(13, boss.hp)
        boss.update(0.56f, active = true)
        assertTrue(boss.takeDamage(1))
        assertEquals(12, boss.hp)
    }

    @Test fun halfHealthEnablesFasterSecondPhaseAndDefeatStopsAttacks() {
        val boss = BossCombat(10, 1.7f)
        repeat(5) {
            boss.takeDamage(1)
            boss.update(0.56f, active = true)
        }
        assertTrue(boss.isEnraged)
        assertTrue(boss.shotInterval < 1.7f)
        repeat(5) {
            boss.takeDamage(1)
            boss.update(0.56f, active = true)
        }
        assertTrue(boss.isDefeated)
        assertEquals(0, boss.hp)
        assertFalse(boss.consumeShot())
        assertFalse(boss.takeDamage(1))
    }
}
