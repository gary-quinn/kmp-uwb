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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.2/KmpUwb.xcframework.zip",
            checksum: "5572faf325c8154e55f0e079a35bea6ca853be8083c461892edd82314da94348"
        ),
    ]
)
