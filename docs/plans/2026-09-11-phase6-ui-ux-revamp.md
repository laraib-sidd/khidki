# Phase 6 Execution Plan: Production UI/UX Overhaul for Khidki

**Target**: Personal Android SMS Forwarding App (`dev.laraib.khidki`)  
**Target Device**: Realme GT 6T (Android 16 / ColorOS)  
**Role Distribution**:
- **Planner (Gemini)**: Specifications, UI architecture, security constraints, verification criteria, handoff package.
- **Worker (Composer)**: Implementation, Compose refactoring, `./gradlew` tests, atomic commits, PR creation.

---

## 1. Executive Summary & Problem Statement

Owner testing on the physical Realme GT 6T confirmed that while backend persistence and Android 15/16 restricted permissions work, the current UI is cramped, unscrollable, poorly styled, and ergonomically broken on a real device:
1. Inline text fields in `ConfigTab` cause soft keyboard overflows and render on top of an unweighted `LazyColumn`.
2. Raw epoch timestamps (`1726038492000`) and raw enum strings (`SESSION_ARMED`) are dumped directly into History.
3. Event logs are trapped inside an unscrollable fixed-height 160.dp box.
4. Settings are unformatted raw text strings.
5. Generated commands appear at the bottom of the screen where they can be clipped.

This plan details the full component-level revamp into a production-grade Material 3 mobile application.

---

## 2. Architecture & File Structure

Refactor the monolith `MainActivity.kt` into clean, maintainable composable modules:

```
app/src/main/java/dev/laraib/khidki/ui/
├── MainActivity.kt                      # Thin activity entry point, permission launcher, scaffold
├── KhidkiViewModel.kt                   # UI state holder & business actions
├── ConfigurationValidator.kt            # RE2 validation utilities
├── theme/
│   └── Theme.kt                         # Material 3 colors, shapes, typography
├── components/
│   ├── UiUtils.kt                       # Formatting (E.164 masking, timestamps, clipboard)
│   ├── PermissionPreflightCard.kt       # Sideload restricted permission guidance
│   ├── CommandRevealDialog.kt           # AlertDialog for revealed passwords
│   └── ConfirmDeleteDialog.kt           # Confirmation modal before deleting rules
└── screens/
    ├── StatusScreen.kt                  # Hero card, active countdown bar, event stream
    ├── ConfigsScreen.kt                 # Rule list, FAB, ModalBottomSheet creation flow
    ├── HistoryScreen.kt                 # Formatted audit cards, type badges, clear history
    └── SettingsScreen.kt                # Device health, security invariants, live regex tester
```

---

## 3. Screen-by-Screen Specifications

### 3.1 Design System (`theme/Theme.kt`)
- **Color Palette**:
  - Light: Primary `#006A60` (Teal), Secondary `#4A635F`, Surface `#F4FBF7`, Card `#FFFFFF`.
  - Dark: Primary `#53DBC9` (Teal), Secondary `#B1CCC7`, Surface `#191C1B`, Card `#222625`.
  - Status Indicators:
    - Ready / Active: Green `#1B873F` / `#E8F5E9`
    - Paused / Pending: Amber `#D97706` / `#FFFBEB`
    - Error / Blocked: Red `#DC2626` / `#FEF2F2`
    - Inbound / Info: Blue `#2563EB` / `#EFF6FF`
- **Shapes**: `16.dp` card corners, `24.dp` dialogs, `12.dp` chips.

### 3.2 Status Tab (`screens/StatusScreen.kt`)
1. **Hero Status Card**:
   - Status Chip:
     - `READY`: Green badge with check icon, "Engine Active"
     - `PAUSED`: Amber badge with pause icon, "Engine Paused"
     - `BLOCKED_PERMISSION`: Red badge with warning icon, "SMS Permission Needed"
   - Master Switch with clear label ("Master Forwarding Engine") and description ("Toggle all inbound SMS evaluation and forwarding").
2. **Active Window Card** (Visible only when an active session exists):
   - Highlighted card with accent border.
   - Header: "Active Forwarding Window: {label}".
   - Target phone masked: `{+91 •••• ••21}`.
   - Live Countdown with LinearProgressIndicator: shows remaining time (`02:45 remaining`) updated every second.
   - Destructive button: "Cancel Active Window" (calls `viewModel.cancelActiveSession()`).
3. **Empty Session Banner** (Visible when no active session):
   - Muted card: "No active window. Engine is armed and awaiting 'req <password>' SMS."
4. **Activity Feed Card**:
   - Title: "Live Diagnostic Stream" with event count.
   - Action buttons: "Copy All Logs" and "Clear Log".
   - Scrollable list of recent events (max 50) rendered with color-coded chips:
     - `[IN]` (Blue): Inbound SMS received
     - `[CMD]` (Green): Valid command recognized
     - `[FWD]` (Teal): SMS forwarded to requester
     - `[DROP]` (Amber): Filtered out / rejected
   - Monospace font for log messages.

### 3.3 Configs Tab (`screens/ConfigsScreen.kt`)
1. **Rule List View**:
   - Header with count: "Forwarding Rules ({count}/20)".
   - If empty: Illustrated placeholder with "No rules configured. Tap + to add a forwarding rule."
   - Config Cards:
     - Top row: Rule label (bold title) + "Active" badge.
     - Requester phone: Formatted and masked (`+91 •••• ••21`).
     - Chips for Sender Filter and Content Filter patterns.
     - Window duration: "5 min window".
     - Delete icon button: Triggers `ConfirmDeleteDialog`.
2. **Floating Action Button (FAB)**:
   - Fixed at bottom-right (`+ New Rule`).
3. **ModalBottomSheet (`AddRuleBottomSheet`)**:
   - Header: "New Forwarding Rule".
   - Form Fields:
     - **Label**: OutlinedTextField with hint "e.g. HDFC NetBanking".
     - **Requester Phone**: OutlinedTextField (`KeyboardType.Phone`) with hint `+919876543210`. Shows phone validation status.
     - **Sender Filter (Regex)**: OutlinedTextField with syntax validation.
       - Preset chips: `All Banks (VK|VM|AD|QP...)`, `HDFC`, `SBI`, `ICICI`, `Any (.*)`. Clicking fills field.
     - **Content Filter (Regex)**: OutlinedTextField with syntax validation.
       - Preset chips: `OTP Only`, `Transactions`, `All Messages (.*)`. Clicking fills field.
     - **Live RE2 Validation Banner**: Green checkmark if both regexes valid; clear red error if invalid.
     - **Action**: "Save & Generate Access Code" button (disabled if inputs invalid or count >= 20).
4. **Command Reveal Dialog (`CommandRevealDialog.kt`)**:
   - Shown when rule is saved.
   - Title: "🔐 Forwarding Command Ready".
   - Body: "Share this exact command with the requester. The 8-digit password is only generated once."
   - Command display: Large bold monospace box: `req 84920192`.
   - "Copy Command" button with clipboard copy and toast confirmation.
   - "Dismiss" button.

### 3.4 History Tab (`screens/HistoryScreen.kt`)
1. **Header**: "Audit Trail ({count} events)" with "Clear History" button (triggers confirmation).
2. **Empty State**: "No audit events recorded yet."
3. **Event Cards**:
   - Formatted relative timestamp: "Just now", "5m ago (11:24:05)", "Yesterday (18:10)".
   - Event Type Badge:
     - `SESSION_ARMED` -> Green badge "Session Armed"
     - `SESSION_SUBMITTED` -> Blue badge "Session Request Received"
     - `AUTH_FAILURE` -> Red badge "Authentication Failed"
     - `DUPLICATE_REJECTED` -> Amber badge "Duplicate Blocked"
     - `SESSION_TERMINAL` -> Slate badge "Session Closed"
   - Event summary with detail expansion.

### 3.5 Settings Tab (`screens/SettingsScreen.kt`)
1. **Device Health & Permissions Card**:
   - SMS Permission status: "Granted" (Green) or "Restricted / Denied" (Red).
   - "Open App Info" button to allow restricted settings on Android 15/16.
   - Background execution notes for Realme GT 6T (Auto-start, Battery optimization exemptions).
2. **Security & Privacy Invariants Card**:
   - Zero Internet verified: `android.permission.INTERNET` strictly absent.
   - Access Code: 8-digit CSPRNG, 5-minute single-use window.
   - Lockout: 5 failed attempts locks phone for 15 minutes.
   - Budget: Max 20 SMS forwarded per window.
   - Storage: Local Room database, hardware-backed Android Keystore.
3. **Interactive Regex Tester (Live Scratchpad)**:
   - Embedded utility to test regexes against sample SMS before creating rules!
   - Test Input: Sender (e.g. `VK-HDFCBK`) and Body (e.g. `Your OTP is 928371 for login`).
   - Patterns: Sender Regex and Content Regex.
   - Live Result: Instant green "MATCH - Would Forward" or red "NO MATCH - Would Drop".

---

## 4. Invariants & Guardrails (Non-Negotiable)

1. **Zero Internet**: Never add `android.permission.INTERNET` to `AndroidManifest.xml`.
2. **Audit Script**: `bash scripts/audit_manifest.sh` must exit 0.
3. **Test Suite**: All 62 unit tests (`./gradlew :app:testDebugUnitTest`) must stay green.
4. **Append-Only Memory**: Any updates to `docs/MODEL_MEMORY.md` must be additions only.
5. **No Regressions**: Existing `KhidkiViewModel` methods and data layer bindings must be preserved.

---

## 5. Ordered Execution Steps & Verification Gates

| Step | Action | Files Touched | Verification Gate |
|---|---|---|---|
| **1** | Design System & Helpers | `ui/theme/Theme.kt`, `ui/components/UiUtils.kt` | Compiles cleanly |
| **2** | Dialogs & Permission Components | `ui/components/PermissionPreflightCard.kt`, `ui/components/CommandRevealDialog.kt`, `ui/components/ConfirmDeleteDialog.kt` | Compiles cleanly |
| **3** | Status Screen Overhaul | `ui/screens/StatusScreen.kt` | Renders hero card, live countdown, activity stream |
| **4** | Configs Screen Overhaul | `ui/screens/ConfigsScreen.kt` | BottomSheet form, presets, live RE2 check, FAB |
| **5** | History Screen Overhaul | `ui/screens/HistoryScreen.kt` | Formatted timestamps, color badges, clear dialog |
| **6** | Settings Screen Overhaul | `ui/screens/SettingsScreen.kt` | Health cards, security notes, live regex scratchpad |
| **7** | MainActivity Integration | `ui/MainActivity.kt` | Thin scaffold routing to the 4 screens |
| **8** | Full Verification | Entire app module | `./gradlew :app:testDebugUnitTest && bash scripts/audit_manifest.sh` |
| **9** | PR Creation | Git / GitHub | Atomic commits, push, PR opened via `gh pr create` |

---

## 6. Definition of Done

- All 4 screens (Status, Configs, History, Settings) follow the Material 3 design system.
- Config creation is handled via `ModalBottomSheet` with preset filter chips and live RE2 feedback.
- Revealed commands display in an `AlertDialog` with one-tap clipboard copy.
- History events show formatted date/time strings and human-readable badges instead of raw epoch timestamps and enum names.
- Settings includes live device health and an interactive regex scratchpad.
- `./gradlew :app:testDebugUnitTest` passes (all 62 tests green).
- `bash scripts/audit_manifest.sh` passes.
- PR opened on GitHub against `main`.
- `docs/MODEL_MEMORY.md` appended with completion log.
