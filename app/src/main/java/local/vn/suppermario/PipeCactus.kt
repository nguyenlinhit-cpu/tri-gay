package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlin.math.abs

/** The pipe rim masks the plant as it rises and retreats. */
class PipeCactus(private val pipe: Pipe, private val tempo: Float, phase: Float) {
    private val cycle = CactusCycle(tempo, phase)
    private val extension: Float get() = cycle.extension
    var isAlive = true
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bounds = RectF()

    fun update(dt: Float, playerCenterX: Float) {
        if (!isAlive) return
        // Never start rising underneath someone waiting on the rim.
        cycle.update(dt, abs(playerCenterX - pipe.bounds.centerX()) < 110f)
    }

    fun getBounds(): RectF {
        val cx = pipe.bounds.centerX()
        bounds.set(cx - 29f, pipe.topY - 104f * extension, cx + 29f, pipe.topY)
        return bounds
    }

    fun canHit() = isAlive && extension > 0.12f

    fun draw(canvas: Canvas) {
        if (!isAlive || extension <= 0f) return
        val cx = pipe.bounds.centerX()
        val top = pipe.topY - 104f * extension
        canvas.save()
        canvas.clipRect(pipe.x, pipe.topY - 120f, pipe.x + pipe.pipeWidth, pipe.topY)
        paint.color = Color.rgb(17, 76, 48)
        canvas.drawRoundRect(cx - 24f, top, cx + 24f, top + 122f, 22f, 22f, paint)
        paint.color = Color.rgb(66, 191, 93)
        canvas.drawRoundRect(cx - 19f, top + 5f, cx + 17f, top + 122f, 18f, 18f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 13f
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(cx - 20f, top + 67f, cx - 37f, top + 67f, paint)
        canvas.drawLine(cx - 37f, top + 67f, cx - 37f, top + 40f, paint)
        canvas.drawLine(cx + 20f, top + 53f, cx + 36f, top + 53f, paint)
        canvas.drawLine(cx + 36f, top + 53f, cx + 36f, top + 29f, paint)
        paint.color = Color.rgb(243, 240, 183)
        paint.strokeWidth = 3f
        for (i in 0..3) {
            val sy = top + 18f + i * 23f
            canvas.drawLine(cx - 21f, sy, cx - 29f, sy - 5f, paint)
            canvas.drawLine(cx + 20f, sy + 6f, cx + 28f, sy + 1f, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(19, 43, 36)
        canvas.drawCircle(cx - 8f, top + 25f, 3f, paint)
        canvas.drawCircle(cx + 8f, top + 25f, 3f, paint)
        canvas.restore()
    }
}
