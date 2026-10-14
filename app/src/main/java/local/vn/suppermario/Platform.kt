package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF

class Platform(
    var x: Float,
    var y: Float,
    val width: Float = 140f,
    val height: Float = 40f,
    private val minX: Float = 0f,
    private val maxX: Float = 0f,
    var moveSpeed: Float = 0f,
    val isRock: Boolean = false,
    private val bitmapCache: BitmapCache
) {
    var bounds = RectF(x, y, x + width, y + height)
    var speed = moveSpeed

    private val dstRect = RectF()
    private val srcRect = Rect()

    fun update(dt: Float) {
        if (moveSpeed != 0f) {
            x += speed * dt
            if (x <= minX) {
                x = minX
                speed = moveSpeed
            } else if (x + width >= maxX) {
                x = maxX - width
                speed = -moveSpeed
            }
            bounds.set(x, y, x + width, y + height)
        }
    }

    fun draw(canvas: Canvas) {
        val bitmap = if (isRock) bitmapCache.rock else bitmapCache.blockBrick
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, bounds, null)
    }
}
