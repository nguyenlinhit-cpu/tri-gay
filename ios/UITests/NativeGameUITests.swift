import XCTest

final class NativeGameUITests: XCTestCase {
    override func setUpWithError() throws { continueAfterFailure = false; XCUIDevice.shared.orientation = .landscapeLeft }
    func testMenuMovementJumpAndPause() {
        let app = XCUIApplication(); app.launch()
        XCTAssertTrue(app.segmentedControls["characterPicker"].waitForExistence(timeout: 10))
        app.buttons["startGame"].tap()
        XCTAssertTrue(app.buttons["moveRight"].waitForExistence(timeout: 8))
        app.buttons["moveRight"].press(forDuration: 0.25)
        app.buttons["jump"].press(forDuration: 0.2)
        app.buttons["pauseGame"].tap()
        XCTAssertTrue(app.buttons["startGame"].exists)
        XCTAssertEqual(app.buttons["startGame"].label, "TIẾP TỤC")
        app.buttons["startGame"].tap()
        XCTAssertTrue(app.buttons["moveLeft"].exists)
        let attachment = XCTAttachment(screenshot: app.screenshot()); attachment.name = "Native iPhone gameplay"; attachment.lifetime = .keepAlways; add(attachment)
        app.terminate()
    }
    func testAllFiveLevelsOpenWithNativeControls() {
        for level in 1...5 {
            let app = XCUIApplication(); app.launchArguments = ["--smoke-level", "\(level)"]; app.launch()
            XCTAssertTrue(app.buttons["jump"].waitForExistence(timeout: 8), "Level \(level) did not open")
            XCTAssertTrue(app.buttons["pauseGame"].exists)
            let attachment = XCTAttachment(screenshot: app.screenshot()); attachment.name = "Level \(level)"; attachment.lifetime = .keepAlways; add(attachment)
            app.terminate()
        }
    }
}
