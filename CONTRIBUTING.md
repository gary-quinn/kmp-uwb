# Contributing to kmp-uwb

## Versioning

This project follows [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

### Pre-1.0 (current)

| Bump | Criteria | Example |
|------|----------|---------|
| **Patch** (0.1.x -> 0.1.y) | Bug fixes, documentation, CI changes. No public API changes | Fix crash in iOS session delegate |
| **Minor** (0.1.x -> 0.2.0) | New features, deprecations, non-breaking API additions | Add `BackpressureStrategy`, new `RangingResult` variant |
| **Breaking** (0.x -> 0.y) | Removed or renamed public API, changed signatures | Remove deprecated `start()` method |

Breaking changes are documented in the CHANGELOG with migration instructions.

### Post-1.0

| Bump | Criteria |
|------|----------|
| **Patch** (1.0.x) | Bug fixes only, no API changes |
| **Minor** (1.x.0) | Backwards-compatible additions and deprecations |
| **Major** (x.0.0) | Breaking changes. Deprecated APIs removed after surviving at least 1 minor release |

## Release Process

1. Changes land on `main` via pull request
2. CI validates: ktlint, Android build/test, iOS build/test
3. Tag `vX.Y.Z` triggers publish to Maven Central and GitHub Releases
4. iOS XCFramework and `Package.swift` are updated automatically

## Development

### Prerequisites

- JDK 17+
- Android SDK (API 33+)
- Xcode 15+ (for iOS targets)

### Build

```bash
./gradlew build
```

### Test

```bash
# Common + JVM + Android host tests
./gradlew check

# iOS tests (requires macOS + Xcode)
./gradlew iosSimulatorArm64Test

# Android instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Code Style

This project uses [ktlint](https://pinterest.github.io/ktlint/). The CI enforces formatting.

```bash
./gradlew ktlintCheck    # verify
./gradlew ktlintFormat   # auto-fix
```

## Pull Requests

- One logical change per PR
- Include tests for new functionality
- Update CHANGELOG.md under `[Unreleased]`
- Keep commit messages concise: `type(scope): description` (e.g., `feat(config): add BackpressureStrategy`)
