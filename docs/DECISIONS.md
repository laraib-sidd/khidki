# Khidki — Locked decisions

Status: locked for M0 and v1. Worker must not reopen these. Owner: Laraib. Date: 2026-09-10.

## Identity

| Item | Value |
|---|---|
| Display name | **Khidki** (खिड़की, “window”) |
| Local folder | `~/pers/window-sms` |
| GitHub repo | `laraib-sidd/khidki` (public) |
| `applicationId` / namespace | `dev.laraib.khidki` |
| License | **GPL-3.0-or-later**. `LICENSE` in repo root. |

`applicationId` is the Android unique ID. Changing it later makes a different app; updates will not replace the old install. That is why it is locked now.

Working name “WindowSMS” is retired.

## Product

- On-device only. Request SMS in. Forward SMS out. No server.
- Personal APK from **GitHub Releases** (`preview` prerelease, overwritten on each push to `main`).
- Installer is still sideload. GitHub Releases is not Play. SMS remains a restricted setting. README must document: App info → overflow → Allow restricted settings → SMS → Allow, plus Realme Auto-start / background activity.
- First physical device: Realme GT 6T, Android 16, India, one SIM.
- Physical testers: owner and a second device. Worker has no device. Worker must not write “tested on GT 6T”.
- **OTP-shaped SMS is the product.** Ordinary SMS-only success is not enough to start M1. If OTP-shaped synthetics do not arrive on the GT 6T, stop. Do not add workarounds.

## Security defaults (v1)

| Item | Locked value |
|---|---|
| Password | **Retired** (was SMS `req` auth). v1.5 arms only via in-app **Start forwarding**. |
| Reuse | **Retired** (was SMS command in both threads). Timed in-app arm only; no SMS command re-arm. |
| Credential lifetime | **Retired** (legacy Room table kept for schema). |
| Window | **v1.5:** 15m–2h or Until I stop (in-app). Legacy 120s auth window retired. |
| Destination | **v1.5:** Rules people (up to five). Legacy “canonical requester” retired. |
| Sessions | **v1.5:** one global timed window from Home Start. Legacy per-request auth retired. |
| Forwarded logical messages | **v1.5:** all matching OTPs while window is open (budget-capped). |
| Max configs | 20 (legacy Room cap; Rules people capped at five in v1.5 UI). |
| Max live credentials per requester | **Retired** (legacy credential table). |
| Failed auth | **Retired** (was SMS password lockout). |
| Command | **Retired.** In-app Start forwarding only. Legacy credential/config Room tables kept for schema safety; no `req` parser in public source. |
| People | Up to five destinations on Rules; each has name, phone, and filter toggles. |
| Filters | Kind + domain category toggles (`ForwardingPresets`); Government (EPF) separate from Banks. |

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

- **Not Play Store.** Do not fake Device Synchronization or hide SMS permissions.
- **GitHub Releases** (sideload, owner-signed) and **F-Droid** (F-Droid-signed) are both valid. They use **different signing keys** unless reproducible builds are added later — users cannot in-place update from one channel to the other.
- Public source: `https://github.com/laraib-sidd/khidki`
- Preview signing: CI debug keystore may change per runner until `KHIDKI_PREVIEW_KEYSTORE_BASE64` exists as a GitHub secret. Until then, uninstall before reinstalling.

## Explicit exclusions (hard)

No server, Firebase, Telegram, internet forwarding, analytics, ads, inbox polling, permanent foreground service, root, accessibility, notification-listener SMS, hidden icon, default-SMS role to bypass restrictions, lowering targetSdk to evade OTP protection.

## Assumed because the owner skipped the question

Q8 was unanswered. Locked as: destination = requester, one global armed window. If Laraib later contradicts this, stop M1 and ask. Do not invent a second destination field.
