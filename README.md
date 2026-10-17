# SleepStore

[日本語](README.ja.md) · English

A Samsung One UI Xposed module that makes deep-sleeping apps visible inside the apps selected in Vector / LSPosed, without removing them from deep sleep.

## How it works

The framework scope controls where the shared PackageManager hooks run. No per-store patches or internal store methods are used. Android System, System UI and Samsung Device Care are always excluded. PackageManager reads include disabled-until-used packages. Only packages with **both** enabled state `DISABLED_UNTIL_USED` (4) **and** Samsung `isPackageAutoDisabled(packageName, userId) == true` are presented as enabled in store responses. Ordinary disabled states (2/3), until-used packages without Samsung confirmation, and failed lookups remain unchanged.

Responses are copied before adjustment, including cached ApplicationInfo and PackageInfo objects. The module never calls enable/disable setters, launches sleeping apps, changes MARs, removes entries from Device Care, or modifies store APKs. It intercepts selected apps’ attempts to set confirmed deep-sleeping packages to DEFAULT or ENABLED and suppresses those requests, preserving the OS state after updates.

Stores still control update availability, compatibility, rollout and installation. Visibility to the store does not guarantee that the server offers an update. The enable guard covers calls made inside selected processes. An external root/Shizuku command, another unselected app or the OS itself runs outside that scope.

## Requirements and setup

- Samsung Android 16 (API 36) with the Samsung auto-disable API.
- An active modern Xposed API 102 framework, tested with Vector 2.2 (3080).
- Install the APK, enable SleepStore, and select the apps you want to use. Recommended: **Google Play Store** (`com.android.vending`), **Galaxy Store** (`com.sec.android.app.samsungapps`), **Aurora Store** (`com.aurora.store`), and **Obtainium** (`dev.imranr.obtainium` or `dev.imranr.obtainium.fdroid`).
- Selecting SleepStore itself enables its framework connection indicator. This indicator does not prove store scopes are configured.
- Force-stop and reopen the selected apps, then check for updates. No Runner launch is required.
- Configure each Android profile separately. Initial device setup applies only to personal profile user 0.

The Material 3 app follows system light/dark theme and dynamic color. Language: system, Japanese, English. Full license texts are available in the app.

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

## Disable / rollback

Disable SleepStore in Vector / LSPosed and restart the selected apps. Or uninstall `dev.ryuya.sleepstore` and restart the selected apps. Deep sleep configuration needs no restoration because SleepStore does not write it.

## License

MIT. [Third party notices](THIRD_PARTY_NOTICES.md). Device evidence, firmware and store APKs are excluded from Git.

Signed local releases use `signing.properties` and a private PKCS12 keystore, both excluded from Git. Back up both to preserve future upgrade compatibility; use `signing.properties.example` as the format.

## Recommended scope catalog

The catalog contains Play Store, Galaxy Store, Aurora Store, Obtainium, F-Droid / F-Droid Basic, Droid-ify, Neo Store, Aptoide, APKPure, Uptodown, HUAWEI AppGallery, Xiaomi GetApps, TapTap, RuStore, Amazon Appstore and APKMirror Installer. These entries are selection suggestions, not individual compatibility certifications. Store availability depends on the device and region. Any other app selected in the framework receives the same hook, except the excluded system policy owners.

The UI lists installed catalog apps. `config/stores.json` defines 16 entries / 19 package IDs; run `python3 tools/sync_stores.py` after changing the catalog. Do not enable unnecessary scopes.
