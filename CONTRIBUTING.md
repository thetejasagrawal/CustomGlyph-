# Contributing

Use JDK 17 and Android SDK 36. Follow the setup steps in the README, including Nothing's SDK license. Do not commit the vendor AAR, signing keys, `signing.properties`, `local.properties`, or build outputs.

Before opening a pull request:

```sh
./gradlew spotlessApply
./gradlew spotlessCheck :app:testDebugUnitTest :app:lintRelease :app:assembleRelease
./gradlew :app:connectedDebugAndroidTest # emulator or connected phone
```

Keep rendering, scheduling and routing policy independent of the Android UI. Add behavioral tests for changes to those policies. Imported files are untrusted: retain decoding limits and never add automatic network/resource resolution to SVG import.

Document device and Nothing OS versions when changing SDK integration. State exactly what was tested on physical hardware. Do not infer successful illumination from a successful SDK call, spoof device identity, or bypass firmware timeouts.

UI copy should name the action or state. Avoid taglines, promotional sentences and redundant reassurance. Keep the matrix visible when adding controls. Preserve 48 dp touch targets, readable contrast and accessibility labels.

Use a short problem / change / verification description for pull requests. Include screenshots for UI changes and note any permissions or dependencies added. Issue reports should include reproduction steps, device model, Nothing OS version, application version and expected versus observed behavior; remove personal images or logs before posting.
