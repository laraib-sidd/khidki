# Installing Khidki

Khidki is distributed as a signed APK via [GitHub Releases](https://github.com/laraib-sidd/khidki/releases). It is not on the Play Store.

## Before you install

- **Android 12+** required (minSdk 31).
- You need **SMS permissions** — Khidki reads and sends SMS only for forwarding you configure.
- **RCS chat is not supported** — only traditional SMS (bank OTPs, delivery codes, etc.).

## Steps

### 1. Download

1. Open GitHub Releases → **Latest** (e.g. `v1.3.0-b42`).
2. Download `khidki-1.3.0-b42-<sha>.apk`.
3. Optional: verify with the bundled `.sha256` file.

### 2. Uninstall old builds (if needed)

If you previously installed a **debug** package (`dev.laraib.khidki.debug`), uninstall it first. Release builds use `dev.laraib.khidki`.

### 3. Install the APK

Open the downloaded file and allow installation from your browser or file manager if prompted.

### 4. Allow restricted settings (Android 15+)

1. **Settings → Apps → Khidki**
2. Tap **⋮** (overflow) → **Allow restricted settings**

Without this, SMS permission prompts may fail silently.

### 5. Grant permissions

1. Open Khidki and complete the welcome steps.
2. Tap **Grant permissions** on the setup card (or use the prompt when the app asks).
3. Allow **SMS** and **Notifications** (notifications show when windows open or messages forward).

### 6. Realme / ColorOS (recommended)

1. **Settings → Apps → Khidki → Battery** → **Unrestricted**
2. Enable **Auto-start** and **Allow background activity**

### 7. First use

1. **Home** → turn **Forwarding** on.
2. **Rules** → create a forwarding rule.
3. **Home** → **Arm timed forwarding**, or have the requester send the `req` command by SMS.

## Sharing with someone else

Send them:

1. Link to the latest GitHub Release APK
2. This install guide
3. `docs/PRIVACY.md` (or summarize: on-device only, no cloud)

## Troubleshooting

| Problem | Fix |
|---|---|
| App closes on permission tap | Complete restricted settings step first |
| OTP never forwards | Window must be armed first; message must be SMS not RCS |
| No notifications | Grant notification permission in system settings |
| Can't update over old install | Uninstall if package ID or signing key changed |
