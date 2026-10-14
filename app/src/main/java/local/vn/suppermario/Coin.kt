package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF

class Coin(val x: Float, val y: Float, private val bitmapCache: BitmapCache) {
    val size = 45f
    var isCollected = false

    private val bounds = RectF(x - size / 2f, y - size / 2f, x + size / 2f, y + size / 2f)
    private val srcRect = Rect()

    fun getBounds(): RectF {
        return bounds
    }

    fun draw(canvas: Canvas) {
        if (isCollected) return

        val bitmap = bitmapCache.coinGold
        srcRect.set(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, bounds, null)
    }
}
