# Khidki v1.3 — World-ready polish plan

**Date:** 2026-09-12  
**Status:** Proposal — awaiting owner approval before implementation  
**Goal:** Make Khidki feel like a finished, shareable product — not a debug sideload — while keeping the locked security model (on-device, no internet, SMS-only).

---

## 1. What “ready to share with the world” means

Someone downloads the APK, installs it without you on a call, and within 5 minutes:

1. Understands what Khidki does and why it needs SMS access  
2. Completes setup (permissions + optional Realme tweaks) without confusion  
3. Creates a rule, arms a window, and sees clear status feedback  
4. Trusts the app (professional icon, consistent copy, privacy clarity)  
5. Does **not** see developer jargon (`[CMD]`, “diagnostic stream”, raw `applicationId`)

| In scope | Out of scope (locked) |
|---|---|
| UI/UX polish, logo, copy, onboarding | Google Play listing |
| Hide/tuck debug tools | Server / cloud / analytics |
| Docs + install guide + privacy URL | Default SMS app pivot |
| Release keystore + v1.3 tag | F-Droid / open-source unless owner decides |

**Target version:** `1.3.0` (user-visible polish release)  
**Distribution:** GitHub Releases (unchanged) — polish makes sideload credible, not Play-ready.

---

## 2. Current gaps (audit summary)

| Area | Problem |
|---|---|
| **Brand** | Launcher icon is geometric placeholder (rectangles); no splash; no cohesive wordmark |
| **Developer UI on main path** | Status tab shows “Live diagnostic stream”, Copy/Clear logs, raw `[IN]`/`[CMD]` tokens |
| **Settings** | “Live regex tester” + raw `BuildConfig.APPLICATION_ID` feel like internal tools |
| **Copy** | “Sideload setup required” — accurate but scary for normal users |
| **Theme** | Dark mode broken on active window card, command dialog, history badges |
| **Configs** | No enable/disable toggle; edit sheet missing preset chips; raw regex on cards |
| **Feedback** | Biometric failure silent; errors not cleared on tab change; Toast-only clipboard |
| **Permissions** | POST_NOTIFICATIONS requested but never explained in UI |
| **Dead code** | `revealedCommand` in ViewModel state, unused string, unused nav-compose dep |
| **Docs** | README/CHANGELOG/COORDINATION still say `preview`, `1.1.0-debug` |

---

## 3. Brand & logo direction

### 3.1 Concept: “Khidki” (window)

The app opens a **short window** for OTP SMS to pass through. Visual metaphor: a **window frame** with light coming through — not a chat bubble, not a lock (too scary), not a forward arrow (implies surveillance).

### 3.2 Logo spec (deliverable)

| Asset | Spec |
|---|---|
| **App icon** | Adaptive icon: rounded-square frame, teal (`#006A60`) background, white/cream window panes with soft glow (`#70F7E5` accent). Readable at 48dp. |
| **Notification icon** | Monochrome white silhouette of window frame (already exists — refine to match icon) |
| **In-app mark** | Small window icon in TopAppBar beside “Khidki” on first launch / About |
| **Feature graphic** (for README / future) | 1024×500: phone + window + “OTP forwarding, on your device” tagline |

### 3.3 Design tokens (refine, don’t reinvent)

Keep existing teal palette from `Theme.kt` — it’s good. Add:

- **Typography:** `Typography` object with distinct `titleLarge` / `labelLarge` weights; optional: single Google Font (e.g. **DM Sans** or **Outfit**) for headings only — keep body system default for performance
- **Spacing scale:** 4 / 8 / 12 / 16 / 24 dp — enforce in cards
- **Elevation:** subtle tonal elevation on cards (M3 `surfaceContainerLow`)

### 3.4 Logo implementation tasks

1. Redesign `ic_launcher_foreground.xml` — curved frame, 2×2 pane grid, subtle depth (not flat rectangles)
2. Match `ic_notification.xml` to new mark
3. Add `res/drawable/ic_khidki_mark.xml` for in-app branding
4. Optional: export 512×512 PNG for README via Android Studio Image Asset Studio

---

## 4. Information architecture (UX)

### 4.1 Tab structure (keep 4 tabs)

No navigation rewrite — tabs work. Improve **content within tabs**.

```
Status     → “Home” mental model: engine, arm window, what’s happening now
Configs    → Rules you’ve set up
History    → Permanent audit log (user-friendly labels)
Settings   → Permissions, privacy, advanced tools
```

### 4.2 Rename & reframe copy

| Current | Proposed |
|---|---|
| Status | **Home** (tab label only; screen title stays “Khidki”) |
| Live diagnostic stream | **Recent activity** |
| `[IN]` / `[CMD]` / `[FWD]` | Human labels: “Message received”, “Window armed”, “Forwarded” |
| Sideload setup required | **Setup required** |
| Master Forwarding Engine | **Forwarding** (switch label) |
| Engine active / paused | **On** / **Off** |
| Copy all logs / Clear log | Move to Settings → **Advanced** (or remove Copy; keep Clear as “Clear activity”) |

### 4.3 First-run onboarding (new)

One-time **3-step bottom sheet** after first launch (flag in `SharedPreferences`):

1. **What Khidki does** — “Forwards OTP SMS to a trusted number, only during a window you control.”
2. **SMS only** — “Does not work with RCS chat. Bank OTPs via SMS work.”
3. **Setup** — “We’ll help you grant permissions next.” → dismiss → show permission card if needed

File: `ui/components/WelcomeSheet.kt`  
No internet — all local copy.

### 4.4 Permission UX (unify)

Single source of truth in Settings **Device health** card:

| Row | Status |
|---|---|
| SMS (receive + send) | ✅ Granted / ⚠️ Required |
| Notifications | ✅ Granted / ⚠️ Recommended (explain why) |
| Restricted settings | Link to App Info |
| Battery / Auto-start | Collapsible “Realme & ColorOS tips” |

Remove duplicate Realme paragraphs scattered across PermissionGate strings vs Settings.

---

## 5. Screen-by-screen polish

### 5.1 Home / Status (`StatusScreen.kt`)

**Hero card**

- Large status pill: **On** (green) / **Off** (amber) / **Setup needed** (red)
- One-line subtitle that changes with state:
  - Off: “Turn on to listen for forwarding windows.”
  - On, no session: “Waiting for a window to be armed.”
  - On, active: “Forwarding window open.”
- Master switch — keep, simplify label

**Timed forwarding card** (`TimedForwardingCard.kt`)

- Already improved in v1.2 — minor polish:
  - Show selected duration more clearly (filled chip)
  - On biometric cancel: Snackbar “Authentication cancelled” (not silent)
  - Disabled state tooltips via helper text

**Active window card**

- Fix dark mode (use `MaterialTheme.colorScheme.primaryContainer`, not hardcoded light teals)
- Show origin badge: “SMS command” vs “Timed arm”
- Countdown: large `MM:SS` typography

**Recent activity** (replaces diagnostic stream)

- Default: last 10 events, human-readable sentences
- Example: `10:42 — OTP from VK-HDFCBK matched and forwarded`
- “Show technical details” expander → monospace raw log (for power users)
- Remove “Copy all logs” from main UI; optional in expanded section

**Empty session card**

- Add small illustration (window icon) + single CTA: “Arm a timed window” scroll hint

### 5.2 Configs (`ConfigsScreen.kt`)

**List cards**

- Label + Active/Disabled toggle (wire `enabled` field — already in domain)
- Masked phone + **chips** for filters (truncate long regex with ellipsis)
- Window duration chip
- Overflow menu: Edit / Disable / Delete (instead of delete-only icon)

**Add / Edit bottom sheets**

- Same fields; **both** get preset chips (Blinkit, HDFC, generic OTP)
- Inline validation with friendly messages (“Phone must start with + and country code”)
- Save button disabled until valid

**Empty state**

- Window illustration + “No forwarding rules yet” + “Tap + to create your first rule”

**Command reveal dialog**

- Fix dark mode colors
- Add emoji-free professional copy: “Share this command once”
- Prominent Copy button (already exists)

### 5.3 History (`HistoryScreen.kt`)

- User-facing summaries only (already partially in `UiUtils.historyEventSummary` — extend)
- Badge colors from theme, not hardcoded `#E2E8F0`
- Empty state: “Events appear when windows arm, OTPs forward, or attempts are blocked.”
- Clear history → confirm dialog with consequence text

### 5.4 Settings (`SettingsScreen.kt`)

**Restructure into sections:**

1. **Permissions & device** — unified health card (SMS, notifications, restricted settings, Realme tips)
2. **Privacy** — short in-app summary + “Read full policy” opens local WebView or `Intent` to hosted `PRIVACY.md` URL
3. **About** — version, build (hide raw applicationId on release builds), link to GitHub releases
4. **Advanced** (collapsed by default)
   - Regex tester (move here from prominent position)
   - Technical activity log export (copy logs)
   - Security invariants (keep — builds trust for technical users)

Remove `BuildConfig.APPLICATION_ID` from About on release builds.

---

## 6. Garbage cleanup (code & resources)

### 6.1 Remove dead code

| Item | File |
|---|---|
| `revealedCommand` in `KhidkiUiState` | `KhidkiViewModel.kt` |
| Unused `hasSmsPermission` param in bottom sheets | `ConfigsScreen.kt` |
| Unused `timed_forwarding_switch_label` | `strings.xml` OR implement switch (prefer remove) |
| Unused `CardBackgroundDark` | `Theme.kt` |
| `navigation-compose` dependency if still unused | `app/build.gradle.kts` |

### 6.2 Consolidate duplicates

| Issue | Fix |
|---|---|
| `enabled` vs `isEnabled` on Configuration | Pick one; migrate TimedForwardingCard to `.enabled` |
| Error message global state | Clear on tab change; use SnackbarHost in Scaffold |
| Permission steps duplicated | Single `PermissionGate.setupSteps()` used by card + Settings |

### 6.3 Gate debug-only UI

```kotlin
// Advanced section visible always, but regex tester + log export
// only if BuildConfig.DEBUG || userEnabledAdvanced in prefs
```

Default: **hidden** for release. Owner can triple-tap version in About to unlock Advanced (Easter egg for power users).

---

## 7. Theme & accessibility

| Task | Detail |
|---|---|
| Dark mode pass | Replace all `*Light` hardcoded colors with `MaterialTheme.colorScheme.*` |
| System bars | `themes.xml`: respect dark mode `windowLightStatusBar` |
| Splash screen | Android 12+ `SplashScreen` API with teal background + icon |
| Content descriptions | All icons in Status, Configs, nav bar |
| Touch targets | Min 48dp for IconButtons |
| Snackbar host | Add to `MainActivity` Scaffold for errors/success |

---

## 8. Strings & localization prep

Externalize all user-visible copy to `res/values/strings.xml` (~80 strings). Benefits:

- Consistent tone review in one file
- Future Hindi (`values-hi`) if owner wants — brand name stays Khidki

**Tone guide:**

- Short sentences. No jargon.
- Honest about risk: “Forwards full SMS content to the number you choose.”
- Never say “sideload” in UI — say “installed outside Play Store” in docs only.

---

## 9. Share-ready distribution package

### 9.1 In-repo docs (sync to v1.3)

| File | Update |
|---|---|
| `README.md` | Hero screenshot, v1.3 install steps, no “preview” |
| `CHANGELOG.md` | Add `[1.3.0]` section |
| `docs/RELEASE.md` | Consistent naming |
| `docs/INSTALL.md` | **New** — step-by-step with screenshots placeholders |
| `docs/PRIVACY.md` | Already good — host copy at GitHub Pages or gist URL |
| `docs/COORDINATION.md` | Mark Phase 8 polish in progress / done |

### 9.2 Release signing

Before sharing widely:

1. Generate release keystore (not debug)
2. GitHub secret `KHIDKI_RELEASE_KEYSTORE_BASE64`
3. CI signs release APKs with stable key → users can update in place

### 9.3 Share bundle (what you send someone)

1. Link to latest GitHub Release APK + sha256
2. `docs/INSTALL.md` (or README install section)
3. Privacy policy URL
4. One-line pitch: *“Khidki forwards OTP SMS to a trusted number — only during a window you arm. Everything stays on your phone.”*

---

## 10. Testing plan

### 10.1 Automated (before merge)

```bash
./gradlew :app:testDebugUnitTest :app:assembleRelease
bash scripts/audit_manifest.sh
```

New tests:

| Test | Covers |
|---|---|
| `UiUtilsTest` | Masking, timestamps, human activity labels |
| `KhidkiViewModelTest` | Error messages, enable/disable config, tab error clear |
| `ConfigurationValidatorTest` | (exists — extend phone edge cases) |

Optional: 1–2 Compose UI tests for empty states (androidTest).

### 10.2 Physical signoff (GT 6T)

Use `docs/PHYSICAL_TEST.md` plus:

- [ ] First-run welcome sheet appears once
- [ ] Dark mode: all cards readable
- [ ] Arm timed window → biometric cancel shows message
- [ ] Disable rule → forwarding stops for that rule
- [ ] Activity feed shows human text by default
- [ ] Advanced section hidden until unlocked
- [ ] Launcher icon looks crisp on home screen

---

## 11. Execution phases (PR batches)

Work in **4 PRs** to keep reviewable diffs. Each PR: tests green, no unrelated changes.

### PR-A: Brand + theme foundation
**Est. 1 day**

- New launcher icon + notification icon + in-app mark
- Splash screen
- `Typography` + dark mode color fixes
- `strings.xml` extraction (phase 1: Status + Permission card)

**Verify:** `./gradlew :app:assembleRelease` — icon visible on device

### PR-B: Home & activity UX
**Est. 1–2 days**

- Welcome sheet (first run)
- Status screen rewrite (hero, activity feed, dark mode active card)
- SnackbarHost + error clearing
- Biometric failure feedback

**Verify:** Physical test Path B (timed arm → OTP)

### PR-C: Configs + History + Settings
**Est. 1–2 days**

- Config enable/disable, chips, edit presets, empty states
- History polish + theme badges
- Settings restructure + Advanced section + notification status
- Remove/gate debug UI

**Verify:** Full rule CRUD + disable flow

### PR-D: Cleanup + docs + release
**Est. 1 day**

- Dead code removal
- README, CHANGELOG, INSTALL.md, RELEASE.md
- Release keystore wiring (if owner provides secret)
- Tag `v1.3.0`

**Verify:** Fresh install from CI release APK on clean device

---

## 12. Success checklist (definition of done)

- [ ] App icon looks professional on home screen (owner sign-off)
- [ ] No developer jargon on default UI path
- [ ] Dark mode works on all screens
- [ ] First-run onboarding completes in < 60 seconds
- [ ] All 4 tabs have helpful empty states
- [ ] Settings shows permission + notification status
- [ ] README install guide matches v1.3 release APK name
- [ ] Privacy policy link works
- [ ] Unit tests pass; physical test matrix signed off
- [ ] `CHANGELOG.md` documents v1.3.0 for recipients

---

## 13. Decisions needed from Laraib (before coding)

1. **Tab rename:** Status → “Home”? (Recommended: yes)
2. **Advanced unlock:** Hidden by default with triple-tap version? (Recommended: yes)
3. **Font:** System default only, or one custom heading font? (Recommended: system only for v1.3 — ship faster)
4. **Repo visibility:** Public for easier sharing? (Your call — affects privacy policy hosting)
5. **Release keystore:** Create now before v1.3 tag? (Recommended: yes)
6. **Hindi UI:** v1.3 or later? (Recommended: later)

---

## 14. Recommended order of work

```
Approve plan → PR-A (brand/theme) → PR-B (home/onboarding) → PR-C (configs/settings) → PR-D (docs/release) → v1.3.0 tag → share
```

**Do not start implementation until you approve §13 decisions** (defaults above are fine to accept with “go ahead”).

---

## References

- Prior UI plan: `docs/plans/2026-09-11-phase6-ui-ux-revamp.md`
- Distribution: `docs/plans/2026-09-12-play-store-release.md`
- Locked product rules: `docs/DECISIONS.md`
