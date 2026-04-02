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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.5/KmpUwb.xcframework.zip",
            checksum: "25189665a0e62079384bd5c8e613e5ad38be65c88916ef61aec6f88038df4c00"
        ),
    ]
)
