# Verification — Custom Glyph 1.0

Date: 2026-10-18. Build environment: JDK 17, Android SDK 36, AGP 8.10.1, Kotlin 2.1.20, Gradle 8.11.1. Emulator: Android 15 / API 35, arm64, 1080 × 2400.

## Automated checks

- **20 unit tests passed.** Hardware masks and symmetry, alpha compositing, brightness, silhouette/inversion, clipping, rotation/translation, dithering, input normalization, accelerometer hysteresis, display policy, stale sensor fallback, lock/unlock route ownership, schedule boundaries/overnights/overlaps/DST, stop deadlines and animation timing.
- **9 Android instrumented tests passed; 1 hardware-only test skipped.** Onboarding/settings navigation; preview remains fixed while scrolling brightness controls; gesture placement and activity recreation; PNG/JPEG/WebP/SVG/GIF decoding; GIF frame disposal and delays; rejected external SVG entities; Gallery/schedule persistence; Gallery save, schedule creation and editing through UI; PNG/SVG export fidelity; Always-On intent ordering and missing/restricted activity fallback. Intent routing uses test doubles; those tests do not verify a Nothing OS screen exists.
- **Formatting passed** with Spotless / ktfmt.
- **Release lint: no errors.** Remaining warnings concern optional Kotlin extension style and newer dependency versions. Dependency versions are intentionally pinned.
- A clean rebuild completed before release packaging. No device identity was spoofed and no SDK success was simulated.

## APK checks

The signed APK passed Android `apksigner`, `zipalign` (including 16 KiB page alignment) and package metadata inspection. The signed, minified release APK installed successfully on the Android 15 emulator and cold-launched successfully (1.101 seconds reported by Activity Manager on the final build); no app crash was found in the launch log. The checksum and public signing-certificate digest accompany the APK in `dist/`; private keys are excluded from source and archives. APK SHA-256: `d6261d76f6361fa43b068e3ae8c6ec0b82b0170e89d4cebadc969b05f88a3844`. The APK signature verifies (v2, RSA 3072), package ID is `dev.customglyph`, version is `1.0` / code `2`, minimum SDK is 33 and target SDK is 36.

Build 2 installed over the signed build 1 without clearing app data; the imported GIF was retained. The signed release also imported an animated, transparent GIF through Android’s document picker and displayed its playback controls without crowding the adjustment panel.

UI screenshots are real emulator captures. They show **Editor mode**, since the emulator has no Glyph Matrix. Portrait, landscape and 130% / 200% system font scales are checked. The preview and controls use separate layout/scroll regions. At large text sizes, editing actions move into a menu and the controls stack vertically. See [visual QA](../design-qa.md).

## Physical release gate

The following checks **are not yet verified on physical hardware**. Perform them on each supported model and record Nothing OS build numbers before describing a public binary as hardware-verified.

| Check | Phone (3) | Phone (4a) Pro |
| --- | --- | --- |
| SDK registration / firmware entitlement | Pending | Pending |
| Correct LED mapping and brightness | Pending | Pending |
| Toy discovery and Always-On selection | Pending | Pending |
| Direct Always-On picker shortcut on installed firmware | Pending | Pending |
| Face-down / face-up transitions while locked | Pending | Pending |
| Unlocked direct app output | Pending | Not offered |
| GIF playback and OS suspension behavior | Pending | Pending |
| Schedule and stop-timer changes in AOD/deep sleep | Pending | Pending |
| Reboot restoration and background battery policy | Pending | Pending |
| Other toys / notifications take priority safely | Pending | Pending |
| Ten repeated lock/unlock cycles without clock/image switching | Pending — reported issue | Pending |

### Phone (3) clock-switching regression

1. Stop/uninstall the older Glyph Studio prototype and other third-party matrix controllers.
2. Select Custom Glyph as the system **Always-On toy**, replacing the clock. A Glyph Button selection alone is insufficient.
3. Apply a static design, keep the phone unlocked for two minutes, then lock it face down for two minutes.
4. Lift it face up while locked; the app should request off. Unlock and confirm the design resumes.
5. Repeat ten times, then repeat with Show while unlocked disabled and with a GIF.
6. Record interruptions, Nothing OS version, the app's status and whether another toy/notification was active.

The code now keeps a single SDK owner, releases direct app output on lock, gives bound Toy output priority, ignores stale callbacks, retries unexpected disconnects with a bounded backoff, and lets fresh face-up sensor readings override AOD fallback. These are software fixes; their physical outcome remains to be confirmed.

## Distribution gate

Original source code is MIT licensed. The SDK AAR is excluded from the public source. Nothing's SDK EULA separately restricts redistribution; the signed APK includes that SDK. Public binary publication requires resolving those terms. No approved production Nothing API key was obtained for this build; the default is the official example's `test` value.

## Always-On shortcut — build 2

The first card in Settings and display setup opens the firmware’s `AodToySelectActivity`. A missing or restricted activity falls back to the documented Glyph Toys manager and then Android Settings; directions appear for the fallback. The app does not change the selected toy or mark it confirmed automatically. Navigation tests cover successful dispatch, missing activities, access denial and an unavailable Settings app. The actual picker and its return behavior require a Nothing Phone; the emulator cannot verify them.
