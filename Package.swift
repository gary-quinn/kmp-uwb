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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.1/KmpUwb.xcframework.zip",
            checksum: "ff2489ed51e15347d0447eb15e640b09da6bce3a8f9b0b3a6e90da1d4725a8b7"
        ),
    ]
)
