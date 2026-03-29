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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.0-alpha1/KmpUwb.xcframework.zip",
            checksum: "a5b6d9595856c89e6c621a9ff35fbc1ad4730fbbb28064190557e79c104da2c6"
        ),
    ]
)
