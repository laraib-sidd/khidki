# Khidki architecture

On-device Android app. SMS command opens a forwarding window; matching OTP-shaped SMS is forwarded back to the same trusted number.

## Layers

| Layer | Package | Role |
|---|---|---|
| UI | `dev.laraib.khidki.ui` | Compose: Status, Configs, History, Settings |
| Platform | `dev.laraib.khidki.platform` | SMS receiver, SmsManager transport, boot receiver, system clock |
| Domain | `dev.laraib.khidki.domain` | Parser, auth, RE2/J filters, session engine, budget (JVM-testable) |
| Data | `dev.laraib.khidki.data` | Room, Keystore HMAC verifiers, preferences |
| Bridge | `dev.laraib.khidki.data.adapter` | Sync adapters from suspend Room to domain ports |

## Flow

1. `SmsReceivedReceiver` receives `SMS_RECEIVED`.
2. `SmsInboundProcessor` parses `req <8-digit>` commands or treats message as forward candidate.
3. `ForwardingEngine` authenticates, arms session, matches filters, sends via `AndroidSmsTransport`.
4. Metadata-only events persist in Room `history`.

## Constraints

- No server, no INTERNET permission.
- One global armed session.
- Destination always equals requester.
- Credentials: 8-digit, reusable until expiry, Keystore HMAC tags in DB.
