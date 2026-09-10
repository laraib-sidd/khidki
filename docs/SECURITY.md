# Security

## Threats mitigated

- Unknown requesters rejected before password check; no SMS reply to failures.
- Lockout: 5 failures / 15 minutes per known requester.
- Short numeric password stored as Keystore HMAC tag, constant-time compare.
- Regex fail-closed; no catch-all default filters.
- Outbound loop guard ignores recently sent bodies.
- History stores metadata only (no bodies, passwords, OTPs).
- `allowBackup=false`; backup rules exclude app data.

## Not guaranteed

- SMS sender identity (spoofable).
- OTP delivery before expiry on all Android versions.
- Android 17+ may delay OTP-shaped SMS for non-default apps (see COMPATIBILITY.md).

## Excluded by design

Default-SMS role, notification listener, accessibility, foreground service workarounds.
