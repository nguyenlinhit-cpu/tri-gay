import UIKit
import AVFoundation

/// Native UIKit/Core Graphics rendering; game simulation has no web view or platform dependencies.
final class NativeGameView: UIView {
    let engine: GameEngine
    private var displayLink: CADisplayLink?
    private var lastTime: CFTimeInterval = 0
    private var images = [String: UIImage]()
    private var audio = [String: [AVAudioPlayer]]()
    private let menu = UIStackView()
    private let characters = UISegmentedControl(items: ["HERO", "PRINCESS", "LUIGI"])
    private let difficulty = UISegmentedControl(items: ["DỄ · 5 mạng", "VỪA · 3 mạng", "KHÓ · 1 mạng"])
    private let startButton = UIButton(type: .system)
    private let leftButton = UIButton(type: .system)
    private let rightButton = UIButton(type: .system)
    private let jumpButton = UIButton(type: .system)
    private let attackButton = UIButton(type: .system)
    private let pauseButton = UIButton(type: .system)
    private let leftControls = UIStackView()
    private let rightControls = UIStackView()
    private let cooldown = CAShapeLayer()
    private var paused = false
    private var shownState: PlayState?
    private let heading = UILabel()
    private let subheading = UILabel()

    init(engine: GameEngine) {
        self.engine = engine
        super.init(frame: .zero)
        isOpaque = true; backgroundColor = .black
        isMultipleTouchEnabled = true
        accessibilityIdentifier = "nativeGameView"
        for name in ["jump", "coin", "hurt", "stomp"] {
            if let url = Bundle.main.url(forResource: "sfx_\(name)", withExtension: "wav", subdirectory: "Resources") {
                audio[name] = (0..<3).compactMap { _ in try? AVAudioPlayer(contentsOf: url) }
                audio[name]?.forEach { $0.volume = 0.45; $0.prepareToPlay() }
            }
        }
        try? AVAudioSession.sharedInstance().setCategory(.ambient, mode: .default)
        configureMenu()
        configureControls()
        NotificationCenter.default.addObserver(self, selector: #selector(backgrounded), name: UIApplication.willResignActiveNotification, object: nil)
        NotificationCenter.default.addObserver(self, selector: #selector(foregrounded), name: UIApplication.didBecomeActiveNotification, object: nil)
        syncInterface()
    }
    required init?(coder: NSCoder) { fatalError("init(coder:) is not supported") }
    deinit { displayLink?.invalidate(); NotificationCenter.default.removeObserver(self) }
    func startDisplayLink() {
        guard displayLink == nil else { return }
        let link = CADisplayLink(target: self, selector: #selector(frameTick(_:)))
        link.preferredFramesPerSecond = 60
        link.add(to: .main, forMode: .common)
        displayLink = link; lastTime = 0
    }
    func stopDisplayLink() { displayLink?.invalidate(); displayLink = nil; lastTime = 0; clearControls() }
    @objc private func backgrounded() {
        if engine.state == .playing { paused = true }
        stopDisplayLink(); syncInterface(); setNeedsDisplay()
    }
    @objc private func foregrounded() { startDisplayLink() }
    @objc private func frameTick(_ link: CADisplayLink) {
        let elapsed = lastTime == 0 ? 0 : link.timestamp - lastTime
        lastTime = link.timestamp
        if !paused { engine.advance(elapsed) }
        let sounds = engine.sounds
        engine.sounds.removeAll(keepingCapacity: true)
        for name in sounds {
            if let players = audio[name], let player = players.first(where: { !$0.isPlaying }) { player.currentTime = 0; player.play() }
        }
        syncInterface(); setNeedsDisplay()
    }
    private func label(_ text: String, size: CGFloat) -> UILabel {
        let value = UILabel(); value.text = text; value.textColor = .white
        value.font = .boldSystemFont(ofSize: size); value.textAlignment = .center
        value.adjustsFontSizeToFitWidth = true; value.minimumScaleFactor = 0.5
        return value
    }
    private func configureMenu() {
        menu.axis = .vertical; menu.spacing = 13; menu.alignment = .fill
        menu.translatesAutoresizingMaskIntoConstraints = false
        addSubview(menu)
        NSLayoutConstraint.activate([
            menu.centerXAnchor.constraint(equalTo: safeAreaLayoutGuide.centerXAnchor),
            menu.centerYAnchor.constraint(equalTo: safeAreaLayoutGuide.centerYAnchor),
            menu.widthAnchor.constraint(lessThanOrEqualTo: safeAreaLayoutGuide.widthAnchor, multiplier: 0.85),
            menu.widthAnchor.constraint(lessThanOrEqualToConstant: 660)
        ])
        let preferredWidth = menu.widthAnchor.constraint(equalToConstant: 660)
        preferredWidth.priority = .defaultHigh; preferredWidth.isActive = true
        heading.textColor = UIColor(red: 1, green: 0.83, blue: 0.3, alpha: 1)
        heading.font = .boldSystemFont(ofSize: 34); heading.textAlignment = .center
        heading.adjustsFontSizeToFitWidth = true; heading.minimumScaleFactor = 0.5
        subheading.textColor = .white; subheading.font = .systemFont(ofSize: 15); subheading.textAlignment = .center
        menu.addArrangedSubview(heading); menu.addArrangedSubview(subheading)
        characters.selectedSegmentIndex = 0; difficulty.selectedSegmentIndex = 1
        for segment in [characters, difficulty] {
            segment.selectedSegmentTintColor = UIColor(red: 0.2, green: 0.7, blue: 0.53, alpha: 1)
            segment.backgroundColor = UIColor.white.withAlphaComponent(0.16)
            segment.setTitleTextAttributes([.foregroundColor: UIColor.white], for: .normal)
            segment.heightAnchor.constraint(equalToConstant: 34).isActive = true
            menu.addArrangedSubview(segment)
        }
        characters.accessibilityIdentifier = "characterPicker"; difficulty.accessibilityIdentifier = "difficultyPicker"
        startButton.backgroundColor = UIColor(red: 0.95, green: 0.38, blue: 0.1, alpha: 1)
        startButton.setTitleColor(.white, for: .normal); startButton.titleLabel?.font = .boldSystemFont(ofSize: 20)
        startButton.layer.cornerRadius = 14; startButton.heightAnchor.constraint(equalToConstant: 48).isActive = true
        startButton.addTarget(self, action: #selector(startTapped), for: .touchUpInside)
        startButton.accessibilityIdentifier = "startGame"
        menu.addArrangedSubview(startButton)
    }
    private func style(_ button: UIButton, title: String, identifier: String) {
        button.setTitle(title, for: .normal); button.setTitleColor(.white, for: .normal)
        button.titleLabel?.font = .boldSystemFont(ofSize: 20)
        button.backgroundColor = UIColor(red: 0.06, green: 0.14, blue: 0.23, alpha: 0.85)
        button.layer.cornerRadius = 18; button.layer.borderColor = UIColor.white.withAlphaComponent(0.4).cgColor; button.layer.borderWidth = 1
        button.accessibilityIdentifier = identifier
        button.widthAnchor.constraint(equalToConstant: 64).isActive = true
        button.heightAnchor.constraint(equalToConstant: 60).isActive = true
    }
    private lazy var bladeImage: UIImage = UIGraphicsImageRenderer(size: CGSize(width: 28, height: 28)).image { renderer in
        let c = renderer.cgContext
        c.setFillColor(UIColor.white.cgColor)
        c.move(to: CGPoint(x: 7, y: 18)); c.addLine(to: CGPoint(x: 23, y: 2)); c.addLine(to: CGPoint(x: 27, y: 1)); c.addLine(to: CGPoint(x: 25, y: 7)); c.addLine(to: CGPoint(x: 11, y: 22)); c.closePath(); c.fillPath()
        c.setStrokeColor(UIColor.white.cgColor); c.setLineWidth(3); c.setLineCap(.round)
        c.move(to: CGPoint(x: 5, y: 15)); c.addLine(to: CGPoint(x: 15, y: 24)); c.move(to: CGPoint(x: 9, y: 20)); c.addLine(to: CGPoint(x: 3, y: 26)); c.strokePath()
    }.withRenderingMode(.alwaysTemplate)
    private func swordIcon() -> UIImage { bladeImage }
    private func configureControls() {
        style(leftButton, title: "◀", identifier: "moveLeft")
        style(rightButton, title: "▶", identifier: "moveRight")
        style(jumpButton, title: "NHẢY", identifier: "jump")
        style(attackButton, title: "CHÉM", identifier: "attack")
        jumpButton.titleLabel?.font = .boldSystemFont(ofSize: 15)
        attackButton.titleLabel?.font = .boldSystemFont(ofSize: 12)
        attackButton.setImage(swordIcon(), for: .normal); attackButton.tintColor = .cyan
        attackButton.imageView?.contentMode = .scaleAspectFit
        attackButton.layer.cornerRadius = 30; attackButton.layer.borderColor = UIColor.cyan.cgColor
        attackButton.addTarget(self, action: #selector(attackDown), for: .touchDown)
        attackButton.addTarget(self, action: #selector(attackUp), for: [.touchUpInside, .touchUpOutside, .touchCancel, .touchDragExit])
        leftButton.accessibilityLabel = "Di chuyển trái"; rightButton.accessibilityLabel = "Di chuyển phải"
        jumpButton.accessibilityLabel = "Nhảy"; attackButton.accessibilityLabel = "Tấn công"
        for button in [leftButton, rightButton, jumpButton] {
            button.addTarget(self, action: #selector(controlDown(_:)), for: [.touchDown, .touchDragEnter])
            button.addTarget(self, action: #selector(controlUp(_:)), for: [.touchUpInside, .touchUpOutside, .touchCancel, .touchDragExit])
        }
        for (stack, buttons) in [(leftControls, [leftButton, rightButton]), (rightControls, [attackButton, jumpButton])] {
            stack.axis = .horizontal; stack.spacing = 16; stack.translatesAutoresizingMaskIntoConstraints = false
            buttons.forEach { stack.addArrangedSubview($0) }; addSubview(stack)
            stack.bottomAnchor.constraint(equalTo: safeAreaLayoutGuide.bottomAnchor, constant: -12).isActive = true
        }
        leftControls.leadingAnchor.constraint(equalTo: safeAreaLayoutGuide.leadingAnchor, constant: 16).isActive = true
        rightControls.trailingAnchor.constraint(equalTo: safeAreaLayoutGuide.trailingAnchor, constant: -16).isActive = true
        pauseButton.setImage(UIImage(systemName: "pause.circle.fill"), for: .normal)
        pauseButton.tintColor = .white; pauseButton.translatesAutoresizingMaskIntoConstraints = false
        pauseButton.accessibilityLabel = "Tạm dừng"; pauseButton.accessibilityIdentifier = "pauseGame"
        pauseButton.addTarget(self, action: #selector(pauseTapped), for: .touchUpInside)
        addSubview(pauseButton)
        NSLayoutConstraint.activate([pauseButton.trailingAnchor.constraint(equalTo: safeAreaLayoutGuide.trailingAnchor, constant: -8), pauseButton.topAnchor.constraint(equalTo: safeAreaLayoutGuide.topAnchor, constant: 4), pauseButton.widthAnchor.constraint(equalToConstant: 44), pauseButton.heightAnchor.constraint(equalToConstant: 44)])
        cooldown.fillColor = UIColor.clear.cgColor; cooldown.strokeColor = UIColor.systemYellow.cgColor; cooldown.lineWidth = 3
        cooldown.path = UIBezierPath(ovalIn: CGRect(x: 2, y: 2, width: 60, height: 56)).cgPath
        cooldown.strokeEnd = 0; attackButton.layer.addSublayer(cooldown)
    }
    private func clearControls() {
        engine.clearInput()
        for button in [leftButton, rightButton, jumpButton, attackButton] { button.isSelected = false; button.alpha = 1 }
    }
    @objc private func startTapped() {
        if paused { paused = false; clearControls(); lastTime = 0 }
        else if engine.state == .menu {
            engine.character = ["mario", "pink", "green"][max(0, characters.selectedSegmentIndex)]
            engine.difficulty = Difficulty.allCases[max(0, difficulty.selectedSegmentIndex)]
            engine.start()
        } else { engine.returnToMenu() }
        shownState = nil; syncInterface()
    }
    @objc private func pauseTapped() { paused.toggle(); clearControls(); lastTime = 0; syncInterface() }
    @objc private func controlDown(_ sender: UIButton) {
        guard !paused && engine.state == .playing else { return }
        sender.isSelected = true; sender.alpha = 0.65
        if sender === leftButton { engine.left = true }
        if sender === rightButton { engine.right = true }
        if sender === jumpButton { engine.jump() }
    }
    @objc private func controlUp(_ sender: UIButton) {
        sender.isSelected = false; sender.alpha = 1
        if sender === leftButton { engine.left = false }
        if sender === rightButton { engine.right = false }
        if sender === jumpButton { engine.releaseJump() }
    }
    @objc private func attackDown() { guard !paused else { return }; attackButton.alpha = 0.6; engine.attack() }
    @objc private func attackUp() { attackButton.alpha = 1 }
    private func syncInterface() {
        let inGame = engine.state == .playing
        leftControls.isHidden = !inGame || paused; rightControls.isHidden = !inGame || paused
        pauseButton.isHidden = !inGame
        menu.isHidden = !(engine.state == .menu || engine.state == .gameOver || engine.state == .cleared || paused)
        characters.isHidden = engine.state != .menu || paused
        difficulty.isHidden = characters.isHidden
        attackButton.isHidden = engine.weapon == .none
        attackButton.setTitle(engine.weapon == .gun ? " BẮN" : (engine.weapon == .rock ? " NÉM" : " CHÉM"), for: .normal)
        attackButton.setImage(engine.weapon == .sword ? swordIcon() : UIImage(systemName: engine.weapon == .gun ? "bolt.fill" : "circle.fill"), for: .normal)
        CATransaction.begin(); CATransaction.setDisableActions(true)
        cooldown.strokeEnd = engine.attackCooldown / engine.attackDuration
        CATransaction.commit()
        if paused {
            heading.text = "TẠM DỪNG"; subheading.text = "Game sẽ tiếp tục khi bạn sẵn sàng"
            startButton.setTitle("TIẾP TỤC", for: .normal)
        } else if engine.state == .menu {
            heading.text = "SUPPER MARIO"; subheading.text = "5 màn thử thách · iOS Native"
            startButton.setTitle("BẮT ĐẦU CHƠI", for: .normal)
        } else if engine.state == .gameOver || engine.state == .cleared {
            heading.text = engine.state == .cleared ? "HOÀN THÀNH 5 MÀN!" : "GAME OVER"
            subheading.text = "Điểm: \(engine.score) · Xu: \(engine.coinCount)"
            startButton.setTitle("VỀ MENU", for: .normal)
        }
        if shownState != engine.state { shownState = engine.state; if !inGame { clearControls() } }
    }
    private func image(_ name: String, in rect: CGRect, flipped: Bool = false) {
        if images[name] == nil,
           let url = Bundle.main.url(forResource: name, withExtension: "png", subdirectory: "Resources") {
            images[name] = UIImage(contentsOfFile: url.path)
        }
        guard let value = images[name], let c = UIGraphicsGetCurrentContext() else { return }
        c.saveGState()
        if flipped { c.translateBy(x: rect.midX, y: 0); c.scaleBy(x: -1, y: 1); c.translateBy(x: -rect.midX, y: 0) }
        value.draw(in: rect); c.restoreGState()
    }
    private func text(_ value: String, x: CGFloat, y: CGFloat, size: CGFloat = 30, color: UIColor = .white, centered: Bool = true) {
        let paragraph = NSMutableParagraphStyle(); paragraph.alignment = centered ? .center : .left
        (value as NSString).draw(in: CGRect(x: centered ? x - 850 : x, y: y, width: 1700, height: size * 2), withAttributes: [.font: UIFont.boldSystemFont(ofSize: size), .foregroundColor: color, .paragraphStyle: paragraph])
    }
    private func fill(_ rect: CGRect, _ color: UIColor, radius: CGFloat = 0) {
        color.setFill(); UIBezierPath(roundedRect: rect, cornerRadius: radius).fill()
    }
    override func draw(_ rect: CGRect) {
        guard let c = UIGraphicsGetCurrentContext() else { return }
        UIColor.black.setFill(); c.fill(bounds)
        let safe = bounds.inset(by: safeAreaInsets)
        let scale = min(safe.width / 1920, safe.height / 1080)
        c.saveGState(); c.translateBy(x: safe.midX - 960 * scale, y: safe.midY - 540 * scale); c.scaleBy(x: scale, y: scale)
        c.clip(to: CGRect(x: 0, y: 0, width: 1920, height: 1080))
        if engine.state == .menu {
            fill(CGRect(x: 0, y: 0, width: 1920, height: 1080), UIColor(red: 0.06, green: 0.1, blue: 0.18, alpha: 1))
            image("bg_clouds", in: CGRect(x: 0, y: 30, width: 1920, height: 500))
            image("bg_hills", in: CGRect(x: 0, y: 650, width: 1920, height: 430))
        } else if engine.state == .transition {
            text(engine.level.worldName, x: 960, y: 320, size: 70, color: .systemYellow)
            text(engine.level.challenge, x: 960, y: 440, size: 38)
            image(engine.character + "_idle", in: CGRect(x: 800, y: 570, width: 100, height: 100))
            text("× \(engine.lives)", x: 1010, y: 590, size: 50)
        } else {
            drawWorld(c)
            if paused || engine.state == .gameOver || engine.state == .cleared { fill(CGRect(x: 0, y: 0, width: 1920, height: 1080), UIColor.black.withAlphaComponent(0.75)) }
        }
        c.restoreGState()
    }
    private func drawWorld(_ c: CGContext) {
        let level = engine.level
        let value = level.skyColor
        fill(CGRect(x: 0, y: 0, width: 1920, height: 1080), UIColor(red: CGFloat((value >> 16) & 255) / 255, green: CGFloat((value >> 8) & 255) / 255, blue: CGFloat(value & 255) / 255, alpha: 1))
        for (name, factor, y) in [("bg_clouds", CGFloat(0.2), CGFloat(0)), ("bg_hills", CGFloat(0.4), CGFloat(540))] {
            let offset = -(engine.cameraX * factor).truncatingRemainder(dividingBy: 1920)
            image(name, in: CGRect(x: offset, y: y, width: 1920, height: 540))
            image(name, in: CGRect(x: offset + 1920, y: y, width: 1920, height: 540))
        }
        c.saveGState(); c.translateBy(x: -engine.cameraX, y: 0)
        for x in stride(from: max(0, Int(engine.cameraX / 70) * 70), through: min(5500, Int(engine.cameraX) + 2000), by: 70) {
            let fx = CGFloat(x)
            if !level.rivers.contains(where: { fx + 70 > $0.startX && fx < $0.endX }) {
                image("grass_top", in: CGRect(x: fx, y: 980, width: 70, height: 70))
                image("dirt_center", in: CGRect(x: fx, y: 1050, width: 70, height: 70))
            }
        }
        for river in level.rivers { image("water_top", in: CGRect(x: river.startX, y: 985, width: river.endX - river.startX, height: 95)) }
        for platform in engine.platforms { image(platform.data.isRock ? "rock" : "block_brick", in: platform.rect) }
        for cactus in engine.cacti where cactus.alive && cactus.rect.height > 0 { drawCactus(cactus, c) }
        let exits = Set(level.pipes.compactMap(\.teleportTargetX))
        for pipe in level.pipes {
            let top = 980 - pipe.height
            fill(CGRect(x: pipe.x + 10, y: top + 26, width: 90, height: pipe.height - 26), UIColor(red: 0.03, green: 0.52, blue: 0.2, alpha: 1))
            fill(CGRect(x: pipe.x + 18, y: top + 26, width: 14, height: pipe.height - 26), UIColor(red: 0.35, green: 0.85, blue: 0.37, alpha: 1))
            fill(CGRect(x: pipe.x, y: top, width: 110, height: 30), UIColor(red: 0.08, green: 0.72, blue: 0.23, alpha: 1), radius: 4)
            if pipe.teleportTargetX != nil || exits.contains(pipe.x) { text(pipe.teleportTargetX != nil ? "»" : "✓", x: pipe.x + 55, y: top + 35, size: 30, color: .systemYellow) }
        }
        for block in engine.blocks { image(block.hit ? "block_empty" : (block.data.type == "BRICK" ? "block_brick" : "block_question"), in: block.rect) }
        for (index, coin) in level.coins.enumerated() where !engine.collectedCoins.contains(index) { image("coin_gold", in: CGRect(x: coin.x, y: 980 - coin.yOffset, width: 45, height: 45)) }
        for pickup in engine.pickups { image(pickup.type == "MUSHROOM" ? "mushroom_red" : (pickup.type == "STAR" ? "star" : "coin_gold"), in: pickup.rect) }
        for enemy in engine.enemies where enemy.alive {
            if enemy.kind == .bird { drawBird(enemy, c) }
            else { image(Int(enemy.clock * 8) % 2 == 0 ? "goomba_walk_a" : "goomba_walk_b", in: enemy.rect, flipped: enemy.direction > 0) }
        }
        for shot in engine.shots {
            if shot.weapon == .sword {
                c.setStrokeColor(UIColor.cyan.cgColor); c.setLineWidth(10)
                let p = UIBezierPath(arcCenter: CGPoint(x: shot.rect.midX, y: shot.rect.midY), radius: 25, startAngle: shot.vx < 0 ? 2 : -1, endAngle: shot.vx < 0 ? 4.2 : 1, clockwise: true); p.stroke()
            } else { image(shot.weapon == .rock ? "rock" : "fireball", in: shot.rect) }
        }
        if let boss = engine.boss, !boss.defeated {
            if boss.invincibleTimer <= 0 || Int(boss.invincibleTimer * 10) % 2 == 1 { image("boss_turtle", in: engine.bossRect) }
            text("BOSS \(boss.hp)/\(boss.maxHp)" + (boss.enraged ? " !" : ""), x: engine.bossRect.midX, y: engine.bossRect.minY - 55, size: 30, color: boss.enraged ? .systemOrange : .white)
            fill(CGRect(x: level.flagpoleX - 230, y: 560, width: 12, height: 420), .systemOrange)
            text("HẠ BOSS ĐỂ MỞ CỔNG", x: level.flagpoleX - 230, y: 510, size: 27)
            if engine.arenaActive { fill(CGRect(x: level.arenaStartX - 12, y: 560, width: 12, height: 420), .systemOrange) }
        }
        fill(CGRect(x: level.flagpoleX, y: 660, width: 8, height: 320), .white)
        fill(CGRect(x: level.flagpoleX - 60, y: 680, width: 60, height: 55), .systemRed)
        if engine.invincible <= 0 || Int(engine.invincible * 10) % 2 == 1 {
            let motion = !engine.grounded ? "jump" : (abs(engine.vx) > 20 ? (Int(engine.animationTime * 10) % 2 == 0 ? "walk_a" : "walk_b") : "idle")
            image(engine.character + "_" + motion, in: engine.player, flipped: engine.facingLeft)
            if engine.weapon == .sword {
                c.saveGState(); c.translateBy(x: engine.player.midX, y: engine.player.midY); c.scaleBy(x: engine.facingLeft ? -1 : 1, y: 1)
                c.setStrokeColor(UIColor(red: 0.8, green: 0.95, blue: 1, alpha: 1).cgColor); c.setLineWidth(8)
                c.move(to: CGPoint(x: engine.player.width / 2, y: 4)); c.addLine(to: CGPoint(x: engine.player.width / 2 + 32, y: -35)); c.strokePath(); c.restoreGState()
            }
            if engine.starTimer > 0 { UIColor.systemYellow.setStroke(); UIBezierPath(ovalIn: engine.player.insetBy(dx: -8, dy: -8)).stroke() }
        }
        c.restoreGState()
        fill(CGRect(x: 0, y: 0, width: 1920, height: 100), UIColor.black.withAlphaComponent(0.35))
        for (value, x) in [("ĐIỂM \(engine.score)", CGFloat(230)), ("XU \(engine.coinCount)", 630), (level.worldName, 1000), ("GIỜ \(max(0, Int(engine.remainingTime)))", 1370), ("MẠNG \(engine.lives)", 1670)] { text(value, x: x, y: 28, size: 28) }
    }
    private func drawCactus(_ cactus: Cactus, _ c: CGContext) {
        let r = cactus.rect; let top = 980 - cactus.pipe.height
        c.saveGState(); c.clip(to: CGRect(x: cactus.pipe.x, y: top - 120, width: 110, height: 120))
        fill(CGRect(x: r.midX - 23, y: r.minY, width: 46, height: 120), UIColor(red: 0.22, green: 0.75, blue: 0.36, alpha: 1), radius: 20)
        c.setStrokeColor(UIColor(red: 0.22, green: 0.75, blue: 0.36, alpha: 1).cgColor); c.setLineWidth(13); c.setLineCap(.round)
        for side: CGFloat in [-1, 1] {
            c.move(to: CGPoint(x: r.midX + side * 20, y: r.minY + 65)); c.addLine(to: CGPoint(x: r.midX + side * 37, y: r.minY + 65)); c.addLine(to: CGPoint(x: r.midX + side * 37, y: r.minY + 35)); c.strokePath()
        }
        c.setStrokeColor(UIColor.systemYellow.cgColor); c.setLineWidth(3)
        for i in 0..<4 { for side: CGFloat in [-1, 1] {
            c.move(to: CGPoint(x: r.midX + side * 21, y: r.minY + 20 + CGFloat(i) * 23)); c.addLine(to: CGPoint(x: r.midX + side * 29, y: r.minY + 15 + CGFloat(i) * 23)); c.strokePath()
        } }
        for side: CGFloat in [-1, 1] { fill(CGRect(x: r.midX + side * 8 - 3, y: r.minY + 22, width: 6, height: 6), .black, radius: 3) }
        c.restoreGState()
    }
    private func drawBird(_ enemy: Enemy, _ c: CGContext) {
        c.saveGState(); c.translateBy(x: enemy.rect.midX, y: enemy.rect.midY); c.scaleBy(x: enemy.direction, y: 1)
        if enemy.dropTimer < 0.8 { image("rock", in: CGRect(x: -10, y: 20, width: 23, height: 23)) }
        fill(CGRect(x: -31, y: -16, width: 62, height: 35), UIColor(red: 0.22, green: 0.18, blue: 0.36, alpha: 1), radius: 18)
        let wing = UIBezierPath(); wing.move(to: CGPoint(x: -22, y: 2)); wing.addLine(to: CGPoint(x: -48, y: -12 - sin(enemy.clock * 12) * 22)); wing.addQuadCurve(to: CGPoint(x: 12, y: 0), controlPoint: CGPoint(x: -6, y: -34)); wing.close()
        UIColor(red: 0.5, green: 0.42, blue: 0.7, alpha: 1).setFill(); wing.fill()
        let beak = UIBezierPath(); beak.move(to: CGPoint(x: 26, y: -5)); beak.addLine(to: CGPoint(x: 43, y: 2)); beak.addLine(to: CGPoint(x: 26, y: 8)); beak.close(); UIColor.systemYellow.setFill(); beak.fill()
        fill(CGRect(x: 13, y: -13, width: 14, height: 14), .white, radius: 7); fill(CGRect(x: 20, y: -9, width: 6, height: 6), .black, radius: 3)
        c.restoreGState()
    }
}
