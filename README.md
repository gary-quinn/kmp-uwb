# kmp-uwb

[![CI](https://github.com/gary-quinn/kmp-uwb/actions/workflows/ci.yml/badge.svg)](https://github.com/gary-quinn/kmp-uwb/actions/workflows/ci.yml)
[![Publish](https://github.com/gary-quinn/kmp-uwb/actions/workflows/publish.yml/badge.svg)](https://github.com/gary-quinn/kmp-uwb/actions/workflows/publish.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.atruedev/kmp-uwb)](https://central.sonatype.com/artifact/com.atruedev/kmp-uwb)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-purple.svg)](https://kotlinlang.org)

Kotlin Multiplatform UWB (Ultra-Wideband) library for Android and iOS.

Part of the **kmp** library family alongside [kmp-ble](https://github.com/gary-quinn/kmp-ble). Use kmp-uwb for centimetre-accurate spatial awareness — device ranging, angle-of-arrival, and peer tracking — with the same shared-code-first philosophy.

## Features

| Capability | Description |
|------------|-------------|
| **Ranging Sessions** | Start peer-to-peer TWR (Two-Way Ranging) sessions with real-time distance and angle measurements |
| **Angle of Arrival** | Azimuth and elevation from the device's UWB antenna, when hardware supports it |
| **Adapter State** | Observe hardware availability and query device capabilities |
| **Session Lifecycle** | 10-state state machine with exhaustive transitions — no ambiguous states |
| **Composable Errors** | Sealed interface hierarchy — errors can implement multiple facets (`SessionError + HardwareError`) |
| **Test Without Hardware** | `FakeRangingSession` and `FakeUwbAdapter` for full UWB simulation in unit tests |
| **FiRa Compliant** | Static, Dynamic, and Provisioned STS security modes. Controller/Controlee roles |

## Setup

### Android / KMP (Gradle)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("com.atruedev:kmp-uwb:0.1.0-alpha2")
        }
    }
}
```

The library auto-initializes on Android via [AndroidX Startup](https://developer.android.com/topic/libraries/app-startup) — no manual `init()` call needed. If your app disables auto-initialization, call `KmpUwb.init(context)` manually.

### iOS (Swift Package Manager)

In Xcode: **File > Add Package Dependencies** and enter:

```
https://github.com/gary-quinn/kmp-uwb
```

Select the version and add `KmpUwb` to your target.

```swift
import KmpUwb
```

> **Note:** UWB requires a U1 or U2 chip. On iOS, the `NearbyInteraction` framework is only available on iPhone 11+ and Apple Watch Series 6+.

## Usage

### Check UWB availability

```kotlin
val adapter = UwbAdapter()

adapter.state.collect { state ->
    when (state) {
        UwbAdapterState.ON -> println("UWB ready")
        UwbAdapterState.OFF -> println("UWB disabled")
        UwbAdapterState.UNSUPPORTED -> println("No UWB hardware")
    }
}
```

### Query capabilities

```kotlin
val capabilities = adapter.capabilities()
println("Roles: ${capabilities.supportedRoles}")
println("AoA: ${capabilities.angleOfArrivalSupported}")
println("Channels: ${capabilities.supportedChannels}")
println("Background: ${capabilities.backgroundRangingSupported}")
```

### Configure a session

```kotlin
val config = rangingConfig {
    role = RangingRole.CONTROLLER
    channel = 9
    stsMode = StsMode.DYNAMIC
    angleOfArrival = true
    rangingInterval = 200.milliseconds
}
```

### Start ranging

```kotlin
val session = RangingSession(config)
val peer = Peer(address = PeerAddress(peerAddressBytes))

session.start(peer)

// Observe state
session.state.collect { state ->
    when (state) {
        is RangingState.Active.Ranging -> println("Ranging active")
        is RangingState.Active.Suspended -> println("Session suspended by system")
        is RangingState.Active.PeerLost -> println("Peer out of range")
        is RangingState.Stopped.ByError -> println("Error: ${state.error}")
        else -> {}
    }
}
```

### Collect measurements

```kotlin
session.rangingResults.collect { result ->
    when (result) {
        is RangingResult.Position -> {
            println("Distance: ${result.measurement.distance}")       // e.g. 1.23 m
            println("Azimuth: ${result.measurement.azimuth}")         // horizontal angle
            println("Elevation: ${result.measurement.elevation}")     // vertical angle
        }
        is RangingResult.PeerLost -> println("Lost: ${result.peer}")
        is RangingResult.PeerRecovered -> println("Recovered: ${result.peer}")
    }
}

// Clean up
session.close()
```

### Test without hardware

```kotlin
val fakeSession = FakeRangingSession(
    config = rangingConfig { role = RangingRole.CONTROLLER },
)
val peer = Peer(address = PeerAddress(byteArrayOf(0x01, 0x02)))

fakeSession.start(peer)

// Inject a measurement
fakeSession.emitResult(
    RangingResult.Position(
        peer = peer,
        measurement = RangingMeasurement(
            distance = Distance.meters(2.5),
            azimuth = Angle.degrees(15.0),
            elevation = Angle.degrees(-5.0),
        ),
    )
)

// Simulate error
fakeSession.simulateError(SessionLost(message = "connection dropped"))
```

## Relationship with kmp-ble

kmp-uwb and kmp-ble are **independent libraries** with no compile-time dependency. They share the same design philosophy:

| | kmp-ble | kmp-uwb |
|---|---|---|
| **Technology** | Bluetooth Low Energy | Ultra-Wideband |
| **Range** | ~100 m | ~10 m (centimetre-accurate) |
| **Use cases** | Data transfer, device control, firmware updates | Spatial awareness, precision ranging, indoor positioning |
| **State model** | 14-state connection FSM | 10-state ranging FSM |
| **Error model** | Composable sealed interfaces | Composable sealed interfaces |
| **Testing** | FakePeripheral, FakeScanner | FakeRangingSession, FakeUwbAdapter |

A typical spatial app might use kmp-ble for device discovery and data exchange, then kmp-uwb for precise positioning — but neither requires the other.

## Architecture

- **State machine:** 10 states with sealed interface hierarchy — exhaustive `when` branches
- **Per-session concurrency:** `limitedParallelism(1)` serialization, no locks
- **Hot Flow ranging:** `Channel(64, DROP_OLDEST)` — consumers always see the latest measurement
- **Value classes:** `Distance` and `Angle` are zero-allocation wrappers with unit conversion
- **Composable errors:** Sealed interfaces — `SessionError`, `RangingError`, `HardwareError`, `SecurityError`
- **Defensive copies:** `PeerAddress` copies on construction and access — no aliasing bugs

See [ARCHITECTURE.md](ARCHITECTURE.md) for full design documentation.

## Requirements

- Kotlin 2.3.0+
- Android minSdk 33
- iOS 15+ (U1/U2 chip required for UWB)
- kotlinx-coroutines 1.10+

## License

[Apache 2.0](LICENSE) — Copyright (C) 2026 Huynh Thien Thach
