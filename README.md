# SleepStore

English · [日本語](README.ja.md)

Keep apps in Samsung's **Deep sleeping apps** list and still check for their updates in Google Play Store or Galaxy Store. SleepStore makes those apps visible to the stores you select in Vector while preserving Samsung's background restrictions and the state of manually disabled apps.

[Download the signed APK](https://github.com/OneUI-Mods/SleepStore/releases/latest) · [Report a problem](https://github.com/OneUI-Mods/SleepStore/issues/new)

## Compatibility

| Requirement | Details |
| --- | --- |
| Device | Samsung Galaxy on Android 16 or 17 with Samsung's auto-disable API |
| Framework | Xposed API 102, such as Vector |
| Verified stores | Google Play Store and Galaxy Store on a Galaxy S26 Ultra |
| Other stores | You can also select stores and updaters such as Aurora Store or Obtainium. |

See the [device validation results](docs/validation.md) for the firmware, store versions, and checks performed.

## Set up SleepStore

1. Install the signed APK from [GitHub Releases](https://github.com/OneUI-Mods/SleepStore/releases/latest).
2. In Vector, enable **SleepStore** and select each store you use in the module's scope. Configure the scope for each Android profile you use.
3. Force-stop and reopen the selected stores once, then check for updates as usual.

Select **SleepStore** itself in the scope if you want to see the connection status in its settings. That status reports whether Vector loaded SleepStore into the settings app. To check a store, confirm that the store is selected in the scope for the same Android profile.

## Update or turn off SleepStore

- **Updating from 0.2.4 or earlier on Android 17.** Install over your existing copy, then force-stop and reopen the selected stores once.
- **Hot updates.** Vector applied the update from 0.2.5 to 0.2.6 while Google Play Store and Galaxy Store stayed running. If a store keeps using the older version, reopen it once.
- **Turning it off.** Disable SleepStore in Vector, then force-stop and reopen the affected stores.

These steps work without restarting the phone. Each store still decides which updates to offer and install based on its compatibility rules and rollout schedule. Android system, SystemUI, and Device Care retain their stock behavior.

## Development and license

[Developer guide](docs/development.md) · [Build environment](docs/build-environment.md) · [Changelog](CHANGELOG.md)

[MIT License](LICENSE) · [Third-party notices](THIRD_PARTY_NOTICES.md) · [OneUI Mods](https://github.com/OneUI-Mods)
