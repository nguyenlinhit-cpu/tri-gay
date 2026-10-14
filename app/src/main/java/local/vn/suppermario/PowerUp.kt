package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF

enum class PowerUpType {
    MUSHROOM,
    STAR,
    COIN
}

class PowerUp(
    var x: Float,
    var y: Float,
    val type: PowerUpType,
    private val bitmapCache: BitmapCache
) {
    val size = 60f
    var isCollected = false
    private var speedX = if (type == PowerUpType.MUSHROOM || type == PowerUpType.STAR) 120f else 0f
    private var popY = y - 60f
    private var popping = true

    private val dstRect = RectF()
    private val srcRect = Rect()

    fun getBounds(): RectF {
        return RectF(x, y, x + size, y + size)
    }

    fun update(dt: Float) {
        if (isCollected) return

        if (popping) {
            y -= 120f * dt
            if (y <= popY) {
                y = popY
                popping = false
            }
        } else {
            x += speedX * dt
        }
    }

    fun draw(canvas: Canvas) {
        if (isCollected) return

        val bitmap = when (type) {
            PowerUpType.MUSHROOM -> bitmapCache.mushroomRed
            PowerUpType.STAR -> bitmapCache.star
            PowerUpType.COIN -> bitmapCache.coinGold
        }

        dstRect.set(x, y, x + size, y + size)
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, dstRect, null)
    }
}
