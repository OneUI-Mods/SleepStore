# SleepStore

日本語 · [English](README.md)

Samsung One UI のディープスリープを維持したまま、Vector / LSPosed で選択したストア・更新アプリから更新を確認するための Xposed モジュールです。

## 仕組みと対象

Vector / LSPosed で選択したアプリ内で取得する情報だけを変更します。全対象で共通の PackageManager フックを使い、各ストアの内部処理を個別に改造しません。OS の状態が `DISABLED_UNTIL_USED`（4）で、さらに Samsung の `isPackageAutoDisabled` が真のアプリに限り、有効として見せます。通常の無効化（2／3）、Samsung の確認が取れないアプリ、判定失敗時は元の値を維持します。

ディープスリープの一覧、MARs の制限、実際の有効・無効設定は書き換えません。アプリを順番に起動する操作も行いません。Android システム・システム UI・デバイスケアには、選択されてもフックを適用しません。

ストアのサーバー側の配信条件や対応バージョンは変わりません。更新の検出と実際のインストールは区別して検証してください。選択したアプリ内から、ディープスリープ対象を DEFAULT／ENABLED に戻す要求も抑止します。外部の root／Shizuku コマンド、選択外のアプリや OS が行う処理は、このフックの範囲外です。

## 導入

1. Samsung Android 16 と modern Xposed API 102 対応環境を用意します。検証環境は Vector 2.2（3080）です。
2. APK をインストールし、フレームワークで SleepStore を有効にします。
3. 対象に使用するアプリを選びます。推奨対象は Play ストア、Galaxy Store、Aurora Store、Obtainium（通常版／F-Droid 版）です。上記以外も Vector で選択すれば同じ処理が適用されます。本アプリも選択すると、UI 内でフレームワークとの接続を表示できます。
4. 選択したアプリを強制終了して開き直し、更新を確認します。Runner の操作は不要です。

初期導入は個人プロファイル（user 0）のみです。他のプロファイルは別途対象設定が必要です。

UI は Material 3、ライト／ダーク、自動配色、日本語／英語／端末言語への追従に対応し、ライセンス全文を収録しています。接続表示は本アプリだけの状態であり、ストアへのフックを証明する表示ではありません。

## 開発と管理

JDK 21、Android SDK 36 を使用し、`./gradlew assembleRelease testReleaseUnitTest lintRelease` でビルド・単体テスト・Lint を実行し、`python3 tools/verify_modern_xposed.py` でAPKの登録方式も検査します。フックは libxposed API 102 のインターセプタ方式と modern `META-INF/xposed` メタデータを使い、API 82 のクラスと `assets/xposed_init` は APK に含めません。GitHub Actions でも同じチェックを行い、検証レポートを保存します。インストール用には Releases の専用鍵で署名したリリース APK を使用してください。

Play ストア・Galaxy Store・Aurora Store・Obtainium（F-Droid版）内での読み取りと通常の無効化の維持を実機で確認しました。Play ストアではディープスリープ中アプリの自動更新も確認しています。その後、通常／バックグラウンドの両プロセスから DEFAULT と ENABLED を要求しても、OS側の無効状態が維持されることを確認しました。全ストアのサーバー側更新検出とインストール完了までを検証したものではありません。公開可能なファームウェアとAPI 102の検証基準は [docs/validation.md](docs/validation.md) に残し、端末固有情報と内部メモは公開しません。

## 元に戻す

Vector / LSPosed で本モジュールを無効にするか、アンインストールし、対象アプリを再起動してください。ディープスリープ設定の復元作業は不要です。

MIT ライセンス。第三者ライブラリの表記は [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。端末情報やストア APK はリポジトリに含めません。

署名済みビルドは `signing.properties` とローカルの PKCS12 鍵を使用します。どちらも Git 対象外です。継続更新のため、両方を安全に保管してください。設定例は `signing.properties.example` にあります。

## 推奨対象の一覧

Play ストア、Galaxy Store、Aurora Store、Obtainium、F-Droid／F-Droid Basic、Droid-ify、Neo Store、Aptoide、APKPure、Uptodown、HUAWEI AppGallery、Xiaomi GetApps、TapTap、RuStore、Amazon Appstore、APKMirror Installer を選択候補に登録しています。個別の動作保証一覧ではなく、選択を補助するカタログです。配信や対応端末は各ストアの条件によります。

UI にはインストール済みの候補を表示します。一覧にないアプリも Vector で選択すれば共通フックを適用できます。カタログは `config/stores.json`（16種類・19パッケージ）で管理し、変更時は `python3 tools/sync_stores.py` で関連ファイルを生成します。
