# Worker prompt — Khidki

Read `docs/DECISIONS.md` then `docs/plans/2026-09-10-khidki-execution.md`. Execute **M0 only**, in task order. Do not start M1 until Laraib replies `GO` after brother physical tests.

**Already decided. Do not revisit.** Password is 8 digits, reusable until expiry. Destination equals requester. One armed window globally. OTP-shaped SMS is the product. GitHub Releases APK. Private repo. No license.

**Done for M0** = `./gradlew test assembleDebug` passes locally, CI publishes a `preview` APK, `docs/COMPATIBILITY.md` has emulator/CI evidence only, and `docs/PHYSICAL_TEST.md` is waiting for Laraib/brother. You never fill physical-device pass/fail yourself.

**Stop and ask after two failed fixes.** Never invent Realme GT 6T results. Never add a server, Firebase, internet, notification listener, accessibility, default-SMS role, or foreground service.

Commit and push after every completed task so GitHub Actions can build the APK.
