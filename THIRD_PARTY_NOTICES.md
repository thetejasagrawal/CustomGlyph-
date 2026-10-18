# Third-party notices

The MIT license at the repository root covers original Custom Glyph code and artwork. It does not relicense third-party libraries, fonts, Android platform components, or the Nothing SDK.

| Component | Version | License |
| --- | --- | --- |
| Nothing Glyph Matrix SDK | 2.0 | [Proprietary EULA](licenses/NOTHING-SDK.md) |
| Doto | Google Fonts / upstream commit `1c587f2` | [SIL Open Font License 1.1](licenses/DOTO.txt) |
| JetBrains Mono | 2.304 | [SIL Open Font License 1.1](licenses/JETBRAINS-MONO.txt) |
| AndroidSVG | 1.4 | [Apache 2.0](licenses/ANDROIDSVG.txt) |
| Glide GIF decoder | 4.16.0 | [Glide notices: BSD, MIT and Apache 2.0](licenses/GLIDE.txt) |
| AndroidX / Compose / Material 3 | Pinned in Gradle | [Apache 2.0](licenses/APACHE-2.0.txt) |
| Kotlin / kotlinx.coroutines | Pinned in Gradle | [Apache 2.0](licenses/APACHE-2.0.txt) |

Full notices are also included in the APK under `assets/licenses/`.

## Nothing SDK

The SDK is a closed-source dependency. Its AAR is **not included in this source repository or the source archive**. The setup script downloads an unmodified copy from Nothing's official repository at a pinned commit and verifies its SHA-256 digest.

Nothing's EULA permits integration through its public APIs, restricts redistribution, and requires prior written permission for commercial use. The free community APK integrates the SDK and includes its EULA; the SDK is not offered as a separate download. The app's MIT license grants no additional rights to that SDK, and no separate Nothing approval is claimed. Anyone reusing or distributing the project must review the SDK's terms separately. Licensing inquiries: GDKsupport@nothing.tech.

## Attribution

Hardware masks and LED spacing are derived from Nothing's published device specifications. SDK calls follow the public documentation and official example project. The app does not modify or reverse engineer the SDK. The Custom Glyph icon and sample C mark are original artwork.

Doto is by Óliver Lalan and the Doto Project Authors. The bundled variable font is used at weight 900 with its rounded-dot axis at 100. Source: https://github.com/google/fonts/tree/main/ofl/doto. JetBrains Mono is used for controls and body text.
