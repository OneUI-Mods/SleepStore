# SleepStore

[日本語](README.ja.md) · English

A Samsung One UI Xposed module that makes deep-sleeping apps visible to Google Play Store and Galaxy Store without removing them from deep sleep.

## How it works

Hooks are confined to the store processes. PackageManager reads include disabled-until-used packages. Only packages with **both** enabled state `DISABLED_UNTIL_USED` (4) **and** Samsung `isPackageAutoDisabled(packageName, userId) == true` are presented as enabled in store responses. Ordinary disabled states (2/3), until-used packages without Samsung confirmation, and failed lookups remain unchanged.

Responses are copied before adjustment, including cached ApplicationInfo and PackageInfo objects. The module never calls enable/disable setters, launches sleeping apps, changes MARs, removes entries from Device Care, or modifies store APKs. Android System and Device Care must not be selected as scope.

Stores still control update availability, compatibility, rollout and installation. Visibility to the store does not guarantee that the server offers an update. Installation may itself affect Samsung's app state; validation should check the state after installing updates.

## Requirements and setup

- Samsung Android 16 (API 36) with the Samsung auto-disable API.
- An active Xposed-compatible framework, tested with Vector 2.2 (3080).
- Install the APK, enable SleepStore, and select **Google Play Store** (`com.android.vending`) and **Galaxy Store** (`com.sec.android.app.samsungapps`).
- Selecting SleepStore itself enables its framework connection indicator. This indicator does not prove store scopes are configured.
- Force-stop and reopen both stores, then check for updates. No Runner launch is required.
- Configure each Android profile separately. Initial device setup applies only to personal profile user 0.

The Material 3 app follows system light/dark theme and dynamic color. Language: system, Japanese, English. Full license texts are available in the app.

## Build

JDK 21, Android SDK 36, Gradle wrapper:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

GitHub Actions performs the same checks and saves the debug APK. Debug builds are signed with the build machine's debug key; CI artifacts from another machine cannot replace a locally signed installation without uninstalling. A stable release signing setup is required before distributing upgradeable releases.

## Validation

Each store's startup runs read-only PackageManager checks and writes `files/sleepstore-diagnostics.json` in that store's private data directory. Framework logs tagged `SleepStore: CHECK` report deep-sleeping visibility, preserved disabled packages, and failures. No credentials or store account databases are read. Production behavior is scoped by package name and Android version; unknown Samsung API results fail closed.

Verified on Samsung Android 16 / One UI 8.5 with Vector 2.2: both stores exposed 216 auto-disabled apps, preserved ordinary disabled states, and left OS deep sleep restrictions unchanged. End-to-end update discovery remains under verification. Detailed device evidence and internal notes are kept outside this public repository.

## Disable / rollback

Disable SleepStore in Vector / LSPosed and restart the two stores. Or uninstall `dev.ryuya.sleepstore` and restart them. Deep sleep configuration needs no restoration because SleepStore does not write it.

## License

MIT. [Third party notices](THIRD_PARTY_NOTICES.md). Device evidence, firmware and store APKs are excluded from Git.

Signed local releases use `signing.properties` and a private PKCS12 keystore, both excluded from Git. Back up both to preserve future upgrade compatibility; use `signing.properties.example` as the format.
