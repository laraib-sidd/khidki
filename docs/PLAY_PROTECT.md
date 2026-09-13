# Google Play Protect and sideloading

Khidki is **not** on Google Play. Recipients either sideload a signed APK from GitHub Releases or install from F-Droid once listed.

**Sideload (GitHub APK, browser, Drive, Files)** can trigger Play Protect. **F-Droid client installs** are not that sideload path.

GitHub and F-Droid APKs use different signing keys — see `docs/RELEASE.md`.

## What users may see

When installing from a browser, Drive, WhatsApp, or Files, **Play Protect may block or warn** on APKs that declare `RECEIVE_SMS` and `SEND_SMS`. This is automatic Google behavior for sideloaded SMS apps — not a bug in Khidki and **not fixable in Gradle or UI**.

Official guidance: [Play Protect warning developer guidance](https://developers.google.com/android/play-protect/warning-dev-guidance).

We **do not** ask users to disable Play Protect.

## What we do instead

1. **Stable release signing** — CI can sign with `KHIDKI_RELEASE_KEYSTORE_BASE64` (see `docs/RELEASE.md` and `scripts/create_release_keystore.sh`). Unsigned or rotating debug keys look more suspicious and break seamless updates.
2. **Play Protect appeal** — File at [Play Protect appeals](https://support.google.com/googleplay/android-developer/answer/2992033) for package `dev.laraib.khidki` with:
   - The signed release APK
   - SHA-256 of the APK
   - Plain-language use case: owner-controlled OTP forwarding to a number they configure, windowed, no internet
   - A Play Console account ($25) is required even if we never publish to Play Store
3. **Honest install docs** — `docs/INSTALL.md` describes sideload steps; we do not promise “no Protect dialog” until Google allowlists the certificate.

## If the appeal is denied

Likely for OTP-interception-shaped apps. Remaining options:

- **ADB install** for testers you know (`adb install khidki-*.apk`)
- **F-Droid** once the listing exists
- **Product change** that drops SMS receive (different product)

We will not fake a Play listing or hide SMS permissions.

## Permissions Khidki must keep

- `RECEIVE_SMS` — read inbound OTPs
- `SEND_SMS` — forward to the trusted number

Removing either stops Khidki from being this product.
