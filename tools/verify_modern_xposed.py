#!/usr/bin/env python3
"""Fail the build if legacy Xposed registration or API classes return."""

from pathlib import Path
import sys
import zipfile

root = Path(__file__).resolve().parents[1]
expected = {
    "minApiVersion": "102",
    "targetApiVersion": "102",
    "staticScope": "false",
    "exceptionMode": "protective",
    "autoHotReload": "false",
}

legacy_files = [root / "app/libs/xposed-api-82.jar", root / "app/src/main/assets/xposed_init"]
for path in legacy_files:
    if path.exists():
        raise SystemExit(f"Legacy Xposed file remains: {path.relative_to(root)}")

sources = list((root / "app/src/main").rglob("*.kt"))
if not sources:
    raise SystemExit("Android Kotlin sources are missing")
for path in sources:
    text = path.read_text()
    for marker in ("de.robv.android.xposed", "IXposedHookLoadPackage", "XposedBridge"):
        if marker in text:
            raise SystemExit(f"Legacy Xposed reference {marker!r} in {path.relative_to(root)}")

metadata = root / "app/src/main/resources/META-INF/xposed"
entry = (metadata / "java_init.list").read_text().strip()
if entry != "dev.ryuya.sleepstore.StoreHook":
    raise SystemExit(f"Unexpected modern entry point: {entry!r}")
actual = {}
for line in (metadata / "module.prop").read_text().splitlines():
    if line.strip() and not line.lstrip().startswith("#"):
        key, value = line.split("=", 1)
        actual[key] = value
if actual != expected:
    raise SystemExit(f"Unexpected module.prop: {actual!r}")

apks = sorted((root / "app/build/outputs/apk/release").glob("*.apk"))
if not apks:
    raise SystemExit("Release APK has not been built")
with zipfile.ZipFile(apks[0]) as apk:
    names = set(apk.namelist())
    required = {
        "META-INF/xposed/java_init.list",
        "META-INF/xposed/module.prop",
        "META-INF/xposed/scope.list",
    }
    missing = required - names
    if missing:
        raise SystemExit(f"APK is missing modern metadata: {sorted(missing)}")
    if "assets/xposed_init" in names:
        raise SystemExit("APK contains legacy assets/xposed_init")
    if apk.read("META-INF/xposed/module.prop").decode().strip() != (metadata / "module.prop").read_text().strip():
        raise SystemExit("APK module.prop differs from source")

print(f"API 102 metadata and legacy-removal checks passed: {apks[0].relative_to(root)}")
