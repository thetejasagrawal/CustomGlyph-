# Custom Glyph 1.0 — implementation plan

Build a native Kotlin / Jetpack Compose Android app for custom Nothing Glyph Matrix designs. Keep the interface monochrome, use monospace typography and restrained red accents, and write plain functional copy.

## Requirements

- Detect Phone (3) and Phone (4a) Pro; use their official LED geometry and supported SDK routes.
- Import PNG, JPEG, WebP, SVG and animated GIF with bounded decoding and transparent image support.
- Edit scale, rotation, placement, brightness, contrast, silhouette, inversion and dithering. Preview the same pixels submitted to the SDK.
- Save multiple designs, export PNG / SVG pixel maps, and share rendered images.
- Schedule saved designs by local time and weekdays; add an automatic stop timer.
- Provide brief onboarding, device setup, animation options, battery settings and an About page linking to PossiblyTejas.
- Use Nothing's official SDK and AOD Toy service. Report restrictions and failures without claiming hardware success.
- Package an installable signed APK. Publish a clean source repository with build instructions, dependency notices and verification results.

## Tasks

- [x] Verify official SDK capabilities, device geometry and repository access.
- [x] Refactor hardware profiles and rendering for both devices.
- [x] Add safe image / SVG / GIF decoding and animation playback.
- [x] Add saved designs, schedules, stop timer, export and sharing.
- [x] Finish Custom Glyph branding, monospace UI, onboarding, settings and icon.
- [x] Test processing, storage, scheduling, UI and release packaging.
- [x] Complete README, licenses, contributor guidance and reproducible build setup.
- [x] Push source to GitHub and package the signed APK for delivery.

## Release validation

Emulator tests can verify the editor, file formats, storage, export, schedules and app lifecycle. Physical Glyph output, firmware registration, AOD binding, battery behavior and locked-device illumination require testing on both supported Nothing phones. No software-only test substitutes for those checks.

## Follow-up requirements implemented

- Keep the preview fixed while controls scroll independently.
- Place the header, preview, controls, Apply action and bottom navigation in floating containers.
- Replace Designs with Gallery; keep recent edits automatically.
- Add a permission/setup checklist with direct Android requests, settings shortcuts and state rechecks.
- Coordinate lock/unlock display ownership on Phone (3), bound reconnection attempts and explicit Always-On toy selection.
- Detect the older Glyph Studio package and link to its settings to stop competing requests.
- Remove taglines and promotional filler from the app copy.

## Reference-driven redesign

- [x] Replace outlined nested cards with flat black, charcoal and off-white widgets.
- [x] Use licensed Doto headings and numeric values with JetBrains Mono controls.
- [x] Center action icons in equal 52 dp circles; put labels below.
- [x] Split controls into Light, Move, Place and Image tabs, keeping the matrix fixed.
- [x] Redesign Gallery, Schedule, onboarding and Settings in the same visual language.
- [x] Adapt landscape and large text layouts without hiding the preview or primary actions.
