# Cloudflare R2 content setup

The Android client accepts content only from `https://download.cryonum.com/` and only after verifying `manifests/content-v1.signed.json`. It never lists the bucket and does not use `releases/`.

## Object keys

Use these versioned lowercase ASCII object keys:

```text
lectures/v1/lecture-01.pdf
lectures/v1/lecture-02.pdf
...
lectures/v1/lecture-12.pdf
privacy-policy/v4.0/privacy-policy.pdf
manifests/content-v1.signed.json
```

`lecture-01` through `lecture-12` are both stable manifest IDs and stable file names. Human-readable Russian and English lecture names live only in each manifest entry's `displayName`. R2 object keys are case-sensitive.

For a future policy such as 5.0, upload `privacy-policy/v5.0/privacy-policy.pdf`; never replace the bytes at the 4.0 key. For a future lecture revision, upload `lectures/v2/lecture-01.pdf` through `lecture-12.pdf`, increment the lecture `contentVersion` and manifest `revision`, then publish the new manifest last.

The old keys are migration sources only. Copy their bytes to the new keys, verify size and SHA-256, and keep the old objects until the website, signed manifest, and released Android client have all been verified:

```text
lectures/Basic Algebraic Structures.pdf -> lectures/v1/lecture-01.pdf
lectures/Divisibility in the Ring of Integers.pdf -> lectures/v1/lecture-02.pdf
lectures/GCD and LCM. Coprime Integers.pdf -> lectures/v1/lecture-03.pdf
lectures/Prime Numbers.pdf -> lectures/v1/lecture-04.pdf
lectures/Numerical Congruences.pdf -> lectures/v1/lecture-05.pdf
lectures/Solving Congruences.pdf -> lectures/v1/lecture-06.pdf
lectures/Complex Numbers. Part 1.pdf -> lectures/v1/lecture-07.pdf
lectures/Complex Numbers. Part 2.pdf -> lectures/v1/lecture-08.pdf
lectures/Systems of Linear Equations. Gauss Method.pdf -> lectures/v1/lecture-09.pdf
lectures/Matrices.pdf -> lectures/v1/lecture-10.pdf
lectures/Determinants.pdf -> lectures/v1/lecture-11.pdf
lectures/Permutations.pdf -> lectures/v1/lecture-12.pdf
privacy-policy/privacy_policy.4.0.pdf -> privacy-policy/v4.0/privacy-policy.pdf
```

The repository includes `tools/migrate_r2_layout.py` for this one-time copy. It never deletes objects, never lists the bucket, refuses to touch `releases/` or `manifests/`, and refuses to overwrite a target whose bytes or metadata differ. Create a temporary R2 API token restricted to **Object Read & Write** for the `icymath-download` bucket, keep its values out of chat, source control, and shell history, and revoke it after the migration.

Install the official AWS SDK for Python and put the credentials into the process environment without typing the secret directly into a recorded command:

```bash
python3 -m pip install boto3
read -r -p 'R2 account ID: ' R2_ACCOUNT_ID; export R2_ACCOUNT_ID
read -r -p 'R2 access key ID: ' R2_ACCESS_KEY_ID; export R2_ACCESS_KEY_ID
read -r -s -p 'R2 secret access key: ' R2_SECRET_ACCESS_KEY; export R2_SECRET_ACCESS_KEY; echo
export R2_BUCKET='icymath-download'
```

First run the read-only preflight. Only if all 13 source keys are found and no conflicting target exists, apply the copies:

```bash
python3 tools/migrate_r2_layout.py
python3 tools/migrate_r2_layout.py --apply
```

The apply run performs server-side `CopyObject`, replaces object metadata with the approved values, streams and compares the real source/target SHA-256 values, checks `%PDF-`, confirms each source still exists, and verifies public `HEAD` and `Range` responses through `download.cryonum.com`. It prints the verified hashes for use when building the signed manifest. It does not create or publish the manifest.

When finished, remove the variables from the shell and revoke the temporary token in Cloudflare:

```bash
unset R2_ACCOUNT_ID R2_ACCESS_KEY_ID R2_SECRET_ACCESS_KEY R2_BUCKET
```

The website publishes `/data/privacy-policy.json`. Its `versionCode`, `versionName`, `contentManifestRevision`, and `objectPath` must describe the same policy entry as the signed content manifest. `versionCode` is the monotonic integer used by Android acceptance logic; `versionName` is the user-facing value used in the R2 file name. Publish the signed manifest before changing the website config so the app never sees a config that refers to a not-yet-trusted manifest.

PDF metadata:

```text
Content-Type: application/pdf
Content-Encoding: (absent)
Cache-Control: public, max-age=31536000, immutable
```

Give the signed manifest a short cache lifetime or require revalidation, for example `Cache-Control: public, max-age=300, must-revalidate`. Upload every new PDF first and the signed manifest last. This prevents a valid manifest from referencing objects that are not yet available.

The website PDF.js viewer reads the policy from the R2 custom domain, so configure bucket CORS for the exact production site origins. Do not use `*` if the set of site origins is known. A suitable starting point is:

```json
[
  {
    "AllowedOrigins": [
      "https://icymath.com",
      "https://www.icymath.com",
      "https://cryonum.com",
      "https://www.cryonum.com"
    ],
    "AllowedMethods": ["GET", "HEAD"],
    "AllowedHeaders": ["Range", "If-Range"],
    "ExposeHeaders": ["Accept-Ranges", "Content-Length", "Content-Range", "Content-Type", "ETag"],
    "MaxAgeSeconds": 3600
  }
]
```

Apply this as the bucket CORS policy in the Cloudflare dashboard or with Wrangler. CORS does not replace access control, signed-manifest verification, or object hashes. The `icymath.com` origins are retained only during the redirect migration and can be removed after the legacy domain stops serving the viewer.

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
  --privacy-pdf /path/to/privacy-policy.4.0.pdf \
  --revision 1 \
  --lectures-version 1 \
  --privacy-version-code 4 \
  --privacy-version-name 4.0 \
  --privacy-object-path privacy-policy/v4.0/privacy-policy.pdf \
  --key-id content-2026-01 \
  --private-key /secure/offline/content-2026-01-private.pem \
  --output /tmp/content-v1.signed.json
```

The helper checks that the directory contains exactly those 12 case-sensitive English PDF names, verifies `%PDF-`, validates the exact policy object key, calculates exact byte sizes and lowercase SHA-256 values, builds the raw compact UTF-8 payload, and writes an atomic envelope with a DER-encoded `SHA256withECDSA` signature represented as unpadded base64url. It does not generate or retain a private key.

Individual hashes can also be checked with:

```bash
sha256sum '/path/to/lectures/Basic Algebraic Structures.pdf'
wc -c < '/path/to/lectures/Basic Algebraic Structures.pdf'
```

## Domain and Range verification

Allow only `GET` and `HEAD` on the download hostname. A Cloudflare WAF/custom rule can block other methods. Rate limiting must permit sequential downloads and repeated `Range` requests; do not challenge Android API traffic or strip `Range`, `If-Range`, `ETag`, `Content-Range`, `Content-Type`, or `Content-Length`.

Verify the custom domain against a real immutable object:

```bash
curl --fail --silent --show-error --head 'https://download.cryonum.com/lectures/v1/lecture-01.pdf'
curl --fail --silent --show-error --head 'https://download.cryonum.com/privacy-policy/v4.0/privacy-policy.pdf'
curl --fail --silent --show-error --output /tmp/lecture-01.pdf 'https://download.cryonum.com/lectures/v1/lecture-01.pdf'
curl --fail --silent --show-error --dump-header /tmp/range.headers \
  --header 'Accept-Encoding: identity' \
  --header 'Range: bytes=0-1023' \
  'https://download.cryonum.com/lectures/v1/lecture-01.pdf' \
  --output /tmp/lecture-01.part
etag=$(awk 'BEGIN{IGNORECASE=1} /^etag:/{sub(/\r$/, ""); print $2}' /tmp/range.headers)
curl --fail --silent --show-error --dump-header - \
  --header 'Accept-Encoding: identity' \
  --header 'Range: bytes=1024-' \
  --header "If-Range: $etag" \
  'https://download.cryonum.com/lectures/v1/lecture-01.pdf' \
  --output /tmp/lecture-01.rest
```

Confirm `HEAD` is `200`; a partial `GET` is `206`; `Content-Range` begins at the requested byte; total length matches the signed manifest; `ETag` is a quoted strong validator (not `W/`); and `Content-Encoding` is absent. Also confirm the manifest itself does not redirect.

The app stores final files below `filesDir/icy_content/` and excludes that tree and its DataStore metadata from Auto Backup/device transfer. Clearing Android cache does not remove it. Clearing app data or uninstalling the app does.
