## Summary

<!-- One paragraph: what this PR does and why -->

## Changes

### <!-- Component / area -->
- **Problem**:
- **Fix**:

## Test Results

| Test | Result |
|------|--------|
| `./gradlew :app:testDebugUnitTest` | pass / fail |
| `./gradlew :app:assembleDebug` | pass / fail |
| `bash scripts/audit_manifest.sh` | pass / fail |

## Security Check

- [ ] No `android.permission.INTERNET` added (source or merged manifest)
- [ ] No plaintext password storage introduced
- [ ] No new network dependencies

## Physical Device Testing

- [ ] Not applicable (CI/docs only)
- [ ] Tested on physical device
