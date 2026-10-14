package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF

enum class BlockType {
    BRICK,
    QUESTION
}

class Block(
    val x: Float,
    val y: Float,
    val size: Float = 80f,
    val type: BlockType = BlockType.QUESTION,
    private val bitmapCache: BitmapCache,
    val powerUpType: PowerUpType? = null,
    val weaponType: WeaponType? = null
) {
    val bounds = RectF(x, y, x + size, y + size)
    var isHit = false

    private val srcRect = Rect()

    fun draw(canvas: Canvas) {
        val currentBitmap = when {
            isHit -> bitmapCache.blockEmpty
            type == BlockType.QUESTION -> bitmapCache.blockQuestion
            else -> bitmapCache.blockBrick
        }

        srcRect.set(0, 0, currentBitmap.width, currentBitmap.height)
        canvas.drawBitmap(currentBitmap, srcRect, bounds, null)
    }
}
