#!/usr/bin/env python3
"""Build and sign Icy Math's content manifest. This script never creates a private key."""

import argparse
import base64
import hashlib
import json
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path


def b64url(data: bytes) -> str:
    return base64.urlsafe_b64encode(data).rstrip(b"=").decode("ascii")


def inspect_pdf(path: Path) -> tuple[int, str]:
    if not path.is_file():
        raise ValueError(f"missing PDF: {path}")
    digest = hashlib.sha256()
    size = 0
    with path.open("rb") as stream:
        if stream.read(5) != b"%PDF-":
            raise ValueError(f"not a PDF: {path}")
        stream.seek(0)
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            size += len(chunk)
            digest.update(chunk)
    if size <= 0:
        raise ValueError(f"empty PDF: {path}")
    return size, digest.hexdigest()


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--lectures-dir", required=True, type=Path)
    parser.add_argument("--privacy-pdf", required=True, type=Path)
    parser.add_argument("--revision", required=True, type=int)
    parser.add_argument("--content-version", required=True)
    parser.add_argument("--key-id", required=True)
    parser.add_argument("--private-key", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    if args.revision <= 0 or not args.content_version.isdigit() or int(args.content_version) <= 0:
        raise ValueError("revision and content-version must be positive integers")
    if not args.private_key.is_file():
        raise ValueError("private key path does not exist")

    files = []
    for order in range(1, 13):
        file_id = f"lecture-{order:02d}"
        source = args.lectures_dir / f"{file_id}.pdf"
        size, sha256 = inspect_pdf(source)
        files.append({
            "id": file_id,
            "category": "lecture",
            "order": order,
            "displayName": {"ru": f"Лекция {order}", "en": f"Lecture {order}"},
            "path": f"lectures/v{args.content_version}/{file_id}.pdf",
            "contentVersion": args.content_version,
            "sizeBytes": size,
            "sha256": sha256,
            "contentType": "application/pdf",
        })

    size, sha256 = inspect_pdf(args.privacy_pdf)
    files.append({
        "id": "privacy-policy",
        "category": "privacy_policy",
        "order": 1,
        "displayName": {"ru": "Политика конфиденциальности", "en": "Privacy Policy"},
        "path": f"privacy-policy/v{args.content_version}/privacy-policy.pdf",
        "contentVersion": args.content_version,
        "sizeBytes": size,
        "sha256": sha256,
        "contentType": "application/pdf",
    })

    payload = json.dumps({
        "revision": args.revision,
        "generatedAt": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
        "files": files,
    }, ensure_ascii=False, separators=(",", ":")).encode("utf-8")

    with tempfile.TemporaryDirectory(prefix="icy-manifest-") as temp_dir:
        payload_path = Path(temp_dir) / "payload.json"
        signature_path = Path(temp_dir) / "signature.der"
        payload_path.write_bytes(payload)
        subprocess.run([
            "openssl", "dgst", "-sha256", "-sign", str(args.private_key),
            "-out", str(signature_path), str(payload_path)
        ], check=True)
        signature = signature_path.read_bytes()

    envelope = {
        "schemaVersion": 1,
        "keyId": args.key_id,
        "payload": b64url(payload),
        "signature": b64url(signature),
    }
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(envelope, separators=(",", ":")) + "\n", encoding="utf-8")
    print(f"wrote {args.output} ({len(files)} files, revision {args.revision})")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (ValueError, subprocess.CalledProcessError) as error:
        print(f"error: {error}", file=sys.stderr)
        raise SystemExit(2)
