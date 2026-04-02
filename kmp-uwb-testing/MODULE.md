# Module kmp-uwb-testing

Test doubles for kmp-uwb. Unit test UWB workflows without hardware.

## Installation

```kotlin
// build.gradle.kts
commonTest.dependencies {
    implementation("com.atruedev:kmp-uwb-testing:<version>")
}
```

## Provided fakes

| Class | Implements |
|---|---|
| `FakeUwbAdapter` | `UwbAdapter` |
| `FakePreparedSession` | `PreparedSession` |
| `FakeRangingSession` | `RangingSession` |
| `FakePeerConnector` | `PeerConnector` |
| `FailingPeerConnector` | `PeerConnector` |

## Usage

```kotlin
val adapter = FakeUwbAdapter()
val session = adapter.prepareSession(config)
val ranging = session.startRanging(remoteParams)

// Inject test measurements
(ranging as FakeRangingSession).emitResult(result)
```
