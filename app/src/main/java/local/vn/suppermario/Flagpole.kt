package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

class Flagpole(val x: Float, val groundY: Float) {
    val height = 320f
    val topY = groundY - height
    val bounds = RectF(x, topY, x + 20f, groundY)

    private val polePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val ballPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
    }
    private val flagPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    fun draw(canvas: Canvas) {
        // Cột cờ
        canvas.drawRect(bounds, polePaint)

        // Quả cầu vàng trên đỉnh
        canvas.drawCircle(x + 10f, topY, 15f, ballPaint)

        // Lá cờ màu đỏ
        val flagRect = RectF(x - 60f, topY + 20f, x, topY + 80f)
        canvas.drawRect(flagRect, flagPaint)
    }
}
