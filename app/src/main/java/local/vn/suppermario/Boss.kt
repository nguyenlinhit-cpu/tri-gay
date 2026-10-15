package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

class Boss(
    var x: Float,
    groundY: Float,
    val width: Float = 140f,
    val height: Float = 140f,
    private val bitmapCache: BitmapCache,
    private val rules: ChallengeRules
) {
    var y = groundY - height
    private val combat = BossCombat(rules.bossHp, rules.bossShotInterval)
    val maxHp get() = combat.maxHp
    val hp get() = combat.hp
    val isDefeated get() = combat.isDefeated
    val isEnraged get() = combat.isEnraged
    val invincibleTimer get() = combat.invincibleTimer
    private val homeX = x
    private var direction = -1f

    private val dstRect = RectF()
    private val srcRect = Rect()
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val hpPaint = Paint().apply {
        color = Color.RED
        textSize = 36f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    fun getBounds(): RectF {
        return RectF(x, y, x + width, y + height)
    }

    fun takeDamage(amount: Int = 1) = combat.takeDamage(amount)

    fun consumeShot() = combat.consumeShot()

    fun update(dt: Float, active: Boolean) {
        combat.update(dt, active)
        if (!active || isDefeated) return
        val speed = if (isEnraged) 175f else 115f
        x += direction * speed * dt
        if (x < homeX - 230f || x > homeX + 230f) {
            x = x.coerceIn(homeX - 230f, homeX + 230f)
            direction = -direction
        }
    }

    fun draw(canvas: Canvas) {
        if (isDefeated) return

        if (invincibleTimer > 0f && (invincibleTimer * 10f).toInt() % 2 == 0) {
            return
        }

        val bitmap = bitmapCache.bossTurtle
        dstRect.set(x, y, x + width, y + height)
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, dstRect, spritePaint)

        hpPaint.color = if (isEnraged) Color.rgb(255, 180, 60) else Color.WHITE
        val hpText = "BOSS $hp/$maxHp" + if (isEnraged) " !" else ""
        canvas.drawText(hpText, x + width / 2f, y - 20f, hpPaint)
    }
}
