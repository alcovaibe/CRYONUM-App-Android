#!/usr/bin/env python3
"""Build and sign Icy Math's content manifest. This script never creates a private key."""

import argparse
import base64
import hashlib
import json
import subprocess
import sys
import tempfile
import re
from datetime import datetime, timezone
from pathlib import Path


LECTURES = (
    ("lecture-01", "Основные алгебраические структуры", "Basic Algebraic Structures", "Basic Algebraic Structures.pdf"),
    ("lecture-02", "Делимость в кольце целых чисел нацело и с остатком", "Divisibility in the Ring of Integers", "Divisibility in the Ring of Integers.pdf"),
    ("lecture-03", "НОД и НОК целых чисел. Взаимно простые числа", "GCD and LCM. Coprime Integers", "GCD and LCM. Coprime Integers.pdf"),
    ("lecture-04", "Простые числа", "Prime Numbers", "Prime Numbers.pdf"),
    ("lecture-05", "Числовые сравнения", "Numerical Congruences", "Numerical Congruences.pdf"),
    ("lecture-06", "Решение сравнений", "Solving Congruences", "Solving Congruences.pdf"),
    ("lecture-07", "Комплексные числа. Часть 1", "Complex Numbers. Part 1", "Complex Numbers. Part 1.pdf"),
    ("lecture-08", "Комплексные числа. Часть 2", "Complex Numbers. Part 2", "Complex Numbers. Part 2.pdf"),
    ("lecture-09", "СЛУ. Метод Гаусса", "Systems of Linear Equations. Gauss Method", "Systems of Linear Equations. Gauss Method.pdf"),
    ("lecture-10", "Матрицы", "Matrices", "Matrices.pdf"),
    ("lecture-11", "Определители", "Determinants", "Determinants.pdf"),
    ("lecture-12", "Подстановки", "Permutations", "Permutations.pdf"),
)


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
    parser.add_argument("--lectures-version", required=True)
    parser.add_argument("--privacy-version-code", required=True)
    parser.add_argument("--privacy-version-name", required=True)
    parser.add_argument("--privacy-object-path", required=True)
    parser.add_argument("--key-id", required=True)
    parser.add_argument("--private-key", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    args = parser.parse_args()

    if (
        args.revision <= 0
        or not args.lectures_version.isdigit()
        or int(args.lectures_version) <= 0
        or not args.privacy_version_code.isdigit()
        or int(args.privacy_version_code) <= 0
    ):
        raise ValueError("revision and content version codes must be positive integers")
    if not re.fullmatch(r"[1-9][0-9]{0,8}(?:\.[0-9]{1,3}){1,2}", args.privacy_version_name):
        raise ValueError("privacy-version-name must look like 4.0 or 4.0.1")
    expected_policy_path = f"privacy-policy/privacy_policy.{args.privacy_version_name}.pdf"
    if args.privacy_object_path != expected_policy_path:
        raise ValueError(f"privacy-object-path must be exactly {expected_policy_path}")
    if not args.private_key.is_file():
        raise ValueError("private key path does not exist")

    if not args.lectures_dir.is_dir():
        raise ValueError("lectures directory does not exist")
    expected_names = {entry[3] for entry in LECTURES}
    actual_names = {path.name for path in args.lectures_dir.iterdir() if path.is_file() and path.suffix.lower() == ".pdf"}
    if actual_names != expected_names:
        missing = sorted(expected_names - actual_names)
        unexpected = sorted(actual_names - expected_names)
        raise ValueError(f"lecture file names do not match; missing={missing}, unexpected={unexpected}")

    files = []
    for order, (file_id, ru_name, en_name, object_name) in enumerate(LECTURES, start=1):
        source = args.lectures_dir / object_name
        size, sha256 = inspect_pdf(source)
        files.append({
            "id": file_id,
            "category": "lecture",
            "order": order,
            "displayName": {"ru": ru_name, "en": en_name},
            "path": f"lectures/{object_name}",
            "contentVersion": args.lectures_version,
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
        "path": args.privacy_object_path,
        "contentVersion": args.privacy_version_code,
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
