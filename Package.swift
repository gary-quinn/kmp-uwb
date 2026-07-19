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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.2.4/KmpUwb.xcframework.zip",
            checksum: "9c9c0f51b98c6ee704be7e17307fb55c5b2de455134e009af3ee70017cb36348"
        ),
    ]
)
