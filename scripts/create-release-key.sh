#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
if [ -e signing.properties ] || [ -e signing/custom-glyph-release.p12 ]; then
  echo 'Signing identity already exists; preserving it.'
  exit 0
fi
mkdir -p signing
umask 077
GLYPH_STORE_PASSWORD=$(python3 -c 'import secrets; print(secrets.token_urlsafe(36))')
export GLYPH_STORE_PASSWORD
"${JAVA_HOME:?Set JAVA_HOME to JDK 17}/bin/keytool" -genkeypair -noprompt \
  -keystore signing/custom-glyph-release.p12 -storetype PKCS12 \
  -storepass:env GLYPH_STORE_PASSWORD -keypass:env GLYPH_STORE_PASSWORD \
  -alias custom-glyph -keyalg RSA -keysize 3072 -validity 10000 \
  -dname 'CN=Custom Glyph Release, O=Independent Development'
{
  printf 'storeFile=signing/custom-glyph-release.p12\nkeyAlias=custom-glyph\n'
  printf 'storePassword=%s\nkeyPassword=%s\n' "$GLYPH_STORE_PASSWORD" "$GLYPH_STORE_PASSWORD"
} > signing.properties
unset GLYPH_STORE_PASSWORD
printf 'Release signing identity created. Back up signing/ and signing.properties privately.\n'
