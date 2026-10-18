# Security

Report sensitive vulnerabilities through GitHub's private vulnerability reporting feature if available. Do not post personal media, private signing keys, access tokens, or exploit data in public issues. For non-sensitive bugs, use a normal issue.

Custom Glyph does not request network or broad storage access. Import uses Android's document picker, then copies files into app-private storage. SVG external resources and entities are rejected. GIFs have bounded dimensions, frames and duration. Shared exports use a scoped FileProvider with temporary read permission.

The exported Glyph Toy service is required by Nothing OS. It accepts only documented lifecycle/AOD messages; it never accepts external pixel data or file paths. SDK registration is still enforced by Nothing OS.

The release signing key is private and not part of this repository. Contributor builds cannot replace an installed official build without the same signing identity. The proprietary SDK is downloaded from a pinned official revision and verified by SHA-256.
