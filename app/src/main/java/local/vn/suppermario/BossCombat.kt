package local.vn.suppermario

class BossCombat(val maxHp: Int, private val baseShotInterval: Float) {
    var hp = maxHp
        private set
    var invincibleTimer = 0f
        private set
    private var shootTimer = 0f
    val isDefeated get() = hp <= 0
    val isEnraged get() = hp <= maxHp / 2 && !isDefeated
    val shotInterval get() = baseShotInterval * if (isEnraged) 0.72f else 1f

    fun takeDamage(amount: Int): Boolean {
        if (amount <= 0 || invincibleTimer > 0f || isDefeated) return false
        hp = (hp - amount).coerceAtLeast(0)
        invincibleTimer = 0.55f
        return true
    }

    fun update(dt: Float, active: Boolean) {
        invincibleTimer = (invincibleTimer - dt).coerceAtLeast(0f)
        if (active && !isDefeated) shootTimer += dt
    }

    fun consumeShot(): Boolean {
        if (isDefeated || shootTimer < shotInterval) return false
        shootTimer = 0f
        return true
    }
}
