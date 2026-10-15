package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class Pipe(
    val x: Float,
    val groundY: Float,
    val pipeWidth: Float = 110f,
    val pipeHeight: Float = 160f,
    val teleportTargetX: Float? = null
) {
    val topY = groundY - pipeHeight
    val bounds = RectF(x, topY, x + pipeWidth, groundY)

    private val pipePaint = Paint().apply {
        color = Color.rgb(0, 180, 0)
        style = Paint.Style.FILL
    }
    private val darkPipePaint = Paint().apply {
        color = Color.rgb(0, 120, 0)
        style = Paint.Style.FILL
    }
    var isTeleportExit = false
    private val portalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(255, 223, 95)
        textSize = 30f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val borderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    fun draw(canvas: Canvas) {
        val bodyRect = RectF(x + 10f, topY + 30f, x + pipeWidth - 10f, groundY)
        canvas.drawRect(bodyRect, pipePaint)
        canvas.drawRect(bodyRect, borderPaint)

        val shadowBody = RectF(x + pipeWidth - 25f, topY + 30f, x + pipeWidth - 10f, groundY)
        canvas.drawRect(shadowBody, darkPipePaint)

        val rimRect = RectF(x, topY, x + pipeWidth, topY + 30f)
        canvas.drawRect(rimRect, pipePaint)
        canvas.drawRect(rimRect, borderPaint)

        val shadowRim = RectF(x + pipeWidth - 20f, topY, x + pipeWidth, topY + 30f)
        canvas.drawRect(shadowRim, darkPipePaint)
        if (teleportTargetX != null || isTeleportExit) {
            canvas.drawText(if (teleportTargetX != null) "»" else "✓",
                bounds.centerX(), topY + 64f, portalPaint)
        }
    }
}
