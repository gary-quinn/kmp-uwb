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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.7/KmpUwb.xcframework.zip",
            checksum: "1674c87319b557a73c4a0b928aa6878515d3f16007acd715fd7b1d0771ad18d6"
        ),
    ]
)
