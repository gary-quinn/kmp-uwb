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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.3/KmpUwb.xcframework.zip",
            checksum: "140627120a187679b5cd22d65b32916b42bf9d3d9551b7c45ef9a37098e72637"
        ),
    ]
)
