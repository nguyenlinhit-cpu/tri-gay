package local.vn.suppermario

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import kotlin.math.abs

class Mario(
    private var worldWidth: Float,
    private var screenHeight: Float,
    private val bitmapCache: BitmapCache,
    var characterType: CharacterType = CharacterType.BEIGE
) {
    var weapon = WeaponType.NONE

    var isBig = false
        private set
    var isSuperSpeed = false
        private set

    var speedTimer = 0f
    var invincibleTimer = 0f

    var size = 80f
        get() = if (isBig) 115f else 80f

    val startX = 100f
    val startY = screenHeight - size - 100f

    var x = startX
    var y = startY

    var velocityY = 0f
    private val gravity = JumpPhysics.GRAVITY
    private val jumpStrength = -JumpPhysics.JUMP_SPEED

    var velocityX = 0f
    private val baseSpeed = JumpPhysics.RUN_SPEED
    private val accel = JumpPhysics.ACCELERATION
    private val friction = 1800f

    var isGrounded = false
        private set

    var facingLeft = false
    private var animTime = 0f

    private val dstRect = RectF()
    private val srcRect = Rect()
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val auraPaint = Paint().apply {
        color = Color.YELLOW
        alpha = 150
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val swordPaint = Paint().apply {
        color = Color.GRAY
        strokeWidth = 8f
        style = Paint.Style.STROKE
    }

    fun reset() {
        x = startX
        y = startY
        velocityX = 0f
        velocityY = 0f
        isGrounded = true
        facingLeft = false
        isBig = false
        isSuperSpeed = false
        speedTimer = 0f
        invincibleTimer = 0f
        weapon = WeaponType.NONE
    }

    fun growBig() {
        isBig = true
        y -= 35f
    }

    fun shrinkToNormal() {
        isBig = false
        invincibleTimer = 1.2f
    }

    fun enableSuperSpeed() {
        isSuperSpeed = true
        speedTimer = 10.0f
    }

    fun getBounds(): RectF {
        return RectF(x, y, x + size, y + size)
    }

    fun updateX(dt: Float, isLeft: Boolean, isRight: Boolean) {
        if (speedTimer > 0f) {
            speedTimer -= dt
            if (speedTimer <= 0f) {
                isSuperSpeed = false
            }
        }

        if (invincibleTimer > 0f) {
            invincibleTimer -= dt
        }

        val currentMaxSpeed = if (isSuperSpeed) baseSpeed * 1.5f else baseSpeed

        if (isLeft && !isRight) {
            velocityX = (velocityX - accel * dt).coerceAtLeast(-currentMaxSpeed)
            facingLeft = true
        } else if (isRight && !isLeft) {
            velocityX = (velocityX + accel * dt).coerceAtMost(currentMaxSpeed)
            facingLeft = false
        } else {
            if (velocityX > 0) {
                velocityX = (velocityX - friction * dt).coerceAtLeast(0f)
            } else if (velocityX < 0) {
                velocityX = (velocityX + friction * dt).coerceAtMost(0f)
            }
        }

        x += velocityX * dt
        if (x < 0f) {
            x = 0f
            velocityX = 0f
        }
        if (x > worldWidth - size) {
            x = worldWidth - size
            velocityX = 0f
        }

        if (abs(velocityX) > 20f && isGrounded) {
            animTime += dt
        } else {
            animTime = 0f
        }
    }

    fun updateY(dt: Float) {
        isGrounded = false
        velocityY += gravity * dt
        y += velocityY * dt

        val groundLevel = screenHeight - size - 100f
        if (y >= groundLevel) {
            y = groundLevel
            velocityY = 0f
            isGrounded = true
        }
    }

    fun jump() {
        if (isGrounded) {
            val currentJumpStrength = if (isBig) jumpStrength * 1.08f else jumpStrength
            velocityY = currentJumpStrength
            isGrounded = false
        }
    }

    fun releaseJump() {
        if (velocityY < -200f) {
            velocityY *= 0.45f
        }
    }

    fun teleportTo(targetX: Float, surfaceTop: Float) {
        x = targetX + 15f
        y = surfaceTop - size - 12f
        velocityX = 0f
        velocityY = 0f
        isGrounded = false
    }

    fun setGroundedOn(topY: Float) {
        y = topY - size
        velocityY = 0f
        isGrounded = true
    }

    fun draw(canvas: Canvas) {
        if (invincibleTimer > 0f && (invincibleTimer * 10f).toInt() % 2 == 0) {
            return
        }

        val (idleBmp, walkABmp, walkBBmp, jumpBmp) = when (characterType) {
            CharacterType.BEIGE -> Quadruple(bitmapCache.marioIdle, bitmapCache.marioWalkA, bitmapCache.marioWalkB, bitmapCache.marioJump)
            CharacterType.PINK -> Quadruple(bitmapCache.pinkIdle, bitmapCache.pinkWalkA, bitmapCache.pinkWalkB, bitmapCache.pinkJump)
            CharacterType.GREEN -> Quadruple(bitmapCache.greenIdle, bitmapCache.greenWalkA, bitmapCache.greenWalkB, bitmapCache.greenJump)
        }

        val animFrame = ((animTime * 10f).toInt() % 2)
        val currentBitmap = when {
            !isGrounded -> jumpBmp
            abs(velocityX) > 20f -> if (animFrame == 0) walkABmp else walkBBmp
            else -> idleBmp
        }

        dstRect.set(x, y, x + size, y + size)
        srcRect.set(0, 0, currentBitmap.width, currentBitmap.height)

        canvas.save()
        if (facingLeft) {
            canvas.scale(-1f, 1f, x + size / 2f, y + size / 2f)
        }
        canvas.drawBitmap(currentBitmap, srcRect, dstRect, spritePaint)

        if (isSuperSpeed) {
            canvas.drawCircle(x + size / 2f, y + size / 2f, size / 2f + 10f, auraPaint)
        }

        // Vẽ Cây Kiếm trên tay nếu đang cầm Kiếm
        if (weapon == WeaponType.SWORD) {
            val swordX = x + size
            canvas.drawLine(swordX, y + size / 2f, swordX + 35f, y + 10f, swordPaint)
        }

        canvas.restore()
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
