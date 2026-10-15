package local.vn.suppermario

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView

enum class GameState {
    SPLASH_SCREEN,
    START_MENU,
    LEVEL_TRANSITION,
    PLAYING,
    GAME_OVER,
    ALL_CLEAR
}

class GameView(context: Context) : SurfaceView(context), Runnable, SurfaceHolder.Callback {
    @Volatile
    private var isPlaying = false
    private var activityResumed = false
    private var surfaceReady = false
    private var gameThread: Thread? = null

    // Cache tài nguyên & Âm thanh
    private var bitmapCache: BitmapCache? = null
    private val soundManager = SoundManager(context)

    private var scaledBgClouds: Bitmap? = null
    private var scaledBgHills: Bitmap? = null

    // Trạng thái Game
    private var gameState = GameState.SPLASH_SCREEN
    private var selectedCharacter = CharacterType.BEIGE
    private var selectedDifficulty = Difficulty.MEDIUM

    // Quản lý 5 Màn chơi
    private var levelsList = listOf<LevelData>()
    private var currentLevelIndex = 0
    private var currentLevelData: LevelData? = null
    private var levelTransitionTimer = 0f

    // Biến đếm nhấp nháy cho Màn hình Chào (Splash Screen)
    private var splashFlashTimer = 0f

    // Chiều rộng bản đồ (5500px) & Camera
    private val worldWidth = 5500f
    private var cameraX = 0f

    // Thời gian đếm lùi
    private var remainingTime = 400
    private var timeAccumulator = 0f

    // Đối tượng game
    private var mario: Mario? = null
    private var boss: Boss? = null
    private var arenaActive = false
    private var flagpole: Flagpole? = null

    private val pipes = mutableListOf<Pipe>()
    private val cacti = mutableListOf<PipeCactus>()
    private val birds = mutableListOf<RockBird>()
    private val blocks = mutableListOf<Block>()
    private val coins = mutableListOf<Coin>()
    private val goombas = mutableListOf<Goomba>()
    private val rivers = mutableListOf<River>()
    private val platforms = mutableListOf<Platform>()
    private val powerUps = mutableListOf<PowerUp>()
    private val projectiles = mutableListOf<Projectile>()

    // Điểm số & Mạng
    private var score = 0
    private var coinCount = 0
    private var lives = 3

    // Vị trí các nút cảm ứng di chuyển & Nút TẤN CÔNG
    private val leftBtn = RectF()
    private val rightBtn = RectF()
    private val jumpBtn = RectF()
    private val attackBtn = RectF()

    private val leftTouchArea = RectF()
    private val rightTouchArea = RectF()
    private val jumpTouchArea = RectF()
    private val attackTouchArea = RectF()

    // Vị trí nút bấm Menu
    private val charBeigeBtn = RectF()
    private val charPinkBtn = RectF()
    private val charGreenBtn = RectF()

    private val diffEasyBtn = RectF()
    private val diffMedBtn = RectF()
    private val diffHardBtn = RectF()

    private val startGameBtn = RectF()

    // Trạng thái phím bấm
    private var isLeftPressed = false
    private var isRightPressed = false
    private var isJumpPressed = false
    private var autoGunTimer = 0f
    private var attackCooldown = 0f
    private var attackDuration = 0.38f
    private var isAttackPressed = false
    private val attackButtonRenderer = AttackButton()

    // Cọ vẽ
    private val spritePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val btnNormalPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 200
    }
    private val btnPressedPaint = Paint().apply {
        color = Color.rgb(230, 40, 40)
        alpha = 240
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 55f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val splashTitlePaint = Paint().apply {
        color = Color.rgb(255, 215, 0)
        textSize = 85f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val splashSubPaint = Paint().apply {
        color = Color.WHITE
        textSize = 42f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val retroHudLabelPaint = Paint().apply {
        color = Color.WHITE
        textSize = 28f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val retroHudValuePaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val attackGatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(200, 255, 130, 55)
    }
    private val overlayPaint = Paint().apply {
        color = Color.BLACK
        alpha = 220
    }
    private val titlePaint = Paint().apply {
        color = Color.rgb(255, 215, 0)
        textSize = 68f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val sectionPaint = Paint().apply {
        color = Color.WHITE
        textSize = 32f
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val cardNormalPaint = Paint().apply {
        color = Color.rgb(45, 55, 85)
        style = Paint.Style.FILL
    }
    private val cardSelectedPaint = Paint().apply {
        color = Color.rgb(0, 180, 90)
        style = Paint.Style.FILL
    }
    private val cardBorderPaint = Paint().apply {
        color = Color.rgb(255, 215, 0)
        style = Paint.Style.STROKE
        strokeWidth = 5f
    }
    private val startBtnPaint = Paint().apply {
        color = Color.rgb(235, 90, 0)
        style = Paint.Style.FILL
    }

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (bitmapCache == null) {
            bitmapCache = BitmapCache(context)
        }
        surfaceReady = true
        startGameThread()
    }

    fun resume() {
        activityResumed = true
        startGameThread()
    }

    private fun startGameThread() {
        if (activityResumed && surfaceReady && !isPlaying) {
            isPlaying = true
            gameThread = Thread(this, "MarioGameLoop").apply { start() }
        }
    }

    fun pause() {
        activityResumed = false
        stopGameThread()
        isLeftPressed = false
        isRightPressed = false
        isJumpPressed = false
        isAttackPressed = false
    }

    private fun stopGameThread() {
        isPlaying = false
        val thread = gameThread ?: return
        thread.interrupt()
        var interrupted = false
        while (thread.isAlive) {
            try {
                thread.join()
            } catch (_: InterruptedException) {
                interrupted = true
            }
        }
        gameThread = null
        if (interrupted) Thread.currentThread().interrupt()
    }

    fun release() {
        pause()
        soundManager.release()
    }

    private fun updateMenuBounds(wF: Float, hF: Float) {
        val cache = bitmapCache ?: return

        if (scaledBgClouds?.width != width || scaledBgClouds?.height != (height * 0.5f).toInt()) {
            scaledBgClouds = Bitmap.createScaledBitmap(cache.bgClouds, width, (height * 0.5f).toInt(), true)
            scaledBgHills = Bitmap.createScaledBitmap(cache.bgHills, width, (height * 0.5f).toInt(), true)
        }

        val cardW = 190f
        val cardH = 150f
        val charY = hF * 0.28f
        val startCharX = (wF - (cardW * 3 + 30f * 2)) / 2f

        charBeigeBtn.set(startCharX, charY, startCharX + cardW, charY + cardH)
        charPinkBtn.set(startCharX + cardW + 30f, charY, startCharX + cardW * 2 + 30f, charY + cardH)
        charGreenBtn.set(startCharX + (cardW + 30f) * 2, charY, startCharX + cardW * 3 + 60f, charY + cardH)

        val diffW = 220f
        val diffH = 85f
        val diffY = hF * 0.58f
        val startDiffX = (wF - (diffW * 3 + 25f * 2)) / 2f

        diffEasyBtn.set(startDiffX, diffY, startDiffX + diffW, diffY + diffH)
        diffMedBtn.set(startDiffX + diffW + 25f, diffY, startDiffX + diffW * 2 + 25f, diffY + diffH)
        diffHardBtn.set(startDiffX + (diffW + 25f) * 2, diffY, startDiffX + diffW * 3 + 50f, diffY + diffH)

        val startW = 400f
        val startH = 95f
        val startY = hF * 0.79f
        startGameBtn.set((wF - startW) / 2f, startY, (wF + startW) / 2f, startY + startH)

        val btnHeight = 160f
        val padding = 35f
        val bottomY = hF - padding - btnHeight

        val squareBtnWidth = 160f
        leftBtn.set(padding, bottomY, padding + squareBtnWidth, bottomY + btnHeight)
        rightBtn.set(padding + squareBtnWidth + 25f, bottomY, padding + squareBtnWidth * 2 + 25f, bottomY + btnHeight)

        val spaceBtnWidth = 240f
        jumpBtn.set(wF - padding - spaceBtnWidth, bottomY, wF - padding, bottomY + btnHeight)

        val attackW = 160f
        attackBtn.set(jumpBtn.left - attackW - 20f, bottomY, jumpBtn.left - 20f, bottomY + btnHeight)

        leftTouchArea.set(leftBtn.left - 20f, leftBtn.top - 20f, leftBtn.right + 12f, leftBtn.bottom + 20f)
        rightTouchArea.set(rightBtn.left - 12f, rightBtn.top - 20f, rightBtn.right + 20f, rightBtn.bottom + 20f)
        jumpTouchArea.set(jumpBtn.left - 8f, jumpBtn.top - 20f, jumpBtn.right + 20f, jumpBtn.bottom + 20f)
        attackTouchArea.set(attackBtn.left - 20f, attackBtn.top - 20f, attackBtn.right + 8f, attackBtn.bottom + 20f)
    }

    private fun startNewGame() {
        score = 0
        coinCount = 0
        lives = when (selectedDifficulty) {
            Difficulty.EASY -> 5
            Difficulty.MEDIUM -> 3
            Difficulty.HARD -> 1
        }
        levelsList = LevelData.getLevels(selectedDifficulty)
        currentLevelIndex = 0
        loadLevel(currentLevelIndex)
    }

    private fun loadLevel(levelIndex: Int) {
        val cache = bitmapCache ?: return
        if (levelIndex >= levelsList.size) return

        val level = levelsList[levelIndex]
        currentLevelData = level

        mario = Mario(worldWidth, height.toFloat(), cache, selectedCharacter)
        cameraX = 0f
        remainingTime = level.timeLimit
        timeAccumulator = 0f
        autoGunTimer = 0f
        attackCooldown = 0f
        isAttackPressed = false
        isLeftPressed = false
        isRightPressed = false
        isJumpPressed = false

        val groundY = height - 100f
        flagpole = Flagpole(level.flagpoleX, groundY)

        arenaActive = false
        boss = if (level.isBossLevel) Boss(level.bossX, groundY, 140f, 140f, cache,
            ChallengeRules.forDifficulty(selectedDifficulty)) else null

        rivers.clear()
        for (rData in level.rivers) {
            rivers.add(River(rData.startX, rData.endX, groundY, cache))
        }

        platforms.clear()
        for (plData in level.platforms) {
            platforms.add(
                Platform(
                    x = plData.x,
                    y = groundY - plData.yOffset,
                    width = plData.width,
                    minX = plData.minX,
                    maxX = plData.maxX,
                    moveSpeed = plData.speed,
                    isRock = plData.isRock,
                    bitmapCache = cache
                )
            )
        }

        pipes.clear()
        for (pData in level.pipes) {
            pipes.add(Pipe(pData.x, groundY, 110f, pData.height, pData.teleportTargetX))
        }

        val hazardTempo = ChallengeRules.forDifficulty(selectedDifficulty).hazardTempo
        cacti.clear()
        val teleportExits = level.pipes.mapNotNull { it.teleportTargetX }.toSet()
        for ((index, pipe) in pipes.withIndex()) {
            pipe.isTeleportExit = pipe.x in teleportExits
            if (pipe.teleportTargetX == null && pipe.x !in teleportExits) {
                cacti.add(PipeCactus(pipe, hazardTempo, index * 0.37f))
            }
        }
        birds.clear()
        for (bird in level.birds) birds.add(RockBird(bird, groundY, hazardTempo))

        blocks.clear()
        for (bData in level.blocks) {
            blocks.add(Block(bData.x, groundY - bData.yOffset, 75f, bData.type, cache, bData.powerUpType, bData.weaponType))
        }

        coins.clear()
        for (cData in level.coins) {
            coins.add(Coin(cData.x, groundY - cData.yOffset, cache))
        }

        goombas.clear()
        for (gData in level.goombas) {
            goombas.add(Goomba(gData.x, groundY, 70f, gData.minX, gData.maxX, cache, gData.speed))
        }

        powerUps.clear()
        projectiles.clear()

        levelTransitionTimer = 3.0f
        gameState = GameState.LEVEL_TRANSITION
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        stopGameThread()
    }

    override fun run() {
        var lastTime = System.nanoTime()
        val targetNanos = 16_666_666L

        while (isPlaying) {
            val now = System.nanoTime()
            val elapsed = now - lastTime

            if (elapsed >= targetNanos) {
                val dt = (elapsed / 1_000_000_000f).coerceIn(0.005f, 0.033f)
                lastTime = now

                splashFlashTimer += dt

                if (gameState == GameState.LEVEL_TRANSITION) {
                    levelTransitionTimer -= dt
                    if (levelTransitionTimer <= 0f) {
                        gameState = GameState.PLAYING
                    }
                } else if (gameState == GameState.PLAYING) {
                    updateGame(dt)
                }
                draw()
            } else {
                val sleepMs = ((targetNanos - elapsed) / 1_000_000L).coerceAtLeast(1)
                try {
                    Thread.sleep(sleepMs)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                }
            }
        }
    }

    private fun updateGame(dt: Float) {
        val m = mario ?: return
        val cache = bitmapCache ?: return

        attackCooldown = (attackCooldown - dt).coerceAtLeast(0f)
        timeAccumulator += dt
        if (timeAccumulator >= 1.0f) {
            timeAccumulator -= 1.0f
            remainingTime--
            if (remainingTime <= 0) {
                handleLifeLost()
                return
            }
        }

        if (m.weapon == WeaponType.GUN) {
            autoGunTimer += dt
            if (autoGunTimer >= 4.0f) {
                autoGunTimer = 0f
                performAttack()
            }
        }

        val viewLeft = cameraX - 100f
        val viewRight = cameraX + width + 100f

        for (plat in platforms) {
            val oldX = plat.x
            plat.update(dt)

            if (m.isGrounded && m.y + m.size >= plat.y - 10f && m.y + m.size <= plat.y + 15f) {
                if (m.x + m.size > oldX && m.x < oldX + plat.width) {
                    m.x += (plat.x - oldX)
                }
            }
        }

        m.updateX(dt, isLeftPressed, isRightPressed)
        var marioBounds = m.getBounds()

        for (pipe in pipes) {
            if (pipe.bounds.right >= viewLeft && pipe.bounds.left <= viewRight) {
                if (RectF.intersects(marioBounds, pipe.bounds)) {
                    if (m.x < pipe.x) m.x = pipe.x - m.size
                    else if (m.x > pipe.x) m.x = pipe.bounds.right
                }
            }
        }
        for (block in blocks) {
            if (block.bounds.right >= viewLeft && block.bounds.left <= viewRight) {
                if (RectF.intersects(marioBounds, block.bounds)) {
                    if (m.x < block.x) m.x = block.x - m.size
                    else if (m.x > block.x) m.x = block.bounds.right
                }
            }
        }

        m.updateY(dt)
        marioBounds = m.getBounds()

        for (pipe in pipes) {
            if (pipe.bounds.right >= viewLeft && pipe.bounds.left <= viewRight) {
                if (RectF.intersects(marioBounds, pipe.bounds)) {
                    if (m.velocityY > 0 && m.y + m.size - m.velocityY * dt <= pipe.topY + 25f) {
                        m.setGroundedOn(pipe.topY)

                        pipe.teleportTargetX?.let { targetX ->
                            val exit = pipes.first { it.x == targetX }
                            m.teleportTo(targetX, exit.topY)
                            marioBounds = m.getBounds()
                            soundManager.playJump()
                        }
                    }
                }
            }
        }

        for (plat in platforms) {
            if (plat.bounds.right >= viewLeft && plat.bounds.left <= viewRight) {
                if (RectF.intersects(marioBounds, plat.bounds)) {
                    if (m.velocityY > 0 && m.y + m.size - m.velocityY * dt <= plat.y + 25f) {
                        m.setGroundedOn(plat.y)
                    }
                }
            }
        }

        for (block in blocks) {
            if (block.bounds.right >= viewLeft && block.bounds.left <= viewRight) {
                val bBounds = block.bounds
                if (RectF.intersects(marioBounds, bBounds)) {
                    if (m.velocityY > 0 && m.y + m.size - m.velocityY * dt <= bBounds.top + 25f) {
                        m.setGroundedOn(bBounds.top)
                    } else if (m.velocityY < 0 && m.y - m.velocityY * dt >= bBounds.bottom - 25f) {
                        m.y = bBounds.bottom
                        m.velocityY = 0f
                        if (!block.isHit) {
                            block.isHit = true
                            score += 100

                            block.weaponType?.let { wType ->
                                m.weapon = wType
                                soundManager.playCoin()
                            }

                            block.powerUpType?.let { pType ->
                                powerUps.add(PowerUp(block.x + 8f, block.y, pType, cache))
                                soundManager.playCoin()
                            } ?: run {
                                if (block.type == BlockType.QUESTION && block.weaponType == null) {
                                    coinCount++
                                    soundManager.playCoin()
                                }
                            }
                        }
                    }
                }
            }
        }

        val groundY = height - 100f
        for (river in rivers) {
            if (m.x + m.size > river.startX && m.x < river.endX) {
                if (!m.isGrounded && m.y + m.size >= groundY) {
                    handleLifeLost()
                    return
                } else if (m.isGrounded && m.y + m.size >= groundY - 10f) {
                    handleLifeLost()
                    return
                }
            }
        }

        val level = currentLevelData
        if (level != null && level.isBossLevel && boss?.isDefeated == false) {
            if (m.x >= level.arenaStartX && !arenaActive) {
                arenaActive = true
                if (m.weapon == WeaponType.NONE) {
                    m.weapon = WeaponType.SWORD
                    soundManager.playCoin()
                }
            }
            if (arenaActive) m.x = m.x.coerceIn(level.arenaStartX, level.flagpoleX - 230f - m.size)
        }
        cameraX = (m.x - width / 3f).coerceIn(0f, (worldWidth - width).coerceAtLeast(0f))
        marioBounds = m.getBounds()

        for (coin in coins) {
            if (!coin.isCollected && coin.getBounds().right >= viewLeft && coin.getBounds().left <= viewRight) {
                if (RectF.intersects(marioBounds, coin.getBounds())) {
                    coin.isCollected = true
                    score += 100
                    coinCount++
                    soundManager.playCoin()
                }
            }
        }

        for (pUp in powerUps) {
            if (!pUp.isCollected) {
                pUp.update(dt)
                if (RectF.intersects(marioBounds, pUp.getBounds())) {
                    pUp.isCollected = true
                    soundManager.playCoin()
                    score += 500
                    when (pUp.type) {
                        PowerUpType.MUSHROOM -> m.growBig()
                        PowerUpType.STAR -> m.enableSuperSpeed()
                        PowerUpType.COIN -> coinCount++
                    }
                }
            }
        }

        // Refresh after landing or teleporting before testing hazards.
        marioBounds = m.getBounds()
        for (cactus in cacti) {
            cactus.update(dt, marioBounds.centerX())
            if (cactus.canHit() && RectF.intersects(marioBounds, cactus.getBounds())) {
                if (hurtPlayer(m)) return
            }
        }
        for (bird in birds) {
            if (!bird.isAlive) continue
            if (bird.getBounds().right < viewLeft || bird.getBounds().left > viewRight) continue
            if (bird.update(dt, marioBounds.centerX())) {
                projectiles.add(Projectile(bird.x + 20f, bird.y + 48f, 0f,
                    isPlayerBullet = false, isRock = true, bitmapCache = cache,
                    speedY = 90f, gravity = 720f))
            }
            if (RectF.intersects(marioBounds, bird.getBounds())) {
                if (m.velocityY > 0f && m.y + m.size - m.velocityY * dt <= bird.getBounds().top + 25f) {
                    bird.isAlive = false
                    m.velocityY = -550f
                    score += 300
                    soundManager.playStomp()
                } else if (hurtPlayer(m)) return
            }
        }

        val projIterator = projectiles.iterator()
        while (projIterator.hasNext()) {
            val proj = projIterator.next()
            if (!proj.isActive) {
                projIterator.remove()
                continue
            }

            proj.update(dt)

            val projBounds = proj.getBounds()
            if (proj.y > groundY || pipes.any { RectF.intersects(projBounds, it.bounds) } ||
                platforms.any { RectF.intersects(projBounds, it.bounds) }) {
                proj.isActive = false
            }
            for (block in blocks) {
                if (RectF.intersects(projBounds, block.bounds)) {
                    proj.isActive = false
                    break
                }
            }

            if (!proj.isActive) continue

            if (proj.isPlayerBullet) {
                for (goomba in goombas) {
                    if (goomba.isAlive && !goomba.isStomped && RectF.intersects(projBounds, goomba.getBounds())) {
                        goomba.stomp()
                        proj.isActive = false
                        score += 200
                        soundManager.playStomp()
                        break
                    }
                }
                if (!proj.isActive) continue
                for (bird in birds) {
                    if (bird.isAlive && RectF.intersects(projBounds, bird.getBounds())) {
                        bird.isAlive = false
                        proj.isActive = false
                        score += 300
                        soundManager.playStomp()
                        break
                    }
                }
                if (!proj.isActive) continue
                for (cactus in cacti) {
                    if (cactus.canHit() && RectF.intersects(projBounds, cactus.getBounds())) {
                        cactus.isAlive = false
                        proj.isActive = false
                        score += 200
                        soundManager.playStomp()
                        break
                    }
                }
                if (!proj.isActive) continue
            }

            val b = boss
            if (proj.isPlayerBullet && b != null && arenaActive && !b.isDefeated) {
                if (RectF.intersects(projBounds, b.getBounds())) {
                    b.takeDamage(1)
                    proj.isActive = false
                    soundManager.playStomp()
                    if (b.isDefeated) {
                        score += 2000
                    }
                }
            }

            if (!proj.isPlayerBullet && RectF.intersects(projBounds, marioBounds)) {
                proj.isActive = false
                if (m.invincibleTimer <= 0f) {
                    if (m.isBig) {
                        m.shrinkToNormal()
                        soundManager.playHurt()
                    } else {
                        handleLifeLost()
                        return
                    }
                }
            }
        }

        val b = boss
        if (b != null && !b.isDefeated) {
            b.update(dt, arenaActive)
            if (arenaActive && b.consumeShot()) {
                val rules = ChallengeRules.forDifficulty(selectedDifficulty)
                val shotX = if (m.x < b.x) b.x - 36f else b.x + b.width
                val shotY = b.y + 48f
                val dx = marioBounds.centerX() - shotX
                val speedX = if (dx < 0f) -rules.bossShotSpeed else rules.bossShotSpeed
                val travelTime = (kotlin.math.abs(dx) / rules.bossShotSpeed).coerceAtLeast(0.35f)
                val speedY = ((marioBounds.centerY() - shotY) / travelTime).coerceIn(-240f, 240f)
                projectiles.add(Projectile(shotX, shotY, speedX, isPlayerBullet = false,
                    bitmapCache = cache, speedY = speedY))
                if (b.isEnraged) {
                    projectiles.add(Projectile(shotX, shotY - 95f, speedX,
                        isPlayerBullet = false, bitmapCache = cache, speedY = speedY * 0.4f))
                }
            }
            if (RectF.intersects(marioBounds, b.getBounds()) && hurtPlayer(m)) return
        }

        for (goomba in goombas) {
            if (!goomba.isAlive) continue

            if (goomba.getBounds().right >= viewLeft - 300f && goomba.getBounds().left <= viewRight + 300f) {
                goomba.update(dt)

                if (!goomba.isStomped) {
                    val gBounds = goomba.getBounds()
                    if (RectF.intersects(marioBounds, gBounds)) {
                        if (m.velocityY > 0 && m.y + m.size - m.velocityY * dt <= gBounds.top + 30f) {
                            goomba.stomp()
                            m.velocityY = -550f
                            score += 200
                            soundManager.playStomp()
                        } else if (m.invincibleTimer <= 0f) {
                            if (m.isBig) {
                                m.shrinkToNormal()
                                soundManager.playHurt()
                            } else {
                                handleLifeLost()
                                return
                            }
                        }
                    }
                }
            }
        }

        flagpole?.let { flag ->
            if ((boss == null || boss?.isDefeated == true) && RectF.intersects(marioBounds, flag.bounds)) {
                soundManager.playCoin()
                score += 1000 + remainingTime * 10
                currentLevelIndex++
                if (currentLevelIndex < levelsList.size) {
                    loadLevel(currentLevelIndex)
                } else {
                    gameState = GameState.ALL_CLEAR
                }
            }
        }
    }

    private fun performAttack() {
        val m = mario ?: return
        val cache = bitmapCache ?: return

        if (gameState != GameState.PLAYING || attackCooldown > 0f || m.weapon == WeaponType.NONE) return
        attackDuration = if (m.weapon == WeaponType.SWORD) 0.38f else 0.28f
        attackCooldown = attackDuration

        when (m.weapon) {
            WeaponType.GUN -> {
                val bulletSpeed = if (m.facingLeft) -600f else 600f
                projectiles.add(Projectile(m.x + m.size / 2f, m.y + 20f, bulletSpeed, isPlayerBullet = true, isRock = false, isSwordWave = false, cache))
                soundManager.playJump()
            }
            WeaponType.ROCK_THROW -> {
                val rockSpeed = if (m.facingLeft) -550f else 550f
                projectiles.add(Projectile(m.x + m.size / 2f, m.y + 20f, rockSpeed, isPlayerBullet = true, isRock = true, isSwordWave = false, cache))
                soundManager.playJump()
            }
            WeaponType.SWORD -> {
                val waveSpeed = if (m.facingLeft) -650f else 650f
                projectiles.add(Projectile(m.x + m.size / 2f, m.y + 10f, waveSpeed, isPlayerBullet = true, isRock = false, isSwordWave = true, cache))
                soundManager.playStomp()
            }
            else -> {}
        }
    }

    /** True when the caller must stop updating the old level. */
    private fun hurtPlayer(m: Mario): Boolean {
        if (m.invincibleTimer > 0f) return false
        if (m.isBig) {
            m.shrinkToNormal()
            soundManager.playHurt()
            return false
        }
        handleLifeLost()
        return true
    }

    private fun handleLifeLost() {
        lives--
        soundManager.playHurt()
        if (lives <= 0) {
            gameState = GameState.GAME_OVER
        } else {
            loadLevel(currentLevelIndex)
        }
    }

    private fun draw() {
        if (holder.surface.isValid) {
            val canvas: Canvas = holder.lockCanvas() ?: return

            try {
                val cache = bitmapCache
                val wF = width.toFloat()
                val hF = height.toFloat()

                updateMenuBounds(wF, hF)

                when (gameState) {
                    GameState.SPLASH_SCREEN -> drawSplashScreen(canvas, cache, wF, hF)
                    GameState.START_MENU -> drawStartMenu(canvas, cache, wF, hF)
                    GameState.LEVEL_TRANSITION -> drawLevelTransition(canvas, cache, wF, hF)
                    GameState.PLAYING -> drawGameWorld(canvas, cache, wF, hF)
                    GameState.GAME_OVER -> drawGameOver(canvas, wF, hF)
                    GameState.ALL_CLEAR -> drawAllClear(canvas, wF, hF)
                }
            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }
    }

    private fun drawSplashScreen(canvas: Canvas, cache: BitmapCache?, wF: Float, hF: Float) {
        canvas.drawColor(Color.rgb(110, 180, 255))

        scaledBgClouds?.let { canvas.drawBitmap(it, 0f, 0f, null) }
        scaledBgHills?.let { canvas.drawBitmap(it, 0f, hF - it.height.toFloat(), null) }

        if (cache != null) {
            val groundY = hF - 100f
            val tileSize = 70f
            var currX = 0f
            while (currX < wF) {
                canvas.drawBitmap(cache.grassTop, null, RectF(currX, groundY, currX + tileSize, groundY + tileSize), null)
                canvas.drawBitmap(cache.dirtCenter, null, RectF(currX, groundY + tileSize, currX + tileSize, hF), null)
                currX += tileSize
            }
        }

        val titleBox = RectF(wF * 0.15f, hF * 0.15f, wF * 0.85f, hF * 0.50f)
        canvas.drawRoundRect(titleBox, 30f, 30f, cardNormalPaint)
        canvas.drawRoundRect(titleBox, 30f, 30f, cardBorderPaint)

        canvas.drawText("SUPER MARIO BROS", wF / 2f, hF * 0.32f, splashTitlePaint)

        val subTextPaint = Paint().apply {
            color = Color.rgb(255, 220, 0)
            textSize = 34f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("MOBILE EDITION", wF / 2f, hF * 0.42f, subTextPaint)

        if (cache != null) {
            val marioX = wF / 2f - 45f
            val marioY = hF * 0.53f
            val dstRect = RectF(marioX, marioY, marioX + 90f, marioY + 90f)
            val srcRect = Rect(0, 0, cache.marioJump.width, cache.marioJump.height)
            canvas.drawBitmap(cache.marioJump, srcRect, dstRect, spritePaint)

            val qRect = RectF(marioX + 5f, marioY + 100f, marioX + 85f, marioY + 180f)
            val qSrc = Rect(0, 0, cache.blockQuestion.width, cache.blockQuestion.height)
            canvas.drawBitmap(cache.blockQuestion, qSrc, qRect, null)
        }

        if ((splashFlashTimer * 2f).toInt() % 2 == 0) {
            canvas.drawText("CHẠM MỌI NƠI ĐỂ BẮT ĐẦU", wF / 2f, hF * 0.88f, splashSubPaint)
        }

        val copyPaint = Paint().apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("© 2026 SUPPER MARIO EDITION", wF / 2f, hF * 0.95f, copyPaint)
    }

    private fun drawStartMenu(canvas: Canvas, cache: BitmapCache?, wF: Float, hF: Float) {
        canvas.drawColor(Color.rgb(25, 35, 60))

        canvas.drawText("SUPER MARIO", wF / 2f, hF * 0.16f, titlePaint)

        canvas.drawText("CHỌN NHÂN VẬT", wF / 2f, hF * 0.25f, sectionPaint)

        if (cache != null) {
            drawCharacterCard(canvas, charBeigeBtn, cache.marioIdle, "HERO", selectedCharacter == CharacterType.BEIGE)
            drawCharacterCard(canvas, charPinkBtn, cache.pinkIdle, "PRINCESS", selectedCharacter == CharacterType.PINK)
            drawCharacterCard(canvas, charGreenBtn, cache.greenIdle, "LUIGI", selectedCharacter == CharacterType.GREEN)
        }

        canvas.drawText("CHỌN ĐỘ KHÓ", wF / 2f, hF * 0.54f, sectionPaint)

        drawDifficultyBtn(canvas, diffEasyBtn, "DỄ (5 Mạng)", selectedDifficulty == Difficulty.EASY)
        drawDifficultyBtn(canvas, diffMedBtn, "VỪA (3 Mạng)", selectedDifficulty == Difficulty.MEDIUM)
        drawDifficultyBtn(canvas, diffHardBtn, "KHÓ (1 Mạng)", selectedDifficulty == Difficulty.HARD)

        canvas.drawRoundRect(startGameBtn, 25f, 25f, startBtnPaint)
        canvas.drawRoundRect(startGameBtn, 25f, 25f, cardBorderPaint)
        canvas.drawText("BẮT ĐẦU CHƠI", startGameBtn.centerX(), startGameBtn.centerY() + 18f, textPaint)
    }

    private fun drawCharacterCard(canvas: Canvas, rect: RectF, bitmap: Bitmap, label: String, isSelected: Boolean) {
        val paint = if (isSelected) cardSelectedPaint else cardNormalPaint
        canvas.drawRoundRect(rect, 20f, 20f, paint)
        if (isSelected) {
            canvas.drawRoundRect(rect, 20f, 20f, cardBorderPaint)
        }

        val iconSize = 75f
        val iconRect = RectF(rect.centerX() - iconSize / 2f, rect.top + 15f, rect.centerX() + iconSize / 2f, rect.top + 15f + iconSize)
        val srcRect = Rect(0, 0, bitmap.width, bitmap.height)
        canvas.drawBitmap(bitmap, srcRect, iconRect, spritePaint)

        val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 28f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, rect.centerX(), rect.bottom - 15f, labelPaint)
    }

    private fun drawDifficultyBtn(canvas: Canvas, rect: RectF, label: String, isSelected: Boolean) {
        val paint = if (isSelected) cardSelectedPaint else cardNormalPaint
        canvas.drawRoundRect(rect, 15f, 15f, paint)
        if (isSelected) {
            canvas.drawRoundRect(rect, 15f, 15f, cardBorderPaint)
        }

        val labelPaint = Paint().apply {
            color = Color.WHITE
            textSize = 26f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(label, rect.centerX(), rect.centerY() + 10f, labelPaint)
    }

    private fun drawLevelTransition(canvas: Canvas, cache: BitmapCache?, wF: Float, hF: Float) {
        canvas.drawColor(Color.BLACK)

        val worldName = currentLevelData?.worldName ?: "WORLD 1-1"
        canvas.drawText(worldName, wF / 2f, hF / 2f - 40f, titlePaint)

        val challenge = currentLevelData?.challenge.orEmpty()
        val hintPaint = Paint(sectionPaint)
        val hintWidth = hintPaint.measureText(challenge)
        if (hintWidth > wF - 50f) hintPaint.textSize *= (wF - 50f) / hintWidth
        canvas.drawText(challenge, wF / 2f, hF / 2f - 100f, hintPaint)
        cache?.let { c ->
            val iconSize = 80f
            val marioBmp = when (selectedCharacter) {
                CharacterType.BEIGE -> c.marioIdle
                CharacterType.PINK -> c.pinkIdle
                CharacterType.GREEN -> c.greenIdle
            }
            val srcRect = Rect(0, 0, marioBmp.width, marioBmp.height)
            val dstRect = RectF(wF / 2f - 90f, hF / 2f + 10f, wF / 2f - 90f + iconSize, hF / 2f + 10f + iconSize)
            canvas.drawBitmap(marioBmp, srcRect, dstRect, spritePaint)

            canvas.drawText("x   $lives", wF / 2f + 50f, hF / 2f + 65f, sectionPaint)
        }
    }

    private fun drawGameWorld(canvas: Canvas, cache: BitmapCache?, wF: Float, hF: Float) {
        val skyColor = currentLevelData?.skyColor ?: Color.rgb(110, 180, 255)
        canvas.drawColor(skyColor)

        val bgClouds = scaledBgClouds
        if (bgClouds != null) {
            val cloudOffset = -(cameraX * 0.2f) % wF
            canvas.drawBitmap(bgClouds, cloudOffset, 0f, null)
            canvas.drawBitmap(bgClouds, cloudOffset + wF, 0f, null)
        }

        val bgHills = scaledBgHills
        if (bgHills != null) {
            val hillOffset = -(cameraX * 0.4f) % wF
            val hillY = hF - bgHills.height.toFloat()
            canvas.drawBitmap(bgHills, hillOffset, hillY, null)
            canvas.drawBitmap(bgHills, hillOffset + wF, hillY, null)
        }

        canvas.save()
        canvas.translate(-cameraX, 0f)

        if (cache != null) {
            val groundY = height - 100f
            val tileSize = 70f
            val startTileX = ((cameraX / tileSize).toInt() * tileSize - tileSize).coerceAtLeast(0f)
            val endTileX = (((cameraX + wF) / tileSize).toInt() * tileSize + tileSize * 2f).coerceAtMost(worldWidth)

            var currX = startTileX
            while (currX < endTileX) {
                var isOverRiver = false
                for (river in rivers) {
                    if (currX + tileSize > river.startX && currX < river.endX) {
                        isOverRiver = true
                        break
                    }
                }

                if (!isOverRiver) {
                    val tileRect = RectF(currX, groundY, currX + tileSize, groundY + tileSize)
                    canvas.drawBitmap(cache.grassTop, null, tileRect, null)

                    val dirtRect = RectF(currX, groundY + tileSize, currX + tileSize, hF)
                    canvas.drawBitmap(cache.dirtCenter, null, dirtRect, null)
                }
                currX += tileSize
            }

            for (river in rivers) {
                if (river.endX >= cameraX - 100f && river.startX <= cameraX + wF + 100f) {
                    river.draw(canvas, hF)
                }
            }
        }

        val viewLeft = cameraX - 100f
        val viewRight = cameraX + wF + 100f

        val level = currentLevelData
        if (level?.isBossLevel == true && boss?.isDefeated == false) {
            val gateX = level.flagpoleX - 230f
            canvas.drawRect(gateX, hF - 420f, gateX + 12f, hF - 100f, attackGatePaint)
            canvas.drawText("HẠ BOSS ĐỂ MỞ CỔNG", gateX, hF - 440f, sectionPaint)
            if (arenaActive) {
                canvas.drawRect(level.arenaStartX - 12f, hF - 420f, level.arenaStartX, hF - 100f, attackGatePaint)
            }
        }

        flagpole?.let { if (it.x + 100f >= viewLeft && it.x <= viewRight) it.draw(canvas) }
        boss?.let { if (it.x + 200f >= viewLeft && it.x <= viewRight) it.draw(canvas) }
        for (plat in platforms) if (plat.bounds.right >= viewLeft && plat.bounds.left <= viewRight) plat.draw(canvas)
        for (cactus in cacti) cactus.draw(canvas)
        for (bird in birds) if (bird.getBounds().right >= viewLeft && bird.getBounds().left <= viewRight) bird.draw(canvas)
        for (pipe in pipes) if (pipe.bounds.right >= viewLeft && pipe.bounds.left <= viewRight) pipe.draw(canvas)
        for (block in blocks) if (block.bounds.right >= viewLeft && block.bounds.left <= viewRight) block.draw(canvas)
        for (coin in coins) if (coin.getBounds().right >= viewLeft && coin.getBounds().left <= viewRight) coin.draw(canvas)
        for (pUp in powerUps) if (pUp.getBounds().right >= viewLeft && pUp.getBounds().left <= viewRight) pUp.draw(canvas)
        for (proj in projectiles) if (proj.getBounds().right >= viewLeft && proj.getBounds().left <= viewRight) proj.draw(canvas)
        for (goomba in goombas) if (goomba.getBounds().right >= viewLeft && goomba.getBounds().left <= viewRight) goomba.draw(canvas)
        mario?.draw(canvas)

        canvas.restore()

        drawControls(canvas)

        drawRetroHUD(canvas, wF)
    }

    private fun drawRetroHUD(canvas: Canvas, wF: Float) {
        val scoreStr = String.format("%06d", score)
        val coinStr = String.format("x%02d", coinCount)
        val worldStr = currentLevelData?.worldName?.replace("WORLD ", "") ?: "1-1"
        val timeStr = String.format("%03d", remainingTime)

        val col1 = wF * 0.10f
        val col2 = wF * 0.28f
        val col3 = wF * 0.50f
        val col4 = wF * 0.70f
        val col5 = wF * 0.88f

        val labelY = 38f
        val valueY = 72f

        canvas.drawText("SCORE", col1, labelY, retroHudLabelPaint)
        canvas.drawText(scoreStr, col1, valueY, retroHudValuePaint)

        canvas.drawText("COINS", col2, labelY, retroHudLabelPaint)
        canvas.drawText(coinStr, col2, valueY, retroHudValuePaint)

        canvas.drawText("WORLD", col3, labelY, retroHudLabelPaint)
        canvas.drawText(worldStr, col3, valueY, retroHudValuePaint)

        canvas.drawText("TIME", col4, labelY, retroHudLabelPaint)
        canvas.drawText(timeStr, col4, valueY, retroHudValuePaint)

        canvas.drawText("LIVES", col5, labelY, retroHudLabelPaint)
        canvas.drawText("$lives", col5, valueY, retroHudValuePaint)
    }

    private fun drawGameOver(canvas: Canvas, wF: Float, hF: Float) {
        canvas.drawRect(0f, 0f, wF, hF, overlayPaint)
        val gameOverPaint = Paint().apply {
            color = Color.RED
            textSize = 80f
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("GAME OVER", wF / 2f, hF / 2f - 30f, gameOverPaint)
        canvas.drawText("Chạm vào màn hình hoặc bấm SPACE để quay về Menu", wF / 2f, hF / 2f + 50f, sectionPaint)
    }

    private fun drawAllClear(canvas: Canvas, wF: Float, hF: Float) {
        canvas.drawRect(0f, 0f, wF, hF, overlayPaint)
        canvas.drawText("CONGRATULATIONS!", wF / 2f, hF / 2f - 50f, titlePaint)
        canvas.drawText("Bạn đã chinh phục hoàn thành tất cả 5 màn chơi!", wF / 2f, hF / 2f + 20f, sectionPaint)
        canvas.drawText("TỔNG ĐIỂM CỦA BẠN: $score", wF / 2f, hF / 2f + 80f, titlePaint)
        canvas.drawText("Chạm vào màn hình để quay về Menu", wF / 2f, hF / 2f + 150f, sectionPaint)
    }

    private fun drawControls(canvas: Canvas) {
        val m = mario

        val pLeft = if (isLeftPressed) btnPressedPaint else btnNormalPaint
        canvas.drawRoundRect(leftBtn, 25f, 25f, pLeft)
        canvas.drawText("◄", leftBtn.centerX(), leftBtn.centerY() + 18f, textPaint)

        val pRight = if (isRightPressed) btnPressedPaint else btnNormalPaint
        canvas.drawRoundRect(rightBtn, 25f, 25f, pRight)
        canvas.drawText("►", rightBtn.centerX(), rightBtn.centerY() + 18f, textPaint)

        val pJump = if (isJumpPressed) btnPressedPaint else btnNormalPaint
        canvas.drawRoundRect(jumpBtn, 25f, 25f, pJump)
        canvas.drawText("SPACE", jumpBtn.centerX(), jumpBtn.centerY() + 18f, textPaint)

        if (m != null && m.weapon != WeaponType.NONE) {
            attackButtonRenderer.draw(canvas, attackBtn, m.weapon, isAttackPressed,
                (attackCooldown / attackDuration).coerceIn(0f, 1f))
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y
        val action = event.actionMasked

        if (gameState == GameState.SPLASH_SCREEN) {
            if (action == MotionEvent.ACTION_DOWN) {
                soundManager.playCoin()
                gameState = GameState.START_MENU
            }
            return true
        }

        if (gameState == GameState.START_MENU) {
            if (action == MotionEvent.ACTION_DOWN) {
                if (charBeigeBtn.contains(touchX, touchY)) selectedCharacter = CharacterType.BEIGE
                if (charPinkBtn.contains(touchX, touchY)) selectedCharacter = CharacterType.PINK
                if (charGreenBtn.contains(touchX, touchY)) selectedCharacter = CharacterType.GREEN

                if (diffEasyBtn.contains(touchX, touchY)) selectedDifficulty = Difficulty.EASY
                if (diffMedBtn.contains(touchX, touchY)) selectedDifficulty = Difficulty.MEDIUM
                if (diffHardBtn.contains(touchX, touchY)) selectedDifficulty = Difficulty.HARD

                if (startGameBtn.contains(touchX, touchY)) {
                    startNewGame()
                }
            }
            return true
        }

        if (gameState == GameState.GAME_OVER || gameState == GameState.ALL_CLEAR) {
            if (action == MotionEvent.ACTION_DOWN) {
                gameState = GameState.START_MENU
            }
            return true
        }

        if (action == MotionEvent.ACTION_CANCEL) {
            isLeftPressed = false
            isRightPressed = false
            isJumpPressed = false
            isAttackPressed = false
            mario?.releaseJump()
            return true
        }
        if (gameState != GameState.PLAYING) return true
        val m = mario ?: return super.onTouchEvent(event)
        val pointerCount = event.pointerCount
        var touchLeft = false
        var touchRight = false
        var touchJump = false
        var touchAttack = false
        val actionIndex = event.actionIndex

        for (i in 0 until pointerCount) {
            if ((action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) && i == actionIndex) {
                continue
            }

            val pX = event.getX(i)
            val pY = event.getY(i)

            if (leftTouchArea.contains(pX, pY)) touchLeft = true
            if (rightTouchArea.contains(pX, pY)) touchRight = true
            if (jumpTouchArea.contains(pX, pY)) {
                touchJump = true
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    if (i == actionIndex) {
                        m.jump()
                        soundManager.playJump()
                    }
                }
            }

            if (attackTouchArea.contains(pX, pY)) {
                touchAttack = true
                if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) {
                    if (i == actionIndex) {
                        performAttack()
                    }
                }
            }
        }

        if (!touchJump && isJumpPressed) {
            m.releaseJump()
        }

        isLeftPressed = touchLeft
        isRightPressed = touchRight
        isJumpPressed = touchJump
        isAttackPressed = touchAttack

        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (gameState == GameState.SPLASH_SCREEN) {
            if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER) {
                soundManager.playCoin()
                gameState = GameState.START_MENU
            }
            return true
        }

        if (gameState == GameState.START_MENU) {
            if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER) {
                startNewGame()
            }
            return true
        }

        if (gameState == GameState.GAME_OVER || gameState == GameState.ALL_CLEAR) {
            if (keyCode == KeyEvent.KEYCODE_SPACE || keyCode == KeyEvent.KEYCODE_ENTER) {
                gameState = GameState.START_MENU
            }
            return true
        }

        val m = mario ?: return super.onKeyDown(keyCode, event)
        when (keyCode) {
            KeyEvent.KEYCODE_A -> isLeftPressed = true
            KeyEvent.KEYCODE_D -> isRightPressed = true
            KeyEvent.KEYCODE_SPACE -> {
                if (!isJumpPressed) {
                    m.jump()
                    soundManager.playJump()
                }
                isJumpPressed = true
            }
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_F -> {
                isAttackPressed = true
                performAttack()
            }
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val m = mario
        when (keyCode) {
            KeyEvent.KEYCODE_A -> isLeftPressed = false
            KeyEvent.KEYCODE_D -> isRightPressed = false
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_F -> isAttackPressed = false
            KeyEvent.KEYCODE_SPACE -> {
                isJumpPressed = false
                m?.releaseJump()
            }
            else -> return super.onKeyUp(keyCode, event)
        }
        return true
    }
}
