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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.2.1/KmpUwb.xcframework.zip",
            checksum: "3c7c9f8b45cc3fa6b205ed88b0485e82da36f83d0f8436fa8c7c3ef80007fceb"
        ),
    ]
)
