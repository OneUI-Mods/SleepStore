# Device validation

This file records the firmware and framework used for public release checks so later OS updates do not erase the compatibility baseline. It contains no device serial, account, application database, or firmware binaries.

## 2026-10-17 — SleepStore 0.2.0

- Device: Samsung Galaxy S26 Ultra, `SM-S948Q` (Japan unlocked model)
- OS: Android 16 / API 36
- Build fingerprint: `samsung/m3qjpnw/m3q:16/BP4A.251205.006/S948QOPS1AZHL_SJP1AZHL:user/release-keys`
- Xposed framework: Vector 2.2, version code 3080, runtime API 102
- Hosts checked: Google Play Store `53.5.48-34 [0] [PR] 995634172` and Galaxy Store `4.6.11.4`
- APK: release-signed SleepStore 0.2.0, version code 2

The release APK was installed over 0.1.0 with its existing signing key. Vector loaded `dev.ryuya.sleepstore.StoreHook` through the modern entry point in both `com.android.vending` and `com.android.vending:background`. Each process reported `MODULE_LOADED api=102`, installed 28 PackageManager method hooks, and completed the read-only startup diagnostic.

The main Play Store process reported 216 confirmed deep-sleeping packages visible, 6 other disabled packages preserved, and 0 diagnostic failures. The background process reported the same result. Galaxy Store also loaded API 102, installed 28 hooks, and reported the same zero-failure diagnostic. The SleepStore UI framework indicator was active. No fatal exception occurred during these checks.

This check verifies module loading, scope persistence across the 0.1.0-to-0.2.0 upgrade, Samsung deep-sleep classification, adjusted PackageManager reads, and preservation of unrelated disabled states. It does not claim that every listed store offered or installed an update during this run.
