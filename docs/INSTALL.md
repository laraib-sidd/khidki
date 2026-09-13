# Installing Khidki

Khidki is **not** on the Play Store. You can install:

1. A signed APK from [GitHub Releases](https://github.com/laraib-sidd/khidki/releases) (owner key), or
2. The F-Droid build once the app is listed (F-Droid key).

Those builds share package `dev.laraib.khidki` but **not** the signing certificate. Switching channel requires uninstall.

## Before you install

- **Android 12+** (minSdk 31).
- **SMS permissions** — Khidki reads and sends SMS only for forwarding you configure.
- **RCS is not supported** — only traditional SMS.

## GitHub Releases (sideload)

### 1. Download

1. Open GitHub Releases → **Latest** (e.g. `v1.5.1` or `v1.5.1-b<N>`).
2. Download `khidki-1.5.1-*.apk`.
3. Optional: verify with the bundled `.sha256` file.

### 2. Uninstall old builds (if needed)

Uninstall first if you previously installed:

- a **debug** package (`dev.laraib.khidki.debug`), or
- an APK from the **other** channel (GitHub vs F-Droid).

### 3. Install the APK

Open the downloaded file and allow installation from your browser or file manager if prompted.

### 4. Allow restricted settings (Android 15+)

1. **Settings → Apps → Khidki**
2. Tap **⋮** → **Allow restricted settings**

Without this, SMS permission prompts may fail silently.

### 5. Grant permissions

1. Open Khidki and complete welcome.
2. Allow **SMS** and **Notifications** (ongoing status while forwarding).

### 6. Realme / ColorOS

1. **Settings → Apps → Khidki → Battery** → **Unrestricted**
2. Enable **Auto-start** and **Allow background activity**

### 7. First use

1. Welcome → name + number for the first person → pick filters.
2. **Rules** → add more people (max 5) if needed.
3. **Home** → **Start forwarding** → 15m–2h or **Until I stop**.
4. Shade notification: **Stop forwarding** (same as Home Stop).

### Play Protect (sideload only)

Sideloaded SMS apps may show a Play Protect warning. That is a platform heuristic, not a Khidki bug. See [`docs/PLAY_PROTECT.md`](PLAY_PROTECT.md).

Trusted testers can skip the dialog with:

```bash
adb install khidki-1.5.1-*.apk
```

Do not turn Play Protect off for random APKs.

## F-Droid

Install from the F-Droid client when `dev.laraib.khidki` is listed. Updates stay on the F-Droid key.

## Sharing

Send:

1. GitHub Release link **or** F-Droid page (not both as “just update”)
2. This guide
3. [`docs/PRIVACY.md`](PRIVACY.md)

## Troubleshooting

| Problem | Fix |
|---|---|
| App closes on permission tap | Restricted settings first |
| Nothing forwards | Window must be started; SMS not RCS; person enabled with matching filters |
| No shade controls | Grant notifications |
| Can't update | Uninstall if package ID or signing key changed |
