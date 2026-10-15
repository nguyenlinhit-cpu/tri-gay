import CoreGraphics
import Foundation

public final class GameEngine {
    public static let worldWidth: CGFloat = 5500
    public static let groundY: CGFloat = 980
    public static let gravity: CGFloat = 1200
    public static let jumpSpeed: CGFloat = 720
    public static let runSpeed: CGFloat = 380
    public let library: [Difficulty: LevelSet]
    public var difficulty: Difficulty = .medium
    public var character = "mario"
    public private(set) var state: PlayState = .menu
    public private(set) var levelIndex = 0
    public private(set) var score = 0
    public private(set) var coinCount = 0
    public private(set) var lives = 3
    public private(set) var remainingTime: CGFloat = 0
    public private(set) var transitionTimer: CGFloat = 0
    public var player = CGRect(x: 100, y: 900, width: 80, height: 80)
    public var vx: CGFloat = 0
    public var vy: CGFloat = 0
    public private(set) var grounded = true
    public private(set) var facingLeft = false
    public private(set) var big = false
    public private(set) var invincible: CGFloat = 0
    public private(set) var starTimer: CGFloat = 0
    public private(set) var weapon: Weapon = .none
    public private(set) var attackCooldown: CGFloat = 0
    public private(set) var attackDuration: CGFloat = 0.38
    public private(set) var animationTime: CGFloat = 0
    public private(set) var platforms = [MovingPlatform]()
    public private(set) var blocks = [BlockState]()
    public private(set) var cacti = [Cactus]()
    public private(set) var enemies = [Enemy]()
    public private(set) var collectedCoins = Set<Int>()
    public private(set) var shots = [Shot]()
    public private(set) var pickups = [Pickup]()
    public private(set) var boss: BossCombat?
    public private(set) var bossRect = CGRect.zero
    public private(set) var arenaActive = false
    public var left = false, right = false
    public var sounds = [String]()
    private var accumulator: CGFloat = 0
    private var bossDirection: CGFloat = -1
    private var autoGunTimer: CGFloat = 0
    public var level: Level { library[difficulty]!.levels[levelIndex] }
    public var rules: Rules { library[difficulty]!.rules }
    public var cameraX: CGFloat { min(max(player.midX - 640, 0), Self.worldWidth - 1920) }

    public init(library: [Difficulty: LevelSet]) { self.library = library }
    public func start(at index: Int = 0) { score = 0; coinCount = 0; lives = difficulty.lives; levelIndex = (0..<5).contains(index) ? index : 0; loadLevel() }
    public func returnToMenu() { state = .menu; clearInput(); sounds.removeAll() }
    public func clearInput() { left = false; right = false; releaseJump() }
    public func jump() {
        guard state == .playing, grounded else { return }
        vy = -Self.jumpSpeed * (big ? 1.08 : 1); grounded = false; sounds.append("jump")
    }
    public func releaseJump() { if vy < -200 { vy *= 0.45 } }
    public func attack() {
        guard state == .playing, weapon != .none, attackCooldown <= 0 else { return }
        attackDuration = weapon == .sword ? 0.38 : 0.28; attackCooldown = attackDuration
        let speed: CGFloat = weapon == .sword ? 650 : (weapon == .gun ? 600 : 550)
        let x = facingLeft ? player.minX - 20 : player.maxX - 15
        shots.append(Shot(x: x, y: player.minY + 15, vx: facingLeft ? -speed : speed, friendly: true, weapon: weapon))
        sounds.append(weapon == .sword ? "stomp" : "jump")
    }
    private func loadLevel() {
        player = CGRect(x: 100, y: Self.groundY - 80, width: 80, height: 80)
        vx = 0; vy = 0; grounded = true; facingLeft = false; big = false
        invincible = 0; starTimer = 0; weapon = .none; attackCooldown = 0; autoGunTimer = 0
        accumulator = 0; left = false; right = false; animationTime = 0
        remainingTime = CGFloat(level.timeLimit); arenaActive = false; bossDirection = -1
        platforms = level.platforms.map(MovingPlatform.init)
        blocks = level.blocks.map(BlockState.init)
        enemies = level.goombas.map(Enemy.init) + level.birds.map { Enemy($0, tempo: rules.hazardTempo) }
        let exits = Set(level.pipes.compactMap(\.teleportTargetX))
        cacti = level.pipes.enumerated().compactMap { i, pipe in
            guard pipe.teleportTargetX == nil, !exits.contains(pipe.x) else { return nil }
            return Cactus(pipe, tempo: rules.hazardTempo, phase: CGFloat(i) * 0.37)
        }
        collectedCoins.removeAll(); shots.removeAll(); pickups.removeAll()
        boss = level.isBossLevel ? BossCombat(hp: rules.bossHp, interval: rules.bossShotInterval) : nil
        bossRect = CGRect(x: level.bossX, y: Self.groundY - 140, width: 140, height: 140)
        transitionTimer = 3; state = .transition
    }
    public func advance(_ elapsed: TimeInterval) {
        let dt = CGFloat(min(max(elapsed, 0), 0.1))
        if state == .transition {
            transitionTimer -= dt
            if transitionTimer <= 0 { state = .playing }
            return
        }
        guard state == .playing else { return }
        accumulator += dt
        let step: CGFloat = 1.0 / 120
        while accumulator >= step && state == .playing { accumulator -= step; tick(step) }
    }
    private func pipeRect(_ p: PipeData) -> CGRect { CGRect(x: p.x, y: Self.groundY - p.height, width: 110, height: p.height) }
    private func loseLife() {
        lives -= 1; sounds.append("hurt"); clearInput()
        if lives <= 0 { state = .gameOver } else { loadLevel() }
    }
    @discardableResult private func hurt() -> Bool {
        if invincible > 0 { return false }
        if big {
            big = false; let bottom = player.maxY
            player.size = CGSize(width: 80, height: 80); player.origin.y = bottom - 80
            invincible = 1.2; sounds.append("hurt"); return false
        }
        loseLife(); return true
    }
    private func hit(_ block: BlockState) {
        guard !block.hit else { return }
        block.hit = true; score += 100
        if let newWeapon = block.data.weaponType { weapon = newWeapon; sounds.append("coin") }
        else if let type = block.data.powerUpType { pickups.append(Pickup(x: block.rect.minX + 8, y: block.rect.minY, type: type)); sounds.append("coin") }
        else if block.data.type == "QUESTION" { coinCount += 1; sounds.append("coin") }
    }
    private func tick(_ dt: CGFloat) {
        animationTime += dt; remainingTime -= dt
        if remainingTime <= 0 { loseLife(); return }
        invincible = max(0, invincible - dt); starTimer = max(0, starTimer - dt)
        attackCooldown = max(0, attackCooldown - dt)
        if weapon == .gun { autoGunTimer += dt; if autoGunTimer >= 4 { autoGunTimer = 0; attack() } }
        for platform in platforms {
            let old = platform.rect
            let delta = platform.update(dt)
            if grounded && abs(player.maxY - old.minY) < 4 && player.maxX > old.minX && player.minX < old.maxX { player.origin.x += delta }
        }
        let speed = Self.runSpeed * (starTimer > 0 ? 1.5 : 1)
        if left != right {
            vx = min(max(vx + (left ? -1 : 1) * 2400 * dt, -speed), speed); facingLeft = left
        } else if vx != 0 {
            vx = vx > 0 ? max(0, vx - 1800 * dt) : min(0, vx + 1800 * dt)
        }
        let previousX = player
        player.origin.x = min(max(player.minX + vx * dt, 0), Self.worldWidth - player.width)
        let solids = level.pipes.map(pipeRect) + blocks.map(\.rect)
        for solid in solids where player.intersects(solid) {
            if previousX.maxX <= solid.minX + 1 { player.origin.x = solid.minX - player.width; vx = 0 }
            else if previousX.minX >= solid.maxX - 1 { player.origin.x = solid.maxX; vx = 0 }
        }
        let oldFeet = player.maxY
        let oldTop = player.minY
        vy += Self.gravity * dt; player.origin.y += vy * dt; grounded = false
        for solid in (solids + platforms.map(\.rect)).sorted(by: { $0.minY < $1.minY }) {
            guard player.maxX > solid.minX, player.minX < solid.maxX else { continue }
            if vy > 0 && oldFeet <= solid.minY + 0.5 && player.maxY >= solid.minY {
                player.origin.y = solid.minY - player.height; vy = 0; grounded = true
            }
        }
        for block in blocks {
            let solid = block.rect
            if vy < 0 && oldTop >= solid.maxY - 0.5 && player.minY <= solid.maxY && player.maxY > solid.minY && player.maxX > solid.minX && player.minX < solid.maxX {
                player.origin.y = solid.maxY; vy = 0; hit(block)
            }
        }
        if player.maxY >= Self.groundY { player.origin.y = Self.groundY - player.height; vy = 0; grounded = true }
        if grounded {
            for pipe in level.pipes where pipe.teleportTargetX != nil {
                if abs(player.maxY - (Self.groundY - pipe.height)) < 1 && player.maxX > pipe.x && player.minX < pipe.x + 110,
                   let exit = level.pipes.first(where: { $0.x == pipe.teleportTargetX }) {
                    player.origin = CGPoint(x: exit.x + 15, y: Self.groundY - exit.height - player.height - 12)
                    vx = 0; vy = 0; grounded = false; sounds.append("jump"); break
                }
            }
        }
        for river in level.rivers where player.maxX > river.startX && player.minX < river.endX {
            if player.maxY >= Self.groundY - 1 { loseLife(); return }
        }
        if let boss = boss, !boss.defeated {
            if player.minX >= level.arenaStartX && !arenaActive { arenaActive = true; if weapon == .none { weapon = .sword; sounds.append("coin") } }
            if arenaActive { player.origin.x = min(max(player.minX, level.arenaStartX), level.flagpoleX - 230 - player.width) }
        }
        for (i, coin) in level.coins.enumerated() where !collectedCoins.contains(i) {
            if player.intersects(CGRect(x: coin.x, y: Self.groundY - coin.yOffset, width: 45, height: 45)) {
                collectedCoins.insert(i); score += 100; coinCount += 1; sounds.append("coin")
            }
        }
        for pickup in pickups where pickup.alive {
            if pickup.rect.minY > pickup.targetY { pickup.rect.origin.y = max(pickup.targetY, pickup.rect.minY - 120 * dt) }
            else if pickup.type != "COIN" { pickup.rect.origin.x += 120 * dt }
            if player.intersects(pickup.rect) {
                pickup.alive = false; score += 500; sounds.append("coin")
                if pickup.type == "MUSHROOM" && !big {
                    big = true; let bottom = player.maxY; player.size = CGSize(width: 115, height: 115); player.origin.y = bottom - 115
                } else if pickup.type == "STAR" { starTimer = 10 }
                else if pickup.type == "COIN" { coinCount += 1 }
            }
        }
        pickups.removeAll { !$0.alive }
        for cactus in cacti where cactus.alive {
            cactus.cycle.update(dt, playerNear: abs(player.midX - cactus.rect.midX) < 110)
            if cactus.dangerous && player.intersects(cactus.rect) && hurt() { return }
        }
        for enemy in enemies where enemy.alive {
            guard enemy.rect.maxX > cameraX - 250 && enemy.rect.minX < cameraX + 2170 else { continue }
            enemy.clock += dt; enemy.rect.origin.x += enemy.direction * enemy.speed * dt
            if enemy.rect.minX < enemy.minX { enemy.rect.origin.x = enemy.minX; enemy.direction = 1 }
            if enemy.rect.minX > enemy.maxX { enemy.rect.origin.x = enemy.maxX; enemy.direction = -1 }
            if enemy.kind == .bird {
                enemy.rect.origin.y = enemy.baseY + sin(enemy.clock * 3) * 12
                enemy.dropTimer = max(0, enemy.dropTimer - dt)
                if enemy.dropTimer <= 0 && abs(player.midX - enemy.rect.midX) < 190 {
                    enemy.dropTimer = 2.8 / rules.hazardTempo
                    shots.append(Shot(x: enemy.rect.midX - 17, y: enemy.rect.maxY, vx: 0, vy: 90, gravity: 720, friendly: false, weapon: .rock))
                }
            }
            if player.intersects(enemy.rect) {
                if vy > 0 && oldFeet <= enemy.rect.minY + 25 {
                    enemy.alive = false; vy = -550; grounded = false; score += enemy.kind == .bird ? 300 : 200; sounds.append("stomp")
                } else if hurt() { return }
            }
        }
        for shot in shots where shot.alive {
            shot.vy += shot.gravity * dt; shot.rect.origin.x += shot.vx * dt; shot.rect.origin.y += shot.vy * dt
            if shot.rect.maxX < -100 || shot.rect.minX > Self.worldWidth + 100 || shot.rect.minY > Self.groundY || shot.rect.maxY < -500 ||
                (solids + platforms.map(\.rect)).contains(where: { shot.rect.intersects($0) }) { shot.alive = false; continue }
            if shot.friendly {
                for enemy in enemies where enemy.alive && shot.rect.intersects(enemy.rect) {
                    enemy.alive = false; shot.alive = false; score += enemy.kind == .bird ? 300 : 200; sounds.append("stomp"); break
                }
                if !shot.alive { continue }
                for cactus in cacti where cactus.dangerous && shot.rect.intersects(cactus.rect) {
                    cactus.alive = false; shot.alive = false; score += 200; sounds.append("stomp"); break
                }
                if !shot.alive { continue }
                if let boss = boss, arenaActive, !boss.defeated, shot.rect.intersects(bossRect) {
                    if boss.takeDamage() { sounds.append("stomp"); if boss.defeated { score += 2000 } }
                    shot.alive = false
                }
            } else if shot.rect.intersects(player) { shot.alive = false; if hurt() { return } }
        }
        shots.removeAll { !$0.alive }
        if let boss = boss, !boss.defeated {
            boss.update(dt, active: arenaActive)
            if arenaActive {
                bossRect.origin.x += bossDirection * (boss.enraged ? 175 : 115) * dt
                if bossRect.minX < level.bossX - 230 { bossRect.origin.x = level.bossX - 230; bossDirection = 1 }
                if bossRect.minX > level.bossX + 230 { bossRect.origin.x = level.bossX + 230; bossDirection = -1 }
                if boss.consumeShot() {
                    let x = player.minX < bossRect.minX ? bossRect.minX - 36 : bossRect.maxX
                    let y = bossRect.minY + 48
                    let dx = player.midX - x
                    let speedX = dx < 0 ? -rules.bossShotSpeed : rules.bossShotSpeed
                    let travel = max(abs(dx) / rules.bossShotSpeed, 0.35)
                    let speedY = min(max((player.midY - y) / travel, -240), 240)
                    shots.append(Shot(x: x, y: y, vx: speedX, vy: speedY, friendly: false, weapon: .gun))
                    if boss.enraged { shots.append(Shot(x: x, y: y - 95, vx: speedX, vy: speedY * 0.4, friendly: false, weapon: .gun)) }
                }
            }
            if player.intersects(bossRect) && hurt() { return }
        }
        if player.maxX >= level.flagpoleX && (boss == nil || boss!.defeated) {
            score += 1000 + max(0, Int(remainingTime)) * 10; sounds.append("coin")
            if levelIndex == 4 { state = .cleared; clearInput() } else { levelIndex += 1; loadLevel() }
        }
    }
}
