// swift-tools-version: 5.9
import PackageDescription

let package = Package(
    name: "GameCore",
    platforms: [.macOS(.v12), .iOS(.v15)],
    products: [.library(name: "GameCore", targets: ["GameCore"])],
    targets: [
        .target(name: "GameCore"),
        .testTarget(name: "GameCoreTests", dependencies: ["GameCore"], resources: [.copy("Fixtures")])
    ]
)
