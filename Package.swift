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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.2.0/KmpUwb.xcframework.zip",
            checksum: "7c7c40d531a41e3b6f384720986cc5105357b72bacd56c8504b0da2e3f915aeb"
        ),
    ]
)
