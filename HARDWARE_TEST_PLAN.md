# Hardware Test Plan

Manual test procedures for UWB hardware. Run before every major release.

CI covers all logic that doesn't require UWB hardware. This document covers everything that does.

---

## Equipment

### Android
- 2 devices with UWB (e.g., Pixel 7 Pro, Samsung Galaxy S24)
- Both running API 33+
- Sample app (`sample-android`) installed on both

### iOS
- 2 iPhones with U1 or U2 chip (iPhone 11+)
- Running iOS 15+
- Sample app (`iosApp`) installed on both

---

## Android Test Matrix

### A1. Adapter State (single device)

| # | Step | Expected |
|---|------|----------|
| 1 | `UwbAdapter().state.value` | `UwbAdapterState.ON` |
| 2 | Toggle UWB off in Settings, recreate adapter | `UwbAdapterState.OFF` |
| 3 | Toggle UWB back on, recreate adapter | `UwbAdapterState.ON` |

### A2. Capabilities (single device)

| # | Step | Expected |
|---|------|----------|
| 1 | `adapter.capabilities()` | `supportedChannels` non-empty (typically {5, 9}) |
| 2 | Check `angleOfArrivalSupported` | `true` on devices with AoA |
| 3 | Check `supportedRoles` | Contains both CONTROLLER and CONTROLEE |

### A3. Session Preparation (single device)

| # | Step | Expected |
|---|------|----------|
| 1 | `prepareSession(CONTROLLER)` | Returns `PreparedSession` with non-empty `localParams` |
| 2 | `prepareSession(CONTROLEE)` | Returns `PreparedSession` with non-empty `localParams` |
| 3 | Decode `localParams` via `SessionParamsCodec` | Valid address, channel > 0 |
| 4 | Call `startRanging` twice | Second call throws `IllegalStateException` |
| 5 | Call `close()` then `startRanging` | Throws `IllegalStateException` |

### A4. Full Ranging (two devices)

| # | Step | Expected |
|---|------|----------|
| 1 | Device A: `prepareSession(CONTROLLER)` | Gets `localParams` |
| 2 | Device B: `prepareSession(CONTROLEE)` | Gets `localParams` |
| 3 | Exchange params over BLE (or manually) | Both devices have remote params |
| 4 | Both: `startRanging(remoteParams)` | State transitions: Negotiating → Initializing → Ranging |
| 5 | Hold devices ~1m apart | `RangingResult.Position` with `distance ~1.0m` (±0.3m) |
| 6 | Verify azimuth present | Non-null on AoA-capable devices |
| 7 | Move to ~3m | Distance updates to ~3.0m |

### A5. Session Lifecycle (two devices)

| # | Step | Expected |
|---|------|----------|
| 1 | Active session, call `close()` on controller | State → `Stopped.ByRequest` |
| 2 | Active session, move device B out of range (~15m+) | `RangingResult.PeerLost`, state → `Active.PeerLost` |
| 3 | Bring device B back in range | `RangingResult.PeerRecovered`, state → `Active.Ranging` |
| 4 | Active session, kill app on device B | State → `Stopped.ByError` (eventually) |
| 5 | Active session, toggle UWB off on device A | State → `Stopped.BySystemEvent` or `Stopped.ByError` |

---

## iOS Test Matrix

### I1. Adapter State (single device)

| # | Step | Expected |
|---|------|----------|
| 1 | `UwbAdapter().state.value` | `UwbAdapterState.ON` |
| 2 | On device without U1/U2 (e.g., iPhone SE) | `UwbAdapterState.UNSUPPORTED` |

### I2. Capabilities (single device)

| # | Step | Expected |
|---|------|----------|
| 1 | `adapter.capabilities()` | `supportedChannels = {5, 9}` |
| 2 | `angleOfArrivalSupported` | `true` |
| 3 | `supportedRoles` | Contains CONTROLLER and CONTROLEE |

### I3. Session Preparation (single device)

| # | Step | Expected |
|---|------|----------|
| 1 | `IosPreparedSession.create(config)` | Completes within 5s timeout |
| 2 | `localParams` non-empty | Starts with version byte `0x81` |
| 3 | Deserialize token from `localParams` bytes | Returns valid `NIDiscoveryToken` |
| 4 | Serialize → deserialize round-trip | Produces equivalent token |

### I4. Full Ranging (two devices)

| # | Step | Expected |
|---|------|----------|
| 1 | Device A: prepare session | Gets discovery token in `localParams` |
| 2 | Device B: prepare session | Gets discovery token in `localParams` |
| 3 | Exchange tokens over BLE (or manually) | Both devices have remote params |
| 4 | Both: `startRanging(remoteParams)` | State → Ranging |
| 5 | Hold devices ~1m apart | `distance ~1.0m`, azimuth/elevation present |
| 6 | Verify `DiscoveryTokenCache` | Same peer token → same `PeerAddress` across callbacks |

### I5. Session Lifecycle (two devices)

| # | Step | Expected |
|---|------|----------|
| 1 | Active session, call `close()` | State → `Stopped.ByRequest`, NISession invalidated |
| 2 | Move device out of range | `didRemoveNearbyObjects` → `PeerLost` |
| 3 | Bring back in range | `didUpdateNearbyObjects` resumes |
| 4 | Background app during ranging | `sessionWasSuspended` → `Active.Suspended` |
| 5 | Foreground app | `sessionSuspensionEnded` → `Active.Ranging` |
| 6 | Kill remote app | `didInvalidateWithError` → `Stopped.ByError` |

---

## Cross-Platform Ranging

UWB ranging between Android and iOS is supported via the OOB parameter exchange:

| # | Step | Expected |
|---|------|----------|
| 1 | Android CONTROLLER + iOS CONTROLEE | Both get `localParams` |
| 2 | Exchange params (version bytes differ: 0x01 vs 0x81) | Each side decodes peer's format |
| 3 | Start ranging | Measurements flow on both devices |

**Note:** Cross-platform requires the OOB connector to handle the version byte dispatch. The `kmp-uwb-connector` module's `BleConnector` handles this transparently.

---

## Pass Criteria

All tests in each section must pass. Any failure blocks the release.

- Distance measurements within ±30% of actual distance at ranges < 5m
- Azimuth measurements within ±15 degrees (device-dependent)
- State transitions match the expected column exactly
- No crashes, ANRs, or unhandled exceptions during any test
