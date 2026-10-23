# 実装・検証の補足

READMEから分離した開発用の説明です。過去の測定値は対応する当時の環境の記録であり、現在の動作保証値ではありません。対応環境はREADMEと互換性・リリース文を確認してください。

## How it works

The framework scope controls where the shared PackageManager hooks run. No per-store patches or internal store methods are used. Android System, System UI and Samsung Device Care are always excluded. PackageManager reads include disabled-until-used packages. Only packages with **both** enabled state `DISABLED_UNTIL_USED` (4) **and** Samsung `isPackageAutoDisabled(packageName, userId) == true` are presented as enabled in store responses. Ordinary disabled states (2/3), until-used packages without Samsung confirmation, and failed lookups remain unchanged.

Responses are copied before adjustment, including cached ApplicationInfo and PackageInfo objects. The module never calls enable/disable setters, launches sleeping apps, changes MARs, removes entries from Device Care, or modifies store APKs. It intercepts selected apps’ attempts to set confirmed deep-sleeping packages to DEFAULT or ENABLED and suppresses those requests, preserving the OS state after updates.

Stores still control update availability, compatibility, rollout and installation. Visibility to the store does not guarantee that the server offers an update. The enable guard covers calls made inside selected processes. An external root/Shizuku command, another unselected app or the OS itself runs outside that scope.

## Build

JDK 21, Android SDK 36, Gradle wrapper:

```sh
./gradlew assembleRelease testReleaseUnitTest lintRelease
python3 tools/verify_modern_xposed.py
```

GitHub Actions performs the same checks. The module uses libxposed API 102's interceptor chain and modern `META-INF/xposed` metadata; no legacy API 82 classes or `assets/xposed_init` entry are packaged. Official Releases contain a release APK signed with a dedicated, persistent key. CI builds validate an unsigned release and publish reports; use Releases for installation.

## Validation

Each selected app's startup runs read-only PackageManager checks and writes `files/sleepstore-diagnostics.json` in that app's private data directory. Framework logs tagged `SleepStore: CHECK` report deep-sleeping visibility, preserved disabled packages, and failures. No credentials or store account databases are read. Production behavior is scoped by the framework selection and Android version; unknown Samsung API results fail closed.

On-device checks cover Play Store, Galaxy Store, Aurora Store and Obtainium (F-Droid build), ordinary disabled app preservation, and deep sleep visibility. Play Store automatically updated deep-sleeping apps during validation. DEFAULT and ENABLED requests from its main and background processes were then verified to be blocked while the raw OS state stayed disabled-until-used. Galaxy Store / Aurora / Obtainium server update offers and full installation cycles have not all been verified. The public firmware and API 102 compatibility baseline is recorded in [docs/validation.md](docs/validation.md); private device evidence and internal notes are excluded.
