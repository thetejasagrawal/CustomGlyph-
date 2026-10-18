#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ "${1:-}" != '--accept-license' ]; then
  echo 'Read licenses/NOTHING-SDK.md before using the proprietary Nothing SDK.'
  echo 'Then run: ./scripts/fetch-sdk.sh --accept-license'
  exit 1
fi
SDK_REVISION=999b1143a6bff0dd79db11d21b92c15c56a346b1
SDK_SHA256=be00ee9cd7115f6b11984c6e31fe98e298fb726940d1555063610685ef3bbf29
SDK_FILE=app/libs/glyph-matrix-sdk-2.0.aar
mkdir -p app/libs
SDK_TEMP=$(mktemp)
trap 'rm -f "$SDK_TEMP"' EXIT HUP INT TERM
curl --fail --location --silent --show-error \
  "https://raw.githubusercontent.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/$SDK_REVISION/glyph-matrix-sdk-2.0.aar" \
  --output "$SDK_TEMP"
SDK_ACTUAL=$(shasum -a 256 "$SDK_TEMP" | cut -d ' ' -f 1)
if [ "$SDK_ACTUAL" != "$SDK_SHA256" ]; then
  echo 'SDK checksum mismatch. The downloaded file was not installed.' >&2
  exit 1
fi
mv "$SDK_TEMP" "$SDK_FILE"
echo 'Nothing Glyph Matrix SDK 2.0 verified and installed.'
