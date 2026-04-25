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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.2.2/KmpUwb.xcframework.zip",
            checksum: "b397fbbbd8053aff4e6861fd4ea58ee8460cc837fc1e4dc088d94c7c379fcff0"
        ),
    ]
)
