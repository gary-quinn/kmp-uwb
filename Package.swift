// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "KmpUwb",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "KmpUwb", targets: ["KmpUwb"]),
    ],
    targets: [
        .binaryTarget(
            name: "KmpUwb",
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.0-alpha2/KmpUwb.xcframework.zip",
            checksum: "f88ee1278cba36ee537221807af0252b03b29808e11e73d6300f9bdc89e488fb"
        ),
    ]
)
