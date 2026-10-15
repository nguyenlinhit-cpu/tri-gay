package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

enum class WeaponType {
    NONE,
    GUN,
    ROCK_THROW,
    SWORD
}

class Projectile(
    var x: Float,
    var y: Float,
    val speedX: Float,
    val isPlayerBullet: Boolean,
    val isRock: Boolean = false,
    val isSwordWave: Boolean = false,
    private val bitmapCache: BitmapCache,
    var speedY: Float = 0f,
    val gravity: Float = 0f
) {
    val size = if (isSwordWave) 50f else 35f
    var isActive = true

    private val dstRect = RectF()
    private val srcRect = Rect()
    private val swordWavePaint = Paint().apply {
        color = Color.CYAN
        strokeWidth = 12f
        style = Paint.Style.STROKE
    }

    fun getBounds(): RectF {
        return RectF(x, y, x + size, y + size)
    }

    fun update(dt: Float) {
        if (!isActive) return

        speedY += gravity * dt
        y += speedY * dt
        x += speedX * dt
        if (x < -100f || x > 6000f) {
            isActive = false
        }
    }

    fun draw(canvas: Canvas) {
        if (!isActive) return

        if (isSwordWave) {
            val bounds = getBounds()
            canvas.drawArc(bounds, -60f, 120f, false, swordWavePaint)
        } else {
            val bitmap = if (isRock) bitmapCache.rock else bitmapCache.fireball
            dstRect.set(x, y, x + size, y + size)
            srcRect.set(0, 0, bitmap.width, bitmap.height)
            canvas.drawBitmap(bitmap, srcRect, dstRect, null)
        }
    }
}
