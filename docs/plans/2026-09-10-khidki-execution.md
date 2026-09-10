# Khidki Implementation Plan

> **For agentic workers:** REQUIRED: follow `WORKER.md`. Execute this plan task-by-task. Checkboxes (`- [ ]`) are for tracking. Do not start M1 until Laraib replies `GO`.

**Goal:** Personal on-device app that, after an authenticated SMS command from a trusted number, opens a short window and forwards matching OTP-shaped SMS back to that same number.

**Architecture:** One Android app module. Domain logic is pure Kotlin, tested on the JVM. Android receiver, SmsManager, Room, and Keystore sit behind interfaces. M0 is a debug log + SMS receive/send probe. No UI product screens until M4.

**Tech stack:** Kotlin 2.1.20, AGP 8.9.1, minSdk 31, compileSdk 36, targetSdk 36, JUnit 4, no INTERNET. Room / Android Keystore / RE2/J after M0.

---

## Worker rules

1. Read `docs/DECISIONS.md` first. Those rows are constraints, not suggestions.
2. One task at a time. Commit and push after each task (`git push origin main`).
3. `git init` only inside `~/pers/window-sms`. Never at `~/pers`.
4. Two failed fixes on the same problem → stop and ask Laraib.
5. Never invent physical-device results, battery numbers, or store approval.
6. Never request or commit real bank OTPs, phone numbers of strangers, or signing keys.
7. If a supported Android API cannot satisfy a requirement, stop. Do not add excluded workarounds.
8. Author commit messages as imperative sentences. Do not put secrets in git.

## Stop conditions (M0)

Stop the project (do not “fix” with excluded APIs) if:

- Restricted settings cannot be allowed on the GT 6T.
- SMS permission stays denied after that.
- Plain synthetics never arrive with the UI closed.
- OTP-shaped synthetics (P3) never arrive. This is the product gate.
- ColorOS only works with a permanent foreground service.
- Only the default SMS app receives `SMS_RECEIVED`.

## What M0 is not

No configuration wizard, passwords, regex, Room, Keystore, Compose navigation, notifications product, or history store. One debug activity. One receiver. Send + sent/delivery callbacks.

---

## Target tree (lock this layout)

```
window-sms/
  WORKER.md
  README.md
  CHANGELOG.md
  .gitignore
  gradle.properties
  settings.gradle.kts
  build.gradle.kts
  gradle/libs.versions.toml
  gradle/wrapper/*
  app/build.gradle.kts
  app/src/main/AndroidManifest.xml
  app/src/main/java/dev/laraib/khidki/
    KhidkiApp.kt
    domain/          # M1+
    data/           # M2+
    platform/sms/   # M0 receiver + transport
    ui/debug/       # M0 log activity only; delete in M5
  app/src/main/res/values/strings.xml
  app/src/test/java/dev/laraib/khidki/
  .github/workflows/apk.yml
  docs/
    DECISIONS.md
    PHYSICAL_TEST.md
    COMPATIBILITY.md
    plans/2026-09-10-khidki-execution.md
```

Later packages (do not create until their milestone): `domain/`, `data/`, `ui/status|config|history|settings/`.

---

## Task 0: Git repo and ignore rules

**Files:**

- Create: `.gitignore`
- Create: `README.md`
- Create: `CHANGELOG.md`

- [ ] **Step 1: Confirm you are in the project folder**

```bash
pwd
# must end with /window-sms
# must NOT be /Users/.../pers
```

- [ ] **Step 2: Write `.gitignore`**

```
/.gradle/
/build/
/app/build/
/local.properties
*.iml
.idea/
.DS_Store
/keystore/
*.jks
*.keystore
google-services.json
.env
```

- [ ] **Step 3: Write `README.md`**

Must include: what Khidki is (armed SMS window, OTP-shaped, personal APK), that it is not on Play, GitHub Releases install, restricted-settings steps, Realme auto-start, “do not use live bank OTPs during development”, and that physical tests are in `docs/PHYSICAL_TEST.md`.

- [ ] **Step 4: Write `CHANGELOG.md`**

```markdown
# Changelog

## [Unreleased]
- M0 feasibility scaffold (not yet device-verified).
```

- [ ] **Step 5: Init git if needed, create private GitHub repo, push**

```bash
git init -b main
git add WORKER.md docs README.md CHANGELOG.md .gitignore
git status
git commit -m "$(cat <<'EOF'
Add Khidki worker plan and locked decisions.

EOF
)"
gh repo create laraib-sidd/khidki --private --source=. --remote=origin --push
```

If `laraib-sidd/khidki` already exists, stop and ask. Do not create `khidki-2`.

Expected: private repo URL printed. `git push -u origin main` succeeds.

---

## Task 1: Gradle project that compiles an empty app

**Files:**

- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `app/build.gradle.kts`
- Create: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/java/dev/laraib/khidki/KhidkiApp.kt`
- Create: `app/src/main/java/dev/laraib/khidki/ui/debug/M0LogActivity.kt`
- Create: `app/src/main/res/values/strings.xml`
- Create: `app/src/main/res/values/themes.xml`

- [ ] **Step 1: Pin versions in `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.9.1"
kotlin = "2.1.20"
junit = "4.13.2"

[libraries]
junit = { group = "junit", name = "junit", version.ref = "junit" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
```

If AGP 8.9.1 refuses to resolve, pin the version the error names in `docs/DECISIONS.md` under Toolchain and continue. Do not jump to a preview AGP.

- [ ] **Step 2: Root `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "khidki"
include(":app")
```

- [ ] **Step 3: Root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
```

- [ ] **Step 4: `gradle.properties`**

```
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 5: `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "dev.laraib.khidki"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.laraib.khidki"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-m0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.core:core-ktx:1.15.0")
    testImplementation(libs.junit)
}
```

Note: debug `applicationId` becomes `dev.laraib.khidki.debug`. README must tell testers to install the debug APK from Releases (that is the CI artifact).

- [ ] **Step 6: Manifest, Application, debug activity, strings, theme**

`AndroidManifest.xml` must declare:

- `android:name=".KhidkiApp"`
- `android:allowBackup="false"`
- `android:dataExtractionRules` / `fullBackupContent` false if you add the xml (M2 will add extraction rules; M0 at least `allowBackup="false"`)
- `usesCleartextTraffic` false
- No `INTERNET`
- Permissions: `RECEIVE_SMS`, `SEND_SMS` only for M0 (`RECEIVE_BOOT_COMPLETED` in M3)
- Activity `.ui.debug.M0LogActivity` as launcher
- Do not register the SMS receiver yet (Task 2)

`M0LogActivity` is a vertical `LinearLayout` with:

- Permission status text
- `ScrollView` log
- `EditText` destination (hint: brother number in E.164, e.g. `+91…`)
- Button `Send synthetic` disabled until `SEND_SMS` granted
- Button `Request SMS permission`

On create: if SMS not granted, explain in the log that GitHub sideload needs Allow restricted settings first.

- [ ] **Step 7: Wrapper and compile**

```bash
gradle wrapper --gradle-version 8.13
chmod +x gradlew
./gradlew :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL` and `app/build/outputs/apk/debug/app-debug.apk`.

If the wrapper cannot be generated, install JDK 17 and retry. Report the exact error; do not switch to pip/homebrew hacks that break the machine.

- [ ] **Step 8: Commit and push**

```bash
git add gradle gradlew gradle.properties settings.gradle.kts build.gradle.kts app
git commit -m "$(cat <<'EOF'
Add M0 Android scaffold that compiles a debug APK.

EOF
)"
git push origin main
```

---

## Task 2: SMS receive and send with callbacks

**Files:**

- Create: `app/src/main/java/dev/laraib/khidki/platform/sms/SmsEvent.kt`
- Create: `app/src/main/java/dev/laraib/khidki/platform/sms/SmsReceivedReceiver.kt`
- Create: `app/src/main/java/dev/laraib/khidki/platform/sms/SmsSendResultReceiver.kt`
- Create: `app/src/main/java/dev/laraib/khidki/platform/sms/DebugSmsLog.kt`
- Create: `app/src/main/java/dev/laraib/khidki/platform/sms/SmsSender.kt`
- Create: `app/src/test/java/dev/laraib/khidki/ManifestContractTest.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `M0LogActivity.kt`

### Receiver rules

- Manifest-registered `android.provider.Telephony.SMS_RECEIVED`
- `android:exported="true"`
- `android:permission="android.permission.BROADCAST_SMS"`
- Validate `intent.action` before parsing
- Bound body size: if reconstructed body `> 8192` chars, log `REJECT_OVERSIZE` and stop. Do not truncate-and-keep.
- Use `Telephony.Sms.Intents.getMessagesFromIntent(intent)` only. Do not invent PDU stitching.
- `goAsync()` only if needed; finish in `finally`. Do not start WorkManager. Do not touch Compose. Do not sleep waiting for delivery.
- Append to an in-memory log (`DebugSmsLog`, max 50 lines). Do not write bodies to disk. Do not `Log.d` the body (use length + sender only in logcat).

### Send rules

- `SmsManager.getDefault()` for M0 (single SIM).
- Destination: only the number typed in the debug field. Reject if empty, if it is a short code (`length < 8` digits), or if it contains letters.
- `sentIntent` and `deliveryIntent` explicit, component-scoped to `SmsSendResultReceiver`, `PendingIntent.FLAG_IMMUTABLE` plus `FLAG_UPDATE_CURRENT`, unique request codes.
- No retries.

### Debug UI

Show in the on-screen log: timestamp, type (`IN` / `SENT` / `SENT_CB` / `DELIVERY_CB`), sender or destination, body (M0 debug only). This screen is removed in M5.

- [ ] **Step 1: Write `ManifestContractTest`**

Read `app/src/main/AndroidManifest.xml` as text in a JVM test (put a copy under `app/src/test/resources/AndroidManifest.xml` generated in the test by reading from `File("../src/main/AndroidManifest.xml")` is fragile in Gradle). Instead parse using:

```kotlin
val xml = File("src/main/AndroidManifest.xml").readText()
```

with `project.projectDir` — simpler: keep the test in `app/src/test` and load via

```kotlin
val xml = javaClass.classLoader!!.getResource("manifest-snapshot.xml")!!.readText()
```

and add a Gradle task… **Too heavy.** Do this instead:

`app/src/test/java/dev/laraib/khidki/ManifestContractTest.kt` reads `System.getProperty("khidki.manifest")` set from `app/build.gradle.kts`:

```kotlin
tasks.withType<Test> {
    systemProperty("khidki.manifest", file("src/main/AndroidManifest.xml").absolutePath)
}
```

Assertions:

- contains `android.permission.RECEIVE_SMS`
- contains `android.permission.SEND_SMS`
- does **not** contain `android.permission.INTERNET`
- does **not** contain `READ_SMS`
- does **not** contain `BIND_NOTIFICATION_LISTENER_SERVICE`
- contains `BROADCAST_SMS`
- contains `android.provider.Telephony.SMS_RECEIVED`

Run: `./gradlew :app:test --tests dev.laraib.khidki.ManifestContractTest`

Expected before receiver exists: FAIL on `SMS_RECEIVED`. After implementation: PASS.

- [ ] **Step 2: Implement receiver, sender, log, wire activity, run unit test**

```bash
./gradlew :app:test :app:assembleDebug --stacktrace
```

Expected: `BUILD SUCCESSFUL`, tests PASS.

- [ ] **Step 3: Commit and push**

```bash
git commit -m "$(cat <<'EOF'
Add M0 SMS receive/send probe and manifest contract tests.

EOF
)"
git push origin main
```

---

## Task 3: GitHub Actions preview APK

**Files:**

- Create: `.github/workflows/apk.yml`

- [ ] **Step 1: Workflow**

```yaml
name: preview-apk
on:
  push:
    branches: [main]
  workflow_dispatch:
permissions:
  contents: write
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: "17"
      - uses: android-actions/setup-android@v3
      - name: Decode preview keystore if present
        env:
          KEYSTORE_B64: ${{ secrets.KHIDKI_PREVIEW_KEYSTORE_BASE64 }}
        run: |
          if [ -n "$KEYSTORE_B64" ]; then
            mkdir -p "$HOME/.android"
            echo "$KEYSTORE_B64" | base64 --decode > "$HOME/.android/debug.keystore"
          else
            echo "No preview keystore secret; using ephemeral debug keystore. Uninstall before reinstall."
          fi
      - run: ./gradlew :app:test :app:assembleDebug --stacktrace
      - uses: softprops/action-gh-release@v2
        with:
          tag_name: preview
          name: Khidki preview
          prerelease: true
          files: app/build/outputs/apk/debug/*.apk
        env:
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
```

- [ ] **Step 2: README Releases section**

Document: open https://github.com/laraib-sidd/khidki/releases/tag/preview , download `app-debug.apk`, install, then `docs/PHYSICAL_TEST.md`. Each push replaces that release.

- [ ] **Step 3: Push and wait for the workflow**

```bash
git add .github/workflows/apk.yml README.md
git commit -m "$(cat <<'EOF'
Publish debug APK to the preview GitHub Release on every main push.

EOF
)"
git push origin main
gh run watch --exit-status
```

Expected: green run, Release `preview` contains an APK.

If the workflow fails, fix once. Second failure → stop and paste the log.

- [ ] **Step 4: Write `docs/COMPATIBILITY.md`**

Fill only what you actually ran:

- Toolchain versions
- CI OS
- Test command output summary
- APK size
- Explicit: “Physical GT 6T: not run by the worker. See PHYSICAL_TEST.md.”

- [ ] **Step 5: Commit COMPATIBILITY.md and push**

Then **stop M0**. Message Laraib: preview APK URL, and “brother should run `docs/PHYSICAL_TEST.md`. Reply GO or STOP.”

Do not start M1.

---

## Milestone gate

| Signal | Worker does |
|---|---|
| Laraib replies `GO` with P3 pass | Start M1 |
| P3 fail, P1 pass | Stop. OTP-shaped blocked. No workarounds. |
| P1 fail | Stop. Receiver never fired. |
| No reply | Wait. Do not assume GO. |

---

## M1 — Domain core (after GO)

Still no real SmsManager in domain tests. Fake clock, fake transport.

**Packages:** `dev.laraib.khidki.domain`

Create these types (names locked):

```kotlin
data class CanonicalPhone(val e164: String)
data class ConfigurationId(val uuid: java.util.UUID)
data class ConfigurationVersion(val n: Int)
enum class AppState { PAUSED, READY, BLOCKED_PERMISSION, BLOCKED_CAPABILITY }
enum class SessionState { ARMED, CLAIMED, SUBMITTING, SUBMITTED }
enum class TerminalOutcome { COMPLETED, EXPIRED, CANCELLED, FAILED, UNCERTAIN }

sealed class IncomingSms {
    data class Command(val from: CanonicalPhone, val raw: String) : IncomingSms()
    data class Candidate(val from: String, val body: String, val receivedAtMillis: Long) : IncomingSms()
}
```

Interfaces:

- `Clock` with `nowMillis()`, `bootId()`, `monotonicNanos()`
- `RequestAuthenticator`
- `RuleMatcher`
- `SessionRepository`
- `BudgetLedger`
- `SmsTransport`
- `AuditStore`

### Task M1.1 Protocol parser

File: `domain/protocol/RequestParser.kt`

Rules (locked):

- Trim leading/trailing whitespace
- ASCII only after trim; otherwise `Reject.NonAscii`
- Keyword `req` case-insensitive
- Exactly one token after keyword: ASCII digits `[0-9]+` length 8 for generated secrets (parser should accept 8 digits; reject length ≠ 8 for v1)
- Bounded spaces/tabs between tokens
- Reject extra tokens, multiline, Unicode digits, length > 64
- Recognized commands are never candidates

Tests in `app/src/test/java/dev/laraib/khidki/domain/protocol/RequestParserTest.kt` — one test method per reject reason plus happy path `req 12345678`.

### Task M1.2 Phone normalization

Use libphonenumber **in M1 tests with the library added then**. Default region `IN`. Ambiguous national numbers without country → reject. Alphanumeric senders are **not** phones; they stay raw strings and never go through phone normalization.

### Task M1.3 Filtering

RE2/J. Sender predicate AND content predicate. Exclusions veto. Version 1: at least one nonempty sender restriction and one nonempty content predicate. Invalid pattern → disable forwarding (fail closed). Pattern max 512. Body max 8192. No catch-all default. Trusted requester numbers excluded from sources by default.

### Task M1.4 Auth and sessions

- CSPRNG 8-digit password
- Reusable until expiry (owner lock)
- Keyed verifier interface; M1 can use a fake `Verifier` with constant-time compare. Real Keystore in M2.
- Unknown number rejected before password check
- Lockout 5 / 15 min persisted (in-memory fake in M1, Room in M2)
- One global ARMED session
- Request during active session: ignore for session replace; do not start a second window
- Ack payload: label, window seconds, allowance. Never echo password
- Ack failure closes unclaimed window

### Task M1.5 State machine tests

Cover every bullet in spec section 11 “Domain tests” that does not need Android. Fake clock for expiry and clock rollback. Reboot: `bootId` change expires ARMED.

**M1 done** = `./gradlew :app:test` green on those tests. No APK behavior change required except still building. Push. Do not send real SMS from domain code.

---

## M2 — Persistence and credentials

- Room for configs, credentials, sessions, budget, duplicate fingerprints, lockout counters, audit metadata
- Atomic transaction: authenticate + consume/check reuse + snapshot config + create session
- Keystore-backed HMAC verifier. Key: available after first unlock, **not** `setUserAuthenticationRequired(true)` (background must work without biometric)
- Migrations with tests
- Crash near send → `UNCERTAIN`
- Exclude backup of these tables (`allowBackup=false` already; add data extraction rules)
- Duplicate fingerprint: keyed digest of sender + body + PDU timestamp + subscription. Store digest, not body.

**M2 done** = JVM tests for migrations, lockout persistence, reboot vs process recreation, credential reuse until expiry, one-use-not-applicable.

---

## M3 — Android integration

- Manifest receiver enabled iff master enable is on. Do **not** disable between windows.
- `RECEIVE_BOOT_COMPLETED`: revalidate permissions, drop sessions (boot id), do not revive authorization
- Permission onboarding disclosure **before** the system prompt
- Denied/revoked → `BLOCKED_PERMISSION`
- Single-SIM `SmsManager`. Multi-SIM: block with “select SIM” rather than silent pick (device is one SIM; still do not silently pick if two appear)
- Per-part sent/delivery callbacks, unique ids, immutable PendingIntents, reject forged/duplicate callbacks
- Pause: invalidate future submissions immediately; cannot retract in-flight SMS
- Serialize submissions; `goAsync` bounded; always finish in `finally`

**M3 done** = instrumented tests where possible; still no claimed GT 6T results unless Laraib pastes them.

---

## M4 — UI

Compose/Material. Screens: Status, Configurations, History, Settings.

Wizard order from the original spec section 3, with locked fields (8-digit, reuse-until-expiry, destination=requester). Reveal command once. Device credential for reveal / change requester / broaden rules. No biometric for background forward. History metadata-only. Strings in `strings.xml`. TalkBack. Light/dark.

Do not show passwords, OTPs, or full source bodies on Status/History.

---

## M5 — Hardening

- Crash-injection tests at persist/submit boundaries
- Merged manifest + dependency review (still no INTERNET)
- Remove `ui/debug` and any test-only entry points from release
- Latency/battery: do not invent; only record if Laraib runs the 30-trial protocol
- Brother/Laraib fill physical matrix (doze, force-stop, reboot, SIM)

---

## M6 — Release prep

- Signed personal APK (keys in GitHub secrets / local, never git)
- Checksum on the Release
- Upgrade test from preview → signed
- Privacy statement, compatibility, troubleshooting
- Still no Play listing

---

## Protocol and filtering (copy for M1; do not weaken)

Command: `req <8-digit>`

Parser rejects: extra tokens, multiline, Unicode lookalikes, `> 64` chars, non-ASCII.

Incoming commands cannot add destination, regex, window, or permissions.

App-generated SMS must not be parsed as commands (loop guard: ignore self-originated / ignore bodies the app just sent).

Default content filters in the wizard must not be empty. Worker must not ship a `.*` default.

Regex engine: RE2/J. No silent fallback to `java.util.regex`.

---

## Cost caps (v1)

- Max 3 SMS parts per forwarded logical message
- 20 attempted outgoing parts / rolling 24h including acks
- Reserve before send. Unknown outcomes keep the reservation
- Oversized logical message: reject all parts, never send the first segment only
- No automatic retries in v1

---

## Required documents (when to write)

| File | When |
|---|---|
| README.md | Task 0 |
| DECISIONS.md | already written |
| PHYSICAL_TEST.md | already written |
| COMPATIBILITY.md | end of M0 (CI only) |
| CHANGELOG.md | every milestone |
| ARCHITECTURE.md | end of M1 |
| SECURITY.md | end of M2 |
| PRIVACY.md | M4 disclosures |
| TEST_PLAN.md / TEST_RESULTS.md | M5; results from Laraib/brother, not invented |
| LICENSE | never, until owner picks one |

---

## Primary sources (reverify if a milestone is months later)

- https://developer.android.com/reference/android/provider/Telephony.Sms.Intents
- https://developer.android.com/reference/android/Manifest.permission#RECEIVE_SMS
- https://developer.android.com/about/versions/17/behavior-changes-all
- https://developer.android.com/about/versions/17/behavior-changes-17
- https://developer.android.com/reference/android/telephony/SmsManager
- https://github.com/google/re2j
