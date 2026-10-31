"""Generate recommendation metadata without changing the framework-selected hook scope."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    entries = json.loads((ROOT / "config/stores.json").read_text())
    packages = list(dict.fromkeys(package for entry in entries for package in entry["packages"]))
    manifest = ROOT / "app/src/main/AndroidManifest.xml"
    source = manifest.read_text()
    start = source.index("    <queries>")
    end = source.index("</queries>", start) + len("</queries>")
    queries = "    <queries>\n" + "".join(
        f'        <package android:name="{package}"/>\n' for package in packages
    ) + "    </queries>"
    manifest.write_text(source[:start] + queries + source[end:])
    (ROOT / "app/src/main/resources/META-INF/xposed/scope.list").write_text(
        "\n".join(packages + ["dev.ryuya.sleepstore"]) + "\n"
    )
    arrays = ",\n".join(
        "        arrayOf(" + ", ".join(json.dumps(p) for p in entry["packages"]) + ")"
        for entry in entries
    )
    labels = ",\n".join("        R.string." + entry["label"] for entry in entries)
    template = (ROOT / "tools/targets.kt.template").read_text()
    (ROOT / "app/src/main/java/dev/ryuya/sleepstore/Targets.kt").write_text(
        template.replace("@PACKAGES@", arrays).replace("@LABELS@", labels)
    )


if __name__ == "__main__":
    main()
