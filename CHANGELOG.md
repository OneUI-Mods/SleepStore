# Changelog

## 0.2.8 — 2026-11-28

- ストアが更新を開始するとき、Samsungが自動無効化したアプリの無効状態を維持するようAndroidへ指定します。
- Android 17のGalaxyでは「ディープスリープを修復」から、既に状態がずれたアプリを検出して修復できます。root権限と修復前の確認が必要です。

## 0.2.6 — 2026-11-28

- Android 17のGalaxyで、ディープスリープ中のアプリを対象ストアから確認できるようにしました。
- SleepStoreの設定画面で接続済みでも「未接続」と表示される問題を修正しました。
- Vectorでホット更新した後に、動作中のストア内で診断を実行します。
- 0.2.4以前から更新する場合は、使用するストアを一度開き直してください。

## 0.2.1 — 2026-10-31

- API 102フレームワークとのアプリ側接続が成立せず再試行される問題を修正。
- 設定を引き継いで上書きし、設定アプリを開き直してください。ソフトリブートは不要です。

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
