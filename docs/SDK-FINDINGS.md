# SDK integration record

Checked against Nothing's [Glyph Matrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit) at commit `999b1143a6bff0dd79db11d21b92c15c56a346b1`, and its [official example project](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Example-Project), on 2026-10-18.

- SDK AAR: `glyph-matrix-sdk-2.0.aar`; SHA-256 `be00ee9cd7115f6b11984c6e31fe98e298fb726940d1555063610685ef3bbf29`.
- Minimum SDK required by the AAR: Android 13 / API 33.
- Phone (3): `Common.is23112()`, target `Glyph.DEVICE_23112`, 25 × 25 grid, 489 LEDs.
- Phone (4a) Pro: `Common.is25111p()`, target `Glyph.DEVICE_25111p`, 13 × 13 grid, 137 LEDs. AOD toys only; no Glyph Touch.
- Row masks and aperture ratios are read from `image/23112_spec.svg` and `image/25111_spec.svg`.
- `register(String)` returns a boolean in the shipped public API, although the README table lists void. The app checks the boolean.
- Required permission: `com.nothing.ketchum.permission.ENABLE`. The system determines its protection level.
- App requests use `setAppMatrixFrame`; supported on system versions dated 2025-08-01 or later. Nothing explicitly warns that using `setMatrixFrame` for app control can conflict with active toys.
- Toy requests use `setMatrixFrame` only while the OS binds the Toy service. AOD support metadata is `com.nothing.glyph.toy.aod_support=1`.
- The selected AOD toy receives `GlyphToy.EVENT_AOD` once per minute. The OS controls the bind, unbind and display duration.
- The SDK does not acknowledge physical illumination. The app reports registration failures and request errors, and distinguishes submission from confirmation.
- The setup shortcut first attempts `com.nothing.thirdparty/com.nothing.thirdparty.matrix.toys.manager.AodToySelectActivity`, an observed firmware activity also referenced by [Matrix Lab](https://github.com/alex-1121/glyph-matrix-lab#quick-start). This is not a documented SDK guarantee: missing activities and security restrictions fall back safely, without private Settings fragments, root, or accessibility automation. Its availability on the user’s Phone (3) remains unverified.
- A documented fallback manager shortcut is `com.nothing.thirdparty/com.nothing.thirdparty.matrix.toys.manager.ToysManagerActivity`. There is no documented API to set or read the user's Always-On toy selection. Setup uses a clearly labeled user confirmation for that item.
- The example uses `NothingKey=test`. No production entitlement was obtained as part of this build. An optional Gradle property replaces it.
- No hidden APIs, wake locks, root access, firmware timeout bypass, or undocumented `setGlyphMatrixTimeout` calls are used.

## Lock/unlock ownership

One process-wide controller owns the SDK singleton. Toy binding has priority. Phone (3)'s direct app route is eligible only when interactive and unlocked. Locking closes the app route and leaves face-down output to the selected OS Toy. Unbinding a Toy does not start a competing app request while locked. Fresh sensor readings override AOD fallback; after two seconds without readings, a system AOD callback can supply the face-down indication. This avoids blocking the toy with a stale face-up reading after background sensor suspension. Connection callbacks carry generation IDs; stale callbacks are ignored. Unexpected disconnections have three bounded reconnect attempts.

The policy is tested for all lock/bind combinations. The reported clock/image alternation must still be retested on the affected physical Phone (3); firmware behavior and the selected AOD toy cannot be reproduced on an emulator.

## Processing

Transforms are applied before calling the SDK; the SDK receives an already positioned native-size bitmap at 100% scale and zero rotation. This avoids coupling pixel placement to vendor transform behavior. The public SDK frame builder converts grayscale to the device's native intensity format. No native raw brightness range is guessed.

GIF playback uses the same maps as the preview. Playback is limited to 12 fps and never takes a wake lock. AOD playback may pause when the system suspends the process; static images are the most predictable AOD content.

## Distribution

The EULA grants SDK integration rights, describes the SDK as closed source, restricts redistribution in §2.1(c), and requires written permission for commercial use in §2.2. Original app code is MIT licensed. The vendor AAR is excluded from source distribution. The signed local APK contains the SDK; public binary distribution requires resolving the vendor's terms.
