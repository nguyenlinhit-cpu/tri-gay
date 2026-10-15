package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.sin

class RockBird(private val data: BirdData, private val groundY: Float, private val tempo: Float) {
    var x = data.x
        private set
    var isAlive = true
    private var direction = 1f
    private var clock = 0f
    private var dropTimer = 1.8f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val wing = Path()
    private val bounds = RectF()
    val y: Float get() = (groundY - data.height).coerceAtLeast(110f) + sin(clock * 3f) * 12f

    fun getBounds(): RectF {
        bounds.set(x + 8f, y + 10f, x + 64f, y + 49f)
        return bounds
    }

    fun update(dt: Float, playerCenterX: Float): Boolean {
        if (!isAlive) return false
        clock += dt
        x += direction * 95f * tempo * dt
        if (x < data.minX || x > data.maxX) {
            x = x.coerceIn(data.minX, data.maxX)
            direction = -direction
        }
        dropTimer = (dropTimer - dt).coerceAtLeast(0f)
        if (dropTimer <= 0f && abs(playerCenterX - (x + 36f)) < 190f) {
            dropTimer = 2.8f / tempo
            return true
        }
        return false
    }

    fun draw(canvas: Canvas) {
        if (!isAlive) return
        canvas.save()
        canvas.translate(x + 36f, y + 30f)
        canvas.scale(direction, 1f)
        // A visible stone in the claws warns of the next drop.
        if (dropTimer < 0.8f) {
            paint.color = Color.rgb(107, 116, 131)
            canvas.drawOval(-11f, 20f, 12f, 39f, paint)
            paint.color = Color.rgb(240, 183, 60)
            canvas.drawRect(-9f, 16f, -5f, 27f, paint)
            canvas.drawRect(5f, 16f, 9f, 27f, paint)
        }
        paint.color = Color.rgb(53, 47, 87)
        canvas.drawOval(-30f, -15f, 29f, 20f, paint)
        wing.reset()
        wing.moveTo(-22f, 2f)
        wing.lineTo(-48f, -12f - sin(clock * 12f) * 22f)
        wing.quadTo(-6f, -34f, 12f, 0f)
        wing.close()
        paint.color = Color.rgb(126, 106, 177)
        canvas.drawPath(wing, paint)
        paint.color = Color.rgb(239, 183, 62)
        wing.reset()
        wing.moveTo(26f, -5f)
        wing.lineTo(43f, 2f)
        wing.lineTo(26f, 8f)
        wing.close()
        canvas.drawPath(wing, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(20f, -6f, 7f, paint)
        paint.color = Color.rgb(26, 29, 48)
        canvas.drawCircle(23f, -6f, 3f, paint)
        canvas.restore()
    }
}
