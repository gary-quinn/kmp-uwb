// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "KmpUwb",
    platforms: [.iOS(.v15)],
    products: [
        .library(name: "KmpUwb", targets: ["KmpUwb"]),
    ],
    targets: [
        // Placeholder — updated automatically by the publish workflow
        .binaryTarget(
            name: "KmpUwb",
            path: "KmpUwb.xcframework"
        ),
    ]
)
