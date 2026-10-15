import CoreGraphics
import XCTest
@testable import GameCore

final class GameCoreTests: XCTestCase {
    func library() throws -> [Difficulty: LevelSet] {
        let url = try XCTUnwrap(Bundle.module.url(forResource: "levels", withExtension: "json", subdirectory: "Fixtures"))
        return try LevelLibrary.decode(Data(contentsOf: url))
    }
    func play(_ level: Int = 0) throws -> GameEngine {
        let game = GameEngine(library: try library()); game.start(at: level)
        for _ in 0..<190 { game.advance(1.0 / 60) }
        XCTAssertEqual(game.state, .playing)
        return game
    }
    func run(_ game: GameEngine, seconds: Double, fps: Int = 60) {
        for _ in 0..<Int(seconds * Double(fps)) { game.advance(1.0 / Double(fps)) }
    }
    func testAllFiveLevelsAndThreeModesDecode() throws {
        let values = try library()
        for mode in Difficulty.allCases {
            let set = try XCTUnwrap(values[mode])
            XCTAssertEqual(set.levels.count, 5)
            XCTAssertTrue(set.levels.allSatisfy { !$0.challenge.isEmpty && $0.flagpoleX == 5200 })
            XCTAssertTrue(set.levels.last!.isBossLevel)
            XCTAssertTrue(set.levels.allSatisfy { $0.timeLimit >= 160 })
        }
        XCTAssertLessThan(values[.easy]!.rules.bossHp, values[.hard]!.rules.bossHp)
        XCTAssertGreaterThan(values[.easy]!.rules.bossShotInterval, values[.hard]!.rules.bossShotInterval)
    }
    func testFixedStepMovementMatchesAcrossDisplayRefreshRates() throws {
        var positions = [CGFloat]()
        for fps in [30, 60, 120] {
            let game = try play(); game.right = true; run(game, seconds: 0.5, fps: fps)
            positions.append(game.player.minX)
        }
        XCTAssertLessThan(positions.max()! - positions.min()!, 4)
    }
    func testJumpHeightAndEarlyRelease() throws {
        let held = try play(); let released = try play()
        held.jump(); released.jump(); released.releaseJump()
        var high = held.player.minY; var low = released.player.minY
        for _ in 0..<70 {
            held.advance(1.0 / 120); released.advance(1.0 / 120)
            high = min(high, held.player.minY); low = min(low, released.player.minY)
        }
        XCTAssertLessThan(high, low - 80)
        XCTAssertEqual(held.player.width, 80)
        XCTAssertGreaterThanOrEqual(high, 680)
    }
    func testWalkingIntoPipeStopsOnItsWall() throws {
        let game = try play(4); game.player.origin.x = 690; game.right = true
        run(game, seconds: 0.8)
        XCTAssertEqual(game.player.maxX, 810, accuracy: 0.1)
        XCTAssertEqual(game.lives, 3)
    }
    func testRiverCostsOneLifeAndReloadsLevel() throws {
        let game = try play(); game.player.origin.x = 1800
        game.advance(1.0 / 60)
        XCTAssertEqual(game.lives, 2); XCTAssertEqual(game.state, .transition)
        XCTAssertEqual(game.player.minX, 100)
    }
    func testNoMidairJumpAfterWalkingOffIsland() throws {
        let game = try play()
        game.player = CGRect(x: 2010, y: 790, width: 80, height: 80)
        game.right = true; run(game, seconds: 0.25)
        XCTAssertFalse(game.grounded)
        let fallingVelocity = game.vy; game.jump()
        XCTAssertEqual(game.vy, fallingVelocity)
    }
    func testJumpCanLandOnFirstStoneWithoutPowerups() throws {
        let game = try play(1)
        game.player.origin.x = 900; game.right = true; game.jump()
        run(game, seconds: 0.6); game.right = false; run(game, seconds: 0.55)
        XCTAssertEqual(game.lives, 3)
        XCTAssertTrue(game.grounded)
        XCTAssertEqual(game.player.maxY, 870, accuracy: 1)
    }
    func testTeleportArrivesAboveSafeExitWithoutCactus() throws {
        let game = try play(3)
        game.player = CGRect(x: 1135, y: 730, width: 80, height: 80)
        game.advance(1.0 / 60)
        XCTAssertEqual(game.player.minX, 2405, accuracy: 1)
        XCTAssertLessThan(game.player.maxY, 810)
        XCTAssertFalse(game.cacti.contains { $0.pipe.x == 2390 || $0.pipe.x == 1120 })
        XCTAssertEqual(game.lives, 3)
    }
    func testArenaLocksFlagAndSuppliesFallbackSword() throws {
        let game = try play(4)
        game.player.origin.x = 5100; game.advance(1.0 / 60)
        XCTAssertTrue(game.arenaActive); XCTAssertEqual(game.weapon, .sword)
        XCTAssertLessThanOrEqual(game.player.maxX, 4970)
        XCTAssertEqual(game.state, .playing)
        let boss = try XCTUnwrap(game.boss)
        for _ in 0..<boss.maxHp { boss.takeDamage(); boss.update(0.56, active: false) }
        XCTAssertTrue(boss.defeated)
        game.player.origin.x = 5200; game.advance(1.0 / 60)
        XCTAssertEqual(game.state, .cleared)
    }
    func testBossHasActivationGateDamageCooldownAndSecondPhase() {
        let boss = BossCombat(hp: 10, interval: 1.7)
        boss.update(30, active: false); XCTAssertFalse(boss.consumeShot())
        XCTAssertTrue(boss.takeDamage()); XCTAssertFalse(boss.takeDamage())
        for _ in 0..<4 { boss.update(0.56, active: false); boss.takeDamage() }
        XCTAssertTrue(boss.enraged); XCTAssertLessThan(boss.shotInterval, 1.7)
        boss.update(2, active: true); XCTAssertTrue(boss.consumeShot()); XCTAssertFalse(boss.consumeShot())
    }
    func testHiddenCactusWaitsForPlayerAndRetreatStillFinishes() {
        var cycle = CactusCycle(tempo: 1)
        for _ in 0..<600 { cycle.update(1.0 / 60, playerNear: true) }
        XCTAssertEqual(cycle.extensionAmount, 0)
        for _ in 0..<180 { cycle.update(1.0 / 60, playerNear: false) }
        XCTAssertEqual(cycle.extensionAmount, 1, accuracy: 0.01)
        for _ in 0..<180 { cycle.update(1.0 / 60, playerNear: true) }
        XCTAssertEqual(cycle.extensionAmount, 0)
    }
    func testTimerAndPauseDoNotAdvanceOutsidePlayingState() throws {
        let game = GameEngine(library: try library())
        game.advance(10); XCTAssertEqual(game.state, .menu)
        game.start(); let time = game.remainingTime
        run(game, seconds: 1)
        XCTAssertEqual(game.remainingTime, time)
        XCTAssertEqual(game.state, .transition)
    }
}
