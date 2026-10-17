# Changelog

## 0.2.0

- Migrated the complete hook entry and callback chain from legacy Xposed API 82 to libxposed API 102.
- Replaced manifest and `assets/xposed_init` registration with modern `META-INF/xposed` metadata.
- Kept the user-selectable framework scope while retaining the recommended store catalog.
- Added protective interceptor handling and API/framework details to runtime diagnostics.

## 0.1.0

- Shared PackageManager hooks for apps selected in Vector / LSPosed.
- Samsung auto-disable confirmation distinguishes deep sleep from ordinary disabled apps.
- Deep-sleeping packages are visible to selected update apps without changing OS restrictions.
- DEFAULT / ENABLED requests from selected processes are blocked for confirmed deep-sleeping packages.
- Recommendation catalog: 16 store/updater/installer families and 19 package IDs.
- Material 3 settings UI, installed app shortcuts, Japanese / English / system language, and in-app licenses.
- Dedicated release signing key; MIT source license and third party notices.

Android 16 on Samsung firmware is required. External root/Shizuku processes and OS-initiated changes are outside the hook scope.
