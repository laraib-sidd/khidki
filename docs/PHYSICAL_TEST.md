# Physical test — Realme GT 6T

Worker never fills the Result column. Laraib or brother does.

Install the `preview` APK from the private GitHub Release. This is sideload, not Play.

## Before any SMS

1. Settings → Apps → Khidki → overflow → **Allow restricted settings**.
2. Permissions → SMS → Allow.
3. Realme: Auto-start = on, Allow background activity = on, battery optimization = unrestricted for Khidki.
4. Open Khidki once. Grant SMS if prompted. Leave the UI.
5. Confirm the phone is not the default SMS app. Do not make it the default SMS app.

## Trials

Use brother’s phone as the peer number. Never a live bank OTP.

| ID | What to send / do | Phone state | Result (pass / fail / time) | Notes |
|---|---|---|---|---|
| P0 | Restricted settings + SMS grant | — | | |
| P1 | Peer → GT 6T: `KHIDKI-M0-PLAIN 001` | Khidki not in front, screen on | | Must appear in Khidki log |
| P2 | Peer → GT 6T: `KHIDKI-M0-PLAIN 002` | Screen off | | |
| P3 | Peer → GT 6T: `Your OTP is 482193. Do not share with anyone.` | UI closed | | **Product gate.** Fail here stops M1. |
| P4 | Peer → GT 6T: `<#> 482193 XYZaBcdEfGh` | UI closed | | Retriever-shaped. Record even if delayed. |
| P5 | Peer → GT 6T: `KHIDKI-M0-UNICODE नमस्ते` | UI closed | | |
| P6 | Khidki Send to peer: `KHIDKI-M0-SEND 001` | UI open | | Record sent callback and delivery (or missing) |
| P7 | Repeat P6 | Screen off | | |
| P8 | Revoke SMS permission, then P1 | UI closed | | Must not crash. Must not send. |
| P9 | Airplane mode, then P6 | Any | | Send fails. No retry. |
| P10 | Reboot, unlock, then P3 | After first unlock | | |

A miss on P1/P2 (plain) is a platform failure. A miss on P3 (OTP-shaped) is a product stop. Do not diagnose P3 from P1.

When finished, paste this table into an issue or reply `GO` / `STOP` with the filled table.
