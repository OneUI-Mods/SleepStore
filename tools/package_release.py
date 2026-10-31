#!/usr/bin/env python3
"""Package a signed release and its exact Git source without exposing signing settings."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
CERTIFICATE = "9d1f5a2cea5340c434682221e355cbfc92f8a97cdf6994f782ff6d96660a90b6"
PACKAGE = "dev.ryuya.sleepstore"


def run(*args, capture=False):
    return subprocess.run(
        [str(arg) for arg in args], cwd=ROOT, check=True, text=True, capture_output=capture
    )


def validate(apk, tools):
    badging = run(tools / "aapt2", "dump", "badging", apk, capture=True).stdout
    if f"name='{PACKAGE}'" not in badging or "application-debuggable" in badging:
        raise RuntimeError("Unexpected package or debuggable APK")
    manifest = run(
        tools / "aapt2", "dump", "xmltree", apk, "--file", "AndroidManifest.xml", capture=True
    ).stdout
    if any(word in manifest.lower() for word in ("testactivity", "probeactivity", "fixtureactivity", "instrumentation")):
        raise RuntimeError("Test component in release")
    if "io.github.libxposed.service.XposedProvider" not in manifest or f"{PACKAGE}.XposedService" not in manifest:
        raise RuntimeError("Missing official API 102 app-side Binder provider")
    with zipfile.ZipFile(apk) as archive:
        metadata = archive.read("META-INF/xposed/module.prop").decode()
        if "minApiVersion=102" not in metadata or "targetApiVersion=102" not in metadata:
            raise RuntimeError("API 102 metadata missing")
        if "assets/xposed_init" in archive.namelist():
            raise RuntimeError("Legacy module entry in release")
    run(tools / "zipalign", "-c", "-P", "16", "4", apk)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--source-ref", default="HEAD")
    parser.add_argument("--check-release-apk", type=Path)
    args = parser.parse_args()
    sdk = Path(os.environ.get("ANDROID_HOME", Path.home() / "Library/Android/sdk"))
    tools = sdk / "build-tools/37.0.0"
    if args.check_release_apk:
        validate(args.check_release_apk.resolve(), tools)
        return
    if run("git", "status", "--porcelain", capture=True).stdout.strip():
        raise RuntimeError("Commit release sources first")
    revision = run("git", "rev-parse", args.source_ref, capture=True).stdout.strip()
    if revision != run("git", "rev-parse", "HEAD", capture=True).stdout.strip():
        raise RuntimeError("Source revision must match checkout")
    if not (ROOT / "signing.properties").is_file():
        raise RuntimeError("Provide the existing private release signing configuration")
    run(ROOT / "gradlew", ":app:assembleRelease", ":app:testDebugUnitTest", ":app:lintRelease")
    run("python3", ROOT / "tools/verify_modern_xposed.py")
    output = ROOT / "app/build/outputs/apk/release"
    metadata = json.loads((output / "output-metadata.json").read_text())
    if metadata["variantName"] != "release" or len(metadata["elements"]) != 1:
        raise RuntimeError("Expected one release variant")
    item = metadata["elements"][0]
    apk = output / item["outputFile"]
    validate(apk, tools)
    signature = run(tools / "apksigner", "verify", "--verbose", "--print-certs", apk, capture=True).stdout
    if CERTIFICATE not in signature:
        raise RuntimeError("Release certificate differs from existing installation")
    destination = ROOT / "artifacts/release" / item["versionName"]
    destination.mkdir(parents=True, exist_ok=True)
    stem = "SleepStore-" + item["versionName"]
    target = destination / (stem + "-release.apk")
    shutil.copy2(apk, target)
    source = destination / (stem + "-source.zip")
    run("git", "archive", "--format=zip", "--prefix=" + stem + "/", "--output=" + str(source), revision)
    (destination / "SHA256SUMS.txt").write_text("".join(
        hashlib.sha256(path.read_bytes()).hexdigest() + "  " + path.name + "\n"
        for path in (target, source)
    ))
    (destination / "signature.txt").write_text(signature)
    print("Signed and verified release:", target)


if __name__ == "__main__":
    main()
