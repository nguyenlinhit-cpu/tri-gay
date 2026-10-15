package local.vn.suppermario

/** Difficulty affects encounter timing as well as lives. All modes use the same jump physics. */
data class ChallengeRules(
    val enemySpeed: Float,
    val hazardTempo: Float,
    val platformSpeed: Float,
    val timeScale: Float,
    val bossHp: Int,
    val bossShotInterval: Float,
    val bossShotSpeed: Float
) {
    companion object {
        fun forDifficulty(difficulty: Difficulty) = when (difficulty) {
            Difficulty.EASY -> ChallengeRules(0.95f, 1f, 0.9f, 1.15f, 10, 1.7f, 370f)
            Difficulty.MEDIUM -> ChallengeRules(1.15f, 1.2f, 1f, 1f, 14, 1.4f, 440f)
            Difficulty.HARD -> ChallengeRules(1.4f, 1.5f, 1.18f, 0.85f, 18, 1.15f, 510f)
        }
    }
}
