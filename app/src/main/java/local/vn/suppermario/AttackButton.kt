package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/** Drawn as geometry so the sword looks identical on every Android font. */
class AttackButton {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val blade = Path()

    fun draw(canvas: Canvas, bounds: RectF, weapon: WeaponType, pressed: Boolean, cooldown: Float) {
        canvas.save()
        canvas.translate(bounds.centerX(), bounds.centerY())
        val scale = minOf(bounds.width(), bounds.height()) / 160f
        canvas.scale(scale, scale)
        if (pressed) canvas.scale(0.94f, 0.94f)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(220, 16, 28, 48)
        canvas.drawCircle(0f, 4f, 76f, paint)
        paint.color = if (pressed) Color.rgb(32, 130, 163) else Color.rgb(25, 65, 93)
        canvas.drawCircle(0f, 0f, 70f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4f
        paint.color = Color.rgb(100, 225, 246)
        canvas.drawCircle(0f, 0f, 70f, paint)
        if (cooldown > 0f) {
            paint.strokeWidth = 7f
            paint.color = Color.rgb(255, 205, 93)
            canvas.drawArc(-76f, -76f, 76f, 76f, -90f, 360f * cooldown, false, paint)
        }
        paint.style = Paint.Style.FILL
        if (weapon == WeaponType.SWORD) {
            blade.reset()
            blade.moveTo(-23f, 4f)
            blade.lineTo(22f, -45f)
            blade.lineTo(39f, -51f)
            blade.lineTo(36f, -32f)
            blade.lineTo(-10f, 17f)
            blade.close()
            paint.color = Color.rgb(222, 247, 255)
            canvas.drawPath(blade, paint)
            paint.strokeWidth = 3f
            paint.color = Color.rgb(87, 180, 214)
            canvas.drawLine(-15f, 8f, 32f, -42f, paint)
            paint.strokeWidth = 10f
            paint.strokeCap = Paint.Cap.ROUND
            paint.color = Color.rgb(255, 200, 77)
            canvas.drawLine(-31f, -1f, -2f, 24f, paint)
            paint.strokeWidth = 11f
            paint.color = Color.rgb(167, 105, 61)
            canvas.drawLine(-22f, 18f, -34f, 31f, paint)
            paint.color = Color.rgb(255, 214, 103)
            canvas.drawCircle(-36f, 33f, 6f, paint)
        } else {
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 46f
            canvas.drawText(if (weapon == WeaponType.GUN) "➤" else "●", 0f, 3f, paint)
        }
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 25f
        paint.isFakeBoldText = true
        val label = when (weapon) {
            WeaponType.SWORD -> "CHÉM"
            WeaponType.GUN -> "BẮN"
            else -> "NÉM"
        }
        canvas.drawText(label, 0f, 56f, paint)
        canvas.restore()
    }
}
