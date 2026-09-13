# Test plan

## Automated (CI)

- Domain: parser, phone normalize, RE2 matcher, forwarding engine.
- Data: Room schema creation, Keystore constant-time helper.
- Manifest contract: SMS permissions only, no INTERNET.

## Physical (required before trusting OTP forwarding)

See `docs/PHYSICAL_TEST.md` on a physical device with a second phone as peer.

Gate: **P3 OTP-shaped synthetic must pass.**

## Not claimed without evidence

Battery %, p95 latency on device, Play/F-Droid approval, universal OEM compatibility.
