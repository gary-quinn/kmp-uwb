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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.6/KmpUwb.xcframework.zip",
            checksum: "6e2b22fd452a65b3c5bc2438f5d4eb3a9edc18be431cf03cd0e1f423a8a251d9"
        ),
    ]
)
