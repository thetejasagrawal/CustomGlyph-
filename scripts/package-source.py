#!/usr/bin/env python3
"""Create a source archive without build outputs, proprietary binaries, or signing secrets."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[1]
excluded = {'.gradle', '.kotlin', '.idea', '.spotless', 'build', 'vendor', 'dist', 'signing', '.git', 'verification'}
skipped = {'local.properties', 'signing.properties', '.DS_Store'}
output = root / 'dist' / 'CustomGlyph-1.0-source.zip'
output.parent.mkdir(exist_ok=True)
with ZipFile(output, 'w', ZIP_DEFLATED) as archive:
    for path in sorted(root.rglob('*')):
        rel = path.relative_to(root)
        if (path.is_file() and not any(part in excluded for part in rel.parts)
                and path.name not in skipped and path.suffix not in {'.aar', '.p12', '.jks', '.keystore', '.pem'}):
            archive.write(path, Path('CustomGlyph') / rel)
print(output)
