# kmp-uwb Roadmap

> Centimetre-accurate spatial awareness for Kotlin Multiplatform — Android & iOS.

This document tracks what's shipped, what's next, and where kmp-uwb is headed. Updated as milestones are reached.

**Platforms:** Android (API 33+), iOS (15+)

> **Note:** All 0.x releases may contain breaking API changes. Pin to a specific minor version for stability. See the [CHANGELOG](CHANGELOG.md) for migration notes between versions.

---

## Shipped

### v0.1.x — Foundation

Everything needed to build production UWB ranging apps on Android and iOS from shared Kotlin code.

| Feature | Version | Details |
|---------|---------|---------|
| **Ranging Sessions** | v0.1.0 | Peer-to-peer TWR (Two-Way Ranging) via `RangingSession` interface. Controller and Controlee roles |
| **Distance & Angle of Arrival** | v0.1.0 | `Distance` (meters/cm), `Angle` (degrees/radians) value classes with unit conversion |
| **10-State State Machine** | v0.1.0 | Exhaustive sealed interface FSM — Idle, Starting, Active, Stopped with sub-states |
| **Composable Error Hierarchy** | v0.1.0 | Sealed interfaces — `SessionError`, `RangingError`, `HardwareError`, `SecurityError`. Errors can implement multiple facets |
| **Adapter & Capabilities** | v0.1.0 | `UwbAdapter` with state observation and hardware capability queries (roles, AoA, channels, background) |
| **Session Configuration** | v0.1.0 | `RangingConfig` DSL builder — role, channel, STS mode (Static/Dynamic/Provisioned), AoA, ranging interval |
| **Testing Infrastructure** | v0.1.0 | `FakeRangingSession`, `FakeUwbAdapter` — full UWB simulation for unit tests, no hardware required |
| **Android Auto-Init** | v0.1.0 | AndroidX Startup — zero configuration for consumers |
| **Distribution** | v0.1.0 | Maven Central (`com.atruedev:kmp-uwb`) + Swift Package Manager (XCFramework) |
| **CI/CD Pipeline** | v0.1.0 | GitHub Actions — ktlint, Android build/test, iOS build/test, Dokka docs, Maven Central publish, GitHub Release |

### Known Limitations

- Desktop (JVM), Web, and other platforms are not yet supported
- iOS requires U1/U2 chip (iPhone 11+, Apple Watch Series 6+)
- Android requires `FEATURE_UWB` system feature and `androidx.core.uwb` alpha API
- Background ranging behaviour varies by platform

---

## Planned

### v0.2 — Multi-Peer & Session Management

**Theme:** Real-world ranging scenarios with multiple devices.

| Feature | Notes |
|---------|-------|
| **Multi-peer ranging** | Range to multiple peers simultaneously within a single session |
| **Session recovery** | Auto-resume after system suspension (iOS background) |
| **Peer discovery** | OOB (Out-of-Band) peer exchange helpers — integrate with kmp-ble or NFC for session bootstrapping |
| **Session events Flow** | Dedicated event stream for session-level events (peer joined, peer left, suspension, resume) |
| **Ranging filters** | Distance smoothing, outlier rejection, moving average |

### v0.3 — Spatial Awareness

**Theme:** Beyond distance — direction, zones, and spatial context.

| Feature | Notes |
|---------|-------|
| **3D positioning** | Combine distance + azimuth + elevation into position coordinates |
| **Proximity zones** | Configurable distance zones (immediate/near/far) with hysteresis |
| **Heading estimation** | Device-relative heading from AoA data |
| **Accuracy metrics** | Confidence intervals and NLOS (Non-Line-of-Sight) detection |

### v1.0 — Stability Guarantee

**Theme:** API stability commitment backed by production usage.

**Criteria:**

API stability:
- Core module API (`UwbAdapter`, `RangingSession`, `RangingConfig`) unchanged for 2+ minor releases
- Deprecation cycle enforced: deprecated APIs survive at least 1 minor release before removal
- All public APIs have KDoc documentation

Quality:
- Zero known critical bugs at release time
- Test coverage for all public API entry points
- CI green on both Android and iOS targets

Distribution:
- Semantic versioning strictly followed from v1.0 onward
- CHANGELOG.md covers every release

---

## Future Considerations (v1.x+)

Features we're tracking but not actively working on. Community interest and use cases will determine priority.

**Inclusion here does not imply commitment.** These items may be reprioritized, redesigned, or dropped based on real-world demand.

| Feature | Notes |
|---------|-------|
| FiRa 2.0 features | Data transfer over UWB, enhanced security modes |
| Secure ranging | End-to-end session key management with STS provisioning |
| Indoor positioning | Map-based positioning using multiple anchors |
| UWB + BLE fusion | Combined kmp-ble + kmp-uwb session management for discovery → ranging workflows |
| Android CCC Digital Key | Car Connectivity Consortium profile support |
| Geofencing | UWB-based precision geofences (sub-meter accuracy) |
| Record-replay testing | Record real UWB sessions and replay for offline testing |
| Additional FiRa profiles | Point-to-multipoint, device-to-infrastructure |
| wasm / JS target | If Web UWB APIs emerge from standards bodies |

---

## How to Influence This Roadmap

- **Feature requests:** [Open an issue](https://github.com/gary-quinn/kmp-uwb/issues) describing your use case
- **Bug reports:** Include platform, device model, and minimal reproduction
- **Contributions:** PRs welcome — see the issue tracker for "good first issue" labels

---

*Current as of v0.1.0*
