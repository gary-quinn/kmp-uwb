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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.0/KmpUwb.xcframework.zip",
            checksum: "24490fd693ed22846d9fec307611acf82051b4aeec19357167c075bd3016b365"
        ),
    ]
)
