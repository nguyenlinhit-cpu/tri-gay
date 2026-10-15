import CoreGraphics
import Foundation

public enum Difficulty: String, CaseIterable, Codable {
    case easy = "EASY", medium = "MEDIUM", hard = "HARD"
    public var title: String { switch self { case .easy: return "DỄ"; case .medium: return "VỪA"; case .hard: return "KHÓ" } }
    public var lives: Int { switch self { case .easy: return 5; case .medium: return 3; case .hard: return 1 } }
}
public enum Weapon: String, Codable { case none = "NONE", gun = "GUN", rock = "ROCK_THROW", sword = "SWORD" }
public enum PlayState { case menu, transition, playing, gameOver, cleared }
public struct Rules: Decodable {
    public let enemySpeed, hazardTempo, platformSpeed, timeScale: CGFloat
    public let bossHp: Int
    public let bossShotInterval, bossShotSpeed: CGFloat
}
public struct PipeData: Decodable { public let x, height: CGFloat; public let teleportTargetX: CGFloat? }
public struct BlockData: Decodable { public let x, yOffset: CGFloat; public let type: String; public let powerUpType: String?; public let weaponType: Weapon? }
public struct CoinData: Decodable { public let x, yOffset: CGFloat }
public struct GoombaData: Decodable { public let x, minX, maxX, speed: CGFloat }
public struct BirdData: Decodable { public let x, minX, maxX, height: CGFloat }
public struct RiverData: Decodable { public let startX, endX: CGFloat }
public struct PlatformData: Decodable { public let x, yOffset, width, minX, maxX, speed: CGFloat; public let isRock: Bool }
public struct Level: Decodable {
    public let worldName: String
    public let skyColor: UInt32
    public let timeLimit: Int
    public let pipes: [PipeData], blocks: [BlockData], coins: [CoinData]
    public let goombas: [GoombaData], birds: [BirdData], rivers: [RiverData], platforms: [PlatformData]
    public let isBossLevel: Bool
    public let bossX, flagpoleX, arenaStartX: CGFloat
    public let challenge: String
}
public struct LevelSet: Decodable { public let rules: Rules; public let levels: [Level] }
public enum LevelLibrary {
    public static func decode(_ data: Data) throws -> [Difficulty: LevelSet] {
        let values = try JSONDecoder().decode([String: LevelSet].self, from: data)
        return Dictionary(uniqueKeysWithValues: try Difficulty.allCases.map { difficulty in
            guard let value = values[difficulty.rawValue], value.levels.count == 5 else {
                throw NSError(domain: "LevelLibrary", code: 1, userInfo: [NSLocalizedDescriptionKey: "Missing levels for \(difficulty.rawValue)"])
            }
            return (difficulty, value)
        })
    }
}
public struct CactusCycle {
    public private(set) var clock: CGFloat
    public let tempo: CGFloat
    public init(tempo: CGFloat, phase: CGFloat = 0) { self.tempo = tempo; clock = phase }
    public var extensionAmount: CGFloat {
        if clock < 1.6 { return 0 }
        if clock < 2.4 { return (clock - 1.6) / 0.8 }
        if clock < 4 { return 1 }
        if clock < 4.8 { return 1 - (clock - 4) / 0.8 }
        return 0
    }
    public mutating func update(_ dt: CGFloat, playerNear: Bool) {
        clock = clock < 1.6 && playerNear ? 0 : (clock + dt * tempo).truncatingRemainder(dividingBy: 5.2)
    }
}
public final class BossCombat {
    public let maxHp: Int
    public private(set) var hp: Int
    public private(set) var invincibleTimer: CGFloat = 0
    private var shotTimer: CGFloat = 0
    private let interval: CGFloat
    public init(hp: Int, interval: CGFloat) { maxHp = hp; self.hp = hp; self.interval = interval }
    public var defeated: Bool { hp <= 0 }
    public var enraged: Bool { hp <= maxHp / 2 && !defeated }
    public var shotInterval: CGFloat { interval * (enraged ? 0.72 : 1) }
    @discardableResult public func takeDamage(_ amount: Int = 1) -> Bool {
        guard amount > 0, invincibleTimer <= 0, !defeated else { return false }
        hp = max(0, hp - amount); invincibleTimer = 0.55; return true
    }
    public func update(_ dt: CGFloat, active: Bool) {
        invincibleTimer = max(0, invincibleTimer - dt)
        if active && !defeated { shotTimer += dt }
    }
    public func consumeShot() -> Bool {
        guard !defeated, shotTimer >= shotInterval else { return false }
        shotTimer = 0; return true
    }
}
public final class MovingPlatform {
    public let data: PlatformData
    public var rect: CGRect
    private var direction: CGFloat = 1
    public init(_ data: PlatformData) {
        self.data = data; rect = CGRect(x: data.x, y: GameEngine.groundY - data.yOffset, width: data.width, height: 40)
    }
    public func update(_ dt: CGFloat) -> CGFloat {
        let oldX = rect.minX
        if data.speed > 0 {
            rect.origin.x += direction * data.speed * dt
            if rect.minX < data.minX { rect.origin.x = data.minX; direction = 1 }
            if rect.maxX > data.maxX { rect.origin.x = data.maxX - rect.width; direction = -1 }
        }
        return rect.minX - oldX
    }
}
public final class Cactus {
    public let pipe: PipeData
    public var cycle: CactusCycle
    public var alive = true
    public init(_ pipe: PipeData, tempo: CGFloat, phase: CGFloat) { self.pipe = pipe; cycle = CactusCycle(tempo: tempo, phase: phase) }
    public var rect: CGRect { CGRect(x: pipe.x + 26, y: GameEngine.groundY - pipe.height - 104 * cycle.extensionAmount, width: 58, height: 104 * cycle.extensionAmount) }
    public var dangerous: Bool { alive && cycle.extensionAmount > 0.12 }
}
public final class Enemy {
    public enum Kind { case goomba, bird }
    public let kind: Kind
    public var rect: CGRect
    public let minX, maxX, speed, baseY: CGFloat
    public var direction: CGFloat = 1
    public var clock: CGFloat = 0
    public var dropTimer: CGFloat = 1.8
    public var alive = true
    public init(_ data: GoombaData) {
        kind = .goomba; minX = data.minX; maxX = data.maxX - 70; speed = data.speed; baseY = GameEngine.groundY - 70
        rect = CGRect(x: data.x, y: baseY, width: 70, height: 70)
    }
    public init(_ data: BirdData, tempo: CGFloat) {
        kind = .bird; minX = data.minX; maxX = data.maxX; speed = 95 * tempo; baseY = GameEngine.groundY - data.height
        rect = CGRect(x: data.x, y: baseY, width: 72, height: 52)
    }
}
public final class BlockState {
    public let data: BlockData
    public var hit = false
    public var rect: CGRect { CGRect(x: data.x, y: GameEngine.groundY - data.yOffset, width: 75, height: 75) }
    public init(_ data: BlockData) { self.data = data }
}
public final class Shot {
    public var rect: CGRect
    public var vx, vy, gravity: CGFloat
    public let friendly: Bool
    public let weapon: Weapon
    public var alive = true
    public init(x: CGFloat, y: CGFloat, vx: CGFloat, vy: CGFloat = 0, gravity: CGFloat = 0, friendly: Bool, weapon: Weapon) {
        let size: CGFloat = weapon == .sword ? 50 : 35
        rect = CGRect(x: x, y: y, width: size, height: size)
        self.vx = vx; self.vy = vy; self.gravity = gravity; self.friendly = friendly; self.weapon = weapon
    }
}
public final class Pickup {
    public var rect: CGRect
    public let type: String
    public let targetY: CGFloat
    public var alive = true
    public init(x: CGFloat, y: CGFloat, type: String) {
        rect = CGRect(x: x, y: y, width: 60, height: 60); targetY = y - 60; self.type = type
    }
}
