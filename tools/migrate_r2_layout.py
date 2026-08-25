#!/usr/bin/env python3
"""Safely copy CRYONUM PDFs to the versioned R2 object-key layout.

The script never deletes objects, never lists the bucket, and never touches
``releases/`` or ``manifests/``. Credentials are read only from environment
variables. Run without ``--apply`` first to perform a read-only preflight.
"""

from __future__ import annotations

import argparse
import hashlib
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from typing import Any, Iterable


BUCKET_DEFAULT = "icymath-download"
PUBLIC_BASE_URL = "https://download.cryonum.com/"
PDF_CONTENT_TYPE = "application/pdf"
IMMUTABLE_CACHE_CONTROL = "public, max-age=31536000, immutable"
CHUNK_SIZE = 1024 * 1024
PROTECTED_PREFIXES = ("manifests/", "releases/")


@dataclass(frozen=True)
class ObjectMapping:
    source: str
    target: str


MAPPINGS = (
    ObjectMapping("lectures/Basic Algebraic Structures.pdf", "lectures/v1/lecture-01.pdf"),
    ObjectMapping("lectures/Divisibility in the Ring of Integers.pdf", "lectures/v1/lecture-02.pdf"),
    ObjectMapping("lectures/GCD and LCM. Coprime Integers.pdf", "lectures/v1/lecture-03.pdf"),
    ObjectMapping("lectures/Prime Numbers.pdf", "lectures/v1/lecture-04.pdf"),
    ObjectMapping("lectures/Numerical Congruences.pdf", "lectures/v1/lecture-05.pdf"),
    ObjectMapping("lectures/Solving Congruences.pdf", "lectures/v1/lecture-06.pdf"),
    ObjectMapping("lectures/Complex Numbers. Part 1.pdf", "lectures/v1/lecture-07.pdf"),
    ObjectMapping("lectures/Complex Numbers. Part 2.pdf", "lectures/v1/lecture-08.pdf"),
    ObjectMapping("lectures/Systems of Linear Equations. Gauss Method.pdf", "lectures/v1/lecture-09.pdf"),
    ObjectMapping("lectures/Matrices.pdf", "lectures/v1/lecture-10.pdf"),
    ObjectMapping("lectures/Determinants.pdf", "lectures/v1/lecture-11.pdf"),
    ObjectMapping("lectures/Permutations.pdf", "lectures/v1/lecture-12.pdf"),
    ObjectMapping("privacy-policy/privacy_policy.4.0.pdf", "privacy-policy/v4.0/privacy-policy.pdf"),
)


class MigrationError(RuntimeError):
    pass


class NoRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req: Any, fp: Any, code: int, msg: str, headers: Any, newurl: str) -> None:
        return None


def validate_mappings(mappings: Iterable[ObjectMapping]) -> tuple[ObjectMapping, ...]:
    mappings = tuple(mappings)
    expected_targets = {
        *(f"lectures/v1/lecture-{index:02d}.pdf" for index in range(1, 13)),
        "privacy-policy/v4.0/privacy-policy.pdf",
    }
    sources = {mapping.source for mapping in mappings}
    targets = {mapping.target for mapping in mappings}
    if len(mappings) != 13 or len(sources) != 13 or targets != expected_targets:
        raise MigrationError("the immutable migration map must contain exactly the 13 approved targets")
    for mapping in mappings:
        for key in (mapping.source, mapping.target):
            if key.startswith(PROTECTED_PREFIXES):
                raise MigrationError(f"protected object key in migration map: {key}")
            if key.startswith("/") or "\\" in key or ".." in key.split("/"):
                raise MigrationError(f"unsafe object key in migration map: {key}")
    return mappings


def require_environment() -> tuple[str, str, str, str]:
    names = ("R2_ACCOUNT_ID", "R2_ACCESS_KEY_ID", "R2_SECRET_ACCESS_KEY")
    missing = [name for name in names if not os.environ.get(name)]
    if missing:
        raise MigrationError(f"missing environment variables: {', '.join(missing)}")
    account_id = os.environ["R2_ACCOUNT_ID"]
    access_key = os.environ["R2_ACCESS_KEY_ID"]
    secret_key = os.environ["R2_SECRET_ACCESS_KEY"]
    bucket = os.environ.get("R2_BUCKET", BUCKET_DEFAULT)
    if not re.fullmatch(r"[a-fA-F0-9]{32}", account_id):
        raise MigrationError("R2_ACCOUNT_ID must be a 32-character hexadecimal account ID")
    if bucket != BUCKET_DEFAULT:
        raise MigrationError(f"R2_BUCKET must be exactly {BUCKET_DEFAULT}")
    return account_id, access_key, secret_key, bucket


def create_client(account_id: str, access_key: str, secret_key: str) -> Any:
    try:
        import boto3
        from botocore.config import Config
    except ImportError as error:
        raise MigrationError("boto3 is required; install it with: python3 -m pip install boto3") from error

    return boto3.client(
        "s3",
        endpoint_url=f"https://{account_id}.r2.cloudflarestorage.com",
        aws_access_key_id=access_key,
        aws_secret_access_key=secret_key,
        region_name="auto",
        config=Config(
            signature_version="s3v4",
            connect_timeout=10,
            read_timeout=60,
            retries={"max_attempts": 3, "mode": "standard"},
        ),
    )


def is_not_found(error: Exception) -> bool:
    response = getattr(error, "response", {})
    status = response.get("ResponseMetadata", {}).get("HTTPStatusCode")
    code = str(response.get("Error", {}).get("Code", ""))
    return status == 404 or code in {"404", "NoSuchKey", "NotFound"}


def head_object(client: Any, bucket: str, key: str, *, allow_missing: bool = False) -> dict[str, Any] | None:
    try:
        return client.head_object(Bucket=bucket, Key=key)
    except Exception as error:
        if allow_missing and is_not_found(error):
            return None
        raise MigrationError(f"HEAD failed for {key}: {type(error).__name__}") from error


def stream_sha256(client: Any, bucket: str, key: str) -> tuple[int, str, bytes]:
    try:
        response = client.get_object(Bucket=bucket, Key=key)
        body = response["Body"]
        digest = hashlib.sha256()
        size = 0
        prefix = bytearray()
        try:
            while True:
                chunk = body.read(CHUNK_SIZE)
                if not chunk:
                    break
                if len(prefix) < 5:
                    prefix.extend(chunk[: 5 - len(prefix)])
                digest.update(chunk)
                size += len(chunk)
        finally:
            body.close()
    except MigrationError:
        raise
    except Exception as error:
        raise MigrationError(f"GET/hash failed for {key}: {type(error).__name__}") from error
    return size, digest.hexdigest(), bytes(prefix)


def validate_target_metadata(head: dict[str, Any], key: str, expected_size: int) -> None:
    if head.get("ContentLength") != expected_size:
        raise MigrationError(f"wrong Content-Length for {key}")
    if head.get("ContentType") != PDF_CONTENT_TYPE:
        raise MigrationError(f"wrong Content-Type for {key}: {head.get('ContentType')!r}")
    if head.get("ContentEncoding"):
        raise MigrationError(f"Content-Encoding must be absent for {key}")
    if head.get("CacheControl") != IMMUTABLE_CACHE_CONTROL:
        raise MigrationError(f"wrong Cache-Control for {key}: {head.get('CacheControl')!r}")


def verify_same_bytes(client: Any, bucket: str, mapping: ObjectMapping) -> tuple[int, str]:
    source_size, source_hash, source_prefix = stream_sha256(client, bucket, mapping.source)
    target_size, target_hash, target_prefix = stream_sha256(client, bucket, mapping.target)
    if source_prefix != b"%PDF-" or target_prefix != b"%PDF-":
        raise MigrationError(f"PDF signature check failed for {mapping.source} or {mapping.target}")
    if source_size != target_size or source_hash != target_hash:
        raise MigrationError(f"source and target bytes differ: {mapping.source} -> {mapping.target}")
    return target_size, target_hash


def public_request(key: str, *, method: str, headers: dict[str, str] | None = None) -> Any:
    quoted = urllib.parse.quote(key, safe="/")
    request = urllib.request.Request(
        PUBLIC_BASE_URL + quoted,
        method=method,
        headers={"Accept-Encoding": "identity", **(headers or {})},
    )
    opener = urllib.request.build_opener(NoRedirectHandler())
    try:
        return opener.open(request, timeout=30)
    except urllib.error.HTTPError as error:
        raise MigrationError(f"public {method} returned HTTP {error.code} for {key}") from error
    except urllib.error.URLError as error:
        raise MigrationError(f"public {method} failed for {key}: {error.reason}") from error


def verify_public_object(key: str, expected_size: int) -> tuple[str, str]:
    with public_request(key, method="HEAD") as response:
        if response.status != 200:
            raise MigrationError(f"public HEAD must return 200 for {key}, got {response.status}")
        if response.headers.get_content_type() != PDF_CONTENT_TYPE:
            raise MigrationError(f"public Content-Type is not application/pdf for {key}")
        if response.headers.get("Content-Encoding"):
            raise MigrationError(f"public Content-Encoding must be absent for {key}")
        if int(response.headers.get("Content-Length", "-1")) != expected_size:
            raise MigrationError(f"public Content-Length is wrong for {key}")
        etag = response.headers.get("ETag", "")
        if not etag or etag.startswith("W/"):
            raise MigrationError(f"public ETag must be a strong validator for {key}")

    range_end = min(expected_size - 1, 1023)
    with public_request(key, method="GET", headers={"Range": f"bytes=0-{range_end}"}) as response:
        payload = response.read(range_end + 2)
        if response.status != 206:
            raise MigrationError(f"public Range must return 206 for {key}, got {response.status}")
        expected_range = f"bytes 0-{range_end}/{expected_size}"
        if response.headers.get("Content-Range") != expected_range:
            raise MigrationError(f"wrong public Content-Range for {key}")
        if len(payload) != range_end + 1 or not payload.startswith(b"%PDF-"):
            raise MigrationError(f"wrong public Range body for {key}")
    return etag, "206"


def copy_object(client: Any, bucket: str, mapping: ObjectMapping) -> None:
    try:
        client.copy_object(
            Bucket=bucket,
            CopySource={"Bucket": bucket, "Key": mapping.source},
            Key=mapping.target,
            MetadataDirective="REPLACE",
            ContentType=PDF_CONTENT_TYPE,
            CacheControl=IMMUTABLE_CACHE_CONTROL,
        )
    except Exception as error:
        raise MigrationError(f"COPY failed for {mapping.source} -> {mapping.target}: {type(error).__name__}") from error


def migrate(apply: bool) -> None:
    mappings = validate_mappings(MAPPINGS)
    account_id, access_key, secret_key, bucket = require_environment()
    client = create_client(account_id, access_key, secret_key)

    missing_targets: list[ObjectMapping] = []
    source_sizes: dict[str, int] = {}
    print("Preflight: checking the 13 exact source and target keys")
    for mapping in mappings:
        source_head = head_object(client, bucket, mapping.source)
        assert source_head is not None
        source_size = int(source_head.get("ContentLength", 0))
        if source_size < 5:
            raise MigrationError(f"source is too small to be a PDF: {mapping.source}")
        source_sizes[mapping.target] = source_size
        target_head = head_object(client, bucket, mapping.target, allow_missing=True)
        if target_head is None:
            missing_targets.append(mapping)
            print(f"  COPY NEEDED  {mapping.source} -> {mapping.target}")
        else:
            validate_target_metadata(target_head, mapping.target, source_size)
            verify_same_bytes(client, bucket, mapping)
            print(f"  ALREADY OK   {mapping.target}")

    if not apply:
        print(f"\nDry run complete: {len(missing_targets)} object(s) need copying.")
        print("Run again with --apply to perform the non-destructive copies.")
        return

    for mapping in missing_targets:
        print(f"Copying: {mapping.source} -> {mapping.target}")
        copy_object(client, bucket, mapping)

    rows: list[tuple[str, str, int, str, str, str]] = []
    for mapping in mappings:
        source_head = head_object(client, bucket, mapping.source)
        target_head = head_object(client, bucket, mapping.target)
        assert source_head is not None and target_head is not None
        expected_size = source_sizes[mapping.target]
        if source_head.get("ContentLength") != expected_size:
            raise MigrationError(f"source changed during migration: {mapping.source}")
        validate_target_metadata(target_head, mapping.target, expected_size)
        size, sha256 = verify_same_bytes(client, bucket, mapping)
        etag, range_status = verify_public_object(mapping.target, size)
        rows.append((mapping.source, mapping.target, size, sha256, etag, range_status))

    print("\nMigration verified. Original objects were not deleted.")
    print("| Source key | Target key | Bytes | SHA-256 | ETag | Range |")
    print("|---|---|---:|---|---|---:|")
    for source, target, size, sha256, etag, range_status in rows:
        print(f"| `{source}` | `{target}` | {size} | `{sha256}` | `{etag}` | {range_status} |")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--apply",
        action="store_true",
        help="copy missing approved objects; without this flag the script is read-only",
    )
    args = parser.parse_args()
    migrate(args.apply)
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except MigrationError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(2)
