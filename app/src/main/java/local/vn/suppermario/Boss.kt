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
    private val bitmapCache: BitmapCache
) {
    var y = groundY - height
    var maxHp = 5
    var hp = maxHp
    var isDefeated = false

    var shootTimer = 0f
    var invincibleTimer = 0f

    private val dstRect = RectF()
    private val srcRect = Rect()
    private val hpPaint = Paint().apply {
        color = Color.RED
        textSize = 36f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }

    fun getBounds(): RectF {
        return RectF(x, y, x + width, y + height)
    }

    fun takeDamage(amount: Int = 1) {
        if (invincibleTimer > 0f || isDefeated) return

        hp -= amount
        invincibleTimer = 0.8f
        if (hp <= 0) {
            hp = 0
            isDefeated = true
        }
    }

    fun update(dt: Float) {
        if (isDefeated) return

        if (invincibleTimer > 0f) {
            invincibleTimer -= dt
        }

        shootTimer += dt
    }

    fun draw(canvas: Canvas) {
        if (isDefeated) return

        if (invincibleTimer > 0f && (invincibleTimer * 10f).toInt() % 2 == 0) {
            return
        }

        val bitmap = bitmapCache.bossTurtle
        dstRect.set(x, y, x + width, y + height)
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, dstRect, null)

        val hpText = "BOSS HP: " + "❤️".repeat(hp)
        canvas.drawText(hpText, x + width / 2f, y - 20f, hpPaint)
    }
}
