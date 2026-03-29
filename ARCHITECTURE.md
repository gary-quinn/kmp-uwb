# kmp-uwb Architecture

This document explains the key design decisions and internal structure of kmp-uwb. It's intended for contributors and anyone curious about how the library works under the hood.

---

## Overview

kmp-uwb is a Kotlin Multiplatform UWB library targeting Android and iOS. The core design principle is: **shared logic in `commonMain`, platform bridges in `expect/actual`, no platform details leaking into the public API.**

```
commonMain (shared)
├── Public API (interfaces, sealed types, value classes)
├── expect declarations (UwbAdapter, RangingSession)
└── Test doubles (FakeRangingSession, FakeUwbAdapter)

androidMain
├── actual UwbAdapter  → androidx.core.uwb
└── actual RangingSession → UwbManager + RangingParameters

iosMain
├── actual UwbAdapter  → NearbyInteraction availability checks
└── actual RangingSession → NISession + NISessionDelegate

jvmMain
├── actual UwbAdapter  → always UNSUPPORTED
└── actual RangingSession → throws at construction
```

Consumers write against the common API. Platform code never appears in import statements.

---

## State Machine

The ranging state machine tracks every session through 10 states with an exhaustive sealed interface hierarchy.

### States

```
RangingState
├── Idle
│   ├── Ready              — Session can be started
│   └── Unsupported        — Hardware not available
├── Starting
│   ├── Negotiating        — Exchanging session parameters with peer
│   └── Initializing       — Platform session being created
├── Active
│   ├── Ranging            — Measurements flowing
│   ├── Suspended          — Temporarily paused by system (iOS background)
│   └── PeerLost           — Peer left range, session still alive
└── Stopped
    ├── ByRequest          — Local close() called
    ├── ByPeer             — Remote peer ended session
    ├── ByError(error)     — Session failed with details
    └── BySystemEvent      — UWB disabled, airplane mode
```

### Transition Rules

States follow a strict forward progression: `Idle → Starting → Active → Stopped`. Within `Active`, lateral transitions are allowed (`Ranging ↔ Suspended ↔ PeerLost`). Once in `Stopped`, no further transitions occur — the session is terminal.

Transitions are enforced by the `check()` precondition in `start()` and the `if (_state.value !is RangingState.Stopped)` guard in `close()`. A session that stops due to error cannot be overwritten by a subsequent `close()`.

### Why 10 States?

Other approaches collapse this into ~3 states (Idle, Active, Stopped). This makes it impossible to distinguish:

- "Session is negotiating parameters" vs "platform session is initializing"
- "Peer is temporarily out of range" vs "session was suspended by the OS"
- "I closed the session" vs "the peer disconnected" vs "UWB was turned off"

Each state maps to a distinct UI condition a consumer needs to handle.

---

## Concurrency Model

### The Problem

Android's `UwbManager` delivers ranging results via `Flow.collect()` on the caller's coroutine. iOS's `NISessionDelegate` delivers callbacks on an Apple-managed dispatch queue. Both platforms need state mutations serialized to prevent races.

### Single-Writer Architecture

```
Layer 1: Platform callbacks (OS-managed)
    ↓  Channel.trySend / scope.launch
Layer 2: Serialized state mutations (limitedParallelism(1))
    ↓  StateFlow / receiveAsFlow
Layer 3: Consumer API (caller's coroutine context)
```

**Layer 2** uses `Dispatchers.Default.limitedParallelism(1)` — a serial execution view that works on all KMP targets without `expect/actual`. At most one coroutine runs at a time per session. No locks, no mutexes.

**Each session has its own serialized scope.** This is critical on iOS, where `NISessionDelegate` callbacks arrive on Apple's dispatch queue. Every delegate method wraps state mutations in `scope.launch { }` to hop back onto the serialized dispatcher.

### Why Not Mutex?

`limitedParallelism(1)` provides stronger guarantees: it serializes the *entire coroutine body*, not just a critical section. With `Mutex`, you can accidentally access shared state outside the lock. With a serial dispatcher, all code on the scope is automatically serialized.

---

## Ranging Results Pipeline

### Hot Flow with Bounded Buffer

```kotlin
private val resultChannel = Channel<RangingResult>(
    capacity = 64,
    onBufferOverflow = BufferOverflow.DROP_OLDEST,
)
override val rangingResults: Flow<RangingResult> = resultChannel.receiveAsFlow()
```

UWB measurements arrive at high frequency (~5 Hz). The pipeline is designed for this:

- **`Channel(64, DROP_OLDEST)`** — if a consumer falls behind, stale measurements are silently dropped. The consumer always sees the most recent data.
- **`receiveAsFlow()`** — converts the channel to a cold-on-subscription Flow. Unlike `channelFlow`, this doesn't start a new platform session per collector.
- **`trySend()`** — non-suspending send from platform callbacks. Never blocks the OS callback thread.

### Why Not SharedFlow?

`MutableSharedFlow(replay=0)` drops values when no collector is active. `MutableSharedFlow(replay=1)` replays stale data to new collectors. The channel approach gives explicit buffer control with `DROP_OLDEST` semantics — the right trade-off for real-time sensor data.

### Single Session Start

On Android, `scope.launch { startRangingWithPeer(peer) }` runs once when `start()` is called. The launched coroutine collects from `sessionScope.prepareSession()` and feeds results into the channel. This avoids the pitfall where each `collect()` call on `rangingResults` would create a new platform session.

### iOS Discovery Token Cache

On iOS, `NIDiscoveryToken` → `PeerAddress` requires `NSKeyedArchiver` serialization. Since the token-to-address mapping is stable for a session's lifetime, `DiscoveryTokenCache` caches it per session rather than re-serializing on every callback. No synchronization needed — the cache is only accessed from Apple's delegate dispatch queue.

---

## Error Model

Errors use **sealed interfaces** for composability. A single error can implement multiple facets:

```
UwbError
├── SessionError
│   ├── SessionLost(message, cause?)
│   └── SessionRejected(message)
├── RangingError
│   ├── PeerUnreachable(message)
│   └── ChipsetError(message)  — also implements HardwareError
├── HardwareError
│   ├── ChipsetError(message)
│   ├── UnsupportedFeature(message)
│   └── UwbUnavailable(message)
└── SecurityError
    └── StsVerificationFailed(message)
```

`ChipsetError` is both a `RangingError` and a `HardwareError`. Consumers can `when`-branch on whichever facet they care about:

```kotlin
when (error) {
    is SecurityError -> promptReAuth()
    is HardwareError -> showHardwareUnavailable()
    is SessionError -> showRetryDialog()
}
```

---

## Value Classes

### Distance

`Distance` is a `@JvmInline value class` that bit-packs a `Double` into a `Long`:

```kotlin
@JvmInline
public value class Distance private constructor(private val packed: Long) : Comparable<Distance> {
    public val meters: Double get() = Double.fromBits(packed)
}
```

Construction validates non-negative values. Extension properties (`2.5.meters`, `150.centimeters`) provide ergonomic creation. `toString()` rounds to two decimal places.

### Angle

`Angle` wraps a `Double` in degrees, with a `radians` computed property. Factory methods: `Angle.degrees()`, `Angle.radians()`. Extension properties: `15.0.degrees`.

### Why Value Classes?

Zero-allocation on JVM. Type safety — you can't accidentally pass a distance where an angle is expected. Unit conversion is built into the type (`angle.radians`, `distance.meters`).

---

## Peer Identity

### PeerAddress

`PeerAddress` wraps a `ByteArray` with content-based equality:

```kotlin
public class PeerAddress(bytes: ByteArray) {
    private val bytes: ByteArray = bytes.copyOf() // defensive copy on construction
    public fun toByteArray(): ByteArray = bytes.copyOf() // defensive copy on access
}
```

This was initially a `@JvmInline value class`, but `ByteArray` uses referential equality by default — two `PeerAddress` instances with identical bytes would not be equal. Converting to a regular class with `contentEquals`/`contentHashCode` fixes this.

### iOS Discovery Token Serialization

On iOS, `NINearbyObject` identifies peers by `NIDiscoveryToken` (an opaque ObjC object). The library serializes tokens to stable byte representations via `NSKeyedArchiver`:

```
NIDiscoveryToken → NSKeyedArchiver.archivedDataWithRootObject() → NSData → memcpy → ByteArray → PeerAddress
```

This ensures consistent peer identity across delegate callbacks throughout a session's lifetime.

---

## Platform Bridges

### Android

| Common | Android |
|--------|---------|
| `UwbAdapter` | `PackageManager.FEATURE_UWB` check + `UwbManager` capabilities |
| `RangingSession` | `UwbManager.controllerSessionScope()` / `controleeSessionScope()` |
| `RangingResult` | `androidx.core.uwb.RangingResult.RangingResultPosition` / `RangingResultPeerDisconnected` |
| Auto-init | `AndroidX Startup` — `KmpUwbInitializer` captures `Context` at app launch |

The Android implementation uses `RangingParameters` with DS-TWR (Double-Sided Two-Way Ranging) and automatic update rate.

### iOS

| Common | iOS |
|--------|-----|
| `UwbAdapter` | `NISession.deviceCapabilities` availability check |
| `RangingSession` | `NISession` with `NISessionDelegate` |
| `RangingResult` | `NINearbyObject.distance` + `direction` (simd_float3 → `Vector128`) |
| Direction parsing | `Vector128.getFloatAt(0/1/2)` for x/y/z → `atan2` for azimuth/elevation |

The `NISessionDelegate` implements multiple `session(_:...)` overloads for the `NISessionDelegateProtocol`. Kotlin/Native 2.3.20 handles the ObjC selector disambiguation automatically — no `@Suppress` annotations needed.

### JVM

The JVM target provides stub implementations that report `UwbAdapterState.UNSUPPORTED` and throw `UnsupportedOperationException` on session creation. This allows common tests to compile and run on host without requiring UWB hardware.

---

## Testing Infrastructure

### FakeRangingSession

A fully controllable test double implementing `RangingSession`:

```kotlin
val fake = FakeRangingSession(config)
fake.start(peer)

// Inject measurements
fake.emitResult(RangingResult.Position(peer, measurement))

// Simulate lifecycle events
fake.simulatePeerLost(peer)
fake.simulateError(SessionLost(message = "test"))
fake.simulateSuspend()
fake.simulateResume()
```

Uses `Channel.UNLIMITED` (not `Channel.BUFFERED`) so test emissions never block, regardless of consumption timing. Tracks active peers via a `MutableSet` exposed as an immutable snapshot.

### FakeUwbAdapter

Controls adapter state and capabilities for tests:

```kotlin
val fake = FakeUwbAdapter()
fake.simulateDisabled()  // state → OFF
fake.simulateEnabled()   // state → ON
fake.simulateUnsupported() // state → UNSUPPORTED
```

---

## Key Design Decisions

| Decision | Rationale |
|----------|-----------|
| Separate repo from kmp-ble | Different hardware, different APIs, different release cadence. No compile-time coupling |
| `expect/actual` factory functions | `UwbAdapter()` and `RangingSession(config)` look like constructors but dispatch to platform implementations. No DI framework required |
| Channel over SharedFlow | Explicit buffer control with `DROP_OLDEST` for real-time sensor data |
| Content-based PeerAddress | `ByteArray` referential equality is a footgun. Regular class with `contentEquals` prevents subtle bugs |
| Single-writer concurrency | `limitedParallelism(1)` serializes all state mutations per session without locks |
| JVM stubs | Lets common tests run on host. Desktop UWB is not a current target |
| AndroidX Startup auto-init | Zero-config for consumers. Opt-out available via manifest merge rules |

---

*Current as of v0.1.0*
