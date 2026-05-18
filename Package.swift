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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.2.3/KmpUwb.xcframework.zip",
            checksum: "b40d6610c5f7292003e888f434013b226a3651c9698060e574aa27c8dbb2d7fb"
        ),
    ]
)
