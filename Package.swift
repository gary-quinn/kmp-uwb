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
            url: "https://github.com/gary-quinn/kmp-uwb/releases/download/v0.1.4/KmpUwb.xcframework.zip",
            checksum: "9be9b34c4ece53ef3c8a85222d63a12d156dd64a2003e73147cbeb6659ae5fe3"
        ),
    ]
)
