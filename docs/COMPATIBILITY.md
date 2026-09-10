# Compatibility

Last verified: 2026-09-10 (CI + local JVM tests only).

## Toolchain

| Item | Value |
|---|---|
| Kotlin | 2.1.20 |
| AGP | 8.9.1 |
| minSdk | 31 |
| compileSdk / targetSdk | 36 |
| JVM | 17 |

## CI

- OS: `ubuntu-latest` (GitHub Actions)
- Command: `./gradlew :app:testDebugUnitTest :app:assembleDebug`
- Unit tests: 51 passed (local run, 2026-09-10)
- APK: `app/build/outputs/apk/debug/app-debug.apk` (~debug build)

## Physical device (Realme GT 6T)

**Not run by the implementation agent.** Laraib/brother must complete `docs/PHYSICAL_TEST.md` and record P3 (OTP-shaped) before treating forwarding as validated.

## Known platform risks

- Sideload + Android 15+: SMS is a **restricted setting** (App info → Allow restricted settings).
- Android 17: OTP-shaped / retriever SMS may be delayed up to 3 hours for non-exempt apps.
- Realme/ColorOS: may require Auto-start and background activity for reliable receive.
