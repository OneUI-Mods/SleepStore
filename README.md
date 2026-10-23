# SleepStore

[日本語](README.ja.md) · English

SleepStore makes Samsung deep-sleeping apps visible to selected app stores without removing them from deep sleep.

## Setup

1. Install the signed release APK from [Releases](https://github.com/OneUI-Mods/SleepStore/releases).
2. Enable SleepStore in an Xposed API 102 framework such as Vector.
3. Select the stores you use, such as Google Play Store, Galaxy Store, Aurora Store or Obtainium.
4. Force-stop and reopen those stores, then check for updates.

The verified baseline is Samsung Android 16 with its auto-disable API. Android System, SystemUI and Device Care are excluded. Set up each Android profile separately. Selecting SleepStore itself only enables its connection indicator; it does not configure the store scopes.

## Limits and disabling

The store still controls compatibility, staged rollouts and whether an update is offered. Visibility does not guarantee an update. Protection applies inside selected processes; external root/Shizuku tools and OS changes are outside that scope.

SleepStore does not write the OS deep-sleep state. Disable the module and restart the selected stores to remove its behavior. The suggested store catalog is not a list of individually certified stores.

## Development and license

Build instructions, implementation details and validation records are in the [developer guide](docs/development.md). The project uses the [MIT License](LICENSE); dependencies are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) and the app.

[OneUI Mods](https://github.com/OneUI-Mods)
