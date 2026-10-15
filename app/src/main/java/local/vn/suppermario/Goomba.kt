package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF

class Goomba(
    var x: Float,
    groundY: Float,
    val size: Float = 75f,
    private val minX: Float = 0f,
    private val maxX: Float = 2000f,
    private val bitmapCache: BitmapCache,
    var moveSpeed: Float = 120f
) {
    var y = groundY - size
    var speed = moveSpeed
    var isAlive = true
    var isStomped = false
    private var squishTimer = 0.4f // Thời gian quái bị đè bẹp dí (0.4s)

    private var animTime = 0f

    private val dstRect = RectF()
    private val srcRect = Rect()
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    fun getBounds(): RectF {
        return RectF(x, y, x + size, y + size)
    }

    fun stomp() {
        isStomped = true
        speed = 0f
    }

    fun update(dt: Float) {
        if (!isAlive) return

        if (isStomped) {
            squishTimer -= dt
            if (squishTimer <= 0f) {
                isAlive = false
            }
            return
        }

        x += speed * dt
        if (x <= minX) {
            x = minX
            speed = moveSpeed
        } else if (x + size >= maxX) {
            x = maxX - size
            speed = -moveSpeed
        }

        animTime += dt
    }

    fun draw(canvas: Canvas) {
        if (!isAlive) return

        if (isStomped) {
            // Khi bị giẫm: Vẽ hình bẹp dí sát mặt đất
            val flatHeight = size * 0.4f
            val flatY = y + size - flatHeight
            val currentBitmap = bitmapCache.goombaFlat
            dstRect.set(x, flatY, x + size, flatY + flatHeight)
            srcRect.set(0, 0, currentBitmap.width, currentBitmap.height)
            canvas.drawBitmap(currentBitmap, srcRect, dstRect, spritePaint)
            return
        }

        val animFrame = ((animTime * 6f).toInt() % 2)
        val currentBitmap = if (animFrame == 0) bitmapCache.goombaWalkA else bitmapCache.goombaWalkB
        dstRect.set(x, y, x + size, y + size)
        srcRect.set(0, 0, currentBitmap.width, currentBitmap.height)

        canvas.drawBitmap(currentBitmap, srcRect, dstRect, spritePaint)
    }
}
