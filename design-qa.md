# Custom Glyph — visual QA

Date: 2026-10-18. Native Jetpack Compose app, Android 15 / API 35 emulator.

## Source and evidence

- Source visual truth: user-supplied Nothing OS widget reference, preserved locally at `docs/verification/design/reference.jpg` (original filename `_ - 2026-10-18T213900.433.jpeg`). The reference is not redistributed with the source.
- The later user screenshot identified off-center action icons and explicitly requested centered icons with labels outside their containers.
- Implementation: `docs/screenshots/editor.png`, `adjustments.png`, `gallery.png`, `schedule.png`, `settings.png`, `onboarding.png` and `landscape.png`.
- Full-view comparison: `docs/verification/design/comparison-final.png` puts the reference and the rendered Android editor in the same image.
- Focused comparison: `docs/verification/design/details-final.png` combines the reference with close views of the action column and adjustment controls.
- Additional local evidence: `font-130.png`, `font-200.png`, and `image-controls.png` in `docs/verification/design/`.

The source is a 736 × 736 widget collage, not a specification for this editor screen. This review checks its visual language and the user's alignment correction, rather than claiming an identical screen layout. Photographs and unrelated weather/music widgets are not app assets and were not copied into the app.

## Viewports and normalization

Portrait captures are 1080 × 2400 physical pixels, 420 dpi (2.625 pixels/dp), approximately 411 × 914 dp. Landscape is 2400 × 1080, approximately 914 × 411 dp. System bars are genuine Android UI. CSS size and browser device scale are not applicable.

The full comparison scales the reference proportionally to 640 × 640 and the portrait screenshot to 460 × 1024. Focused crops retain the action icons at their original capture density; the controls crop is proportionally reduced. No screen content was stretched or reconstructed. Font scaling was checked at 100%, 130% and 200%.

## Findings and iterations

1. **[P1, fixed] Controls below the fold.** The first editor capture showed the contrast slider clipped by the panel. Evidence: `comparison-01.png`. Replaced the long form with Light, Move, Place and Image tabs. Light and Move use paired controls; the preview remains fixed. The final portrait capture shows both sliders in full.
2. **[P2, fixed] Circular action alignment.** Labels inside the early circles displaced icons upward, and sizes differed. Evidence: the user's alignment screenshot and `editor-before-alignment.png`. All portrait action circles now use a 52 dp diameter and centered 22 dp icons, with 64 dp label columns underneath. Settings, preview actions, Export and Save share the right-side center line. The final focused comparison verifies the geometry.
3. **[P1, fixed] Landscape preview clipped.** The initial vertical action stack exceeded landscape height. Replaced it with a compact Actions menu and a matrix sized to the remaining height beside the adjustment panel. Evidence after correction: `docs/screenshots/landscape.png`.
4. **[P1, fixed] Large text collapsed the control region.** At 200% text size, wrapping action captions consumed the editor height. The large-text layout now uses the compact Actions menu, a pinned smaller matrix, stacked controls, and labeled accessibility semantics for icon navigation. Evidence after correction: `font-200.png`. At 130%, the regular layout retains both light sliders without clipping (`font-130.png`).
5. **[P2, fixed] GIF playback crowded the controls.** The signed release import check exposed a wide default playback track and reduced control space. Animated designs now use a two-by-two action grid and a thin playback track with a separate frame counter. This preserves the controls beneath the fixed preview. Release import evidence: `docs/verification/design/release-gif-import.png`.

## Fidelity surfaces

- **Fonts and typography:** Doto at weight 900 / rounded-dot axis 100 provides the dot display treatment. JetBrains Mono is used for controls and body text. Both fonts are bundled under the OFL. Numeric values, labels and tabs align; large-text layouts avoid squeezed captions. This is an openly licensed interpretation, not Nothing's proprietary font.
- **Spacing and layout:** 18 dp outer gutters, 10–12 dp module gaps, flat rounded tiles, equal circular actions, a circular matrix, and pill navigation. No nested decorative borders or bevels. The matrix is independently sized and does not scroll away with adjustments.
- **Colors and tokens:** Black `#000000`, charcoal `#1C1C1E`, off-white `#EFEDF1`, muted gray and restrained red. Light tiles use a complete light control palette. Red marks the Apply action and relevant error/step accents. No gradients or decorative shadows.
- **Image quality and assets:** The matrix is the actual processed LED map rendered natively with official per-device masks and aperture spacing. It is not a bitmap mockup. Material outlined icons remain crisp. Original app artwork and the sample C mark are separate from the supplied reference. Transparent-image and export fidelity have automated coverage.
- **Copy and content:** Functional labels and brief device/permission explanations. No account/privacy slogans or promotional taglines. Unsupported hardware is identified as Editor mode; SDK submission is never presented as proof of physical illumination.

## Interaction and accessibility checks

Onboarding, Preview, Settings/setup navigation, brightness changes, gestures, Center, activity recreation, Gallery save, and schedule creation/editing are covered by Android tests. The tab panels, Gallery, Schedule and Settings were also opened for real emulator captures. Circular actions have semantic labels and press feedback. At larger font sizes the compact menu preserves access to Choose Image, Preview, Center, Reset and Export / Share. Android animation duration settings govern Compose animations.

This is native-app QA; browser console checks are not applicable. Android release launch and SDK behavior are tracked separately in `docs/VERIFICATION.md`.

## Implementation checklist

- [x] Compare reference and rendered implementation in combined inputs.
- [x] Correct icon centers, labels, action sizes and shared alignment.
- [x] Keep the preview visible while adjusting controls.
- [x] Verify portrait, landscape and increased font scales.
- [x] Review Gallery, Schedule, onboarding, Settings and all adjustment tabs.
- [x] Preserve native controls, accessibility semantics and actual image processing.

No actionable P0/P1/P2 visual issues remain in the inspected states. Physical Nothing OS rendering and behavior remain unverified; this visual result does not remove that hardware release gate.

final result: passed
