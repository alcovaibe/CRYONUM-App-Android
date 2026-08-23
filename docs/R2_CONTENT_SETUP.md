# Cloudflare R2 content setup

The Android client accepts content only from `https://download.icymath.com/` and only after verifying `manifests/content-v1.signed.json`. It never lists the bucket and does not use `releases/`.

## Object keys

Use the existing case-sensitive English lecture object keys below. Spaces and capitalization are part of each key and must not be changed:

```text
lectures/Basic Algebraic Structures.pdf
lectures/Divisibility in the Ring of Integers.pdf
lectures/GCD and LCM. Coprime Integers.pdf
lectures/Prime Numbers.pdf
lectures/Numerical Congruences.pdf
lectures/Solving Congruences.pdf
lectures/Complex Numbers. Part 1.pdf
lectures/Complex Numbers. Part 2.pdf
lectures/Systems of Linear Equations. Gauss Method.pdf
lectures/Matrices.pdf
lectures/Determinants.pdf
lectures/Permutations.pdf
privacy-policy/v1/privacy-policy.pdf
manifests/content-v1.signed.json
```

`lecture-01` through `lecture-12` remain stable internal manifest IDs; they are not R2 file names. R2 object keys are case-sensitive. In an HTTP URL, clients encode each space as `%20`, but the stored object key still contains an ordinary space.

The policy path above is still the recommended production target, not a confirmed current key. The current policy folder may be named `Privacy_policy` or something else; verify the real object key in R2 rather than guessing. Migrate it to `privacy-policy/` before publishing a manifest that names the recommended path. Do not overwrite an already published object. For a future lecture revision, keep the English filenames but publish them under a new version prefix such as `lectures/v2/`, update the Android allowlist when that migration is planned, increment `contentVersion` and manifest `revision`, then publish the new manifest last.

PDF metadata:

```text
Content-Type: application/pdf
Content-Encoding: (absent)
Cache-Control: public, max-age=31536000, immutable
```

Give the signed manifest a short cache lifetime or require revalidation, for example `Cache-Control: public, max-age=300, must-revalidate`. Upload every new PDF first and the signed manifest last. This prevents a valid manifest from referencing objects that are not yet available.

After the custom domain works, disable the public `r2.dev` development URL. Cloudflare documents `r2.dev` as a development-only, rate-limited endpoint; the custom domain remains available when that URL is disabled.

## Offline signing

Create the P-256 private key outside this repository on a protected offline machine:

```bash
openssl ecparam -name prime256v1 -genkey -noout -out /secure/offline/content-2026-01-private.pem
openssl ec -in /secure/offline/content-2026-01-private.pem -pubout -out /secure/offline/content-2026-01-public.pem
```

Never copy the private key into Git, R2, BuildConfig, CI artifacts, or the Android app. The Android app needs only the public SubjectPublicKeyInfo value. Obtain its base64 form and add it under the matching key ID in `ProductionContentKeys.TRUSTED_X509_KEYS`:

```bash
openssl pkey -pubin -in /secure/offline/content-2026-01-public.pem -outform DER | openssl base64 -A
```

Prepare a local directory containing exactly the 12 English filenames listed above, then run:

```bash
python3 tools/build_signed_content_manifest.py \
  --lectures-dir /path/to/lectures \
  --privacy-pdf /path/to/privacy-policy.pdf \
  --revision 1 \
  --content-version 1 \
  --key-id content-2026-01 \
  --private-key /secure/offline/content-2026-01-private.pem \
  --output /tmp/content-v1.signed.json
```

The helper checks that the directory contains exactly those 12 case-sensitive English PDF names, verifies `%PDF-`, calculates exact byte sizes and lowercase SHA-256 values, builds the raw compact UTF-8 payload, and writes an atomic envelope with a DER-encoded `SHA256withECDSA` signature represented as unpadded base64url. It does not generate or retain a private key.

Individual hashes can also be checked with:

```bash
sha256sum '/path/to/lectures/Basic Algebraic Structures.pdf'
wc -c < '/path/to/lectures/Basic Algebraic Structures.pdf'
```

## Domain and Range verification

Allow only `GET` and `HEAD` on the download hostname. A Cloudflare WAF/custom rule can block other methods. Rate limiting must permit sequential downloads and repeated `Range` requests; do not challenge Android API traffic or strip `Range`, `If-Range`, `ETag`, `Content-Range`, `Content-Type`, or `Content-Length`.

Verify the custom domain against a real immutable object:

```bash
curl --fail --silent --show-error --head 'https://download.icymath.com/lectures/Basic%20Algebraic%20Structures.pdf'
curl --fail --silent --show-error --output /tmp/lecture-01.pdf 'https://download.icymath.com/lectures/Basic%20Algebraic%20Structures.pdf'
curl --fail --silent --show-error --dump-header /tmp/range.headers \
  --header 'Accept-Encoding: identity' \
  --header 'Range: bytes=0-1023' \
  'https://download.icymath.com/lectures/Basic%20Algebraic%20Structures.pdf' \
  --output /tmp/lecture-01.part
etag=$(awk 'BEGIN{IGNORECASE=1} /^etag:/{sub(/\r$/, ""); print $2}' /tmp/range.headers)
curl --fail --silent --show-error --dump-header - \
  --header 'Accept-Encoding: identity' \
  --header 'Range: bytes=1024-' \
  --header "If-Range: $etag" \
  'https://download.icymath.com/lectures/Basic%20Algebraic%20Structures.pdf' \
  --output /tmp/lecture-01.rest
```

Confirm `HEAD` is `200`; a partial `GET` is `206`; `Content-Range` begins at the requested byte; total length matches the signed manifest; `ETag` is a quoted strong validator (not `W/`); and `Content-Encoding` is absent. Also confirm the manifest itself does not redirect.

The app stores final files below `filesDir/icy_content/` and excludes that tree and its DataStore metadata from Auto Backup/device transfer. Clearing Android cache does not remove it. Clearing app data or uninstalling the app does.
