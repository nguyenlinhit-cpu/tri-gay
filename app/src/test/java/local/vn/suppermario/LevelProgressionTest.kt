package local.vn.suppermario

import kotlin.math.sqrt
import org.junit.Assert.*
import org.junit.Test

class LevelProgressionTest {
    private data class Surface(val left: Float, val right: Float, val height: Float)

    // Start from rest, keep jump held, require 20px of landing overlap. The constants are
    // the same ones Mario uses; no mushroom, star, damage boost or midair jump is assumed.
    private fun canJump(from: Surface, to: Surface, landingOverlap: Float): Boolean {
        val rise = to.height - from.height
        val discriminant = JumpPhysics.JUMP_SPEED * JumpPhysics.JUMP_SPEED - 2f * JumpPhysics.GRAVITY * rise
        if (discriminant < 0f) return false
        val flightTime = (JumpPhysics.JUMP_SPEED + sqrt(discriminant)) / JumpPhysics.GRAVITY
        val accelerationTime = JumpPhysics.RUN_SPEED / JumpPhysics.ACCELERATION
        val travel = if (flightTime < accelerationTime) {
            0.5f * JumpPhysics.ACCELERATION * flightTime * flightTime
        } else {
            JumpPhysics.RUN_SPEED * (flightTime - accelerationTime / 2f)
        }
        // Reserve 15px for discrete frame timing. River banks require the entire body
        // on land because ground-level water overlap counts as a fall.
        return to.left > from.left && to.left + landingOverlap <= from.right + travel - 15f
    }

    @Test fun allRiverCrossingsHaveReachableLandingsInEveryMode() {
        for (difficulty in Difficulty.entries) {
            for (level in LevelData.getLevels(difficulty)) {
                for (river in level.rivers) {
                    val portal = level.pipes.firstOrNull {
                        it.x + 110f <= river.startX && it.teleportTargetX != null &&
                            it.teleportTargetX >= river.endX
                    }
                    if (portal != null) {
                        assertTrue(level.pipes.any { it.x == portal.teleportTargetX })
                        continue
                    }
                    // Check both extremes of moving platforms as well as their midpoint.
                    for (phase in listOf(0f, 0.5f, 1f)) {
                        val surfaces = mutableListOf(Surface(river.startX - 200f, river.startX, 0f))
                        surfaces += level.platforms.filter { it.x < river.endX && it.x + it.width > river.startX }
                            .map {
                                val x = if (it.speed == 0f) it.x else it.minX + (it.maxX - it.width - it.minX) * phase
                                Surface(x, x + it.width, it.yOffset)
                            }
                        surfaces += Surface(river.endX, river.endX + 200f, 0f)
                        surfaces.sortBy { it.left }
                        val reachable = mutableSetOf(0)
                        for (target in 1 until surfaces.size) {
                            val overlap = if (target == surfaces.lastIndex) 80f else 20f
                            if (reachable.any { canJump(surfaces[it], surfaces[target], overlap) }) reachable.add(target)
                        }
                        assertTrue("${level.worldName} $difficulty river ${river.startX} phase $phase has no route",
                            surfaces.lastIndex in reachable)
                    }
                }
            }
        }
    }

    @Test fun enemiesAndTeleportExitsHaveSolidGroundAndRoomToReact() {
        for (difficulty in Difficulty.entries) {
            val levels = LevelData.getLevels(difficulty)
            assertEquals(5, levels.size)
            for (level in levels) {
                assertTrue(level.challenge.isNotBlank())
                assertTrue(level.timeLimit >= 160)
                for (pipe in level.pipes) {
                    assertTrue(pipe.height <= 200f)
                    assertFalse(level.rivers.any { pipe.x < it.endX && pipe.x + 110f > it.startX })
                    pipe.teleportTargetX?.let { target ->
                        val exit = level.pipes.single { it.x == target }
                        assertNull(exit.teleportTargetX)
                        assertFalse(level.goombas.any { it.minX < target + 230f && it.maxX > target - 120f })
                    }
                }
                for (enemy in level.goombas) {
                    assertTrue(enemy.x >= enemy.minX && enemy.x + 70f <= enemy.maxX)
                    assertFalse(level.rivers.any { enemy.minX < it.endX && enemy.maxX > it.startX })
                    assertFalse(level.pipes.any { enemy.minX < it.x + 110f && enemy.maxX > it.x })
                }
            }
        }
    }

    @Test fun difficultyChangesCombatTimingAndTimeBudgetWithoutChangingJumpPhysics() {
        val easy = ChallengeRules.forDifficulty(Difficulty.EASY)
        val medium = ChallengeRules.forDifficulty(Difficulty.MEDIUM)
        val hard = ChallengeRules.forDifficulty(Difficulty.HARD)
        assertTrue(easy.enemySpeed < medium.enemySpeed && medium.enemySpeed < hard.enemySpeed)
        assertTrue(easy.hazardTempo < medium.hazardTempo && medium.hazardTempo < hard.hazardTempo)
        assertTrue(easy.bossHp < medium.bossHp && medium.bossHp < hard.bossHp)
        assertTrue(easy.bossShotInterval > medium.bossShotInterval && medium.bossShotInterval > hard.bossShotInterval)
        assertTrue(easy.timeScale > medium.timeScale && medium.timeScale > hard.timeScale)
        val finale = LevelData.getLevels(Difficulty.MEDIUM).last()
        assertTrue(finale.isBossLevel)
        assertTrue(finale.arenaStartX < finale.bossX && finale.bossX < finale.flagpoleX - 230f)
        assertTrue(finale.blocks.any { it.weaponType != null && it.x > 2900f && it.x < finale.arenaStartX })
    }
}
