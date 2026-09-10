# Khidki — Locked decisions

Status: locked for M0 and v1. Worker must not reopen these. Owner: Laraib. Date: 2026-09-10.

## Identity

| Item | Value |
|---|---|
| Display name | **Khidki** (खिड़की, “window”) |
| Local folder | `~/pers/window-sms` |
| GitHub repo | `laraib-sidd/khidki` (private) |
| `applicationId` / namespace | `dev.laraib.khidki` |
| License | None. All rights reserved. Do not add SPDX or LICENSE. |

`applicationId` is the Android unique ID. Changing it later makes a different app; updates will not replace the old install. That is why it is locked now.

Working name “WindowSMS” is retired.

## Product

- On-device only. Request SMS in. Forward SMS out. No server.
- Personal APK from **GitHub Releases** (`preview` prerelease, overwritten on each push to `main`).
- Installer is still sideload. GitHub Releases is not Play. SMS remains a restricted setting. README must document: App info → overflow → Allow restricted settings → SMS → Allow, plus Realme Auto-start / background activity.
- First physical device: Realme GT 6T, Android 16, India, one SIM.
- Physical testers: Laraib and brother. Worker has no device. Worker must not write “tested on GT 6T”.
- **OTP-shaped SMS is the product.** Ordinary SMS-only success is not enough to start M1. If OTP-shaped synthetics do not arrive on the GT 6T, stop. Do not add workarounds.

## Security defaults (v1)

| Item | Locked value |
|---|---|
| Password | 8 ASCII digits, CSPRNG, no user-chosen secrets, no 5-digit |
| Reuse | Reusable until expiry. Not one-use. Residual risk: the command sits in both SMS threads and can re-arm until expiry. |
| Credential lifetime | 24 hours default; owner-configurable 5 minutes to 7 days (UI in M4) |
| Window | 120 seconds default; 30–300 seconds |
| Destination | Always the canonical requester number. No separate destination field. |
| Sessions | One active authorization globally. A request during an active session must not extend, replace, or consume extra usage. Optional BUSY reply stays off. |
| Forwarded logical messages | One per authorization |
| Max configs | 20 |
| Max live credentials per requester | 5 |
| Failed auth | 5 failures / 15 minutes locks that known requester for 15 minutes. Unknown numbers: reject before password check, no reply, do not lock known requesters. |
| Command | `req <PASSWORD>` — ASCII, case-insensitive keyword, one numeric token, max 64 chars. Spec parser rules in the execution plan. |

## Toolchain (M0 pin; change only if the build proves a version unusable)

| Item | Value |
|---|---|
| minSdk | 31 |
| compileSdk | 36 |
| targetSdk | 36 |
| Java / Kotlin JVM | 17 |
| Kotlin | 2.1.20 |
| AGP | 8.9.1 |
| Compose | Not in M0. M4 only. |
| Room / Keystore / RE2/J / libphonenumber | Not in M0. M1+ as listed in the plan. |
| INTERNET | Never in v1. Audit merged manifest. |

Do not set targetSdk 37. That opts into Android 17 standard-OTP delay.

## Distribution

- Not Play. Not F-Droid. Do not disguise the app.
- Private GitHub. Brother gets the APK from Laraib, not a public download.
- Preview signing: CI debug keystore may change per runner until `KHIDKI_PREVIEW_KEYSTORE_BASE64` exists as a GitHub secret. Until then, uninstall before reinstalling.

## Explicit exclusions (hard)

No server, Firebase, Telegram, internet forwarding, analytics, ads, inbox polling, permanent foreground service, root, accessibility, notification-listener SMS, hidden icon, default-SMS role to bypass restrictions, lowering targetSdk to evade OTP protection.

## Assumed because the owner skipped the question

Q8 was unanswered. Locked as: destination = requester, one global armed window. If Laraib later contradicts this, stop M1 and ask. Do not invent a second destination field.
